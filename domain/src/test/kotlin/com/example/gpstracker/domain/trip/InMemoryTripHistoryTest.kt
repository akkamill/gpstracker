package com.example.gpstracker.domain.trip

import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryTripHistoryTest {
    private fun saved(id: String, savedAtMs: Long, score: Int = 100) =
        SavedTrip(id, savedAtMs, Trip(0, 1, 1.0, emptyList(), emptyList(), 50.0, 0, score, null, null, false))

    @Test
    fun `newest trip first, and saving the same id replaces it`() = runTest {
        val history = InMemoryTripHistory()
        history.save("ona", listOf(saved("a", 10)))
        history.save("ona", listOf(saved("b", 30), saved("c", 20)))
        history.save("ona", listOf(saved("a", 10, score = 70)))
        val trips = history.trips("ona").first()
        assertEquals(listOf("b", "c", "a"), trips.map { it.id })
        assertEquals(70, trips.last().trip.score)
    }

    @Test
    fun `each owner sees only their own trips`() = runTest {
        val history = InMemoryTripHistory()
        history.save(TripHistory.GUEST, listOf(saved("demo", 1)))
        history.save("ona", listOf(saved("mine", 2)))
        assertEquals(listOf("demo"), history.trips(TripHistory.GUEST).first().map { it.id })
        assertEquals(listOf("mine"), history.trips("ona").first().map { it.id })
        assertTrue(history.trips("jonas").first().isEmpty())
    }
}
