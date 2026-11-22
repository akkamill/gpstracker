package com.example.gpstracker.domain.trip

import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryTripHistoryTest {
    private fun saved(id: String, savedAtMs: Long, score: Int = 100) =
        SavedTrip(id, savedAtMs, Trip(0, 1, 1.0, emptyList(), emptyList(), 50.0, 0, score, null, null, false))

    @Test
    fun `newest trip first, and saving the same id replaces it`() = runTest {
        val history = InMemoryTripHistory(listOf(saved("a", 10)))
        history.save(listOf(saved("b", 30), saved("c", 20)))
        history.save(listOf(saved("a", 10, score = 70)))
        assertEquals(listOf("b", "c", "a"), history.all().map { it.id })
        assertEquals(70, history.all().last().trip.score)
    }
}
