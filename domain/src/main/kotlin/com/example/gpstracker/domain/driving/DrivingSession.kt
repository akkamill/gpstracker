package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.telemetry.TelemetryPoint

/** Everything known about the drive so far. */
data class DrivingSession(
    val latest: TelemetryPoint? = null,
    val score: Int = MAX_SCORE,
    val events: List<DrivingEvent> = emptyList(),
    val distanceMeters: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
) {
    companion object {
        const val MAX_SCORE = 100
    }
}
