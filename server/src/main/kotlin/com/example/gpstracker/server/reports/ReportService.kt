package com.example.gpstracker.server.reports

import com.example.gpstracker.domain.reports.ReportRules
import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.api.NewReportRequest
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.server.ApiException
import com.example.gpstracker.tracing.Tracing
import io.ktor.http.HttpStatusCode
import java.util.UUID

/**
 * Road reports shared between drivers. The lifetimes and vote rules are the same [ReportRules]
 * the app uses, from `:domain`. Other drivers never learn who made a report.
 */
class ReportService(
    private val store: ReportStore,
    private val rules: ReportRules = ReportRules(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val tracer = Tracing.tracer("server")

    suspend fun create(request: NewReportRequest, reporterId: UUID): RoadReport {
        validate(request.id.isNotBlank() && request.id.length <= MAX_ID, "A report needs an id of at most $MAX_ID characters.")
        validate(request.position.isValid, "The position is not on the map.")
        validate((request.roadName?.length ?: 0) <= MAX_ROAD_NAME, "The road name is too long.")
        validate(request.headingDeg?.let { it in 0.0..FULL_CIRCLE } ?: true, "The heading must be between 0 and 360 degrees.")
        val report = rules.create(request.type, request.position, clock(), request.headingDeg, request.roadName, request.id)
        val stored = store.insert(report, reporterId)
        tracer.info("road report shared", "type" to request.type, "road" to request.roadName)
        return stored.seenBy(reporterId)
    }

    /** Active reports, around [near] when given; the asking driver's own ones are marked as theirs. */
    suspend fun active(viewer: UUID?, near: LatLon?): List<RoadReport> =
        store.active(clock(), near?.let { Area(it, RADIUS_KM) }, MAX_RESULTS).map { it.seenBy(viewer) }

    suspend fun vote(id: String, voter: UUID, stillThere: Boolean): RoadReport {
        val now = clock()
        val stored = store.vote(id, voter, stillThere) { rules.vote(it, stillThere, now) }
            ?: throw ApiException(HttpStatusCode.NotFound, ApiError(ApiError.NOT_FOUND, "This report no longer exists."))
        tracer.info("road report answered", "stillThere" to stillThere, "active" to stored.report.isActive(now))
        return stored.seenBy(voter)
    }

    /**
     * Puts the demo reports back when they are missing, expired or voted away, so the demo drive
     * always meets some. Returns how many were added.
     */
    suspend fun refreshDemoReports(demo: List<RoadReport>): Int {
        val now = clock()
        val missing = demo.filter { store.find(it.id)?.report?.isActive(now) != true }
        missing.forEach { store.replace(it) }
        if (missing.isNotEmpty()) tracer.info("demo road reports added", "count" to missing.size)
        return missing.size
    }

    private fun StoredReport.seenBy(viewer: UUID?) =
        report.copy(source = if (viewer != null && viewer == reporterId) ReportSource.Me else ReportSource.OtherDriver)

    private fun validate(ok: Boolean, message: String) {
        if (!ok) throw ApiException(HttpStatusCode.BadRequest, ApiError(ApiError.INVALID_INPUT, message))
    }

    private val LatLon.isValid get() = latitude in -MAX_LAT..MAX_LAT && longitude in -MAX_LON..MAX_LON

    companion object {
        /** Reports this far from the driver are sent. */
        const val RADIUS_KM = 30.0
        const val MAX_RESULTS = 500
        private const val MAX_ID = 64
        private const val MAX_ROAD_NAME = 100
        private const val FULL_CIRCLE = 360.0
        private const val MAX_LAT = 90.0
        private const val MAX_LON = 180.0
    }
}
