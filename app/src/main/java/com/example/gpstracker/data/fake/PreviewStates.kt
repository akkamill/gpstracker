package com.example.gpstracker.data.fake

import com.example.gpstracker.data.estimate.InsightsCalculator
import com.example.gpstracker.data.estimate.TripRecord
import com.example.gpstracker.data.estimate.VehicleNumbers
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.InsightsPeriod
import com.example.gpstracker.ui.model.SignInUiState
import com.example.gpstracker.ui.model.SignUpUiState
import com.example.gpstracker.ui.model.VehicleErrorsUi
import com.example.gpstracker.ui.model.VehicleProfileUi
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

    // ---- v2: account, vehicle, insights ----

    private const val PREVIEW_NOW = 1_760_000_000_000L
    private val calculator = InsightsCalculator()
    private val history = FakeData.history(PREVIEW_NOW)
    private val lastWeekStart = PREVIEW_NOW - InsightsCalculator.WEEK_MS

    private fun insights(
        records: List<TripRecord> = history,
        period: InsightsPeriod = InsightsPeriod.Week,
        vehicle: VehicleNumbers = FakeData.demoVehicle,
    ) = calculator.compute(records, PREVIEW_NOW, period, vehicle)

    val insightsWeekBetter = insights()

    /** Month view where braking got worse than the benchmark. */
    val insightsMonthWorse = insights(
        records = history.map { it.copy(harshBrakings = it.harshBrakings + 3) },
        period = InsightsPeriod.Month,
    )

    val insightsNotEnoughData = insights(
        records = history.filter { it.startMs <= lastWeekStart } +
            history.filter { it.startMs > lastWeekStart }.take(2),
    )

    val insightsNoPrice = insights(vehicle = FakeData.demoVehicle.copy(fuelPricePerL = null))

    val settingsSignedIn = SettingsUiState(accountName = "Kamila K.", accountEmail = "kamila@example.com")

    val signInEmpty = SignInUiState()
    val signInFilled = SignInUiState(email = "demo@drivemetrics.app", password = "demo1234")
    val signInLoading = signInFilled.copy(isLoading = true)
    val signInWrongPassword = signInFilled.copy(formError = "Email or password is incorrect.")
    val signInInvalidEmail = SignInUiState(email = "kamila@", password = "demo1234", emailError = "Enter a valid email address.")

    val signUpEmpty = SignUpUiState()
    val signUpErrors = SignUpUiState(
        name = "",
        email = "kamila@",
        password = "abc",
        confirmPassword = "abcd",
        acceptedPrivacy = true,
        nameError = "Enter your name.",
        emailError = "Enter a valid email address.",
        passwordError = "Use at least 8 characters with a letter and a number.",
        confirmError = "Passwords do not match.",
    )
    val signUpEmailExists = SignUpUiState(
        name = "Kamila",
        email = "demo@drivemetrics.app",
        password = "demo1234",
        confirmPassword = "demo1234",
        acceptedPrivacy = true,
        formError = "An account with this email already exists.",
    )
    val signUpLoading = signUpEmailExists.copy(formError = null, isLoading = true)

    val vehicleDefault = VehicleProfileUi()
    val vehicleInvalid = VehicleProfileUi(consumptionL100 = "40")
    val vehicleInvalidErrors = VehicleErrorsUi(consumption = "Enter a value between 2.0 and 25.0.")
}
