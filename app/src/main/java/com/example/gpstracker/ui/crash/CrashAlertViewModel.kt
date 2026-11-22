package com.example.gpstracker.ui.crash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.data.settings.PhoneNumbers
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.model.GeoPointUi
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
    private val settings: StateFlow<SettingsUiState>,
    private val position: () -> GeoPointUi? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(CrashAlertUiState())
    val state: StateFlow<CrashAlertUiState> = _state.asStateFlow()

    val contactName: String get() = settings.value.emergencyContactName

    /** The contact's number ready to dial, or empty when none (or no valid one) is set. */
    val contactPhone: String get() = PhoneNumbers.normalize(settings.value.emergencyContactPhone).orEmpty()

    /** The text prepared for the contact: when it happened and where the car was. */
    fun alertMessage(): String = EmergencyMessage.build(position(), clock())

    fun onCallContact() = tracer.info("emergency contact called", "trigger" to "driver")

    fun onTextContact() = tracer.info("emergency text prepared", "trigger" to "driver")

    private var countdown: Job? = null
    private val tracer = Tracing.tracer("app")

    fun trigger() {
        tracer.warn("crash alert shown", "seconds" to COUNTDOWN_SECONDS)
        countdown?.cancel()
        _state.value = CrashAlertUiState(visible = true, secondsLeft = COUNTDOWN_SECONDS)
        countdown = viewModelScope.launch {
            while (_state.value.secondsLeft > 0) {
                delay(ONE_SECOND_MS)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
            _state.update { it.copy(sent = true) }
            tracer.warn("emergency alert sent (demo)", "trigger" to "countdown")
        }
    }

    fun onImOk() {
        tracer.info("driver is OK, alert cancelled")
        countdown?.cancel()
        _state.value = CrashAlertUiState()
    }

    fun onSendNow() {
        tracer.warn("emergency alert sent (demo)", "trigger" to "driver")
        countdown?.cancel()
        _state.update { it.copy(secondsLeft = 0, sent = true) }
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 10
        const val ONE_SECOND_MS = 1_000L
    }
}
