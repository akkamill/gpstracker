package com.example.gpstracker.ui.theme

import androidx.compose.ui.graphics.Color

val Asphalt = Color(0xFF0F1417)
val Kerb = Color(0xFF182025)
val KerbHigh = Color(0xFF222C32)
val LineWhite = Color(0xFFEEF2F3)
val Concrete = Color(0xFF93A1A8)
val NightBlue = Color(0xFF4DA3FF)
val SignalAmber = Color(0xFFFFB020)
val SignRed = Color(0xFFD7263D)
val GoGreen = Color(0xFF3DD68C)

/** Status colors, accessed directly rather than through Material roles. */
object DriveColors {
    val warning = SignalAmber
    val danger = SignRed
    val success = GoGreen
}
