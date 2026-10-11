package com.example.gpstracker.ui.auth.signin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.auth.AuthException
import com.example.gpstracker.data.auth.AuthFailure
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.ui.auth.AuthMessages
import com.example.gpstracker.ui.auth.AuthValidator
import com.example.gpstracker.ui.model.SignInUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: AuthRepository = AppContainer.auth,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, formError = null) }

    fun onTogglePassword() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onSubmit() {
        val current = _state.value
        if (!current.canSubmit) return
        val context = getApplication<Application>()
        AuthValidator.validateEmail(current.email)?.let { error ->
            _state.update { it.copy(emailError = AuthMessages.field(context, error)) }
            return
        }
        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            val result = auth.signIn(current.email, current.password)
            val failure = (result.exceptionOrNull() as? AuthException)?.failure
            _state.update {
                it.copy(
                    isLoading = false,
                    formError = result.exceptionOrNull()?.let { _ ->
                        AuthMessages.failure(context, failure ?: AuthFailure.InvalidCredentials)
                    },
                )
            }
        }
    }
}
