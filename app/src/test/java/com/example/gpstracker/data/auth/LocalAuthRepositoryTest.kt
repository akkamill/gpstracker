package com.example.gpstracker.data.auth

import com.example.gpstracker.domain.auth.PasswordHasher
import com.example.gpstracker.testing.FakeAccountDao
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAuthRepositoryTest {
    private val accounts = FakeAccountDao()
    private val sessions = InMemorySessionStore()

    /** A fresh repository on the same storage, as after an app restart. Hashing is cheap in tests. */
    private fun TestScope.open() = LocalAuthRepository(
        accounts = accounts,
        sessions = sessions,
        scope = backgroundScope,
        hasher = PasswordHasher(iterations = 2),
        worker = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun `starts signed out, with the demo account ready`() = runTest {
        val repo = open()
        assertEquals(SessionState.SignedOut, repo.session.value)
        assertTrue(LocalAuthRepository.DEMO_EMAIL in accounts.rows)
    }

    @Test
    fun `demo account signs in, whatever the case of the email`() = runTest {
        val repo = open()
        assertTrue(repo.signIn("DEMO@drivemetrics.app ", LocalAuthRepository.DEMO_PASSWORD).isSuccess)
        assertEquals(SessionState.SignedIn("Demo Driver", "demo@drivemetrics.app"), repo.session.value)
    }

    @Test
    fun `a wrong password or an unknown email is rejected the same way`() = runTest {
        val repo = open()
        listOf("demo@drivemetrics.app" to "wrong", "nobody@example.com" to "demo1234").forEach { (email, password) ->
            val result = repo.signIn(email, password)
            assertEquals(AuthFailure.InvalidCredentials, (result.exceptionOrNull() as AuthException).failure)
        }
        assertEquals(SessionState.SignedOut, repo.session.value)
    }

    @Test
    fun `sign up creates an account that can sign in again`() = runTest {
        val repo = open()
        assertTrue(repo.signUp(" Kamila ", "kamila@example.com", "abcd1234").isSuccess)
        assertEquals(SessionState.SignedIn("Kamila", "kamila@example.com"), repo.session.value)
        repo.signOut()
        assertTrue(repo.signIn("kamila@example.com", "abcd1234").isSuccess)
    }

    @Test
    fun `sign up with an existing email fails`() = runTest {
        val result = open().signUp("Someone", "Demo@drivemetrics.app", "abcd1234")
        assertEquals(AuthFailure.EmailTaken, (result.exceptionOrNull() as AuthException).failure)
    }

    @Test
    fun `passwords are stored only as salted hashes`() = runTest {
        val repo = open()
        repo.signUp("A", "a@example.com", "same-password")
        repo.signUp("B", "b@example.com", "same-password")
        val a = accounts.rows.getValue("a@example.com")
        val b = accounts.rows.getValue("b@example.com")
        assertTrue("same-password" !in a.passwordHash)
        // the same password gives different hashes, so equal passwords cannot be spotted
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.passwordHash, b.passwordHash)
    }

    @Test
    fun `the signed in account is still signed in after a restart`() = runTest {
        open().signUp("Kamila", "kamila@example.com", "abcd1234")
        assertEquals(SessionState.SignedIn("Kamila", "kamila@example.com"), open().session.value)
    }

    @Test
    fun `guest and sign out are remembered too`() = runTest {
        val repo = open()
        repo.continueAsGuest()
        assertEquals(SessionState.Guest, repo.session.value)
        testScheduler.runCurrent()
        assertEquals(SessionState.Guest, open().session.value)

        repo.signOut()
        testScheduler.runCurrent()
        assertEquals(SessionState.SignedOut, open().session.value)
    }

    @Test
    fun `a saved session of a deleted account opens signed out`() = runTest {
        sessions.save(SavedSession.Account("gone@example.com"))
        assertEquals(SessionState.SignedOut, open().session.value)
    }
}
