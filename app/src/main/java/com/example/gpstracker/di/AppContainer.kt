package com.example.gpstracker.di

import com.example.gpstracker.data.simulated.SimulatedTelemetrySource
import com.example.gpstracker.domain.driving.ObserveDrivingSession
import com.example.gpstracker.domain.telemetry.TelemetrySource

/** Hand-written composition root. Swap [telemetrySource] to move from simulation to live data. */
object AppContainer {
    val telemetrySource: TelemetrySource by lazy { SimulatedTelemetrySource() }
    val observeDrivingSession: ObserveDrivingSession by lazy { ObserveDrivingSession(telemetrySource) }
}
