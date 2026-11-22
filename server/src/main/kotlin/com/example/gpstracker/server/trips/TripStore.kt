package com.example.gpstracker.server.trips

import com.example.gpstracker.model.trip.SavedTrip
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Each account's trips. */
interface TripStore {
    /** The account's trips, newest first. */
    suspend fun trips(accountId: UUID, limit: Int): List<SavedTrip>

    /** Saves a trip; one with the same id is replaced. */
    suspend fun save(accountId: UUID, trip: SavedTrip)

    suspend fun count(accountId: UUID): Int
}

/** Trips in PostgreSQL, each as a JSON document. JDBC blocks, so every query runs on the IO threads. */
class PostgresTripStore(private val dataSource: DataSource) : TripStore {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun trips(accountId: UUID, limit: Int): List<SavedTrip> = db { c ->
        c.prepareStatement("SELECT data FROM trips WHERE account_id = ? ORDER BY saved_at DESC LIMIT ?").use {
            it.setObject(1, accountId)
            it.setInt(2, limit)
            it.executeQuery().use { rows ->
                generateSequence { if (rows.next()) json.decodeFromString<SavedTrip>(rows.getString("data")) else null }.toList()
            }
        }
    }

    override suspend fun save(accountId: UUID, trip: SavedTrip) {
        db { c ->
            c.prepareStatement(
                "INSERT INTO trips (account_id, id, saved_at, score, data) VALUES (?, ?, ?, ?, CAST(? AS JSONB)) " +
                    "ON CONFLICT (account_id, id) DO UPDATE SET saved_at = EXCLUDED.saved_at, score = EXCLUDED.score, data = EXCLUDED.data",
            ).use {
                it.setObject(1, accountId)
                it.setString(2, trip.id)
                it.setTimestamp(3, Timestamp(trip.savedAtMs))
                it.setInt(4, trip.trip.score)
                it.setString(5, json.encodeToString(SavedTrip.serializer(), trip))
                it.executeUpdate()
            }
        }
    }

    override suspend fun count(accountId: UUID): Int = db { c ->
        c.prepareStatement("SELECT COUNT(*) FROM trips WHERE account_id = ?").use {
            it.setObject(1, accountId)
            it.executeQuery().use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }
    }

    private suspend fun <T> db(block: (Connection) -> T): T = withContext(Dispatchers.IO) { dataSource.connection.use(block) }
}

/** Trips in memory, for tests. */
class InMemoryTripStore : TripStore {
    private val trips = HashMap<UUID, LinkedHashMap<String, SavedTrip>>()

    override suspend fun trips(accountId: UUID, limit: Int): List<SavedTrip> =
        synchronized(this) { trips[accountId].orEmpty().values.sortedByDescending { it.savedAtMs }.take(limit) }

    override suspend fun save(accountId: UUID, trip: SavedTrip) {
        synchronized(this) { trips.getOrPut(accountId) { LinkedHashMap() }[trip.id] = trip }
    }

    override suspend fun count(accountId: UUID): Int = synchronized(this) { trips[accountId]?.size ?: 0 }
}
