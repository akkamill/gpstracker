package com.example.gpstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.KerbHigh
import com.example.gpstracker.ui.theme.LineWhite
import com.example.gpstracker.ui.theme.NightBlue
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

private const val BOUNDS_PADDING = 0.10
private const val FOLLOW_SPAN_DEG = 0.006
private const val MIN_SPAN_DEG = 1e-5

/**
 * Route map with a stable signature. Version 1 draws on a [Canvas], so it
 * needs no API key; a later step can swap the body for Google Maps Compose
 * without touching any screen.
 */
@Composable
fun RouteMap(
    route: List<GeoPointUi>,
    events: List<DrivingEventUi>,
    modifier: Modifier = Modifier,
    currentPosition: GeoPointUi? = null,
    selectedEventId: String? = null,
    followCurrent: Boolean = false,
) {
    CanvasRouteMap(route, events, modifier, currentPosition, selectedEventId, followCurrent)
}

@Composable
private fun CanvasRouteMap(
    route: List<GeoPointUi>,
    events: List<DrivingEventUi>,
    modifier: Modifier,
    currentPosition: GeoPointUi?,
    selectedEventId: String?,
    followCurrent: Boolean,
) {
    val description = stringResource(R.string.map_preview_label)
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGrid()
            val projection = Projection.fit(
                points = route + events.map { it.position } + listOfNotNull(currentPosition),
                focus = if (followCurrent) currentPosition else null,
                width = size.width,
                height = size.height,
            ) ?: return@Canvas

            if (route.size > 1) {
                val path = Path()
                route.forEachIndexed { i, p ->
                    val o = projection.toOffset(p)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(
                    path,
                    NightBlue,
                    style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            events.forEach { e ->
                val selected = e.id == selectedEventId
                val radius = (if (selected) 14 else 10).dp.toPx()
                val o = projection.toOffset(e.position)
                drawCircle(e.type.color(), radius, o)
                drawCircle(Color.White, radius, o, style = Stroke(2.dp.toPx()))
            }
            currentPosition?.let {
                val o = projection.toOffset(it)
                drawCircle(NightBlue, 13.dp.toPx(), o)
                drawCircle(LineWhite, 9.dp.toPx(), o)
            }
        }
        Text(
            text = stringResource(R.string.map_preview_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}

private fun DrawScope.drawGrid() {
    val step = 32.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(KerbHigh, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(KerbHigh, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        y += step
    }
}

/** Equirectangular projection fitted to a bounding box, preserving aspect ratio. */
private class Projection(
    private val minLon: Double,
    private val maxLat: Double,
    private val scale: Double,
    private val cosLat: Double,
    private val offsetX: Float,
    private val offsetY: Float,
) {
    fun toOffset(p: GeoPointUi) = Offset(
        offsetX + ((p.lon - minLon) * cosLat * scale).toFloat(),
        offsetY + ((maxLat - p.lat) * scale).toFloat(),
    )

    companion object {
        fun fit(points: List<GeoPointUi>, focus: GeoPointUi?, width: Float, height: Float): Projection? {
            if (points.isEmpty() && focus == null) return null
            val minLat: Double
            val maxLat: Double
            val minLon: Double
            val maxLon: Double
            if (focus != null) {
                minLat = focus.lat - FOLLOW_SPAN_DEG
                maxLat = focus.lat + FOLLOW_SPAN_DEG
                minLon = focus.lon - FOLLOW_SPAN_DEG
                maxLon = focus.lon + FOLLOW_SPAN_DEG
            } else {
                minLat = points.minOf { it.lat }
                maxLat = points.maxOf { it.lat }
                minLon = points.minOf { it.lon }
                maxLon = points.maxOf { it.lon }
            }
            val midLat = (minLat + maxLat) / 2
            val cosLat = cos(Math.toRadians(midLat))
            val latSpan = max(maxLat - minLat, MIN_SPAN_DEG)
            val lonSpan = max((maxLon - minLon) * cosLat, MIN_SPAN_DEG)
            val usableW = width * (1 - 2 * BOUNDS_PADDING)
            val usableH = height * (1 - 2 * BOUNDS_PADDING)
            val scale = min(usableW / lonSpan, usableH / latSpan)
            val offsetX = (width - lonSpan * scale) / 2
            val offsetY = (height - latSpan * scale) / 2
            return Projection(minLon, maxLat, scale, cosLat, offsetX.toFloat(), offsetY.toFloat())
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun RouteMapPreview() {
    DriveMetricsTheme {
        RouteMap(
            route = FakeData.route,
            events = FakeData.events,
            currentPosition = FakeData.route[60],
            selectedEventId = "e2",
            modifier = Modifier.height(240.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun RouteMapEmptyPreview() {
    DriveMetricsTheme {
        RouteMap(
            route = emptyList(),
            events = emptyList(),
            currentPosition = FakeData.route[0],
            modifier = Modifier.height(240.dp),
        )
    }
}
