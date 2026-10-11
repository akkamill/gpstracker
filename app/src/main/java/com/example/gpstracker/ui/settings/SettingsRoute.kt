package com.example.gpstracker.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsRoute(
    onTriggerTestCrash: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onDataSourceChange = viewModel::onDataSourceChange,
        onServerUrlChange = viewModel::onServerUrlChange,
        onToleranceChange = viewModel::onToleranceChange,
        onCrashDetectionChange = viewModel::onCrashDetectionChange,
        onEmergencyContactChange = viewModel::onEmergencyContactChange,
        onTriggerTestCrash = onTriggerTestCrash,
    )
}
