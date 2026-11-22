package com.example.gpstracker.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.gpstracker.domain.auth.PasswordHasher
import com.example.gpstracker.model.api.AccountInfo
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.AuthResponse
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import com.example.gpstracker.server.ApiException
import com.example.gpstracker.server.accounts.Account
import com.example.gpstracker.server.accounts.AccountRepository
import com.example.gpstracker.tracing.Tracing
import io.ktor.http.HttpStatusCode
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Makes and checks login tokens (JWT, signed with the server's secret). */
class Tokens(secret: String, private val lifetimeMs: Long, private val clock: () -> Long = System::currentTimeMillis) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(ISSUER).withAudience(AUDIENCE).build()

    /** A token for [accountId] and when it expires. */
    fun issue(accountId: UUID): Pair<String, Long> {
        val expiresAt = clock() + lifetimeMs
        val token = JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(accountId.toString())
            .withIssuedAt(Date(clock()))
            .withExpiresAt(Date(expiresAt))
            .sign(algorithm)
        return token to expiresAt
    }

    companion object {
        const val ISSUER = "drivemetrics-server"
        const val AUDIENCE = "drivemetrics-app"
    }
}

/** The account shown in the README for trying the app; created when SEED_DEMO_ACCOUNT=true. */
object DemoAccount {
    const val NAME = "Demo Driver"
    const val EMAIL = "demo@drivemetrics.app"
    const val PASSWORD = "demo1234"
}

/**
 * Sign-up and sign-in. Emails are compared in lower case. Logs never contain emails, names,
 * passwords or tokens.
 */
class AuthService(
    private val accounts: AccountRepository,
    private val tokens: Tokens,
    private val hasher: PasswordHasher = PasswordHasher(),
) {
    private val tracer = Tracing.tracer("server")

    suspend fun signUp(request: SignUpRequest): AuthResponse {
        val name = request.name.trim()
        val email = normalize(request.email)
        validate(name.length in 1..MAX_NAME, "Enter your name (at most $MAX_NAME characters).")
        validate(EMAIL.matches(email) && email.length <= MAX_EMAIL, "Enter a valid email address.")
        validate(isStrong(request.password), "Use at least $MIN_PASSWORD characters with a letter and a number.")

        val account = newAccount(name, email, request.password)
        if (!accounts.insert(account)) {
            tracer.warn("sign up refused: email already registered")
            throw ApiException(HttpStatusCode.Conflict, ApiError(ApiError.EMAIL_TAKEN, "An account with this email already exists."))
        }
        tracer.info("account created", "account" to account.id)
        return response(account)
    }

    /** Creates the demo account unless it exists. Returns true when it was created. */
    suspend fun addDemoAccount(): Boolean {
        if (accounts.findByEmail(DemoAccount.EMAIL) != null) return false
        val created = accounts.insert(newAccount(DemoAccount.NAME, DemoAccount.EMAIL, DemoAccount.PASSWORD))
        if (created) tracer.info("demo account created")
        return created
    }

    suspend fun signIn(request: SignInRequest): AuthResponse {
        val account = accounts.findByEmail(normalize(request.email))
        // an unknown email takes as long as a wrong password, so timing does not reveal who has an account
        val matches = withContext(Dispatchers.Default) {
            hasher.matches(request.password, account?.salt ?: UNKNOWN_SALT, account?.passwordHash.orEmpty())
        }
        if (account == null || !matches) {
            tracer.warn("sign in refused: email or password is wrong")
            throw ApiException(HttpStatusCode.Unauthorized, ApiError(ApiError.INVALID_CREDENTIALS, "Email or password is wrong."))
        }
        tracer.info("signed in", "account" to account.id)
        return response(account)
    }

    suspend fun account(id: UUID): AccountInfo =
        accounts.findById(id)?.info() ?: throw ApiException(HttpStatusCode.NotFound, ApiError(ApiError.NOT_FOUND, "Account not found."))

    private fun response(account: Account): AuthResponse {
        val (token, expiresAt) = tokens.issue(account.id)
        return AuthResponse(token, expiresAt, account.info())
    }

    private suspend fun newAccount(name: String, email: String, password: String): Account {
        val salt = hasher.newSalt()
        val hash = withContext(Dispatchers.Default) { hasher.hash(password, salt) }
        return Account(UUID.randomUUID(), email, name, hash, salt)
    }

    /** The same rule the app checks before sending: long enough, with a letter and a number. */
    private fun isStrong(password: String) =
        password.length in MIN_PASSWORD..MAX_PASSWORD && password.any { it.isLetter() } && password.any { it.isDigit() }

    private fun Account.info() = AccountInfo(id.toString(), name, email)

    private fun validate(ok: Boolean, message: String) {
        if (!ok) throw ApiException(HttpStatusCode.BadRequest, ApiError(ApiError.INVALID_INPUT, message))
    }

    private fun normalize(email: String) = email.trim().lowercase()

    private companion object {
        const val MAX_NAME = 80
        const val MAX_EMAIL = 254
        const val MIN_PASSWORD = 8
        const val MAX_PASSWORD = 128
        const val UNKNOWN_SALT = "00000000000000000000000000000000"
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
