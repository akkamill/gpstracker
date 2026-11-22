package com.example.gpstracker.data.telematics

import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.telematics.AvlTelemetryMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End to end without the UI: simulated tracker records → mapper → domain pipeline.
 * The simulator plans one incident of each kind on the way there and none on the way
 * back; the pipeline has to find exactly those in the raw, noisy data.
 */
class SimulatedDrivePipelineTest {

    /** Drives the demo loop until the trips there and back are finished, and returns them. */
    private fun dayOfTrips(seed: Long): List<Trip> {
        val car = SimulatedCar(startMs = 1_700_000_000_000L, seed = seed)
        val tracker = DriveTracker(map = SimulatorMapLookup())
        val trips = mutableListOf<Trip>()
        var seconds = 0
        while (trips.size < 2) {
            check(++seconds < MAX_S) { "two trips did not finish within $MAX_S s" }
            car.step().forEach { record ->
                tracker.onPoint(AvlTelemetryMapper.toTelemetry(record)).finishedTrip?.let(trips::add)
            }
        }
        return trips
    }

    @Test
    fun `the drive there finds exactly the planned events`() {
        val there = dayOfTrips(seed = 7).first()
        assertEquals(
            listOf(
                DrivingEventType.HarshBraking,
                DrivingEventType.HarshAcceleration,
                DrivingEventType.Speeding,
                DrivingEventType.SharpCorner,
            ),
            there.events.map { it.type },
        )
        assertEquals("Žaliakalnis", there.startPlace)
        assertEquals("Akropolis", there.endPlace)
        assertEquals(Kaunas.toAkropolis.lengthM / 1000, there.distanceKm, 0.1)
        assertTrue("score ${there.score}", there.score in 60..84)
        assertTrue(there.speedingMs > 0)
        assertFalse(there.endedWithoutSignal)
    }

    @Test
    fun `events carry the road and limit where they happened`() {
        val there = dayOfTrips(seed = 7).first()
        val speeding = there.events.single { it.type == DrivingEventType.Speeding }
        assertEquals("Vytauto pr.", speeding.roadName)
        assertEquals(50, speeding.speedLimitKmh)
        assertEquals("Savanorių pr.", there.events.first { it.type == DrivingEventType.HarshBraking }.roadName)
    }

    @Test
    fun `the calm drive back has no events and a full score`() {
        val back = dayOfTrips(seed = 7)[1]
        assertTrue(back.events.isEmpty())
        assertEquals(100, back.score)
        assertEquals("Akropolis", back.startPlace)
        assertEquals("Žaliakalnis", back.endPlace)
    }

    @Test
    fun `results do not depend on the sensor noise`() {
        listOf(1L, 2L, 3L, 42L).forEach { seed ->
            val trips = dayOfTrips(seed)
            assertEquals("seed $seed", 2, trips.size)
            assertEquals("seed $seed", 4, trips[0].events.size)
            assertEquals("seed $seed", 0, trips[1].events.size)
        }
    }

    @Test
    fun `the GPS outage does not break the trip or lose distance`() {
        val there = dayOfTrips(seed = 7).first()
        // speed comes from OBD during the outage, so distance matches the road length
        assertEquals(Kaunas.toAkropolis.lengthM / 1000, there.distanceKm, 0.1)
        // but the route has a hole where there was no fix
        assertTrue(there.route.size < (there.durationMs / 1000).toInt())
    }

    private companion object {
        /** Car-time limit for one loop, so a broken simulation fails instead of hanging. */
        const val MAX_S = 5_000
    }
}
