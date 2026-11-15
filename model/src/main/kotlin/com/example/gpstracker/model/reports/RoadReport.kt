package com.example.gpstracker.model.reports

import com.example.gpstracker.model.geo.LatLon
import kotlinx.serialization.Serializable

/** What a driver can report about the road. */
@Serializable
enum class ReportType { Accident, Police, Hazard, TrafficJam, RoadClosed, SpeedCamera }

/** Who made the report: this phone's driver or someone else. */
@Serializable
enum class ReportSource { Me, OtherDriver }

/**
 * Something a driver reported at a place on the road. It disappears by itself when it
 * expires, or earlier when more drivers say it is gone than say it is still there.
 *
 * @property headingDeg the reporter's direction of travel, so others coming the same way are warned
 * @property stillThereVotes drivers who passed it and confirmed it
 * @property goneVotes drivers who passed it and said it is no longer there
 */
@Serializable
data class RoadReport(
    val id: String,
    val type: ReportType,
    val position: LatLon,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val source: ReportSource,
    val headingDeg: Double? = null,
    val roadName: String? = null,
    val stillThereVotes: Int = 0,
    val goneVotes: Int = 0,
) {
    fun isActive(nowMs: Long): Boolean = nowMs < expiresAtMs && goneVotes <= stillThereVotes
}
