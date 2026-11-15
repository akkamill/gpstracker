package com.example.gpstracker.data.reports

import com.example.gpstracker.data.drive.TelemetryDriveRepository
import com.example.gpstracker.data.telematics.SimulatedTelemetrySource
import com.example.gpstracker.data.telematics.SimulatorMapLookup
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.simulator.vehicle.CommunityReports
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoadReportServiceTest {
    private var now = 0L
    private val repository = InMemoryReportRepository(clock = { now })
    private val service = RoadReportService(repository, clock = { now })

    private fun north(m: Double) = LatLon(54.9 + m / 111_195.0, 23.9)

    private fun point(m: Double, speedMps: Double = 14.0) =
        north(m).let { TelemetryPoint(now, it.latitude, it.longitude, speedMps, headingDeg = 0.0) }

    private fun police(atM: Double) =
        RoadReport("police", ReportType.Police, north(atM), 0, Long.MAX_VALUE, ReportSource.OtherDriver, roadName = "Main st.")

    @Test
    fun `reporting needs a known position and places the report where the car is`() {
        assertFalse(service.report(ReportType.Hazard))
        assertFalse(service.state.value.canReport)

        service.onPoint(point(100.0), "Main st.", inTrip = true)
        assertTrue(service.report(ReportType.Hazard))

        val mine = repository.reports.value.single()
        assertEquals(ReportType.Hazard, mine.type)
        assertEquals(ReportSource.Me, mine.source)
        assertEquals(north(100.0), mine.position)
        assertEquals("Main st.", mine.roadName)
        assertEquals(0.0, mine.headingDeg!!, 1e-9)
        assertTrue(service.state.value.canReport)
    }

    @Test
    fun `warns ahead, then asks after passing, and a no removes the report`() {
        repository.add(police(400.0))
        service.onPoint(point(0.0), null, inTrip = true)
        assertEquals("police", service.state.value.ahead?.report?.id)

        (10..520 step 10).forEach { service.onPoint(point(it.toDouble()), null, inTrip = true) }
        assertEquals("police", service.state.value.askStillThere?.id)
        assertNull(service.state.value.ahead)

        service.answer("police", stillThere = false)
        assertNull(service.state.value.askStillThere)
        assertTrue(service.state.value.reports.isEmpty())
    }

    @Test
    fun `an unanswered question goes away after a while`() {
        repository.add(police(300.0))
        (0..450 step 10).forEach { service.onPoint(point(it.toDouble()), null, inTrip = true) }
        assertEquals("police", service.state.value.askStillThere?.id)
        now += 20_000
        service.onPoint(point(460.0), null, inTrip = true)
        assertNull(service.state.value.askStillThere)
    }

    @Test
    fun `no warnings while parked`() {
        repository.add(police(200.0))
        service.onPoint(point(0.0), null, inTrip = false)
        assertNull(service.state.value.ahead)
    }

    @Test
    fun `expired reports disappear`() {
        repository.add(police(200.0).copy(expiresAtMs = 1_000))
        now = 2_000
        service.onPoint(point(0.0), null, inTrip = true)
        assertTrue(service.state.value.reports.isEmpty())
    }

    @Test
    fun `on the demo drive the app warns about each report on the way and asks about it`() = runTest {
        val start = 1_700_000_000_000L
        val demo = CommunityReports.demo(start)
        val reports = RoadReportService(InMemoryReportRepository(demo, clock = { start }), clock = { testScheduler.currentTime })
        val drive = TelemetryDriveRepository(
            source = SimulatedTelemetrySource(SimulatedCar(startMs = start)),
            tracker = DriveTracker(map = SimulatorMapLookup()),
            settings = MutableStateFlow(SettingsUiState()),
            clock = { testScheduler.currentTime },
            reports = reports,
        )
        val states = mutableListOf<ReportsState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reports.state.toList(states) }
        drive.start(backgroundScope)
        advanceTimeBy(100_000) // the drive there

        val warned = states.mapNotNull { it.ahead?.report?.type }.toSet()
        val asked = states.mapNotNull { it.askStillThere?.type }.toSet()
        // only the reports on the way there, not the ones facing the other way
        assertEquals(setOf(ReportType.Police, ReportType.Hazard), warned)
        assertEquals(setOf(ReportType.Police, ReportType.Hazard), asked)
    }
}
