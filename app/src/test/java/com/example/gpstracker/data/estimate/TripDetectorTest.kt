package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDetectorTest {
    private val detector = TripDetector()
    private var now = 0L
    private val events = mutableListOf<TripEvent>()

    /** Feeds one sample per second for [seconds] seconds. */
    private fun feed(seconds: Int, speed: Double, ignition: Boolean? = true) {
        repeat(seconds) {
            now += 1_000
            events += detector.onSample(TripSample(now, speed, ignition))
        }
    }

    private fun jump(ms: Long) {
        now += ms
    }

    private fun feedOne(speed: Double, ignition: Boolean? = true) {
        events += detector.onSample(TripSample(now, speed, ignition))
    }

    private inline fun <reified T : TripEvent> List<TripEvent>.only(): T = filterIsInstance<T>().single()

    @Test
    fun `normal trip with ignition starts and ends`() {
        feed(10, speed = 0.0, ignition = false)
        feed(10, speed = 0.0, ignition = true)
        assertEquals(VehicleState.Parked, detector.state)
        feed(120, speed = 50.0)
        assertEquals(VehicleState.Driving, detector.state)
        feed(40, speed = 0.0, ignition = false)

        events.only<TripEvent.Started>()
        val ended = events.only<TripEvent.Ended>()
        assertEquals(VehicleState.Parked, detector.state)
        assertEquals(50.0 * 120 / 3600, ended.distanceKm, 0.2)
        assertTrue(!ended.endedWithoutSignal)
    }

    @Test
    fun `trip without ignition data needs ten seconds of movement`() {
        feed(20, speed = 0.0, ignition = null)
        feed(5, speed = 40.0, ignition = null)
        assertEquals(VehicleState.Parked, detector.state)
        assertTrue(events.isEmpty())

        feed(10, speed = 40.0, ignition = null)
        assertEquals(VehicleState.Driving, detector.state)
        val started = events.only<TripEvent.Started>()
        // the trip begins at the first moving sample, not when the 10 s were up
        assertEquals(21_000L, started.startMs)
    }

    @Test
    fun `a short burst of movement without ignition does not start a trip`() {
        feed(5, speed = 30.0, ignition = null)
        feed(10, speed = 0.0, ignition = null)
        feed(5, speed = 30.0, ignition = null)
        assertEquals(VehicleState.Parked, detector.state)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a traffic light stop does not split the trip`() {
        feed(60, speed = 50.0)
        feed(20, speed = 0.0)
        assertEquals(VehicleState.Stopped, detector.state)
        feed(60, speed = 50.0)

        assertEquals(VehicleState.Driving, detector.state)
        events.only<TripEvent.Started>()
        assertTrue(events.none { it is TripEvent.Ended })
    }

    @Test
    fun `a long stop ends the trip at the moment the car stopped`() {
        feed(120, speed = 50.0)
        val stoppedAt = now + 1_000
        feed(6 * 60, speed = 0.0)

        val ended = events.only<TripEvent.Ended>()
        assertEquals(stoppedAt, ended.endMs)
        assertEquals(VehicleState.Parked, detector.state)
    }

    @Test
    fun `the end-after setting changes how long a stop may last`() {
        detector.setTripEndAfterMinutes(2)
        feed(120, speed = 50.0)
        feed(150, speed = 0.0)
        events.only<TripEvent.Ended>()
    }

    @Test
    fun `ignition off and on within 30 seconds keeps one trip`() {
        feed(60, speed = 50.0)
        feed(20, speed = 0.0, ignition = false)
        feed(60, speed = 50.0, ignition = true)

        assertEquals(VehicleState.Driving, detector.state)
        events.only<TripEvent.Started>()
        assertTrue(events.none { it is TripEvent.Ended })
    }

    @Test
    fun `a trip that is too short is discarded`() {
        feed(10, speed = 20.0)
        feed(40, speed = 0.0, ignition = false)

        events.only<TripEvent.Started>()
        events.only<TripEvent.Discarded>()
        assertTrue(events.none { it is TripEvent.Ended })
        assertEquals(VehicleState.Parked, detector.state)
    }

    @Test
    fun `a signal gap under ten minutes continues the same trip`() {
        feed(60, speed = 50.0)
        jump(5 * 60_000)
        feedOne(speed = 50.0)

        assertEquals(VehicleState.Driving, detector.state)
        events.only<TripEvent.Started>()
        assertTrue(events.none { it is TripEvent.Ended })
    }

    @Test
    fun `a signal gap of ten minutes or more ends the trip at the last sample`() {
        feed(120, speed = 50.0)
        val lastSample = now
        jump(11 * 60_000)
        feedOne(speed = 0.0, ignition = false)

        val ended = events.only<TripEvent.Ended>()
        assertEquals(lastSample, ended.endMs)
        assertTrue(ended.endedWithoutSignal)
        assertEquals(VehicleState.Parked, detector.state)
    }

    @Test
    fun `ticking without samples also ends a silent trip`() {
        feed(120, speed = 50.0)
        val lastSample = now
        events += detector.onTick(now + 5 * 60_000)
        assertTrue(events.none { it is TripEvent.Ended })

        events += detector.onTick(now + 10 * 60_000)
        val ended = events.only<TripEvent.Ended>()
        assertEquals(lastSample, ended.endMs)
        assertTrue(ended.endedWithoutSignal)
    }
}
