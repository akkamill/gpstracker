package com.example.gpstracker.model.trip

import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.geo.LatLon
import kotlinx.serialization.Serializable

private const val MS_PER_HOUR = 3_600_000.0

/** What the car is doing, as far as trips are concerned. Comes from the device data, never from the user. */
@Serializable
enum class VehicleState {
    /** Trip in progress, car moving. */
    Driving,

    /** Trip in progress, car standing (traffic light, jam); the trip is not ended yet. */
    Stopped,

    /** No trip in progress: ignition off, or stationary long enough. */
    Parked,
}

/** A finished trip, ready to be stored or sent to the server. */
@Serializable
data class Trip(
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val events: List<DrivingEvent>,
    val route: List<LatLon>,
    val maxSpeedKmh: Double,
    /** Time spent above the limit plus tolerance; used by the fuel estimate. */
    val speedingMs: Long,
    val score: Int,
    val startPlace: String?,
    val endPlace: String?,
    /** True when the trip ended because data stopped arriving. */
    val endedWithoutSignal: Boolean,
) {
    val durationMs: Long get() = endMs - startMs

    val avgSpeedKmh: Double get() = if (durationMs <= 0) 0.0 else distanceKm / (durationMs / MS_PER_HOUR)
}
