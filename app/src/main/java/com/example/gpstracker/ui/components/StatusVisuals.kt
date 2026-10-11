package com.example.gpstracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gpstracker.R
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.ScoreBand
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.NightBlue

/*
 * One place that decides how each status looks. Screens use these helpers
 * and never pick colors or icons themselves. Every state carries an icon
 * and text, so meaning is never conveyed by color alone.
 */

fun ScoreBand.color(): Color = when (this) {
    ScoreBand.Good -> DriveColors.success
    ScoreBand.Fair -> DriveColors.warning
    ScoreBand.Risky -> DriveColors.danger
}

@StringRes
fun ScoreBand.labelRes(): Int = when (this) {
    ScoreBand.Good -> R.string.score_good
    ScoreBand.Fair -> R.string.score_fair
    ScoreBand.Risky -> R.string.score_risky
}

fun ScoreBand.icon(): ImageVector = when (this) {
    ScoreBand.Good -> Icons.Rounded.Check
    ScoreBand.Fair -> Icons.Rounded.Warning
    ScoreBand.Risky -> Icons.Rounded.CarCrash
}

fun EventType.color(): Color = when (this) {
    EventType.Speeding, EventType.CrashSuspected -> DriveColors.danger
    EventType.HarshBraking, EventType.HarshAcceleration, EventType.SharpCorner -> DriveColors.warning
}

fun EventType.icon(): ImageVector = when (this) {
    EventType.Speeding -> Icons.Rounded.Speed
    EventType.HarshBraking -> Icons.Rounded.TrendingDown
    EventType.HarshAcceleration -> Icons.Rounded.TrendingUp
    EventType.SharpCorner -> Icons.Rounded.TurnRight
    EventType.CrashSuspected -> Icons.Rounded.CarCrash
}

@StringRes
fun EventType.titleRes(): Int = when (this) {
    EventType.Speeding -> R.string.event_speeding
    EventType.HarshBraking -> R.string.event_harsh_braking
    EventType.HarshAcceleration -> R.string.event_harsh_acceleration
    EventType.SharpCorner -> R.string.event_sharp_corner
    EventType.CrashSuspected -> R.string.event_crash
}

/** Connecting has no icon: the chip shows a small progress indicator instead. */
fun ConnectionStatus.icon(): ImageVector? = when (this) {
    ConnectionStatus.Live -> Icons.Rounded.Circle
    ConnectionStatus.Replay -> Icons.Rounded.PlayCircle
    ConnectionStatus.Connecting -> null
    ConnectionStatus.Reconnecting -> Icons.Rounded.Sync
    ConnectionStatus.Offline -> Icons.Rounded.CloudOff
}

fun ConnectionStatus.color(): Color = when (this) {
    ConnectionStatus.Live -> DriveColors.success
    ConnectionStatus.Replay -> NightBlue
    ConnectionStatus.Connecting -> Concrete
    ConnectionStatus.Reconnecting -> DriveColors.warning
    ConnectionStatus.Offline -> DriveColors.danger
}

@StringRes
fun ConnectionStatus.labelRes(): Int = when (this) {
    ConnectionStatus.Live -> R.string.conn_live
    ConnectionStatus.Replay -> R.string.conn_replay
    ConnectionStatus.Connecting -> R.string.conn_connecting
    ConnectionStatus.Reconnecting -> R.string.conn_reconnecting
    ConnectionStatus.Offline -> R.string.conn_offline
}

fun GpsStatus.icon(): ImageVector = when (this) {
    GpsStatus.Good -> Icons.Rounded.GpsFixed
    GpsStatus.Weak -> Icons.Rounded.GpsNotFixed
    GpsStatus.Lost -> Icons.Rounded.GpsOff
}

fun GpsStatus.color(): Color = when (this) {
    GpsStatus.Good -> DriveColors.success
    GpsStatus.Weak -> DriveColors.warning
    GpsStatus.Lost -> DriveColors.danger
}

@StringRes
fun GpsStatus.labelRes(): Int = when (this) {
    GpsStatus.Good -> R.string.gps_good
    GpsStatus.Weak -> R.string.gps_weak
    GpsStatus.Lost -> R.string.gps_lost
}
