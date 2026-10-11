package com.example.gpstracker.ui.settings

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.ui.model.DataSource
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val store: MutableStateFlow<SettingsUiState> = AppContainer.settings,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = store

    fun onDataSourceChange(value: DataSource) = store.update { it.copy(dataSource = value) }

    fun onServerUrlChange(value: String) = store.update { it.copy(serverUrl = value) }

    fun onToleranceChange(value: Int) = store.update { it.copy(speedTolerance = value) }

    fun onCrashDetectionChange(value: Boolean) = store.update { it.copy(crashDetectionEnabled = value) }

    fun onEmergencyContactChange(value: String) = store.update { it.copy(emergencyContactName = value) }
}
