package com.example.gpstracker.server.reports

import com.example.gpstracker.domain.auth.PasswordHasher
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.NewReportRequest
import com.example.gpstracker.model.api.VoteRequest
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.server.ServerDependencies
import com.example.gpstracker.server.accounts.InMemoryAccountRepository
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.driveMetrics
import com.example.gpstracker.server.support.InMemorySupportRepository
import com.example.gpstracker.server.support.SupportHub
import com.example.gpstracker.server.trips.InMemoryTripStore
import com.example.gpstracker.simulator.vehicle.CommunityReports
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sharing road reports over HTTP, with storage in memory and a clock the test moves. */
class ReportApiTest {
    private var now = 1_700_000_000_000L
    private val minute = 60_000L
    private val tokens = Tokens(secret = "a".repeat(32), lifetimeMs = Long.MAX_VALUE / 2, clock = { now })
    private val store = InMemoryReportStore()
    private val service = ReportService(store, clock = { now })
    private val ona = UUID.randomUUID()
    private val jonas = UUID.randomUUID()
    private val kaunas = LatLon(54.9, 23.9)

    private fun api(test: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application {
            driveMetrics(
                ServerDependencies(
                    auth = AuthService(InMemoryAccountRepository(), tokens, PasswordHasher(iterations = 2)),
                    tokens = tokens,
                    databaseUp = { true },
                    support = SupportHub(InMemorySupportRepository()),
                    reports = service,
                    trips = InMemoryTripStore(),
                    agentKey = null,
                ),
            )
        }
        test(createClient { install(ContentNegotiation) { json() } })
    }

    private fun token(account: UUID) = tokens.issue(account).first

    private suspend fun HttpClient.report(account: UUID, request: NewReportRequest) = post("/api/reports") {
        bearerAuth(token(account))
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    private suspend fun HttpClient.vote(account: UUID, id: String, stillThere: Boolean) = post("/api/reports/$id/vote") {
        bearerAuth(token(account))
        contentType(ContentType.Application.Json)
        setBody(VoteRequest(stillThere))
    }

    private suspend fun HttpClient.nearby(account: UUID? = null, at: LatLon = kaunas): List<RoadReport> =
        get("/api/reports?lat=${at.latitude}&lon=${at.longitude}") { account?.let { bearerAuth(token(it)) } }.body()

    private val police = NewReportRequest("r1", ReportType.Police, kaunas, headingDeg = 90.0, roadName = "Savanorių pr.")

    @Test
    fun `a report is shared with other drivers, who do not learn who made it`() = api { client ->
        val created = client.report(ona, police)
        assertEquals(HttpStatusCode.Created, created.status)
        val mine = created.body<RoadReport>()
        assertEquals(ReportSource.Me, mine.source)
        assertEquals(now + 30 * minute, mine.expiresAtMs) // police: half an hour, as in the app

        assertEquals(listOf(ReportSource.Me), client.nearby(ona).map { it.source })
        assertEquals(listOf(ReportSource.OtherDriver), client.nearby(jonas).map { it.source })
        // demo mode (no account) sees reports too
        assertEquals(listOf("r1"), client.nearby().map { it.id })
    }

    @Test
    fun `sending the same report again does not make a second one`() = api { client ->
        client.report(ona, police)
        now += minute
        assertEquals(HttpStatusCode.Created, client.report(ona, police).status)
        assertEquals(1, client.nearby().size)
    }

    @Test
    fun `only reports nearby are sent`() = api { client ->
        client.report(ona, police)
        client.report(ona, police.copy(id = "vilnius", position = LatLon(54.69, 25.28)))
        assertEquals(listOf("r1"), client.nearby(at = kaunas).map { it.id })
        assertEquals(listOf("vilnius"), client.nearby(at = LatLon(54.69, 25.28)).map { it.id })
    }

    @Test
    fun `reports expire, and still there keeps them a while longer`() = api { client ->
        client.report(ona, police)
        now += 25 * minute
        val confirmed = client.vote(jonas, "r1", stillThere = true).body<RoadReport>()
        assertEquals(1, confirmed.stillThereVotes)
        assertEquals(now + 15 * minute, confirmed.expiresAtMs)
        now += 10 * minute
        assertEquals(1, client.nearby().size)
        now += 6 * minute
        assertTrue(client.nearby().isEmpty())
    }

    @Test
    fun `each driver counts once, and a report voted away disappears`() = api { client ->
        client.report(ona, police)
        client.vote(jonas, "r1", stillThere = false)
        client.vote(jonas, "r1", stillThere = false)
        assertTrue("one gone vote is enough when nobody confirmed", client.nearby().isEmpty())

        client.report(ona, police.copy(id = "r2"))
        client.vote(ona, "r2", stillThere = true)
        val disputed = client.vote(jonas, "r2", stillThere = false).body<RoadReport>()
        assertEquals(1, disputed.stillThereVotes)
        assertEquals(1, disputed.goneVotes)
        assertEquals(listOf("r2"), client.nearby().map { it.id })
    }

    @Test
    fun `sharing and voting need an account, and bad reports are refused`() = api { client ->
        val anonymous = client.post("/api/reports") {
            contentType(ContentType.Application.Json)
            setBody(police)
        }
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
        assertEquals(HttpStatusCode.NotFound, client.vote(ona, "nope", stillThere = true).status)
        listOf(police.copy(position = LatLon(95.0, 0.0)), police.copy(id = ""), police.copy(headingDeg = 400.0)).forEach {
            val refused = client.report(ona, it)
            assertEquals(HttpStatusCode.BadRequest, refused.status)
            assertEquals(ApiError.INVALID_INPUT, refused.body<ApiError>().code)
        }
    }

    @Test
    fun `demo reports come back when they expired or were voted away`() {
        runBlocking {
            val demo = CommunityReports.demo(now)
            assertEquals(demo.size, service.refreshDemoReports(demo))
            assertEquals(0, service.refreshDemoReports(demo))
            service.vote(demo.first().id, jonas, stillThere = false)
            assertEquals(1, service.refreshDemoReports(demo))
            now = demo.first().expiresAtMs + 1
            assertEquals(demo.size, service.refreshDemoReports(CommunityReports.demo(now)))
        }
    }
}
