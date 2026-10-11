package com.example.gpstracker.data.estimate

/**
 * Constants of the fuel estimate. These are simple heuristics chosen to be easy
 * to explain, not measured values; the project report has to justify them.
 *
 * @property litresPerHarshAcceleration extra fuel for one harsh acceleration event
 * @property litresPerHarshBraking extra fuel for one harsh braking event
 * @property litresPerSpeedingMinute extra fuel for one minute above the speed limit
 * @property co2KgPerLitrePetrol approximate CO2 emitted per litre of petrol burned
 * @property co2KgPerLitreDiesel approximate CO2 emitted per litre of diesel burned
 */
data class FuelModelConfig(
    val litresPerHarshAcceleration: Double = 0.020,
    val litresPerHarshBraking: Double = 0.015,
    val litresPerSpeedingMinute: Double = 0.010,
    val co2KgPerLitrePetrol: Double = 2.31,
    val co2KgPerLitreDiesel: Double = 2.68,
)
