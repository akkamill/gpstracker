package com.example.gpstracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport

@Entity(tableName = "road_reports")
data class RoadReportEntity(
    @PrimaryKey val id: String,
    val type: ReportType,
    val latitude: Double,
    val longitude: Double,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val source: ReportSource,
    val headingDeg: Double?,
    val roadName: String?,
    val stillThereVotes: Int,
    val goneVotes: Int,
)

fun RoadReport.toEntity() = RoadReportEntity(
    id = id,
    type = type,
    latitude = position.latitude,
    longitude = position.longitude,
    createdAtMs = createdAtMs,
    expiresAtMs = expiresAtMs,
    source = source,
    headingDeg = headingDeg,
    roadName = roadName,
    stillThereVotes = stillThereVotes,
    goneVotes = goneVotes,
)

fun RoadReportEntity.toModel() = RoadReport(
    id = id,
    type = type,
    position = LatLon(latitude, longitude),
    createdAtMs = createdAtMs,
    expiresAtMs = expiresAtMs,
    source = source,
    headingDeg = headingDeg,
    roadName = roadName,
    stillThereVotes = stillThereVotes,
    goneVotes = goneVotes,
)
