package com.example.gpstracker.domain.reports

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.model.telemetry.GnssQuality
import com.example.gpstracker.model.telemetry.TelemetryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRadarTest {
    private val radar = ReportRadar()

    /** About 111 m per 0.001 degree of latitude: the car drives north along longitude 23.9. */
    private fun north(metres: Double) = LatLon(54.9 + metres / 111_195.0, 23.9)

    private fun report(
        id: String,
        atM: Double,
        source: ReportSource = ReportSource.OtherDriver,
        headingDeg: Double? = null,
        type: ReportType = ReportType.Police,
    ) = RoadReport(id, type, north(atM), createdAtMs = 0, expiresAtMs = Long.MAX_VALUE, source = source, headingDeg = headingDeg)

    private fun carAt(m: Double, heading: Double = 0.0, speedMps: Double = 14.0, gnss: GnssQuality = GnssQuality.Good) =
        north(m).let { TelemetryPoint(0, it.latitude, it.longitude, speedMps, headingDeg = heading, gnss = gnss) }

    @Test
    fun `warns about the nearest report ahead within 500 m`() {
        val reports = listOf(report("far", 900.0), report("near", 400.0), report("mid", 450.0))
        val ahead = radar.update(carAt(0.0), reports).ahead!!
        assertEquals("near", ahead.report.id)
        assertEquals(400.0, ahead.distanceM, 2.0)
    }

    @Test
    fun `ignores reports behind, too far or off to the side`() {
        val side = RoadReport("side", ReportType.Hazard, LatLon(54.9, 23.904), 0, Long.MAX_VALUE, ReportSource.OtherDriver)
        assertNull(radar.update(carAt(0.0), listOf(report("behind", -200.0), report("far", 800.0), side)).ahead)
    }

    @Test
    fun `my own reports are not announced back to me`() {
        assertNull(radar.update(carAt(0.0), listOf(report("mine", 300.0, ReportSource.Me))).ahead)
    }

    @Test
    fun `reports from drivers going the other way are ignored`() {
        val oncoming = report("oncoming", 300.0, headingDeg = 180.0)
        val sameWay = report("same", 350.0, headingDeg = 10.0)
        assertEquals("same", radar.update(carAt(0.0), listOf(oncoming, sameWay)).ahead!!.report.id)
    }

    @Test
    fun `after driving past a report it asks once whether it is still there`() {
        val reports = listOf(report("police", 300.0))
        val results = (0..450 step 10).map { radar.update(carAt(it.toDouble()), reports) }
        val asked = results.mapNotNull { it.passed }
        assertEquals(listOf("police"), asked.map { it.id })
        // warned while approaching, quiet afterwards
        assertTrue(results.first().ahead != null)
        assertNull(results.last().ahead)
    }

    @Test
    fun `a report never seen ahead is not asked about`() {
        // the car starts already past it
        val reports = listOf(report("police", 300.0))
        val asked = (320..500 step 10).mapNotNull { radar.update(carAt(it.toDouble()), reports).passed }
        assertTrue(asked.isEmpty())
    }

    @Test
    fun `while standing or without GPS the last warning stays`() {
        val reports = listOf(report("jam", 200.0, type = ReportType.TrafficJam))
        radar.update(carAt(0.0), reports)
        // heading of a standing car is meaningless; turning "around" must not clear the warning
        assertEquals("jam", radar.update(carAt(0.0, heading = 180.0, speedMps = 0.0), reports).ahead!!.report.id)
        assertEquals("jam", radar.update(carAt(0.0, gnss = GnssQuality.Lost), reports).ahead!!.report.id)
    }

    @Test
    fun `reset forgets what was asked, so the next trip asks again`() {
        val reports = listOf(report("police", 300.0))
        (0..450 step 10).forEach { radar.update(carAt(it.toDouble()), reports) }
        radar.reset()
        val asked = (0..450 step 10).mapNotNull { radar.update(carAt(it.toDouble()), reports).passed }
        assertEquals(1, asked.size)
    }
}
