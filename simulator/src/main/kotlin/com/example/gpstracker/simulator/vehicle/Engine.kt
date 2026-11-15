package com.example.gpstracker.simulator.vehicle

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * What the car's own computer knows: RPM, coolant temperature, fuel level and battery
 * voltage. The tracker reads these over OBD-II / CAN. Rough models, only meant to look
 * plausible on a dashboard.
 */
class Engine(
    private var fuelLiters: Double = 32.0,
    private val tankLiters: Double = 50.0,
    private val consumptionL100: Double = 6.5,
    private var coolantC: Double = 18.0,
) {
    var running = false

    fun step(speedMps: Double, accelMps2: Double, distanceM: Double, dtS: Double) {
        fuelLiters = max(0.0, fuelLiters - distanceM / M_PER_100KM * consumptionL100)
        coolantC = if (running) {
            minOf(WARM_C, coolantC + WARM_UP_C_PER_S * dtS)
        } else {
            max(AMBIENT_C, coolantC - COOL_DOWN_C_PER_S * dtS)
        }
        lastSpeedKmh = speedMps * KMH_PER_MPS
        lastAccel = accelMps2
    }

    private var lastSpeedKmh = 0.0
    private var lastAccel = 0.0

    /** Idle when standing; otherwise a five-gear guess plus a bump under load. */
    val rpm: Int
        get() {
            if (!running) return 0
            val v = lastSpeedKmh
            if (v < 1.0) return IDLE_RPM
            val rpmPerKmh = GEARS.first { v < it.first }.second
            return (IDLE_RPM + v * rpmPerKmh + max(0.0, lastAccel) * LOAD_RPM).roundToInt()
        }

    val coolantTemperatureC: Int get() = coolantC.roundToInt()

    val fuelLevelPercent: Int get() = (fuelLiters / tankLiters * PERCENT).roundToInt()

    /** Higher while the alternator charges. */
    val batteryMillivolts: Int get() = if (running) CHARGING_MV else RESTING_MV

    private companion object {
        const val M_PER_100KM = 100_000.0
        const val KMH_PER_MPS = 3.6
        const val PERCENT = 100.0
        const val IDLE_RPM = 780
        const val LOAD_RPM = 220.0
        const val WARM_C = 90.0
        const val AMBIENT_C = 12.0
        const val WARM_UP_C_PER_S = 0.4
        const val COOL_DOWN_C_PER_S = 0.05
        const val CHARGING_MV = 14_100
        const val RESTING_MV = 12_450

        /** (upper speed of the gear in km/h, rpm added per km/h). */
        val GEARS = listOf(20.0 to 90.0, 35.0 to 55.0, 50.0 to 42.0, 70.0 to 32.0, Double.MAX_VALUE to 26.0)
    }
}
