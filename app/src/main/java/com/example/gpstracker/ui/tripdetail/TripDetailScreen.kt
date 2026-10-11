package com.example.gpstracker.ui.tripdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.components.EventRow
import com.example.gpstracker.ui.components.FuelCard
import com.example.gpstracker.ui.components.RouteMap
import com.example.gpstracker.ui.components.ScoreBadge
import com.example.gpstracker.ui.components.ScoreBadgeSize
import com.example.gpstracker.ui.components.StatTile
import com.example.gpstracker.ui.model.ScoreBand
import com.example.gpstracker.ui.model.TripDetailUi
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Trip review: score, stats, route with markers, deductions and tips. Stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    state: TripDetailUi,
    selectedEventId: String?,
    onEventClick: (String) -> Unit,
    onSetFuelPrice: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(summary.fromTo, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.detail_subtitle, summary.dateLabel, summary.timeRange),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ScoreHeader(state)
            StatsGrid(state)
            RouteCard(state, selectedEventId)
            Deductions(state, selectedEventId, onEventClick)
            FuelCard(
                estimate = state.fuel,
                savedLabel = null,
                projectionLabel = null,
                onSetPrice = onSetFuelPrice,
            )
            if (state.tips.isNotEmpty()) Tips(state.tips)
        }
    }
}

@Composable
private fun ScoreHeader(state: TripDetailUi) {
    val summary = state.summary
    val eventsText = pluralStringResource(R.plurals.events_count, summary.eventCount, summary.eventCount)
    val verdict = when {
        summary.eventCount == 0 -> stringResource(R.string.verdict_perfect)
        summary.scoreBand == ScoreBand.Good -> stringResource(R.string.verdict_good, eventsText)
        else -> stringResource(R.string.verdict_other, eventsText)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScoreBadge(summary.score, summary.scoreBand, size = ScoreBadgeSize.Large)
        Text(
            text = verdict,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatsGrid(state: TripDetailUi) {
    val summary = state.summary
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            StatTile("%.1f".format(summary.distanceKm), stringResource(R.string.unit_km), stringResource(R.string.stat_distance), Modifier.weight(1f))
            StatTile(summary.durationLabel, null, stringResource(R.string.stat_duration), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            StatTile(state.avgSpeedKmh.toString(), stringResource(R.string.gauge_unit), stringResource(R.string.stat_avg_speed), Modifier.weight(1f))
            StatTile(state.maxSpeedKmh.toString(), stringResource(R.string.gauge_unit), stringResource(R.string.stat_max_speed), Modifier.weight(1f))
        }
    }
}

@Composable
private fun RouteCard(state: TripDetailUi, selectedEventId: String?) {
    RouteMap(
        route = state.route,
        events = state.events,
        selectedEventId = selectedEventId,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.medium),
    )
}

@Composable
private fun Deductions(state: TripDetailUi, selectedEventId: String?, onEventClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.deductions_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.events.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.event_penalty, state.events.sumOf { it.penalty }),
                    style = MaterialTheme.typography.titleMedium,
                    color = DriveColors.danger,
                )
            }
        }
        if (state.events.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = DriveColors.success, modifier = Modifier.size(24.dp))
                Text(
                    text = stringResource(R.string.deductions_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            state.events.sortedBy { it.timeLabel }.forEach { event ->
                EventRow(event, selected = event.id == selectedEventId, onClick = { onEventClick(event.id) })
            }
        }
    }
}

@Composable
private fun Tips(tips: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.tips_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        tips.take(3).forEach { tip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = DriveColors.warning, modifier = Modifier.size(24.dp))
                Text(tip, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private const val BG = 0xFF0F1417

@Preview(name = "With events", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=1200dp")
@Composable
private fun TripDetailEventsPreview() {
    DriveMetricsTheme {
        TripDetailScreen(FakeData.tripWithEvents, selectedEventId = "e2", onEventClick = {}, onSetFuelPrice = {}, onBack = {})
    }
}

@Preview(name = "Perfect trip", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun TripDetailPerfectPreview() {
    DriveMetricsTheme {
        TripDetailScreen(FakeData.perfectTrip, selectedEventId = null, onEventClick = {}, onSetFuelPrice = {}, onBack = {})
    }
}

@Preview(name = "Fuel price not set", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=1200dp")
@Composable
private fun TripDetailNoPricePreview() {
    DriveMetricsTheme {
        TripDetailScreen(
            state = FakeData.tripWithEvents.copy(fuel = FakeData.tripWithEvents.fuel.copy(wastedCostLabel = null)),
            selectedEventId = null,
            onEventClick = {},
            onSetFuelPrice = {},
            onBack = {},
        )
    }
}
