package com.example.gpstracker.testing

import com.example.gpstracker.data.remote.ApiClient
import com.example.gpstracker.model.api.AccountInfo
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.AuthResponse
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.net.ConnectException
import kotlinx.serialization.json.Json

/**
 * A stand-in for the project's server, answering like the real one (same JSON classes from
 * `:model`), so the app's account code can be tested without a network.
 */
class FakeServer {
    private class Stored(val id: String, val name: String, val password: String)

    private val accounts = HashMap<String, Stored>()

    /** False: every call fails as if the server were not running. */
    var reachable = true

    /** False: the server no longer accepts tokens it gave out (for example after a reset). */
    var acceptsTokens = true

    val calls = mutableListOf<String>()

    private val engine = MockEngine { request ->
        handle(request.url.encodedPath, String(request.body.toByteArray()), request.headers[HttpHeaders.Authorization])
    }

    val api = ApiClient(ApiClient.httpClient(engine), "http://server.test/")

    fun addAccount(name: String, email: String, password: String) {
        accounts[email] = Stored("id-${accounts.size + 1}", name, password)
    }

    fun rename(email: String, name: String) {
        accounts[email] = accounts.getValue(email).let { Stored(it.id, name, it.password) }
    }

    private fun MockRequestHandleScope.handle(path: String, body: String, auth: String?): HttpResponseData {
        if (!reachable) throw ConnectException("Connection refused")
        calls += path
        return when (path) {
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

            "/api/me" -> {
                val email = auth?.removePrefix("Bearer token-")
                val account = accounts[email]
                if (account == null || !acceptsTokens) error(HttpStatusCode.Unauthorized, ApiError.UNAUTHORIZED) else ok(info(email!!))
            }

            else -> error(HttpStatusCode.NotFound, ApiError.NOT_FOUND)
        }
    }

    private fun info(email: String) = accounts.getValue(email).let { AccountInfo(it.id, it.name, email) }

    private fun auth(email: String) = AuthResponse("token-$email", expiresAtMs = Long.MAX_VALUE, account = info(email))

    private inline fun <reified T> MockRequestHandleScope.ok(value: T, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(Json.encodeToString(value), status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(status: HttpStatusCode, code: String) = ok(ApiError(code, "refused"), status)
}
