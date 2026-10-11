package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.VehicleProfileUi

private const val DEFAULT_CONSUMPTION = 6.5
private const val DEFAULT_MONTHLY_KM = 1000.0

/**
 * Parses the vehicle form into numbers for the estimator. Invalid or empty
 * fields fall back to defaults; an empty or invalid price means money is hidden.
 */
fun VehicleProfileUi.toNumbers(): VehicleNumbers = VehicleNumbers(
    fuelType = fuelType,
    consumptionL100 = parseDecimal(consumptionL100)?.takeIf { it > 0.0 } ?: DEFAULT_CONSUMPTION,
    fuelPricePerL = parseDecimal(fuelPricePerL)?.takeIf { it > 0.0 },
    monthlyKm = parseDecimal(monthlyKm)?.takeIf { it > 0.0 } ?: DEFAULT_MONTHLY_KM,
)
