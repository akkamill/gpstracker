package com.example.gpstracker.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/** Who used the app last, so they do not have to sign in again after a restart. */
sealed interface SavedSession {
    data object None : SavedSession

    data object Guest : SavedSession

    /** A server account and the token the server gave for it. */
    data class SignedIn(
        val accountId: String,
        val name: String,
        val email: String,
        val token: String,
        val expiresAtMs: Long,
    ) : SavedSession {
        override fun toString() = "SignedIn(accountId=$accountId, name=***, email=***, token=***, expiresAtMs=$expiresAtMs)"
    }
}

interface SessionStore {
    suspend fun load(): SavedSession

    suspend fun save(session: SavedSession)
}

/** Keeps the session in memory only, for tests. */
class InMemorySessionStore(private var session: SavedSession = SavedSession.None) : SessionStore {
    override suspend fun load(): SavedSession = session

    override suspend fun save(session: SavedSession) {
        this.session = session
    }
}

/**
 * Keeps the session in a preferences file of its own, in the app's private storage, never a
 * password. The file is left out of backups (see res/xml), so the token stays on this phone.
 */
class DataStoreSessionStore(private val store: DataStore<Preferences>) : SessionStore {
    private val kind = stringPreferencesKey("kind")
    private val accountId = stringPreferencesKey("account_id")
    private val name = stringPreferencesKey("name")
    private val email = stringPreferencesKey("email")
    private val token = stringPreferencesKey("token")
    private val expiresAt = longPreferencesKey("expires_at_ms")

    override suspend fun load(): SavedSession {
        val prefs = store.data.first()
        return when (prefs[kind]) {
            KIND_GUEST -> SavedSession.Guest

            KIND_SIGNED_IN -> signedIn(prefs) ?: SavedSession.None

            // includes sessions of the old phone-only accounts, which have no token
            else -> SavedSession.None
        }
    }

    /** Null when any part is missing, for example in a file written by an older version. */
    private fun signedIn(prefs: Preferences): SavedSession.SignedIn? {
        val expires = prefs[expiresAt] ?: return null
        val parts = listOf(accountId, name, email, token).map { prefs[it] ?: return null }
        return SavedSession.SignedIn(accountId = parts[0], name = parts[1], email = parts[2], token = parts[3], expiresAtMs = expires)
    }

    override suspend fun save(session: SavedSession) {
        store.edit { prefs ->
            prefs.clear()
            when (session) {
                SavedSession.None -> Unit

                SavedSession.Guest -> prefs[kind] = KIND_GUEST

                is SavedSession.SignedIn -> {
                    prefs[kind] = KIND_SIGNED_IN
                    prefs[accountId] = session.accountId
                    prefs[name] = session.name
                    prefs[email] = session.email
                    prefs[token] = session.token
                    prefs[expiresAt] = session.expiresAtMs
                }
            }
        }
    }

    private companion object {
        const val KIND_GUEST = "guest"
        const val KIND_SIGNED_IN = "server_account"
    }
}
