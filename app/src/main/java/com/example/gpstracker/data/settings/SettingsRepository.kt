package com.example.gpstracker.data.settings

import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The user's settings and vehicle profile. The account fields of [SettingsUiState] are not
 * stored here; they come from the session.
 */
interface SettingsRepository {
    val settings: StateFlow<SettingsUiState>

    fun update(transform: (SettingsUiState) -> SettingsUiState)
}

/** Settings in memory only, for tests and previews. */
class InMemorySettingsRepository(initial: SettingsUiState = SettingsUiState()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<SettingsUiState> = state.asStateFlow()

    override fun update(transform: (SettingsUiState) -> SettingsUiState) = state.update(transform)
}
