package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.telemetry.TelemetryPoint

/**
 * Folds a stream of [TelemetryPoint]s into a [DrivingSession].
 *
 * Each event kind uses a start threshold and a lower exit threshold, so one
 * sustained hard stop produces one event rather than one per sample.
 * Holds state, does no I/O.
 */
class DrivingSessionTracker {
    private var session = DrivingSession()
    private var lastTimestampMs: Long? = null

    private val detectors = listOf(
        Detector(DrivingEventType.HARD_BRAKING, PENALTY_BRAKING, start = HARD_ACCEL_MPS2, exit = EASE_ACCEL_MPS2) {
            -it.accelerationMps2
        },
        Detector(DrivingEventType.HARD_ACCELERATION, PENALTY_ACCEL, start = HARD_ACCEL_MPS2, exit = EASE_ACCEL_MPS2) {
            it.accelerationMps2
        },
        Detector(DrivingEventType.SPEEDING, PENALTY_SPEEDING, start = SPEED_LIMIT_KMH, exit = SPEED_LIMIT_KMH - 5.0) {
            it.speedKmh
        },
    )

    /** Feeds one sample and returns the updated session. */
    fun onPoint(point: TelemetryPoint): DrivingSession {
        val previous = lastTimestampMs
        val elapsedSec = if (previous == null) 0.0 else ((point.timestampMs - previous) / MS_PER_SEC).coerceIn(0.0, MAX_GAP_SEC)
        lastTimestampMs = point.timestampMs

        val newEvents = detectors.mapNotNull { it.update(point) }
        val events = session.events + newEvents
        session = session.copy(
            latest = point,
            events = events,
            score = (DrivingSession.MAX_SCORE - events.sumOf { it.penalty }).coerceAtLeast(0),
            distanceMeters = session.distanceMeters + point.speedMps * elapsedSec,
            maxSpeedKmh = maxOf(session.maxSpeedKmh, point.speedKmh),
        )
        return session
    }

    private class Detector(
        private val type: DrivingEventType,
        private val penalty: Int,
        private val start: Double,
        private val exit: Double,
        private val value: (TelemetryPoint) -> Double,
    ) {
        private var active = false

        fun update(point: TelemetryPoint): DrivingEvent? {
            val v = value(point)
            val enter = start
            val leave = exit
            return when {
                !active && v >= enter -> {
                    active = true
                    DrivingEvent(type, point.timestampMs, point.latitude, point.longitude, penalty)
                }
                active && v < leave -> {
                    active = false
                    null
                }
                else -> null
            }
        }
    }

    private companion object {
        const val HARD_ACCEL_MPS2 = 3.0
        const val EASE_ACCEL_MPS2 = 1.5
        const val SPEED_LIMIT_KMH = 90.0
        const val PENALTY_BRAKING = 5
        const val PENALTY_ACCEL = 3
        const val PENALTY_SPEEDING = 2
        const val MS_PER_SEC = 1000.0
        const val MAX_GAP_SEC = 10.0
    }
}
