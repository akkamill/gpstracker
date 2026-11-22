package com.example.gpstracker.data.support

import com.example.gpstracker.model.api.ChatClientEvent
import com.example.gpstracker.model.api.ChatJson
import com.example.gpstracker.model.api.ChatServerEvent
import com.example.gpstracker.model.api.decodeChatOrNull
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** One way of reaching the support chat; tests use a fake one. */
fun interface ChatTransport {
    /**
     * Keeps one chat connection open until it closes or fails: sends what arrives in [outgoing]
     * and passes every event from the server to [onEvent]. Returns or throws when the
     * connection ends; the caller decides whether to try again.
     */
    suspend fun run(token: String, outgoing: ReceiveChannel<ChatClientEvent>, onEvent: suspend (ChatServerEvent) -> Unit)
}

/** The chat over a WebSocket to the project's server, signed in with the account's token. */
class WebSocketChatTransport(private val http: HttpClient, baseUrl: String) : ChatTransport {
    private val url = baseUrl.trimEnd('/').replaceFirst("http", "ws") + "/api/support/chat"

    override suspend fun run(token: String, outgoing: ReceiveChannel<ChatClientEvent>, onEvent: suspend (ChatServerEvent) -> Unit) {
        http.webSocket(urlString = url, request = { bearerAuth(token) }) {
            coroutineScope {
                val sender = launch {
                    for (event in outgoing) send(Frame.Text(ChatJson.encodeToString(ChatClientEvent.serializer(), event)))
                }
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) ChatJson.decodeChatOrNull<ChatServerEvent>(frame.readText())?.let { onEvent(it) }
                    }
                } finally {
                    sender.cancel()
                }
            }
        }
    }
}
