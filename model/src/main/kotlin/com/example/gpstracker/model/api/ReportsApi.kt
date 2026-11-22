package com.example.gpstracker.model.api

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportType
import kotlinx.serialization.Serializable

/*
 * Road reports shared between drivers through the server. Answers are plain RoadReport objects;
 * their "source" is Me for reports of the driver who asks, OtherDriver for everybody else's.
 */

/**
 * A report made on a phone. The phone chooses the [id], so sending the same report again after
 * a lost connection does not create a second one. The server sets the times itself.
 */
@Serializable
data class NewReportRequest(
    val id: String,
    val type: ReportType,
    val position: LatLon,
    val headingDeg: Double? = null,
    val roadName: String? = null,
)

/** A driver's answer to "still there?". Each driver counts once per report. */
@Serializable
data class VoteRequest(val stillThere: Boolean)
