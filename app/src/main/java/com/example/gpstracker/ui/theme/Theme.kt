package com.example.gpstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DashboardColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = OnSurfaceMuted,
    error = Danger,
)

/** App-wide theme. The dashboard is dark-only because it is glanced at while driving. */
@Composable
fun GpstrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DashboardColorScheme,
        typography = Typography,
        content = content,
    )
}
