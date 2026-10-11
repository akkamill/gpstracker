package com.example.gpstracker.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.ui.components.color
import com.example.gpstracker.ui.model.scoreBandOf
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.NightBlue

private const val MAX_SCORE = 100f

/** 120 x 32 dp line of recent scores; the last point is a dot in its score band color. */
@Composable
fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(120.dp).height(32.dp)) {
        if (values.isEmpty()) return@Canvas
        val pad = 5.dp.toPx()
        val usableW = size.width - pad * 2
        val usableH = size.height - pad * 2
        val points = values.mapIndexed { i, v ->
            val x = if (values.size == 1) size.width / 2 else pad + usableW * i / (values.size - 1)
            Offset(x, pad + usableH * (1f - v.coerceIn(0, 100) / MAX_SCORE))
        }
        val path = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(path, NightBlue, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(scoreBandOf(values.last()).color(), radius = 4.dp.toPx(), center = points.last())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SparklinePreview() {
    DriveMetricsTheme { Sparkline(values = listOf(41, 58, 63, 74, 89, 91, 100)) }
}
