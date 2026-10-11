package com.example.gpstracker.ui.settings

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    SettingsScreen(
        state = state,
        onBack = onBack,
        onDataSourceChange = viewModel::onDataSourceChange,
        onServerUrlChange = viewModel::onServerUrlChange,
        onToleranceChange = viewModel::onToleranceChange,
        onTripEndAfterChange = viewModel::onTripEndAfterChange,
        onCrashDetectionChange = viewModel::onCrashDetectionChange,
        onEmergencyContactChange = viewModel::onEmergencyContactChange,
        onEditVehicle = onEditVehicle,
        onTriggerTestCrash = onTriggerTestCrash,
        onSignOut = {
            viewModel.signOut()
            onSignedOut()
        },
        onCreateAccount = onCreateAccount,
        showDebugTools = isDebuggable,
        onSkipToNextDrive = viewModel::skipToNextDrive,
    )
}
