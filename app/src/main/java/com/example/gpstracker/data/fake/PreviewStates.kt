package com.example.gpstracker.data.fake

import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.ScoreBand
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.DataSource
import com.example.gpstracker.ui.model.TripsUiState

/** Canned states used by `@Preview`s and UI tests. Screens never build fake data inline. */
object PreviewStates {

    val driveIdle = DriveUiState(
        connection = ConnectionStatus.Replay,
        speedKmh = 0,
        speedLimitKmh = 50,
        route = FakeData.route.take(1),
    )

    val driveActive = DriveUiState(
        connection = ConnectionStatus.Replay,
        isTripActive = true,
        speedKmh = 48,
        speedLimitKmh = 50,
        score = 74,
        scoreBand = ScoreBand.Fair,
        distanceKm = 12.4,
        durationLabel = "23:41",
        eventCount = 3,
        avgSpeedKmh = 36,
        route = FakeData.route.take(50),
        events = FakeData.events.take(3),
    )

    val driveOverLimit = driveActive.copy(speedKmh = 72)

    val driveEventBanner = driveActive.copy(latestEvent = FakeData.events.first())

    val driveReconnecting = driveActive.copy(connection = ConnectionStatus.Reconnecting)

    val driveGpsLost = driveActive.copy(gps = GpsStatus.Lost)

    val driveOffline = DriveUiState(
        connection = ConnectionStatus.Offline,
        speedKmh = 0,
        speedLimitKmh = 50,
    )

    val driveUnknownLimit = driveActive.copy(speedLimitKmh = null)

    val tripsPopulated: TripsUiState = FakeData.tripsState

    val tripsEmpty = TripsUiState()

    val settingsDefault = SettingsUiState()

    val settingsInvalidUrl = SettingsUiState(
        dataSource = DataSource.Live,
        serverUrl = "http://10.0.2.2:8080",
    )

    val crashCounting = CrashAlertUiState(visible = true, secondsLeft = 7)

    val crashSent = CrashAlertUiState(visible = true, secondsLeft = 0, sent = true)
}
