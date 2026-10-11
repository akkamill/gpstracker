package com.example.gpstracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import kotlinx.coroutines.delay
import java.util.Locale

private const val SLIDE_MS = 250
private const val VISIBLE_MS = 6_000L

/**
 * Slides in from the bottom for 6 s when a trip has just ended by itself, with
 * a "View" action. Announced politely to TalkBack.
 */
@Composable
fun TripSavedBanner(trip: TripSummaryUi?, onView: (String) -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(trip) }

    LaunchedEffect(trip?.id) {
        if (trip != null) {
            shown = trip
            visible = true
            delay(VISIBLE_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(SLIDE_MS)) { it },
        exit = slideOutVertically(tween(SLIDE_MS)) { it },
    ) {
        shown?.let { SavedContent(it, onView) }
    }
}

@Composable
private fun SavedContent(trip: TripSummaryUi, onView: (String) -> Unit) {
    val summary = stringResource(
        R.string.trip_saved_summary,
        String.format(Locale.US, "%.1f", trip.distanceKm),
        trip.score,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = DriveColors.success,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = stringResource(R.string.trip_saved_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { onView(trip.id) }) { Text(stringResource(R.string.trip_saved_view)) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun TripSavedBannerPreview() {
    DriveMetricsTheme { TripSavedBanner(FakeData.trips[1], onView = {}) }
}
