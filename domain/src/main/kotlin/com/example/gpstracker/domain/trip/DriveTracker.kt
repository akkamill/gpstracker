package com.example.gpstracker.domain.trip

import com.example.gpstracker.domain.driving.DrivingRules
import com.example.gpstracker.domain.driving.EventDetector
import com.example.gpstracker.domain.driving.Scoring
import com.example.gpstracker.domain.map.MapLookup
import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.map.RoadInfo
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.model.trip.VehicleState
import kotlin.math.max

/** The trip being driven right now. */
data class TripProgress(
    val startMs: Long,
    val durationMs: Long,
    val distanceKm: Double,
    val events: List<DrivingEvent>,
    val route: List<LatLon>,
    val maxSpeedKmh: Double,
    val speedingMs: Long,
    val score: Int,
    val startPlace: String?,
)

/** Everything known about the car right now. */
data class DriveSnapshot(
    val latest: TelemetryPoint? = null,
    val vehicleState: VehicleState = VehicleState.Parked,
    val road: RoadInfo? = null,
    val trip: TripProgress? = null,
)

/**
 * Result of one sample.
 *
 * @property newEvents events confirmed by this sample, for a live banner
 * @property finishedTrip set once, when a trip has just ended
 */
data class DriveUpdate(
    val snapshot: DriveSnapshot,
    val newEvents: List<DrivingEvent> = emptyList(),
    val finishedTrip: Trip? = null,
)

/**
 * The whole driving pipeline for one vehicle: samples in, live state, events and
 * finished trips out. It combines [TripDetector] (when do trips start and end),
 * [EventDetector] (what happened) and [Scoring] (how well), and uses [MapLookup]
 * for speed limits and place names.
 *
 * While the GPS has no fix the trip goes on (speed still comes from the car), but
 * the route is not extended and events are not checked: the score is paused.
 *
 * Pure Kotlin, no clock of its own, no I/O; the app and the server can both run it.
 */
class DriveTracker(
    rules: DrivingRules = DrivingRules(),
    private val map: MapLookup = MapLookup.NONE,
    tripConfig: TripDetectorConfig = TripDetectorConfig(),
) {
    private val tripDetector = TripDetector(tripConfig)
    private val eventDetector = EventDetector(rules)
    private var current: TripBuilder? = null
    private var latest: TelemetryPoint? = null
    private var road: RoadInfo? = null

    var rules: DrivingRules
        get() = eventDetector.rules
        set(value) {
            eventDetector.rules = value
        }

    fun setTripEndAfterMinutes(minutes: Int) = tripDetector.setTripEndAfterMinutes(minutes)

    /** Feeds one sample, in time order. */
    fun onPoint(point: TelemetryPoint): DriveUpdate {
        val previous = latest
        latest = point
        if (point.hasPosition) road = map.roadAt(point.latitude, point.longitude)
        val finished = handle(tripDetector.onSample(point.toTripSample()), point)
        val newEvents = current?.add(point, previous).orEmpty()
        return DriveUpdate(snapshot(), newEvents, finished)
    }

    /** Lets time pass without samples, so a car that went silent still ends its trip. */
    fun onTick(nowMs: Long): DriveUpdate? {
        val events = tripDetector.onTick(nowMs)
        if (events.isEmpty()) return null
        return DriveUpdate(snapshot(), finishedTrip = handle(events, latest))
    }

    fun snapshot(): DriveSnapshot = DriveSnapshot(
        latest = latest,
        vehicleState = tripDetector.state,
        road = road,
        trip = current?.progress(),
    )

    private fun handle(events: List<TripEvent>, point: TelemetryPoint?): Trip? {
        var finished: Trip? = null
        events.forEach { event ->
            when (event) {
                is TripEvent.Started -> {
                    eventDetector.reset()
                    current = TripBuilder(event.startMs, point?.takeIf { it.hasPosition }?.let { placeOf(it.position) })
                }
                is TripEvent.Ended -> {
                    finished = current?.finish(event)
                    current = null
                }
                is TripEvent.Discarded -> current = null
            }
        }
        return finished
    }

    private fun placeOf(at: LatLon): String? =
        map.placeName(at.latitude, at.longitude) ?: map.roadAt(at.latitude, at.longitude)?.name

    private fun TelemetryPoint.toTripSample() = TripSample(timestampMs, speedKmh, ignitionOn, hasPosition)

    private inner class TripBuilder(val startMs: Long, val startPlace: String?) {
        private val events = mutableListOf<DrivingEvent>()
        private val route = mutableListOf<LatLon>()
        private var maxSpeedKmh = 0.0
        private var speedingMs = 0L

        fun add(point: TelemetryPoint, previous: TelemetryPoint?): List<DrivingEvent> {
            if (!point.hasPosition) return emptyList()
            route += point.position
            maxSpeedKmh = max(maxSpeedKmh, point.speedKmh)
            val limit = road?.speedLimitKmh
            if (previous != null && limit != null && point.speedKmh > limit + rules.speedingToleranceKmh) {
                speedingMs += (point.timestampMs - previous.timestampMs).coerceIn(0L, MAX_STEP_MS)
            }
            val found = eventDetector.onPoint(point, road)
            events += found
            return found
        }

        fun progress(): TripProgress {
            val distanceKm = tripDetector.distanceKm
            return TripProgress(
                startMs = startMs,
                durationMs = ((latest?.timestampMs ?: startMs) - startMs).coerceAtLeast(0L),
                distanceKm = distanceKm,
                events = events.toList(),
                route = route.toList(),
                maxSpeedKmh = maxSpeedKmh,
                speedingMs = speedingMs,
                score = Scoring.score(events.sumOf { it.penalty }, distanceKm),
                startPlace = startPlace,
            )
        }

        fun finish(ended: TripEvent.Ended) = Trip(
            startMs = ended.startMs,
            endMs = ended.endMs,
            distanceKm = ended.distanceKm,
            events = events.toList(),
            route = route.toList(),
            maxSpeedKmh = maxSpeedKmh,
            speedingMs = speedingMs,
            score = Scoring.score(events.sumOf { it.penalty }, ended.distanceKm),
            startPlace = startPlace,
            endPlace = route.lastOrNull()?.let { placeOf(it) },
            endedWithoutSignal = ended.endedWithoutSignal,
        )
    }

    private companion object {
        /** Longer gaps between samples are not counted as speeding time. */
        const val MAX_STEP_MS = 10_000L
    }
}
