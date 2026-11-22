package com.example.gpstracker.simulator.road

/**
 * The roads the simulated car can drive on, with speed limits. A tracker does not
 * know speed limits; in the real system they come from open map data. For the
 * simulation this small map plays that role.
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

/**
 * The part of Kaunas the demo car drives in. The street shapes and speed limits are traced
 * from open map data, so the route lies on the real streets of the street map. The two
 * directions differ near Akropolis, where some streets are one-way.
 */
object Kaunas {
    const val SAVANORIU = "Savanorių pr."
    const val PETRAUSKO = "K. Petrausko g."
    const val PARODOS = "Parodos g."
    const val VYTAUTO = "Vytauto pr."
    const val KARO_LIGONINES = "Karo Ligoninės g."

    private val savanoriuThere = road(
        SAVANORIU,
        50,
        "54.916049 23.945077, 54.910883 23.933713, 54.909126 23.929752, 54.908936 23.929297, " +
            "54.908735 23.928707",
    )
    private val petrauskoThere = road(
        PETRAUSKO,
        50,
        "54.908735 23.928707, 54.906370 23.931539, 54.906121 23.931742, 54.905684 23.931926, " +
            "54.905414 23.931950, 54.903301 23.931436, 54.901864 23.931278, 54.901625 23.931297, " +
            "54.899615 23.932035, 54.899240 23.932074, 54.898860 23.932053",
    )
    private val parodosThere = road(
        PARODOS,
        40,
        "54.898860 23.932053, 54.898850 23.930535, 54.898820 23.930257, 54.898717 23.929937, " +
            "54.898442 23.929454, 54.898322 23.929090, 54.898170 23.927828, 54.898129 23.927622, " +
            "54.897580 23.925787, 54.897461 23.925528, 54.897196 23.925277",
    )
    private val vytautoThere = road(
        VYTAUTO,
        50,
        "54.897196 23.925277, 54.890907 23.924836, 54.890663 23.924906",
    )
    private val karoLigoninesThere = road(
        KARO_LIGONINES,
        50,
        "54.890663 23.924906, 54.888850 23.921874",
    )
    private val kaunakiemioThere = road(
        "Kaunakiemio g.",
        50,
        "54.888850 23.921874, 54.889450 23.920569, 54.889472 23.920344",
    )
    private val parkingThere = road(
        "Akropolis parking",
        20,
        "54.889472 23.920344, 54.889704 23.919866, 54.889813 23.919700, 54.889993 23.919527, " +
            "54.890030 23.919413, 54.890302 23.919220, 54.890353 23.919135, 54.891597 23.918191",
    )

    private val parkingBack = road(
        "Akropolis parking",
        20,
        "54.891597 23.918191, 54.891910 23.917953, 54.891927 23.917477, 54.891984 23.917096",
    )
    private val mindaugoBack = road(
        "Karaliaus Mindaugo pr.",
        50,
        "54.891984 23.917096, 54.892666 23.916409",
    )
    private val mickeviciausBack = road(
        "A. Mickevičiaus g.",
        50,
        "54.892666 23.916409, 54.892887 23.916883, 54.893001 23.917027, 54.893816 23.917161",
    )
    private val miskoBack = road(
        "Miško g.",
        50,
        "54.893816 23.917161, 54.893566 23.925023",
    )
    private val vytautoBack = road(
        VYTAUTO,
        50,
        "54.893566 23.925023, 54.897196 23.925277",
    )
    private val parodosBack = road(
        PARODOS,
        40,
        "54.897196 23.925277, 54.897461 23.925528, 54.897580 23.925787, 54.898129 23.927622, " +
            "54.898170 23.927828, 54.898322 23.929090, 54.898442 23.929454, 54.898717 23.929937, " +
            "54.898820 23.930257, 54.898850 23.930535, 54.898860 23.932053",
    )
    private val petrauskoBack = road(
        PETRAUSKO,
        50,
        "54.898860 23.932053, 54.899240 23.932074, 54.899615 23.932035, 54.901625 23.931297, " +
            "54.901864 23.931278, 54.903301 23.931436, 54.905414 23.931950, 54.905684 23.931926, " +
            "54.906121 23.931742, 54.906370 23.931539, 54.908735 23.928707",
    )
    private val savanoriuBack = road(
        SAVANORIU,
        50,
        "54.908735 23.928707, 54.908936 23.929297, 54.909126 23.929752, 54.910883 23.933713, " +
            "54.916049 23.945077",
    )

    val placeZaliakalnis = Place("Žaliakalnis", savanoriuThere.points.first())
    val placeAkropolis = Place("Akropolis", parkingThere.points.last())

    /** About 4.4 km: Savanorių pr., a long city street, a 40 zone, sharp turns and a car park at the end. */
    val toAkropolis = Route(
        name = "Žaliakalnis → Akropolis",
        from = placeZaliakalnis,
        to = placeAkropolis,
        segments = listOf(savanoriuThere, petrauskoThere, parodosThere, vytautoThere, karoLigoninesThere, kaunakiemioThere, parkingThere),
    )

    /** About 4.2 km back, around the one-way streets near Akropolis. */
    val toZaliakalnis = Route(
        name = "Akropolis → Žaliakalnis",
        from = placeAkropolis,
        to = placeZaliakalnis,
        segments = listOf(parkingBack, mindaugoBack, mickeviciausBack, miskoBack, vytautoBack, parodosBack, petrauskoBack, savanoriuBack),
    )

    val map = RoadMap(
        roads = toAkropolis.segments + toZaliakalnis.segments,
        places = listOf(placeZaliakalnis, placeAkropolis),
    )
}

/** A street piece from "lat lon, lat lon, ..." text, which keeps long coordinate lists readable. */
private fun road(name: String, speedLimitKmh: Int, points: String) = RoadSegment(
    name = name,
    speedLimitKmh = speedLimitKmh,
    points = points.split(",").map { pair ->
        val (lat, lon) = pair.trim().split(" ")
        GeoPoint(lat.toDouble(), lon.toDouble())
    },
)
