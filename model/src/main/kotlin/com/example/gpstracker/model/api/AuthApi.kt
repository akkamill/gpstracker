package com.example.gpstracker.model.api

import kotlinx.serialization.Serializable

/*
 * What the app and the server send each other, as JSON. Both sides use these same classes,
 * so a renamed field cannot break one side without the other noticing at compile time.
 *
 * Requests that carry a password hide it in toString(), so it never ends up in a log.
 */

@Serializable
data class SignUpRequest(val name: String, val email: String, val password: String) {
    override fun toString() = "SignUpRequest(name=***, email=***, password=***)"
}

@Serializable
data class SignInRequest(val email: String, val password: String) {
    override fun toString() = "SignInRequest(email=***, password=***)"
}

@Serializable
data class AccountInfo(val id: String, val name: String, val email: String)

/** A signed-in session: send [token] as "Authorization: Bearer <token>" with later requests. */
@Serializable
data class AuthResponse(val token: String, val expiresAtMs: Long, val account: AccountInfo)

/** Every error the server returns has a stable [code] the app can react to, and a readable [message]. */
@Serializable
data class ApiError(val code: String, val message: String) {
    companion object {
        const val INVALID_INPUT = "invalid_input"
        const val EMAIL_TAKEN = "email_taken"
        const val INVALID_CREDENTIALS = "invalid_credentials"
        const val UNAUTHORIZED = "unauthorized"
        const val NOT_FOUND = "not_found"
        const val SERVER_ERROR = "server_error"
    }
}

@Serializable
data class HealthResponse(val status: String, val database: String)
