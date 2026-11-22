package com.example.gpstracker.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip

/** One finished trip. The route is stored as one JSON column; events have their own table. */
@Entity(tableName = "trips", indices = [Index("savedAtMs")])
data class TripEntity(
    @PrimaryKey val id: String,
    val savedAtMs: Long,
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val maxSpeedKmh: Double,
    val speedingMs: Long,
    val score: Int,
    val startPlace: String?,
    val endPlace: String?,
    val endedWithoutSignal: Boolean,
    val route: List<LatLon>,
)

/** One driving event of a trip; deleted together with its trip. */
@Entity(
    tableName = "trip_events",
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tripId")],
)
data class TripEventEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val tripId: String,
    val type: DrivingEventType,
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val peak: Double,
    val speedKmh: Double,
    val penalty: Int,
    val speedLimitKmh: Int?,
    val roadName: String?,
)

/** A trip with its events, read in one go. */
data class TripWithEvents(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId") val events: List<TripEventEntity>,
)

fun SavedTrip.toEntity() = TripEntity(
    id = id,
    savedAtMs = savedAtMs,
    startMs = trip.startMs,
    endMs = trip.endMs,
    distanceKm = trip.distanceKm,
    maxSpeedKmh = trip.maxSpeedKmh,
    speedingMs = trip.speedingMs,
    score = trip.score,
    startPlace = trip.startPlace,
    endPlace = trip.endPlace,
    endedWithoutSignal = trip.endedWithoutSignal,
    route = trip.route,
)

fun SavedTrip.eventEntities() = trip.events.map {
    TripEventEntity(
        tripId = id,
        type = it.type,
        timestampMs = it.timestampMs,
        latitude = it.position.latitude,
        longitude = it.position.longitude,
        peak = it.peak,
        speedKmh = it.speedKmh,
        penalty = it.penalty,
        speedLimitKmh = it.speedLimitKmh,
        roadName = it.roadName,
    )
}

fun TripWithEvents.toModel() = SavedTrip(
    id = trip.id,
    savedAtMs = trip.savedAtMs,
    trip = Trip(
        startMs = trip.startMs,
        endMs = trip.endMs,
        distanceKm = trip.distanceKm,
        events = events.sortedBy { it.timestampMs }.map {
            DrivingEvent(
                type = it.type,
                timestampMs = it.timestampMs,
                position = LatLon(it.latitude, it.longitude),
                peak = it.peak,
                speedKmh = it.speedKmh,
                penalty = it.penalty,
                speedLimitKmh = it.speedLimitKmh,
                roadName = it.roadName,
            )
        },
        route = trip.route,
        maxSpeedKmh = trip.maxSpeedKmh,
        speedingMs = trip.speedingMs,
        score = trip.score,
        startPlace = trip.startPlace,
        endPlace = trip.endPlace,
        endedWithoutSignal = trip.endedWithoutSignal,
    ),
)
