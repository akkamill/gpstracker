package com.example.gpstracker.ui.trips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.components.AccountAvatar
import com.example.gpstracker.ui.components.EmptyState
import com.example.gpstracker.ui.components.GuestBanner
import com.example.gpstracker.ui.components.ScoreBadge
import com.example.gpstracker.ui.components.ScoreBadgeSize
import com.example.gpstracker.ui.components.charts.Sparkline
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.model.TripsUiState
import com.example.gpstracker.ui.model.scoreBandOf
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Trip history: weekly summary card and trips grouped by date. Stateless. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripsScreen(
    state: TripsUiState,
    isGuest: Boolean,
    onTripClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateAccount: () -> Unit,
    onGoToDrive: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitials: String? = null,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.trips_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            AccountAvatar(initials = avatarInitials, onClick = onOpenSettings)
        }
        if (isGuest) {
            GuestBanner(onCreateAccount, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        if (state.sections.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Route,
                title = stringResource(R.string.trips_empty_title),
                message = stringResource(R.string.trips_empty_message),
                actionLabel = stringResource(R.string.trips_go_to_drive),
                onAction = onGoToDrive,
                modifier = Modifier.padding(top = 48.dp),
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item { WeekCard(state, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            state.sections.forEach { (label, trips) ->
                stickyHeader(key = label) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(trips, key = { it.id }) { trip -> TripRow(trip) { onTripClick(trip.id) } }
            }
        }
    }
}

@Composable
private fun WeekCard(state: TripsUiState, modifier: Modifier = Modifier) {
    val average = state.weekAverageScore ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreBadge(average, scoreBandOf(average), size = ScoreBadgeSize.Medium)
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    text = stringResource(R.string.trips_week_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.trips_week_summary,
                        pluralStringResource(R.plurals.trips_count, state.sections.sumOf { it.second.size }, state.sections.sumOf { it.second.size }),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Sparkline(state.weekScores)
    }
}

@Composable
private fun TripRow(trip: TripSummaryUi, onClick: () -> Unit) {
    val events = pluralStringResource(R.plurals.events_count, trip.eventCount, trip.eventCount)
    val meta = stringResource(R.string.trip_row_meta, trip.timeRange, "%.1f".format(trip.distanceKm), events)
    val description = stringResource(R.string.trip_row_cd, trip.fromTo, trip.timeRange, trip.score, events)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreBadge(trip.score, trip.scoreBand, size = ScoreBadgeSize.Small)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(trip.fromTo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val BG = 0xFF0F1417

@Preview(name = "Populated", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun TripsPopulatedPreview() {
    DriveMetricsTheme { TripsScreen(PreviewStates.tripsPopulated, isGuest = false, onTripClick = {}, onOpenSettings = {}, onCreateAccount = {}, onGoToDrive = {}, avatarInitials = "KA") }
}

@Preview(name = "Empty", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun TripsEmptyPreview() {
    DriveMetricsTheme { TripsScreen(PreviewStates.tripsEmpty, isGuest = false, onTripClick = {}, onOpenSettings = {}, onCreateAccount = {}, onGoToDrive = {}) }
}

@Preview(name = "Guest", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun TripsGuestPreview() {
    DriveMetricsTheme {
        TripsScreen(PreviewStates.tripsPopulated, isGuest = true, onTripClick = {}, onOpenSettings = {}, onCreateAccount = {}, onGoToDrive = {})
    }
}
