package com.example.gpstracker.ui.components

import android.graphics.Bitmap
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.ReportKind
import com.example.gpstracker.ui.model.RoadReportUi
import com.example.gpstracker.ui.theme.LineWhite
import com.example.gpstracker.ui.theme.NightBlue
import kotlin.math.roundToInt
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * True where a real street map may be shown. The app turns it on at its root; design previews
 * and UI tests leave it off and get the drawn map, which needs no network.
 */
val LocalStreetMap = staticCompositionLocalOf { false }

/** Free vector map tiles from OpenStreetMap data, in a dark style that fits the app; no key needed. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val FOLLOW_ZOOM = 15.5
private const val SINGLE_POINT_ZOOM = 14.0
private const val FIT_PADDING_DP = 48
private const val CAMERA_MS = 400
private const val DIMMED_OPACITY = 0.5f

/**
 * The route, driving events, road reports and the car on a real street map (MapLibre). The same
 * things as the drawn map, as map layers; [onUserMoved] is called when the driver drags the map.
 */
@Composable
internal fun StreetMap(
    route: List<GeoPointUi>,
    events: List<DrivingEventUi>,
    modifier: Modifier,
    currentPosition: GeoPointUi?,
    selectedEventId: String?,
    followCurrent: Boolean,
    dimmed: Boolean,
    reports: List<RoadReportUi>,
    description: String,
    onUserMoved: () -> Unit,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val painters = ReportKind.entries.associateWith { rememberVectorPainter(it.icon()) }
    val markers = remember(density) { painters.mapValues { (kind, painter) -> markerBitmap(kind.color(), painter, density) } }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val userMoved by rememberUpdatedState(onUserMoved)

    // the map view follows the screen's lifecycle, so it stops drawing when the app is in the background
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.uiSettings.apply {
                isLogoEnabled = false
                // the map data's credit stays one tap away, as its license asks; top right, where
                // no card or button covers it
                isAttributionEnabled = true
                attributionGravity = Gravity.TOP or Gravity.END
                val margin = with(density) { ATTRIBUTION_MARGIN_DP.dp.toPx() }.roundToInt()
                val top = with(density) { ATTRIBUTION_TOP_DP.dp.toPx() }.roundToInt()
                setAttributionMargins(0, top, margin, 0)
                setAttributionTintColor(LineWhite.toArgb())
                isCompassEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                if (!interactive) setAllGesturesEnabled(false)
            }
            m.addOnMoveListener(
                object : MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(detector: org.maplibre.android.gestures.MoveGestureDetector) = userMoved()

                    override fun onMove(detector: org.maplibre.android.gestures.MoveGestureDetector) = Unit

                    override fun onMoveEnd(detector: org.maplibre.android.gestures.MoveGestureDetector) = Unit
                },
            )
            m.setStyle(Style.Builder().fromUri(STYLE_URL)) { s ->
                addLayers(s, markers)
                map = m
                style = s
            }
        }
    }

    LaunchedEffect(style, route, events, reports, currentPosition, selectedEventId, dimmed) {
        style?.let { showData(it, route, events, reports, currentPosition, selectedEventId, dimmed) }
    }

    // follow the car, or show everything once when the content changes
    val content = route.size to reports.size + events.size
    LaunchedEffect(map, followCurrent, if (followCurrent) currentPosition else content) {
        val m = map ?: return@LaunchedEffect
        val target = currentPosition
        if (followCurrent && target != null) {
            m.easeCamera(CameraUpdateFactory.newLatLngZoom(target.latLng(), FOLLOW_ZOOM), CAMERA_MS)
        } else {
            fitAll(m, route + events.map { it.position } + reports.map { it.position } + listOfNotNull(target), density)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier.semantics { contentDescription = description })
}

private fun addLayers(style: Style, markers: Map<ReportKind, Bitmap>) {
    markers.forEach { (kind, bitmap) -> style.addImage(kind.name, bitmap) }
    listOf(ROUTE, EVENTS, REPORTS, CAR).forEach { style.addSource(GeoJsonSource(it)) }
    style.addLayer(
        LineLayer(ROUTE, ROUTE).withProperties(
            PropertyFactory.lineColor(NightBlue.toArgb()),
            PropertyFactory.lineWidth(ROUTE_WIDTH),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        CircleLayer(EVENTS, EVENTS).withProperties(
            PropertyFactory.circleColor(Expression.toColor(Expression.get(COLOR))),
            PropertyFactory.circleRadius(Expression.get(RADIUS)),
            PropertyFactory.circleStrokeColor(Color.White.toArgb()),
            PropertyFactory.circleStrokeWidth(2f),
        ),
    )
    style.addLayer(
        SymbolLayer(REPORTS, REPORTS).withProperties(
            PropertyFactory.iconImage(Expression.get(KIND)),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
        ),
    )
    style.addLayer(
        CircleLayer(CAR, CAR).withProperties(
            PropertyFactory.circleColor(LineWhite.toArgb()),
            PropertyFactory.circleRadius(CAR_RADIUS),
            PropertyFactory.circleStrokeColor(NightBlue.toArgb()),
            PropertyFactory.circleStrokeWidth(CAR_STROKE),
        ),
    )
}

@Suppress("LongParameterList") // one value per map layer
private fun showData(
    style: Style,
    route: List<GeoPointUi>,
    events: List<DrivingEventUi>,
    reports: List<RoadReportUi>,
    car: GeoPointUi?,
    selectedEventId: String?,
    dimmed: Boolean,
) {
    val line = if (route.size > 1) listOf(Feature.fromGeometry(LineString.fromLngLats(route.map { it.point() }))) else emptyList()
    style.source(ROUTE)?.setGeoJson(FeatureCollection.fromFeatures(line))
    style.getLayer(ROUTE)?.setProperties(PropertyFactory.lineOpacity(if (dimmed) DIMMED_OPACITY else 1f))
    style.source(EVENTS)?.setGeoJson(
        FeatureCollection.fromFeatures(
            events.map { e ->
                Feature.fromGeometry(e.position.point()).apply {
                    addStringProperty(COLOR, e.type.color().hex())
                    addNumberProperty(RADIUS, if (e.id == selectedEventId) SELECTED_RADIUS else EVENT_RADIUS)
                }
            },
        ),
    )
    style.source(REPORTS)?.setGeoJson(
        FeatureCollection.fromFeatures(
            reports.map { r -> Feature.fromGeometry(r.position.point()).apply { addStringProperty(KIND, r.kind.name) } },
        ),
    )
    style.source(CAR)?.setGeoJson(FeatureCollection.fromFeatures(listOfNotNull(car?.let { Feature.fromGeometry(it.point()) })))
}

private fun fitAll(map: MapLibreMap, points: List<GeoPointUi>, density: Density) {
    when {
        points.isEmpty() -> Unit

        points.distinct().size == 1 -> map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().latLng(), SINGLE_POINT_ZOOM))

        else -> {
            val bounds = LatLngBounds.Builder().includes(points.map { it.latLng() }).build()
            val padding = with(density) { FIT_PADDING_DP.dp.toPx() }.roundToInt()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }
}

/** A report marker drawn once into a bitmap, the same rounded square as on the drawn map. */
private fun markerBitmap(color: Color, icon: Painter, density: Density): Bitmap {
    val px = with(density) { MARKER_BITMAP_DP.dp.toPx() }.roundToInt()
    val image = ImageBitmap(px, px)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(px.toFloat(), px.toFloat())) {
        drawReportMarker(Offset(size.width / 2, size.height / 2), color, icon)
    }
    return image.asAndroidBitmap()
}

private fun Style.source(id: String) = getSourceAs<GeoJsonSource>(id)

private fun GeoPointUi.point() = Point.fromLngLat(lon, lat)

private fun GeoPointUi.latLng() = LatLng(lat, lon)

private fun Color.hex() = "#%06X".format(toArgb() and RGB_MASK)

private const val ROUTE = "route"
private const val EVENTS = "events"
private const val REPORTS = "reports"
private const val CAR = "car"
private const val COLOR = "color"
private const val RADIUS = "radius"
private const val KIND = "kind"
private const val ROUTE_WIDTH = 5f
private const val EVENT_RADIUS = 8
private const val SELECTED_RADIUS = 12
private const val CAR_RADIUS = 7f
private const val CAR_STROKE = 4f
private const val MARKER_BITMAP_DP = 30
private const val RGB_MASK = 0xFFFFFF
private const val ATTRIBUTION_MARGIN_DP = 12

// below the status bar and level with the status chips
private const val ATTRIBUTION_TOP_DP = 56
