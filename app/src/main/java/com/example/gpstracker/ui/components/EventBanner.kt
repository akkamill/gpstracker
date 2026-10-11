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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import kotlinx.coroutines.delay

private const val SLIDE_MS = 250
private const val VISIBLE_MS = 3_000L

/**
 * Slides in from the top when [event] changes, stays 3 s, then hides itself.
 * Announced politely to TalkBack.
 */
@Composable
fun EventBanner(event: DrivingEventUi?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(event) }

    LaunchedEffect(event?.id) {
        if (event != null) {
            shown = event
            visible = true
            delay(VISIBLE_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(SLIDE_MS)) { -it },
        exit = slideOutVertically(tween(SLIDE_MS)) { -it },
    ) {
        shown?.let { EventBannerContent(it) }
    }
}

@Composable
private fun EventBannerContent(event: DrivingEventUi) {
    val description = stringResource(R.string.event_row_cd, event.title, event.detail, event.penalty)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = event.type.icon(),
            contentDescription = null,
            tint = event.type.color(),
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(event.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.event_penalty, event.penalty),
            style = MaterialTheme.typography.titleMedium,
            color = DriveColors.danger,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun EventBannerPreview() {
    DriveMetricsTheme { EventBanner(FakeData.events.first()) }
}
