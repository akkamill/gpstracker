package com.example.gpstracker.domain.trip

import com.example.gpstracker.model.trip.SavedTrip

/** Where finished trips are kept: a database on the phone, or memory in tests and previews. */
interface TripHistory {
    /** All saved trips, newest first. */
    suspend fun all(): List<SavedTrip>

    /** Saves [trips]; a trip with the same id is replaced. */
    suspend fun save(trips: List<SavedTrip>)
}

/** Keeps trips in memory only; they are gone when the app closes. */
class InMemoryTripHistory(initial: List<SavedTrip> = emptyList()) : TripHistory {
    private val trips = LinkedHashMap<String, SavedTrip>().apply { initial.forEach { put(it.id, it) } }

    override suspend fun all(): List<SavedTrip> = synchronized(trips) { trips.values.sortedByDescending { it.savedAtMs } }

    override suspend fun save(trips: List<SavedTrip>) = synchronized(this.trips) { trips.forEach { this.trips[it.id] = it } }
}
