package com.example.gpstracker.data.local

import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.model.trip.SavedTrip

/** Trip history in the phone's database. */
class RoomTripHistory(private val dao: TripDao) : TripHistory {
    override suspend fun all(): List<SavedTrip> = dao.all().map { it.toModel() }

    override suspend fun save(trips: List<SavedTrip>) {
        if (trips.isEmpty()) return
        dao.save(trips.map { it.toEntity() }, trips.flatMap { it.eventEntities() })
    }
}
