package com.example.gpstracker.presentation.dashboard

import com.example.gpstracker.domain.driving.DrivingEventType

enum class ConnectionStatus { OFFLINE, CONNECTING, LIVE, RECONNECTING }

/** A driving event, ready to display. */
data class EventUi(
    val id: Long,
    val type: DrivingEventType,
    val penalty: Int,
    val secondsAgo: Int,
)

/** Everything the dashboard screen needs; immutable. */
data class DashboardUiState(
    val connection: ConnectionStatus = ConnectionStatus.OFFLINE,
    val speedKmh: Int = 0,
    val score: Int = 100,
    val distanceKm: Double = 0.0,
    val maxSpeedKmh: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val events: List<EventUi> = emptyList(),
)
