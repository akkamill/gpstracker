package com.example.gpstracker.data.auth

import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gpstracker.testing.FakePreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreSessionStoreTest {
    private val file = FakePreferencesStore()
    private val store = DataStoreSessionStore(file)

    @Test
    fun `nothing saved means signed out`() = runTest {
        assertEquals(SavedSession.None, store.load())
    }

    @Test
    fun `each kind of session is read back`() = runTest {
        listOf(SavedSession.Account("demo@drivemetrics.app"), SavedSession.Guest, SavedSession.None).forEach {
            store.save(it)
            assertEquals(it, store.load())
        }
    }

    @Test
    fun `signing out removes the email from the file`() = runTest {
        store.save(SavedSession.Account("demo@drivemetrics.app"))
        store.save(SavedSession.Guest)
        assertTrue(file.state.value[stringPreferencesKey("email")] == null)
    }
}
