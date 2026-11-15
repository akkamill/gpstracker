package com.example.gpstracker.data.telematics

import com.example.gpstracker.domain.telemetry.GnssQuality
import com.example.gpstracker.simulator.avl.AvlIo
import com.example.gpstracker.simulator.avl.AvlPriority
import com.example.gpstracker.simulator.avl.AvlRecord
import com.example.gpstracker.simulator.avl.GpsElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvlTelemetryMapperTest {
    private val fix = GpsElement(54.9, 23.9, altitudeM = 70, angleDeg = 245, satellites = 10, speedKmh = 52)

    private fun record(gps: GpsElement = fix, io: Map<Int, Long>) = AvlRecord(1_000L, AvlPriority.Low, gps, io = io)

    @Test
    fun `prefers the car's OBD speed and converts units`() {
        val point = AvlTelemetryMapper.toTelemetry(
            record(io = mapOf(AvlIo.OBD_VEHICLE_SPEED to 50L, AvlIo.AXIS_X to -459L, AvlIo.AXIS_Y to 204L, AvlIo.IGNITION to 1L)),
        )
        assertEquals(50.0, point.speedKmh, 1e-9)
        assertEquals(-4.5, point.accelerationMps2, 0.01)
        assertEquals(2.0, point.lateralAccelerationMps2, 0.01)
        assertEquals(245.0, point.headingDeg, 1e-9)
        assertEquals(true, point.ignitionOn)
        assertEquals(GnssQuality.Good, point.gnss)
    }

    @Test
    fun `falls back to GPS speed without OBD and to unknown ignition`() {
        val point = AvlTelemetryMapper.toTelemetry(record(io = emptyMap()))
        assertEquals(52.0, point.speedKmh, 1e-9)
        assertNull(point.ignitionOn)
        assertEquals(0.0, point.accelerationMps2, 1e-9)
    }

    @Test
    fun `no satellites means the position is lost but OBD speed still counts`() {
        val noFix = fix.copy(satellites = 0, speedKmh = 0)
        val point = AvlTelemetryMapper.toTelemetry(record(noFix, mapOf(AvlIo.OBD_VEHICLE_SPEED to 61L)))
        assertEquals(GnssQuality.Lost, point.gnss)
        assertEquals(61.0, point.speedKmh, 1e-9)
    }

    @Test
    fun `few satellites or a poor HDOP is a weak fix`() {
        assertEquals(GnssQuality.Weak, AvlTelemetryMapper.toTelemetry(record(fix.copy(satellites = 4), emptyMap())).gnss)
        assertEquals(GnssQuality.Weak, AvlTelemetryMapper.toTelemetry(record(io = mapOf(AvlIo.GNSS_HDOP to 35L))).gnss)
    }
}
