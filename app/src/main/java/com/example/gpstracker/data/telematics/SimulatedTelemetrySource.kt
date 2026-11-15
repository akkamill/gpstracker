package com.example.gpstracker.data.telematics

import com.example.gpstracker.domain.telemetry.ConnectionState
import com.example.gpstracker.domain.telemetry.TelemetrySource
import com.example.gpstracker.model.telemetry.TelemetryPoint
import com.example.gpstracker.simulator.DevicePlayback
import com.example.gpstracker.simulator.DeviceUpdate
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.telematics.AvlTelemetryMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/** Presentation helpers that act on the simulated car, never on trips. */
interface DemoControls {
    /** Ends the parked wait so the car starts its next drive now. */
    fun skipToNextDrive()
}

/**
 * Demo data source: a simulated car with a vehicle tracker, played in real
 * time. When the tracker loses mobile coverage the connection shows as reconnecting,
 * and the buffered records arrive together when it is back.
 *
 * Later a WebSocket source from the server implements the same [TelemetrySource].
 */
class SimulatedTelemetrySource(
    private val car: SimulatedCar,
    private val playback: DevicePlayback = DevicePlayback(car),
    private val connectDelayMs: Long = CONNECT_DELAY_MS,
) : TelemetrySource, DemoControls {

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    /** Cold: collect it once, it drives the car. */
    override val points: Flow<TelemetryPoint> = flow {
        _connection.value = ConnectionState.Connecting
        delay(connectDelayMs)
        playback.updates().collect { update ->
            when (update) {
                is DeviceUpdate.Link -> _connection.value =
                    if (update.online) ConnectionState.Connected else ConnectionState.Reconnecting(attempt = 1)
                is DeviceUpdate.Records -> update.records.forEach { emit(AvlTelemetryMapper.toTelemetry(it)) }
            }
        }
    }

    override fun skipToNextDrive() = car.skipToNextDrive()

    private companion object {
        const val CONNECT_DELAY_MS = 1_500L
    }
}
