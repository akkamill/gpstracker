package com.example.gpstracker.model.telemetry

import com.example.gpstracker.model.geo.LatLon
import kotlinx.serialization.Serializable

private const val MPS_TO_KMH = 3.6

/** How trustworthy the position is. */
@Serializable
enum class GnssQuality {
    Good,
    Weak,

    /** No satellite fix: coordinates are the last known ones, do not use them for the route. */
    Lost,
}

/**
 * One telemetry sample from a vehicle tracker or a phone, already converted to SI units.
 * Device formats (such as AVL records) are mapped into this before any logic sees them.
 *
 * @property timestampMs sample time in epoch milliseconds
 * @property latitude degrees, -90..90
 * @property longitude degrees, -180..180
 * @property speedMps ground speed in metres per second, never negative
 * @property headingDeg direction of travel in degrees, 0 until < 360
 * @property accelerationMps2 forward acceleration; negative while braking
 * @property lateralAccelerationMps2 sideways acceleration in corners; positive to the left
 * @property ignitionOn null when the source cannot tell (a phone, for example)
 */
@Serializable
data class TelemetryPoint(
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Double,
    val headingDeg: Double = 0.0,
    val accelerationMps2: Double = 0.0,
    val lateralAccelerationMps2: Double = 0.0,
    val ignitionOn: Boolean? = null,
    val gnss: GnssQuality = GnssQuality.Good,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
        require(speedMps >= 0.0) { "speed must not be negative: $speedMps" }
    }

    /** Speed in kilometres per hour. */
    val speedKmh: Double get() = speedMps * MPS_TO_KMH

    /** False when the coordinates are stale (no satellite fix). */
    val hasPosition: Boolean get() = gnss != GnssQuality.Lost

    val position: LatLon get() = LatLon(latitude, longitude)
}
