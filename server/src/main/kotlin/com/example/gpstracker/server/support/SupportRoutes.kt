package com.example.gpstracker.server.support

import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.ChatClientEvent
import com.example.gpstracker.model.api.ChatJson
import com.example.gpstracker.model.api.ChatServerEvent
import com.example.gpstracker.model.api.decodeChatOrNull
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.security.MessageDigest
import java.util.UUID

/** `WS /api/support/chat`: a signed-in driver's chat. Must sit inside an authenticated route. */
fun Route.driverChat(hub: SupportHub) {
    webSocket("/support/chat") {
        val accountId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
        val peer = peer()
        val conversationId = hub.driverJoined(accountId, peer)
        try {
            forEachEvent(peer) { hub.fromDriver(conversationId, it, peer) }
        } finally {
            hub.driverLeft(conversationId, peer)
        }
    }
}

/**
 * `WS /api/support/agent?key=...`: the support team's page. A browser cannot add headers to a
 * WebSocket, so the team's key comes in the address. Without a configured key, nobody gets in.
 */
fun Route.agentChat(hub: SupportHub, agentKey: String?) {
    webSocket("/support/agent") {
        val key = call.request.queryParameters["key"].orEmpty()
        if (agentKey == null || !MessageDigest.isEqual(key.toByteArray(), agentKey.toByteArray())) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Wrong support key"))
            return@webSocket
        }
        val peer = peer()
        hub.agentJoined(peer)
        try {
            forEachEvent(peer) { hub.fromAgent(it, peer) }
        } finally {
            hub.agentLeft(peer)
        }
    }
}

private fun DefaultWebSocketServerSession.peer() = ChatPeer { event ->
    send(Frame.Text(ChatJson.encodeToString(ChatServerEvent.serializer(), event)))
}

private suspend fun DefaultWebSocketServerSession.forEachEvent(peer: ChatPeer, handle: suspend (ChatClientEvent) -> Unit) {
    for (frame in incoming) {
        if (frame !is Frame.Text) continue
        val event = ChatJson.decodeChatOrNull<ChatClientEvent>(frame.readText())
        if (event == null) {
            peer.send(ChatServerEvent.Error(ApiError(ApiError.INVALID_INPUT, "Unknown event.")))
        } else {
            handle(event)
        }
    }
}
