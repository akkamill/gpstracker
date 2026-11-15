package com.example.gpstracker.model

import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.model.trip.Trip
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The models travel between the phone and the server as JSON; this checks they survive the trip. */
class JsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val trip = Trip(
        startMs = 1_000,
        endMs = 361_000,
        distanceKm = 4.9,
        events = listOf(
            DrivingEvent(DrivingEventType.Speeding, 120_000, LatLon(54.89, 23.89), 18.0, 68.0, 4, 50, "Jonavos g."),
        ),
        route = listOf(LatLon(54.9068, 23.952), LatLon(54.896, 23.888)),
        maxSpeedKmh = 68.0,
        speedingMs = 30_000,
        score = 76,
        startPlace = "Žaliakalnis",
        endPlace = "Akropolis",
        endedWithoutSignal = false,
    )

    @Test
    fun `a trip survives a round trip through JSON`() {
        val text = json.encodeToString(Trip.serializer(), trip)
        assertTrue(text.contains("\"type\":\"Speeding\""))
        assertEquals(trip, json.decodeFromString(Trip.serializer(), text))
    }

    @Test
    fun `computed values are not sent, only the data`() {
        val text = json.encodeToString(Trip.serializer(), trip)
        assertTrue("durationMs" !in text && "avgSpeedKmh" !in text)
        assertEquals(49.0, json.decodeFromString(Trip.serializer(), text).avgSpeedKmh, 0.1)
    }

    @Test
    fun `defaults are filled in when a sender leaves them out`() {
        val point = json.decodeFromString(
            TelemetryPoint.serializer(),
            """{"timestampMs":5,"latitude":54.9,"longitude":23.9,"speedMps":10.0}""",
        )
        assertEquals(null, point.ignitionOn)
        assertEquals(36.0, point.speedKmh, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid data is rejected when it arrives`() {
        json.decodeFromString(
            TelemetryPoint.serializer(),
            """{"timestampMs":5,"latitude":95.0,"longitude":23.9,"speedMps":10.0}""",
        )
    }

    @Test
    fun `a road report survives a round trip through JSON`() {
        val report =
            RoadReport("r1", ReportType.Police, LatLon(54.9, 23.9), 1_000, 1_801_000, ReportSource.OtherDriver, 245.0, "Savanorių pr.")
        val text = json.encodeToString(RoadReport.serializer(), report)
        assertTrue(text.contains("\"type\":\"Police\""))
        assertEquals(report, json.decodeFromString(RoadReport.serializer(), text))
    }

    @Test(expected = SerializationException::class)
    fun `missing required fields are rejected`() {
        json.decodeFromString(TelemetryPoint.serializer(), """{"timestampMs":5}""")
    }
}
