package com.example.gpstracker.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun version1To2KeepsTripsAndReportsAndDropsLocalAccounts() = runTest {
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

        helper.runMigrationsAndValidate(dbName, 2, true, *DriveMetricsDatabase.MIGRATIONS).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'accounts'").use { assertFalse(it.moveToFirst()) }
        }

        // the migrated file opens in the current app version with the trip intact
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val current = Room.databaseBuilder(context, DriveMetricsDatabase::class.java, dbName)
            .addMigrations(*DriveMetricsDatabase.MIGRATIONS)
            .build()
        try {
            val trip = RoomTripHistory(current.trips()).all().single()
            assertEquals("Žaliakalnis", trip.trip.startPlace)
            assertEquals("Savanorių pr.", trip.trip.events.single().roadName)
            assertEquals(1, trip.trip.route.size)
        } finally {
            current.close()
        }
    }
}
