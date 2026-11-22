package com.example.gpstracker.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.simulator.history.DemoHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        history.save("ona", trips)
        assertEquals(trips, history.trips("ona").first())
    }

    @Test
    fun eachOwnerHasTheirOwnTripsAndTheSyncKnowsWhatIsNew() = runTest {
        val history = RoomTripHistory(db.trips())
        val (mine, fromServer, guest) = DemoHistory.build(nowMs = 1_700_000_000_000L).take(3)
        history.save("ona", listOf(mine))
        history.saveFromServer("ona", listOf(fromServer, mine))
        history.save(TripHistory.GUEST, listOf(guest))

        assertEquals(setOf(mine.id, fromServer.id), history.trips("ona").first().map { it.id }.toSet())
        assertEquals(listOf(guest.id), history.trips(TripHistory.GUEST).first().map { it.id })
        assertEquals(listOf(mine.id), history.unsynced("ona").map { it.id })
        history.markSynced(listOf(mine.id))
        assertTrue(history.unsynced("ona").isEmpty())
    }

    @Test
    fun savingATripAgainReplacesItAndItsEvents() = runTest {
        val history = RoomTripHistory(db.trips())
        val trip = DemoHistory.build(nowMs = 1_700_000_000_000L).first { it.trip.events.size >= 2 }
        history.save("ona", listOf(trip))
        val fewer = trip.copy(trip = trip.trip.copy(events = trip.trip.events.take(1)))
        history.save("ona", listOf(fewer))
        assertEquals(listOf(fewer), history.trips("ona").first())
    }

    @Test
    fun reportsTableHoldsExactlyTheLatestList() = runTest {
        fun report(id: String) = RoadReport(id, ReportType.Police, LatLon(54.9, 23.9), 0, 1, ReportSource.Me, headingDeg = 90.0)
        db.reports().replaceAll(listOf(report("a"), report("b")).map { it.toEntity() })
        db.reports().replaceAll(listOf(report("b")).map { it.toEntity() })
        assertEquals(listOf(report("b")), db.reports().all().map { it.toModel() })
    }
}
