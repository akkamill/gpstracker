package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.map.RoadInfo
import com.example.gpstracker.domain.telemetry.TelemetryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDetectorTest {
    private val detector = EventDetector()
    private var now = 0L
    private val street50 = RoadInfo("Savanorių pr.", 50)

    /** One sample per second. */
    private fun feed(
        seconds: Int,
        kmh: Double = 40.0,
        accel: Double = 0.0,
        lateral: Double = 0.0,
        road: RoadInfo? = street50,
    ): List<DrivingEvent> = (1..seconds).flatMap {
        now += 1_000
        detector.onPoint(TelemetryPoint(now, 54.9, 23.9, kmh / 3.6, accelerationMps2 = accel, lateralAccelerationMps2 = lateral), road)
    }

    @Test
    fun `smooth driving produces no events`() {
        assertTrue(feed(60, accel = 1.5, lateral = 2.0).isEmpty())
        assertTrue(feed(60, kmh = 55.0).isEmpty()) // within the 5 km/h tolerance
    }

    @Test
    fun `one sustained hard stop is one harsh braking event with its peak`() {
        val events = feed(2, accel = -4.0) + feed(1, accel = -5.2) + feed(2, accel = -3.0) + feed(3)
        val braking = events.single()
        assertEquals(DrivingEventType.HarshBraking, braking.type)
        assertEquals(4.0, braking.peak, 1e-9)
        assertEquals(3, braking.penalty)
    }

    @Test
    fun `braking again after easing off is a second event`() {
        val events = feed(1, accel = -4.0) + feed(2, accel = -0.5) + feed(1, accel = -4.0)
        assertEquals(2, events.size)
    }

    @Test
    fun `hard acceleration and sharp corners are detected in either direction`() {
        assertEquals(DrivingEventType.HarshAcceleration, feed(1, accel = 3.6).single().type)
        assertEquals(DrivingEventType.SharpCorner, feed(1, lateral = -5.5).single().type)
        feed(2)
        assertEquals(DrivingEventType.SharpCorner, feed(1, lateral = 5.5).single().type)
    }

    @Test
    fun `speeding must last three seconds and remembers the limit and road`() {
        assertTrue(feed(3, kmh = 62.0).isEmpty())
        val speeding = feed(1, kmh = 66.0).single()
        assertEquals(DrivingEventType.Speeding, speeding.type)
        assertEquals(16.0, speeding.peak, 1e-9)
        assertEquals(50, speeding.speedLimitKmh)
        assertEquals("Savanorių pr.", speeding.roadName)
        // still speeding: no second event
        assertTrue(feed(30, kmh = 66.0).isEmpty())
    }

    @Test
    fun `speeding is not detected where the limit is unknown`() {
        assertTrue(feed(30, kmh = 120.0, road = null).isEmpty())
        assertTrue(feed(30, kmh = 120.0, road = RoadInfo("Field track", null)).isEmpty())
    }

    @Test
    fun `the tolerance setting changes what counts as speeding`() {
        detector.rules = DrivingRules(speedingToleranceKmh = 0)
        assertEquals(DrivingEventType.Speeding, feed(5, kmh = 53.0).single().type)
        feed(5, kmh = 40.0)
        detector.rules = DrivingRules(speedingToleranceKmh = 10)
        assertTrue(feed(10, kmh = 58.0).isEmpty())
    }

    @Test
    fun `a crash is reported as a crash, not as harsh braking`() {
        val events = feed(1, kmh = 0.0, accel = -34.0)
        assertEquals(listOf(DrivingEventType.CrashSuspected), events.map { it.type })
    }

    @Test
    fun `score falls with penalties and is fairer on longer trips`() {
        assertEquals(100, Scoring.score(0, 10.0))
        assertEquals(70, Scoring.score(3, 0.5)) // short trips count as 1 km
        assertEquals(97, Scoring.score(3, 10.0))
        assertEquals(0, Scoring.score(50, 1.0))
    }
}
