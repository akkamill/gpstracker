package com.example.gpstracker.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsRoute(
    onTriggerTestCrash: () -> Unit,
    onBack: () -> Unit,
    onEditVehicle: () -> Unit,
    onSignedOut: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onDataSourceChange = viewModel::onDataSourceChange,
        onServerUrlChange = viewModel::onServerUrlChange,
        onToleranceChange = viewModel::onToleranceChange,
        onCrashDetectionChange = viewModel::onCrashDetectionChange,
        onEmergencyContactChange = viewModel::onEmergencyContactChange,
        onEditVehicle = onEditVehicle,
        onTriggerTestCrash = onTriggerTestCrash,
        onSignOut = {
            viewModel.signOut()
            onSignedOut()
        },
        onCreateAccount = onCreateAccount,
    )
}
