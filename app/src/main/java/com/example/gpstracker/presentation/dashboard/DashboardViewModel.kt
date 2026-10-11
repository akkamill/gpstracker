package com.example.gpstracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.di.AppContainer
import com.example.gpstracker.domain.driving.DrivingSession
import com.example.gpstracker.domain.driving.ObserveDrivingSession
import com.example.gpstracker.domain.telemetry.ConnectionState
import com.example.gpstracker.domain.telemetry.TelemetrySource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

class DashboardViewModel(
    observeDrivingSession: ObserveDrivingSession = AppContainer.observeDrivingSession,
    source: TelemetrySource = AppContainer.telemetrySource,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        observeDrivingSession().onStart { emit(DrivingSession()) },
        source.connection,
        ::toUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState())

    private fun toUiState(session: DrivingSession, connection: ConnectionState): DashboardUiState {
        val now = session.latest?.timestampMs
        return DashboardUiState(
            connection = connection.toStatus(),
            speedKmh = session.latest?.speedKmh?.roundToInt() ?: 0,
            score = session.score,
            distanceKm = session.distanceMeters / METERS_PER_KM,
            maxSpeedKmh = session.maxSpeedKmh.roundToInt(),
            latitude = session.latest?.latitude,
            longitude = session.latest?.longitude,
            events = session.events.asReversed().take(MAX_EVENTS).map {
                EventUi(
                    id = it.timestampMs,
                    type = it.type,
                    penalty = it.penalty,
                    secondsAgo = if (now == null) 0 else ((now - it.timestampMs) / MS_PER_SEC).toInt(),
                )
            },
        )
    }

    private fun ConnectionState.toStatus() = when (this) {
        ConnectionState.Disconnected -> ConnectionStatus.OFFLINE
        ConnectionState.Connecting -> ConnectionStatus.CONNECTING
        ConnectionState.Connected -> ConnectionStatus.LIVE
        is ConnectionState.Reconnecting -> ConnectionStatus.RECONNECTING
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_EVENTS = 5
        const val METERS_PER_KM = 1_000.0
        const val MS_PER_SEC = 1_000
    }
}
