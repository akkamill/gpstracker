package com.example.gpstracker.data.reports

import com.example.gpstracker.domain.reports.ReportAhead
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.ReportKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportUiMapperTest {
    private val mapper = ReportUiMapper()
    private val report = RoadReport("r", ReportType.SpeedCamera, LatLon(54.9, 23.9), 0, 1, ReportSource.Me, roadName = "Jonavos g.")

    @Test
    fun `distances are rounded for a quick glance`() {
        assertEquals("80 m", mapper.distanceLabel(83.0))
        assertEquals("400 m", mapper.distanceLabel(398.0))
        assertEquals("1.2 km", mapper.distanceLabel(1_234.0))
    }

    @Test
    fun `every report type has a matching kind on screen`() {
        ReportType.entries.forEach { assertEquals(it, mapper.type(mapper.kind(it))) }
    }

    @Test
    fun `report state is added to the drive state`() {
        val state = mapper.apply(
            DriveUiState(),
            ReportsState(reports = listOf(report), ahead = ReportAhead(report, 250.0), canReport = true),
        )
        assertEquals(ReportKind.SpeedCamera, state.reports.single().kind)
        assertTrue(state.reports.single().mine)
        assertEquals("250 m", state.reportAhead?.distanceLabel)
        assertEquals("Jonavos g.", state.reportAhead?.roadLabel)
        assertNull(state.stillThere)
        assertTrue(state.canReport)
    }
}
