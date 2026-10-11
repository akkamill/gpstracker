package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.EventType

/** Where the "typical driver" numbers come from. A real backend average can replace the simulated one. */
interface BenchmarkSource {
    val score: Int

    /** Events per 100 km for [type]. Lower is better. */
    fun eventsPer100Km(type: EventType): Double

    /** Score trend with [points] values, oldest first. */
    fun trend(points: Int): List<Int>
}

/**
 * Invented assumptions for a simulated typical driver. These are NOT real data
 * from other drivers and must always be labelled "Simulated benchmark".
 */
data class BenchmarkConfig(
    val score: Int = 74,
    val speedingPer100Km: Double = 2.0,
    val harshBrakingPer100Km: Double = 2.6,
    val harshAccelerationPer100Km: Double = 1.8,
    val sharpCornersPer100Km: Double = 1.2,
    /** Small repeating offsets that make the otherwise flat trend look natural. */
    val noise: List<Int> = listOf(0, 1, -1, 2, -2, 1, -1, 0),
)

class SimulatedBenchmarkSource(private val config: BenchmarkConfig = BenchmarkConfig()) : BenchmarkSource {
    override val score: Int get() = config.score

    override fun eventsPer100Km(type: EventType): Double = when (type) {
        EventType.Speeding -> config.speedingPer100Km
        EventType.HarshBraking -> config.harshBrakingPer100Km
        EventType.HarshAcceleration -> config.harshAccelerationPer100Km
        EventType.SharpCorner -> config.sharpCornersPer100Km
        EventType.CrashSuspected -> 0.0
    }

    override fun trend(points: Int): List<Int> =
        List(points) { i -> config.score + config.noise[i % config.noise.size] }
}

enum class ComparisonResult { Better, Similar, Worse }

private const val SIMILAR_BAND = 0.10
private const val EPSILON = 1e-9

/**
 * Compares your events per 100 km with the benchmark. At least 10% lower is
 * "Better", at least 10% higher is "Worse", anything in between is "Similar".
 */
fun compareToBenchmark(you: Double, benchmark: Double): ComparisonResult = when {
    you <= benchmark * (1 - SIMILAR_BAND) + EPSILON && you < benchmark -> ComparisonResult.Better
    you >= benchmark * (1 + SIMILAR_BAND) - EPSILON && you > benchmark -> ComparisonResult.Worse
    else -> ComparisonResult.Similar
}
