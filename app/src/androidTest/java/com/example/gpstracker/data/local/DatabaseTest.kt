package com.example.gpstracker.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gpstracker.data.fake.DemoHistory
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The real database (SQLite on the device), kept in memory so every test starts empty. */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private val db = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        DriveMetricsDatabase::class.java,
    ).build()

    @After
    fun close() = db.close()

    @Test
    fun tripsWithRoutesAndEventsAreReadBackUnchanged() = runTest {
        val history = RoomTripHistory(db.trips())
        val trips = DemoHistory.build(nowMs = 1_700_000_000_000L)
        history.save(trips)
        assertEquals(trips, history.all())
    }

    @Test
    fun savingATripAgainReplacesItAndItsEvents() = runTest {
        val history = RoomTripHistory(db.trips())
        val trip = DemoHistory.build(nowMs = 1_700_000_000_000L).first { it.trip.events.size >= 2 }
        history.save(listOf(trip))
        val fewer = trip.copy(trip = trip.trip.copy(events = trip.trip.events.take(1)))
        history.save(listOf(fewer))
        assertEquals(listOf(fewer), history.all())
    }

    @Test
    fun reportsTableHoldsExactlyTheLatestList() = runTest {
        fun report(id: String) = RoadReport(id, ReportType.Police, LatLon(54.9, 23.9), 0, 1, ReportSource.Me, headingDeg = 90.0)
        db.reports().replaceAll(listOf(report("a"), report("b")).map { it.toEntity() })
        db.reports().replaceAll(listOf(report("b")).map { it.toEntity() })
        assertEquals(listOf(report("b")), db.reports().all().map { it.toModel() })
    }

    @Test
    fun anEmailCanOnlyBeRegisteredOnce() = runTest {
        val account = AccountEntity("demo@drivemetrics.app", "Demo", "hash", "salt", 0)
        assertEquals(1L, db.accounts().insert(account))
        assertEquals(-1L, db.accounts().insert(account.copy(name = "Someone else")))
        assertEquals("Demo", db.accounts().find("demo@drivemetrics.app")?.name)
        assertNull(db.accounts().find("nobody@example.com"))
    }
}
