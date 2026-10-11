package com.example.gpstracker.data.fake

import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GpsStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDriveSimulatorTest {
    private var now = 1_700_000_000_000L
    private val simulator = FakeDriveSimulator(clock = { now.also { now += 500 } })

    private fun ticks(count: Int) = repeat(count) { simulator.tick() }

    @Test
    fun `connects after the first few ticks`() {
        assertEquals(ConnectionStatus.Connecting, simulator.state.value.connection)
        ticks(10)
        assertEquals(ConnectionStatus.Replay, simulator.state.value.connection)
    }

    @Test
    fun `speed follows the curve and exceeds the limit`() {
        val speeds = (1..120).map { simulator.tick(); simulator.state.value }
        assertTrue(speeds.any { it.speedKmh >= 60 })
        assertTrue(speeds.any { it.isOverLimit })
        assertTrue(speeds.all { it.speedKmh >= 0 })
    }

    @Test
    fun `active trip produces events and a score within bounds`() {
        simulator.startTrip()
        ticks(120)
        val state = simulator.state.value
        assertTrue(state.isTripActive)
        assertTrue(state.eventCount >= 2)
        assertTrue(state.score in 0..100)
        assertTrue(state.distanceKm > 0.0)
        assertTrue(state.events.any { it.type == EventType.Speeding })
    }

    @Test
    fun `no trip means no events and a full score`() {
        ticks(120)
        val state = simulator.state.value
        assertFalse(state.isTripActive)
        assertEquals(0, state.eventCount)
        assertEquals(100, state.score)
    }

    @Test
    fun `gps loss and reconnecting both appear`() {
        val states = (1..200).map { simulator.tick(); simulator.state.value }
        assertTrue(states.any { it.gps == GpsStatus.Lost })
        assertTrue(states.any { it.connection == ConnectionStatus.Reconnecting })
    }

    @Test
    fun `stopping a trip prepends it to the trips list with a detail`() {
        val before = simulator.trips.value.sections.sumOf { it.second.size }
        simulator.startTrip()
        ticks(120)
        simulator.stopTrip()

        val trips = simulator.trips.value
        assertEquals(before + 1, trips.sections.sumOf { it.second.size })
        val newest = trips.sections.first().second.first()
        assertEquals("Today", newest.dateLabel)
        assertNotNull(simulator.tripDetail(newest.id))
        assertFalse(simulator.state.value.isTripActive)
    }
}
