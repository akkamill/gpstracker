package com.example.gpstracker.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.Caution
import com.example.gpstracker.ui.theme.Danger
import com.example.gpstracker.ui.theme.GpstrackerTheme
import com.example.gpstracker.ui.theme.Safe

private const val START_ANGLE = 150f
private const val SWEEP_ANGLE = 240f
private const val MAX_SPEED_KMH = 160f
private const val CAUTION_KMH = 70
private const val DANGER_KMH = 90

/** Animated 240-degree speedometer; the arc colour warns as speed approaches the limit. */
@Composable
fun SpeedGauge(speedKmh: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = (speedKmh / MAX_SPEED_KMH).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "gauge-fraction",
    )
    val arcColor by animateColorAsState(
        targetValue = when {
            speedKmh >= DANGER_KMH -> Danger
            speedKmh >= CAUTION_KMH -> Caution
            else -> Safe
        },
        label = "gauge-color",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val description = stringResource(R.string.gauge_description, speedKmh)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val stroke = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val diameter = size.width - stroke.width
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, START_ANGLE, SWEEP_ANGLE, false, topLeft, arcSize, style = stroke)
            drawArc(arcColor, START_ANGLE, SWEEP_ANGLE * fraction, false, topLeft, arcSize, style = stroke)
        }
        Column(modifier = Modifier.padding(top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedKmh.toString(),
                fontSize = 88.sp,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = stringResource(R.string.gauge_unit),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1116)
@Composable
private fun SpeedGaugePreview() {
    GpstrackerTheme { SpeedGauge(speedKmh = 96) }
}
