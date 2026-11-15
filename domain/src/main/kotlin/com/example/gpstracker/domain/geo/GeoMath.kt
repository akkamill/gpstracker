package com.example.gpstracker.domain.geo

import com.example.gpstracker.model.geo.LatLon
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Distances in metres and directions in degrees (0 = north, clockwise) between map points. */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    /** Great-circle (haversine) distance. */
    fun distanceM(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    /** Direction from [a] towards [b]. */
    fun bearingDeg(a: LatLon, b: LatLon): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + FULL_TURN) % FULL_TURN
    }

    /** Smallest angle between two directions, 0..180. */
    fun angleBetweenDeg(a: Double, b: Double): Double {
        val d = ((a - b) % FULL_TURN + FULL_TURN) % FULL_TURN
        return if (d > HALF_TURN) FULL_TURN - d else d
    }
}
