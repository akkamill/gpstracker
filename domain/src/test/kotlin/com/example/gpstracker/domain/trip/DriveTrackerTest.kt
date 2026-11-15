package com.example.gpstracker.domain.trip

import com.example.gpstracker.domain.driving.DrivingRules
import com.example.gpstracker.domain.map.MapLookup
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.map.RoadInfo
import com.example.gpstracker.model.telemetry.GnssQuality
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.model.trip.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveTrackerTest {
    /** A straight road going north: 50 km/h below latitude 54.98, 30 km/h above. */
    private val map = object : MapLookup {
        override fun roadAt(latitude: Double, longitude: Double) =
            if (latitude < 54.98) RoadInfo("Main street", 50) else RoadInfo("School street", 30)

        override fun placeName(latitude: Double, longitude: Double) = when {
            latitude < 54.901 -> "Home"
            latitude in 54.955..54.98 -> "Office"
            else -> null
        }
    }

    private val tracker = DriveTracker(DrivingRules(), map)
    private var now = 0L
    private var lat = 54.9
    private val updates = mutableListOf<DriveUpdate>()

    private fun drive(
        seconds: Int,
        kmh: Double,
        ignition: Boolean? = true,
        accel: Double = 0.0,
        gnss: GnssQuality = GnssQuality.Good,
    ) = repeat(seconds) {
        now += 1_000
        lat += kmh / 3.6 / 111_000 // metres north per second
        updates += tracker.onPoint(
            TelemetryPoint(now, lat, 23.9, kmh / 3.6, accelerationMps2 = accel, ignitionOn = ignition, gnss = gnss),
        )
    }

    private val finished get() = updates.mapNotNull { it.finishedTrip }
    private val allEvents get() = updates.flatMap { it.newEvents }

    @Test
    fun `a clean trip starts, ends and gets a full score with place names`() {
        drive(10, 0.0, ignition = false)
        drive(5, 0.0)
        drive(600, 40.0) // about 6.7 km
        drive(40, 0.0, ignition = false)

        val trip = finished.single()
        assertEquals(100, trip.score)
        assertEquals(6.7, trip.distanceKm, 0.1)
        assertEquals("Home", trip.startPlace)
        assertEquals("Office", trip.endPlace)
        assertTrue(trip.events.isEmpty())
        assertTrue(trip.route.size >= 600)
        assertEquals(40.0, trip.avgSpeedKmh, 2.0)
        assertEquals(VehicleState.Parked, updates.last().snapshot.vehicleState)
        assertNull(updates.last().snapshot.trip)
    }

    @Test
    fun `live progress shows the trip while driving`() {
        drive(120, 50.0)
        val snapshot = updates.last().snapshot
        assertEquals(VehicleState.Driving, snapshot.vehicleState)
        val progress = snapshot.trip!!
        assertEquals(1.67, progress.distanceKm, 0.05)
        assertEquals("Main street", snapshot.road?.name)
        assertEquals(100, progress.score)
    }

    @Test
    fun `events are found during the trip and lower the score`() {
        drive(60, 45.0)
        drive(1, 45.0, accel = -4.5)
        drive(60, 45.0)
        val trip = tracker.snapshot().trip!!
        assertEquals(listOf(DrivingEventType.HarshBraking), trip.events.map { it.type })
        assertTrue(trip.score < 100)
        assertEquals(1, allEvents.size)
    }

    @Test
    fun `keeping the speed into a lower limit is speeding, and the time over is counted`() {
        drive(1_000, 45.0) // crosses into the 30 zone at 54.98
        val trip = tracker.snapshot().trip!!
        val speeding = trip.events.single { it.type == DrivingEventType.Speeding }
        assertEquals(30, speeding.speedLimitKmh)
        assertEquals("School street", speeding.roadName)
        assertTrue(trip.speedingMs > 60_000)
    }

    @Test
    fun `without a GPS fix the trip continues but the score pauses`() {
        drive(30, 40.0)
        val routeBefore = tracker.snapshot().trip!!.route.size
        drive(20, 40.0, accel = -5.0, gnss = GnssQuality.Lost)
        val during = tracker.snapshot()
        assertEquals(VehicleState.Driving, during.vehicleState)
        assertEquals(routeBefore, during.trip!!.route.size)
        assertTrue(during.trip!!.events.isEmpty())
        drive(30, 40.0)
        assertTrue(tracker.snapshot().trip!!.route.size > routeBefore)
    }

    @Test
    fun `nothing is recorded while parked`() {
        drive(120, 0.0, ignition = false)
        assertTrue(updates.all { it.snapshot.trip == null && it.newEvents.isEmpty() })
    }

    @Test
    fun `a silent car ends its trip on a tick`() {
        drive(300, 40.0)
        assertNull(tracker.onTick(now + 60_000))
        val update = tracker.onTick(now + 11 * 60_000)!!
        assertTrue(update.finishedTrip!!.endedWithoutSignal)
        assertEquals(VehicleState.Parked, update.snapshot.vehicleState)
    }

    @Test
    fun `a new trip starts with fresh events`() {
        drive(60, 45.0)
        drive(1, 45.0, accel = -4.5)
        drive(60, 45.0)
        drive(40, 0.0, ignition = false)
        drive(5, 0.0)
        drive(120, 40.0)
        assertEquals(1, finished.size)
        assertTrue(tracker.snapshot().trip!!.events.isEmpty())
    }
}
