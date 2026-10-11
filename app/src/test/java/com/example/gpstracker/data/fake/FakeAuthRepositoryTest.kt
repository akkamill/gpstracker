package com.example.gpstracker.data.fake

import com.example.gpstracker.data.auth.AuthException
import com.example.gpstracker.data.auth.AuthFailure
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepositoryTest {
    private val repo = FakeAuthRepository(latencyMs = 0)

    @Test
    fun `starts signed out`() {
        assertEquals(SessionState.SignedOut, repo.session.value)
    }

    @Test
    fun `demo account signs in`() = runBlocking {
        val result = repo.signIn("DEMO@drivemetrics.app", "demo1234")
        assertTrue(result.isSuccess)
        assertEquals(SessionState.SignedIn("Demo Driver", "demo@drivemetrics.app"), repo.session.value)
    }

    @Test
    fun `wrong password is rejected`() = runBlocking {
        val result = repo.signIn("demo@drivemetrics.app", "wrong")
        assertEquals(AuthFailure.InvalidCredentials, (result.exceptionOrNull() as AuthException).failure)
        assertEquals(SessionState.SignedOut, repo.session.value)
    }

    @Test
    fun `sign up creates an account that can sign in again`() = runBlocking {
        assertTrue(repo.signUp("Kamila", "kamila@example.com", "abcd1234").isSuccess)
        repo.signOut()
        assertTrue(repo.signIn("kamila@example.com", "abcd1234").isSuccess)
    }

    @Test
    fun `sign up with an existing email fails`() = runBlocking {
        val result = repo.signUp("Someone", "demo@drivemetrics.app", "abcd1234")
        assertEquals(AuthFailure.EmailTaken, (result.exceptionOrNull() as AuthException).failure)
    }

    @Test
    fun `guest and sign out change the session`() {
        repo.continueAsGuest()
        assertEquals(SessionState.Guest, repo.session.value)
        repo.signOut()
        assertEquals(SessionState.SignedOut, repo.session.value)
    }
}
