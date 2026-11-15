package com.example.gpstracker.domain.map

import com.example.gpstracker.model.map.RoadInfo

/**
 * Map knowledge the tracker itself does not have: which road the car is on, its
 * speed limit, and a place name for trip start and end. Implemented in the data
 * layer (the simulator's road map now, open map data later).
 */
interface MapLookup {
    fun roadAt(latitude: Double, longitude: Double): RoadInfo?

    fun placeName(latitude: Double, longitude: Double): String? = null

    companion object {
        /** No map at all: limits are unknown and speeding cannot be detected. */
        val NONE: MapLookup = object : MapLookup {
            override fun roadAt(latitude: Double, longitude: Double): RoadInfo? = null
        }
    }
}
