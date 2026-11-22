package com.example.gpstracker.data.auth

import com.example.gpstracker.data.local.AccountDao
import com.example.gpstracker.data.local.AccountEntity
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accounts kept on this phone, until the project's server takes over sign-in.
 *
 * Passwords are only kept as salted hashes. The session is saved, so the app opens signed in
 * (or as a guest) after a restart. Logs never contain emails, names or passwords.
 *
 * @param worker where hashing runs; it is slow on purpose, so it stays off the main thread
 */
class LocalAuthRepository(
    private val accounts: AccountDao,
    private val sessions: SessionStore,
    private val scope: CoroutineScope,
    private val hasher: PasswordHasher = PasswordHasher(),
    private val worker: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {
    private val tracer = Tracing.tracer("auth")

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    init {
        scope.launch(worker) {
            addDemoAccountIfMissing()
            _session.value = restore()
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = withContext(worker) {
        val key = normalize(email)
        val account = accounts.find(key)
        // a missing account takes as long as a wrong password, so timing does not tell which emails exist
        val matches = hasher.matches(password, account?.salt ?: UNKNOWN_SALT, account?.passwordHash.orEmpty())
        if (account == null || !matches) {
            tracer.warn("sign in failed: email or password is wrong")
            return@withContext Result.failure(AuthException(AuthFailure.InvalidCredentials))
        }
        sessions.save(SavedSession.Account(key))
        _session.value = SessionState.SignedIn(account.name, key)
        tracer.info("signed in")
        Result.success(Unit)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> = withContext(worker) {
        val key = normalize(email)
        if (!accounts.tryInsert(newAccount(name.trim(), key, password))) {
            tracer.warn("sign up failed: email already registered")
            return@withContext Result.failure(AuthException(AuthFailure.EmailTaken))
        }
        sessions.save(SavedSession.Account(key))
        _session.value = SessionState.SignedIn(name.trim(), key)
        tracer.info("account created")
        Result.success(Unit)
    }

    override fun continueAsGuest() {
        tracer.info("demo started as guest")
        _session.value = SessionState.Guest
        scope.launch { sessions.save(SavedSession.Guest) }
    }

    override fun signOut() {
        tracer.info("signed out")
        _session.value = SessionState.SignedOut
        scope.launch { sessions.save(SavedSession.None) }
    }

    private suspend fun restore(): SessionState {
        val state = when (val saved = sessions.load()) {
            SavedSession.None -> SessionState.SignedOut

            SavedSession.Guest -> SessionState.Guest

            is SavedSession.Account -> accounts.find(saved.email)?.let { SessionState.SignedIn(it.name, it.email) }
                ?: SessionState.SignedOut
        }
        tracer.info("session restored", "state" to state.javaClass.simpleName)
        return state
    }

    private suspend fun addDemoAccountIfMissing() {
        if (accounts.find(DEMO_EMAIL) == null) accounts.tryInsert(newAccount(DEMO_NAME, DEMO_EMAIL, DEMO_PASSWORD))
    }

    private fun newAccount(name: String, email: String, password: String): AccountEntity {
        val salt = hasher.newSalt()
        return AccountEntity(email = email, name = name, passwordHash = hasher.hash(password, salt), salt = salt, createdAtMs = clock())
    }

    private suspend fun AccountDao.tryInsert(account: AccountEntity): Boolean = insert(account) != NOT_INSERTED

    private fun normalize(email: String) = email.trim().lowercase()

    companion object {
        const val DEMO_EMAIL = "demo@drivemetrics.app"
        const val DEMO_PASSWORD = "demo1234"
        private const val DEMO_NAME = "Demo Driver"
        private const val UNKNOWN_SALT = "00000000000000000000000000000000"
        private const val NOT_INSERTED = -1L
    }
}
