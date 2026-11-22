package com.example.gpstracker.data.auth

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gpstracker.testing.FakePreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreSessionStoreTest {
    private val file = FakePreferencesStore()
    private val store = DataStoreSessionStore(file)
    private val signedIn = SavedSession.SignedIn("id-1", "Ona", "ona@example.com", "secret-token", 99)

    @Test
    fun `nothing saved means signed out`() = runTest {
        assertEquals(SavedSession.None, store.load())
    }

    @Test
    fun `each kind of session is read back`() = runTest {
        listOf(signedIn, SavedSession.Guest, SavedSession.None).forEach {
            store.save(it)
            assertEquals(it, store.load())
        }
    }

    @Test
    fun `signing out removes the account and the token from the file`() = runTest {
        store.save(signedIn)
        store.save(SavedSession.Guest)
        assertTrue(file.state.value.asMap().keys.map { it.name } == listOf("kind"))
    }

    @Test
    fun `a session of the old phone-only accounts opens signed out`() = runTest {
        file.state.value = mutablePreferencesOf(stringPreferencesKey("kind") to "account", stringPreferencesKey("email") to "a@b.c")
        assertEquals(SavedSession.None, store.load())
    }

    @Test
    fun `the token is never printed`() {
        assertTrue("secret-token" !in signedIn.toString() && "ona@" !in signedIn.toString())
    }
}
