package com.example.gpstracker.data.drive

import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.ui.model.EventType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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

    @Test
    fun `saved trips are grouped by the phone's calendar days`() {
        val noon = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
        }.timeInMillis
        val hour = 3_600_000L
        assertEquals("Today", mapper.dayLabel(noon - 11 * hour, noon))
        assertEquals("Yesterday", mapper.dayLabel(noon - 13 * hour, noon))
        assertEquals(
            SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(noon - 48 * hour)),
            mapper.dayLabel(noon - 48 * hour, noon),
        )
    }

    @Test
    fun `a saved trip's summary and record use the time it was saved`() {
        val savedAt = 1_700_000_000_000L
        val trip = Trip(
            startMs = savedAt - 600_000,
            endMs = savedAt,
            distanceKm = 5.0,
            events = listOf(event(DrivingEventType.Speeding, 18.0)),
            route = emptyList(),
            maxSpeedKmh = 68.0,
            speedingMs = 90_000,
            score = 92,
            startPlace = "A",
            endPlace = "B",
            endedWithoutSignal = false,
        )
        val saved = SavedTrip("t1", savedAt, trip)
        val summary = mapper.summary(saved, nowMs = savedAt)
        assertEquals("Today", summary.dateLabel)
        assertEquals("A → B", summary.fromTo)
        assertEquals("10 min", summary.durationLabel)
        val record = mapper.record(saved)
        assertEquals(savedAt, record.startMs)
        assertEquals(1, record.speedingEvents)
        assertEquals(1.5, record.speedingMinutes, 1e-9)
    }
}
