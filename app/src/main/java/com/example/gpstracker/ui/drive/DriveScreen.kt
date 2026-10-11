package com.example.gpstracker.ui.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.components.BannerKind
import com.example.gpstracker.ui.components.ConnectionChip
import com.example.gpstracker.ui.components.EventBanner
import com.example.gpstracker.ui.components.GpsChip
import com.example.gpstracker.ui.components.KeepScreenOn
import com.example.gpstracker.ui.components.LastTripCard
import com.example.gpstracker.ui.components.ScoreBadge
import com.example.gpstracker.ui.components.SpeedGauge
import com.example.gpstracker.ui.components.SpeedLimitSign
import com.example.gpstracker.ui.components.StatTile
import com.example.gpstracker.ui.components.StatTileRow
import com.example.gpstracker.ui.components.StatusBanner
import com.example.gpstracker.ui.components.TripSavedBanner
import com.example.gpstracker.ui.components.TripStatusBar
import com.example.gpstracker.ui.components.labelRes
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import java.util.Locale

/**
 * Live dashboard. Read-only: it shows what the car reports and nothing on it
 * starts or ends a trip. Stateless: everything comes in as [state] and goes out
 * as callbacks, so it can be previewed and tested without a ViewModel.
 */
@Composable
fun DriveScreen(
    state: DriveUiState,
    onOpenTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOn(enabled = state.isTripInProgress)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            LandscapeLayout(state, onOpenTrip, onOpenSettings)
        } else {
            PortraitLayout(state, onOpenTrip, onOpenSettings)
        }
        EventBanner(
            event = state.latestEvent,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
        )
        TripSavedBanner(
            trip = state.tripJustSaved,
            onView = onOpenTrip,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
        )
    }
}

@Composable
private fun PortraitLayout(
    state: DriveUiState,
    onOpenTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectionChip(state.connection)
                    GpsChip(state.gps)
                }
                if (state.isTripInProgress) SpeedLimitSign(state.speedLimitKmh)
            }
            Banner(state, onOpenSettings)
            SpeedGauge(
                speedKmh = state.speedKmh,
                limitKmh = state.speedLimitKmh,
                isOverLimit = state.isOverLimit,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
                dimmed = !state.isTripInProgress,
                offline = state.connection == ConnectionStatus.Offline,
            )
            if (state.isTripInProgress) {
                ScoreSection(state, Modifier.fillMaxWidth())
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                StatTileRow(
                    tiles = listOf(
                        { m -> StatTile(distanceValue(state), stringResource(R.string.unit_km), stringResource(R.string.stat_distance), m) },
                        { m -> StatTile(state.durationLabel, null, stringResource(R.string.stat_time), m) },
                        { m -> StatTile(state.eventCount.toString(), null, stringResource(R.string.stat_events), m) },
                    ),
                )
            } else {
                ParkedContent(state, onOpenTrip)
            }
        }
        TripStatusBar(
            vehicleState = state.vehicleState,
            tripStartedLabel = state.tripStartedLabel,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun LandscapeLayout(
    state: DriveUiState,
    onOpenTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.weight(0.55f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            SpeedGauge(
                speedKmh = state.speedKmh,
                limitKmh = state.speedLimitKmh,
                isOverLimit = state.isOverLimit,
                modifier = Modifier.fillMaxHeight(),
                dimmed = !state.isTripInProgress,
                offline = state.connection == ConnectionStatus.Offline,
            )
            if (state.isTripInProgress) {
                SpeedLimitSign(state.speedLimitKmh, Modifier.align(Alignment.TopEnd))
            }
        }
        Column(
            modifier = Modifier.weight(0.45f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectionChip(state.connection)
                    GpsChip(state.gps)
                }
                Banner(state, onOpenSettings)
                if (state.isTripInProgress) {
                    ScoreSection(state, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile(distanceValue(state), stringResource(R.string.unit_km), stringResource(R.string.stat_distance))
                        StatTile(state.durationLabel, null, stringResource(R.string.stat_time))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile(state.eventCount.toString(), null, stringResource(R.string.stat_events))
                        StatTile(state.avgSpeedKmh.toString(), stringResource(R.string.gauge_unit), stringResource(R.string.stat_avg_speed))
                    }
                } else {
                    ParkedContent(state, onOpenTrip)
                }
            }
            TripStatusBar(state.vehicleState, state.tripStartedLabel)
        }
    }
}

@Composable
private fun ParkedContent(state: DriveUiState, onOpenTrip: (String) -> Unit) {
    val last = state.lastTrip
    if (last != null) {
        LastTripCard(trip = last, onClick = { onOpenTrip(last.id) })
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.first_drive_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.first_drive_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Banner(state: DriveUiState, onOpenSettings: () -> Unit) {
    when {
        state.connection == ConnectionStatus.Offline -> StatusBanner(
            kind = BannerKind.Offline,
            message = stringResource(R.string.banner_offline),
            actionLabel = stringResource(R.string.nav_settings),
            onAction = onOpenSettings,
        )
        state.gps == GpsStatus.Lost && state.isTripInProgress -> StatusBanner(
            kind = BannerKind.GpsLost,
            message = stringResource(R.string.banner_gps_lost),
        )
        state.connection == ConnectionStatus.Reconnecting -> StatusBanner(
            kind = BannerKind.Reconnecting,
            message = stringResource(R.string.banner_reconnecting, state.retryInSeconds),
        )
    }
}

@Composable
private fun ScoreSection(state: DriveUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ScoreBadge(score = state.score, band = state.scoreBand)
        Column(Modifier.padding(start = 16.dp)) {
            Text(
                text = stringResource(R.string.score_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.score_summary,
                    stringResource(state.scoreBand.labelRes()),
                    pluralStringResource(R.plurals.events_count, state.eventCount, state.eventCount),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun distanceValue(state: DriveUiState): String = String.format(Locale.US, "%.1f", state.distanceKm)

// ---- Previews: one per state listed in the spec ----

@Composable
private fun PreviewScreen(state: DriveUiState) {
    DriveMetricsTheme {
        Box(Modifier.fillMaxSize()) {
            DriveScreen(state, onOpenTrip = {}, onOpenSettings = {})
        }
    }
}

private const val PHONE = "spec:width=411dp,height=891dp"
private const val BG = 0xFF0F1417

@Preview(name = "1 Parked, last trip", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun ParkedPreview() = PreviewScreen(PreviewStates.driveParked)

@Preview(name = "2 Parked, first use", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun FirstUsePreview() = PreviewScreen(PreviewStates.driveFirstUse)

@Preview(name = "3 Driving", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun DrivingPreview() = PreviewScreen(PreviewStates.driveDriving)

@Preview(name = "4 Over limit", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun OverLimitPreview() = PreviewScreen(PreviewStates.driveOverLimit)

@Preview(name = "5 Event banner", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun EventBannerPreview() = PreviewScreen(PreviewStates.driveEventBanner)

@Preview(name = "6 Stopped at a light", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun StoppedPreview() = PreviewScreen(PreviewStates.driveStopped)

@Preview(name = "7 Trip just saved", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun TripSavedPreview() = PreviewScreen(PreviewStates.driveTripSaved)

@Preview(name = "8 Reconnecting", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun ReconnectingPreview() = PreviewScreen(PreviewStates.driveReconnecting)

@Preview(name = "9 GPS lost", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun GpsLostPreview() = PreviewScreen(PreviewStates.driveGpsLost)

@Preview(name = "10 Offline", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun OfflinePreview() = PreviewScreen(PreviewStates.driveOffline)

@Preview(name = "11 Unknown limit", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun UnknownLimitPreview() = PreviewScreen(PreviewStates.driveUnknownLimit)

@Preview(name = "12 Landscape, driving", showBackground = true, backgroundColor = BG, device = "spec:width=891dp,height=411dp")
@Composable
private fun LandscapePreview() = PreviewScreen(PreviewStates.driveDriving)
