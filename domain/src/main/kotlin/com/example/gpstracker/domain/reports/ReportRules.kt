package com.example.gpstracker.domain.reports

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import java.util.UUID

private const val MINUTE_MS = 60_000L

/** How long each kind of report usually stays true, in minutes. */
private val DEFAULT_LIFETIME_MINUTES = mapOf(
    ReportType.Accident to 60L,
    ReportType.Police to 30L,
    ReportType.Hazard to 45L,
    ReportType.TrafficJam to 20L,
    ReportType.RoadClosed to 180L,
    ReportType.SpeedCamera to 240L,
)

/**
 * How long reports live and how votes change them. All in one place so the numbers
 * can be explained and tuned.
 *
 * Lifetimes follow how long such things usually last: a police check moves on within
 * half an hour, a closed road stays closed for hours.
 */
data class ReportRules(
    val lifetimeMinutes: Map<ReportType, Long> = DEFAULT_LIFETIME_MINUTES,
    /** A "still there" answer keeps the report at least this long from now. */
    val stillThereExtendsMinutes: Long = 15,
) {
    fun lifetimeMs(type: ReportType): Long = (lifetimeMinutes[type] ?: DEFAULT_LIFETIME_MIN) * MINUTE_MS

    /** A new report from this phone's driver. */
    fun create(
        type: ReportType,
        position: LatLon,
        nowMs: Long,
        headingDeg: Double? = null,
        roadName: String? = null,
        id: String = UUID.randomUUID().toString(),
    ) = RoadReport(
        id = id,
        type = type,
        position = position,
        createdAtMs = nowMs,
        expiresAtMs = nowMs + lifetimeMs(type),
        source = ReportSource.Me,
        headingDeg = headingDeg,
        roadName = roadName,
    )

    /**
     * Applies one driver's answer. "Still there" counts as a vote and keeps the report alive
     * a while longer; "gone" counts against it. Check [RoadReport.isActive] afterwards: a report
     * with more "gone" than "still there" votes is no longer shown.
     */
    fun vote(report: RoadReport, stillThere: Boolean, nowMs: Long): RoadReport = if (stillThere) {
        report.copy(
            stillThereVotes = report.stillThereVotes + 1,
            expiresAtMs = maxOf(report.expiresAtMs, nowMs + stillThereExtendsMinutes * MINUTE_MS),
        )
    } else {
        report.copy(goneVotes = report.goneVotes + 1)
    }

    private companion object {
        const val DEFAULT_LIFETIME_MIN = 30L
    }
}
