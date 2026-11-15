package com.example.gpstracker.data.drive

import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.ui.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveUiMapperTest {
    private val mapper = DriveUiMapper()

    private fun event(type: DrivingEventType, peak: Double, road: String? = "Jonavos g.") =
        DrivingEvent(type, 0L, LatLon(54.9, 23.9), peak, speedKmh = 68.0, penalty = 4, speedLimitKmh = 50, roadName = road)

    @Test
    fun `events get readable titles and details`() {
        assertEquals("68 in a 50 zone", mapper.event(event(DrivingEventType.Speeding, 18.0)).detail)
        assertEquals("−4.6 m/s²", mapper.event(event(DrivingEventType.HarshBraking, 4.6)).detail)
        assertEquals("+3.8 m/s²", mapper.event(event(DrivingEventType.HarshAcceleration, 3.8)).detail)
        assertEquals("0.6 g lateral", mapper.event(event(DrivingEventType.SharpCorner, 5.6)).detail)
        val crash = mapper.event(event(DrivingEventType.CrashSuspected, 34.0))
        assertEquals(EventType.CrashSuspected, crash.type)
        assertEquals("Possible crash", crash.title)
    }

    @Test
    fun `place falls back when the road is unknown`() {
        assertEquals("Unknown road", mapper.event(event(DrivingEventType.HarshBraking, 4.0, road = null)).placeLabel)
    }

    @Test
    fun `tips name the road and do not end with a double full stop`() {
        val tips = mapper.tips(listOf(event(DrivingEventType.Speeding, 18.0)))
        assertEquals(listOf("Keep to the posted limit, especially on Jonavos g."), tips)
        assertEquals(
            "Keep to the posted limit, especially on Savanorių pr.",
            mapper.tips(listOf(event(DrivingEventType.Speeding, 18.0, road = "Savanorių pr."))).single(),
        )
    }

    @Test
    fun `at most three tips`() {
        val all = DrivingEventType.entries.map { event(it, 30.0) }
        assertEquals(3, mapper.tips(all).size)
    }
}
