package com.example.gpstracker.data.auth

import com.example.gpstracker.testing.FakeServer
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteAuthRepositoryTest {
    private val server = FakeServer().apply { addAccount("Demo Driver", "demo@drivemetrics.app", "demo1234") }
    private val sessions = InMemorySessionStore()
    private var now = 1_000L

    /** A fresh repository on the same session file, as after an app restart. */
    private fun TestScope.open() = RemoteAuthRepository(server.api, sessions, backgroundScope, clock = { now })
        .also { testScheduler.runCurrent() }

    private fun failureOf(result: Result<Unit>) = (result.exceptionOrNull() as AuthException).failure

    @Test
    fun `starts signed out when nothing was saved`() = runTest {
        assertEquals(SessionState.SignedOut, open().session.value)
    }

    @Test
    fun `signing in asks the server and saves the token`() = runTest {
        val repo = open()
        assertTrue(repo.signIn(" demo@drivemetrics.app ", "demo1234").isSuccess)
        assertEquals(SessionState.SignedIn("Demo Driver", "demo@drivemetrics.app"), repo.session.value)
        assertEquals("token-demo@drivemetrics.app", repo.token)
        assertEquals("token-demo@drivemetrics.app", (sessions.load() as SavedSession.SignedIn).token)
    }

    @Test
    fun `the server's answers become messages the screens know`() = runTest {
        val repo = open()
        assertEquals(AuthFailure.InvalidCredentials, failureOf(repo.signIn("demo@drivemetrics.app", "wrong1234")))
        assertEquals(AuthFailure.EmailTaken, failureOf(repo.signUp("Someone", "demo@drivemetrics.app", "abcd1234")))
        server.reachable = false
        assertEquals(AuthFailure.ServerUnreachable, failureOf(repo.signIn("demo@drivemetrics.app", "demo1234")))
        assertEquals(SessionState.SignedOut, repo.session.value)
    }

    @Test
    fun `sign up creates the account on the server`() = runTest {
        val repo = open()
        assertTrue(repo.signUp(" Ona ", "ona@example.com", "abcd1234").isSuccess)
        assertEquals(SessionState.SignedIn("Ona", "ona@example.com"), repo.session.value)
        repo.signOut()
        assertTrue(repo.signIn("ona@example.com", "abcd1234").isSuccess)
    }

    @Test
    fun `after a restart the app is still signed in, also without the server`() = runTest {
        open().signIn("demo@drivemetrics.app", "demo1234")
        server.reachable = false
        val restarted = open()
        assertEquals(SessionState.SignedIn("Demo Driver", "demo@drivemetrics.app"), restarted.session.value)
        assertEquals("token-demo@drivemetrics.app", restarted.token)
    }

    @Test
    fun `a token the server no longer accepts signs the user out`() = runTest {
        open().signIn("demo@drivemetrics.app", "demo1234")
        server.acceptsTokens = false
        val restarted = open()
        // the check with the server runs in the background, on the HTTP client's threads
        restarted.session.first { it == SessionState.SignedOut }
        assertNull(restarted.token)
        testScheduler.runCurrent()
        assertEquals(SavedSession.None, sessions.load())
    }

    @Test
    fun `an expired token is not used at all`() = runTest {
        sessions.save(SavedSession.SignedIn("id-1", "Demo Driver", "demo@drivemetrics.app", "token-old", expiresAtMs = 500))
        server.calls.clear()
        assertEquals(SessionState.SignedOut, open().session.value)
        assertTrue("no call with an expired token", server.calls.isEmpty())
    }

    @Test
    fun `a name changed on the server shows after the next start`() = runTest {
        open().signIn("demo@drivemetrics.app", "demo1234")
        server.rename("demo@drivemetrics.app", "Demo Driver 2")
        val renamed = open().session.first { (it as? SessionState.SignedIn)?.name == "Demo Driver 2" }
        assertEquals(SessionState.SignedIn("Demo Driver 2", "demo@drivemetrics.app"), renamed)
    }

    @Test
    fun `guest mode needs no server and is remembered`() = runTest {
        server.reachable = false
        val repo = open()
        repo.continueAsGuest()
        testScheduler.runCurrent()
        assertEquals(SessionState.Guest, open().session.value)
    }
}
