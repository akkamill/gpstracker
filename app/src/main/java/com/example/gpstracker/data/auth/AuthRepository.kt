package com.example.gpstracker.data.auth

import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.flow.StateFlow

enum class AuthFailure {
    InvalidCredentials,
    EmailTaken,

    /** No answer from the server: no network, or the server is not running. */
    ServerUnreachable,

    /** The server answered, but could not do it. */
    ServerError,
}

class AuthException(val failure: AuthFailure) : Exception(failure.name)

/** Account and session handling. */
interface AuthRepository {
    val session: StateFlow<SessionState>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(name: String, email: String, password: String): Result<Unit>

    fun continueAsGuest()

    fun signOut()
}
