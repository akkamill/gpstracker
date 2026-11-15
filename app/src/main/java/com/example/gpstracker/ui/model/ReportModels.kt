package com.example.gpstracker.ui.model

/** What a driver can report. UI copy of the model type, so screens do not depend on other modules. */
enum class ReportKind { Accident, Police, Hazard, TrafficJam, RoadClosed, SpeedCamera }

/** A report shown as a marker on the map. */
data class RoadReportUi(
    val id: String,
    val kind: ReportKind,
    val position: GeoPointUi,
    val roadLabel: String,
    val mine: Boolean,
)

/** The nearest report on the road ahead, with a readable distance ("400 m"). */
data class ReportAheadUi(
    val id: String,
    val kind: ReportKind,
    val distanceLabel: String,
    val roadLabel: String,
)
