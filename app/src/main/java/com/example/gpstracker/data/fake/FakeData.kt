package com.example.gpstracker.data.fake

import com.example.gpstracker.data.estimate.FuelEstimator
import com.example.gpstracker.data.estimate.FuelInput
import com.example.gpstracker.data.estimate.InsightsCalculator
import com.example.gpstracker.data.estimate.TripRecord
import com.example.gpstracker.data.estimate.VehicleNumbers
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.FuelEstimateUi
import com.example.gpstracker.ui.model.FuelType
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.TripDetailUi
import com.example.gpstracker.ui.model.TripSummaryUi
import com.example.gpstracker.ui.model.TripsUiState
import com.example.gpstracker.ui.model.scoreBandOf

/** Hand-made sample data used by previews and by the demo simulator. */
object FakeData {

    private const val ROUTE_POINTS = 80

    private val waypoints = listOf(
        GeoPointUi(54.9068, 23.9520),
        GeoPointUi(54.9040, 23.9410),
        GeoPointUi(54.9000, 23.9320),
        GeoPointUi(54.8960, 23.9210),
        GeoPointUi(54.8925, 23.9090),
        GeoPointUi(54.8900, 23.8960),
        GeoPointUi(54.8930, 23.8840),
    )

    /** About 80 points through Kaunas, roughly Žaliakalnis to Savanorių pr. to Akropolis. */
    val route: List<GeoPointUi> = buildRoute()

    private fun buildRoute(): List<GeoPointUi> {
        val segments = waypoints.size - 1
        return List(ROUTE_POINTS) { i ->
            val t = i.toDouble() / (ROUTE_POINTS - 1) * segments
            val seg = t.toInt().coerceAtMost(segments - 1)
            val f = t - seg
            val a = waypoints[seg]
            val b = waypoints[seg + 1]
            GeoPointUi(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)
        }
    }

    val events: List<DrivingEventUi> = listOf(
        DrivingEventUi(
            id = "e1", type = EventType.HarshBraking, title = "Harsh braking", detail = "−4.1 m/s²",
            penalty = 3, timeLabel = "14:12", placeLabel = "Savanorių pr.", position = route[18],
        ),
        DrivingEventUi(
            id = "e2", type = EventType.Speeding, title = "Speeding", detail = "72 in a 50 zone",
            penalty = 4, timeLabel = "14:19", placeLabel = "Savanorių pr.", position = route[36],
        ),
        DrivingEventUi(
            id = "e3", type = EventType.SharpCorner, title = "Sharp corner", detail = "0.5 g lateral",
            penalty = 2, timeLabel = "14:26", placeLabel = "Jonavos g.", position = route[54],
        ),
        DrivingEventUi(
            id = "e4", type = EventType.HarshAcceleration, title = "Harsh acceleration", detail = "+3.6 m/s²",
            penalty = 2, timeLabel = "14:33", placeLabel = "Karaliaus Mindaugo pr.", position = route[68],
        ),
    )

    private fun summary(
        id: String, date: String, range: String, fromTo: String,
        km: Double, duration: String, score: Int, events: Int,
    ) = TripSummaryUi(id, date, range, fromTo, km, duration, score, scoreBandOf(score), events)

    private const val ROUTE_FROM_TO = "Žaliakalnis → Akropolis"

    val trips: List<TripSummaryUi> = listOf(
        summary("trip-1", "Today", "14:05 – 14:41", ROUTE_FROM_TO, 18.2, "36 min", 100, 0),
        summary("trip-2", "Today", "08:10 – 08:34", "Namai → Darbas", 11.4, "24 min", 91, 1),
        summary("trip-3", "Yesterday", "17:48 – 18:20", "Darbas → Namai", 12.1, "32 min", 89, 4),
        summary("trip-4", "Yesterday", "12:30 – 12:52", "Centras → Eiguliai", 9.6, "22 min", 74, 3),
        summary("trip-5", "Mon, 5 Oct", "19:02 – 19:40", "Šančiai → Domeikava", 15.3, "38 min", 63, 5),
        summary("trip-6", "Mon, 5 Oct", "09:15 – 09:31", "Namai → Centras", 6.8, "16 min", 58, 4),
        summary("trip-7", "Mon, 5 Oct", "07:40 – 07:58", "Namai → Vilijampolė", 7.2, "18 min", 41, 7),
    )

    val tripsState = TripsUiState(
        weekAverageScore = trips.map { it.score }.average().toInt(),
        weekScores = trips.map { it.score }.reversed(),
        sections = trips.groupBy { it.dateLabel }.toList(),
    )

    /** Demo vehicle used by canned data: petrol, 6.5 L/100 km, EUR 1.60 per litre. */
    val demoVehicle = VehicleNumbers(FuelType.Petrol, 6.5, 1.60, 1000.0)

    private val estimator = FuelEstimator()

    private fun fuelFor(distanceKm: Double, events: List<DrivingEventUi>, speedingMinutes: Double): FuelEstimateUi =
        estimator.estimate(
            FuelInput(
                distanceKm = distanceKm,
                consumptionL100 = demoVehicle.consumptionL100,
                harshAccelerations = events.count { it.type == EventType.HarshAcceleration },
                harshBrakings = events.count { it.type == EventType.HarshBraking },
                speedingMinutes = speedingMinutes,
                fuelType = demoVehicle.fuelType,
            ),
        ).toUi(demoVehicle.fuelPricePerL)

    /** A trip with four deductions and three tips. */
    val tripWithEvents = TripDetailUi(
        summary = trips.first { it.id == "trip-3" },
        avgSpeedKmh = 38,
        maxSpeedKmh = 72,
        route = route,
        events = events,
        fuel = fuelFor(distanceKm = 12.1, events = events, speedingMinutes = 2.0),
        tips = listOf(
            "Brake earlier before the Savanorių pr. junction.",
            "Keep to 50 on Savanorių pr.; you were 22 over at 14:19.",
            "Ease into the throttle when pulling away from lights.",
        ),
    )

    /** A perfect trip: no events and no tips, so nothing was wasted. */
    val perfectTrip = TripDetailUi(
        summary = trips.first { it.id == "trip-1" },
        avgSpeedKmh = 30,
        maxSpeedKmh = 49,
        route = route,
        events = emptyList(),
        fuel = fuelFor(distanceKm = 18.2, events = emptyList(), speedingMinutes = 0.0),
        tips = emptyList(),
    )

    val tripDetails: Map<String, TripDetailUi> = mapOf(
        tripWithEvents.summary.id to tripWithEvents,
        perfectTrip.summary.id to perfectTrip,
    )

    private const val WEEKS = 8
    private const val WEEK_MS = InsightsCalculator.WEEK_MS
    private const val GAP_WEEK = 2

    /**
     * Eight weeks of fake trips ending at [nowMs], getting better over time, with
     * one week without trips so the trend chart shows a gap.
     */
    fun history(nowMs: Long): List<TripRecord> {
        val records = mutableListOf<TripRecord>()
        var seed = 7
        for (week in 0 until WEEKS) {
            if (week == GAP_WEEK) continue
            val weekEnd = nowMs - (WEEKS - 1 - week) * WEEK_MS
            val tripCount = 4 + week % 3
            repeat(tripCount) { index ->
                seed = (seed * 31 + 17) % 97
                val score = (66 + week * 2.6 + seed % 9 - 4).toInt().coerceIn(35, 100)
                val risk = (100 - score) / 8
                val speeding = risk / 3 + seed % 2
                records += TripRecord(
                    id = "hist-$week-$index",
                    startMs = weekEnd - WEEK_MS + (index + 1) * WEEK_MS / (tripCount + 1),
                    distanceKm = 8.0 + seed % 15,
                    score = score,
                    harshAccelerations = (risk + 1) / 4,
                    harshBrakings = risk / 3,
                    speedingEvents = speeding,
                    sharpCorners = risk / 4,
                    speedingMinutes = speeding * 1.5,
                )
            }
        }
        return records
    }
}
