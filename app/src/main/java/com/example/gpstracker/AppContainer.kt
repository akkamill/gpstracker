package com.example.gpstracker

import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.data.drive.TelemetryDriveRepository
import com.example.gpstracker.data.fake.FakeAuthRepository
import com.example.gpstracker.data.telematics.DemoControls
import com.example.gpstracker.data.telematics.SimulatedTelemetrySource
import com.example.gpstracker.data.telematics.SimulatorMapLookup
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-written composition root. Koin replaces it later; screens never see it,
 * only the ViewModels do.
 *
 * Data path: simulated car + tracker (:simulator) → AVL records → TelemetryPoint
 * → DriveTracker (:domain) → DriveRepository → ViewModels → screens.
 */
object AppContainer {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Settings and the vehicle profile are kept in memory only for now. */
    val settings = MutableStateFlow(SettingsUiState())

    /** Fake accounts; replaced by the server later. */
    val auth: AuthRepository = FakeAuthRepository()

    private val telemetry = SimulatedTelemetrySource(SimulatedCar(startMs = System.currentTimeMillis()))

    /** Demo helpers that move the simulated car. */
    val demoControls: DemoControls = telemetry

    val drive: DriveRepository = TelemetryDriveRepository(
        source = telemetry,
        tracker = DriveTracker(map = SimulatorMapLookup()),
        settings = settings,
    ).also { it.start(appScope) }
}
