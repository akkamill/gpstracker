package com.example.gpstracker.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gpstracker.domain.trip.TripHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An update must never lose a driver's history. Each test creates the database as an older
 * app version left it (from the schema files in app/schemas), migrates it, and checks the data.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DriveMetricsDatabase::class.java)

    @Test
    fun version2To3GivesOldTripsToDemoModeAndMarksThemNotSynced() = runTest {
        helper.createDatabase(dbName, 2).use { db -> insertTrip(db) }
        helper.runMigrationsAndValidate(dbName, 3, true, *DriveMetricsDatabase.MIGRATIONS).use { db ->
            db.query("SELECT owner, synced FROM trips WHERE id = 't1'").use {
                assertTrue(it.moveToFirst())
                assertEquals("guest", it.getString(0))
                assertEquals(0, it.getInt(1))
            }
        }
    }

    private fun insertTrip(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO trips (id, savedAtMs, startMs, endMs, distanceKm, maxSpeedKmh, speedingMs, score, " +
                "startPlace, endPlace, endedWithoutSignal, route) " +
                "VALUES ('t1', 10, 1, 9, 4.9, 62.0, 0, 97, 'Žaliakalnis', 'Akropolis', 0, " +
                "'[{\"latitude\":54.9,\"longitude\":23.9}]')",
        )
    }

    @Test
    fun anOldVersion1DatabaseOpensInTheCurrentVersionWithItsTrips() = runTest {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO trips (id, savedAtMs, startMs, endMs, distanceKm, maxSpeedKmh, speedingMs, score, " +
                    "startPlace, endPlace, endedWithoutSignal, route) " +
                    "VALUES ('t1', 10, 1, 9, 4.9, 62.0, 0, 97, 'Žaliakalnis', 'Akropolis', 0, " +
                    "'[{\"latitude\":54.9,\"longitude\":23.9}]')",
            )
            db.execSQL(
                "INSERT INTO trip_events (tripId, type, timestampMs, latitude, longitude, peak, speedKmh, penalty, " +
                    "speedLimitKmh, roadName) " +
                    "VALUES ('t1', 'HarshBraking', 5, 54.9, 23.9, 4.1, 42.0, 3, 50, 'Savanorių pr.')",
            )
            db.execSQL(
                "INSERT INTO accounts (email, name, passwordHash, salt, createdAtMs) VALUES ('a@b.c', 'A', 'h', 's', 0)",
            )
        }

        helper.runMigrationsAndValidate(dbName, DriveMetricsDatabase.VERSION, true, *DriveMetricsDatabase.MIGRATIONS).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'accounts'").use { assertFalse(it.moveToFirst()) }
        }

        // the migrated file opens in the current app version with the trip intact
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val current = Room.databaseBuilder(context, DriveMetricsDatabase::class.java, dbName)
            .addMigrations(*DriveMetricsDatabase.MIGRATIONS)
            .build()
        try {
            val trip = RoomTripHistory(current.trips()).trips(TripHistory.GUEST).first().single()
            assertEquals("Žaliakalnis", trip.trip.startPlace)
            assertEquals("Savanorių pr.", trip.trip.events.single().roadName)
            assertEquals(1, trip.trip.route.size)
        } finally {
            current.close()
        }
    }
}
