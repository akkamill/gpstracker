package com.example.gpstracker.simulator.vehicle

import com.example.gpstracker.telematics.AvlIo
import com.example.gpstracker.telematics.AvlPriority
import com.example.gpstracker.telematics.AvlRecord
import com.example.gpstracker.telematics.GpsElement
import com.example.gpstracker.simulator.road.Geo
import com.example.gpstracker.simulator.road.GeoPoint
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/** Everything the tracker can sense at one moment. */
data class CarState(
    val timestampMs: Long,
    val ignitionOn: Boolean,
    val motion: Motion,
    val engine: Engine,
    val gnssAvailable: Boolean,
    val online: Boolean,
)

/** Sensor noise. Zero noise makes records exact, which some tests prefer. */
data class Noise(
    val positionM: Double = 2.5,
    val speedKmh: Double = 1.0,
    val accelMilliG: Double = 15.0,
) {
    companion object {
        val NONE = Noise(0.0, 0.0, 0.0)
    }
}

/**
 * The simulated tracker box. It turns the car's state into [AvlRecord]s the way a
 * typical vehicle tracker does:
 *
 * - a record every second while the ignition is on, and every [parkedPeriodS] while parked;
 * - an extra high-priority record when the ignition changes;
 * - without a satellite fix it keeps the last coordinates, with 0 satellites and speed 0;
 * - "static navigation": while the car stands still it repeats the last position with
 *   speed 0, so GPS noise does not make a parked car look as if it moves;
 * - without mobile coverage it keeps records in memory and sends them all when coverage returns.
 */
class Tracker(
    private val random: Random,
    private val noise: Noise = Noise(),
    private val parkedPeriodS: Int = 10,
    startOdometerM: Long = 84_250_000,
) {
    private val buffer = ArrayDeque<AvlRecord>()
    private var lastFix: GeoPoint? = null
    private var lastIgnition: Boolean? = null
    private var secondsSinceRecord = Int.MAX_VALUE / 2
    private var odometerM = startOdometerM.toDouble()

    /** Records waiting to be sent. */
    val buffered: Int get() = buffer.size

    /**
     * Takes one sample of [state] (called once per second) and returns the records
     * that leave the tracker now: none while offline, all buffered ones when back online.
     */
    fun sample(state: CarState, travelledM: Double): List<AvlRecord> {
        odometerM += travelledM
        val ignitionChanged = lastIgnition != null && lastIgnition != state.ignitionOn
        lastIgnition = state.ignitionOn
        secondsSinceRecord++

        val due = state.ignitionOn || secondsSinceRecord >= parkedPeriodS
        if (due || ignitionChanged) {
            buffer += record(state, eventIo = if (ignitionChanged) AvlIo.IGNITION else 0)
            secondsSinceRecord = 0
        }
        if (!state.online) return emptyList()
        val out = buffer.toList()
        buffer.clear()
        return out
    }

    private fun record(state: CarState, eventIo: Int): AvlRecord {
        val m = state.motion
        val standing = m.speedMps <= MOVING_MPS
        val gps = if (state.gnssAvailable) {
            val held = lastFix.takeIf { standing }
            val p = held ?: Geo.offset(m.position, jitter(noise.positionM), jitter(noise.positionM))
            lastFix = p
            GpsElement(
                latitude = p.lat,
                longitude = p.lon,
                altitudeM = ALTITUDE_M + random.nextInt(-2, 3),
                angleDeg = m.headingDeg.roundToInt() % FULL_TURN,
                satellites = random.nextInt(MIN_SATELLITES, MAX_SATELLITES + 1),
                speedKmh = if (standing) 0 else max(0, (m.speedMps * KMH_PER_MPS + jitter(noise.speedKmh)).roundToInt()),
            )
        } else {
            val p = lastFix ?: m.position
            GpsElement(p.lat, p.lon, altitudeM = 0, angleDeg = 0, satellites = 0, speedKmh = 0)
        }
        val io = buildMap<Int, Long> {
            put(AvlIo.IGNITION, if (state.ignitionOn) 1 else 0)
            put(AvlIo.MOVEMENT, if (m.speedMps > MOVING_MPS) 1 else 0)
            put(AvlIo.GSM_SIGNAL, if (state.online) GOOD_GSM else 0)
            put(AvlIo.GNSS_STATUS, if (state.gnssAvailable) AvlIo.GNSS_FIX else AvlIo.GNSS_NO_FIX)
            if (state.gnssAvailable) put(AvlIo.GNSS_HDOP, random.nextLong(MIN_HDOP_X10, MAX_HDOP_X10 + 1))
            put(AvlIo.EXTERNAL_VOLTAGE, state.engine.batteryMillivolts.toLong())
            put(AvlIo.TOTAL_ODOMETER, odometerM.roundToLong())
            put(AvlIo.AXIS_X, milliG(m.accelMps2))
            put(AvlIo.AXIS_Y, milliG(m.lateralMps2))
            put(AvlIo.AXIS_Z, milliG(G_MPS2))
            if (state.ignitionOn) {
                put(AvlIo.OBD_VEHICLE_SPEED, (m.speedMps * KMH_PER_MPS).roundToLong())
                put(AvlIo.OBD_ENGINE_RPM, state.engine.rpm.toLong())
                put(AvlIo.OBD_COOLANT_TEMPERATURE, state.engine.coolantTemperatureC.toLong())
                put(AvlIo.OBD_FUEL_LEVEL, state.engine.fuelLevelPercent.toLong())
            }
        }
        return AvlRecord(
            timestampMs = state.timestampMs,
            priority = when {
                eventIo != 0 -> AvlPriority.High
                else -> AvlPriority.Low
            },
            gps = gps,
            eventIoId = eventIo,
            io = io,
        )
    }

    private fun milliG(mps2: Double): Long = (mps2 / G_MPS2 * MILLI + jitter(noise.accelMilliG)).roundToLong()

    private fun jitter(amplitude: Double): Double =
        if (amplitude == 0.0) 0.0 else (random.nextDouble() * 2 - 1) * amplitude

    private companion object {
        const val G_MPS2 = 9.81
        const val MILLI = 1_000.0
        const val KMH_PER_MPS = 3.6
        const val FULL_TURN = 360
        const val ALTITUDE_M = 72
        const val MIN_SATELLITES = 8
        const val MAX_SATELLITES = 12
        const val MIN_HDOP_X10 = 7L
        const val MAX_HDOP_X10 = 14L
        const val GOOD_GSM = 4L
        const val MOVING_MPS = 0.5
    }
}
