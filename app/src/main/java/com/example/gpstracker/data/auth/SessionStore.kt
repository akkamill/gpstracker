package com.example.gpstracker.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/** Who used the app last, so they do not have to sign in again after a restart. */
sealed interface SavedSession {
    data object None : SavedSession

    data object Guest : SavedSession

    data class Account(val email: String) : SavedSession
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

/** Keeps the session in a preferences file of its own. Only the email is stored, never a password. */
class DataStoreSessionStore(private val store: DataStore<Preferences>) : SessionStore {
    private val kind = stringPreferencesKey("kind")
    private val email = stringPreferencesKey("email")

    override suspend fun load(): SavedSession {
        val prefs = store.data.first()
        return when (prefs[kind]) {
            KIND_GUEST -> SavedSession.Guest
            KIND_ACCOUNT -> prefs[email]?.let { SavedSession.Account(it) } ?: SavedSession.None
            else -> SavedSession.None
        }
    }

    override suspend fun save(session: SavedSession) {
        store.edit { prefs ->
            prefs.clear()
            when (session) {
                SavedSession.None -> Unit

                SavedSession.Guest -> prefs[kind] = KIND_GUEST

                is SavedSession.Account -> {
                    prefs[kind] = KIND_ACCOUNT
                    prefs[email] = session.email
                }
            }
        }
    }

    private companion object {
        const val KIND_GUEST = "guest"
        const val KIND_ACCOUNT = "account"
    }
}
