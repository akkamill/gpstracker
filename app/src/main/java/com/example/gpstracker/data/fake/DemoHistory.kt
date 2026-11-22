package com.example.gpstracker.data.fake

import com.example.gpstracker.data.estimate.InsightsCalculator
import com.example.gpstracker.domain.driving.DrivingRules
import com.example.gpstracker.domain.driving.Scoring
import com.example.gpstracker.model.driving.DrivingEvent
import com.example.gpstracker.model.driving.DrivingEventType
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.model.trip.Trip
import com.example.gpstracker.simulator.road.GeoPoint
import com.example.gpstracker.simulator.road.Kaunas
import com.example.gpstracker.simulator.road.Route

/**
 * Eight weeks of past trips on the demo routes, written to the database on the first start so
 * Trips and Insights are not empty in a demo. They are ordinary trips: real routes, events on
 * real roads and scores from the real scoring rule. The driving gets better week by week, and
 * one week has no trips, so the trend chart shows a gap.
 */
object DemoHistory {
    private const val WEEKS = 8
    private const val GAP_WEEK = 2
    private const val WEEK_MS = InsightsCalculator.WEEK_MS
    private const val ROUTE_STEP_M = 50
    private const val MS_PER_HOUR = 3_600_000.0
    private const val SPEEDING_OVER_KMH = 16.0
    private const val SPEEDING_MS_PER_EVENT = 90_000L
    private const val SLOWER_THAN_LIMIT_KMH = 8.0
    private const val SEED_MULTIPLIER = 31
    private const val SEED_INCREMENT = 17
    private const val SEED_MODULUS = 97

    private val rules = DrivingRules()
    private val eventOrder = listOf(
        DrivingEventType.HarshBraking,
        DrivingEventType.Speeding,
        DrivingEventType.SharpCorner,
        DrivingEventType.HarshAcceleration,
    )

    /** Peak value of each kind of event: m/s², or km/h over the limit for speeding. */
    private val peaks = mapOf(
        DrivingEventType.HarshBraking to 4.1,
        DrivingEventType.Speeding to SPEEDING_OVER_KMH,
        DrivingEventType.SharpCorner to 4.6,
        DrivingEventType.HarshAcceleration to 3.6,
    )

    fun build(nowMs: Long): List<SavedTrip> {
        val trips = mutableListOf<SavedTrip>()
        var seed = 7
        for (week in 0 until WEEKS) {
            if (week == GAP_WEEK) continue
            val weekEnd = nowMs - (WEEKS - 1 - week) * WEEK_MS
            val tripCount = 4 + week % 3
            repeat(tripCount) { index ->
                seed = (seed * SEED_MULTIPLIER + SEED_INCREMENT) % SEED_MODULUS
                val endMs = weekEnd - WEEK_MS + (index + 1) * WEEK_MS / (tripCount + 1)
                val route = if (index % 2 == 0) Kaunas.toAkropolis else Kaunas.toZaliakalnis
                // early weeks have up to six events a trip, the last weeks mostly none
                val events = ((WEEKS - 1 - week) * 2 / 3 + seed % 3 - 1).coerceAtLeast(0)
                trips += SavedTrip(id = "demo-$week-$index", savedAtMs = endMs, trip = trip(route, endMs, events, seed))
            }
        }
        return trips.sortedByDescending { it.savedAtMs }
    }

    private fun trip(route: Route, endMs: Long, eventCount: Int, seed: Int): Trip {
        val km = route.lengthM / 1_000
        val avgKmh = 26.0 + seed % 9
        val durationMs = (km / avgKmh * MS_PER_HOUR).toLong()
        val startMs = endMs - durationMs
        val events = List(eventCount) { k ->
            val fraction = (k + 1.0) / (eventCount + 1)
            event(route, eventOrder[(k + seed) % eventOrder.size], route.lengthM * fraction, startMs + (durationMs * fraction).toLong())
        }
        val speedings = events.count { it.type == DrivingEventType.Speeding }
        return Trip(
            startMs = startMs,
            endMs = endMs,
            distanceKm = km,
            events = events,
            route = (0..route.lengthM.toInt() step ROUTE_STEP_M).map { route.positionAt(it.toDouble()).toLatLon() },
            maxSpeedKmh = events.maxOfOrNull { it.speedKmh } ?: (route.segments.maxOf { it.speedLimitKmh } - SLOWER_THAN_LIMIT_KMH),
            speedingMs = speedings * SPEEDING_MS_PER_EVENT,
            score = Scoring.score(events.sumOf { it.penalty }, km),
            startPlace = route.from.name,
            endPlace = route.to.name,
            endedWithoutSignal = false,
        )
    }

    private fun event(route: Route, type: DrivingEventType, atM: Double, timestampMs: Long): DrivingEvent {
        val road = route.segmentAt(atM)
        val limit = road.speedLimitKmh.toDouble()
        return DrivingEvent(
            type = type,
            timestampMs = timestampMs,
            position = route.positionAt(atM).toLatLon(),
            peak = peaks.getValue(type),
            speedKmh = if (type == DrivingEventType.Speeding) limit + SPEEDING_OVER_KMH else limit - SLOWER_THAN_LIMIT_KMH,
            penalty = rules.penaltyOf(type),
            speedLimitKmh = road.speedLimitKmh,
            roadName = road.name,
        )
    }

    private fun GeoPoint.toLatLon() = LatLon(lat, lon)
}
