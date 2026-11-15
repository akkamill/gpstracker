package com.example.gpstracker.data.telematics

import com.example.gpstracker.domain.telemetry.GnssQuality
import com.example.gpstracker.domain.telemetry.TelemetryPoint
import com.example.gpstracker.simulator.avl.AvlIo
import com.example.gpstracker.simulator.avl.AvlRecord

/**
 * Turns a tracker's AVL record into the app's [TelemetryPoint]. This is the only place
 * that knows the device format; the rest of the app works with SI units.
 *
 * - Speed comes from the car's computer (OBD) when available: it is exact and keeps
 *   working without GPS, for example under a bridge. Otherwise GPS speed is used.
 * - Accelerometer axes arrive in milli-g and are converted to m/s².
 */
object AvlTelemetryMapper {
    private const val G_MPS2 = 9.81
    private const val MILLI = 1_000.0
    private const val KMH_PER_MPS = 3.6
    private const val MIN_GOOD_SATELLITES = 6
    private const val MAX_GOOD_HDOP_X10 = 20L

    fun toTelemetry(record: AvlRecord): TelemetryPoint {
        val gps = record.gps
        val speedKmh = record[AvlIo.OBD_VEHICLE_SPEED]?.toDouble()
            ?: if (gps.hasFix) gps.speedKmh.toDouble() else 0.0
        return TelemetryPoint(
            timestampMs = record.timestampMs,
            latitude = gps.latitude,
            longitude = gps.longitude,
            speedMps = speedKmh.coerceAtLeast(0.0) / KMH_PER_MPS,
            headingDeg = gps.angleDeg.toDouble(),
            accelerationMps2 = record[AvlIo.AXIS_X]?.let(::milliGToMps2) ?: 0.0,
            lateralAccelerationMps2 = record[AvlIo.AXIS_Y]?.let(::milliGToMps2) ?: 0.0,
            ignitionOn = record[AvlIo.IGNITION]?.let { it == 1L },
            gnss = when {
                !gps.hasFix -> GnssQuality.Lost
                gps.satellites < MIN_GOOD_SATELLITES -> GnssQuality.Weak
                (record[AvlIo.GNSS_HDOP] ?: 0L) > MAX_GOOD_HDOP_X10 -> GnssQuality.Weak
                else -> GnssQuality.Good
            },
        )
    }

    private fun milliGToMps2(milliG: Long): Double = milliG / MILLI * G_MPS2
}
