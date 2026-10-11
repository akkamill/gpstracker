package com.example.gpstracker.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.estimate.ComparisonResult
import com.example.gpstracker.data.estimate.compareToBenchmark
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.model.ComparisonRowUi
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.NightBlue
import java.util.Locale

private const val SCALE_HEADROOM = 1.1
private val VALUE_COLUMN_WIDTH = 40.dp
private val BAR_HEIGHT = 12.dp

/**
 * Events per 100 km, you against the benchmark, one block per category.
 * "You" is a filled blue bar; "Benchmark" is an outline-only grey bar, so the
 * two stay distinguishable without color. Lower is better.
 */
@Composable
fun ComparisonBarChart(rows: List<ComparisonRowUi>, modifier: Modifier = Modifier) {
    val scaleMax = (rows.maxOfOrNull { maxOf(it.you, it.benchmark) } ?: 0.0) * SCALE_HEADROOM

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row -> ComparisonBlock(row, scaleMax) }
        Text(
            text = stringResource(R.string.comparison_caption),
            style = MaterialTheme.typography.labelMedium,
            color = Concrete,
        )
    }
}

@Composable
private fun ComparisonBlock(row: ComparisonRowUi, scaleMax: Double) {
    val result = compareToBenchmark(row.you, row.benchmark)
    val resultText = stringResource(
        when (result) {
            ComparisonResult.Better -> R.string.compare_better
            ComparisonResult.Similar -> R.string.compare_similar
            ComparisonResult.Worse -> R.string.compare_worse
        },
    )
    val you = format(row.you)
    val benchmark = format(row.benchmark)
    val description = stringResource(R.string.comparison_row_cd, row.label, you, benchmark, resultText)

    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            ResultBadge(result, resultText)
        }
        BarRow(value = row.you, text = you, scaleMax = scaleMax, filled = true)
        BarRow(value = row.benchmark, text = benchmark, scaleMax = scaleMax, filled = false)
    }
}

@Composable
private fun ResultBadge(result: ComparisonResult, text: String) {
    val (icon, color) = when (result) {
        ComparisonResult.Better -> Icons.Rounded.TrendingDown to DriveColors.success
        ComparisonResult.Worse -> Icons.Rounded.TrendingUp to DriveColors.warning
        ComparisonResult.Similar -> Icons.Rounded.Remove to Concrete
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun BarRow(value: Double, text: String, scaleMax: Double, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.weight(1f).height(BAR_HEIGHT)) {
            val fraction = if (scaleMax <= 0.0) 0f else (value / scaleMax).toFloat().coerceIn(0f, 1f)
            val barWidth: Dp = maxWidth * fraction
            val shape = RoundedCornerShape(4.dp)
            Box(
                Modifier
                    .width(barWidth.coerceAtLeast(2.dp))
                    .height(BAR_HEIGHT)
                    .then(
                        if (filled) {
                            Modifier.background(NightBlue, shape)
                        } else {
                            Modifier.border(2.dp, Concrete, shape)
                        },
                    ),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) MaterialTheme.colorScheme.onSurface else Concrete,
            modifier = Modifier.width(VALUE_COLUMN_WIDTH),
        )
    }
}

private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun ComparisonBarChartPreview() {
    DriveMetricsTheme { ComparisonBarChart(PreviewStates.insightsWeekBetter.comparison) }
}
