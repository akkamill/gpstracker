package com.example.gpstracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    /** The owner's trips, newest first; Room sends a new list after every change. */
    @Transaction
    @Query("SELECT * FROM trips WHERE owner = :owner ORDER BY savedAtMs DESC")
    fun trips(owner: String): Flow<List<TripWithEvents>>

    @Transaction
    @Query("SELECT * FROM trips WHERE owner = :owner AND synced = 0 ORDER BY savedAtMs")
    suspend fun unsynced(owner: String): List<TripWithEvents>

    @Query("SELECT id FROM trips WHERE owner = :owner")
    suspend fun ids(owner: String): List<String>

    @Query("UPDATE trips SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Insert
    suspend fun insertEvents(events: List<TripEventEntity>)

    @Query("DELETE FROM trip_events WHERE tripId IN (:tripIds)")
    suspend fun deleteEvents(tripIds: List<String>)

    /** Saves trips with their events in one transaction; saving a trip again replaces it. */
    @Transaction
    suspend fun save(trips: List<TripEntity>, events: List<TripEventEntity>) {
        deleteEvents(trips.map { it.id })
        insertTrips(trips)
        insertEvents(events)
    }
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM road_reports ORDER BY createdAtMs")
    suspend fun all(): List<RoadReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<RoadReportEntity>)

    @Query("DELETE FROM road_reports")
    suspend fun deleteAll()

    /** Makes the table hold exactly [reports]. */
    @Transaction
    suspend fun replaceAll(reports: List<RoadReportEntity>) {
        deleteAll()
        insertAll(reports)
    }
}
