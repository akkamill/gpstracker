package com.example.gpstracker.server.support

import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.ChatClientEvent
import com.example.gpstracker.model.api.ChatLimits
import com.example.gpstracker.model.api.ChatSender
import com.example.gpstracker.model.api.ChatServerEvent
import com.example.gpstracker.tracing.Tracing
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** One open chat connection: a driver's app or a support agent's page. */
fun interface ChatPeer {
    suspend fun send(event: ChatServerEvent)
}

/**
 * The live part of the support chat. It knows who is connected, saves every message and passes
 * it on: a driver's message to every agent (and the driver's other devices), an agent's
 * message to that driver and to the other agents.
 *
 * Logs never contain message texts or names.
 */
class SupportHub(private val repository: SupportRepository) {
    private val tracer = Tracing.tracer("server")
    private val drivers = ConcurrentHashMap<String, MutableSet<ChatPeer>>()
    private val agents: MutableSet<ChatPeer> = ConcurrentHashMap.newKeySet()

    /** A driver opened the chat. Returns their conversation id. */
    suspend fun driverJoined(accountId: UUID, peer: ChatPeer): String {
        val conversationId = repository.conversationOf(accountId)
        drivers.computeIfAbsent(conversationId) { ConcurrentHashMap.newKeySet() } += peer
        peer.send(ChatServerEvent.History(conversationId, repository.messages(conversationId)))
        peer.send(ChatServerEvent.AgentsOnline(agents.isNotEmpty()))
        tracer.info("driver joined the support chat", "conversation" to conversationId)
        return conversationId
    }

    fun driverLeft(conversationId: String, peer: ChatPeer) {
        drivers[conversationId]?.remove(peer)
    }

    suspend fun fromDriver(conversationId: String, event: ChatClientEvent, peer: ChatPeer) {
        when (event) {
            // a driver can only write in their own conversation, whatever the event says
            is ChatClientEvent.Send -> post(conversationId, ChatSender.Driver, event, peer)

            is ChatClientEvent.Open -> peer.send(ChatServerEvent.History(conversationId, repository.messages(conversationId)))
        }
    }

    suspend fun agentJoined(peer: ChatPeer) {
        val first = agents.isEmpty()
        agents += peer
        peer.send(ChatServerEvent.Conversations(repository.conversations()))
        if (first) toAllDrivers(ChatServerEvent.AgentsOnline(true))
        tracer.info("support agent joined", "agentsOnline" to agents.size)
    }

    suspend fun agentLeft(peer: ChatPeer) {
        agents.remove(peer)
        if (agents.isEmpty()) toAllDrivers(ChatServerEvent.AgentsOnline(false))
        tracer.info("support agent left", "agentsOnline" to agents.size)
    }

    suspend fun fromAgent(event: ChatClientEvent, peer: ChatPeer) {
        val conversationId = when (event) {
            is ChatClientEvent.Send -> event.conversationId
            is ChatClientEvent.Open -> event.conversationId
        }
        if (conversationId == null || !repository.exists(conversationId)) {
            peer.send(ChatServerEvent.Error(ApiError(ApiError.NOT_FOUND, "Conversation not found.")))
            return
        }
        when (event) {
            is ChatClientEvent.Send -> post(conversationId, ChatSender.Agent, event, peer)
            is ChatClientEvent.Open -> peer.send(ChatServerEvent.History(conversationId, repository.messages(conversationId)))
        }
    }

    private suspend fun post(conversationId: String, sender: ChatSender, event: ChatClientEvent.Send, peer: ChatPeer) {
        val text = event.text.trim()
        if (text.isEmpty() || text.length > ChatLimits.MAX_TEXT) {
            peer.send(ChatServerEvent.Error(ApiError(ApiError.INVALID_INPUT, "Write between 1 and ${ChatLimits.MAX_TEXT} characters.")))
            return
        }
        val message = ChatServerEvent.Message(repository.add(conversationId, sender, text, event.clientId))
        drivers[conversationId].orEmpty().forEach { it.trySend(message) }
        agents.forEach { it.trySend(message) }
        val list = ChatServerEvent.Conversations(repository.conversations())
        agents.forEach { it.trySend(list) }
        tracer.info("support message", "conversation" to conversationId, "from" to sender)
    }

    private suspend fun toAllDrivers(event: ChatServerEvent) = drivers.values.flatten().forEach { it.trySend(event) }

    /** A connection that just closed must not stop the message reaching everyone else. */
    private suspend fun ChatPeer.trySend(event: ChatServerEvent) {
        runCatching { send(event) }.onFailure { tracer.warn("could not reach a chat connection", "reason" to it.javaClass.simpleName) }
    }
}
