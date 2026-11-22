package com.example.gpstracker.simulator.vehicle

import com.example.gpstracker.simulator.road.Corner
import com.example.gpstracker.simulator.road.GeoPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

/** The true motion of the car after one step, before any sensor noise. */
data class Motion(
    val distanceM: Double,
    val speedMps: Double,
    /** Forward acceleration; negative while braking. */
    val accelMps2: Double,
    /** Sideways acceleration; positive to the left (a left turn). */
    val lateralMps2: Double,
    val headingDeg: Double,
    val position: GeoPoint,
)

/**
 * Drives one [DrivePlan] with simple, explainable physics, one step at a time.
 *
 * Each step the driver picks a target speed: the speed limit (or a speeding target),
 * lowered so that the car can still stop at the next red light, slow down for the
 * next corner or lower limit using normal braking. Incidents then break these
 * habits on purpose, which is what produces driving events.
 */
class Driver(private val plan: DrivePlan, private val style: DriverStyle = DriverStyle()) {
    private val route = plan.route
    private val lights = plan.all<Incident.TrafficLight>().sortedBy { it.atM }
    private val brakes = plan.all<Incident.SuddenBrake>().sortedBy { it.atM }
    private val crash = plan.all<Incident.Crash>().minByOrNull { it.atM }
    private val carelessCorners: Map<Corner, Double> = plan.all<Incident.CarelessCorner>().associate { incident ->
        route.corners.minBy { abs(it.atM - incident.atM) } to incident.speedKmh / KMH_PER_MPS
    }

    private var distanceM = 0.0
    private var speedMps = 0.0
    private var nextLight = 0
    private var nextBrake = 0
    private var waitLeftS = 0
    private var launching = false
    private var braking: Incident.SuddenBrake? = null

    var finished = false
        private set

    var crashed = false
        private set

    val distance: Double get() = distanceM

    /** Advances the car by [dtS] seconds. */
    fun step(dtS: Double = 1.0): Motion {
        val before = speedMps
        when {
            finished -> speedMps = 0.0
            waitLeftS > 0 -> waitAtLight()
            else -> drive(dtS)
        }
        val accel = when {
            crashed && before > 0.0 -> CRASH_DECEL_MPS2
            else -> (speedMps - before) / dtS
        }
        return Motion(
            distanceM = distanceM,
            speedMps = speedMps,
            accelMps2 = accel,
            lateralMps2 = lateral(),
            headingDeg = route.headingAt(distanceM),
            position = route.positionAt(distanceM),
        )
    }

    private fun waitAtLight() {
        speedMps = 0.0
        waitLeftS--
        if (waitLeftS == 0) launching = lights[nextLight - 1].aggressiveLaunch
    }

    private fun drive(dtS: Double) {
        if (hitCrash()) return
        val v0 = speedMps
        var v1 = max(0.0, v0 + acceleration(v0, dtS) * dtS)
        var next = distanceM + (v0 + v1) / 2 * dtS
        startSuddenBrakeIfReached(next)

        val stopAt = lights.getOrNull(nextLight)?.atM ?: route.lengthM
        val arrivingSlowly = next >= stopAt - ARRIVE_M && v0 < ARRIVE_SPEED_MPS
        if (arrivingSlowly || next >= stopAt) {
            next = stopAt
            v1 = 0.0
            arriveAtStop()
        }
        distanceM = next
        speedMps = v1
    }

    /** Stops the car for good when it reaches a planned crash. */
    private fun hitCrash(): Boolean {
        val at = crash?.atM ?: return false
        if (distanceM < at) return false
        speedMps = 0.0
        crashed = true
        finished = true
        return true
    }

    /** Hard braking while a sudden brake is going on, otherwise the driver's normal choice. */
    private fun acceleration(v0: Double, dtS: Double): Double {
        val brake = braking ?: return chosenAcceleration(v0, dtS)
        if (v0 <= brake.toKmh / KMH_PER_MPS) {
            braking = null
            return 0.0
        }
        return -brake.decelMps2
    }

    private fun chosenAcceleration(v0: Double, dtS: Double): Double {
        val target = targetSpeed()
        if (launching && (v0 >= style.launchUntilKmh / KMH_PER_MPS || v0 >= target)) launching = false
        val maxAccel = if (launching) style.launchAccelMps2 else style.accelMps2
        return ((target - v0) / dtS).coerceIn(-style.decelMps2, maxAccel)
    }

    private fun startSuddenBrakeIfReached(distance: Double) {
        val next = brakes.getOrNull(nextBrake) ?: return
        if (distance >= next.atM) {
            braking = next
            nextBrake++
        }
    }

    /** At a red light the car waits; at the end of the route the drive is over. */
    private fun arriveAtStop() {
        if (nextLight < lights.size) {
            waitLeftS = lights[nextLight].waitS
            nextLight++
        } else {
            finished = true
        }
    }

    /**
     * The highest speed the driver is willing to drive right now. Distances to the
     * next stop, corner or lower limit are measured from where the car will be after
     * this step, so it never arrives a little too fast.
     */
    private fun targetSpeed(): Double {
        val s = distanceM + speedMps * LOOKAHEAD_S
        val plan = planDecel()
        val speeding = this.plan.all<Incident.SpeedingStretch>().firstOrNull { s in it.fromM..it.toM }
        var target = speeding?.targetKmh?.div(KMH_PER_MPS) ?: cruiseSpeed(route.segmentAt(distanceM).speedLimitKmh)

        // Stop at the next red light or at the end of the route.
        val stopAt = lights.getOrNull(nextLight)?.atM ?: route.lengthM
        target = min(target, sqrt(2 * plan * max(0.0, stopAt - s)) + CREEP_MPS)

        // Slow down for corners ahead (or the one we are in); wide bends allow full speed anyway.
        route.corners.filter { it.endM > distanceM }.forEach { corner ->
            val vc = carelessCorners[corner] ?: sqrt(style.cornerLateralMps2 * corner.radiusM)
            target = min(target, sqrt(vc * vc + 2 * plan * max(0.0, corner.startM - s)))
        }

        // Respect a lower limit before reaching it (unless speeding there on purpose).
        route.segments.indices.forEach { i ->
            val start = route.segmentStartM(i)
            if (start > distanceM) {
                val vl = cruiseSpeed(route.segments[i].speedLimitKmh)
                target = min(target, sqrt(vl * vl + 2 * plan * max(0.0, start - s)))
            }
        }
        return target
    }

    private fun cruiseSpeed(limitKmh: Int) = (limitKmh - style.belowLimitKmh) / KMH_PER_MPS

    /** Plans with a little less braking than the driver can do, so normal stops stay smooth. */
    private fun planDecel() = style.decelMps2 * PLANNING_MARGIN

    /** v² / r inside a corner arc, signed: left turns are positive. */
    private fun lateral(): Double {
        val corner = route.cornerAt(distanceM) ?: return 0.0
        return -sign(corner.turnDeg) * speedMps * speedMps / corner.radiusM
    }

    private companion object {
        const val KMH_PER_MPS = 3.6
        const val PLANNING_MARGIN = 0.9
        const val ARRIVE_M = 3.0
        const val ARRIVE_SPEED_MPS = 2.0
        const val CREEP_MPS = 0.5
        const val LOOKAHEAD_S = 1.0

        /** About 3.5 g: what an accelerometer sees in a minor collision. */
        const val CRASH_DECEL_MPS2 = -34.0
    }
}
