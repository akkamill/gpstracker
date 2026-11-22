package com.example.gpstracker.data.support

import com.example.gpstracker.model.api.ChatClientEvent
import com.example.gpstracker.model.api.ChatMessage
import com.example.gpstracker.model.api.ChatSender
import com.example.gpstracker.model.api.ChatServerEvent
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupportChatRepositoryTest {
    /** A stand-in for the WebSocket: the test plays the server. */
    private class FakeTransport : ChatTransport {
        val sent = mutableListOf<ChatClientEvent>()
        val tokens = mutableListOf<String>()
        var server: Channel<ChatServerEvent>? = null
        var reachable = true

        override suspend fun run(token: String, outgoing: ReceiveChannel<ChatClientEvent>, onEvent: suspend (ChatServerEvent) -> Unit) {
            tokens += token
            if (!reachable) throw IOException("Connection refused")
            val events = Channel<ChatServerEvent>(Channel.UNLIMITED).also { server = it }
            coroutineScope {
                val reader = launch { for (e in outgoing) sent += e }
                for (e in events) onEvent(e)
                reader.cancel()
            }
        }

        fun emit(event: ChatServerEvent) = server!!.trySend(event)

        /** The connection drops. */
        fun drop() = server!!.close()
    }

    private val transport = FakeTransport()
    private var token: String? = "token-ona"
    private var ids = 0

    private fun TestScope.repository() = SupportChatRepository(transport, {
        token
    }, backgroundScope, clock = { 0 }, newId = { "c${++ids}" })

    private fun message(id: Long, text: String, sender: ChatSender = ChatSender.Driver, clientId: String? = null) =
        ChatMessage(id, "conv", sender, text, sentAtMs = id, clientId = clientId)

    @Test
    fun `demo mode has no chat`() = runTest {
        token = null
        val chat = repository()
        chat.open()
        runCurrent()
        assertEquals(ChatConnection.SignedOut, chat.state.value.connection)
        assertTrue(transport.tokens.isEmpty())
    }

    @Test
    fun `opening the chat connects and shows the earlier messages`() = runTest {
        val chat = repository()
        chat.open()
        runCurrent()
        assertEquals(ChatConnection.Connecting, chat.state.value.connection)
        transport.emit(ChatServerEvent.History("conv", listOf(message(1, "Hello"))))
        transport.emit(ChatServerEvent.AgentsOnline(true))
        runCurrent()
        assertEquals(ChatConnection.Connected, chat.state.value.connection)
        assertEquals(listOf("Hello"), chat.state.value.messages.map { it.text })
        assertTrue(chat.state.value.agentsOnline)
        assertEquals(listOf("token-ona"), transport.tokens)
    }

    @Test
    fun `a message shows as sending until the server confirms it`() = runTest {
        val chat = repository()
        chat.open()
        runCurrent()
        transport.emit(ChatServerEvent.History("conv", emptyList()))
        runCurrent()

        assertTrue(chat.send("  My trip was not saved  "))
        runCurrent()
        assertEquals(listOf("My trip was not saved"), chat.state.value.pending.map { it.text })
        assertEquals(listOf(ChatClientEvent.Send("My trip was not saved", "c1")), transport.sent)

        transport.emit(ChatServerEvent.Message(message(5, "My trip was not saved", clientId = "c1")))
        runCurrent()
        assertTrue(chat.state.value.pending.isEmpty())
        assertEquals(listOf(5L), chat.state.value.messages.map { it.id })
    }

    @Test
    fun `messages written without a connection go out after reconnecting`() = runTest {
        val chat = repository()
        chat.open()
        runCurrent()
        transport.emit(ChatServerEvent.History("conv", emptyList()))
        runCurrent()
        transport.reachable = false
        transport.drop()
        runCurrent()
        assertEquals(ChatConnection.Offline, chat.state.value.connection)
        assertFalse(chat.state.value.agentsOnline)

        chat.send("Are you there?")
        advanceTimeBy(SupportChatRepository.retryDelayMs(0) + 1) // first retry: the server is still down
        runCurrent()
        assertEquals(listOf("token-ona", "token-ona"), transport.tokens)
        assertTrue(transport.sent.isEmpty())

        transport.reachable = true
        advanceTimeBy(SupportChatRepository.retryDelayMs(1) + 1) // second retry: it is back
        runCurrent()
        transport.emit(ChatServerEvent.History("conv", emptyList()))
        runCurrent()
        assertEquals(ChatConnection.Connected, chat.state.value.connection)
        assertEquals(listOf(ChatClientEvent.Send("Are you there?", "c1")), transport.sent)
        assertEquals(1, chat.state.value.pending.size) // until the server confirms it
    }

    @Test
    fun `retries wait longer each time, up to half a minute`() {
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (0..6).map {
                SupportChatRepository.retryDelayMs(it)
            },
        )
    }

    @Test
    fun `empty or too long messages are not sent`() = runTest {
        val chat = repository()
        assertFalse(chat.send("   "))
        assertFalse(chat.send("x".repeat(2_001)))
        assertTrue(chat.state.value.pending.isEmpty())
    }

    @Test
    fun `closing the chat ends the connection, and another account starts empty`() = runTest {
        val chat = repository()
        chat.open()
        runCurrent()
        transport.emit(ChatServerEvent.History("conv", listOf(message(1, "Ona's question"))))
        runCurrent()
        chat.close()
        runCurrent()
        assertTrue("connection closed", transport.server!!.isClosedForSend || transport.server!!.isEmpty)

        token = "token-jonas"
        chat.open()
        runCurrent()
        assertTrue(chat.state.value.messages.isEmpty())
        assertEquals(listOf("token-ona", "token-jonas"), transport.tokens)
    }
}
