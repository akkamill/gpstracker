package com.example.gpstracker.server.trips

import com.example.gpstracker.domain.auth.PasswordHasher
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.server.ServerDependencies
import com.example.gpstracker.server.accounts.InMemoryAccountRepository
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.driveMetrics
import com.example.gpstracker.server.reports.InMemoryReportStore
import com.example.gpstracker.server.reports.ReportService
import com.example.gpstracker.server.support.InMemorySupportRepository
import com.example.gpstracker.server.support.SupportHub
import com.example.gpstracker.simulator.history.DemoHistory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each driver's trip history over HTTP, with storage in memory. */
class TripApiTest {
    private val tokens = Tokens(secret = "a".repeat(32), lifetimeMs = 60_000)
    private val ona = UUID.randomUUID()
    private val jonas = UUID.randomUUID()
    private val history = DemoHistory.build(nowMs = 1_700_000_000_000L)

    private fun api(test: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application {
            driveMetrics(
                ServerDependencies(
                    auth = AuthService(InMemoryAccountRepository(), tokens, PasswordHasher(iterations = 2)),
                    tokens = tokens,
                    databaseUp = { true },
                    support = SupportHub(InMemorySupportRepository()),
                    reports = ReportService(InMemoryReportStore()),
                    trips = InMemoryTripStore(),
                    agentKey = null,
                ),
            )
        }
        test(createClient { install(ContentNegotiation) { json() } })
    }

    private suspend fun HttpClient.upload(account: UUID, trip: SavedTrip, id: String = trip.id) = put("/api/trips/$id") {
        bearerAuth(tokens.issue(account).first)
        contentType(ContentType.Application.Json)
        setBody(trip)
    }

    private suspend fun HttpClient.list(account: UUID): List<SavedTrip> =
        get("/api/trips") { bearerAuth(tokens.issue(account).first) }.body()

    @Test
    fun `a driver's trips come back complete, newest first`() = api { client ->
        history.take(3).reversed().forEach { assertEquals(HttpStatusCode.OK, client.upload(ona, it).status) }
        assertEquals(history.take(3), client.list(ona))
    }

    @Test
    fun `sending a trip again replaces it`() = api { client ->
        client.upload(ona, history[0])
        client.upload(ona, history[0].copy(trip = history[0].trip.copy(score = 42)))
        assertEquals(listOf(42), client.list(ona).map { it.trip.score })
    }

    @Test
    fun `drivers only see their own trips`() = api { client ->
        client.upload(ona, history[0])
        assertTrue(client.list(jonas).isEmpty())
    }

    @Test
    fun `trips need an account, and broken trips are refused`() = api { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/trips").status)
        val wrongId = client.upload(ona, history[0], id = "other-id")
        assertEquals(HttpStatusCode.BadRequest, wrongId.status)
        assertEquals(ApiError.INVALID_INPUT, wrongId.body<ApiError>().code)
        assertEquals(HttpStatusCode.BadRequest, client.upload(ona, history[0].copy(trip = history[0].trip.copy(score = 101))).status)
    }
}
