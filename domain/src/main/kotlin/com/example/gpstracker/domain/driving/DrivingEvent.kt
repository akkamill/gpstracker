package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.telemetry.LatLon

/** Kinds of risky driving behaviour the app recognises. */
enum class DrivingEventType { Speeding, HarshBraking, HarshAcceleration, SharpCorner, CrashSuspected }

/**
 * A single detected driving event.
 *
 * @property peak the strongest value seen when the event was confirmed: m/s² for
 * braking, acceleration, corners and crashes (always positive), km/h over the limit
 * for speeding
 * @property penalty points taken off the score because of this event
 * @property speedLimitKmh the limit at that place, if known
 * @property roadName the road at that place, if known
 */
data class DrivingEvent(
    val type: DrivingEventType,
    val timestampMs: Long,
    val position: LatLon,
    val peak: Double,
    val speedKmh: Double,
    val penalty: Int,
    val speedLimitKmh: Int? = null,
    val roadName: String? = null,
)
