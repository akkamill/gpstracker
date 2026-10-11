package com.example.gpstracker.ui.auth.vehicle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.auth.components.AuthScaffold
import com.example.gpstracker.ui.auth.components.AuthTextField
import com.example.gpstracker.ui.auth.components.PrimaryButton
import com.example.gpstracker.ui.model.FuelType
import com.example.gpstracker.ui.model.VehicleErrorsUi
import com.example.gpstracker.ui.model.VehicleProfileUi
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/**
 * Vehicle form used for fuel and cost estimates. Shown after sign up (with a skip
 * link) and from Settings (with a back arrow).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSetupScreen(
    state: VehicleProfileUi,
    errors: VehicleErrorsUi,
    onFuelTypeChange: (FuelType) -> Unit,
    onConsumptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onMonthlyKmChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    AuthScaffold(modifier = modifier, onBack = onBack) {
        Text(
            text = stringResource(R.string.vehicle_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.vehicle_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.fuelType == FuelType.Petrol,
                onClick = { onFuelTypeChange(FuelType.Petrol) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.fuel_type_petrol)) }
            SegmentedButton(
                selected = state.fuelType == FuelType.Diesel,
                onClick = { onFuelTypeChange(FuelType.Diesel) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.fuel_type_diesel)) }
        }
        AuthTextField(
            value = state.consumptionL100,
            onValueChange = onConsumptionChange,
            label = stringResource(R.string.vehicle_consumption),
            error = errors.consumption,
            supportingText = stringResource(R.string.vehicle_consumption_hint),
            suffix = stringResource(R.string.vehicle_consumption_suffix),
            keyboardType = KeyboardType.Decimal,
        )
        AuthTextField(
            value = state.fuelPricePerL,
            onValueChange = onPriceChange,
            label = stringResource(R.string.vehicle_price),
            error = errors.price,
            supportingText = stringResource(R.string.vehicle_price_hint),
            prefix = stringResource(R.string.vehicle_price_prefix),
            suffix = stringResource(R.string.vehicle_price_suffix),
            keyboardType = KeyboardType.Decimal,
        )
        AuthTextField(
            value = state.monthlyKm,
            onValueChange = onMonthlyKmChange,
            label = stringResource(R.string.vehicle_monthly),
            error = errors.monthlyKm,
            suffix = stringResource(R.string.vehicle_monthly_suffix),
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            onImeAction = onSave,
        )
        PrimaryButton(text = stringResource(R.string.vehicle_save), onClick = onSave)
        TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.vehicle_skip))
        }
    }
}

private const val BG = 0xFF0F1417
private const val PHONE = "spec:width=411dp,height=891dp"

@Preview(name = "Defaults", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun VehicleDefaultsPreview() {
    DriveMetricsTheme {
        VehicleSetupScreen(PreviewStates.vehicleDefault, VehicleErrorsUi(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Consumption error", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun VehicleErrorPreview() {
    DriveMetricsTheme {
        VehicleSetupScreen(PreviewStates.vehicleInvalid, PreviewStates.vehicleInvalidErrors, {}, {}, {}, {}, {}, {})
    }
}
