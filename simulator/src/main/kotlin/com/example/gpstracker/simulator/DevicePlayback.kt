package com.example.gpstracker.simulator

import com.example.gpstracker.simulator.avl.AvlRecord
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** What a receiver gets from the simulated tracker. */
sealed interface DeviceUpdate {
    /** The tracker's mobile link went down or came back. */
    data class Link(val online: Boolean) : DeviceUpdate

    /** Records that arrived together, oldest first. After a coverage gap this is a large batch. */
    data class Records(val records: List<AvlRecord>) : DeviceUpdate
}

/**
 * Plays a [SimulatedCar] in real time, [speedup] times faster than the car's own clock.
 * Every [tickMs] it advances the car by `speedup × tickMs` of car time and emits what
 * the tracker sent in that time.
 *
 * With the defaults (×10, every 500 ms) a 5-minute park takes 30 seconds, so a whole
 * demo loop fits in about three minutes while trip rules (30 s, 5 min) stay realistic.
 */
class DevicePlayback(
    private val car: SimulatedCar,
    private val speedup: Int = DEFAULT_SPEEDUP,
    private val tickMs: Long = DEFAULT_TICK_MS,
) {
    init {
        require(speedup >= 1) { "speedup must be at least 1" }
        require(speedup * tickMs % MS_PER_STEP == 0L) { "speedup × tickMs must be whole seconds of car time" }
    }

    private val stepsPerTick = (speedup * tickMs / MS_PER_STEP).toInt()

    /** Cold flow: each collector drives the same car, so collect it once. */
    fun updates(): Flow<DeviceUpdate> = flow {
        var online = car.online
        emit(DeviceUpdate.Link(online))
        while (true) {
            val batch = buildList { repeat(stepsPerTick) { addAll(car.step()) } }
            if (car.online != online) {
                online = car.online
                emit(DeviceUpdate.Link(online))
            }
            if (batch.isNotEmpty()) emit(DeviceUpdate.Records(batch))
            delay(tickMs)
        }
    }

    companion object {
        const val DEFAULT_SPEEDUP = 10
        const val DEFAULT_TICK_MS = 500L
        private const val MS_PER_STEP = 1_000L
    }
}
