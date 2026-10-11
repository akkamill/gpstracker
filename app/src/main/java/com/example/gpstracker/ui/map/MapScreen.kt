package com.example.gpstracker.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.components.ConnectionChip
import com.example.gpstracker.ui.components.GpsChip
import com.example.gpstracker.ui.components.RouteMap
import com.example.gpstracker.ui.components.ScoreBadge
import com.example.gpstracker.ui.components.ScoreBadgeSize
import com.example.gpstracker.ui.components.SpeedLimitSign
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.speedValue

/** Live map with status chips, a recenter button and a summary card. Stateless. */
@Composable
fun MapScreen(
    state: DriveUiState,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var followCurrent by remember { mutableStateOf(false) }
    val inProgress = state.isTripInProgress
    // While parked the last trip is shown dimmed; with no trips at all only the grid and the car.
    val shownRoute = if (inProgress) state.route else state.lastTripRoute.takeIf { state.lastTrip != null }.orEmpty()

    Box(modifier.fillMaxSize()) {
        RouteMap(
            route = shownRoute,
            events = if (inProgress) state.events else emptyList(),
            currentPosition = state.position ?: state.route.lastOrNull(),
            followCurrent = followCurrent && inProgress,
            dimmed = !inProgress,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectionChip(state.connection)
            GpsChip(state.gps)
        }
        SmallFloatingActionButton(
            onClick = {
                followCurrent = !followCurrent
                onRecenter()
            },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.map_recenter_cd))
        }
        OverlayCard(
            state = state,
            inProgress = inProgress,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

@Composable
private fun OverlayCard(state: DriveUiState, inProgress: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!inProgress) {
            Text(
                text = stringResource(R.string.map_parked_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = state.speedKmh.toString(),
                    style = MaterialTheme.typography.speedValue.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                    color = if (state.isOverLimit) DriveColors.danger else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.gauge_unit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                if (state.isOverLimit) {
                    Text(
                        text = stringResource(R.string.map_over_limit, state.overLimitBy),
                        style = MaterialTheme.typography.labelLarge,
                        color = DriveColors.danger,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            SpeedLimitSign(state.speedLimitKmh, size = 40.dp)
            ScoreBadge(state.score, state.scoreBand, size = ScoreBadgeSize.Small)
        }
        Text(
            text = stringResource(
                R.string.map_summary,
                "%.1f".format(state.distanceKm),
                state.durationLabel,
                pluralStringResource(R.plurals.events_count, state.eventCount, state.eventCount),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val BG = 0xFF0F1417

@Preview(name = "Active trip, 3 events", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun MapActivePreview() {
    DriveMetricsTheme { MapScreen(PreviewStates.driveOverLimit, onRecenter = {}) }
}

@Preview(name = "Empty", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun MapEmptyPreview() {
    DriveMetricsTheme { MapScreen(PreviewStates.driveFirstUse, onRecenter = {}) }
}

@Preview(name = "Parked, last trip dimmed", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun MapParkedPreview() {
    DriveMetricsTheme { MapScreen(PreviewStates.driveParked, onRecenter = {}) }
}
