package com.example.gpstracker.domain.telemetry

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Where telemetry comes from. Live stream, replay and simulator all implement
 * this, so the rest of the app does not care which one is active.
 */
interface TelemetrySource {
    /** Samples in arrival order. */
    val points: Flow<TelemetryPoint>

    /** Current state of the connection. */
    val connection: StateFlow<ConnectionState>
}
