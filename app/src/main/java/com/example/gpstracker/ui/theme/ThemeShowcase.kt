package com.example.gpstracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R

/** Temporary screen used to check the color tokens and text styles on a device. */
@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
    val swatches = listOf(
        "Asphalt" to Asphalt, "Kerb" to Kerb, "KerbHigh" to KerbHigh, "LineWhite" to LineWhite,
        "Concrete" to Concrete, "NightBlue" to NightBlue, "SignalAmber" to SignalAmber,
        "SignRed" to SignRed, "GoGreen" to GoGreen,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.showcase_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.showcase_colors),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        swatches.forEach { (name, color) -> Swatch(name, color) }
        Text(
            stringResource(R.string.showcase_type),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.showcase_sample_speed), style = MaterialTheme.typography.speedValue)
        Text(stringResource(R.string.showcase_sample_score), style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.showcase_sample_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.showcase_sample_card), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.showcase_sample_body), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.showcase_sample_secondary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.showcase_sample_button), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.showcase_sample_unit),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color),
        )
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417, heightDp = 900)
@Composable
private fun ThemeShowcasePreview() {
    DriveMetricsTheme { ThemeShowcase() }
}
