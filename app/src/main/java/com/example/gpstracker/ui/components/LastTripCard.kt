package com.example.gpstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import java.util.Locale

/** The most recent trip, shown on the dashboard while the car is parked. Opens the trip detail. */
@Composable
fun LastTripCard(trip: TripSummaryUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val km = String.format(Locale.US, "%.1f", trip.distanceKm)
    val events = pluralStringResource(R.plurals.events_count, trip.eventCount, trip.eventCount)
    val meta = stringResource(R.string.last_trip_meta, trip.dateLabel, trip.timeRange, km)
    val description = stringResource(R.string.trip_row_cd, trip.fromTo, trip.timeRange, trip.score, events)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreBadge(trip.score, trip.scoreBand, size = ScoreBadgeSize.Small)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = stringResource(R.string.last_trip_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(trip.fromTo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun LastTripCardPreview() {
    DriveMetricsTheme { LastTripCard(FakeData.trips[1], onClick = {}) }
}
