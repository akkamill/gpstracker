package com.example.gpstracker.simulator

import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.vehicle.DemoPlans
import com.example.gpstracker.simulator.vehicle.DrivePlan
import com.example.gpstracker.simulator.vehicle.Driver
import com.example.gpstracker.simulator.vehicle.Incident
import com.example.gpstracker.simulator.vehicle.Motion
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverTest {

    private fun drive(plan: DrivePlan, maxSteps: Int = 3_600): Pair<Driver, List<Motion>> {
        val driver = Driver(plan)
        val motions = mutableListOf<Motion>()
        while (!driver.finished && motions.size < maxSteps) motions += driver.step()
        return driver to motions
    }

    private val Motion.kmh get() = speedMps * 3.6

    /** Longest run of consecutive standing samples. */
    private fun longestStop(motions: List<Motion>): Int =
        motions.fold(0 to 0) { (best, run), m -> if (m.speedMps == 0.0) maxOf(best, run + 1) to run + 1 else best to 0 }.first

    @Test
    fun `the eventful drive reaches Akropolis in a realistic time`() {
        val (driver, motions) = drive(DemoPlans.toAkropolis)
        assertTrue(driver.finished)
        assertEquals(Kaunas.toAkropolis.lengthM, driver.distance, 0.5)
        assertTrue("took ${motions.size} s", motions.size in 360..900)
    }

    @Test
    fun `the eventful drive contains every kind of incident in its raw data`() {
        val motions = drive(DemoPlans.toAkropolis).second
        assertTrue("harsh braking", motions.minOf { it.accelMps2 } <= -4.0)
        assertTrue("harsh acceleration", motions.maxOf { it.accelMps2 } >= 3.5)
        assertTrue("sharp corner", motions.maxOf { abs(it.lateralMps2) } >= 5.0)
        // speeding: about 68 km/h on a 50 km/h street
        val vytauto = motions.filter { Kaunas.toAkropolis.segmentAt(it.distanceM).name == Kaunas.VYTAUTO }
        assertTrue(vytauto.maxOf { it.kmh } in 64.0..70.0)
        // the red light: standing for its whole wait
        assertTrue(longestStop(motions) >= 25)
    }

    @Test
    fun `the calm drive stays smooth and within the limits`() {
        val route = Kaunas.toZaliakalnis
        val (driver, motions) = drive(DemoPlans.toZaliakalnis)
        assertTrue(driver.finished)
        // well under the 3 m/s² that counts as harsh
        val worstAccel = motions.maxOf { abs(it.accelMps2) }
        assertTrue("accel $worstAccel", worstAccel <= 2.5)
        val worstLateral = motions.maxOf { abs(it.lateralMps2) }
        assertTrue("lateral $worstLateral", worstLateral < 3.0)
        val worstOver = motions.maxOf { it.kmh - route.segmentAt(it.distanceM).speedLimitKmh }
        assertTrue("over the limit by $worstOver", worstOver <= 1.0)
        assertTrue(longestStop(motions) >= 20)
    }

    @Test
    fun `a crash stops the car at once with a huge deceleration`() {
        val plan = DrivePlan(Kaunas.toAkropolis, listOf(Incident.Crash(atM = 800.0)))
        val (driver, motions) = drive(plan)
        assertTrue(driver.crashed)
        assertTrue(motions.last().speedMps == 0.0)
        assertTrue(motions.minOf { it.accelMps2 } < -30.0)
        assertEquals(800.0, driver.distance, 20.0)
    }

    @Test
    fun `speed never goes negative and distance never goes backwards`() {
        val motions = drive(DemoPlans.toAkropolis).second
        assertTrue(motions.all { it.speedMps >= 0.0 })
        assertTrue(motions.zipWithNext().all { (a, b) -> b.distanceM >= a.distanceM })
    }
}
