package com.example.gpstracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.model.ScoreBand
import com.example.gpstracker.ui.model.scoreBandOf
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.KerbHigh

enum class ScoreBadgeSize(val diameter: Dp) {
    Small(40.dp),
    Medium(72.dp),
    Large(120.dp),
}

/** Ring filled in proportion to the score, with the number (and band label when large). */
@Composable
fun ScoreBadge(
    score: Int,
    band: ScoreBand,
    modifier: Modifier = Modifier,
    size: ScoreBadgeSize = ScoreBadgeSize.Medium,
) {
    val fraction by animateFloatAsState((score / 100f).coerceIn(0f, 1f), tween(300), label = "score-ring")
    val color = band.color()
    val label = stringResource(band.labelRes())
    val description = stringResource(R.string.score_cd, score, label)
    val numberStyle = when (size) {
        ScoreBadgeSize.Small -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.displaySmall
    }

    Box(
        modifier = modifier
            .size(size.diameter)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size.diameter)) {
            val stroke = 6.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(KerbHigh, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = score.toString(), style = numberStyle, color = MaterialTheme.colorScheme.onSurface)
            if (size == ScoreBadgeSize.Large) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = Concrete)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun ScoreBadgePreview() {
    DriveMetricsTheme {
        Row {
            ScoreBadge(96, scoreBandOf(96), size = ScoreBadgeSize.Small)
            ScoreBadge(74, scoreBandOf(74), size = ScoreBadgeSize.Medium)
            ScoreBadge(41, scoreBandOf(41), size = ScoreBadgeSize.Large)
        }
    }
}
