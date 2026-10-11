package com.example.gpstracker.domain.telemetry

/**
 * One telemetry sample from a vehicle or phone.
 *
 * @property timestampMs sample time in epoch milliseconds
 * @property latitude degrees, -90..90
 * @property longitude degrees, -180..180
 * @property speedMps ground speed in metres per second, never negative
 * @property headingDeg direction of travel in degrees, 0 until < 360
 * @property accelerationMps2 longitudinal acceleration; negative while braking
 */
data class TelemetryPoint(
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Double,
    val headingDeg: Double = 0.0,
    val accelerationMps2: Double = 0.0,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
        require(speedMps >= 0.0) { "speed must not be negative: $speedMps" }
    }

    /** Speed in kilometres per hour. */
    val speedKmh: Double get() = speedMps * MPS_TO_KMH

    private companion object {
        const val MPS_TO_KMH = 3.6
    }
}
