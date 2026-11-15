package com.example.gpstracker.simulator

import com.example.gpstracker.telematics.AvlIo
import com.example.gpstracker.telematics.AvlPriority
import com.example.gpstracker.telematics.AvlRecord
import com.example.gpstracker.simulator.road.Geo
import com.example.gpstracker.simulator.road.GeoPoint
import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.vehicle.CarPhase
import com.example.gpstracker.simulator.vehicle.Noise
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SimulatedCarTest {
    private val start = 1_700_000_000_000L

    /** Steps until the car has driven both plans and is parked again; returns each step's records. */
    private fun oneLoop(car: SimulatedCar): List<List<AvlRecord>> {
        val steps = mutableListOf<List<AvlRecord>>()
        var drives = 0
        var wasDriving = false
        while (drives < 2 || car.phase != CarPhase.Parked) {
            steps += car.step()
            val driving = car.phase == CarPhase.Driving
            if (driving && !wasDriving) drives++
            wasDriving = driving
            check(steps.size < 10_000) { "loop did not finish" }
        }
        return steps
    }

    @Test
    fun `while parked it reports every ten seconds with the ignition off and no OBD data`() {
        val car = SimulatedCar(start, noise = Noise.NONE)
        val records = (1..60).flatMap { car.step() }
        assertEquals(6, records.size)
        assertTrue(records.all { it[AvlIo.IGNITION] == 0L && it[AvlIo.MOVEMENT] == 0L })
        assertTrue(records.all { it[AvlIo.OBD_VEHICLE_SPEED] == null && it[AvlIo.OBD_ENGINE_RPM] == null })
    }

    @Test
    fun `switching the ignition on sends a high priority ignition record`() {
        val car = SimulatedCar(start, noise = Noise.NONE)
        car.step()
        car.skipToNextDrive()
        val records = (1..5).flatMap { car.step() }
        val ignition = records.single { it.eventIoId == AvlIo.IGNITION }
        assertEquals(AvlPriority.High, ignition.priority)
        assertEquals(1L, ignition[AvlIo.IGNITION])
        assertTrue(ignition[AvlIo.OBD_ENGINE_RPM]!! > 0)
    }

    @Test
    fun `a full loop drives there and back and returns records in time order`() {
        val car = SimulatedCar(start, noise = Noise.NONE)
        // a few more seconds so the final ignition-off record is included
        val records = oneLoop(car).flatten() + (1..5).flatMap { car.step() }
        assertTrue(records.zipWithNext().all { (a, b) -> b.timestampMs > a.timestampMs })
        val ignitionEvents = records.filter { it.eventIoId == AvlIo.IGNITION }.map { it[AvlIo.IGNITION] }
        assertEquals(listOf(1L, 0L, 1L, 0L), ignitionEvents)
        val km = (records.last()[AvlIo.TOTAL_ODOMETER]!! - records.first()[AvlIo.TOTAL_ODOMETER]!!) / 1000.0
        assertEquals(2 * Kaunas.toAkropolis.lengthM / 1000, km, 0.05)
        assertEquals(0, car.planIndex)
    }

    @Test
    fun `in a coverage gap nothing is sent, then everything arrives in one batch`() {
        val steps = oneLoop(SimulatedCar(start, noise = Noise.NONE))
        // the first second without records while the car was moving
        val gapStart = steps.indices.first { i ->
            i > 0 && steps[i].isEmpty() && steps[i - 1].singleOrNull()?.get(AvlIo.MOVEMENT) == 1L
        }
        val batch = steps.drop(gapStart).first { it.isNotEmpty() }
        assertTrue("batch of ${batch.size}", batch.size >= 20)
        assertTrue(batch.zipWithNext().all { (a, b) -> b.timestampMs - a.timestampMs == 1_000L })
    }

    @Test
    fun `without a satellite fix GPS speed is zero but OBD speed keeps working`() {
        val records = oneLoop(SimulatedCar(start, noise = Noise.NONE)).flatten()
        val noFix = records.filter { it.gps.satellites == 0 && it[AvlIo.IGNITION] == 1L }
        assertTrue(noFix.size >= 10)
        assertTrue(noFix.all { it.gps.speedKmh == 0 && it[AvlIo.GNSS_STATUS] == AvlIo.GNSS_NO_FIX })
        assertTrue(noFix.any { it[AvlIo.OBD_VEHICLE_SPEED]!! > 30 })
        assertTrue(noFix.all { it[AvlIo.GNSS_HDOP] == null })
    }

    @Test
    fun `accelerometer shows the harsh moments in milli-g`() {
        val records = oneLoop(SimulatedCar(start, noise = Noise.NONE)).flatten()
        assertTrue(records.minOf { it[AvlIo.AXIS_X]!! } <= -400)
        assertTrue(records.maxOf { it[AvlIo.AXIS_X]!! } >= 350)
        assertTrue(records.maxOf { abs(it[AvlIo.AXIS_Y]!!) } >= 500)
        assertTrue(records.all { it[AvlIo.AXIS_Z] == 1000L })
    }

    @Test
    fun `noisy positions stay within a few metres of the road`() {
        val records = oneLoop(SimulatedCar(start)).flatten().filter { it.gps.hasFix }
        assertTrue(records.all { Kaunas.map.roadAt(GeoPoint(it.gps.latitude, it.gps.longitude), 10.0) != null })
    }

    @Test
    fun `a parked car reports speed 0 and does not wander, even with GPS noise`() {
        val car = SimulatedCar(start)
        val parked = (1..120).flatMap { car.step() }
        assertTrue(parked.all { it.gps.speedKmh == 0 })
        assertEquals(1, parked.map { it.gps.latitude to it.gps.longitude }.distinct().size)
    }

    @Test
    fun `the same seed gives exactly the same records`() {
        val a = SimulatedCar(start, seed = 3)
        val b = SimulatedCar(start, seed = 3)
        repeat(600) { assertEquals(a.step(), b.step()) }
    }

    @Test
    fun `parked position is the end of the last drive`() {
        val car = SimulatedCar(start, noise = Noise.NONE)
        oneLoop(car)
        val last = (1..30).flatMap { car.step() }.last()
        val home = Kaunas.toAkropolis.from.point
        assertEquals(0.0, Geo.distanceM(home, GeoPoint(last.gps.latitude, last.gps.longitude)), 1.0)
        assertEquals(0L, last[AvlIo.IGNITION])
        assertNull(last[AvlIo.OBD_VEHICLE_SPEED])
    }

    @Test
    fun `playback emits the link state first and then batches of records`() = runTest {
        val car = SimulatedCar(start, noise = Noise.NONE)
        val updates = DevicePlayback(car).updates().take(3).toList()
        assertEquals(DeviceUpdate.Link(online = true), updates.first())
        val records = updates.drop(1).filterIsInstance<DeviceUpdate.Records>().flatMap { it.records }
        assertTrue(records.isNotEmpty())
        assertTrue(records.zipWithNext().all { (a, b) -> b.timestampMs > a.timestampMs })
    }
}
