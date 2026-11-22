package com.example.gpstracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TripDao {
    @Transaction
    @Query("SELECT * FROM trips ORDER BY savedAtMs DESC")
    suspend fun all(): List<TripWithEvents>

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
