package com.example.gpstracker.ui.crash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.AppContainer
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Runs the 10-second countdown; after it, the demo alert counts as sent. */
class CrashAlertViewModel(
    private val settings: StateFlow<SettingsUiState> = AppContainer.settings,
) : ViewModel() {
    private val _state = MutableStateFlow(CrashAlertUiState())
    val state: StateFlow<CrashAlertUiState> = _state.asStateFlow()

    val contactName: String get() = settings.value.emergencyContactName

    private var countdown: Job? = null

    fun trigger() {
        countdown?.cancel()
        _state.value = CrashAlertUiState(visible = true, secondsLeft = COUNTDOWN_SECONDS)
        countdown = viewModelScope.launch {
            while (_state.value.secondsLeft > 0) {
                delay(ONE_SECOND_MS)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
            _state.update { it.copy(sent = true) }
        }
    }

    fun onImOk() {
        countdown?.cancel()
        _state.value = CrashAlertUiState()
    }

    fun onSendNow() {
        countdown?.cancel()
        _state.update { it.copy(secondsLeft = 0, sent = true) }
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 10
        const val ONE_SECOND_MS = 1_000L
    }
}
