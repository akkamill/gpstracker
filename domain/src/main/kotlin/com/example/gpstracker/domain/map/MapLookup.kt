package com.example.gpstracker.domain.map

/**
 * The road under the car.
 *
 * @property speedLimitKmh null when the limit is not known
 */
data class RoadInfo(val name: String, val speedLimitKmh: Int?)

/**
 * Map knowledge the tracker itself does not have: which road the car is on, its
 * speed limit, and a place name for trip start and end. Implemented in the data
 * layer (the simulator's road map now, OpenStreetMap data later).
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
