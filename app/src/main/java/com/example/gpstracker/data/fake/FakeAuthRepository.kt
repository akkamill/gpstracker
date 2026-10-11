package com.example.gpstracker.data.fake

import com.example.gpstracker.data.auth.AuthException
import com.example.gpstracker.data.auth.AuthFailure
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * In-memory accounts for the demo. Passwords are never stored or logged in plain
 * text; only a SHA-256 hash is kept. A real implementation (Firebase Authentication
 * or the project's own backend) replaces this entirely.
 */
class FakeAuthRepository(private val latencyMs: Long = LATENCY_MS) : AuthRepository {
    private class Account(val name: String, val passwordHash: String)

    private val accounts = HashMap<String, Account>().apply {
        put(DEMO_EMAIL, Account("Demo Driver", hash(DEMO_PASSWORD)))
    }

    private val _session = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        delay(latencyMs)
        val key = email.trim().lowercase()
        val account = synchronized(accounts) { accounts[key] }
        if (account == null || account.passwordHash != hash(password)) {
            return Result.failure(AuthException(AuthFailure.InvalidCredentials))
        }
        _session.value = SessionState.SignedIn(account.name, key)
        return Result.success(Unit)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        delay(latencyMs)
        val key = email.trim().lowercase()
        synchronized(accounts) {
            if (accounts.containsKey(key)) {
                return Result.failure(AuthException(AuthFailure.EmailTaken))
            }
            accounts[key] = Account(name.trim(), hash(password))
        }
        _session.value = SessionState.SignedIn(name.trim(), key)
        return Result.success(Unit)
    }

    override fun continueAsGuest() {
        _session.value = SessionState.Guest
    }

    override fun signOut() {
        _session.value = SessionState.SignedOut
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val DEMO_EMAIL = "demo@drivemetrics.app"
        const val DEMO_PASSWORD = "demo1234"
        private const val LATENCY_MS = 800L
    }
}
