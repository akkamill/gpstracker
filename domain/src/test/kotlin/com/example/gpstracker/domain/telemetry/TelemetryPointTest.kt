package com.example.gpstracker.domain.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryPointTest {
    @Test
    fun `converts metres per second to km per h`() {
        val point = TelemetryPoint(0L, 40.4, 49.8, speedMps = 10.0)
        assertEquals(36.0, point.speedKmh, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects negative speed`() {
        TelemetryPoint(0L, 40.4, 49.8, speedMps = -1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects latitude out of range`() {
        TelemetryPoint(0L, 91.0, 49.8, speedMps = 0.0)
    }
}
