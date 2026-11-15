package com.example.gpstracker.simulator.road

/**
 * The roads the simulated car can drive on, with speed limits. A tracker does not
 * know speed limits; in the real system they come from map data (for example
 * OpenStreetMap). For the simulation this small map plays that role.
 */
class RoadMap(val roads: List<RoadSegment>, val places: List<Place>) {

    /** The road closest to [point], or null if none is within [maxDistanceM]. */
    fun roadAt(point: GeoPoint, maxDistanceM: Double = ROAD_MATCH_M): RoadSegment? =
        roads.map { road -> road to road.distanceTo(point) }
            .filter { it.second <= maxDistanceM }
            .minByOrNull { it.second }
            ?.first

    /** The landmark closest to [point], or null if none is within [maxDistanceM]. */
    fun placeNear(point: GeoPoint, maxDistanceM: Double = PLACE_MATCH_M): Place? =
        places.map { it to Geo.distanceM(it.point, point) }
            .filter { it.second <= maxDistanceM }
            .minByOrNull { it.second }
            ?.first

    private fun RoadSegment.distanceTo(point: GeoPoint): Double =
        points.zipWithNext().minOf { (a, b) -> Geo.distanceToSegmentM(point, a, b) }

    private companion object {
        const val ROAD_MATCH_M = 40.0
        const val PLACE_MATCH_M = 800.0
    }
}

/** A small, approximate piece of Kaunas used by the demo. Coordinates are close, not surveyed. */
object Kaunas {
    private val zaliakalnis = GeoPoint(54.9068, 23.9520)
    private val savanoriuMid = GeoPoint(54.9000, 23.9320)
    private val savanoriuEnd = GeoPoint(54.8925, 23.9090)
    private val jonavosEnd = GeoPoint(54.8900, 23.8960)
    private val mindaugoEnd = GeoPoint(54.8945, 23.8925)
    private val akropolis = GeoPoint(54.8960, 23.8880)

    val savanoriuCity = RoadSegment("Savanorių pr.", 50, listOf(zaliakalnis, GeoPoint(54.9040, 23.9410), savanoriuMid))
    val savanoriuFast = RoadSegment("Savanorių pr.", 70, listOf(savanoriuMid, GeoPoint(54.8960, 23.9210), savanoriuEnd))
    val jonavos = RoadSegment("Jonavos g.", 50, listOf(savanoriuEnd, jonavosEnd))
    val mindaugo = RoadSegment("Karaliaus Mindaugo pr.", 50, listOf(jonavosEnd, mindaugoEnd))
    val akropolisAccess = RoadSegment("Akropolis access road", 30, listOf(mindaugoEnd, akropolis))

    val placeZaliakalnis = Place("Žaliakalnis", zaliakalnis)
    val placeAkropolis = Place("Akropolis", akropolis)

    val map = RoadMap(
        roads = listOf(savanoriuCity, savanoriuFast, jonavos, mindaugo, akropolisAccess),
        places = listOf(placeZaliakalnis, placeAkropolis),
    )

    /** About 5 km: city street, a faster stretch, a sharp right turn and a 30 zone at the end. */
    val toAkropolis = Route(
        name = "Žaliakalnis → Akropolis",
        from = placeZaliakalnis,
        to = placeAkropolis,
        segments = listOf(savanoriuCity, savanoriuFast, jonavos, mindaugo, akropolisAccess),
    )

    val toZaliakalnis: Route = toAkropolis.reversed()
}
