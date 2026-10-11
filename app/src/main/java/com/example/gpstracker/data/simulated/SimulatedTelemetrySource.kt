package com.example.gpstracker.data.simulated

import com.example.gpstracker.domain.telemetry.ConnectionState
import com.example.gpstracker.domain.telemetry.TelemetryPoint
import com.example.gpstracker.domain.telemetry.TelemetrySource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Scripted, repeatable drive used until the real stream exists. Each 40-step
 * cycle contains a hard acceleration, speeding, and a hard stop.
 */
class SimulatedTelemetrySource(
    private val stepDelayMs: Long = STEP_DELAY_MS,
) : TelemetrySource {

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    override val points: Flow<TelemetryPoint> = flow {
        _connection.value = ConnectionState.Connecting
        delay(CONNECT_DELAY_MS)
        _connection.value = ConnectionState.Connected

        var speed = 0.0
        var lat = START_LAT
        var lon = START_LON
        var time = System.currentTimeMillis()
        var step = 0L
        try {
            while (true) {
                val accel = accelerationAt((step % CYCLE_STEPS).toInt())
                speed = (speed + accel).coerceAtLeast(0.0)
                val heading = (step * HEADING_DRIFT_DEG) % FULL_CIRCLE_DEG
                val rad = Math.toRadians(heading)
                lat += speed * cos(rad) / METERS_PER_DEGREE
                lon += speed * sin(rad) / (METERS_PER_DEGREE * cos(Math.toRadians(lat)))
                time += STEP_SIM_MS
                emit(TelemetryPoint(time, lat, lon, speed, heading, accel))
                step++
                delay(stepDelayMs)
            }
        } finally {
            _connection.value = ConnectionState.Disconnected
        }
    }

    private fun accelerationAt(step: Int): Double = when (step) {
        in 0..4 -> 3.4
        in 5..14 -> 1.0
        in 15..24 -> 0.0
        in 25..29 -> -4.5
        else -> -0.45
    }

    private companion object {
        const val STEP_DELAY_MS = 700L
        const val CONNECT_DELAY_MS = 1_500L
        const val STEP_SIM_MS = 1_000L
        const val CYCLE_STEPS = 40
        const val START_LAT = 40.4093
        const val START_LON = 49.8671
        const val METERS_PER_DEGREE = 111_320.0
        const val HEADING_DRIFT_DEG = 2.0
        const val FULL_CIRCLE_DEG = 360.0
    }
}
