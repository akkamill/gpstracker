package com.example.gpstracker.data.fake

import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.TripDetailUi
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.model.TripsUiState
import com.example.gpstracker.ui.model.scoreBandOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Drives the demo. Every [tickMs] it moves along the fake route, follows a
 * looping speed curve, and, while a trip is active, emits scripted events and
 * updates the score. Later it is replaced by the real domain pipeline without
 * touching any screen.
 */
class FakeDriveSimulator(
    private val clock: () -> Long = System::currentTimeMillis,
    private val tickMs: Long = TICK_MS,
) {
    private val lock = Any()
    private val route = FakeData.route

    private val _state = MutableStateFlow(
        DriveUiState(
            connection = ConnectionStatus.Connecting,
            speedLimitKmh = 50,
            route = route.take(1),
        ),
    )
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    private var summaries: List<TripSummaryUi> = FakeData.trips
    private val _trips = MutableStateFlow(FakeData.tripsState)
    val trips: StateFlow<TripsUiState> = _trips.asStateFlow()

    private val details = HashMap<String, TripDetailUi>(FakeData.tripDetails)

    private var tick = 0L
    private var run: TripRun? = null
    private var tripCounter = 0
    private var job: Job? = null

    /** Starts the ticking loop in [scope]; calling it again has no effect. */
    fun start(scope: CoroutineScope) {
        synchronized(lock) {
            if (job != null) return
            job = scope.launch {
                while (true) {
                    tick()
                    delay(tickMs)
                }
            }
        }
    }

    fun startTrip() = synchronized(lock) {
        if (run == null) run = TripRun(startMs = clock())
        publish(snapshot = lastSnapshot)
    }

    fun stopTrip() = synchronized(lock) {
        val finished = run ?: return@synchronized
        run = null
        tripCounter++
        val summary = finished.toSummary(id = "trip-sim-$tripCounter", endMs = clock())
        details[summary.id] = finished.toDetail(summary)
        summaries = listOf(summary) + summaries
        _trips.value = buildTripsState(summaries)
        publish(snapshot = lastSnapshot)
    }

    /** The detail of a finished trip, or null when unknown. */
    fun tripDetail(id: String): TripDetailUi? = synchronized(lock) { details[id] }

    // ---- ticking ----

    private data class Snapshot(
        val speed: Int,
        val limit: Int,
        val gps: GpsStatus,
        val connection: ConnectionStatus,
        val position: GeoPointUi,
        val retryIn: Int,
    )

    private var lastSnapshot = Snapshot(0, 50, GpsStatus.Good, ConnectionStatus.Connecting, route.first(), 4)

    /** Advances the simulation by one step. Public so tests can drive it without delays. */
    fun tick() = synchronized(lock) {
        tick++
        val cycleTick = (tick % CYCLE_TICKS).toInt()
        val speed = speedAt(cycleTick)
        val limit = limitAt(cycleTick)
        val position = route[(cycleTick * (route.size - 1)) / (CYCLE_TICKS - 1)]
        val gps = if (cycleTick in GPS_LOST_TICKS) GpsStatus.Lost else GpsStatus.Good
        val periodTick = (tick % RECONNECT_PERIOD_TICKS).toInt()
        val connection = when {
            tick < CONNECT_TICKS -> ConnectionStatus.Connecting
            periodTick in RECONNECT_TICKS -> ConnectionStatus.Reconnecting
            else -> ConnectionStatus.Replay
        }
        val retryIn = if (connection == ConnectionStatus.Reconnecting) {
            ((RECONNECT_TICKS.last - periodTick) * tickMs / MS_PER_SECOND).toInt() + 1
        } else {
            0
        }
        lastSnapshot = Snapshot(speed, limit, gps, connection, position, retryIn)

        run?.let { r ->
            r.ticks++
            if (gps != GpsStatus.Lost) {
                r.distanceKm += speed / KMH_PER_KM_PER_SECOND * (tickMs / MS_PER_SECOND.toDouble())
                r.route.add(position)
                r.maxSpeed = max(r.maxSpeed, speed)
                r.speedSum += speed
                r.speedSamples++
                detectEvents(r, cycleTick, speed, limit, position)
            }
        }
        publish(lastSnapshot)
    }

    private fun detectEvents(r: TripRun, cycleTick: Int, speed: Int, limit: Int, position: GeoPointUi) {
        if (speed - limit > OVER_LIMIT_TOLERANCE) {
            r.overTicks++
            if (r.overTicks >= SPEEDING_TICKS && !r.speedingFired) {
                r.speedingFired = true
                addEvent(r, EventType.Speeding, "Speeding", "$speed in a $limit zone", PENALTY_SPEEDING, position)
            }
        } else {
            r.overTicks = 0
            r.speedingFired = false
        }
        if (cycleTick == BRAKING_TICK) {
            addEvent(r, EventType.HarshBraking, "Harsh braking", "−4.1 m/s²", PENALTY_BRAKING, position)
        }
        if (cycleTick == CORNER_TICK) {
            addEvent(r, EventType.SharpCorner, "Sharp corner", "0.5 g lateral", PENALTY_CORNER, position)
        }
        if (r.latestEvent != null && tick - r.latestEventTick > BANNER_TICKS) r.latestEvent = null
    }

    private fun addEvent(r: TripRun, type: EventType, title: String, detail: String, penalty: Int, at: GeoPointUi) {
        val event = DrivingEventUi(
            id = "sim-${++r.eventCounter}-$tick",
            type = type,
            title = title,
            detail = detail,
            penalty = penalty,
            timeLabel = timeLabel(clock()),
            placeLabel = PLACES[(r.route.size / PLACE_SPAN).coerceAtMost(PLACES.lastIndex)],
            position = at,
        )
        r.events.add(event)
        r.latestEvent = event
        r.latestEventTick = tick
    }

    private fun publish(snapshot: Snapshot) {
        val r = run
        val score = r?.score() ?: FULL_SCORE
        _state.value = DriveUiState(
            connection = snapshot.connection,
            gps = snapshot.gps,
            isTripActive = r != null,
            speedKmh = snapshot.speed,
            speedLimitKmh = snapshot.limit,
            score = score,
            scoreBand = scoreBandOf(score),
            distanceKm = r?.distanceKm ?: 0.0,
            durationLabel = r?.let { durationLabel(it.ticks * tickMs) } ?: "00:00",
            eventCount = r?.events?.size ?: 0,
            latestEvent = r?.latestEvent,
            route = r?.route?.toList() ?: listOf(snapshot.position),
            events = r?.events?.toList() ?: emptyList(),
            avgSpeedKmh = r?.avgSpeed() ?: 0,
            retryInSeconds = snapshot.retryIn,
        )
    }

    // ---- trip bookkeeping ----

    private inner class TripRun(val startMs: Long) {
        var ticks = 0
        var distanceKm = 0.0
        var maxSpeed = 0
        var speedSum = 0L
        var speedSamples = 0
        var overTicks = 0
        var speedingFired = false
        var eventCounter = 0
        var latestEvent: DrivingEventUi? = null
        var latestEventTick = 0L
        val route = ArrayList<GeoPointUi>()
        val events = ArrayList<DrivingEventUi>()

        fun avgSpeed() = if (speedSamples == 0) 0 else (speedSum / speedSamples).toInt()

        /** `100 − 10 × penalties / max(km, 1)`, clamped to 0..100. */
        fun score(): Int {
            val penalties = events.sumOf { it.penalty }
            return (FULL_SCORE - SCORE_FACTOR * penalties / max(distanceKm, 1.0)).roundToInt().coerceIn(0, FULL_SCORE)
        }

        fun toSummary(id: String, endMs: Long): TripSummaryUi {
            val minutes = max(1, (ticks * tickMs / MS_PER_MINUTE).toInt())
            val score = score()
            return TripSummaryUi(
                id = id,
                dateLabel = "Today",
                timeRange = "${timeLabel(startMs)} – ${timeLabel(endMs)}",
                fromTo = "Žaliakalnis → Akropolis",
                distanceKm = distanceKm,
                durationLabel = "$minutes min",
                score = score,
                scoreBand = scoreBandOf(score),
                eventCount = events.size,
            )
        }

        fun toDetail(summary: TripSummaryUi) = TripDetailUi(
            summary = summary,
            avgSpeedKmh = avgSpeed(),
            maxSpeedKmh = maxSpeed,
            route = route.toList().ifEmpty { FakeData.route.take(2) },
            events = events.toList(),
            tips = tipsFor(events),
        )
    }

    private fun buildTripsState(list: List<TripSummaryUi>): TripsUiState {
        val recent = list.take(WEEK_TRIPS)
        return TripsUiState(
            weekAverageScore = recent.map { it.score }.average().roundToInt(),
            weekScores = recent.map { it.score }.reversed(),
            sections = list.groupBy { it.dateLabel }.toList(),
        )
    }

    private fun tipsFor(events: List<DrivingEventUi>): List<String> {
        val types = events.map { it.type }.toSet()
        return buildList {
            if (EventType.Speeding in types) add("Keep to the posted limit, especially on Savanorių pr.")
            if (EventType.HarshBraking in types) add("Brake earlier and more gently before junctions.")
            if (EventType.HarshAcceleration in types) add("Ease into the throttle when pulling away.")
            if (EventType.SharpCorner in types) add("Slow down before corners rather than in them.")
        }.take(MAX_TIPS)
    }

    private fun timeLabel(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

    private fun durationLabel(ms: Long): String {
        val seconds = (ms / MS_PER_SECOND).toInt()
        return "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }

    private companion object {
        const val TICK_MS = 500L
        const val CYCLE_TICKS = 120
        const val CONNECT_TICKS = 4
        const val RECONNECT_PERIOD_TICKS = 180
        val RECONNECT_TICKS = 160..171
        val GPS_LOST_TICKS = 80..92
        const val BRAKING_TICK = 42
        const val CORNER_TICK = 100
        const val SPEEDING_TICKS = 6
        const val BANNER_TICKS = 7
        const val OVER_LIMIT_TOLERANCE = 5
        const val PENALTY_SPEEDING = 4
        const val PENALTY_BRAKING = 3
        const val PENALTY_CORNER = 2
        const val FULL_SCORE = 100
        const val SCORE_FACTOR = 10.0
        const val WEEK_TRIPS = 7
        const val MAX_TIPS = 3
        const val MS_PER_SECOND = 1_000
        const val MS_PER_MINUTE = 60_000
        const val SECONDS_PER_MINUTE = 60
        const val KMH_PER_KM_PER_SECOND = 3_600.0
        const val PLACE_SPAN = 20
        val PLACES = listOf("Žaliakalnis", "Savanorių pr.", "Jonavos g.", "Karaliaus Mindaugo pr.")

        /** Speed keyframes (cycle tick to km/h): 0 → 45 → 52 → 30 → 68 → 20 → 0, looping. */
        val SPEED_KEYS = listOf(0 to 0, 10 to 30, 20 to 45, 40 to 52, 55 to 30, 70 to 68, 90 to 20, 110 to 0, 120 to 0)

        fun speedAt(cycleTick: Int): Int {
            val next = SPEED_KEYS.indexOfFirst { it.first >= cycleTick }.coerceAtLeast(1)
            val (t0, v0) = SPEED_KEYS[next - 1]
            val (t1, v1) = SPEED_KEYS[next]
            if (t1 == t0) return v1
            return (v0 + (v1 - v0) * (cycleTick - t0).toDouble() / (t1 - t0)).roundToInt()
        }

        /** Speed limit per segment: 50, 70, 50, 30. */
        fun limitAt(cycleTick: Int): Int = when {
            cycleTick < 40 -> 50
            cycleTick < 60 -> 70
            cycleTick < 90 -> 50
            else -> 30
        }
    }
}
