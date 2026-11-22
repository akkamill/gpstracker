package com.example.gpstracker.data.drive

import com.example.gpstracker.data.telematics.SimulatedTelemetrySource
import com.example.gpstracker.data.telematics.SimulatorMapLookup
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.domain.trip.InMemoryTripHistory
import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.simulator.history.DemoHistory
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.VehicleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's live data path in virtual time: the simulated car plays at demo speed,
 * and the repository has to turn it into what the Drive, Trips and Insights screens show,
 * with no one starting or stopping anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryDriveRepositoryTest {
    private val settings = MutableStateFlow(SettingsUiState())

    private class Run(val repository: TelemetryDriveRepository, val states: List<DriveUiState>)

    /** Starts the whole path and lets [seconds] of real (virtual) time pass. */
    private fun TestScope.run(
        seconds: Int,
        history: TripHistory = InMemoryTripHistory(),
        demo: (Long) -> List<SavedTrip> = DemoHistory::build,
        owner: MutableStateFlow<String> = MutableStateFlow(TripHistory.GUEST),
        onTripSaved: () -> Unit = {},
    ): Run {
        val source = SimulatedTelemetrySource(SimulatedCar(startMs = 1_700_000_000_000L))
        val repository = TelemetryDriveRepository(
            source = source,
            tracker = DriveTracker(map = SimulatorMapLookup()),
            settings = settings,
            clock = { testScheduler.currentTime },
            history = history,
            demoHistory = demo,
            owner = owner,
            onTripSaved = onTripSaved,
        )
        val states = mutableListOf<DriveUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.drive.toList(states) }
        repository.start(backgroundScope)
        advanceTimeBy(seconds * 1_000L)
        return Run(repository, states)
    }

    @Test
    fun `connects after a moment and shows demo replay`() = runTest {
        val states = run(seconds = 5).states
        assertEquals(ConnectionStatus.Connecting, states.first().connection)
        assertEquals(ConnectionStatus.Replay, states.last().connection)
    }

    @Test
    fun `starts parked with the last trip on the dashboard`() = runTest {
        val state = run(seconds = 10).states.last()
        assertEquals(VehicleState.Parked, state.vehicleState)
        assertNotNull(state.lastTrip)
        assertEquals(null, state.speedLimitKmh)
    }

    @Test
    fun `a trip starts by itself, survives the red light and ends by itself`() = runTest {
        val r = run(seconds = DRIVE_THERE_S)
        val states = r.states
        val firstDriving = states.indexOfFirst { it.vehicleState == VehicleState.Driving }
        assertTrue(firstDriving >= 0)
        assertTrue(states[firstDriving].tripStartedLabel!!.startsWith("Trip started"))
        val stopped = states.indexOfFirst { it.vehicleState == VehicleState.Stopped }
        assertTrue(stopped > firstDriving)
        assertTrue(states.drop(stopped).any { it.vehicleState == VehicleState.Driving })
        // ended by itself, exactly one trip
        val saved = states.mapNotNull { it.tripJustSaved }.distinct()
        assertEquals(1, saved.size)
        assertEquals("Žaliakalnis → Akropolis", saved.single().fromTo)
        val savedAt = states.indexOfFirst { it.tripJustSaved != null }
        assertEquals(VehicleState.Parked, states[savedAt].vehicleState)
        assertEquals(null, states[savedAt].tripStartedLabel)
    }

    @Test
    fun `the dashboard shows events, limits, a lost GPS and a reconnect while driving`() = runTest {
        val states = run(seconds = DRIVE_THERE_S).states
        val seen = states.flatMap { it.events }.map { it.type }.toSet()
        assertEquals(setOf(EventType.HarshBraking, EventType.HarshAcceleration, EventType.Speeding, EventType.SharpCorner), seen)
        assertTrue(states.any { it.latestEvent != null })
        assertTrue(states.any { it.isOverLimit && it.speedLimitKmh == 50 })
        assertTrue(states.any { it.speedLimitKmh == 70 })
        assertTrue(states.any { it.gps == GpsStatus.Lost && it.isTripInProgress })
        assertTrue(states.any { it.connection == ConnectionStatus.Reconnecting && it.isTripInProgress })
        assertTrue(states.all { it.score in 0..100 })
    }

    @Test
    fun `a finished trip has a detail, a record and becomes the last trip`() = runTest {
        val before = run(seconds = 1).repository.records.value.size
        val r = run(seconds = DRIVE_THERE_S)
        val saved = r.states.mapNotNull { it.tripJustSaved }.first()
        val detail = r.repository.tripDetail(saved.id)!!
        assertEquals(saved.eventCount, detail.events.size)
        assertTrue(detail.tips.isNotEmpty())
        assertTrue(detail.route.size > 100)
        assertEquals(before + 1, r.repository.records.value.size)
        assertEquals(saved.id, r.states.last().lastTrip?.id)
        assertEquals(saved.id, r.repository.trips.value.sections.first().second.first().id)
    }

    @Test
    fun `banners disappear by themselves`() = runTest {
        val states = run(seconds = DRIVE_THERE_S).states
        val event = states.indexOfFirst { it.latestEvent != null }
        assertTrue(states.drop(event).any { it.latestEvent == null && it.isTripInProgress })
        val saved = states.indexOfFirst { it.tripJustSaved != null }
        assertTrue(states.drop(saved).any { it.tripJustSaved == null })
    }

    @Test
    fun `the trips tab says when a trip is in progress`() = runTest {
        val r = run(seconds = 60)
        assertTrue(r.repository.trips.value.tripInProgressLabel!!.startsWith("Driving now"))
    }

    @Test
    fun `finished trips are saved to the history and are there after a restart`() = runTest {
        val history = InMemoryTripHistory()
        val saved = run(seconds = DRIVE_THERE_S, history = history).states.mapNotNull { it.tripJustSaved }.first()
        assertEquals(saved.id, history.guestTrips().first().id)

        // the next start loads it; the demo trips are not added again
        val restarted = run(seconds = 1, history = history, demo = { error("history is not empty") })
        assertEquals(saved.id, restarted.repository.trips.value.sections.first().second.first().id)
        assertNotNull(restarted.repository.tripDetail(saved.id))
        assertEquals(history.guestTrips().size, restarted.repository.records.value.size)
    }

    @Test
    fun `an empty history is filled with the demo trips once`() = runTest {
        val history = InMemoryTripHistory()
        val r = run(seconds = 1, history = history)
        assertEquals(DemoHistory.build(0).size, history.guestTrips().size)
        assertEquals(DemoHistory.build(0).first().id, r.states.last().lastTrip?.id)
    }

    @Test
    fun `a new account starts with an empty history, without demo trips`() = runTest {
        val history = InMemoryTripHistory()
        val r = run(seconds = 1, history = history, owner = MutableStateFlow("ona"))
        assertTrue(r.repository.trips.value.sections.isEmpty())
        assertTrue(r.repository.records.value.isEmpty())
        assertTrue(history.trips("ona").first().isEmpty())
    }

    @Test
    fun `each account sees its own trips, and a trip is saved for whoever is signed in`() = runTest {
        val history = InMemoryTripHistory()
        val owner = MutableStateFlow("ona")
        var syncRequests = 0
        val r = run(seconds = DRIVE_THERE_S, history = history, owner = owner, onTripSaved = { syncRequests++ })
        val saved = r.states.mapNotNull { it.tripJustSaved }.first()
        assertEquals(listOf(saved.id), history.trips("ona").first().map { it.id })
        assertEquals(1, syncRequests)

        // signing out to demo mode shows the demo trips, not Ona's
        owner.value = TripHistory.GUEST
        advanceTimeBy(1_000)
        val guestIds = r.repository.trips.value.sections.flatMap { it.second }.map { it.id }
        assertTrue(saved.id !in guestIds)
        assertEquals(DemoHistory.build(0).size, guestIds.size)
        assertEquals(null, r.repository.tripDetail(saved.id))

        // and back: Ona's trip is there again
        owner.value = "ona"
        advanceTimeBy(1_000)
        assertNotNull(r.repository.tripDetail(saved.id))
    }

    @Test
    fun `trips that arrive in the history later show up by themselves`() = runTest {
        val history = InMemoryTripHistory()
        val r = run(seconds = 1, history = history, owner = MutableStateFlow("ona"))
        val fromServer = DemoHistory.build(0, idPrefix = "server").take(2)
        history.save("ona", fromServer)
        advanceTimeBy(1_000)
        assertEquals(fromServer.map { it.id }, r.repository.trips.value.sections.flatMap { it.second }.map { it.id })
    }

    private suspend fun TripHistory.guestTrips() = trips(TripHistory.GUEST).first()

    private companion object {
        /** Enough for: connect, 30 s parked, the drive there (about a minute at ×10) and its end. */
        const val DRIVE_THERE_S = 120
    }
}
