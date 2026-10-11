package com.example.gpstracker.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.domain.driving.DrivingEventType
import com.example.gpstracker.presentation.dashboard.EventUi
import com.example.gpstracker.ui.theme.Danger
import com.example.gpstracker.ui.theme.GpstrackerTheme

/** List of the latest driving events, newest first. */
@Composable
fun EventsCard(events: List<EventUi>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.events_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.events_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            events.forEach { EventRow(it) }
        }
    }
}

@Composable
private fun EventRow(event: EventUi) {
    val title = when (event.type) {
        DrivingEventType.HARD_BRAKING -> R.string.event_hard_braking
        DrivingEventType.HARD_ACCELERATION -> R.string.event_hard_acceleration
        DrivingEventType.SPEEDING -> R.string.event_speeding
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(R.string.event_when, event.secondsAgo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(stringResource(R.string.event_penalty, event.penalty), style = MaterialTheme.typography.titleMedium, color = Danger)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1116)
@Composable
private fun EventsCardPreview() {
    GpstrackerTheme {
        EventsCard(
            listOf(
                EventUi(1, DrivingEventType.HARD_BRAKING, 5, 4),
                EventUi(2, DrivingEventType.SPEEDING, 2, 18),
            ),
        )
    }
}
