package com.example.gpstracker.data.telematics

import com.example.gpstracker.domain.map.MapLookup
import com.example.gpstracker.model.map.RoadInfo
import com.example.gpstracker.simulator.road.GeoPoint
import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.road.RoadMap

/** Speed limits and place names from the simulator's road map (OpenStreetMap data later). */
class SimulatorMapLookup(private val map: RoadMap = Kaunas.map) : MapLookup {
    override fun roadAt(latitude: Double, longitude: Double): RoadInfo? =
        map.roadAt(GeoPoint(latitude, longitude))?.let { RoadInfo(it.name, it.speedLimitKmh) }

    override fun placeName(latitude: Double, longitude: Double): String? =
        map.placeNear(GeoPoint(latitude, longitude))?.name
}
