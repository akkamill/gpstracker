package com.example.gpstracker.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings kept in a preferences file on the phone.
 *
 * The screens read and change the copy in memory, so typing in a text field never waits for
 * the disk. The file is read once at start and written after every change; only the latest
 * settings are written when changes come quickly.
 *
 * A change made before the file has been read is not lost: it is applied on top of the saved
 * settings once they are loaded.
 */
class DataStoreSettingsRepository(
    private val store: DataStore<Preferences>,
    scope: CoroutineScope,
) : SettingsRepository {
    private val tracer = Tracing.tracer("app")
    private val lock = Any()
    private var loaded = false
    private val pending = mutableListOf<(SettingsUiState) -> SettingsUiState>()

    private val state = MutableStateFlow(SettingsUiState())
    override val settings: StateFlow<SettingsUiState> = state.asStateFlow()

    init {
        scope.launch {
            val saved = SettingsPreferences.read(store.data.first())
            synchronized(lock) {
                state.value = pending.fold(saved) { settings, change -> change(settings) }
                pending.clear()
                loaded = true
            }
            tracer.info("settings loaded")
            state.collect { settings -> store.edit { SettingsPreferences.write(it, settings) } }
        }
    }

    override fun update(transform: (SettingsUiState) -> SettingsUiState) = synchronized(lock) {
        if (!loaded) pending += transform
        state.update(transform)
    }
}
