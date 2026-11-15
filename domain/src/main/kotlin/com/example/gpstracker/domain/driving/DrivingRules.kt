package com.example.gpstracker.domain.driving

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Thresholds and penalties of the scoring model, kept in one place so they can be
 * explained (and tuned) in the report.
 *
 * Every event uses a start and a lower exit threshold (hysteresis), so one long hard
 * stop is one event, not one per sample. Acceleration thresholds are in the range
 * commonly used by telematics providers for "harsh" events (about 0.3–0.4 g).
 */
data class DrivingRules(
    /** Speed above the limit that is not penalised; set by the user (0, 5 or 10). */
    val speedingToleranceKmh: Int = 5,
    /** Being over the limit must last this long to count. */
    val speedingMinDurationMs: Long = 3_000,
    val harshBrakingMps2: Double = 3.5,
    val harshAccelerationMps2: Double = 3.0,
    val sharpCornerMps2: Double = 4.0,
    /** About 2.5 g: far beyond any braking a driver can do on purpose. */
    val crashMps2: Double = 25.0,
    /** An event ends when the value drops below this share of its start threshold. */
    val exitRatio: Double = 0.5,
    val speedingPenalty: Int = 4,
    val harshBrakingPenalty: Int = 3,
    val harshAccelerationPenalty: Int = 3,
    val sharpCornerPenalty: Int = 2,
    /** A crash is a safety alert, not something to score. */
    val crashPenalty: Int = 0,
) {
    fun penaltyOf(type: DrivingEventType): Int = when (type) {
        DrivingEventType.Speeding -> speedingPenalty
        DrivingEventType.HarshBraking -> harshBrakingPenalty
        DrivingEventType.HarshAcceleration -> harshAccelerationPenalty
        DrivingEventType.SharpCorner -> sharpCornerPenalty
        DrivingEventType.CrashSuspected -> crashPenalty
    }
}

/** Driving score of a trip. */
object Scoring {
    const val MAX_SCORE = 100
    private const val POINTS_PER_PENALTY_PER_KM = 10.0
    private const val MIN_KM = 1.0

    /**
     * `100 − 10 × penalties / max(km, 1)`, clamped to 0..100. Dividing by distance makes
     * long trips comparable with short ones: one harsh stop matters less over 30 km.
     */
    fun score(totalPenalty: Int, distanceKm: Double): Int =
        (MAX_SCORE - POINTS_PER_PENALTY_PER_KM * totalPenalty / max(distanceKm, MIN_KM))
            .roundToInt()
            .coerceIn(0, MAX_SCORE)
}
