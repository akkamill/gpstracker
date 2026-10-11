package com.example.gpstracker.data.auth

import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.flow.StateFlow

enum class AuthFailure { InvalidCredentials, EmailTaken }

class AuthException(val failure: AuthFailure) : Exception(failure.name)

/** Account and session handling. The fake implementation is replaced by a real one later. */
interface AuthRepository {
    val session: StateFlow<SessionState>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(name: String, email: String, password: String): Result<Unit>

    fun continueAsGuest()

    fun signOut()
}
