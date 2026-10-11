package com.example.gpstracker.data.fake

import com.example.gpstracker.data.estimate.FuelEstimator
import com.example.gpstracker.data.estimate.FuelInput
import com.example.gpstracker.data.estimate.TripDetector
import com.example.gpstracker.data.estimate.TripEvent
import com.example.gpstracker.data.estimate.TripRecord
import com.example.gpstracker.data.estimate.TripSample
import com.example.gpstracker.data.estimate.VehicleNumbers
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.TripDetailUi
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.model.TripsUiState
import com.example.gpstracker.ui.model.VehicleState
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
 * Simulates a car, not a trip. It emits a sample (speed, limit, position, ignition)
 * every [tickMs] and lets [TripDetector] decide when trips begin and end, exactly
 * as it will with real device data. There is no start or stop control.
 *
 * One loop of "a day" in demo time (about 3 minutes):
 * parked, ignition on, drive the Kaunas route with one traffic-light stop,
 * arrive, ignition off, parked. The next loop drives the route in reverse.
 *
 * Demo time runs [SIM_MS_PER_TICK] / [TICK_MS] times faster than real time so that
 * trip-detection thresholds (5 s, 30 s, 5 min) behave realistically in a short demo.
 */
class FakeDriveSimulator(
    private val clock: () -> Long = System::currentTimeMillis,
    private val tickMs: Long = TICK_MS,
    private val vehicleProvider: () -> VehicleNumbers = { FakeData.demoVehicle.copy(fuelPricePerL = null) },
    private val tripEndAfterMinutes: () -> Int = { DEFAULT_END_AFTER_MIN },
) {
    private val lock = Any()
    private val route = FakeData.route
    private val estimator = FuelEstimator()
    private val detector = TripDetector()
    private val simStartMs = clock()

    private var summaries: List<TripSummaryUi> = FakeData.trips
    private var lastTripRoute: List<GeoPointUi> = FakeData.route

    private val _state = MutableStateFlow(
        DriveUiState(
            connection = ConnectionStatus.Connecting,
            lastTrip = summaries.firstOrNull(),
            position = route.first(),
            lastTripRoute = lastTripRoute,
        ),
    )
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    private val _trips = MutableStateFlow(FakeData.tripsState)
    val trips: StateFlow<TripsUiState> = _trips.asStateFlow()

    private val details = HashMap<String, TripDetailUi>(FakeData.tripDetails)

    /** Finished trips as numbers; Insights are computed from these. */
    private val _records = MutableStateFlow(FakeData.history(simStartMs))
    val records: StateFlow<List<TripRecord>> = _records.asStateFlow()

    private var tickIndex = 0L
    private var cycleOffset = 0
    private var run: TripRun? = null
    private var tripCounter = 0
    private var savedTrip: TripSummaryUi? = null
    private var savedAtTick = 0L
    private var job: Job? = null
    private var snapshot = Snapshot()

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

    /**
     * Debug shortcut for presentations: jumps the simulated car to the moment the
     * ignition is turned on. It controls the car, not the trip.
     */
    fun skipToNextDrive() = synchronized(lock) {
        val cycleTick = ((tickIndex + cycleOffset) % CYCLE_TICKS).toInt()
        cycleOffset += (IGNITION_ON_TICK - cycleTick + CYCLE_TICKS) % CYCLE_TICKS
    }

    /** The detail of a finished trip, or null when unknown. */
    fun tripDetail(id: String): TripDetailUi? = synchronized(lock) { details[id] }

    // ---- ticking ----

    private data class Snapshot(
        val speed: Int = 0,
        val limit: Int? = null,
        val gps: GpsStatus = GpsStatus.Good,
        val connection: ConnectionStatus = ConnectionStatus.Connecting,
        val position: GeoPointUi? = null,
        val retryIn: Int = 0,
    )

    /** Advances the simulation by one step. Public so tests can drive it without delays. */
    fun tick() = synchronized(lock) {
        tickIndex++
        val simMs = simStartMs + tickIndex * SIM_MS_PER_TICK
        val position = tickIndex + cycleOffset
        val cycleTick = (position % CYCLE_TICKS).toInt()
        val loop = (position / CYCLE_TICKS).toInt()

        val driveTick = cycleTick - DRIVE_START_TICK
        val driving = driveTick in 0 until DRIVE_TICKS
        val ignitionOn = cycleTick in IGNITION_ON_TICK until (DRIVE_START_TICK + DRIVE_TICKS)
        val speed = if (driving) speedAt(driveTick) else 0
        val limit = if (driving) limitAt(driveTick) else null
        val reversed = loop % 2 == 1
        val path = if (reversed) route.asReversed() else route
        val point = when {
            driving -> path[(driveTick * (path.size - 1)) / (DRIVE_TICKS - 1)]
            cycleTick < DRIVE_START_TICK -> path.first()
            else -> path.last()
        }
        val gps = if (driving && driveTick in GPS_LOST_TICKS) GpsStatus.Lost else GpsStatus.Good
        val reconnecting = driving && driveTick in RECONNECT_TICKS
        val connection = when {
            tickIndex < CONNECT_TICKS -> ConnectionStatus.Connecting
            reconnecting -> ConnectionStatus.Reconnecting
            else -> ConnectionStatus.Replay
        }
        val retryIn = if (reconnecting) {
            ((RECONNECT_TICKS.last - driveTick) * tickMs / MS_PER_SECOND).toInt() + 1
        } else {
            0
        }
        snapshot = Snapshot(speed, limit, gps, connection, point, retryIn)

        detector.setTripEndAfterMinutes(tripEndAfterMinutes())
        val events = detector.onSample(
            TripSample(simMs, speed.toDouble(), ignitionOn = ignitionOn, gpsValid = gps != GpsStatus.Lost),
        )
        events.forEach { handleTripEvent(it, reversed) }

        run?.let { r ->
            r.ticks++
            if (gps != GpsStatus.Lost) {
                r.route.add(point)
                r.maxSpeed = max(r.maxSpeed, speed)
                r.speedSum += speed
                r.speedSamples++
                detectEvents(r, driveTick, speed, limit ?: 0, point, simMs)
            }
            if (r.latestEvent != null && tickIndex - r.latestEventTick > BANNER_TICKS) r.latestEvent = null
        }
        if (savedTrip != null && tickIndex - savedAtTick > SAVED_TICKS) savedTrip = null
        publish(simMs)
    }

    private fun handleTripEvent(event: TripEvent, reversed: Boolean) {
        when (event) {
            is TripEvent.Started -> run = TripRun(startMs = event.startMs, reversed = reversed)
            is TripEvent.Discarded -> run = null
            is TripEvent.Ended -> {
                val finished = run ?: return
                run = null
                finish(finished, event)
            }
        }
    }

    private fun finish(r: TripRun, event: TripEvent.Ended) {
        tripCounter++
        val summary = r.toSummary("trip-sim-$tripCounter", event)
        val record = r.toRecord(summary, event)
        details[summary.id] = r.toDetail(summary, record)
        _records.value = _records.value + record
        summaries = listOf(summary) + summaries
        lastTripRoute = r.route.toList().ifEmpty { lastTripRoute }
        savedTrip = summary
        savedAtTick = tickIndex
    }

    private fun detectEvents(r: TripRun, driveTick: Int, speed: Int, limit: Int, position: GeoPointUi, simMs: Long) {
        if (speed - limit > OVER_LIMIT_TOLERANCE) {
            r.overTicks++
            r.speedingTicks++
            if (r.overTicks >= SPEEDING_TICKS && !r.speedingFired) {
                r.speedingFired = true
                addEvent(r, EventType.Speeding, "Speeding", "$speed in a $limit zone", PENALTY_SPEEDING, position, simMs)
            }
        } else {
            r.overTicks = 0
            r.speedingFired = false
        }
        if (driveTick == BRAKING_TICK) {
            addEvent(r, EventType.HarshBraking, "Harsh braking", "−4.1 m/s²", PENALTY_BRAKING, position, simMs)
        }
        if (driveTick == CORNER_TICK) {
            addEvent(r, EventType.SharpCorner, "Sharp corner", "0.5 g lateral", PENALTY_CORNER, position, simMs)
        }
    }

    private fun addEvent(
        r: TripRun,
        type: EventType,
        title: String,
        detail: String,
        penalty: Int,
        at: GeoPointUi,
        simMs: Long,
    ) {
        val event = DrivingEventUi(
            id = "sim-${++r.eventCounter}-$tickIndex",
            type = type,
            title = title,
            detail = detail,
            penalty = penalty,
            timeLabel = timeLabel(simMs),
            placeLabel = PLACES[(r.route.size / PLACE_SPAN).coerceAtMost(PLACES.lastIndex)],
            position = at,
        )
        r.events.add(event)
        r.latestEvent = event
        r.latestEventTick = tickIndex
    }

    private fun publish(simMs: Long) {
        val r = run
        val s = snapshot
        val km = if (r != null) detector.distanceKm else 0.0
        val score = r?.score(km) ?: FULL_SCORE
        val startLabel = r?.let { timeLabel(it.startMs) }
        _state.value = DriveUiState(
            connection = s.connection,
            gps = s.gps,
            vehicleState = detector.state,
            tripStartedLabel = startLabel?.let { "Trip started $it" },
            lastTrip = summaries.firstOrNull(),
            tripJustSaved = savedTrip,
            speedKmh = s.speed,
            speedLimitKmh = s.limit,
            score = score,
            scoreBand = scoreBandOf(score),
            distanceKm = km,
            durationLabel = r?.let { durationLabel(simMs - it.startMs) } ?: "00:00",
            eventCount = r?.events?.size ?: 0,
            latestEvent = r?.latestEvent,
            route = r?.route?.toList() ?: emptyList(),
            events = r?.events?.toList() ?: emptyList(),
            avgSpeedKmh = r?.avgSpeed() ?: 0,
            retryInSeconds = s.retryIn,
            position = s.position,
            lastTripRoute = lastTripRoute,
        )
        _trips.value = buildTripsState(summaries, startLabel)
    }

    // ---- trip bookkeeping ----

    private inner class TripRun(val startMs: Long, val reversed: Boolean) {
        var ticks = 0
        var maxSpeed = 0
        var speedSum = 0L
        var speedSamples = 0
        var overTicks = 0
        var speedingTicks = 0
        var speedingFired = false
        var eventCounter = 0
        var latestEvent: DrivingEventUi? = null
        var latestEventTick = 0L
        val route = ArrayList<GeoPointUi>()
        val events = ArrayList<DrivingEventUi>()

        fun avgSpeed() = if (speedSamples == 0) 0 else (speedSum / speedSamples).toInt()

        /** `100 − 10 × penalties / max(km, 1)`, clamped to 0..100. */
        fun score(distanceKm: Double): Int {
            val penalties = events.sumOf { it.penalty }
            return (FULL_SCORE - SCORE_FACTOR * penalties / max(distanceKm, 1.0)).roundToInt().coerceIn(0, FULL_SCORE)
        }

        fun toSummary(id: String, event: TripEvent.Ended): TripSummaryUi {
            val minutes = max(1, ((event.endMs - event.startMs) / MS_PER_MINUTE).toInt())
            val score = score(event.distanceKm)
            return TripSummaryUi(
                id = id,
                dateLabel = "Today",
                timeRange = "${timeLabel(event.startMs)} – ${timeLabel(event.endMs)}",
                fromTo = if (reversed) "Akropolis → Žaliakalnis" else "Žaliakalnis → Akropolis",
                distanceKm = event.distanceKm,
                durationLabel = "$minutes min",
                score = score,
                scoreBand = scoreBandOf(score),
                eventCount = events.size,
            )
        }

        fun toRecord(summary: TripSummaryUi, event: TripEvent.Ended) = TripRecord(
            id = summary.id,
            // history is on the real calendar, so the record is stamped with the real clock
            startMs = clock(),
            distanceKm = event.distanceKm,
            score = summary.score,
            harshAccelerations = events.count { it.type == EventType.HarshAcceleration },
            harshBrakings = events.count { it.type == EventType.HarshBraking },
            speedingEvents = events.count { it.type == EventType.Speeding },
            sharpCorners = events.count { it.type == EventType.SharpCorner },
            speedingMinutes = speedingTicks * SIM_MS_PER_TICK / MS_PER_MINUTE.toDouble(),
        )

        fun toDetail(summary: TripSummaryUi, record: TripRecord): TripDetailUi {
            val vehicle = vehicleProvider()
            val estimate = estimator.estimate(
                FuelInput(
                    distanceKm = record.distanceKm,
                    consumptionL100 = vehicle.consumptionL100,
                    harshAccelerations = record.harshAccelerations,
                    harshBrakings = record.harshBrakings,
                    speedingMinutes = record.speedingMinutes,
                    fuelType = vehicle.fuelType,
                ),
            )
            return TripDetailUi(
                summary = summary,
                avgSpeedKmh = avgSpeed(),
                maxSpeedKmh = maxSpeed,
                route = route.toList().ifEmpty { FakeData.route.take(2) },
                events = events.toList(),
                fuel = estimate.toUi(vehicle.fuelPricePerL),
                tips = tipsFor(events),
            )
        }
    }

    private fun buildTripsState(list: List<TripSummaryUi>, startLabel: String?): TripsUiState {
        val recent = list.take(WEEK_TRIPS)
        return TripsUiState(
            tripInProgressLabel = startLabel?.let { "Driving now, started $it" },
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
        val seconds = (ms / MS_PER_SECOND).toInt().coerceAtLeast(0)
        return "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }

    private companion object {
        const val TICK_MS = 500L

        /** Demo time per tick: 10x real time. This is the single compression constant. */
        const val SIM_MS_PER_TICK = 5_000L

        const val DEFAULT_END_AFTER_MIN = 5
        const val CONNECT_TICKS = 4

        // One loop: parked, ignition on (idle), drive, parked.
        const val PARKED_TICKS = 60
        const val IDLE_TICKS = 4
        const val DRIVE_TICKS = 210
        const val IGNITION_ON_TICK = PARKED_TICKS
        const val DRIVE_START_TICK = PARKED_TICKS + IDLE_TICKS
        const val CYCLE_TICKS = PARKED_TICKS + IDLE_TICKS + DRIVE_TICKS + PARKED_TICKS

        val RECONNECT_TICKS = 40..51
        val GPS_LOST_TICKS = 110..121
        const val BRAKING_TICK = 78
        const val CORNER_TICK = 130
        const val SPEEDING_TICKS = 6
        const val BANNER_TICKS = 7
        const val SAVED_TICKS = 14
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
        const val PLACE_SPAN = 20
        val PLACES = listOf("Žaliakalnis", "Savanorių pr.", "Jonavos g.", "Karaliaus Mindaugo pr.")

        /** Speed keyframes (drive tick to km/h), with a traffic-light stop at ticks 90..94. */
        val SPEED_KEYS = listOf(
            0 to 0, 18 to 30, 35 to 45, 60 to 52, 80 to 30, 90 to 0, 94 to 0,
            105 to 35, 125 to 55, 145 to 68, 160 to 68, 175 to 30, 195 to 20, 207 to 0, 210 to 0,
        )

        fun speedAt(driveTick: Int): Int {
            val next = SPEED_KEYS.indexOfFirst { it.first >= driveTick }.coerceAtLeast(1)
            val (t0, v0) = SPEED_KEYS[next - 1]
            val (t1, v1) = SPEED_KEYS[next]
            if (t1 == t0) return v1
            return (v0 + (v1 - v0) * (driveTick - t0).toDouble() / (t1 - t0)).roundToInt()
        }

        /** Speed limit per segment: 50, 70, 50, 30. */
        fun limitAt(driveTick: Int): Int = when {
            driveTick < 60 -> 50
            driveTick < 90 -> 70
            driveTick < 180 -> 50
            else -> 30
        }
    }
}
