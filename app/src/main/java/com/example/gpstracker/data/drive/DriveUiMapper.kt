package com.example.gpstracker.data.drive

import com.example.gpstracker.data.estimate.FuelEstimator
import com.example.gpstracker.data.estimate.FuelInput
import com.example.gpstracker.data.estimate.TripRecord
import com.example.gpstracker.data.estimate.VehicleNumbers
import com.example.gpstracker.domain.telemetry.ConnectionState
import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.telemetry.GnssQuality
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.model.trip.VehicleState as DomainVehicleState
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.model.TripDetailUi
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.model.TripsUiState
import com.example.gpstracker.ui.model.VehicleState
import com.example.gpstracker.ui.model.scoreBandOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns domain results (events, trips, connection) into the ready-to-show UI models.
 * All wording and number formatting for driving data lives here.
 */
class DriveUiMapper(private val estimator: FuelEstimator = FuelEstimator()) {

    fun event(e: DrivingEvent): DrivingEventUi = DrivingEventUi(
        id = "${e.type.name}-${e.timestampMs}",
        type = EventType.valueOf(e.type.name),
        title = titleOf(e.type),
        detail = detailOf(e),
        penalty = e.penalty,
        timeLabel = timeLabel(e.timestampMs),
        placeLabel = e.roadName ?: UNKNOWN_ROAD,
        position = point(e.position),
    )

    fun point(p: LatLon) = GeoPointUi(p.latitude, p.longitude)

    /** The trip's row in the history; [nowMs] decides whether it was "Today" or "Yesterday". */
    fun summary(saved: SavedTrip, nowMs: Long): TripSummaryUi = summary(saved.id, saved.trip, dayLabel(saved.savedAtMs, nowMs))

    private fun summary(id: String, trip: Trip, dateLabel: String): TripSummaryUi = TripSummaryUi(
        id = id,
        dateLabel = dateLabel,
        timeRange = "${timeLabel(trip.startMs)} – ${timeLabel(trip.endMs)}",
        fromTo = "${trip.startPlace ?: UNKNOWN_PLACE} → ${trip.endPlace ?: UNKNOWN_PLACE}",
        distanceKm = trip.distanceKm,
        durationLabel = "${max(1, (trip.durationMs / MS_PER_MINUTE).toInt())} min",
        score = trip.score,
        scoreBand = scoreBandOf(trip.score),
        eventCount = trip.events.size,
    )

    /** History stays on the real calendar, so the record is stamped with the time it was saved. */
    fun record(saved: SavedTrip): TripRecord = record(saved.id, saved.trip, saved.savedAtMs)

    private fun record(id: String, trip: Trip, savedAtMs: Long) = TripRecord(
        id = id,
        startMs = savedAtMs,
        distanceKm = trip.distanceKm,
        score = trip.score,
        harshAccelerations = trip.count(DrivingEventType.HarshAcceleration),
        harshBrakings = trip.count(DrivingEventType.HarshBraking),
        speedingEvents = trip.count(DrivingEventType.Speeding),
        sharpCorners = trip.count(DrivingEventType.SharpCorner),
        speedingMinutes = trip.speedingMs / MS_PER_MINUTE.toDouble(),
    )

    /** Detail of a saved trip; the fuel estimate uses the vehicle profile as it is now. */
    fun detail(saved: SavedTrip, nowMs: Long, vehicle: VehicleNumbers): TripDetailUi =
        detail(summary(saved, nowMs), saved.trip, record(saved), vehicle)

    private fun detail(summary: TripSummaryUi, trip: Trip, record: TripRecord, vehicle: VehicleNumbers): TripDetailUi {
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
            avgSpeedKmh = trip.avgSpeedKmh.roundToInt(),
            maxSpeedKmh = trip.maxSpeedKmh.roundToInt(),
            route = trip.route.map(::point),
            events = trip.events.map(::event),
            fuel = estimate.toUi(vehicle.fuelPricePerL),
            tips = tips(trip.events),
        )
    }

    fun tripsState(summaries: List<TripSummaryUi>, tripStartMs: Long?): TripsUiState {
        val recent = summaries.take(WEEK_TRIPS)
        return TripsUiState(
            tripInProgressLabel = tripStartMs?.let { "Driving now, started ${timeLabel(it)}" },
            weekAverageScore = recent.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.roundToInt(),
            weekScores = recent.map { it.score }.reversed(),
            sections = summaries.groupBy { it.dateLabel }.toList(),
        )
    }

    /** Up to three tips, one per kind of mistake, naming the road where it happened. */
    fun tips(events: List<DrivingEvent>): List<String> {
        val first = events.groupBy { it.type }.mapValues { it.value.first() }
        return buildList {
            first[DrivingEventType.Speeding]?.let {
                add(sentence("Keep to the posted limit, especially on ${it.roadName ?: "fast roads"}"))
            }
            first[DrivingEventType.HarshBraking]?.let { add("Brake earlier and more gently before junctions.") }
            first[DrivingEventType.HarshAcceleration]?.let { add("Ease into the throttle when pulling away.") }
            first[DrivingEventType.SharpCorner]?.let { add("Slow down before corners rather than in them.") }
        }.take(MAX_TIPS)
    }

    fun connection(state: ConnectionState, connected: ConnectionStatus): ConnectionStatus = when (state) {
        ConnectionState.Connected -> connected
        ConnectionState.Connecting -> ConnectionStatus.Connecting
        ConnectionState.Disconnected -> ConnectionStatus.Offline
        is ConnectionState.Reconnecting -> ConnectionStatus.Reconnecting
    }

    fun gps(quality: GnssQuality): GpsStatus = when (quality) {
        GnssQuality.Good -> GpsStatus.Good
        GnssQuality.Weak -> GpsStatus.Weak
        GnssQuality.Lost -> GpsStatus.Lost
    }

    fun vehicleState(state: DomainVehicleState): VehicleState = when (state) {
        DomainVehicleState.Driving -> VehicleState.Driving
        DomainVehicleState.Stopped -> VehicleState.Stopped
        DomainVehicleState.Parked -> VehicleState.Parked
    }

    fun timeLabel(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

    /** "Today", "Yesterday" or a short date such as "Mon, 5 Oct", on the phone's calendar. */
    fun dayLabel(ms: Long, nowMs: Long): String = when (dayNumber(nowMs) - dayNumber(ms)) {
        0L -> TODAY
        1L -> YESTERDAY
        else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(ms))
    }

    /** Days since the epoch in the phone's time zone, so a day starts at local midnight. */
    fun dayNumber(ms: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = ms }
        val offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)
        return Math.floorDiv(ms + offset, MS_PER_DAY)
    }

    /** "mm:ss" of a running trip. */
    fun durationLabel(ms: Long): String {
        val seconds = (ms / MS_PER_SECOND).toInt().coerceAtLeast(0)
        return "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }

    private fun titleOf(type: DrivingEventType) = when (type) {
        DrivingEventType.Speeding -> "Speeding"
        DrivingEventType.HarshBraking -> "Harsh braking"
        DrivingEventType.HarshAcceleration -> "Harsh acceleration"
        DrivingEventType.SharpCorner -> "Sharp corner"
        DrivingEventType.CrashSuspected -> "Possible crash"
    }

    private fun detailOf(e: DrivingEvent): String = when (e.type) {
        DrivingEventType.Speeding -> "${e.speedKmh.roundToInt()} in a ${e.speedLimitKmh ?: "?"} zone"
        DrivingEventType.HarshBraking -> "−%.1f m/s²".format(Locale.US, e.peak)
        DrivingEventType.HarshAcceleration -> "+%.1f m/s²".format(Locale.US, e.peak)
        DrivingEventType.SharpCorner -> "%.1f g lateral".format(Locale.US, e.peak / G_MPS2)
        DrivingEventType.CrashSuspected -> "%.1f g impact".format(Locale.US, e.peak / G_MPS2)
    }

    private fun Trip.count(type: DrivingEventType) = events.count { it.type == type }

    /** Ends [text] with one full stop, also when it ends with an abbreviation such as "g.". */
    private fun sentence(text: String) = if (text.endsWith(".")) text else "$text."

    private companion object {
        const val TODAY = "Today"
        const val YESTERDAY = "Yesterday"
        const val MS_PER_DAY = 86_400_000L
        const val UNKNOWN_ROAD = "Unknown road"
        const val UNKNOWN_PLACE = "Unknown place"
        const val WEEK_TRIPS = 7
        const val MAX_TIPS = 3
        const val G_MPS2 = 9.81
        const val MS_PER_SECOND = 1_000
        const val MS_PER_MINUTE = 60_000L
        const val SECONDS_PER_MINUTE = 60
    }
}
