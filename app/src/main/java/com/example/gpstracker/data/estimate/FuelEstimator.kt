package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.FuelEstimateUi
import com.example.gpstracker.ui.model.FuelType
import java.util.Locale

/** What the estimator needs to know about a trip or a period. */
data class FuelInput(
    val distanceKm: Double,
    val consumptionL100: Double,
    val harshAccelerations: Int,
    val harshBrakings: Int,
    val speedingMinutes: Double,
    val fuelType: FuelType,
)

/** Result of an estimate. Every value is an estimate, never a measurement. */
data class FuelEstimate(
    val fuelUsedL: Double,
    val fuelWastedL: Double,
    val co2Kg: Double,
) {
    /** Money lost to harsh driving, or null when no fuel price is known. */
    fun wastedCost(pricePerL: Double?): Double? = pricePerL?.let { fuelWastedL * it }

    fun toUi(pricePerL: Double?): FuelEstimateUi = FuelEstimateUi(
        fuelUsedL = fuelUsedL,
        fuelWastedL = fuelWastedL,
        wastedCostLabel = wastedCost(pricePerL)?.let(Money::euro),
        co2Kg = co2Kg,
    )
}

/** Totals of one period used for the saved-versus-previous comparison. */
data class PeriodFuelTotals(val distanceKm: Double, val fuelWastedL: Double)

/**
 * Deliberately simple, explainable fuel model:
 *
 * ```
 * baseFuelL   = distanceKm * consumptionL100 / 100
 * excessFuelL = accelerations * a + brakings * b + speedingMinutes * s
 * fuelUsedL   = baseFuelL + excessFuelL
 * fuelWastedL = excessFuelL
 * co2Kg       = fuelUsedL * factor(fuelType)
 * ```
 */
class FuelEstimator(private val config: FuelModelConfig = FuelModelConfig()) {

    fun estimate(input: FuelInput): FuelEstimate {
        val base = input.distanceKm.coerceAtLeast(0.0) * input.consumptionL100 / PER_100_KM
        val excess = input.harshAccelerations * config.litresPerHarshAcceleration +
            input.harshBrakings * config.litresPerHarshBraking +
            input.speedingMinutes * config.litresPerSpeedingMinute
        val used = base + excess
        val factor = when (input.fuelType) {
            FuelType.Petrol -> config.co2KgPerLitrePetrol
            FuelType.Diesel -> config.co2KgPerLitreDiesel
        }
        return FuelEstimate(fuelUsedL = used, fuelWastedL = excess, co2Kg = used * factor)
    }

    /** Wasted litres per 100 km, or 0 when no distance was driven. */
    fun wastedPer100Km(totals: PeriodFuelTotals): Double =
        if (totals.distanceKm <= 0.0) 0.0 else totals.fuelWastedL / totals.distanceKm * PER_100_KM

    /**
     * Money saved against the previous period. Positive means less was wasted now.
     * Null when there is no previous data or no price.
     */
    fun savedVsPrevious(previous: PeriodFuelTotals?, current: PeriodFuelTotals, pricePerL: Double?): Double? {
        if (previous == null || previous.distanceKm <= 0.0 || pricePerL == null) return null
        val litresPer100 = wastedPer100Km(previous) - wastedPer100Km(current)
        return litresPer100 * current.distanceKm / PER_100_KM * pricePerL
    }

    /**
     * Yearly saving if the driver matched their best week. Null when the price is
     * unknown or the saving is not worth showing (at most one euro).
     */
    fun projectedYearlySaving(
        currentWastedPer100: Double,
        bestWeekWastedPer100: Double,
        monthlyKm: Double,
        pricePerL: Double?,
    ): Double? {
        if (pricePerL == null || monthlyKm <= 0.0) return null
        val saving = (currentWastedPer100 - bestWeekWastedPer100) * monthlyKm * MONTHS_PER_YEAR / PER_100_KM * pricePerL
        return saving.takeIf { it > MIN_PROJECTION_EUR }
    }

    private companion object {
        const val PER_100_KM = 100.0
        const val MONTHS_PER_YEAR = 12
        const val MIN_PROJECTION_EUR = 1.0
    }
}

/** Euro formatting with two decimals and a leading symbol, e.g. "€0.19". */
object Money {
    fun euro(amount: Double): String = "€" + String.format(Locale.US, "%.2f", amount)
}

/** Parses user input that may use a comma or a dot as the decimal separator. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
