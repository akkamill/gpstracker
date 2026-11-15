package com.example.gpstracker.simulator

import com.example.gpstracker.simulator.road.Geo
import com.example.gpstracker.simulator.road.GeoPoint
import com.example.gpstracker.simulator.road.Kaunas
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadTest {
    private val route = Kaunas.toAkropolis

    @Test
    fun `distance between two known points is about right`() {
        // 0.01 degree of latitude is about 1.11 km everywhere
        assertEquals(1112.0, Geo.distanceM(GeoPoint(54.90, 23.90), GeoPoint(54.91, 23.90)), 2.0)
    }

    @Test
    fun `turn is signed, right is positive`() {
        assertEquals(90.0, Geo.turnDeg(0.0, 90.0), 1e-9)
        assertEquals(-90.0, Geo.turnDeg(0.0, 270.0), 1e-9)
        assertEquals(20.0, Geo.turnDeg(350.0, 10.0), 1e-9)
    }

    @Test
    fun `demo route is about five kilometres and starts and ends at its places`() {
        assertTrue(route.lengthM in 4_500.0..5_500.0)
        assertEquals(0.0, Geo.distanceM(route.positionAt(0.0), route.from.point), 1e-6)
        assertEquals(0.0, Geo.distanceM(route.positionAt(route.lengthM), route.to.point), 1e-6)
    }

    @Test
    fun `route has one sharp right turn onto Karaliaus Mindaugo pr`() {
        val sharp = route.corners.filter { it.isSharp }
        val right = sharp.single { it.turnDeg > 0 }
        assertTrue(right.turnDeg in 60.0..100.0)
        assertEquals(route.segmentStartM(3), right.atM, 1.0)
    }

    @Test
    fun `heading turns smoothly through a corner`() {
        val corner = route.corners.first { it.isSharp }
        val before = route.headingAt(corner.startM - 1)
        val middle = route.headingAt(corner.atM)
        val after = route.headingAt(corner.endM + 1)
        assertEquals(corner.turnDeg / 2, Geo.turnDeg(before, middle), 1.0)
        assertEquals(corner.turnDeg, Geo.turnDeg(before, after), 1.0)
    }

    @Test
    fun `reversed route has the same length and swapped ends`() {
        val back = route.reversed()
        assertEquals(route.lengthM, back.lengthM, 1e-6)
        assertEquals(route.to, back.from)
        assertEquals(route.corners.size, back.corners.size)
    }

    @Test
    fun `segment lookup follows the speed limits along the route`() {
        val limits = listOf(0.15, 0.45, 0.72, 0.88, 0.97).map { route.segmentAt(route.lengthM * it).speedLimitKmh }
        assertEquals(listOf(50, 70, 50, 50, 30), limits)
    }

    @Test
    fun `road map finds the road under a point and nothing far away`() {
        val onFastStretch = route.positionAt(route.segmentStartM(1) + 500)
        assertEquals(70, Kaunas.map.roadAt(Geo.offset(onFastStretch, 8.0, 0.0))?.speedLimitKmh)
        assertNull(Kaunas.map.roadAt(Geo.offset(onFastStretch, 500.0, 0.0)))
    }

    @Test
    fun `places name the ends of the route`() {
        assertEquals("Akropolis", Kaunas.map.placeNear(route.to.point)?.name)
        assertEquals("Žaliakalnis", Kaunas.map.placeNear(Geo.offset(route.from.point, 100.0, 100.0))?.name)
        assertTrue(abs(route.lengthM - Kaunas.toZaliakalnis.lengthM) < 1e-6)
    }
}
