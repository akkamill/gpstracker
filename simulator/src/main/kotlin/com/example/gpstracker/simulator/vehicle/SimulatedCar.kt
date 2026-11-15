package com.example.gpstracker.simulator.vehicle

import com.example.gpstracker.simulator.road.GeoPoint
import com.example.gpstracker.telematics.AvlRecord
import com.example.gpstracker.tracing.Tracing
import kotlin.random.Random

/** What the car is doing in its daily loop. */
enum class CarPhase { Parked, EngineIdle, Driving, Arrived }

/** Timing of the daily loop, in seconds of car time. */
data class LoopTiming(
    val parkedS: Int = 300,
    val idleBeforeDriveS: Int = 20,
    val idleAfterDriveS: Int = 10,
)

/**
 * A car with a tracker in it, living through a repeating "day": parked, engine on,
 * drive one plan, arrive, engine off, parked, then drive the next plan.
 *
 * Time only moves when [step] is called (one second per call), so tests run instantly
 * and the same seed always gives the same records. Nothing here knows about trips,
 * scores or events; finding those in the data is the app's job.
 */
class SimulatedCar(
    startMs: Long,
    private val plans: List<DrivePlan> = DemoPlans.day,
    private val timing: LoopTiming = LoopTiming(),
    noise: Noise = Noise(),
    seed: Long = DEFAULT_SEED,
) {
    init {
        require(plans.isNotEmpty()) { "the car needs at least one drive plan" }
    }

    private val lock = Any()
    private val tracer = Tracing.tracer("simulator")
    private val engine = Engine()
    private val tracker = Tracker(Random(seed), noise)

    var timeMs: Long = startMs
        private set

    var phase: CarPhase = CarPhase.Parked
        private set

    /** Index into [plans] of the current or next drive. */
    var planIndex: Int = 0
        private set

    private var phaseLeftS = timing.parkedS
    private var driver: Driver? = null

    /** Where the car stands when it is not driving, and which way it faces. */
    private var restAt: GeoPoint = plans.first().route.positionAt(0.0)
    private var restHeading: Double = plans.first().route.headingAt(0.0)
    private var lastMotion = restingMotion()

    /** False while the car is in a coverage gap; records are then held in the tracker. */
    val online: Boolean get() = synchronized(lock) { isOnline() }

    /** The plan being driven, or the next one while parked. */
    val currentPlan: DrivePlan get() = plans[planIndex]

    /** Advances one second and returns the records the tracker sends in that second. */
    fun step(): List<AvlRecord> = synchronized(lock) {
        timeMs += STEP_MS
        val before = lastMotion.distanceM
        lastMotion = when (phase) {
            CarPhase.Driving -> driver!!.step(STEP_S)
            else -> restingMotion()
        }
        val travelled = if (phase == CarPhase.Driving) (lastMotion.distanceM - before).coerceAtLeast(0.0) else 0.0
        engine.running = phase != CarPhase.Parked
        engine.step(lastMotion.speedMps, lastMotion.accelMps2, travelled, STEP_S)

        val records = tracker.sample(
            CarState(
                timestampMs = timeMs,
                ignitionOn = engine.running,
                motion = lastMotion,
                engine = engine,
                gnssAvailable = !(phase == CarPhase.Driving && currentPlan.gnssLostAt(lastMotion.distanceM)),
                online = isOnline(),
            ),
            travelled,
        )
        advancePhase()
        records
    }

    /**
     * Presentation shortcut: ends the parked wait now, so the engine starts on the
     * next step. It controls the car, not the trip.
     */
    fun skipToNextDrive() = synchronized(lock) {
        if (phase == CarPhase.Parked) phaseLeftS = 0
    }

    private fun advancePhase() {
        when (phase) {
            CarPhase.Parked -> if (--phaseLeftS <= 0) enter(CarPhase.EngineIdle, timing.idleBeforeDriveS)

            CarPhase.EngineIdle -> if (--phaseLeftS <= 0) {
                driver = Driver(currentPlan)
                enter(CarPhase.Driving, 0)
            }

            CarPhase.Driving -> if (driver!!.finished) {
                restAt = lastMotion.position
                restHeading = lastMotion.headingDeg
                enter(CarPhase.Arrived, timing.idleAfterDriveS)
            }

            CarPhase.Arrived -> if (--phaseLeftS <= 0) {
                driver = null
                planIndex = (planIndex + 1) % plans.size
                enter(CarPhase.Parked, timing.parkedS)
            }
        }
    }

    private fun enter(next: CarPhase, seconds: Int) {
        phase = next
        phaseLeftS = seconds
        when (next) {
            CarPhase.EngineIdle -> tracer.info("engine started", "at" to currentPlan.route.from.name)

            CarPhase.Driving -> tracer.info("drive started", "route" to currentPlan.route.name)

            CarPhase.Arrived -> if (driver?.crashed == true) {
                tracer.warn("car stopped after a collision", "atM" to lastMotion.distanceM.toInt())
            } else {
                tracer.info("arrived", "at" to currentPlan.route.to.name)
            }

            CarPhase.Parked -> tracer.info("engine stopped")
        }
    }

    private fun isOnline() = !(phase == CarPhase.Driving && currentPlan.coverageLostAt(lastMotion.distanceM))

    private fun restingMotion() = Motion(0.0, 0.0, 0.0, 0.0, restHeading, restAt)

    private companion object {
        const val STEP_MS = 1_000L
        const val STEP_S = 1.0
        const val DEFAULT_SEED = 7L
    }
}
