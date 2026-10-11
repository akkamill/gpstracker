package com.example.gpstracker.domain.driving

import com.example.gpstracker.domain.telemetry.TelemetrySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Use case: turns the raw telemetry of a [TelemetrySource] into a live [DrivingSession]. */
class ObserveDrivingSession(private val source: TelemetrySource) {
    operator fun invoke(): Flow<DrivingSession> = flow {
        val tracker = DrivingSessionTracker()
        source.points.collect { emit(tracker.onPoint(it)) }
    }
}
