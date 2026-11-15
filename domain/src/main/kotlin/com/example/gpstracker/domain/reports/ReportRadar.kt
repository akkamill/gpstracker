package com.example.gpstracker.domain.reports

import com.example.gpstracker.domain.geo.GeoMath
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.model.telemetry.TelemetryPoint

/** A report on the road ahead and how far away it is. */
data class ReportAhead(val report: RoadReport, val distanceM: Double)

/**
 * Result of one sample.
 *
 * @property ahead the nearest report in front of the car, or null
 * @property passed a report from another driver that the car has just driven past;
 * the app asks the driver whether it is still there (once per report)
 */
data class RadarResult(val ahead: ReportAhead? = null, val passed: RoadReport? = null)

/** Distances and angles of the radar; see [ReportRadar]. */
data class RadarSettings(
    /** Warn about reports this close or closer. */
    val warnWithinM: Double = 500.0,
    /** "Ahead" means within this angle of the driving direction. */
    val aheadConeDeg: Double = 40.0,
    /** A report counts as passed once it is behind the car and within this distance. */
    val passedWithinM: Double = 150.0,
    /** Below this speed the GPS heading is unreliable, so the radar keeps its last answer. */
    val minSpeedMps: Double = 2.0,
)

/**
 * Watches the road ahead for reports, one sample at a time. Holds a little state (which
 * reports were seen ahead and which were already asked about), does no I/O.
 *
 * - Ahead: within [RadarSettings.warnWithinM] and within [RadarSettings.aheadConeDeg] of the
 *   driving direction. The driver's own reports are not announced back to them, and reports
 *   made by drivers going the opposite way are ignored.
 * - Passed: a report that was ahead earlier and is now behind and close; asked about once.
 */
class ReportRadar(private val settings: RadarSettings = RadarSettings()) {
    private val seenAhead = HashSet<String>()
    private val asked = HashSet<String>()
    private var last = RadarResult()

    fun update(point: TelemetryPoint, reports: List<RoadReport>): RadarResult {
        if (!point.hasPosition || point.speedMps < settings.minSpeedMps) {
            last = last.copy(passed = null)
            return last
        }
        var nearest: ReportAhead? = null
        var passed: RoadReport? = null
        reports.asSequence().filter { it.source != ReportSource.Me && sameDirection(it, point) }.forEach { report ->
            val distance = GeoMath.distanceM(point.position, report.position)
            val offCourse = GeoMath.angleBetweenDeg(point.headingDeg, GeoMath.bearingDeg(point.position, report.position))
            when {
                distance <= settings.warnWithinM && offCourse <= settings.aheadConeDeg -> {
                    seenAhead += report.id
                    if (nearest == null || distance < nearest!!.distanceM) nearest = ReportAhead(report, distance)
                }

                report.id in seenAhead && report.id !in asked && offCourse > BEHIND_DEG && distance <= settings.passedWithinM -> {
                    asked += report.id
                    passed = report
                }
            }
        }
        last = RadarResult(nearest, passed)
        return last
    }

    /** Reports made by drivers going the other way are about the other side of the road. */
    private fun sameDirection(report: RoadReport, point: TelemetryPoint): Boolean {
        val heading = report.headingDeg ?: return true
        return GeoMath.angleBetweenDeg(heading, point.headingDeg) <= BEHIND_DEG
    }

    /** Forgets what was seen and asked, for example when a new trip starts. */
    fun reset() {
        seenAhead.clear()
        asked.clear()
        last = RadarResult()
    }

    private companion object {
        const val BEHIND_DEG = 90.0
    }
}
