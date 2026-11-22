package com.example.gpstracker.data.remote

import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.SignInRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.net.ConnectException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiClientTest {
    private fun client(engine: MockEngine) = ApiClient(ApiClient.httpClient(engine), "http://server.test/")

    private val request = SignInRequest("a@b.c", "abcd1234")

    @Test
    fun `calls the right address with the token`() = runTest {
        var seen = ""
        val engine = MockEngine { call ->
            seen = "${call.method.value} ${call.url} ${call.headers["Authorization"]}"
            respond("""{"id":"1","name":"A","email":"a@b.c"}""", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val result = client(engine).me("t0k3n")
        assertEquals("GET http://server.test/api/me Bearer t0k3n", seen)
        assertEquals("A", (result as ApiResult.Success).value.name)
    }

    @Test
    fun `an error answer keeps the server's code`() = runTest {
        val engine = MockEngine {
            respond(
                """{"code":"invalid_credentials","message":"no"}""",
                HttpStatusCode.Unauthorized,
                headersOf("Content-Type", "application/json"),
            )
        }
        assertEquals(ApiResult.Failure(401, ApiError(ApiError.INVALID_CREDENTIALS, "no")), client(engine).signIn(request))
    }

    @Test
    fun `an answer that is not JSON still becomes an error`() = runTest {
        val engine = MockEngine { respond("<html>Bad gateway</html>", HttpStatusCode.BadGateway) }
        val result = client(engine).signIn(request) as ApiResult.Failure
        assertEquals(502, result.status)
        assertEquals(ApiError.SERVER_ERROR, result.error.code)
    }

    @Test
    fun `no server means unreachable, not a crash`() = runTest {
        val engine = MockEngine { throw ConnectException("Connection refused") }
        assertEquals(ApiResult.Unreachable, client(engine).signIn(request))
    }
}
