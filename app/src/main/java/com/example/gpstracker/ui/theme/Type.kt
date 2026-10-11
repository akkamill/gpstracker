package com.example.gpstracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gpstracker.R

/** Road-signage style UI font. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

/** Condensed variant used for big numerals. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
)

/** Tabular figures so digits do not jump while a number changes. */
private const val TABULAR = "tnum"

/** Current speed on the gauge and map overlay. */
val SpeedValueStyle = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.SemiBold,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    fontFeatureSettings = TABULAR,
)

/** Stat values (distance, time, events). */
val StatValueStyle = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    fontFeatureSettings = TABULAR,
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = TABULAR,
    ),
    headlineSmall = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, fontFeatureSettings = TABULAR,
    ),
    titleMedium = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR,
    ),
    bodyLarge = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR,
    ),
    bodyMedium = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR,
    ),
    labelLarge = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR,
    ),
    labelMedium = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR,
    ),
)

/** Spec style `speedValue`, reachable as `MaterialTheme.typography.speedValue`. */
val Typography.speedValue: TextStyle get() = SpeedValueStyle

/** Spec style for stat tile values. */
val Typography.statValue: TextStyle get() = StatValueStyle
