package com.example.gpstracker.data.estimate

import com.example.gpstracker.ui.model.VehicleState

/**
 * One reading from the car.
 *
 * @property ignitionOn null when the device does not report the ignition
 */
data class TripSample(
    val timestampMs: Long,
    val speedKmh: Double,
    val ignitionOn: Boolean? = null,
    val gpsValid: Boolean = true,
)

/** Thresholds of the trip state machine. Times are in milliseconds of sample time. */
data class TripDetectorConfig(
    val movingKmh: Double = 5.0,
    val stoppedKmh: Double = 3.0,
    val stopConfirmMs: Long = 5_000,
    /** Without an ignition signal, speed above [movingKmh] must last this long to start a trip. */
    val startWithoutIgnitionMs: Long = 10_000,
    /** Ignition off for less than this is a quick restart and keeps the same trip. */
    val ignitionGraceMs: Long = 30_000,
    /** No data for this long ends the trip at the last sample. */
    val noDataMs: Long = 10 * 60_000,
    /** Standing still this long ends the trip; changeable in Settings (2, 5 or 10 minutes). */
    val tripEndAfterMs: Long = 5 * 60_000,
    val minDistanceKm: Double = 0.3,
    val minDurationMs: Long = 60_000,
)

sealed interface TripEvent {
    data class Started(val startMs: Long) : TripEvent

    /** @property endedWithoutSignal true when the trip ended because data stopped arriving */
    data class Ended(
        val startMs: Long,
        val endMs: Long,
        val distanceKm: Double,
        val endedWithoutSignal: Boolean,
    ) : TripEvent

    /** The car only moved a little (a parking lot); nothing is saved. */
    data class Discarded(val startMs: Long, val endMs: Long) : TripEvent
}

/**
 * Turns the stream of car samples into trips. There is no manual start or stop:
 *
 * ```
 * Parked -> Driving <-> Stopped -> Parked
 * ```
 *
 * Pure Kotlin with no clock of its own: time comes from the samples (and from
 * [onTick] for "no data" detection), so tests run instantly. It moves to `:domain` later.
 */
class TripDetector(private var config: TripDetectorConfig = TripDetectorConfig()) {

    var state: VehicleState = VehicleState.Parked
        private set

    /** Start of the trip in progress, or null when parked. */
    var tripStartMs: Long? = null
        private set

    /** Distance of the trip in progress. */
    var distanceKm: Double = 0.0
        private set

    private var lastSampleMs: Long? = null
    private var movingSinceMs: Long? = null
    private var belowStopSinceMs: Long? = null
    private var stoppedSinceMs: Long? = null
    private var ignitionOffSinceMs: Long? = null

    /** Changes how long the car must stand still before the trip ends. */
    fun setTripEndAfterMinutes(minutes: Int) {
        config = config.copy(tripEndAfterMs = minutes * 60_000L)
    }

    /** Feeds one sample and returns the trip events it caused, in order. */
    fun onSample(sample: TripSample): List<TripEvent> {
        val events = mutableListOf<TripEvent>()
        val previous = lastSampleMs

        // A long silence ends the trip at the last sample, before the new one is handled.
        if (previous != null && state != VehicleState.Parked && sample.timestampMs - previous >= config.noDataMs) {
            endTrip(previous, endedWithoutSignal = true, events)
        }

        if (previous != null && state != VehicleState.Parked && sample.timestampMs > previous) {
            val hours = (sample.timestampMs - previous) / MS_PER_HOUR
            distanceKm += sample.speedKmh * hours
        }
        lastSampleMs = sample.timestampMs

        when (state) {
            VehicleState.Parked -> handleParked(sample, events)
            VehicleState.Driving -> handleDriving(sample, events)
            VehicleState.Stopped -> handleStopped(sample, events)
        }
        return events
    }

    /** Call when time passes without samples, so a silent car still ends its trip. */
    fun onTick(nowMs: Long): List<TripEvent> {
        val last = lastSampleMs ?: return emptyList()
        if (state == VehicleState.Parked || nowMs - last < config.noDataMs) return emptyList()
        return mutableListOf<TripEvent>().also { endTrip(last, endedWithoutSignal = true, it) }
    }

    private fun handleParked(sample: TripSample, events: MutableList<TripEvent>) {
        val moving = sample.speedKmh > config.movingKmh
        when (sample.ignitionOn) {
            true -> if (moving) start(sample.timestampMs, events)
            false -> movingSinceMs = null
            null -> {
                if (!moving) {
                    movingSinceMs = null
                } else {
                    val since = movingSinceMs ?: sample.timestampMs.also { movingSinceMs = it }
                    if (sample.timestampMs - since >= config.startWithoutIgnitionMs) start(since, events)
                }
            }
        }
    }

    private fun handleDriving(sample: TripSample, events: MutableList<TripEvent>) {
        if (checkIgnitionOff(sample, events)) return
        if (sample.speedKmh < config.stoppedKmh) {
            val since = belowStopSinceMs ?: sample.timestampMs.also { belowStopSinceMs = it }
            if (sample.timestampMs - since >= config.stopConfirmMs) {
                state = VehicleState.Stopped
                stoppedSinceMs = since
            }
        } else {
            belowStopSinceMs = null
        }
    }

    private fun handleStopped(sample: TripSample, events: MutableList<TripEvent>) {
        if (checkIgnitionOff(sample, events)) return
        if (sample.speedKmh > config.movingKmh) {
            state = VehicleState.Driving
            belowStopSinceMs = null
            stoppedSinceMs = null
            return
        }
        val since = stoppedSinceMs ?: return
        if (sample.timestampMs - since >= config.tripEndAfterMs) {
            // The trip ends at the moment the car stopped, not when the wait ran out.
            endTrip(since, endedWithoutSignal = false, events)
        }
    }

    /** Returns true when the trip was ended because the ignition stayed off long enough. */
    private fun checkIgnitionOff(sample: TripSample, events: MutableList<TripEvent>): Boolean {
        if (sample.ignitionOn != false) {
            ignitionOffSinceMs = null
            return false
        }
        val since = ignitionOffSinceMs ?: sample.timestampMs.also { ignitionOffSinceMs = it }
        if (sample.timestampMs - since >= config.ignitionGraceMs) {
            endTrip(since, endedWithoutSignal = false, events)
            return true
        }
        return false
    }

    private fun start(startMs: Long, events: MutableList<TripEvent>) {
        state = VehicleState.Driving
        tripStartMs = startMs
        distanceKm = 0.0
        movingSinceMs = null
        belowStopSinceMs = null
        stoppedSinceMs = null
        ignitionOffSinceMs = null
        events += TripEvent.Started(startMs)
    }

    private fun endTrip(endMs: Long, endedWithoutSignal: Boolean, events: MutableList<TripEvent>) {
        val start = tripStartMs
        if (start != null) {
            val tooShort = distanceKm < config.minDistanceKm || endMs - start < config.minDurationMs
            events += if (tooShort) {
                TripEvent.Discarded(start, endMs)
            } else {
                TripEvent.Ended(start, endMs, distanceKm, endedWithoutSignal)
            }
        }
        state = VehicleState.Parked
        tripStartMs = null
        distanceKm = 0.0
        movingSinceMs = null
        belowStopSinceMs = null
        stoppedSinceMs = null
        ignitionOffSinceMs = null
    }

    private companion object {
        const val MS_PER_HOUR = 3_600_000.0
    }
}
