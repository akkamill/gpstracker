package com.example.gpstracker.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.domain.driving.DrivingEventType
import com.example.gpstracker.presentation.dashboard.components.EventsCard
import com.example.gpstracker.presentation.dashboard.components.MapPlaceholderCard
import com.example.gpstracker.presentation.dashboard.components.ScoreAndStats
import com.example.gpstracker.presentation.dashboard.components.SpeedGauge
import com.example.gpstracker.presentation.dashboard.components.StatusBanner
import com.example.gpstracker.ui.theme.GpstrackerTheme

/** Stateful entry point: wires the ViewModel to the stateless [DashboardContent]. */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(state)
}

@Composable
fun DashboardContent(state: DashboardUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusBanner(state.connection)
        SpeedGauge(speedKmh = state.speedKmh)
        ScoreAndStats(state.score, state.distanceKm, state.maxSpeedKmh)
        MapPlaceholderCard(state.latitude, state.longitude)
        EventsCard(state.events)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1116, heightDp = 900)
@Composable
private fun DashboardContentPreview() {
    GpstrackerTheme {
        DashboardContent(
            DashboardUiState(
                connection = ConnectionStatus.LIVE,
                speedKmh = 72,
                score = 88,
                distanceKm = 4.2,
                maxSpeedKmh = 97,
                latitude = 40.4093,
                longitude = 49.8671,
                events = listOf(EventUi(1, DrivingEventType.HARD_BRAKING, 5, 6)),
            ),
        )
    }
}
