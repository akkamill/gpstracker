package com.example.gpstracker.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpstracker.data.emergency.PhoneEmergencyNotifier
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
    fun alertsAllowed() = PhoneEmergencyNotifier.PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var allowed by remember { mutableStateOf(alertsAllowed()) }
    val askForAlerts = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { allowed = alertsAllowed() }
    // the driver may change it in the system settings and come back
    LifecycleResumeEffect(Unit) {
        allowed = alertsAllowed()
        onPauseOrDispose {}
    }
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    SettingsScreen(
        state = state,
        onBack = onBack,
        onToleranceChange = viewModel::onToleranceChange,
        onTripEndAfterChange = viewModel::onTripEndAfterChange,
        onCrashDetectionChange = viewModel::onCrashDetectionChange,
        onEmergencyContactChange = viewModel::onEmergencyContactChange,
        onEmergencyPhoneChange = viewModel::onEmergencyPhoneChange,
        onEditVehicle = onEditVehicle,
        onTriggerTestCrash = onTriggerTestCrash,
        onSignOut = {
            viewModel.signOut()
            onSignedOut()
        },
        onCreateAccount = onCreateAccount,
        onOpenSupport = onOpenSupport,
        alertsAllowed = allowed,
        onAllowAlerts = { askForAlerts.launch(PhoneEmergencyNotifier.PERMISSIONS) },
        showDebugTools = isDebuggable,
        onSkipToNextDrive = viewModel::skipToNextDrive,
    )
}
