package com.example.gpstracker

import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.data.estimate.toNumbers
import com.example.gpstracker.data.fake.FakeAuthRepository
import com.example.gpstracker.data.fake.FakeDriveSimulator
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-written composition root for the UI-first phase. Koin replaces it
 * later; screens never see it, only the ViewModels do.
 */
object AppContainer {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Settings and the vehicle profile are kept in memory only for now. */
    val settings = MutableStateFlow(SettingsUiState())

    /** Fake accounts; replaced by a real implementation later. */
    val auth: AuthRepository = FakeAuthRepository()

    /** The single fake producer behind the whole app. */
    val simulator: FakeDriveSimulator = FakeDriveSimulator(
        vehicleProvider = { settings.value.vehicle.toNumbers() },
    ).also { it.start(appScope) }
}
