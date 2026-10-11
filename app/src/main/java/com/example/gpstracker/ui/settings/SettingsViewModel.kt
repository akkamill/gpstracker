package com.example.gpstracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.ui.model.DataSource
import com.example.gpstracker.ui.model.SessionState
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Shows the stored settings together with the current account from the session. */
class SettingsViewModel(
    private val store: MutableStateFlow<SettingsUiState> = AppContainer.settings,
    private val auth: AuthRepository = AppContainer.auth,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(store, auth.session) { settings, session ->
        when (session) {
            is SessionState.SignedIn -> settings.copy(accountName = session.name, accountEmail = session.email)
            else -> settings.copy(accountName = null, accountEmail = null)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, store.value)

    fun onDataSourceChange(value: DataSource) = store.update { it.copy(dataSource = value) }

    fun onServerUrlChange(value: String) = store.update { it.copy(serverUrl = value) }

    fun onToleranceChange(value: Int) = store.update { it.copy(speedTolerance = value) }

    fun onCrashDetectionChange(value: Boolean) = store.update { it.copy(crashDetectionEnabled = value) }

    fun onEmergencyContactChange(value: String) = store.update { it.copy(emergencyContactName = value) }

    fun signOut() = auth.signOut()
}
