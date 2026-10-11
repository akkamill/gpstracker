package com.example.gpstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.model.FuelEstimateUi
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import java.util.Locale

/**
 * Fuel and money estimate for a trip or a period. Everything here is an
 * estimate and is labelled as such. When no fuel price is set, money lines are
 * replaced by a prompt to add one.
 */
@Composable
fun FuelCard(
    estimate: FuelEstimateUi?,
    savedLabel: String?,
    projectionLabel: String?,
    onSetPrice: () -> Unit,
    modifier: Modifier = Modifier,
    savedIsWorse: Boolean = false,
) {
    var showInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.fuel_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.fuel_info_cd))
            }
        }

        if (estimate == null) {
            Text(
                text = stringResource(R.string.fuel_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            StatTileRow(
                tiles = listOf(
                    { m -> StatTile(oneDecimal(estimate.fuelUsedL), stringResource(R.string.unit_l), stringResource(R.string.fuel_used), m) },
                    { m -> StatTile(twoDecimals(estimate.fuelWastedL), stringResource(R.string.unit_l), stringResource(R.string.fuel_wasted), m) },
                    { m -> StatTile(oneDecimal(estimate.co2Kg), stringResource(R.string.unit_kg), stringResource(R.string.fuel_co2), m) },
                ),
            )
            if (estimate.wastedCostLabel != null) {
                Text(
                    text = stringResource(R.string.fuel_lost, estimate.wastedCostLabel),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (savedLabel != null) {
                    val color = if (savedIsWorse) DriveColors.warning else DriveColors.success
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Savings, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Text(savedLabel, style = MaterialTheme.typography.bodyMedium, color = color)
                    }
                }
                if (projectionLabel != null) {
                    Text(
                        text = projectionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.fuel_add_price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSetPrice) { Text(stringResource(R.string.fuel_add_price_action)) }
                }
            }
        }

        Text(
            text = stringResource(R.string.fuel_footer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showInfo) {
        InfoBottomSheet(
            title = stringResource(R.string.estimates_title),
            text = stringResource(R.string.estimates_body),
            onDismiss = { showInfo = false },
        )
    }
}

private fun oneDecimal(value: Double) = String.format(Locale.US, "%.1f", value)

private fun twoDecimals(value: Double) = String.format(Locale.US, "%.2f", value)

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun FuelCardPreview() {
    DriveMetricsTheme {
        val state = PreviewStates.insightsWeekBetter
        FuelCard(state.fuel, state.savedVsPreviousLabel, state.projectedYearlySavingLabel, onSetPrice = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun FuelCardNoPricePreview() {
    DriveMetricsTheme {
        FuelCard(PreviewStates.insightsNoPrice.fuel, null, null, onSetPrice = {})
    }
}
