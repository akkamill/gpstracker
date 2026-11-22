package com.example.gpstracker.data.support

import com.example.gpstracker.model.api.ChatClientEvent
import com.example.gpstracker.model.api.ChatLimits
import com.example.gpstracker.model.api.ChatMessage
import com.example.gpstracker.model.api.ChatServerEvent
import com.example.gpstracker.tracing.Tracing
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ChatConnection {
    /** Chat needs an account; demo mode has none. */
    SignedOut,
    Connecting,
    Connected,

    /** The connection failed or dropped; the next try is on its way. */
    Offline,
}

/** A message typed on this phone that the server has not confirmed yet. */
data class PendingMessage(val clientId: String, val text: String, val createdAtMs: Long)

data class SupportChatState(
    val connection: ChatConnection = ChatConnection.Connecting,
    val agentsOnline: Boolean = false,
    /** Confirmed messages, oldest first. */
    val messages: List<ChatMessage> = emptyList(),
    /** Messages still on their way, oldest first; sent again after a reconnect. */
    val pending: List<PendingMessage> = emptyList(),
)

/** Gives the signed-in account's token, or null in demo mode. */
fun interface TokenProvider {
    fun currentToken(): String?
}

/**
 * The support chat for the screen. While the chat is open it keeps a connection to the server,
 * reconnecting by itself with growing pauses. A message is shown at once as "sending" and
 * becomes a normal message when the server confirms it; messages not confirmed before a
 * connection drops are sent again (the server stores each one once).
 *
 * Logs never contain message texts.
 */
class SupportChatRepository(
    private val transport: ChatTransport,
    private val tokens: TokenProvider,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val tracer = Tracing.tracer("app")
    private val lock = Any()
    private val _state = MutableStateFlow(SupportChatState())
    val state: StateFlow<SupportChatState> = _state.asStateFlow()

    private var job: Job? = null
    private var users = 0
    private var outgoing: Channel<ChatClientEvent>? = null
    private var lastToken: String? = null

    /** The chat screen opened. Call [close] when it closes. */
    fun open() = synchronized(lock) {
        users++
        if (job?.isActive != true) job = scope.launch { stayConnected() }
    }

    fun close() = synchronized(lock) {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) {
            job?.cancel()
            job = null
        }
    }

    /** Returns false when the text is empty or too long. */
    fun send(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > ChatLimits.MAX_TEXT) return false
        val message = PendingMessage(newId(), clean, clock())
        _state.update { it.copy(pending = it.pending + message) }
        if (_state.value.connection == ChatConnection.Connected) outgoing?.trySend(ChatClientEvent.Send(message.text, message.clientId))
        return true
    }

    private suspend fun stayConnected() {
        var failures = 0
        while (currentCoroutineContext().isActive) {
            val token = tokens.currentToken()
            if (token == null) {
                _state.value = SupportChatState(connection = ChatConnection.SignedOut)
                return
            }
            // another account signed in: its chat starts empty
            if (token != lastToken) {
                if (lastToken != null) _state.value = SupportChatState()
                lastToken = token
            }
            _state.update { it.copy(connection = ChatConnection.Connecting) }
            val channel = Channel<ChatClientEvent>(Channel.UNLIMITED).also { outgoing = it }
            try {
                transport.run(token, channel) { event ->
                    failures = 0
                    onEvent(event, channel)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // any failure of the connection (network, server, refused sign-in) means: try again later
                tracer.warn("support chat connection failed", "reason" to e.javaClass.simpleName)
            } finally {
                channel.close()
            }
            _state.update { it.copy(connection = ChatConnection.Offline, agentsOnline = false) }
            delay(retryDelayMs(failures++))
        }
    }

    private fun onEvent(event: ChatServerEvent, channel: Channel<ChatClientEvent>) {
        when (event) {
            is ChatServerEvent.History -> {
                _state.update { it.copy(connection = ChatConnection.Connected, messages = event.messages) }
                drop(event.messages)
                // whatever was typed while offline goes out now
                _state.value.pending.forEach { channel.trySend(ChatClientEvent.Send(it.text, it.clientId)) }
                tracer.info("support chat connected", "messages" to event.messages.size)
            }

            is ChatServerEvent.Message -> {
                _state.update { s -> s.copy(messages = (s.messages + event.message).distinctBy { it.id }.sortedBy { it.id }) }
                drop(listOf(event.message))
            }

            is ChatServerEvent.AgentsOnline -> _state.update { it.copy(agentsOnline = event.online) }

            is ChatServerEvent.Error -> tracer.warn("support chat refused a message", "code" to event.error.code)

            is ChatServerEvent.Conversations -> Unit
        }
    }

    /** Pending messages the server has confirmed are no longer pending. */
    private fun drop(confirmed: List<ChatMessage>) {
        val ids = confirmed.mapNotNull { it.clientId }.toSet()
        if (ids.isNotEmpty()) _state.update { s -> s.copy(pending = s.pending.filterNot { it.clientId in ids }) }
    }

    companion object {
        private const val FIRST_RETRY_MS = 1_000L
        private const val MAX_RETRY_MS = 30_000L
        private const val MAX_DOUBLINGS = 5

        /** 1, 2, 4, 8, 16, then 30 seconds. */
        fun retryDelayMs(failures: Int): Long = (FIRST_RETRY_MS shl failures.coerceAtMost(MAX_DOUBLINGS)).coerceAtMost(MAX_RETRY_MS)
    }
}
