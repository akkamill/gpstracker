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
    fun `demo route is between four and five kilometres and starts and ends at its places`() {
        assertTrue(route.lengthM in 4_000.0..5_000.0)
        assertEquals(0.0, Geo.distanceM(route.positionAt(0.0), route.from.point), 1e-6)
        assertEquals(0.0, Geo.distanceM(route.positionAt(route.lengthM), route.to.point), 1e-6)
    }

    @Test
    fun `the turns between streets are where the streets meet`() {
        // left into K. Petrausko g., right into Parodos g., right into Karo Ligoninės g.
        listOf(Kaunas.PETRAUSKO to -1, Kaunas.PARODOS to 1, Kaunas.KARO_LIGONINES to 1).forEach { (street, side) ->
            val turn = route.corners.single { abs(it.atM - route.segmentStartM(street)) < 1.0 }
            assertTrue("$street: ${turn.turnDeg}", turn.isSharp && turn.turnDeg * side in 50.0..110.0)
        }
    }

    @Test
    fun `bends never overlap, however short the pieces of a curved street are`() {
        listOf(route, Kaunas.toZaliakalnis).forEach { r ->
            assertTrue(r.corners.zipWithNext().all { (a, b) -> a.endM <= b.startM + 1e-6 })
        }
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
    fun `a reversed route has the same length and swapped ends`() {
        val back = route.reversed()
        assertEquals(route.lengthM, back.lengthM, 1e-6)
        assertEquals(route.to, back.from)
        assertEquals(route.corners.size, back.corners.size)
    }

    @Test
    fun `segment lookup follows the speed limits along the route`() {
        val streets = route.segments.map { it.name }
        assertEquals(listOf(Kaunas.SAVANORIU, Kaunas.PETRAUSKO, Kaunas.PARODOS, Kaunas.VYTAUTO), streets.take(4))
        val limits = listOf(0.15, 0.45, 0.62, 0.75, 0.99).map { route.segmentAt(route.lengthM * it).speedLimitKmh }
        assertEquals(listOf(50, 50, 40, 50, 20), limits)
    }

    @Test
    fun `road map finds the road under a point and nothing far away`() {
        val onParodos = route.positionAt(route.segmentStartM(Kaunas.PARODOS) + 250)
        assertEquals(40, Kaunas.map.roadAt(Geo.offset(onParodos, 8.0, 0.0))?.speedLimitKmh)
        assertNull(Kaunas.map.roadAt(Geo.offset(onParodos, 500.0, 0.0)))
    }

    @Test
    fun `places name the ends of the route`() {
        assertEquals("Akropolis", Kaunas.map.placeNear(route.to.point)?.name)
        assertEquals("Žaliakalnis", Kaunas.map.placeNear(Geo.offset(route.from.point, 100.0, 100.0))?.name)
        // the way back ends where the way there starts, so the demo loop never jumps
        assertEquals(route.from, Kaunas.toZaliakalnis.to)
        assertEquals(route.to, Kaunas.toZaliakalnis.from)
    }
}
