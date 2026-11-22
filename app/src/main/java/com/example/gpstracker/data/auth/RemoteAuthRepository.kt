package com.example.gpstracker.data.auth

import com.example.gpstracker.data.remote.ApiClient
import com.example.gpstracker.data.remote.ApiResult
import com.example.gpstracker.data.support.TokenProvider
import com.example.gpstracker.model.api.AuthResponse
import com.example.gpstracker.model.api.SignInRequest
import com.example.gpstracker.model.api.SignUpRequest
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Accounts on the project's server.
 *
 * Signing in or up asks the server, which answers with a token. The token is saved with the
 * account, so the app opens signed in after a restart, also without network. Once started, the
 * app asks the server whether the token is still good; if the server says no, the user is
 * signed out. Demo mode (guest) never needs the server.
 *
 * Logs never contain emails, names, passwords or tokens.
 */
class RemoteAuthRepository(
    private val api: ApiClient,
    private val sessions: SessionStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository, TokenProvider {
    private val tracer = Tracing.tracer("auth")

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    /** The token to send with requests that need a signed-in account, or null. */
    @Volatile
    var token: String? = null
        private set

    override fun currentToken(): String? = token

    init {
        scope.launch {
            val saved = restore()
            if (saved != null) checkWithServer(saved)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        finish(api.signIn(SignInRequest(email.trim(), password)), "signed in")

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> =
        finish(api.signUp(SignUpRequest(name.trim(), email.trim(), password)), "account created")

    override fun continueAsGuest() {
        tracer.info("demo started as guest")
        token = null
        _session.value = SessionState.Guest
        scope.launch { sessions.save(SavedSession.Guest) }
    }

    override fun signOut() {
        tracer.info("signed out")
        token = null
        _session.value = SessionState.SignedOut
        scope.launch { sessions.save(SavedSession.None) }
    }

    private suspend fun finish(result: ApiResult<AuthResponse>, done: String): Result<Unit> = when (result) {
        is ApiResult.Success -> {
            val auth = result.value
            val saved = SavedSession.SignedIn(auth.account.id, auth.account.name, auth.account.email, auth.token, auth.expiresAtMs)
            sessions.save(saved)
            token = auth.token
            _session.value = SessionState.SignedIn(saved.name, saved.email)
            tracer.info(done)
            Result.success(Unit)
        }

        is ApiResult.Failure -> {
            val failure = when (result.status) {
                HTTP_UNAUTHORIZED -> AuthFailure.InvalidCredentials
                HTTP_CONFLICT -> AuthFailure.EmailTaken
                else -> AuthFailure.ServerError
            }
            tracer.warn("server refused", "status" to result.status, "code" to result.error.code)
            Result.failure(AuthException(failure))
        }

        ApiResult.Unreachable -> {
            tracer.warn("server unreachable")
            Result.failure(AuthException(AuthFailure.ServerUnreachable))
        }
    }

    /** Opens the app as it was left. Returns the saved account, if any, to be checked. */
    private suspend fun restore(): SavedSession.SignedIn? {
        val saved = sessions.load()
        val state = when {
            saved == SavedSession.Guest -> SessionState.Guest
            saved is SavedSession.SignedIn && saved.expiresAtMs > clock() -> SessionState.SignedIn(saved.name, saved.email)
            else -> SessionState.SignedOut
        }
        if (saved is SavedSession.SignedIn && state is SessionState.SignedIn) token = saved.token
        if (saved is SavedSession.SignedIn && state == SessionState.SignedOut) sessions.save(SavedSession.None)
        _session.value = state
        tracer.info("session restored", "state" to state.javaClass.simpleName)
        return saved.takeIf { state is SessionState.SignedIn } as? SavedSession.SignedIn
    }

    private suspend fun checkWithServer(saved: SavedSession.SignedIn) {
        when (val result = api.me(saved.token)) {
            is ApiResult.Success -> {
                val account = result.value
                if (account.name != saved.name && _session.value is SessionState.SignedIn) {
                    sessions.save(saved.copy(name = account.name))
                    _session.value = SessionState.SignedIn(account.name, account.email)
                }
            }

            is ApiResult.Failure -> if (result.status == HTTP_UNAUTHORIZED && token == saved.token) {
                tracer.warn("session no longer accepted by the server, signing out")
                signOut()
            }

            // offline: stay signed in, the server checks the token again on the next request
            ApiResult.Unreachable -> Unit
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
    }
}
