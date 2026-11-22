package com.example.gpstracker.server

import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.HealthResponse
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.reports.ReportService
import com.example.gpstracker.server.reports.roadReports
import com.example.gpstracker.server.support.SupportHub
import com.example.gpstracker.server.support.agentChat
import com.example.gpstracker.server.support.driverChat
import com.example.gpstracker.tracing.Tracing
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

/** An error that becomes a JSON [ApiError] response with [status]. */
class ApiException(val status: HttpStatusCode, val error: ApiError) : RuntimeException(error.code)

/** What the HTTP layer needs; [main] builds it with PostgreSQL, tests with fakes. */
class ServerDependencies(
    val auth: AuthService,
    val tokens: Tokens,
    val databaseUp: () -> Boolean,
    val support: SupportHub,
    val reports: ReportService,
    /** The support team's key for the chat page; null turns the page's chat off. */
    val agentKey: String?,
)

/**
 * The server's endpoints:
 *
 * - `GET  /health`              is the server up, and can it reach the database?
 * - `POST /api/auth/sign-up`    create an account, returns a token
 * - `POST /api/auth/sign-in`    returns a token
 * - `GET  /api/me`              the signed-in account (needs "Authorization: Bearer <token>")
 * - `WS   /api/support/chat`    a driver's support chat (needs the token)
 * - `WS   /api/support/agent`   the support team's side of all chats (needs the team's key)
 * - `/api/reports`              shared road reports (see ReportRoutes)
 * - `GET  /support`             the support team's web page
 */
fun Application.driveMetrics(deps: ServerDependencies) {
    val tracer = Tracing.tracer("server")

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(StatusPages) {
        exception<ApiException> { call, e -> call.respond(e.status, e.error) }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError(ApiError.INVALID_INPUT, "The request could not be read."))
        }
        exception<Throwable> { call, e ->
            tracer.error("request failed", e, "path" to call.request.local.uri)
            call.respond(HttpStatusCode.InternalServerError, ApiError(ApiError.SERVER_ERROR, "Something went wrong on the server."))
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ApiError(ApiError.UNAUTHORIZED, "Sign in again."))
        }
    }
    install(WebSockets) {
        pingPeriod = PING_PERIOD
        timeout = PING_TIMEOUT
        maxFrameSize = MAX_FRAME_BYTES
    }
    install(Authentication) {
        jwt(TOKEN_AUTH) {
            verifier(deps.tokens.verifier)
            validate { credential -> credential.payload.subject?.let { JWTPrincipal(credential.payload) } }
        }
    }

    routing {
        get("/health") {
            val db = deps.databaseUp()
            call.respond(
                if (db) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(status = if (db) "ok" else "degraded", database = if (db) "up" else "down"),
            )
        }
        route("/api") {
            post("/auth/sign-up") { call.respond(HttpStatusCode.Created, deps.auth.signUp(call.receive<SignUpRequest>())) }
            post("/auth/sign-in") { call.respond(deps.auth.signIn(call.receive<SignInRequest>())) }
            authenticate(TOKEN_AUTH) {
                get("/me") {
                    val id = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
                    call.respond(deps.auth.account(id))
                }
                driverChat(deps.support)
            }
            agentChat(deps.support, deps.agentKey)
            roadReports(deps.reports, TOKEN_AUTH)
        }
        staticResources("/support", "support")
    }
}

private const val TOKEN_AUTH = "token"
private val PING_PERIOD = 15.seconds
private val PING_TIMEOUT = 30.seconds
private const val MAX_FRAME_BYTES = 64L * 1024
