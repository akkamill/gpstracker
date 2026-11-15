package com.example.gpstracker.data.reports

import com.example.gpstracker.domain.reports.ReportAhead
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.ReportAheadUi
import com.example.gpstracker.ui.model.ReportKind
import com.example.gpstracker.ui.model.RoadReportUi
import java.util.Locale
import kotlin.math.roundToInt

/** Turns report data into what the Drive and Map screens show. */
class ReportUiMapper {

    /** Adds the report parts to a drive state. */
    fun apply(drive: DriveUiState, reports: ReportsState): DriveUiState = drive.copy(
        reports = reports.reports.map(::report),
        reportAhead = reports.ahead?.let(::ahead),
        stillThere = reports.askStillThere?.let(::report),
        canReport = reports.canReport,
    )

    fun report(r: RoadReport) = RoadReportUi(
        id = r.id,
        kind = kind(r.type),
        position = GeoPointUi(r.position.latitude, r.position.longitude),
        roadLabel = r.roadName.orEmpty(),
        mine = r.source == ReportSource.Me,
    )

    fun ahead(a: ReportAhead) = ReportAheadUi(
        id = a.report.id,
        kind = kind(a.report.type),
        distanceLabel = distanceLabel(a.distanceM),
        roadLabel = a.report.roadName.orEmpty(),
    )

    /** "80 m" in 10 m steps below a kilometre, "1.2 km" above. */
    fun distanceLabel(metres: Double): String = if (metres < M_PER_KM) {
        "${(metres / ROUND_TO_M).roundToInt() * ROUND_TO_M.toInt()} m"
    } else {
        "%.1f km".format(Locale.US, metres / M_PER_KM)
    }

    fun kind(type: ReportType): ReportKind = ReportKind.valueOf(type.name)

    fun type(kind: ReportKind): ReportType = ReportType.valueOf(kind.name)

    private companion object {
        const val M_PER_KM = 1_000.0
        const val ROUND_TO_M = 10.0
    }
}
