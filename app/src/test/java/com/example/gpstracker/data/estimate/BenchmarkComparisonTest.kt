package com.example.gpstracker.data.estimate

import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.InsightsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkComparisonTest {
    @Test
    fun `lower than the benchmark by 10 percent or more is better`() {
        assertEquals(ComparisonResult.Better, compareToBenchmark(you = 1.8, benchmark = 2.6))
        assertEquals(ComparisonResult.Better, compareToBenchmark(you = 0.9, benchmark = 1.0))
    }

    @Test
    fun `within 10 percent is similar`() {
        assertEquals(ComparisonResult.Similar, compareToBenchmark(you = 2.0, benchmark = 2.0))
        assertEquals(ComparisonResult.Similar, compareToBenchmark(you = 2.1, benchmark = 2.0))
        assertEquals(ComparisonResult.Similar, compareToBenchmark(you = 1.85, benchmark = 2.0))
    }

    @Test
    fun `higher than the benchmark by more than 10 percent is worse`() {
        assertEquals(ComparisonResult.Worse, compareToBenchmark(you = 3.0, benchmark = 2.0))
    }

    @Test
    fun `simulated benchmark exposes fixed assumptions`() {
        val benchmark = SimulatedBenchmarkSource()
        assertEquals(74, benchmark.score)
        assertEquals(2.6, benchmark.eventsPer100Km(EventType.HarshBraking), 0.0)
        assertEquals(8, benchmark.trend(8).size)
        assertTrue(benchmark.trend(8).all { it in 72..76 })
    }

    private val now = 1_760_000_000_000L
    private val calculator = InsightsCalculator()
    private val vehicle = FakeData.demoVehicle

    @Test
    fun `week insights compare you with the benchmark and keep a gap`() {
        val state = calculator.compute(FakeData.history(now), now, InsightsPeriod.Week, vehicle)
        assertTrue(state.hasEnoughData)
        assertEquals(8, state.trendLabels.size)
        assertEquals(8, state.yourTrend.size)
        assertNull(state.yourTrend[2])
        assertNotNull(state.yourTrend.last())
        assertEquals(4, state.comparison.size)
        assertTrue(state.verdict.contains("benchmark driver"))
        assertNotNull(state.fuel?.wastedCostLabel)
    }

    @Test
    fun `month view uses six buckets`() {
        val state = calculator.compute(FakeData.history(now), now, InsightsPeriod.Month, vehicle)
        assertEquals(6, state.trendLabels.size)
        assertEquals(6, state.benchmarkTrend.size)
    }

    @Test
    fun `fewer than three trips means not enough data`() {
        val recent = FakeData.history(now).filter { it.startMs > now - InsightsCalculator.WEEK_MS }.take(2)
        val state = calculator.compute(recent, now, InsightsPeriod.Week, vehicle)
        assertFalse(state.hasEnoughData)
    }

    @Test
    fun `money is hidden without a fuel price`() {
        val state = calculator.compute(FakeData.history(now), now, InsightsPeriod.Week, vehicle.copy(fuelPricePerL = null))
        assertNull(state.fuel?.wastedCostLabel)
        assertNull(state.savedVsPreviousLabel)
        assertNull(state.projectedYearlySavingLabel)
    }
}
