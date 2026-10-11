package com.example.gpstracker.ui.model

/** Who is using the app. [Guest] is the no-account demo mode. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data object Guest : SessionState
    data class SignedIn(val name: String, val email: String) : SessionState
}

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
            confirmPassword.isNotBlank() && acceptedPrivacy && !isLoading
}

/** Field errors for the vehicle form; null means the field is valid. */
data class VehicleErrorsUi(
    val consumption: String? = null,
    val price: String? = null,
    val monthlyKm: String? = null,
)
