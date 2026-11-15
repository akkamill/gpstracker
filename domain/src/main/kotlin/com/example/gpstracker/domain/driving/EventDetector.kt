package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.map.RoadInfo
import com.example.gpstracker.domain.telemetry.TelemetryPoint
import kotlin.math.abs
import kotlin.math.max

/**
 * Finds driving events in a stream of samples. Holds state, does no I/O.
 *
 * Each event kind is a small state machine: it becomes a candidate when its value
 * reaches the start threshold, fires once after the minimum duration, and only
 * re-arms after the value falls below the lower exit threshold.
 */
class EventDetector(initialRules: DrivingRules = DrivingRules()) {

    /** Can be changed at any time, for example when the user picks another tolerance. */
    var rules: DrivingRules = initialRules

    private val detectors = listOf(
        Threshold(
            DrivingEventType.HarshBraking,
            start = { rules.harshBrakingMps2 },
            exit = { rules.harshBrakingMps2 * rules.exitRatio },
        ) { p, _ -> -p.accelerationMps2 },
        Threshold(
            DrivingEventType.HarshAcceleration,
            start = { rules.harshAccelerationMps2 },
            exit = { rules.harshAccelerationMps2 * rules.exitRatio },
        ) { p, _ -> p.accelerationMps2 },
        Threshold(
            DrivingEventType.SharpCorner,
            start = { rules.sharpCornerMps2 },
            exit = { rules.sharpCornerMps2 * rules.exitRatio },
        ) { p, _ -> abs(p.lateralAccelerationMps2) },
        Threshold(
            DrivingEventType.CrashSuspected,
            start = { rules.crashMps2 },
            exit = { rules.crashMps2 * rules.exitRatio },
        ) { p, _ -> abs(p.accelerationMps2) },
        // km/h over the limit; "more than the tolerance" counts, and it re-arms 2 km/h below it
        Threshold(
            DrivingEventType.Speeding,
            start = { rules.speedingToleranceKmh + 1.0 },
            exit = { rules.speedingToleranceKmh - SPEEDING_HYSTERESIS_KMH },
            minDurationMs = { rules.speedingMinDurationMs },
        ) { p, road -> road?.speedLimitKmh?.let { p.speedKmh - it } },
    )

    /** Feeds one sample and returns the events it confirmed (usually none). */
    fun onPoint(point: TelemetryPoint, road: RoadInfo?): List<DrivingEvent> {
        val events = detectors.mapNotNull { detector ->
            detector.update(point, road)?.let { peak ->
                DrivingEvent(
                    type = detector.type,
                    timestampMs = point.timestampMs,
                    position = point.position,
                    peak = peak,
                    speedKmh = point.speedKmh,
                    penalty = rules.penaltyOf(detector.type),
                    speedLimitKmh = road?.speedLimitKmh,
                    roadName = road?.name,
                )
            }
        }
        // A crash also looks like extreme braking; report it once, as a crash.
        return if (events.any { it.type == DrivingEventType.CrashSuspected }) {
            events.filterNot { it.type == DrivingEventType.HarshBraking }
        } else {
            events
        }
    }

    /** Forgets anything in progress, for example when a new trip starts. */
    fun reset() = detectors.forEach { it.reset() }

    private class Threshold(
        val type: DrivingEventType,
        private val start: () -> Double,
        private val exit: () -> Double,
        private val minDurationMs: () -> Long = { 0L },
        private val value: (TelemetryPoint, RoadInfo?) -> Double?,
    ) {
        private var since: Long? = null
        private var fired = false
        private var peak = 0.0

        /** Returns the peak value when the event is confirmed by this sample. */
        fun update(point: TelemetryPoint, road: RoadInfo?): Double? {
            val v = value(point, road)
            if (v == null || v < exit()) {
                reset()
                return null
            }
            if (v < start()) return null
            val firstSeen = since ?: point.timestampMs.also {
                since = it
                peak = v
            }
            peak = max(peak, v)
            if (!fired && point.timestampMs - firstSeen >= minDurationMs()) {
                fired = true
                return peak
            }
            return null
        }

        fun reset() {
            since = null
            fired = false
            peak = 0.0
        }
    }

    private companion object {
        const val SPEEDING_HYSTERESIS_KMH = 2.0
    }
}
