package com.example.gpstracker.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * The support chat: a driver and the support team write to each other over a WebSocket.
 * Every frame is one JSON object with a "type" field naming the event, for example
 * {"type":"message","message":{...}}. The app, the server and the agents' web page all use
 * these same events.
 */

@Serializable
enum class ChatSender { Driver, Agent }

/**
 * One chat message, as stored by the server.
 *
 * @property clientId set by the sender's device, so it can match the server's copy to the
 * message it showed as "sending"
 */
@Serializable
data class ChatMessage(
    val id: Long,
    val conversationId: String,
    val sender: ChatSender,
    val text: String,
    val sentAtMs: Long,
    val clientId: String? = null,
)

/** One driver's conversation, as listed for the support team. */
@Serializable
data class ConversationSummary(
    val id: String,
    val driverName: String,
    val lastMessage: ChatMessage? = null,
    /** The driver wrote last, so the team owes an answer. */
    val waitingForAgent: Boolean = false,
)

/** What the server sends. */
@Serializable
sealed interface ChatServerEvent {
    /** The latest messages of a conversation, oldest first; sent when a chat opens. */
    @Serializable
    @SerialName("history")
    data class History(val conversationId: String, val messages: List<ChatMessage>) : ChatServerEvent

    /** A new message, also the sender's own once it is saved. */
    @Serializable
    @SerialName("message")
    data class Message(val message: ChatMessage) : ChatServerEvent

    /** Whether someone from the support team is connected right now (sent to drivers). */
    @Serializable
    @SerialName("agents")
    data class AgentsOnline(val online: Boolean) : ChatServerEvent

    /** All conversations, most recent first (sent to the support team). */
    @Serializable
    @SerialName("conversations")
    data class Conversations(val conversations: List<ConversationSummary>) : ChatServerEvent

    @Serializable
    @SerialName("error")
    data class Error(val error: ApiError) : ChatServerEvent
}

/** What a driver's app or the support team's page sends. */
@Serializable
sealed interface ChatClientEvent {
    /**
     * A new message. A driver always writes in their own conversation; the support team names
     * the [conversationId] they answer in.
     */
    @Serializable
    @SerialName("send")
    data class Send(val text: String, val clientId: String, val conversationId: String? = null) : ChatClientEvent

    /** The support team opens a conversation and gets its [ChatServerEvent.History]. */
    @Serializable
    @SerialName("open")
    data class Open(val conversationId: String) : ChatClientEvent
}

/** Limits both sides check. */
object ChatLimits {
    const val MAX_TEXT = 2_000
    const val HISTORY_SIZE = 100
}

/**
 * How chat events are written as JSON on every side. Default values are written too, so the
 * agents' web page always finds every field.
 */
val ChatJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

/** Reads one chat frame; null when it is not a known event. */
inline fun <reified T> Json.decodeChatOrNull(text: String): T? = runCatching { decodeFromString<T>(text) }.getOrNull()
