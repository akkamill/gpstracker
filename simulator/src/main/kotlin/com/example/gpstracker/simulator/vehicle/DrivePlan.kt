package com.example.gpstracker.simulator.vehicle

import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.road.Route

/**
 * Something that happens at a place on the route. Positions are metres from the
 * start of the route, so the same plan always produces the same drive.
 */
sealed interface Incident {
    /** The driver stops and waits; with [aggressiveLaunch] pulls away hard (harsh acceleration). */
    data class TrafficLight(val atM: Double, val waitS: Int, val aggressiveLaunch: Boolean = false) : Incident

    /** Someone pulls out: the driver brakes hard down to [toKmh] (harsh braking). */
    data class SuddenBrake(val atM: Double, val toKmh: Double, val decelMps2: Double = 4.6) : Incident

    /** The driver ignores the limit and drives [targetKmh] (speeding). */
    data class SpeedingStretch(val fromM: Double, val toM: Double, val targetKmh: Double) : Incident

    /** The driver takes the corner nearest to [atM] at [speedKmh] instead of slowing down (sharp corner). */
    data class CarelessCorner(val atM: Double, val speedKmh: Double) : Incident

    /** The car hits something at [atM] and stops at once (possible crash). */
    data class Crash(val atM: Double) : Incident

    /** No satellite fix between the two points, as under a bridge. */
    data class GnssOutage(val fromM: Double, val toM: Double) : Incident

    /** No mobile coverage: the tracker keeps recording and sends everything later. */
    data class CoverageGap(val fromM: Double, val toM: Double) : Incident
}

/** One drive: a route plus the incidents along it. */
data class DrivePlan(val route: Route, val incidents: List<Incident> = emptyList()) {
    inline fun <reified T : Incident> all(): List<T> = incidents.filterIsInstance<T>()

    fun gnssLostAt(distanceM: Double) = all<Incident.GnssOutage>().any { distanceM in it.fromM..it.toM }

    fun coverageLostAt(distanceM: Double) = all<Incident.CoverageGap>().any { distanceM in it.fromM..it.toM }
}

/** How the driver behaves when nothing special happens. */
data class DriverStyle(
    val accelMps2: Double = 1.6,
    val decelMps2: Double = 1.8,
    val launchAccelMps2: Double = 3.8,
    /** Aggressive launches stop being aggressive at this speed. */
    val launchUntilKmh: Double = 50.0,
    /** Sideways acceleration the driver accepts in corners. */
    val cornerLateralMps2: Double = 2.4,
    /** Cruises this far under the limit. */
    val belowLimitKmh: Double = 2.0,
)

/** The two drives of the demo loop. */
object DemoPlans {
    private const val KM = 1_000.0

    /**
     * There: one of each event, plus a GPS outage and a coverage gap.
     * Expected events: harsh braking, harsh acceleration, speeding, sharp corner.
     */
    val toAkropolis: DrivePlan = Kaunas.toAkropolis.let { r ->
        val savanoriuCity = r.segmentStartM(0)
        val savanoriuFast = r.segmentStartM(1)
        val jonavos = r.segmentStartM(2)
        val mindaugo = r.segmentStartM(3)
        DrivePlan(
            route = r,
            incidents = listOf(
                Incident.SuddenBrake(atM = savanoriuCity + 0.9 * KM, toKmh = 15.0),
                Incident.CoverageGap(fromM = savanoriuCity + 1.0 * KM, toM = savanoriuCity + 1.45 * KM),
                Incident.TrafficLight(atM = savanoriuFast + 0.6 * KM, waitS = 25, aggressiveLaunch = true),
                Incident.GnssOutage(fromM = savanoriuFast + 0.9 * KM, toM = savanoriuFast + 1.4 * KM),
                Incident.SpeedingStretch(fromM = jonavos + 0.08 * KM, toM = jonavos + 0.65 * KM, targetKmh = 68.0),
                Incident.CarelessCorner(atM = mindaugo, speedKmh = 38.0),
            ),
        )
    }

    /** Back: a calm drive with one traffic light and no events. */
    val toZaliakalnis: DrivePlan = Kaunas.toZaliakalnis.let { r ->
        DrivePlan(route = r, incidents = listOf(Incident.TrafficLight(atM = r.segmentStartM(3) + 1.1 * KM, waitS = 20)))
    }

    val day: List<DrivePlan> = listOf(toAkropolis, toZaliakalnis)
}
