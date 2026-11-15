package com.example.gpstracker.data.reports

import com.example.gpstracker.domain.reports.ReportAhead
import com.example.gpstracker.domain.reports.ReportRadar
import com.example.gpstracker.domain.reports.ReportRules
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.model.telemetry.TelemetryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the driver should see about reports right now.
 *
 * @property reports all active reports (for the map)
 * @property ahead the nearest report on the road ahead
 * @property askStillThere a report the car just passed; cleared after an answer or a timeout
 * @property canReport true once the car's position is known
 */
data class ReportsState(
    val reports: List<RoadReport> = emptyList(),
    val ahead: ReportAhead? = null,
    val askStillThere: RoadReport? = null,
    val canReport: Boolean = false,
)

/**
 * Connects the live drive to road reports: runs the [ReportRadar] on every sample, creates
 * a report where the car is when the driver taps one, and records "still there" answers.
 */
class RoadReportService(
    private val repository: ReportRepository,
    private val rules: ReportRules = ReportRules(),
    private val radar: ReportRadar = ReportRadar(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val askForMs: Long = ASK_FOR_MS,
) {
    private val lock = Any()
    private var lastFix: TelemetryPoint? = null
    private var lastRoad: String? = null
    private var tripInProgress = false
    private var ahead: ReportAhead? = null
    private var asking: RoadReport? = null
    private var askedAtMs = 0L

    private val _state = MutableStateFlow(ReportsState(reports = repository.reports.value))
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    /** Call for every sample, with the road the car is on and whether a trip is in progress. */
    fun onPoint(point: TelemetryPoint, roadName: String?, inTrip: Boolean) = synchronized(lock) {
        if (inTrip && !tripInProgress) radar.reset()
        tripInProgress = inTrip
        if (point.hasPosition) {
            lastFix = point
            lastRoad = roadName
        }
        repository.removeInactive()
        val active = repository.reports.value
        if (inTrip) {
            val result = radar.update(point, active)
            ahead = result.ahead
            result.passed?.let {
                asking = it
                askedAtMs = clock()
            }
        } else {
            ahead = null
        }
        if (asking != null && clock() - askedAtMs > askForMs) asking = null
        publish()
    }

    /** The driver reports [type] where the car is now. Returns false when the position is unknown. */
    fun report(type: ReportType): Boolean = synchronized(lock) {
        val fix = lastFix ?: return false
        repository.add(
            rules.create(
                type = type,
                position = fix.position,
                nowMs = clock(),
                headingDeg = fix.headingDeg.takeIf { fix.speedMps > MOVING_MPS },
                roadName = lastRoad,
            ),
        )
        publish()
        true
    }

    /** The driver's answer to "still there?". */
    fun answer(id: String, stillThere: Boolean) = synchronized(lock) {
        repository.vote(id, stillThere)
        if (asking?.id == id) asking = null
        publish()
    }

    private fun publish() {
        val active = repository.reports.value
        _state.value = ReportsState(
            reports = active,
            // a report voted away disappears from the warning at once
            ahead = ahead?.takeIf { a -> active.any { it.id == a.report.id } },
            askStillThere = asking,
            canReport = lastFix != null,
        )
    }

    private companion object {
        /** The "still there?" question stays this long before it goes away unanswered. */
        const val ASK_FOR_MS = 15_000L
        const val MOVING_MPS = 2.0
    }
}
