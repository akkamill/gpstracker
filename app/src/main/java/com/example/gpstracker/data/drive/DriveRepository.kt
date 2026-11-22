package com.example.gpstracker.data.drive

import com.example.gpstracker.data.estimate.TripRecord
import com.example.gpstracker.data.estimate.toNumbers
import com.example.gpstracker.data.fake.DemoHistory
import com.example.gpstracker.data.reports.RoadReportService
import com.example.gpstracker.domain.driving.Scoring
import com.example.gpstracker.domain.telemetry.TelemetrySource
import com.example.gpstracker.domain.trip.DriveSnapshot
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.domain.trip.InMemoryTripHistory
import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.model.trip.VehicleState
import com.example.gpstracker.tracing.SpanContext
import com.example.gpstracker.tracing.Tracing
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
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
 * Finished trips are kept in [history] (the phone's database in the app). The history is
 * loaded once at start; when it is empty, [demoHistory] fills it so Trips and Insights are
 * not empty in a demo. A finished trip shows up at once and is written in the background.
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
    private val history: TripHistory = InMemoryTripHistory(),
    private val demoHistory: (nowMs: Long) -> List<SavedTrip> = DemoHistory::build,
    private val reports: RoadReportService? = null,
) : DriveRepository {
    private val lock = Any()
    private val tracer = Tracing.tracer("app")
    private var scope: CoroutineScope? = null

    private var snapshot = DriveSnapshot()

    /** All finished trips, newest first, and what the screens show of them. */
    private var saved: List<SavedTrip> = emptyList()
    private var summaries: List<TripSummaryUi> = emptyList()
    private var summariesDay = 0L
    private var lastTripRoute: List<GeoPointUi> = emptyList()

    private var latestEvent: DrivingEventUi? = null
    private var latestEventAtMs = 0L
    private var savedTrip: TripSummaryUi? = null
    private var savedAtMs = 0L

    private val _drive = MutableStateFlow(DriveUiState())
    override val drive: StateFlow<DriveUiState> = _drive.asStateFlow()

    private val _trips = MutableStateFlow(mapper.tripsState(emptyList(), null))
    override val trips: StateFlow<TripsUiState> = _trips.asStateFlow()

    private val _records = MutableStateFlow<List<TripRecord>>(emptyList())
    override val records: StateFlow<List<TripRecord>> = _records.asStateFlow()

    /** Built when asked for, so the fuel estimate follows the vehicle profile as it is now. */
    override fun tripDetail(id: String): TripDetailUi? = synchronized(lock) {
        saved.firstOrNull { it.id == id }?.let { mapper.detail(it, clock(), settings.value.vehicle.toNumbers()) }
    }

    /** Starts listening in [scope]. Call once. */
    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch { loadHistory() }
        scope.launch {
            settings.map { it.speedTolerance to it.tripEndAfterMinutes }.distinctUntilChanged().collect { (tolerance, endAfter) ->
                synchronized(lock) {
                    tracker.rules = tracker.rules.copy(speedingToleranceKmh = tolerance)
                    tracker.setTripEndAfterMinutes(endAfter)
                }
                tracer.info("driving rules set", "toleranceKmh" to tolerance, "tripEndAfterMin" to endAfter)
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

    /** Runs in the sample's own trace, so the domain's logs join it. */
    private fun onPoint(point: TelemetryPoint) = synchronized(lock) {
        tracer.span("drive.update", parent = SpanContext.parse(point.traceparent)) { span ->
            span.attribute("speedKmh", point.speedKmh.roundToInt())
            process(point)
        }
    }

    private fun process(point: TelemetryPoint) {
        val update = tracker.onPoint(point)
        snapshot = update.snapshot
        update.newEvents.lastOrNull()?.let {
            latestEvent = mapper.event(it)
            latestEventAtMs = clock()
        }
        update.finishedTrip?.let(::save)
        reports?.onPoint(point, snapshot.road?.name, inTrip = snapshot.vehicleState != VehicleState.Parked)
        publish()
    }

    private suspend fun loadHistory() {
        var stored = history.all()
        if (stored.isEmpty()) {
            stored = demoHistory(clock())
            history.save(stored)
            tracer.info("demo trips added to the history", "trips" to stored.size)
        }
        synchronized(lock) {
            // a trip finished while loading is kept
            saved = (saved + stored).distinctBy { it.id }.sortedByDescending { it.savedAtMs }
            refreshHistory()
            publish()
        }
        tracer.info("trip history loaded", "trips" to stored.size)
    }

    private fun save(trip: Trip) {
        val entry = SavedTrip(id = "trip-${trip.startMs}", savedAtMs = clock(), trip = trip)
        saved = listOf(entry) + saved
        refreshHistory()
        savedTrip = summaries.first()
        savedAtMs = entry.savedAtMs
        latestEvent = null
        scope?.launch { history.save(listOf(entry)) }
        tracer.info("trip saved", "id" to entry.id, "route" to savedTrip?.fromTo, "score" to trip.score)
    }

    /** Rebuilds what the screens show of the history, after a change or when the day changes. */
    private fun refreshHistory() {
        val now = clock()
        summariesDay = mapper.dayNumber(now)
        summaries = saved.map { mapper.summary(it, now) }
        lastTripRoute = saved.firstOrNull { it.trip.route.isNotEmpty() }?.trip?.route?.map(mapper::point).orEmpty()
        _records.value = saved.asReversed().map(mapper::record)
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
        // "Today" becomes "Yesterday" at midnight
        if (mapper.dayNumber(now) != summariesDay) {
            refreshHistory()
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
