package com.example.gpstracker.ui.auth.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.auth.AuthException
import com.example.gpstracker.data.auth.AuthFailure
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.ui.auth.AuthMessages
import com.example.gpstracker.ui.auth.AuthValidator
import com.example.gpstracker.ui.model.SignUpUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Validates on submit, and after the first submit re-validates each field as it
 * changes, so nobody sees errors while still typing for the first time.
 */
class SignUpViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: AuthRepository = AppContainer.auth,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private var submitted = false
    private val context get() = getApplication<Application>()

    fun onNameChange(value: String) = _state.update {
        it.copy(name = value, nameError = if (submitted) nameError(value) else null, formError = null)
    }

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = if (submitted) emailError(value) else null, formError = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(
            password = value,
            passwordError = if (submitted) passwordError(value) else null,
            confirmError = if (submitted) confirmError(value, it.confirmPassword) else null,
            formError = null,
        )
    }

    fun onConfirmChange(value: String) = _state.update {
        it.copy(
            confirmPassword = value,
            confirmError = if (submitted) confirmError(it.password, value) else null,
            formError = null,
        )
    }

    fun onTogglePassword() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onAcceptPrivacy(value: Boolean) = _state.update { it.copy(acceptedPrivacy = value) }

    fun onSubmit() {
        val s = _state.value
        if (!s.canSubmit) return
        submitted = true
        val updated = s.copy(
            nameError = nameError(s.name),
            emailError = emailError(s.email),
            passwordError = passwordError(s.password),
            confirmError = confirmError(s.password, s.confirmPassword),
            formError = null,
        )
        val hasErrors = listOf(updated.nameError, updated.emailError, updated.passwordError, updated.confirmError)
            .any { it != null }
        if (hasErrors) {
            _state.value = updated
            return
        }
        _state.value = updated.copy(isLoading = true)
        viewModelScope.launch {
            val result = auth.signUp(s.name, s.email, s.password)
            val failure = (result.exceptionOrNull() as? AuthException)?.failure
            _state.update {
                it.copy(
                    isLoading = false,
                    formError = result.exceptionOrNull()?.let { _ ->
                        AuthMessages.failure(context, failure ?: AuthFailure.EmailTaken)
                    },
                )
            }
        }
    }

    private fun nameError(name: String) = AuthValidator.validateName(name)?.let { AuthMessages.field(context, it) }

    private fun emailError(email: String) = AuthValidator.validateEmail(email)?.let { AuthMessages.field(context, it) }

    private fun passwordError(password: String) =
        AuthValidator.validatePassword(password)?.let { AuthMessages.field(context, it) }

    private fun confirmError(password: String, confirm: String) =
        AuthValidator.validateConfirm(password, confirm)?.let { AuthMessages.field(context, it) }
}
