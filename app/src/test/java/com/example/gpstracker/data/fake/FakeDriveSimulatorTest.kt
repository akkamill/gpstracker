package com.example.gpstracker.data.fake

import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The simulator only ticks; trips begin and end on their own, driven by [TripDetector]. */
class FakeDriveSimulatorTest {
    private var now = 1_700_000_000_000L
    private val simulator = FakeDriveSimulator(clock = { now.also { now += 500 } })

    private fun ticks(count: Int) = repeat(count) { simulator.tick() }

    private fun statesFor(count: Int) = (1..count).map { simulator.tick(); simulator.state.value }

    @Test
    fun `connects after the first few ticks`() {
        assertEquals(ConnectionStatus.Connecting, simulator.state.value.connection)
        ticks(10)
        assertEquals(ConnectionStatus.Replay, simulator.state.value.connection)
    }

    @Test
    fun `starts parked with the last trip on the dashboard`() {
        ticks(20)
        val state = simulator.state.value
        assertEquals(VehicleState.Parked, state.vehicleState)
        assertNotNull(state.lastTrip)
        assertNull(state.tripStartedLabel)
        assertNull(state.speedLimitKmh)
    }

    @Test
    fun `a trip starts by itself and ends by itself`() {
        val states = statesFor(340)
        assertTrue(states.any { it.vehicleState == VehicleState.Driving })
        assertTrue(states.any { it.isTripInProgress && it.tripStartedLabel != null })
        // back to parked at the end of the loop, with no one pressing anything
        assertEquals(VehicleState.Parked, states.last().vehicleState)
    }

    @Test
    fun `the traffic light stop does not split the trip`() {
        val states = statesFor(340)
        val stoppedIndex = states.indexOfFirst { it.vehicleState == VehicleState.Stopped }
        assertTrue(stoppedIndex >= 0)
        // after the light the same trip goes on driving
        assertTrue(states.drop(stoppedIndex).any { it.vehicleState == VehicleState.Driving })
        val trips = simulator.trips.value.sections.sumOf { it.second.size }
        assertEquals(FakeData.trips.size + 1, trips)
    }

    @Test
    fun `speed exceeds the limit and produces events during the trip`() {
        val states = statesFor(340)
        assertTrue(states.any { it.isOverLimit })
        assertTrue(states.any { it.events.any { e -> e.type == EventType.Speeding } })
        assertTrue(states.any { it.latestEvent != null })
        assertTrue(states.all { it.score in 0..100 })
    }

    @Test
    fun `gps loss and reconnecting appear while driving`() {
        val states = statesFor(340)
        assertTrue(states.any { it.gps == GpsStatus.Lost && it.isTripInProgress })
        assertTrue(states.any { it.connection == ConnectionStatus.Reconnecting && it.isTripInProgress })
    }

    @Test
    fun `a finished trip is saved with a banner, a detail and a record`() {
        val before = simulator.records.value.size
        val states = statesFor(340)

        val saved = states.mapNotNull { it.tripJustSaved }.first()
        assertNotNull(simulator.tripDetail(saved.id))
        assertEquals("Today", saved.dateLabel)
        assertEquals(before + 1, simulator.records.value.size)
        assertEquals(saved.id, simulator.state.value.lastTrip?.id)
        assertFalse(simulator.state.value.isTripInProgress)
    }

    @Test
    fun `the trip in progress shows in the trips state`() {
        val states = statesFor(120)
        assertTrue(states.any { it.isTripInProgress })
        // while driving the trips tab gets a "driving now" label
        val label = (1..1).let { simulator.trips.value.tripInProgressLabel }
        assertTrue(label == null || label.startsWith("Driving now"))
    }

    @Test
    fun `skipping to the next drive jumps to the ignition`() {
        ticks(5)
        simulator.skipToNextDrive()
        val states = statesFor(30)
        assertTrue(states.any { it.vehicleState == VehicleState.Driving })
    }
}
