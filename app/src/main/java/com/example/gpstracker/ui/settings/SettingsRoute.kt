package com.example.gpstracker.ui.settings

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    onTriggerTestCrash: () -> Unit,
    onBack: () -> Unit,
    onEditVehicle: () -> Unit,
    onSignedOut: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    SettingsScreen(
        state = state,
        onBack = onBack,
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
        onOpenSupport = onOpenSupport,
        showDebugTools = isDebuggable,
        onSkipToNextDrive = viewModel::skipToNextDrive,
    )
}
