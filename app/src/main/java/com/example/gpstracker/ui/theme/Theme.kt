package com.example.gpstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DriveColorScheme = darkColorScheme(
    background = Asphalt,
    surface = Kerb,
    surfaceVariant = KerbHigh,
    surfaceContainerHigh = KerbHigh,
    onBackground = LineWhite,
    onSurface = LineWhite,
    onSurfaceVariant = Concrete,
    primary = NightBlue,
    onPrimary = Asphalt,
    error = SignRed,
    onError = LineWhite,
    outline = KerbHigh,
    secondary = NightBlue,
    onSecondary = Asphalt,
    secondaryContainer = KerbHigh,
    onSecondaryContainer = LineWhite,
    primaryContainer = KerbHigh,
    onPrimaryContainer = LineWhite,
    errorContainer = KerbHigh,
    onErrorContainer = LineWhite,
    outlineVariant = KerbHigh,
)

/**
 * App theme. Dark only, on purpose: the app is used in a car at night and
 * must never flash white, so the system light mode setting is ignored.
 */
@Composable
fun DriveMetricsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DriveColorScheme,
        typography = Typography,
        shapes = DriveShapes,
        content = content,
    )
}
