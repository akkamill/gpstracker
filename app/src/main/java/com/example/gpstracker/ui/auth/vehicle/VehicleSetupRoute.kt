package com.example.gpstracker.ui.auth.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * @param onDone called after a successful save or when the user skips
 * @param onBack shown as a back arrow when the screen is opened from Settings, else null
 */
@Composable
fun VehicleSetupRoute(
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleSetupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()

    VehicleSetupScreen(
        state = state,
        errors = errors,
        onFuelTypeChange = viewModel::onFuelTypeChange,
        onConsumptionChange = viewModel::onConsumptionChange,
        onPriceChange = viewModel::onPriceChange,
        onMonthlyKmChange = viewModel::onMonthlyKmChange,
        onSave = { if (viewModel.save()) onDone() },
        onSkip = onDone,
        onBack = onBack,
    )
}
