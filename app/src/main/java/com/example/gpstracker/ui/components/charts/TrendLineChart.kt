package com.example.gpstracker.ui.components.charts

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.KerbHigh
import com.example.gpstracker.ui.theme.LineWhite
import com.example.gpstracker.ui.theme.NightBlue
import kotlin.math.abs

private val GRID_LEVELS = listOf(50, 70, 85)
private const val BAND_ALPHA = 0.08f
private const val DRAW_IN_MS = 600
private const val MAX_LABELS_ALL = 8

/**
 * Score over time: your trend (solid, with dots, gaps where there were no trips)
 * against the simulated benchmark (dashed). Tap a point for its values.
 */
@Composable
fun TrendLineChart(
    labels: List<String>,
    you: List<Int?>,
    benchmark: List<Int>,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started || reducedMotion) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(DRAW_IN_MS),
        label = "trend-draw-in",
    )
    var selected by remember(labels, you) { mutableStateOf<Int?>(null) }

    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelMedium.copy(color = Concrete)
    val tipStyle = MaterialTheme.typography.labelMedium.copy(color = LineWhite)

    val first = you.firstOrNull { it != null }
    val last = you.lastOrNull { it != null }
    val description = if (first != null && last != null) {
        stringResource(R.string.trend_cd, first, last, labels.size, benchmark.average().toInt())
    } else {
        stringResource(R.string.trend_cd_empty)
    }
    val tooltip = selected?.let { i ->
        val youValue = you.getOrNull(i)
        if (youValue != null) {
            stringResource(R.string.trend_tooltip, labels.getOrElse(i) { "" }, youValue, benchmark.getOrElse(i) { 0 })
        } else {
            stringResource(R.string.trend_tooltip_no_trips, labels.getOrElse(i) { "" }, benchmark.getOrElse(i) { 0 })
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description }
            .pointerInput(labels, you) {
                detectTapGestures { tap ->
                    val leftPad = LEFT_PAD_DP.dp.toPx()
                    val count = labels.size
                    if (count == 0) return@detectTapGestures
                    val plotW = size.width - leftPad - RIGHT_PAD_DP.dp.toPx()
                    val index = (0 until count).minByOrNull { i ->
                        abs(tap.x - xAt(i, count, leftPad, plotW))
                    }
                    selected = if (index == selected) null else index
                }
            },
    ) {
        val leftPad = LEFT_PAD_DP.dp.toPx()
        val rightPad = RIGHT_PAD_DP.dp.toPx()
        val topPad = TOP_PAD_DP.dp.toPx()
        val bottomPad = BOTTOM_PAD_DP.dp.toPx()
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad
        val count = labels.size

        fun yAt(value: Float) = topPad + (1f - value / 100f) * plotH

        // score-band backdrop, very subtle
        drawRect(DriveColors.danger.copy(alpha = BAND_ALPHA), Offset(leftPad, yAt(60f)), Size(plotW, yAt(0f) - yAt(60f)))
        drawRect(DriveColors.warning.copy(alpha = BAND_ALPHA), Offset(leftPad, yAt(85f)), Size(plotW, yAt(60f) - yAt(85f)))
        drawRect(DriveColors.success.copy(alpha = BAND_ALPHA), Offset(leftPad, yAt(100f)), Size(plotW, yAt(85f) - yAt(100f)))

        // grid and y labels
        GRID_LEVELS.forEach { level ->
            val y = yAt(level.toFloat())
            drawLine(KerbHigh, Offset(leftPad, y), Offset(leftPad + plotW, y), strokeWidth = 1.dp.toPx())
            val layout = measurer.measure(level.toString(), axisStyle)
            drawText(layout, topLeft = Offset(leftPad - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
        }

        // x labels
        val labelStep = if (count <= MAX_LABELS_ALL) 1 else 2
        labels.forEachIndexed { i, text ->
            if (i % labelStep != 0 && i != count - 1) return@forEachIndexed
            val layout = measurer.measure(text, axisStyle)
            val x = xAt(i, count, leftPad, plotW)
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, size.height - bottomPad + 6.dp.toPx()))
        }

        clipRect(right = leftPad + plotW * progress + rightPad) {
            // benchmark: dashed, no dots
            if (benchmark.size > 1) {
                val path = Path()
                benchmark.forEachIndexed { i, v ->
                    val p = Offset(xAt(i, count, leftPad, plotW), yAt(v.toFloat()))
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                drawPath(
                    path,
                    Concrete,
                    style = Stroke(
                        2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }
            // you: solid, dots, a null breaks the line
            var segment: Path? = null
            you.forEachIndexed { i, v ->
                if (v == null) {
                    segment?.let { drawPath(it, NightBlue, style = lineStroke()) }
                    segment = null
                } else {
                    val p = Offset(xAt(i, count, leftPad, plotW), yAt(v.toFloat()))
                    if (segment == null) segment = Path().apply { moveTo(p.x, p.y) } else segment!!.lineTo(p.x, p.y)
                }
            }
            segment?.let { drawPath(it, NightBlue, style = lineStroke()) }
            you.forEachIndexed { i, v ->
                if (v != null) {
                    drawCircle(NightBlue, 3.dp.toPx(), Offset(xAt(i, count, leftPad, plotW), yAt(v.toFloat())))
                }
            }
        }

        // tooltip
        val index = selected
        if (index != null && tooltip != null && index in labels.indices) {
            val value = you.getOrNull(index) ?: benchmark.getOrElse(index) { 0 }
            val anchor = Offset(xAt(index, count, leftPad, plotW), yAt(value.toFloat()))
            val layout = measurer.measure(tooltip, tipStyle)
            val padH = 8.dp.toPx()
            val padV = 4.dp.toPx()
            val w = layout.size.width + padH * 2
            val h = layout.size.height + padV * 2
            val x = (anchor.x - w / 2).coerceIn(0f, size.width - w)
            val y = (anchor.y - h - 10.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(KerbHigh, Offset(x, y), Size(w, h), CornerRadius(8.dp.toPx()))
            drawText(layout, topLeft = Offset(x + padH, y + padV))
            drawCircle(LineWhite, 8.dp.toPx() / 2, anchor)
        }
    }
}

private const val LEFT_PAD_DP = 32
private const val RIGHT_PAD_DP = 12
private const val TOP_PAD_DP = 12
private const val BOTTOM_PAD_DP = 26

private fun xAt(index: Int, count: Int, leftPad: Float, plotW: Float): Float =
    if (count <= 1) leftPad + plotW / 2 else leftPad + plotW * index / (count - 1)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.lineStroke() =
    Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** True when the user turned system animations off, in which case charts show at once. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun TrendLineChartPreview() {
    DriveMetricsTheme {
        TrendLineChart(
            labels = listOf("W36", "W37", "W38", "W39", "W40", "W41", "W42", "W43"),
            you = listOf(68, 71, null, 74, 78, 80, 84, 86),
            benchmark = listOf(74, 75, 73, 76, 72, 75, 73, 74),
        )
    }
}
