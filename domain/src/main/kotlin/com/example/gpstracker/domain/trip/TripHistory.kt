package com.example.gpstracker.domain.trip

import com.example.gpstracker.model.trip.SavedTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Where finished trips are kept: a database on the phone, or memory in tests and previews.
 * Every trip belongs to an [owner]: an account id, or [GUEST] for demo mode.
 */
interface TripHistory {
    /** The owner's trips, newest first; a new list follows every change. */
    fun trips(owner: String): Flow<List<SavedTrip>>

    /** Saves [trips] for [owner]; a trip with the same id is replaced. */
    suspend fun save(owner: String, trips: List<SavedTrip>)

    companion object {
        /** The owner of trips driven in demo mode. */
        const val GUEST = "guest"
    }
}

/** Keeps trips in memory only; they are gone when the app closes. */
class InMemoryTripHistory : TripHistory {
    private val state = MutableStateFlow<Map<String, Map<String, SavedTrip>>>(emptyMap())

    override fun trips(owner: String): Flow<List<SavedTrip>> =
        state.map { all -> all[owner].orEmpty().values.sortedByDescending { it.savedAtMs } }

    override suspend fun save(owner: String, trips: List<SavedTrip>) = state.update { all ->
        // a trip belongs to one owner only
        val others = all.mapValues { (o, list) -> if (o == owner) list else list - trips.map { it.id }.toSet() }
        others + (owner to others[owner].orEmpty() + trips.associateBy { it.id })
    }
}
