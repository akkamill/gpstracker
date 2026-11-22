package com.example.gpstracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.data.settings.SettingsRepository
import com.example.gpstracker.data.telematics.DemoControls
import com.example.gpstracker.ui.model.SessionState
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Shows the stored settings together with the current account from the session. */
class SettingsViewModel(
    private val store: SettingsRepository,
    private val auth: AuthRepository,
    private val demo: DemoControls,
    private val serverAddress: String = "",
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(store.settings, auth.session) { settings, session ->
        when (session) {
            is SessionState.SignedIn -> settings.copy(accountName = session.name, accountEmail = session.email)
            else -> settings.copy(accountName = null, accountEmail = null)
        }.copy(serverAddress = serverAddress)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, store.settings.value)

    fun onToleranceChange(value: Int) = store.update { it.copy(speedTolerance = value) }

    fun onTripEndAfterChange(value: Int) = store.update { it.copy(tripEndAfterMinutes = value) }

    fun onCrashDetectionChange(value: Boolean) = store.update { it.copy(crashDetectionEnabled = value) }

    fun onEmergencyContactChange(value: String) = store.update { it.copy(emergencyContactName = value) }

    fun signOut() = auth.signOut()

    /** Debug-only presentation shortcut: moves the simulated car to its next drive. */
    fun skipToNextDrive() = demo.skipToNextDrive()
}
