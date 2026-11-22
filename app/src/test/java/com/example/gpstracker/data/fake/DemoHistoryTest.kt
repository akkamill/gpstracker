package com.example.gpstracker.data.fake

import com.example.gpstracker.data.estimate.InsightsCalculator
import com.example.gpstracker.domain.driving.Scoring
import com.example.gpstracker.model.trip.SavedTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoHistoryTest {
    private val now = 1_700_000_000_000L
    private val trips = DemoHistory.build(now)

    private fun weeksAgo(trip: SavedTrip) = ((now - trip.savedAtMs) / InsightsCalculator.WEEK_MS).toInt()

    @Test
    fun `eight weeks of trips in the past, newest first, with unique ids`() {
        assertTrue(trips.size in 30..45)
        assertTrue(trips.all { it.savedAtMs < now && it.trip.endMs == it.savedAtMs && it.trip.startMs < it.trip.endMs })
        assertEquals(trips.sortedByDescending { it.savedAtMs }, trips)
        assertEquals(trips.size, trips.map { it.id }.toSet().size)
        assertEquals(7, weeksAgo(trips.last()))
    }

    @Test
    fun `one week has no trips, so the trend shows a gap`() {
        val weeks = trips.map(::weeksAgo).toSet()
        assertEquals((0..7).toSet() - 5, weeks)
    }

    @Test
    fun `the driving gets better over the weeks`() {
        val byWeek = trips.groupBy(::weeksAgo).mapValues { (_, t) -> t.map { it.trip.score }.average() }
        assertTrue(byWeek.getValue(0) > byWeek.getValue(7) + 10)
    }

    @Test
    fun `trips are real trips on the demo roads, scored by the real rule`() {
        trips.forEach { saved ->
            val trip = saved.trip
            assertTrue(trip.route.size > 50)
            assertEquals(Scoring.score(trip.events.sumOf { it.penalty }, trip.distanceKm), trip.score)
            assertTrue(trip.events.all { it.roadName != null && it.timestampMs in trip.startMs..trip.endMs })
            assertTrue(trip.startPlace in setOf("Žaliakalnis", "Akropolis"))
        }
    }
}
