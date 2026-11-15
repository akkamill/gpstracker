package com.example.gpstracker.simulator.road

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lon: Double)

/** Small geometry helpers. Distances in metres, angles in degrees (0 = north, clockwise). */
object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    fun toRad(deg: Double) = deg * PI / HALF_TURN

    fun toDeg(rad: Double) = rad * HALF_TURN / PI

    /** Great-circle (haversine) distance. */
    fun distanceM(a: GeoPoint, b: GeoPoint): Double {
        val dLat = toRad(b.lat - a.lat)
        val dLon = toRad(b.lon - a.lon)
        val h = sin(dLat / 2).let { it * it } +
            cos(toRad(a.lat)) * cos(toRad(b.lat)) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    /** Direction from [a] to [b]. */
    fun bearingDeg(a: GeoPoint, b: GeoPoint): Double {
        val y = sin(toRad(b.lon - a.lon)) * cos(toRad(b.lat))
        val x = cos(toRad(a.lat)) * sin(toRad(b.lat)) -
            sin(toRad(a.lat)) * cos(toRad(b.lat)) * cos(toRad(b.lon - a.lon))
        return normalize(toDeg(atan2(y, x)))
    }

    /** Point at fraction [f] (0..1) of the way from [a] to [b]. Fine for short distances. */
    fun lerp(a: GeoPoint, b: GeoPoint, f: Double) =
        GeoPoint(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)

    /** Moves [p] by the given metres north and east. */
    fun offset(p: GeoPoint, northM: Double, eastM: Double): GeoPoint {
        val dLat = toDeg(northM / EARTH_RADIUS_M)
        val dLon = toDeg(eastM / (EARTH_RADIUS_M * cos(toRad(p.lat))))
        return GeoPoint(p.lat + dLat, p.lon + dLon)
    }

    /** Shortest distance from [p] to the line segment [a]–[b], using a local flat projection. */
    fun distanceToSegmentM(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val kx = cos(toRad(a.lat)) * toRad(1.0) * EARTH_RADIUS_M
        val ky = toRad(1.0) * EARTH_RADIUS_M
        val bx = (b.lon - a.lon) * kx
        val by = (b.lat - a.lat) * ky
        val px = (p.lon - a.lon) * kx
        val py = (p.lat - a.lat) * ky
        val len2 = bx * bx + by * by
        val t = if (len2 == 0.0) 0.0 else ((px * bx + py * by) / len2).coerceIn(0.0, 1.0)
        val dx = px - t * bx
        val dy = py - t * by
        return sqrt(dx * dx + dy * dy)
    }

    /** Signed turn from one heading to the next, -180..180. Positive = right (clockwise). */
    fun turnDeg(fromDeg: Double, toDeg: Double): Double {
        var d = (toDeg - fromDeg) % FULL_TURN
        if (d > HALF_TURN) d -= FULL_TURN
        if (d < -HALF_TURN) d += FULL_TURN
        return d
    }

    fun normalize(deg: Double): Double = ((deg % FULL_TURN) + FULL_TURN) % FULL_TURN
}
