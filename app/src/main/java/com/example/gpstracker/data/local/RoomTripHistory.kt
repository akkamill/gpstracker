package com.example.gpstracker.data.local

import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.model.trip.SavedTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What syncing trips with the server needs besides the plain history. */
interface TripSyncStore {
    /** The owner's trips the server does not have yet, oldest first. */
    suspend fun unsynced(owner: String): List<SavedTrip>

    suspend fun markSynced(ids: List<String>)

    /** Adds trips that came from the server; ones already on the phone are left as they are. */
    suspend fun saveFromServer(owner: String, trips: List<SavedTrip>)
}

/** Trip history in the phone's database. */
class RoomTripHistory(private val dao: TripDao) : TripHistory, TripSyncStore {
    override fun trips(owner: String): Flow<List<SavedTrip>> = dao.trips(owner).map { list -> list.map { it.toModel() } }

    override suspend fun save(owner: String, trips: List<SavedTrip>) = write(owner, trips, synced = false)

    override suspend fun unsynced(owner: String): List<SavedTrip> = dao.unsynced(owner).map { it.toModel() }

    override suspend fun markSynced(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markSynced(ids)
    }

    override suspend fun saveFromServer(owner: String, trips: List<SavedTrip>) {
        val known = dao.ids(owner).toSet()
        write(owner, trips.filter { it.id !in known }, synced = true)
    }

    private suspend fun write(owner: String, trips: List<SavedTrip>, synced: Boolean) {
        if (trips.isEmpty()) return
        dao.save(trips.map { it.toEntity(owner, synced) }, trips.flatMap { it.eventEntities() })
    }
}
