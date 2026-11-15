package com.example.gpstracker.data.drive

import com.example.gpstracker.data.estimate.TripRecord
import com.example.gpstracker.data.estimate.toNumbers
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.domain.driving.Scoring
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.domain.telemetry.TelemetrySource
import com.example.gpstracker.domain.trip.DriveSnapshot
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.model.trip.VehicleState
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.TripDetailUi
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.model.TripsUiState
import com.example.gpstracker.ui.model.scoreBandOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Live driving data and trip history, ready for the screens. */
interface DriveRepository {
    val drive: StateFlow<DriveUiState>
    val trips: StateFlow<TripsUiState>

    /** Finished trips as numbers; Insights are computed from these. */
    val records: StateFlow<List<TripRecord>>

    fun tripDetail(id: String): TripDetailUi?
}

/**
 * Runs the domain pipeline ([DriveTracker]) on the samples of a [TelemetrySource] and
 * publishes what the screens show. It does not care whether the samples come from the
 * simulated tracker or, later, from the server.
 *
 * History is kept in memory for now, seeded with sample trips so Trips and Insights
 * are not empty in a demo; a database replaces this later.
 *
 * @param connectedStatus how a working connection is labelled ("Demo replay" or "Live")
 */
class TelemetryDriveRepository(
    private val source: TelemetrySource,
    private val tracker: DriveTracker,
    private val settings: StateFlow<SettingsUiState>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val connectedStatus: ConnectionStatus = ConnectionStatus.Replay,
    private val mapper: DriveUiMapper = DriveUiMapper(),
    seedHistory: Boolean = true,
) : DriveRepository {
    private val lock = Any()

    private var snapshot = DriveSnapshot()
    private var summaries: List<TripSummaryUi> = if (seedHistory) FakeData.trips else emptyList()
    private val details = HashMap<String, TripDetailUi>(if (seedHistory) FakeData.tripDetails else emptyMap())
    private var lastTripRoute: List<GeoPointUi> = if (seedHistory) FakeData.route else emptyList()
    private var latestEvent: DrivingEventUi? = null
    private var latestEventAtMs = 0L
    private var savedTrip: TripSummaryUi? = null
    private var savedAtMs = 0L
    private var tripCounter = 0

    private val _drive = MutableStateFlow(
        DriveUiState(lastTrip = summaries.firstOrNull(), lastTripRoute = lastTripRoute),
    )
    override val drive: StateFlow<DriveUiState> = _drive.asStateFlow()

    private val _trips = MutableStateFlow(mapper.tripsState(summaries, null))
    override val trips: StateFlow<TripsUiState> = _trips.asStateFlow()

    private val _records = MutableStateFlow(if (seedHistory) FakeData.history(clock()) else emptyList())
    override val records: StateFlow<List<TripRecord>> = _records.asStateFlow()

    override fun tripDetail(id: String): TripDetailUi? = synchronized(lock) { details[id] }

    /** Starts listening in [scope]. Call once. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            settings.collect { s ->
                synchronized(lock) {
                    tracker.rules = tracker.rules.copy(speedingToleranceKmh = s.speedTolerance)
                    tracker.setTripEndAfterMinutes(s.tripEndAfterMinutes)
                }
            }
        }
        scope.launch { source.connection.collect { publish() } }
        scope.launch { source.points.collect(::onPoint) }
        scope.launch {
            while (true) {
                delay(BANNER_CHECK_MS)
                expireBanners()
            }
        }
    }

    private fun onPoint(point: TelemetryPoint) = synchronized(lock) {
        val update = tracker.onPoint(point)
        snapshot = update.snapshot
        update.newEvents.lastOrNull()?.let {
            latestEvent = mapper.event(it)
            latestEventAtMs = clock()
        }
        update.finishedTrip?.let(::save)
        publish()
    }

    private fun save(trip: Trip) {
        val id = "trip-${++tripCounter}-${trip.startMs}"
        val summary = mapper.summary(id, trip)
        val record = mapper.record(id, trip, savedAtMs = clock())
        details[id] = mapper.detail(summary, trip, record, settings.value.vehicle.toNumbers())
        summaries = listOf(summary) + summaries
        _records.value = _records.value + record
        if (trip.route.isNotEmpty()) lastTripRoute = trip.route.map(mapper::point)
        savedTrip = summary
        savedAtMs = clock()
        latestEvent = null
    }

    private fun expireBanners() = synchronized(lock) {
        val now = clock()
        var changed = false
        if (latestEvent != null && now - latestEventAtMs > EVENT_BANNER_MS) {
            latestEvent = null
            changed = true
        }
        if (savedTrip != null && now - savedAtMs > SAVED_BANNER_MS) {
            savedTrip = null
            changed = true
        }
        if (changed) publish()
    }

    private fun publish() = synchronized(lock) {
        val trip = snapshot.trip
        val point = snapshot.latest
        val parked = snapshot.vehicleState == VehicleState.Parked
        val score = trip?.score ?: Scoring.MAX_SCORE
        _drive.value = DriveUiState(
            connection = mapper.connection(source.connection.value, connectedStatus),
            gps = point?.let { mapper.gps(it.gnss) } ?: GpsStatus.Good,
            vehicleState = mapper.vehicleState(snapshot.vehicleState),
            tripStartedLabel = trip?.let { "Trip started ${mapper.timeLabel(it.startMs)}" },
            lastTrip = summaries.firstOrNull(),
            tripJustSaved = savedTrip,
            speedKmh = point?.speedKmh?.roundToInt() ?: 0,
            speedLimitKmh = if (parked) null else snapshot.road?.speedLimitKmh,
            score = score,
            scoreBand = scoreBandOf(score),
            distanceKm = trip?.distanceKm ?: 0.0,
            durationLabel = trip?.let { mapper.durationLabel(it.durationMs) } ?: ZERO_DURATION,
            eventCount = trip?.events?.size ?: 0,
            latestEvent = latestEvent.takeIf { trip != null },
            route = trip?.route?.map(mapper::point).orEmpty(),
            events = trip?.events?.map(mapper::event).orEmpty(),
            avgSpeedKmh = trip?.let { averageKmh(it.distanceKm, it.durationMs) } ?: 0,
            retryInSeconds = RETRY_SECONDS,
            position = point?.let { GeoPointUi(it.latitude, it.longitude) },
            lastTripRoute = lastTripRoute,
        )
        _trips.value = mapper.tripsState(summaries, trip?.startMs)
    }

    private fun averageKmh(km: Double, durationMs: Long): Int =
        if (durationMs <= 0) 0 else (km / (durationMs / MS_PER_HOUR)).roundToInt()

    private companion object {
        const val BANNER_CHECK_MS = 500L
        const val EVENT_BANNER_MS = 3_500L
        const val SAVED_BANNER_MS = 7_000L
        const val RETRY_SECONDS = 4
        const val ZERO_DURATION = "00:00"
        const val MS_PER_HOUR = 3_600_000.0
    }
}
