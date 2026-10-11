package com.example.gpstracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.KerbHigh
import com.example.gpstracker.ui.theme.LineWhite
import com.example.gpstracker.ui.theme.NightBlue
import com.example.gpstracker.ui.theme.SignRed
import com.example.gpstracker.ui.theme.SignalAmber
import com.example.gpstracker.ui.theme.speedValue
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
private const val MAX_KMH = 180f
private const val MAJOR_TICK_STEP = 20
private const val MINOR_TICK_STEP = 10
private const val LABEL_STEP = 40
private const val ANIMATION_MS = 300

/** Round speedometer, 0–180 km/h over 270 degrees, with a marker at the speed limit. */
@Composable
fun SpeedGauge(
    speedKmh: Int,
    limitKmh: Int?,
    isOverLimit: Boolean,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    offline: Boolean = false,
) {
    val fraction by animateFloatAsState(
        targetValue = (speedKmh / MAX_KMH).coerceIn(0f, 1f),
        animationSpec = tween(ANIMATION_MS),
        label = "gauge-arc",
    )
    val shownSpeed by animateIntAsState(speedKmh, tween(ANIMATION_MS), label = "gauge-number")
    val arcColor = if (isOverLimit) SignRed else NightBlue
    val overBy = limitKmh?.let { (speedKmh - it).coerceAtLeast(0) } ?: 0
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = Concrete)

    val inactive = dimmed || offline
    val description = when {
        offline -> stringResource(R.string.gauge_cd_offline)
        limitKmh == null -> stringResource(R.string.gauge_cd_unknown, speedKmh)
        isOverLimit -> stringResource(R.string.gauge_cd, speedKmh, limitKmh, overBy)
        else -> stringResource(R.string.gauge_cd_within, speedKmh, limitKmh)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth * 1.5f
            val diameter = size.minDimension - inset * 2
            val radius = diameter / 2
            val center = Offset(size.width / 2, size.height / 2)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(diameter, diameter)

            drawArc(KerbHigh, START_ANGLE, SWEEP_ANGLE, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            if (!inactive) {
                drawArc(arcColor, START_ANGLE, SWEEP_ANGLE * fraction, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }

            fun pointAt(kmh: Float, r: Float): Offset {
                val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * kmh / MAX_KMH).toDouble())
                return Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
            }

            var tick = 0
            while (tick <= MAX_KMH.toInt()) {
                val major = tick % MAJOR_TICK_STEP == 0
                val outer = radius - strokeWidth / 2 - 4.dp.toPx()
                val inner = outer - if (major) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    color = Concrete,
                    start = pointAt(tick.toFloat(), inner),
                    end = pointAt(tick.toFloat(), outer),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                )
                if (tick % LABEL_STEP == 0 && tick <= 160) {
                    val layout = measurer.measure(tick.toString(), labelStyle)
                    val at = pointAt(tick.toFloat(), inner - 14.dp.toPx())
                    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2, at.y - layout.size.height / 2))
                }
                tick += MINOR_TICK_STEP
            }

            if (limitKmh != null && !inactive) {
                val clamped = limitKmh.coerceIn(0, MAX_KMH.toInt()).toFloat()
                drawLine(
                    color = SignalAmber,
                    start = pointAt(clamped, radius - strokeWidth * 0.9f),
                    end = pointAt(clamped, radius + strokeWidth * 0.9f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (offline) stringResource(R.string.sign_unknown_text) else shownSpeed.toString(),
                style = MaterialTheme.typography.speedValue,
                color = if (inactive) Concrete else if (isOverLimit) SignRed else LineWhite,
            )
            Text(
                text = stringResource(R.string.gauge_unit),
                style = MaterialTheme.typography.labelMedium,
                color = Concrete,
            )
            if (isOverLimit && !inactive) {
                Text(
                    text = stringResource(R.string.gauge_over, overBy),
                    style = MaterialTheme.typography.labelLarge,
                    color = SignRed,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SpeedGaugeNormalPreview() {
    DriveMetricsTheme { SpeedGauge(speedKmh = 48, limitKmh = 50, isOverLimit = false) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SpeedGaugeOverLimitPreview() {
    DriveMetricsTheme { SpeedGauge(speedKmh = 72, limitKmh = 50, isOverLimit = true) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SpeedGaugeParkedPreview() {
    DriveMetricsTheme { SpeedGauge(speedKmh = 0, limitKmh = null, isOverLimit = false, dimmed = true) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SpeedGaugeOfflinePreview() {
    DriveMetricsTheme { SpeedGauge(speedKmh = 0, limitKmh = 50, isOverLimit = false, offline = true) }
}
