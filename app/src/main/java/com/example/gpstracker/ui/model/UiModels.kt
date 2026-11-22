package com.example.gpstracker.ui.model

/*
 * UI models only. They are already formatted for display; the domain layer
 * will map into them later, so no screen depends on domain or fake classes.
 */

enum class ConnectionStatus { Live, Replay, Connecting, Reconnecting, Offline }

enum class GpsStatus { Good, Weak, Lost }

/** Good >= 85, Fair 60..84, Risky < 60. */
enum class ScoreBand { Good, Fair, Risky }

enum class EventType { Speeding, HarshBraking, HarshAcceleration, SharpCorner, CrashSuspected }

/** Comes from the device data, never from the user. */
enum class VehicleState {
    /** Trip in progress, car moving. */
    Driving,

    /** Trip in progress, car standing (traffic light, jam); the trip is not ended yet. */
    Stopped,

    /** No trip in progress: ignition off, or stationary long enough. */
    Parked,
}

/** Version 1 supports petrol and diesel; hybrid users choose petrol, EVs are out of scope. */
enum class FuelType { Petrol, Diesel }

enum class InsightsPeriod { Week, Month }

private const val GOOD_SCORE_MIN = 85
private const val FAIR_SCORE_MIN = 60

fun scoreBandOf(score: Int): ScoreBand = when {
    score >= GOOD_SCORE_MIN -> ScoreBand.Good
    score >= FAIR_SCORE_MIN -> ScoreBand.Fair
    else -> ScoreBand.Risky
}

data class GeoPointUi(val lat: Double, val lon: Double)

data class DrivingEventUi(
    val id: String,
    val type: EventType,
    val title: String,
    val detail: String,
    val penalty: Int,
    val timeLabel: String,
    val placeLabel: String,
    val position: GeoPointUi,
)

/** Speed over the limit that is not flagged as over limit. */
private const val OVER_LIMIT_TOLERANCE_KMH = 5

data class DriveUiState(
    val connection: ConnectionStatus = ConnectionStatus.Connecting,
    val gps: GpsStatus = GpsStatus.Good,
    val vehicleState: VehicleState = VehicleState.Parked,
    /** "Trip started 14:05"; null when parked. */
    val tripStartedLabel: String? = null,
    /** Shown on the dashboard while parked. */
    val lastTrip: TripSummaryUi? = null,
    /** Set once when a trip has just ended by itself; drives the "Trip saved" banner. */
    val tripJustSaved: TripSummaryUi? = null,
    val speedKmh: Int = 0,
    val speedLimitKmh: Int? = null,
    val score: Int = 100,
    val scoreBand: ScoreBand = ScoreBand.Good,
    val distanceKm: Double = 0.0,
    val durationLabel: String = "00:00",
    val eventCount: Int = 0,
    val latestEvent: DrivingEventUi? = null,
    val route: List<GeoPointUi> = emptyList(),
    val events: List<DrivingEventUi> = emptyList(),
    val avgSpeedKmh: Int = 0,
    val retryInSeconds: Int = 4,
    /** Where the car is now (also its parked position). */
    val position: GeoPointUi? = null,
    /** Route of the most recent finished trip, drawn dimmed on the map while parked. */
    val lastTripRoute: List<GeoPointUi> = emptyList(),
    /** Reports from drivers, shown on the map. */
    val reports: List<RoadReportUi> = emptyList(),
    /** The nearest report ahead; drives the warning banner. */
    val reportAhead: ReportAheadUi? = null,
    /** A report the car just passed: the driver is asked whether it is still there. */
    val stillThere: RoadReportUi? = null,
    /** False until the car's position is known; reporting needs a place. */
    val canReport: Boolean = false,
) {
    val overLimitBy: Int get() = speedLimitKmh?.let { (speedKmh - it).coerceAtLeast(0) } ?: 0
    val isOverLimit: Boolean get() = overLimitBy > OVER_LIMIT_TOLERANCE_KMH
    val isTripInProgress: Boolean get() = vehicleState != VehicleState.Parked
}

data class TripSummaryUi(
    val id: String,
    val dateLabel: String,
    val timeRange: String,
    val fromTo: String,
    val distanceKm: Double,
    val durationLabel: String,
    val score: Int,
    val scoreBand: ScoreBand,
    val eventCount: Int,
)

/**
 * Fuel and money estimate for one trip or one period. All values are estimates.
 *
 * @property wastedCostLabel formatted cost such as "€0.19", or null when the user has not
 * entered a fuel price (money is then hidden and the card asks for the price)
 */
data class FuelEstimateUi(
    val fuelUsedL: Double,
    val fuelWastedL: Double,
    val wastedCostLabel: String?,
    val co2Kg: Double,
)

data class TripDetailUi(
    val summary: TripSummaryUi,
    val avgSpeedKmh: Int,
    val maxSpeedKmh: Int,
    val route: List<GeoPointUi>,
    val events: List<DrivingEventUi>,
    val fuel: FuelEstimateUi,
    val tips: List<String>,
)

data class TripsUiState(
    /** "Driving now, started 14:05"; null when parked. */
    val tripInProgressLabel: String? = null,
    val weekAverageScore: Int? = null,
    val weekScores: List<Int> = emptyList(),
    /** Trips saved in the last seven days. */
    val weekTripCount: Int = 0,
    val sections: List<Pair<String, List<TripSummaryUi>>> = emptyList(),
)

/** Vehicle inputs for fuel estimates. Number fields stay text so the user can type freely. */
data class VehicleProfileUi(
    val fuelType: FuelType = FuelType.Petrol,
    val consumptionL100: String = "6.5",
    val fuelPricePerL: String = "",
    val monthlyKm: String = "1000",
)

data class SettingsUiState(
    val accountName: String? = null,
    val accountEmail: String? = null,
    /** The server the app talks to; set by the build, shown for information only. */
    val serverAddress: String = "",
    val speedTolerance: Int = 5,
    /** The car must stand still this long before a trip ends: 2, 5 or 10 minutes. */
    val tripEndAfterMinutes: Int = 5,
    val crashDetectionEnabled: Boolean = true,
    val emergencyContactName: String = "",
    /** As typed; optional. See PhoneNumbers for what counts as a number. */
    val emergencyContactPhone: String = "",
    val vehicle: VehicleProfileUi = VehicleProfileUi(),
    val appVersion: String = "0.2.0",
) {
    val isGuest: Boolean get() = accountName == null
}

/** What happened when the alert went out. */
enum class AlertOutcome {
    /** The text was sent and the call started. */
    TextedAndCalled,
    TextedOnly,
    CalledOnly,

    /** No phone number in Settings. */
    NoNumber,

    /** The driver has not allowed texts and calls. */
    NotAllowed,

    /** The phone could neither text nor call, for example a tablet. */
    Failed,
}

data class CrashAlertUiState(
    val visible: Boolean = false,
    val secondsLeft: Int = 10,
    val sent: Boolean = false,
    val outcome: AlertOutcome? = null,
) {
    /** The contact was reached automatically, at least by text or by call. */
    val reached: Boolean get() = outcome in setOf(AlertOutcome.TextedAndCalled, AlertOutcome.TextedOnly, AlertOutcome.CalledOnly)
}
