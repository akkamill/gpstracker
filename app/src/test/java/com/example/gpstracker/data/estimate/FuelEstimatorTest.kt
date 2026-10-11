package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.FuelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FuelEstimatorTest {
    private val estimator = FuelEstimator()

    private fun input(
        km: Double = 100.0,
        accel: Int = 0,
        brake: Int = 0,
        speedingMin: Double = 0.0,
        type: FuelType = FuelType.Petrol,
    ) = FuelInput(km, 6.5, accel, brake, speedingMin, type)

    @Test
    fun `base fuel comes from distance and consumption`() {
        val estimate = estimator.estimate(input(km = 100.0))
        assertEquals(6.5, estimate.fuelUsedL, 1e-9)
        assertEquals(0.0, estimate.fuelWastedL, 1e-9)
    }

    @Test
    fun `harsh events and speeding add wasted fuel`() {
        val estimate = estimator.estimate(input(km = 0.0, accel = 2, brake = 2, speedingMin = 3.0))
        // 2 * 0.020 + 2 * 0.015 + 3 * 0.010
        assertEquals(0.100, estimate.fuelWastedL, 1e-9)
        assertEquals(0.100, estimate.fuelUsedL, 1e-9)
    }

    @Test
    fun `co2 depends on fuel type`() {
        val petrol = estimator.estimate(input(type = FuelType.Petrol))
        val diesel = estimator.estimate(input(type = FuelType.Diesel))
        assertEquals(6.5 * 2.31, petrol.co2Kg, 1e-9)
        assertEquals(6.5 * 2.68, diesel.co2Kg, 1e-9)
    }

    @Test
    fun `cost is hidden without a price`() {
        val estimate = estimator.estimate(input(brake = 4))
        assertNull(estimate.wastedCost(null))
        assertNull(estimate.toUi(null).wastedCostLabel)
        assertEquals("€0.10", estimate.toUi(1.60).wastedCostLabel)
    }

    @Test
    fun `saved versus previous is positive when less is wasted now`() {
        val previous = PeriodFuelTotals(distanceKm = 100.0, fuelWastedL = 2.0)
        val current = PeriodFuelTotals(distanceKm = 100.0, fuelWastedL = 1.0)
        // 1 L per 100 km less, over 100 km, at 1.50 per litre
        assertEquals(1.5, estimator.savedVsPrevious(previous, current, 1.50)!!, 1e-9)
    }

    @Test
    fun `saved versus previous is negative when more is wasted now`() {
        val previous = PeriodFuelTotals(100.0, 1.0)
        val current = PeriodFuelTotals(50.0, 1.5)
        // previous 1.0 per 100 km, current 3.0 per 100 km: -2.0 * 0.5 * price
        assertEquals(-1.0, estimator.savedVsPrevious(previous, current, 1.0)!!, 1e-9)
    }

    @Test
    fun `saved versus previous is null without data or price`() {
        val totals = PeriodFuelTotals(100.0, 1.0)
        assertNull(estimator.savedVsPrevious(null, totals, 1.5))
        assertNull(estimator.savedVsPrevious(totals, totals, null))
        assertNull(estimator.savedVsPrevious(PeriodFuelTotals(0.0, 0.0), totals, 1.5))
    }

    @Test
    fun `yearly projection uses monthly km and hides tiny savings`() {
        // (3 - 1) L per 100 km * 1000 km * 12 / 100 * 1.5 = 360
        assertEquals(360.0, estimator.projectedYearlySaving(3.0, 1.0, 1000.0, 1.5)!!, 1e-9)
        assertNull(estimator.projectedYearlySaving(1.001, 1.0, 100.0, 1.0))
        assertNull(estimator.projectedYearlySaving(3.0, 1.0, 1000.0, null))
    }

    @Test
    fun `parses comma and dot decimals`() {
        assertEquals(6.5, parseDecimal("6.5")!!, 0.0)
        assertEquals(6.5, parseDecimal(" 6,5 ")!!, 0.0)
        assertNull(parseDecimal("abc"))
        assertNotNull(parseDecimal("12"))
    }
}
