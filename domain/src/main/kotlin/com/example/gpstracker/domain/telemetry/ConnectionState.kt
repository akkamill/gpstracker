package com.example.gpstracker.domain.telemetry

/** State of the link to a [TelemetrySource]. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState

    /** Link dropped; [attempt] is the 1-based reconnect attempt in progress. */
    data class Reconnecting(val attempt: Int) : ConnectionState
}
