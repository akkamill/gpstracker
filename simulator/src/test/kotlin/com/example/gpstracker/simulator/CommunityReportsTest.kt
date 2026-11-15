package com.example.gpstracker.simulator

import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.simulator.road.GeoPoint
import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.vehicle.CommunityReports
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityReportsTest {
    private val reports = CommunityReports.demo(nowMs = 1_000)

    @Test
    fun `demo reports lie on the roads they name`() {
        reports.forEach { r ->
            val road = Kaunas.map.roadAt(GeoPoint(r.position.latitude, r.position.longitude), maxDistanceM = 5.0)
            assertEquals(r.roadName, road?.name)
        }
    }

    @Test
    fun `demo reports come from other drivers and last the whole demo`() {
        assertEquals(4, reports.size)
        assertTrue(reports.all { it.source == ReportSource.OtherDriver && it.isActive(1_000 + 3_600_000) })
        assertEquals(reports.size, reports.map { it.id }.distinct().size)
    }

    @Test
    fun `each report faces the direction of its route`() {
        val headings = reports.map { it.headingDeg!! }
        // Two on the way there (heading roughly south-west), two on the way back (north-east)
        assertTrue(headings.count { it in 180.0..270.0 } >= 1)
        assertTrue(headings.count { it in 0.0..90.0 } >= 1)
    }
}
