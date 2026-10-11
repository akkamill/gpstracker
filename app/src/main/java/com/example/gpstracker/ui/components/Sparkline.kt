package com.example.gpstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.ui.model.scoreBandOf
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.NightBlue

private const val MAX_SCORE = 100f

/** Line of recent trip scores with one dot per trip, colored by score band. */
@Composable
fun Sparkline(scores: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        if (scores.isEmpty()) return@Canvas
        val pad = 6.dp.toPx()
        val usableW = size.width - pad * 2
        val usableH = size.height - pad * 2
        val points = scores.mapIndexed { i, s ->
            val x = if (scores.size == 1) size.width / 2 else pad + usableW * i / (scores.size - 1)
            val y = pad + usableH * (1f - s.coerceIn(0, 100) / MAX_SCORE)
            Offset(x, y)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(NightBlue, a, b, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEachIndexed { i, p ->
            drawCircle(scoreBandOf(scores[i]).color(), radius = 4.dp.toPx(), center = p)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SparklinePreview() {
    DriveMetricsTheme { Sparkline(scores = listOf(41, 58, 63, 74, 89, 91, 100)) }
}
