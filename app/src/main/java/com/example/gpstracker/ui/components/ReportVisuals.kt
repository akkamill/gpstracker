package com.example.gpstracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gpstracker.R
import com.example.gpstracker.ui.model.ReportKind
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.NightBlue

/** Icon of a report kind. Each kind has its own icon, so color is never the only difference. */
fun ReportKind.icon(): ImageVector = when (this) {
    ReportKind.Accident -> Icons.Rounded.CarCrash
    ReportKind.Police -> Icons.Rounded.LocalPolice
    ReportKind.Hazard -> Icons.Rounded.Warning
    ReportKind.TrafficJam -> Icons.Rounded.Traffic
    ReportKind.RoadClosed -> Icons.Rounded.Block
    ReportKind.SpeedCamera -> Icons.Rounded.PhotoCamera
}

fun ReportKind.color(): Color = when (this) {
    ReportKind.Accident, ReportKind.RoadClosed -> DriveColors.danger
    ReportKind.Hazard, ReportKind.TrafficJam -> DriveColors.warning
    ReportKind.Police -> NightBlue
    ReportKind.SpeedCamera -> Concrete
}

@StringRes
fun ReportKind.label(): Int = when (this) {
    ReportKind.Accident -> R.string.report_kind_accident
    ReportKind.Police -> R.string.report_kind_police
    ReportKind.Hazard -> R.string.report_kind_hazard
    ReportKind.TrafficJam -> R.string.report_kind_jam
    ReportKind.RoadClosed -> R.string.report_kind_closed
    ReportKind.SpeedCamera -> R.string.report_kind_camera
}
