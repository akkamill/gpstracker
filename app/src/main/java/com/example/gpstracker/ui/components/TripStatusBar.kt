package com.example.gpstracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.PauseCircle
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
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.model.VehicleState
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.NightBlue

/**
 * One quiet line that says what the car is doing. Not clickable: trips are
 * detected from the car's data and the user never starts or ends one.
 */
@Composable
fun TripStatusBar(
    vehicleState: VehicleState,
    tripStartedLabel: String?,
    modifier: Modifier = Modifier,
) {
    val (icon, color) = when (vehicleState) {
        VehicleState.Driving -> Icons.Rounded.DirectionsCar to NightBlue
        VehicleState.Stopped -> Icons.Rounded.PauseCircle to Concrete
        VehicleState.Parked -> Icons.Rounded.LocalParking to Concrete
    }
    val text = when (vehicleState) {
        VehicleState.Driving ->
            if (tripStartedLabel != null) {
                stringResource(R.string.status_driving, tripStartedLabel)
            } else {
                stringResource(R.string.status_driving_plain)
            }
        VehicleState.Stopped -> stringResource(R.string.status_stopped)
        VehicleState.Parked -> stringResource(R.string.status_parked)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clearAndSetSemantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun TripStatusBarPreview() {
    DriveMetricsTheme {
        Column {
            TripStatusBar(VehicleState.Driving, "Trip started 14:05")
            TripStatusBar(VehicleState.Stopped, "Trip started 14:05")
            TripStatusBar(VehicleState.Parked, null)
        }
    }
}
