package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.telemetry.TelemetryPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DrivingSessionTrackerTest {
    private var clock = 0L

    private fun point(speedMps: Double = 10.0, accel: Double = 0.0): TelemetryPoint {
        clock += 1000
        return TelemetryPoint(clock, 40.4, 49.8, speedMps, accelerationMps2 = accel)
    }

    @Test
    fun `smooth driving keeps full score`() {
        val tracker = DrivingSessionTracker()
        repeat(10) { tracker.onPoint(point(accel = 0.5)) }
        assertEquals(DrivingSession.MAX_SCORE, tracker.onPoint(point()).score)
    }

    @Test
    fun `one sustained hard stop counts as one event`() {
        val tracker = DrivingSessionTracker()
        repeat(5) { tracker.onPoint(point(accel = -4.0)) }
        val session = tracker.onPoint(point(accel = 0.0))
        assertEquals(listOf(DrivingEventType.HARD_BRAKING), session.events.map { it.type })
        assertEquals(95, session.score)
    }

    @Test
    fun `new event after the exit threshold is crossed`() {
        val tracker = DrivingSessionTracker()
        tracker.onPoint(point(accel = -4.0))
        tracker.onPoint(point(accel = -1.0))
        val session = tracker.onPoint(point(accel = -4.0))
        assertEquals(2, session.events.size)
    }

    @Test
    fun `speeding above 90 km h is flagged`() {
        val tracker = DrivingSessionTracker()
        val session = tracker.onPoint(point(speedMps = 28.0))
        assertEquals(DrivingEventType.SPEEDING, session.events.single().type)
    }

    @Test
    fun `distance accumulates from speed and elapsed time`() {
        val tracker = DrivingSessionTracker()
        tracker.onPoint(point(speedMps = 10.0))
        val session = tracker.onPoint(point(speedMps = 10.0))
        assertEquals(10.0, session.distanceMeters, 1e-9)
    }

    @Test
    fun `score never goes below zero`() {
        val tracker = DrivingSessionTracker()
        var session = DrivingSession()
        repeat(40) {
            session = tracker.onPoint(point(accel = -4.0))
            session = tracker.onPoint(point(accel = 0.0))
        }
        assertEquals(0, session.score)
    }
}
