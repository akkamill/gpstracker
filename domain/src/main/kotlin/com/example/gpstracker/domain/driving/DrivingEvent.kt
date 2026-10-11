package com.example.gpstracker.domain.driving

/** Kinds of risky driving behaviour the app recognises. */
enum class DrivingEventType { HARD_BRAKING, HARD_ACCELERATION, SPEEDING }

/**
 * A single detected driving event.
 *
 * @property penalty points taken off the score because of this event
 */
data class DrivingEvent(
    val type: DrivingEventType,
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val penalty: Int,
)
