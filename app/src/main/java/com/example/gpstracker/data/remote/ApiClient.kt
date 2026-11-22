package com.example.gpstracker.data.remote

import com.example.gpstracker.model.api.AccountInfo
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.AuthResponse
import com.example.gpstracker.model.api.NewReportRequest
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import com.example.gpstracker.model.api.VoteRequest
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.tracing.Tracing
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.json.Json

/** The outcome of one call to the server. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    /** The server answered with an error, such as a wrong password. */
    data class Failure(val status: Int, val error: ApiError) : ApiResult<Nothing>

    /** No answer: no network, the server is not running, or it took too long. */
    data object Unreachable : ApiResult<Nothing>
}

/**
 * Calls the project's server. Every call returns an [ApiResult] instead of throwing, so
 * callers decide what an error means for the user. Request and response classes come from
 * `:model`, the same ones the server uses.
 */
class ApiClient(private val http: HttpClient, baseUrl: String) {
    private val tracer = Tracing.tracer("app")
    private val base = baseUrl.trimEnd('/')

    suspend fun signUp(request: SignUpRequest): ApiResult<AuthResponse> = call {
        http.post("$base/api/auth/sign-up") { json(request) }
    }

    suspend fun signIn(request: SignInRequest): ApiResult<AuthResponse> = call {
        http.post("$base/api/auth/sign-in") { json(request) }
    }

    suspend fun me(token: String): ApiResult<AccountInfo> = call {
        http.get("$base/api/me") { bearerAuth(token) }
    }

    /** Active road reports around [near] (all when null); [token] marks the driver's own ones. */
    suspend fun reports(near: LatLon?, token: String?): ApiResult<List<RoadReport>> = call {
        http.get("$base/api/reports") {
            near?.let {
                parameter("lat", it.latitude)
                parameter("lon", it.longitude)
            }
            token?.let { bearerAuth(it) }
        }
    }

    suspend fun shareReport(request: NewReportRequest, token: String): ApiResult<RoadReport> = call {
        http.post("$base/api/reports") {
            bearerAuth(token)
            json(request)
        }
    }

    suspend fun voteOnReport(id: String, stillThere: Boolean, token: String): ApiResult<RoadReport> = call {
        http.post("$base/api/reports/$id/vote") {
            bearerAuth(token)
            json(VoteRequest(stillThere))
        }
    }

    private inline fun <reified B : Any> io.ktor.client.request.HttpRequestBuilder.json(body: B) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend inline fun <reified T> call(send: () -> HttpResponse): ApiResult<T> {
        val response = try {
            send()
        } catch (e: IOException) {
            // no connection, refused, or timed out: the caller only needs to know there was no answer
            tracer.warn("no answer from the server", "reason" to e.javaClass.simpleName, "detail" to e.message)
            return ApiResult.Unreachable
        }
        if (response.status.isSuccess()) return ApiResult.Success(response.body())
        val error = runCatching { response.body<ApiError>() }
            .getOrDefault(ApiError(ApiError.SERVER_ERROR, "HTTP ${response.status.value}"))
        return ApiResult.Failure(response.status.value, error)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val REQUEST_TIMEOUT_MS = 15_000L
        private const val PING_INTERVAL_MS = 20_000L

        /** The HTTP client the app uses; tests pass a mock [engine] instead. */
        fun httpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            // the support chat; pings keep the connection alive and notice when it is gone
            install(WebSockets) { pingIntervalMillis = PING_INTERVAL_MS }
        }
    }
}
