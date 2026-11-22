package com.example.gpstracker.server

import com.example.gpstracker.domain.auth.PasswordHasher
import com.example.gpstracker.model.api.AccountInfo
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.AuthResponse
import com.example.gpstracker.model.api.HealthResponse
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import com.example.gpstracker.server.accounts.InMemoryAccountRepository
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.DemoAccount
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.reports.InMemoryReportStore
import com.example.gpstracker.server.reports.ReportService
import com.example.gpstracker.server.support.InMemorySupportRepository
import com.example.gpstracker.server.support.SupportHub
import com.example.gpstracker.tracing.MemorySink
import com.example.gpstracker.tracing.Tracing
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The HTTP API end to end, with accounts in memory instead of PostgreSQL. */
class ServerApiTest {
    private val sink = MemorySink()
    private var databaseUp = true
    private val tokens = Tokens(secret = "a".repeat(32), lifetimeMs = 60_000)

    @Before
    fun setUp() {
        Tracing.reset()
        Tracing.install(sink)
    }

    @After
    fun tearDown() = Tracing.reset()

    private fun api(test: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application {
            driveMetrics(
                ServerDependencies(
                    auth = AuthService(InMemoryAccountRepository(), tokens, PasswordHasher(iterations = 2)),
                    tokens = tokens,
                    databaseUp = { databaseUp },
                    reports = ReportService(InMemoryReportStore()),
                    support = SupportHub(InMemorySupportRepository()),
                    agentKey = null,
                ),
            )
        }
        test(createClient { install(ContentNegotiation) { json() } })
    }

    private suspend fun HttpClient.postJson(path: String, body: Any): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private val ona = SignUpRequest("Ona", "Ona@Example.com", "secret123")

    @Test
    fun `health says whether the database answers`() = api { client ->
        assertEquals(HealthResponse("ok", "up"), client.get("/health").body<HealthResponse>())
        databaseUp = false
        val down = client.get("/health")
        assertEquals(HttpStatusCode.ServiceUnavailable, down.status)
        assertEquals("down", down.body<HealthResponse>().database)
    }

    @Test
    fun `sign up returns a token that opens the account`() = api { client ->
        val created = client.postJson("/api/auth/sign-up", ona)
        assertEquals(HttpStatusCode.Created, created.status)
        val auth = created.body<AuthResponse>()
        assertEquals("ona@example.com", auth.account.email)

        val me = client.get("/api/me") { bearerAuth(auth.token) }
        assertEquals(AccountInfo(auth.account.id, "Ona", "ona@example.com"), me.body<AccountInfo>())
    }

    @Test
    fun `an email can be registered only once, whatever its case`() = api { client ->
        client.postJson("/api/auth/sign-up", ona)
        val again = client.postJson("/api/auth/sign-up", ona.copy(email = "ONA@example.com "))
        assertEquals(HttpStatusCode.Conflict, again.status)
        assertEquals(ApiError.EMAIL_TAKEN, again.body<ApiError>().code)
    }

    @Test
    fun `sign in works with the right password only`() = api { client ->
        client.postJson("/api/auth/sign-up", ona)
        assertEquals(HttpStatusCode.OK, client.postJson("/api/auth/sign-in", SignInRequest("ona@example.com", "secret123")).status)
        listOf(SignInRequest("ona@example.com", "wrong-one"), SignInRequest("nobody@example.com", "secret123")).forEach {
            val refused = client.postJson("/api/auth/sign-in", it)
            assertEquals(HttpStatusCode.Unauthorized, refused.status)
            assertEquals(ApiError.INVALID_CREDENTIALS, refused.body<ApiError>().code)
        }
    }

    @Test
    fun `bad input is refused with a reason`() = api { client ->
        val badInput = listOf(
            ona.copy(name = " "),
            ona.copy(email = "ona@"),
            ona.copy(password = "short1"),
            ona.copy(password = "only-letters"),
            ona.copy(password = "12345678"),
        )
        badInput.forEach {
            val refused = client.postJson("/api/auth/sign-up", it)
            assertEquals(HttpStatusCode.BadRequest, refused.status)
            assertEquals(ApiError.INVALID_INPUT, refused.body<ApiError>().code)
        }
        val unreadable = client.post("/api/auth/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }
        assertEquals(HttpStatusCode.BadRequest, unreadable.status)
    }

    @Test
    fun `the demo account is created once and can sign in`() = testApplication {
        val auth = AuthService(InMemoryAccountRepository(), tokens, PasswordHasher(iterations = 2))
        assertTrue(auth.addDemoAccount())
        assertFalse(auth.addDemoAccount())
        assertEquals("Demo Driver", auth.signIn(SignInRequest(DemoAccount.EMAIL, DemoAccount.PASSWORD)).account.name)
    }

    @Test
    fun `the account needs a valid token`() = api { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/me").status)
        val forged = Tokens(secret = "b".repeat(32), lifetimeMs = 60_000).issue(java.util.UUID.randomUUID()).first
        val refused = client.get("/api/me") { bearerAuth(forged) }
        assertEquals(HttpStatusCode.Unauthorized, refused.status)
        assertEquals(ApiError.UNAUTHORIZED, refused.body<ApiError>().code)
    }

    @Test
    fun `logs never contain emails, names, passwords or tokens`() = api { client ->
        val token = client.postJson("/api/auth/sign-up", ona).body<AuthResponse>().token
        client.postJson("/api/auth/sign-in", SignInRequest("ona@example.com", "wrong-one"))
        client.postJson("/api/auth/sign-up", ona)
        val logs = sink.records.value.joinToString("\n") { it.format() }
        assertTrue(logs.contains("account created"))
        listOf("@", "Ona", "secret123", "wrong-one", token).forEach { assertTrue("logs contain $it", it !in logs) }
        assertTrue("password shows in toString", "secret123" !in ona.toString())
        assertTrue(client.get("/health").bodyAsText().isNotEmpty())
    }
}
