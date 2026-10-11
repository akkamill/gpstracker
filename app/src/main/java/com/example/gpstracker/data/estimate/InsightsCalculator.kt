package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.ComparisonRowUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.FuelType
import com.example.gpstracker.ui.model.InsightsPeriod
import com.example.gpstracker.ui.model.InsightsUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** One finished trip, reduced to the numbers the insights need. */
data class TripRecord(
    val id: String,
    val startMs: Long,
    val distanceKm: Double,
    val score: Int,
    val harshAccelerations: Int,
    val harshBrakings: Int,
    val speedingEvents: Int,
    val sharpCorners: Int,
    val speedingMinutes: Double,
)

/** Vehicle numbers already parsed from the user's text fields. */
data class VehicleNumbers(
    val fuelType: FuelType,
    val consumptionL100: Double,
    val fuelPricePerL: Double?,
    val monthlyKm: Double,
)

/**
 * Builds the Insights screen state from trip history. Pure Kotlin so it can be
 * unit tested; the benchmark is simulated and every money value is an estimate.
 */
class InsightsCalculator(
    private val benchmark: BenchmarkSource = SimulatedBenchmarkSource(),
    private val estimator: FuelEstimator = FuelEstimator(),
) {

    fun compute(
        records: List<TripRecord>,
        nowMs: Long,
        period: InsightsPeriod,
        vehicle: VehicleNumbers,
    ): InsightsUiState {
        val bucketCount = if (period == InsightsPeriod.Week) WEEK_BUCKETS else MONTH_BUCKETS
        val bucketMs = if (period == InsightsPeriod.Week) WEEK_MS else MONTH_MS

        val buckets = List(bucketCount) { index -> bucketTrips(records, nowMs, bucketMs, bucketCount - 1 - index) }
        val current = buckets.last()
        val previous = buckets.getOrNull(bucketCount - 2)?.takeIf { it.isNotEmpty() }

        val yourScore = current.averageScore() ?: 0
        val benchmarkScore = benchmark.score
        val currentDistance = current.sumOf { it.distanceKm }

        val currentEstimate = estimateFor(current, vehicle)
        val currentTotals = PeriodFuelTotals(currentDistance, currentEstimate.fuelWastedL)
        val previousTotals = previous?.let { PeriodFuelTotals(it.sumOf { r -> r.distanceKm }, estimateFor(it, vehicle).fuelWastedL) }

        val saved = estimator.savedVsPrevious(previousTotals, currentTotals, vehicle.fuelPricePerL)
        val savedLabel = saved?.takeIf { abs(it) >= MIN_SAVED_EUR }?.let {
            val amount = Money.euro(abs(it))
            val since = if (period == InsightsPeriod.Week) "last week" else "last month"
            if (it >= 0) "$amount less wasted than $since" else "$amount more wasted than $since"
        }

        return InsightsUiState(
            period = period,
            hasEnoughData = current.size >= MIN_TRIPS,
            yourScore = yourScore,
            benchmarkScore = benchmarkScore,
            verdict = verdict(yourScore, benchmarkScore),
            trendLabels = trendLabels(nowMs, bucketMs, bucketCount, period),
            yourTrend = buckets.map { it.averageScore() },
            benchmarkTrend = benchmark.trend(bucketCount),
            comparison = comparisonRows(current, currentDistance),
            fuel = currentEstimate.toUi(vehicle.fuelPricePerL),
            savedVsPreviousLabel = savedLabel,
            savedIsWorse = saved != null && saved < 0 && savedLabel != null,
            projectedYearlySavingLabel = projection(records, nowMs, vehicle, currentTotals),
            distanceKm = currentDistance,
        )
    }

    /** Fuel estimate for a trip, using the user's vehicle. */
    fun estimateTrip(record: TripRecord, vehicle: VehicleNumbers): FuelEstimate = estimateFor(listOf(record), vehicle)

    private fun estimateFor(trips: List<TripRecord>, vehicle: VehicleNumbers): FuelEstimate =
        estimator.estimate(
            FuelInput(
                distanceKm = trips.sumOf { it.distanceKm },
                consumptionL100 = vehicle.consumptionL100,
                harshAccelerations = trips.sumOf { it.harshAccelerations },
                harshBrakings = trips.sumOf { it.harshBrakings },
                speedingMinutes = trips.sumOf { it.speedingMinutes },
                fuelType = vehicle.fuelType,
            ),
        )

    private fun projection(
        records: List<TripRecord>,
        nowMs: Long,
        vehicle: VehicleNumbers,
        current: PeriodFuelTotals,
    ): String? {
        val weeks = List(WEEK_BUCKETS) { bucketTrips(records, nowMs, WEEK_MS, it) }.filter { it.isNotEmpty() }
        val bestPer100 = weeks.minOfOrNull {
            estimator.wastedPer100Km(PeriodFuelTotals(it.sumOf { r -> r.distanceKm }, estimateFor(it, vehicle).fuelWastedL))
        } ?: return null
        val saving = estimator.projectedYearlySaving(
            currentWastedPer100 = estimator.wastedPer100Km(current),
            bestWeekWastedPer100 = bestPer100,
            monthlyKm = vehicle.monthlyKm,
            pricePerL = vehicle.fuelPricePerL,
        ) ?: return null
        return "Up to €${saving.roundToInt()} a year if you match your best week"
    }

    private fun comparisonRows(current: List<TripRecord>, distanceKm: Double): List<ComparisonRowUi> {
        fun per100(events: Int) = if (distanceKm <= 0.0) 0.0 else events * PER_100_KM / distanceKm
        return listOf(
            Triple(EventType.Speeding, "Speeding", current.sumOf { it.speedingEvents }),
            Triple(EventType.HarshBraking, "Harsh braking", current.sumOf { it.harshBrakings }),
            Triple(EventType.HarshAcceleration, "Harsh acceleration", current.sumOf { it.harshAccelerations }),
            Triple(EventType.SharpCorner, "Sharp corners", current.sumOf { it.sharpCorners }),
        ).map { (type, label, count) ->
            ComparisonRowUi(type, label, you = per100(count), benchmark = benchmark.eventsPer100Km(type))
        }
    }

    private fun verdict(you: Int, bench: Int): String = when {
        you > bench -> "${you - bench} points above the benchmark driver"
        you < bench -> "${bench - you} points below the benchmark driver"
        else -> "Level with the benchmark driver"
    }

    private fun trendLabels(nowMs: Long, bucketMs: Long, count: Int, period: InsightsPeriod): List<String> =
        List(count) { index ->
            val end = nowMs - (count - 1 - index) * bucketMs
            if (period == InsightsPeriod.Week) {
                val cal = Calendar.getInstance().apply { timeInMillis = end }
                "W" + cal.get(Calendar.WEEK_OF_YEAR)
            } else {
                SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(end))
            }
        }

    /** Trips in the bucket that ended [back] buckets ago (0 = the latest bucket). */
    private fun bucketTrips(records: List<TripRecord>, nowMs: Long, bucketMs: Long, back: Int): List<TripRecord> {
        val end = nowMs - back * bucketMs
        val start = end - bucketMs
        return records.filter { it.startMs > start && it.startMs <= end }
    }

    private fun List<TripRecord>.averageScore(): Int? =
        if (isEmpty()) null else map { it.score }.average().roundToInt()

    companion object {
        const val WEEK_BUCKETS = 8
        const val MONTH_BUCKETS = 6
        const val MIN_TRIPS = 3
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        const val MONTH_MS = 30L * 24 * 60 * 60 * 1000
        private const val PER_100_KM = 100.0
        private const val MIN_SAVED_EUR = 0.01
    }
}
