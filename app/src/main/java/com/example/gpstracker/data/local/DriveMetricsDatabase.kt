package com.example.gpstracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gpstracker.model.geo.LatLon
import kotlinx.serialization.json.Json

/**
 * The app's database on the phone: trip history and road reports.
 *
 * Every version's schema is exported to `app/schemas`. When an entity changes, raise
 * the version and add a migration, so drivers keep their history after an update.
 */
@Database(
    entities = [TripEntity::class, TripEventEntity::class, RoadReportEntity::class],
    version = DriveMetricsDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DriveMetricsDatabase : RoomDatabase() {
    abstract fun trips(): TripDao

    abstract fun reports(): ReportDao

    companion object {
        const val FILE_NAME = "drivemetrics.db"
        const val VERSION = 3

        /** Version 2: accounts moved to the server, so the phone-only accounts table goes. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS accounts")
            }
        }

        /**
         * Version 3: every trip belongs to an account (or to demo mode), and remembers whether the
         * server has it. Trips from before belong to demo mode.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN owner TEXT NOT NULL DEFAULT 'guest'")
                db.execSQL("ALTER TABLE trips ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_owner_savedAtMs ON trips (owner, savedAtMs)")
            }
        }

        /** Every migration, oldest first. Trips and reports are kept through all of them. */
        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        @Suppress("SpreadOperator") // runs once at start, with a handful of migrations
        fun create(context: Context): DriveMetricsDatabase =
            Room.databaseBuilder(context, DriveMetricsDatabase::class.java, FILE_NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}

/** A route is always read and written as a whole, so it is stored as one JSON text column. */
class Converters {
    @TypeConverter
    fun routeToJson(route: List<LatLon>): String = Json.encodeToString(route)

    @TypeConverter
    fun routeFromJson(json: String): List<LatLon> = Json.decodeFromString(json)
}
