package com.example.gpstracker.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.Caution
import com.example.gpstracker.ui.theme.Danger
import com.example.gpstracker.ui.theme.GpstrackerTheme
import com.example.gpstracker.ui.theme.Safe

private const val GOOD_SCORE = 80
private const val OK_SCORE = 60

/** Score, distance and max speed in one row of equal cards. */
@Composable
fun ScoreAndStats(score: Int, distanceKm: Double, maxSpeedKmh: Int, modifier: Modifier = Modifier) {
    val scoreColor = when {
        score >= GOOD_SCORE -> Safe
        score >= OK_SCORE -> Caution
        else -> Danger
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.score_label),
            value = score.toString(),
            valueColor = scoreColor,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.stat_distance),
            value = stringResource(R.string.stat_distance_value, distanceKm),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.stat_max_speed),
            value = stringResource(R.string.stat_max_speed_value, maxSpeedKmh),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1116)
@Composable
private fun ScoreAndStatsPreview() {
    GpstrackerTheme { ScoreAndStats(score = 85, distanceKm = 3.4, maxSpeedKmh = 97) }
}
