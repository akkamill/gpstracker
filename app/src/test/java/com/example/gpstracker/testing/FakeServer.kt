package com.example.gpstracker.testing

import com.example.gpstracker.data.remote.ApiClient
import com.example.gpstracker.model.api.AccountInfo
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.AuthResponse
import com.example.gpstracker.model.api.NewReportRequest
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import com.example.gpstracker.model.api.VoteRequest
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.model.trip.SavedTrip
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.net.ConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

/**
 * A stand-in for the project's server, answering like the real one (same JSON classes from
 * `:model`), so the app's account and report code can be tested without a network.
 */
class FakeServer {
    private class Stored(val id: String, val name: String, val password: String)

    private val accounts = HashMap<String, Stored>()

    /** False: every call fails as if the server were not running. */
    var reachable = true

    /** False: the server no longer accepts tokens it gave out (for example after a reset). */
    var acceptsTokens = true

    val calls = mutableListOf<String>()

    /** Shared road reports the server holds, and who made each (by email). */
    val reports = LinkedHashMap<String, RoadReport>()
    private val reporters = HashMap<String, String>()

    /** Votes received, as "id:stillThere". */
    val votes = mutableListOf<String>()

    /** Each account's trips, by email. */
    val trips = HashMap<String, LinkedHashMap<String, SavedTrip>>()

    // answers on the calling thread, so tests running in virtual time see every call finish
    private val engine = MockEngine(
        MockEngineConfig().apply {
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                handle(
                    request.method,
                    request.url.encodedPath,
                    String(request.body.toByteArray()),
                    request.headers[HttpHeaders.Authorization],
                )
            }
        },
    )

    val api = ApiClient(ApiClient.httpClient(engine), "http://server.test/")

    fun addAccount(name: String, email: String, password: String) {
        accounts[email] = Stored("id-${accounts.size + 1}", name, password)
    }

    fun rename(email: String, name: String) {
        accounts[email] = accounts.getValue(email).let { Stored(it.id, name, it.password) }
    }

    private fun MockRequestHandleScope.handle(method: HttpMethod, path: String, body: String, auth: String?): HttpResponseData {
        if (!reachable) throw ConnectException("Connection refused")
        calls += path
        val email = auth?.removePrefix("Bearer token-")
        return when {
            path.startsWith("/api/auth/") || path == "/api/me" -> accounts(path, body, email)

            path == "/api/reports" && method == HttpMethod.Get -> ok(reports.values.map { it.seenBy(email) })

            path == "/api/reports" -> share(body, email)

            path == "/api/trips" || path.startsWith("/api/trips/") -> trips(method, body, email)

            path.startsWith(
                "/api/reports/",
            ) && path.endsWith("/vote") -> vote(path.removePrefix("/api/reports/").removeSuffix("/vote"), body)

            else -> error(HttpStatusCode.NotFound, ApiError.NOT_FOUND)
        }
    }

    private fun MockRequestHandleScope.accounts(path: String, body: String, tokenEmail: String?): HttpResponseData = when (path) {
        "/api/auth/sign-up" -> {
            val request = Json.decodeFromString<SignUpRequest>(body)
            val email = request.email.lowercase()
            if (email in accounts) {
                error(HttpStatusCode.Conflict, ApiError.EMAIL_TAKEN)
            } else {
                addAccount(request.name, email, request.password)
                ok(auth(email), HttpStatusCode.Created)
            }
        }

        "/api/auth/sign-in" -> {
            val request = Json.decodeFromString<SignInRequest>(body)
            val email = request.email.lowercase()
            val matches = accounts[email]?.password == request.password
            if (matches) ok(auth(email)) else error(HttpStatusCode.Unauthorized, ApiError.INVALID_CREDENTIALS)
        }

        else -> if (tokenEmail !in accounts ||
            !acceptsTokens
        ) {
            error(HttpStatusCode.Unauthorized, ApiError.UNAUTHORIZED)
        } else {
            ok(info(tokenEmail!!))
        }
    }

    private fun MockRequestHandleScope.share(body: String, email: String?): HttpResponseData {
        if (email == null || email !in accounts) return error(HttpStatusCode.Unauthorized, ApiError.UNAUTHORIZED)
        val request = Json.decodeFromString<NewReportRequest>(body)
        val report = reports.getOrPut(request.id) {
            reporters[request.id] = email
            RoadReport(
                request.id,
                request.type,
                request.position,
                0,
                Long.MAX_VALUE,
                ReportSource.OtherDriver,
                request.headingDeg,
                request.roadName,
            )
        }
        return ok(report.seenBy(email), HttpStatusCode.Created)
    }

    private fun MockRequestHandleScope.vote(id: String, body: String): HttpResponseData {
        val report = reports[id] ?: return error(HttpStatusCode.NotFound, ApiError.NOT_FOUND)
        val stillThere = Json.decodeFromString<VoteRequest>(body).stillThere
        votes += "$id:$stillThere"
        val voted = if (stillThere) {
            report.copy(stillThereVotes = report.stillThereVotes + 1)
        } else {
            report.copy(
                goneVotes =
                    report.goneVotes + 1,
            )
        }
        reports[id] = voted
        return ok(voted)
    }

    private fun MockRequestHandleScope.trips(method: HttpMethod, body: String, email: String?): HttpResponseData {
        if (email == null || email !in accounts) return error(HttpStatusCode.Unauthorized, ApiError.UNAUTHORIZED)
        val mine = trips.getOrPut(email) { LinkedHashMap() }
        if (method == HttpMethod.Get) return ok(mine.values.sortedByDescending { it.savedAtMs })
        val trip = Json.decodeFromString<SavedTrip>(body)
        mine[trip.id] = trip
        return ok(trip)
    }

    private fun RoadReport.seenBy(email: String?) = copy(
        source = if (email != null &&
            reporters[id] == email
        ) {
            ReportSource.Me
        } else {
            ReportSource.OtherDriver
        },
    )

    private fun info(email: String) = accounts.getValue(email).let { AccountInfo(it.id, it.name, email) }

    private fun auth(email: String) = AuthResponse("token-$email", expiresAtMs = Long.MAX_VALUE, account = info(email))

    private inline fun <reified T> MockRequestHandleScope.ok(value: T, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(Json.encodeToString(value), status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(status: HttpStatusCode, code: String) = ok(ApiError(code, "refused"), status)
}
