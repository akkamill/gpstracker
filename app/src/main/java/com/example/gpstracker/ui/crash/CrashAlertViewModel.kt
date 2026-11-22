package com.example.gpstracker.ui.crash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.data.emergency.EmergencyNotifier
import com.example.gpstracker.data.settings.PhoneNumbers
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.AlertOutcome
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The crash alert. A suspected crash (or the test button) starts a 10-second countdown. Without
 * an answer, the emergency contact gets a text with the time and the car's last position, and a
 * call. "I'm OK" stops it; "Send alert now" skips the wait.
 *
 * Logs never contain the contact's name or number.
 */
class CrashAlertViewModel(
    private val settings: StateFlow<SettingsUiState>,
    private val position: () -> GeoPointUi? = { null },
    private val notifier: EmergencyNotifier? = null,
    crashes: Flow<DrivingEventUi?> = emptyFlow(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(CrashAlertUiState())
    val state: StateFlow<CrashAlertUiState> = _state.asStateFlow()

    val contactName: String get() = settings.value.emergencyContactName

    /** The contact's number ready to dial, or empty when none (or no valid one) is set. */
    val contactPhone: String get() = PhoneNumbers.normalize(settings.value.emergencyContactPhone).orEmpty()

    private var countdown: Job? = null
    private var isTest = false
    private val tracer = Tracing.tracer("app")

    init {
        // a crash suspected while driving starts the alert, when crash detection is on
        viewModelScope.launch {
            crashes.filterNotNull()
                .filter { it.type == EventType.CrashSuspected }
                .distinctUntilChangedBy { it.id }
                .collect { if (settings.value.crashDetectionEnabled) start(test = false) }
        }
    }

    /** The test button in Settings: the same alert, but the text says it is a test. */
    fun trigger() = start(test = true)

    private fun start(test: Boolean) {
        if (_state.value.visible && !_state.value.sent) return
        tracer.warn("crash alert shown", "seconds" to COUNTDOWN_SECONDS, "test" to test)
        isTest = test
        countdown?.cancel()
        _state.value = CrashAlertUiState(visible = true, secondsLeft = COUNTDOWN_SECONDS)
        countdown = viewModelScope.launch {
            while (_state.value.secondsLeft > 0) {
                delay(ONE_SECOND_MS)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
            alertContact(trigger = "no answer")
        }
    }

    fun onImOk() {
        tracer.info("driver is OK, alert cancelled")
        countdown?.cancel()
        _state.value = CrashAlertUiState()
    }

    fun onSendNow() {
        countdown?.cancel()
        alertContact(trigger = "driver")
    }

    /** The text prepared for the contact: when it happened and where the car was. */
    fun alertMessage(): String = (if (isTest) TEST_PREFIX else "") + EmergencyMessage.build(position(), clock())

    fun onCallContact() = tracer.info("emergency contact called by hand")

    fun onTextContact() = tracer.info("emergency text opened by hand")

    private fun alertContact(trigger: String) {
        val phone = contactPhone
        val outcome = when {
            phone.isBlank() -> AlertOutcome.NoNumber
            notifier?.canAlert() != true -> AlertOutcome.NotAllowed
            !notifier.sendText(phone, alertMessage()) -> if (notifier.call(phone)) AlertOutcome.CalledOnly else AlertOutcome.Failed
            notifier.call(phone) -> AlertOutcome.TextedAndCalled
            else -> AlertOutcome.TextedOnly
        }
        _state.update { it.copy(secondsLeft = 0, sent = true, outcome = outcome) }
        tracer.warn("emergency contact alerted", "trigger" to trigger, "outcome" to outcome, "test" to isTest)
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 10
        const val ONE_SECOND_MS = 1_000L
        const val TEST_PREFIX = "TEST, please ignore: "
    }
}
