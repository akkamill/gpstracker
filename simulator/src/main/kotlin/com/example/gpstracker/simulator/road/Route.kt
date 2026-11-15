package com.example.gpstracker.simulator.road

import kotlin.math.abs

/** A named stretch of road with one speed limit, drawn as a polyline. */
data class RoadSegment(val name: String, val speedLimitKmh: Int, val points: List<GeoPoint>) {
    init {
        require(points.size >= 2) { "a road segment needs at least two points" }
    }
}

/** A landmark used to name where a trip starts and ends. */
data class Place(val name: String, val point: GeoPoint)

/**
 * A bend of the route at one polyline vertex. The car does not turn on the spot:
 * it drives an arc of [radiusM] centred on the vertex.
 *
 * @property turnDeg signed, positive = right
 */
data class Corner(val atM: Double, val turnDeg: Double, val radiusM: Double) {
    val arcLengthM: Double get() = radiusM * Geo.toRad(abs(turnDeg))
    val startM: Double get() = atM - arcLengthM / 2
    val endM: Double get() = atM + arcLengthM / 2
    val isSharp: Boolean get() = abs(turnDeg) >= SHARP_TURN_DEG

    companion object {
        /** Turns at least this sharp are junction turns with a tight radius. */
        const val SHARP_TURN_DEG = 20.0
    }
}

/**
 * A drivable path made of consecutive [segments]. Positions along it are given as
 * distance in metres from the start.
 */
class Route(val name: String, val from: Place, val to: Place, val segments: List<RoadSegment>) {

    /** All points in driving order, with the shared joints between segments kept once. */
    val points: List<GeoPoint>

    /** Road segment of each leg (leg i goes from points[i] to points[i + 1]). */
    private val legSegment: IntArray

    /** Distance from the start to each point. */
    private val cumulativeM: DoubleArray

    val lengthM: Double

    /** Interior vertices where the heading changes. */
    val corners: List<Corner>

    init {
        require(segments.isNotEmpty()) { "a route needs at least one segment" }
        segments.zipWithNext().forEach { (a, b) ->
            require(a.points.last() == b.points.first()) { "segments ${a.name} and ${b.name} do not join" }
        }
        val pts = mutableListOf(segments.first().points.first())
        val legs = mutableListOf<Int>()
        segments.forEachIndexed { index, segment ->
            segment.points.drop(1).forEach {
                pts += it
                legs += index
            }
        }
        points = pts
        legSegment = legs.toIntArray()
        cumulativeM = DoubleArray(pts.size)
        for (i in 1 until pts.size) cumulativeM[i] = cumulativeM[i - 1] + Geo.distanceM(pts[i - 1], pts[i])
        lengthM = cumulativeM.last()
        corners = (1 until pts.size - 1).mapNotNull { i ->
            val turn = Geo.turnDeg(legBearing(i - 1), legBearing(i))
            if (abs(turn) < MIN_CORNER_DEG) {
                null
            } else {
                Corner(cumulativeM[i], turn, if (abs(turn) >= Corner.SHARP_TURN_DEG) SHARP_RADIUS_M else GENTLE_RADIUS_M)
            }
        }
    }

    private fun legBearing(leg: Int): Double = Geo.bearingDeg(points[leg], points[leg + 1])

    /** Index of the leg that contains [distanceM]. */
    fun legAt(distanceM: Double): Int {
        val d = distanceM.coerceIn(0.0, lengthM)
        var leg = 0
        while (leg < points.size - 2 && cumulativeM[leg + 1] <= d) leg++
        return leg
    }

    fun positionAt(distanceM: Double): GeoPoint {
        val leg = legAt(distanceM)
        val len = cumulativeM[leg + 1] - cumulativeM[leg]
        val f = if (len == 0.0) 0.0 else ((distanceM - cumulativeM[leg]) / len).coerceIn(0.0, 1.0)
        return Geo.lerp(points[leg], points[leg + 1], f)
    }

    fun segmentAt(distanceM: Double): RoadSegment = segments[legSegment[legAt(distanceM)]]

    /** Distance from the start of the route to the start of segment [index]. */
    fun segmentStartM(index: Int): Double {
        val firstLeg = legSegment.indexOfFirst { it == index }
        return if (firstLeg < 0) lengthM else cumulativeM[firstLeg]
    }

    /** Distance from the start of the route to where [road] begins (matched by name and limit). */
    fun segmentStartM(road: RoadSegment): Double {
        val index = segments.indexOfFirst { it.name == road.name && it.speedLimitKmh == road.speedLimitKmh }
        require(index >= 0) { "${road.name} is not on route $name" }
        return segmentStartM(index)
    }

    /** The corner whose arc contains [distanceM], if any. */
    fun cornerAt(distanceM: Double): Corner? = corners.firstOrNull { distanceM in it.startM..it.endM }

    /** Heading of the car at [distanceM]; turns smoothly through corner arcs. */
    fun headingAt(distanceM: Double): Double {
        val corner = cornerAt(distanceM)
        if (corner != null) {
            val before = legBearing(legAt(corner.atM - SMALL_M))
            val f = (distanceM - corner.startM) / corner.arcLengthM
            return Geo.normalize(before + corner.turnDeg * f)
        }
        return legBearing(legAt(distanceM))
    }

    /** The same roads driven the other way. */
    fun reversed(): Route = Route(
        name = "${to.name} → ${from.name}",
        from = to,
        to = from,
        segments = segments.asReversed().map { it.copy(points = it.points.asReversed()) },
    )

    private companion object {
        const val MIN_CORNER_DEG = 3.0
        const val SHARP_RADIUS_M = 20.0
        const val GENTLE_RADIUS_M = 250.0
        const val SMALL_M = 0.01
    }
}
