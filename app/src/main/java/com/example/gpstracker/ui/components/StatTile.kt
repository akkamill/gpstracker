package com.example.gpstracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.statValue

/** Big value with an optional unit and a small label below. No card background. */
@Composable
fun StatTile(value: String, unit: String?, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val unitStyle = MaterialTheme.typography.labelMedium.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = buildAnnotatedString {
                append(value)
                if (unit != null) {
                    append(" ")
                    withStyle(unitStyle) { append(unit) }
                }
            },
            style = MaterialTheme.typography.statValue,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Equal-width tiles separated by 1 dp vertical dividers. */
@Composable
fun StatTileRow(modifier: Modifier = Modifier, tiles: List<@Composable (Modifier) -> Unit>) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tiles.forEachIndexed { index, tile ->
            if (index > 0) VerticalDivider(color = MaterialTheme.colorScheme.outline)
            tile(Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun StatTileRowPreview() {
    DriveMetricsTheme {
        StatTileRow(
            tiles = listOf(
                { m -> StatTile("12.4", "km", "Distance", m) },
                { m -> StatTile("23:41", null, "Time", m) },
                { m -> StatTile("3", null, "Events", m) },
            ),
        )
    }
}
