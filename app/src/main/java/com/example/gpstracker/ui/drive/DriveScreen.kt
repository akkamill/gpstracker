package com.example.gpstracker.ui.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.components.BannerKind
import com.example.gpstracker.ui.components.ConnectionChip
import com.example.gpstracker.ui.components.EventBanner
import com.example.gpstracker.ui.components.GpsChip
import com.example.gpstracker.ui.components.KeepScreenOn
import com.example.gpstracker.ui.components.ScoreBadge
import com.example.gpstracker.ui.components.SpeedGauge
import com.example.gpstracker.ui.components.SpeedLimitSign
import com.example.gpstracker.ui.components.StatTile
import com.example.gpstracker.ui.components.StatTileRow
import com.example.gpstracker.ui.components.StatusBanner
import com.example.gpstracker.ui.components.TripButton
import com.example.gpstracker.ui.components.labelRes
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/**
 * Live dashboard. Stateless: everything comes in as [state] and goes out as
 * callbacks, so it can be previewed and tested without a ViewModel.
 */
@Composable
fun DriveScreen(
    state: DriveUiState,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOn(enabled = state.isTripActive)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            LandscapeLayout(state, onStartTrip, onStopTrip, onOpenSettings)
        } else {
            PortraitLayout(state, onStartTrip, onStopTrip, onOpenSettings)
        }
        EventBanner(
            event = state.latestEvent,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
        )
    }
}

@Composable
private fun PortraitLayout(
    state: DriveUiState,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
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
                SpeedLimitSign(state.speedLimitKmh)
            }
            Banner(state, onOpenSettings)
            SpeedGauge(
                speedKmh = state.speedKmh,
                limitKmh = state.speedLimitKmh,
                isOverLimit = state.isOverLimit,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            )
            ScoreSection(state, Modifier.fillMaxWidth())
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            StatTileRow(
                tiles = listOf(
                    { m -> StatTile(distanceValue(state), unitKm(state), stringResource(R.string.stat_distance), m) },
                    { m -> StatTile(timeValue(state), null, stringResource(R.string.stat_time), m) },
                    { m -> StatTile(eventsValue(state), null, stringResource(R.string.stat_events), m) },
                ),
            )
        }
        TripButton(
            isTripActive = state.isTripActive,
            enabled = tripButtonEnabled(state),
            onStart = onStartTrip,
            onStop = onStopTrip,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun LandscapeLayout(
    state: DriveUiState,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(0.55f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            SpeedGauge(
                speedKmh = state.speedKmh,
                limitKmh = state.speedLimitKmh,
                isOverLimit = state.isOverLimit,
                modifier = Modifier.fillMaxHeight(),
            )
            SpeedLimitSign(state.speedLimitKmh, Modifier.align(Alignment.TopEnd))
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
                ScoreSection(state, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatTile(distanceValue(state), unitKm(state), stringResource(R.string.stat_distance))
                    StatTile(timeValue(state), null, stringResource(R.string.stat_time))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatTile(eventsValue(state), null, stringResource(R.string.stat_events))
                    StatTile(avgSpeedValue(state), avgSpeedUnit(state), stringResource(R.string.stat_avg_speed))
                }
            }
            TripButton(
                isTripActive = state.isTripActive,
                enabled = tripButtonEnabled(state),
                onStart = onStartTrip,
                onStop = onStopTrip,
            )
        }
    }
}

@Composable
private fun Banner(state: DriveUiState, onOpenSettings: () -> Unit) {
    when {
        state.gps == GpsStatus.Lost && state.isTripActive -> StatusBanner(
            kind = BannerKind.GpsLost,
            message = stringResource(R.string.banner_gps_lost),
        )
        state.connection == ConnectionStatus.Reconnecting -> StatusBanner(
            kind = BannerKind.Reconnecting,
            message = stringResource(R.string.banner_reconnecting, state.retryInSeconds),
        )
        state.connection == ConnectionStatus.Offline && !state.isTripActive -> StatusBanner(
            kind = BannerKind.Offline,
            message = stringResource(R.string.banner_offline),
            actionLabel = stringResource(R.string.nav_settings),
            onAction = onOpenSettings,
        )
    }
}

@Composable
private fun ScoreSection(state: DriveUiState, modifier: Modifier = Modifier) {
    if (!state.isTripActive) {
        Text(
            text = stringResource(R.string.score_start_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
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

private fun tripButtonEnabled(state: DriveUiState) =
    state.connection != ConnectionStatus.Offline && state.connection != ConnectionStatus.Connecting

@Composable
private fun distanceValue(state: DriveUiState): String =
    if (state.isTripActive) "%.1f".format(state.distanceKm) else stringResource(R.string.stat_none)

@Composable
private fun unitKm(state: DriveUiState): String? =
    if (state.isTripActive) stringResource(R.string.unit_km) else null

@Composable
private fun timeValue(state: DriveUiState): String =
    if (state.isTripActive) state.durationLabel else stringResource(R.string.stat_none)

@Composable
private fun eventsValue(state: DriveUiState): String =
    if (state.isTripActive) state.eventCount.toString() else stringResource(R.string.stat_none)

@Composable
private fun avgSpeedValue(state: DriveUiState): String =
    if (state.isTripActive) state.avgSpeedKmh.toString() else stringResource(R.string.stat_none)

@Composable
private fun avgSpeedUnit(state: DriveUiState): String? =
    if (state.isTripActive) stringResource(R.string.gauge_unit) else null

// ---- Previews: one per state listed in the spec ----

@Composable
private fun PreviewScreen(state: DriveUiState) {
    DriveMetricsTheme {
        Box(Modifier.fillMaxSize().padding(top = 0.dp)) {
            DriveScreen(state, {}, {}, {})
        }
    }
}

private const val PHONE = "spec:width=411dp,height=891dp"
private const val BG = 0xFF0F1417

@Preview(name = "1 Idle replay", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun IdlePreview() = PreviewScreen(PreviewStates.driveIdle)

@Preview(name = "2 Active normal", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun ActivePreview() = PreviewScreen(PreviewStates.driveActive)

@Preview(name = "3 Over limit", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun OverLimitPreview() = PreviewScreen(PreviewStates.driveOverLimit)

@Preview(name = "4 Event banner", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun EventBannerPreview() = PreviewScreen(PreviewStates.driveEventBanner)

@Preview(name = "5 Reconnecting", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun ReconnectingPreview() = PreviewScreen(PreviewStates.driveReconnecting)

@Preview(name = "6 GPS lost", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun GpsLostPreview() = PreviewScreen(PreviewStates.driveGpsLost)

@Preview(name = "7 Offline", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun OfflinePreview() = PreviewScreen(PreviewStates.driveOffline)

@Preview(name = "8 Unknown limit", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun UnknownLimitPreview() = PreviewScreen(PreviewStates.driveUnknownLimit)

@Preview(name = "9 Landscape", showBackground = true, backgroundColor = BG, device = "spec:width=891dp,height=411dp")
@Composable
private fun LandscapePreview() = PreviewScreen(PreviewStates.driveActive)
