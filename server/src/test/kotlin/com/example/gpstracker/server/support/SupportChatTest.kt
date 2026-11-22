package com.example.gpstracker.server.support

import com.example.gpstracker.domain.auth.PasswordHasher
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.ChatClientEvent
import com.example.gpstracker.model.api.ChatJson
import com.example.gpstracker.model.api.ChatSender
import com.example.gpstracker.model.api.ChatServerEvent
import com.example.gpstracker.server.ServerDependencies
import com.example.gpstracker.server.accounts.InMemoryAccountRepository
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.driveMetrics
import com.example.gpstracker.tracing.MemorySink
import com.example.gpstracker.tracing.Tracing
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.UUID
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The support chat end to end over real WebSockets, with storage in memory. */
class SupportChatTest {
    private val sink = MemorySink()
    private val tokens = Tokens(secret = "a".repeat(32), lifetimeMs = 60_000)
    private val ona = UUID.randomUUID()
    private val jonas = UUID.randomUUID()
    private val names = mapOf(ona to "Ona", jonas to "Jonas")

    @Before
    fun setUp() {
        Tracing.reset()
        Tracing.install(sink)
    }

    @After
    fun tearDown() = Tracing.reset()

    private fun chat(test: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application {
            driveMetrics(
                ServerDependencies(
                    auth = AuthService(InMemoryAccountRepository(), tokens, PasswordHasher(iterations = 2)),
                    tokens = tokens,
                    databaseUp = { true },
                    support = SupportHub(InMemorySupportRepository(names = { names.getValue(it) })),
                    agentKey = AGENT_KEY,
                ),
            )
        }
        test(createClient { install(WebSockets) })
    }

    private suspend fun HttpClient.driver(account: UUID) =
        webSocketSession("/api/support/chat") { bearerAuth(tokens.issue(account).first) }

    private suspend fun HttpClient.agent(key: String = AGENT_KEY) = webSocketSession("/api/support/agent?key=$key")

    private suspend fun DefaultClientWebSocketSession.next(): ChatServerEvent = withTimeout(5_000) {
        ChatJson.decodeFromString(ChatServerEvent.serializer(), (incoming.receive() as Frame.Text).readText())
    }

    private suspend fun DefaultClientWebSocketSession.say(event: ChatClientEvent) =
        send(Frame.Text(ChatJson.encodeToString(ChatClientEvent.serializer(), event)))

    @Test
    fun `a driver and the support team talk to each other`() = chat { client ->
        val driver = client.driver(ona)
        val history = driver.next() as ChatServerEvent.History
        assertTrue(history.messages.isEmpty())
        assertEquals(ChatServerEvent.AgentsOnline(false), driver.next())

        val agent = client.agent()
        assertEquals(listOf("Ona"), (agent.next() as ChatServerEvent.Conversations).conversations.map { it.driverName })
        assertEquals(ChatServerEvent.AgentsOnline(true), driver.next())

        driver.say(ChatClientEvent.Send("My trip was not saved", clientId = "c1"))
        val sent = (driver.next() as ChatServerEvent.Message).message
        assertEquals("c1", sent.clientId)
        assertEquals(sent, (agent.next() as ChatServerEvent.Message).message)
        val list = (agent.next() as ChatServerEvent.Conversations).conversations.single()
        assertTrue("waiting for an answer", list.waitingForAgent)

        agent.say(ChatClientEvent.Send("Let me check that for you.", clientId = "a1", conversationId = history.conversationId))
        val answer = (driver.next() as ChatServerEvent.Message).message
        assertEquals(ChatSender.Agent, answer.sender)
        assertEquals("Let me check that for you.", answer.text)

        agent.close()
        assertEquals(ChatServerEvent.AgentsOnline(false), driver.next())
    }

    @Test
    fun `a driver sees the earlier messages when coming back`() = chat { client ->
        val first = client.driver(ona)
        first.next()
        first.next()
        first.say(ChatClientEvent.Send("Hello", clientId = "c1"))
        first.next()
        first.close()

        val again = client.driver(ona)
        assertEquals(listOf("Hello"), (again.next() as ChatServerEvent.History).messages.map { it.text })
    }

    @Test
    fun `a message sent again after a lost connection is saved once`() = chat { client ->
        val first = client.driver(ona)
        first.next()
        first.next()
        first.say(ChatClientEvent.Send("Hello", clientId = "c1"))
        val saved = (first.next() as ChatServerEvent.Message).message
        first.close()

        val again = client.driver(ona)
        again.next()
        again.next()
        again.say(ChatClientEvent.Send("Hello", clientId = "c1"))
        assertEquals(saved, (again.next() as ChatServerEvent.Message).message)
        val third = client.driver(ona)
        assertEquals(1, (third.next() as ChatServerEvent.History).messages.size)
    }

    @Test
    fun `drivers only ever see and write in their own conversation`() = chat { client ->
        val onaChat = client.driver(ona)
        val onaConversation = (onaChat.next() as ChatServerEvent.History).conversationId
        onaChat.next()
        val jonasChat = client.driver(jonas)
        jonasChat.next()
        jonasChat.next()

        // naming another conversation does not help: the message lands in Jonas's own
        jonasChat.say(ChatClientEvent.Send("hi", clientId = "j1", conversationId = onaConversation))
        val message = (jonasChat.next() as ChatServerEvent.Message).message
        assertTrue(message.conversationId != onaConversation)
    }

    @Test
    fun `empty, too long or unreadable messages are refused`() = chat { client ->
        val driver = client.driver(ona)
        driver.next()
        driver.next()
        driver.say(ChatClientEvent.Send("   ", clientId = "c1"))
        assertEquals(ApiError.INVALID_INPUT, (driver.next() as ChatServerEvent.Error).error.code)
        driver.say(ChatClientEvent.Send("x".repeat(2_001), clientId = "c2"))
        assertEquals(ApiError.INVALID_INPUT, (driver.next() as ChatServerEvent.Error).error.code)
        driver.send(Frame.Text("{not json"))
        assertEquals(ApiError.INVALID_INPUT, (driver.next() as ChatServerEvent.Error).error.code)
    }

    @Test
    fun `the chat needs a signed-in driver or the team's key`() = chat { client ->
        val refused = runCatching { client.webSocketSession("/api/support/chat") }
        assertTrue("no token, no chat", refused.isFailure)

        val agent = client.agent(key = "guess")
        val reason = withTimeout(5_000) { agent.closeReason.await() }
        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, reason?.code)
    }

    @Test
    fun `the team answers only in conversations that exist`() = chat { client ->
        val agent = client.agent()
        agent.next()
        agent.say(ChatClientEvent.Send("hello?", clientId = "a1", conversationId = UUID.randomUUID().toString()))
        assertEquals(ApiError.NOT_FOUND, (agent.next() as ChatServerEvent.Error).error.code)
    }

    @Test
    fun `the support page is served and logs never contain message texts or names`() = chat { client ->
        assertTrue(client.get("/support/index.html").bodyAsText().contains("DriveMetrics Support"))
        assertTrue(client.get("/support/").bodyAsText().contains("DriveMetrics Support"))

        val driver = client.driver(ona)
        driver.next()
        driver.next()
        driver.say(ChatClientEvent.Send("my private question", clientId = "c1"))
        driver.next()
        val logs = sink.records.value.joinToString("\n") { it.format() }
        assertTrue(logs.contains("support message"))
        listOf("my private question", "Ona").forEach { assertTrue("logs contain $it", it !in logs) }
    }

    private companion object {
        const val AGENT_KEY = "test-support-key"
    }
}
