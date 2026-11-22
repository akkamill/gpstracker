package com.example.gpstracker.simulator.vehicle

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.road.Route

/**
 * Reports from "other drivers" along the demo routes, so the demo shows warnings before
 * the server shares real ones. Each faces the direction of its route, so a report on the
 * way there does not warn on the way back.
 */
object CommunityReports {
    private const val KM = 1_000.0
    private const val HOUR_MS = 3_600_000L
    private const val DEMO_LIFETIME_HOURS = 12L

    private data class Spot(val type: ReportType, val route: Route, val street: String, val afterM: Double)

    private val spots = listOf(
        Spot(ReportType.Police, Kaunas.toAkropolis, Kaunas.SAVANORIU, 0.9 * KM),
        Spot(ReportType.Hazard, Kaunas.toAkropolis, Kaunas.PETRAUSKO, 0.45 * KM),
        Spot(ReportType.TrafficJam, Kaunas.toZaliakalnis, Kaunas.PETRAUSKO, 0.3 * KM),
        Spot(ReportType.SpeedCamera, Kaunas.toZaliakalnis, Kaunas.SAVANORIU, 0.6 * KM),
    )

    /** The demo reports, created at [nowMs] and valid for the length of a demo session. */
    fun demo(nowMs: Long, lifetimeMs: Long = DEMO_LIFETIME_HOURS * HOUR_MS): List<RoadReport> = spots.mapIndexed { i, spot ->
        val atM = spot.route.segmentStartM(spot.street) + spot.afterM
        val p = spot.route.positionAt(atM)
        RoadReport(
            id = "community-${spot.type.name.lowercase()}-$i",
            type = spot.type,
            position = LatLon(p.lat, p.lon),
            createdAtMs = nowMs,
            expiresAtMs = nowMs + lifetimeMs,
            source = ReportSource.OtherDriver,
            headingDeg = spot.route.headingAt(atM),
            roadName = spot.street,
        )
    }
}
