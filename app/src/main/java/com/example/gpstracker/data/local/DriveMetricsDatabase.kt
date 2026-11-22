package com.example.gpstracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.gpstracker.model.geo.LatLon
import kotlinx.serialization.json.Json

/**
 * The app's database on the phone: trip history, road reports and local accounts.
 *
 * Every version's schema is exported to `app/schemas`. When an entity changes, raise
 * the version and add a migration, so drivers keep their history after an update.
 */
@Database(
    entities = [TripEntity::class, TripEventEntity::class, RoadReportEntity::class, AccountEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DriveMetricsDatabase : RoomDatabase() {
    abstract fun trips(): TripDao

    abstract fun reports(): ReportDao

    abstract fun accounts(): AccountDao

    companion object {
        const val FILE_NAME = "drivemetrics.db"

        fun create(context: Context): DriveMetricsDatabase =
            Room.databaseBuilder(context, DriveMetricsDatabase::class.java, FILE_NAME).build()
    }
}

/** A route is always read and written as a whole, so it is stored as one JSON text column. */
class Converters {
    @TypeConverter
    fun routeToJson(route: List<LatLon>): String = Json.encodeToString(route)

    @TypeConverter
    fun routeFromJson(json: String): List<LatLon> = Json.decodeFromString(json)
}
