package com.example.gpstracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.estimate.parseDecimal
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.data.settings.PhoneNumbers
import com.example.gpstracker.ui.auth.components.PrimaryButton
import com.example.gpstracker.ui.auth.initialsOf
import com.example.gpstracker.ui.components.InfoBottomSheet
import com.example.gpstracker.ui.model.FuelType
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.VehicleProfileUi
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme

private val TOLERANCE_OPTIONS = listOf(0, 5, 10)

/**
 * Settings, pushed from the account avatar. Stateless apart from the sign-out
 * confirmation and the two info sheets, which are one-off dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onToleranceChange: (Int) -> Unit,
    onTripEndAfterChange: (Int) -> Unit,
    onCrashDetectionChange: (Boolean) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onEditVehicle: () -> Unit,
    onTriggerTestCrash: () -> Unit,
    onSignOut: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSupport: () -> Unit = {},
    onEmergencyPhoneChange: (String) -> Unit = {},
    alertsAllowed: Boolean = true,
    onAllowAlerts: () -> Unit = {},
    showDebugTools: Boolean = false,
    onSkipToNextDrive: () -> Unit = {},
) {
    var confirmSignOut by remember { mutableStateOf(false) }
    var showEstimates by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AccountSection(state, onSignOutClick = { confirmSignOut = true }, onCreateAccount = onCreateAccount)
            VehicleSection(state.vehicle, onEditVehicle)
            CarDataSection(state)
            ScoringSection(state, onToleranceChange)
            TripsSection(state, onTripEndAfterChange)
            SafetySection(
                state = state,
                onCrashDetectionChange = onCrashDetectionChange,
                onEmergencyContactChange = onEmergencyContactChange,
                onEmergencyPhoneChange = onEmergencyPhoneChange,
                alertsAllowed = alertsAllowed,
                onAllowAlerts = onAllowAlerts,
                onTriggerTestCrash = onTriggerTestCrash,
            )
            HelpSection(onOpenSupport)
            AboutSection(
                state = state,
                onShowEstimates = { showEstimates = true },
                showDebugTools = showDebugTools,
                onSkipToNextDrive = onSkipToNextDrive,
            )
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.signout_title)) },
            text = { Text(stringResource(R.string.signout_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text(stringResource(R.string.signout_confirm), color = DriveColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (showEstimates) {
        InfoBottomSheet(
            title = stringResource(R.string.estimates_title),
            text = stringResource(R.string.estimates_body),
            onDismiss = { showEstimates = false },
        )
    }
}

@Composable
private fun SectionTitle(@androidx.annotation.StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccountSection(state: SettingsUiState, onSignOutClick: () -> Unit, onCreateAccount: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.settings_section_account)
        val name = state.accountName
        if (name != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initialsOf(name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    state.accountEmail?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onSignOutClick) {
                    Text(stringResource(R.string.signout_action), color = DriveColors.danger)
                }
            }
        } else {
            Text(
                text = stringResource(R.string.settings_guest_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PrimaryButton(text = stringResource(R.string.welcome_create), onClick = onCreateAccount)
        }
    }
}

@Composable
private fun HelpSection(onOpenSupport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_section_help)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onOpenSupport),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_support),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_support_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VehicleSection(vehicle: VehicleProfileUi, onEditVehicle: () -> Unit) {
    val fuel = stringResource(
        if (vehicle.fuelType == FuelType.Petrol) R.string.fuel_type_petrol else R.string.fuel_type_diesel,
    )
    val consumption = parseDecimal(vehicle.consumptionL100)?.let { "%.1f".format(java.util.Locale.US, it) }
        ?: vehicle.consumptionL100
    val price = if (vehicle.fuelPricePerL.isBlank()) {
        stringResource(R.string.settings_vehicle_no_price)
    } else {
        stringResource(R.string.settings_vehicle_price, vehicle.fuelPricePerL)
    }
    val summary = stringResource(R.string.settings_vehicle_summary, fuel, consumption, price)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_section_vehicle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onEditVehicle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Where the car's data comes from. Read-only: the demo car is the only source for now; a real
 * tracker would be connected through the server.
 */
@Composable
private fun CarDataSection(state: SettingsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_section_car_data)
        Text(
            stringResource(R.string.settings_car_source),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_car_source_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.serverAddress.isNotBlank()) {
            Text(
                stringResource(R.string.settings_server_address, state.serverAddress),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoringSection(state: SettingsUiState, onToleranceChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.settings_section_scoring)
        Text(
            text = stringResource(R.string.settings_tolerance_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TOLERANCE_OPTIONS.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = state.speedTolerance == value,
                    onClick = { onToleranceChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, TOLERANCE_OPTIONS.size),
                ) {
                    Text(
                        if (value == 0) {
                            stringResource(R.string.settings_tolerance_zero)
                        } else {
                            stringResource(R.string.settings_tolerance_plus, value)
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_tolerance_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val TRIP_END_OPTIONS = listOf(2, 5, 10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripsSection(state: SettingsUiState, onTripEndAfterChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.settings_section_trips)
        Text(
            text = stringResource(R.string.settings_trip_end_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TRIP_END_OPTIONS.forEachIndexed { index, minutes ->
                SegmentedButton(
                    selected = state.tripEndAfterMinutes == minutes,
                    onClick = { onTripEndAfterChange(minutes) },
                    shape = SegmentedButtonDefaults.itemShape(index, TRIP_END_OPTIONS.size),
                ) { Text(stringResource(R.string.settings_trip_end_option, minutes)) }
            }
        }
        Text(
            text = stringResource(R.string.settings_trip_end_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_trip_auto_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SafetySection(
    state: SettingsUiState,
    onCrashDetectionChange: (Boolean) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onEmergencyPhoneChange: (String) -> Unit,
    alertsAllowed: Boolean,
    onAllowAlerts: () -> Unit,
    onTriggerTestCrash: () -> Unit,
) {
    val phoneInvalid = !PhoneNumbers.isAcceptable(state.emergencyContactPhone)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.settings_section_safety)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_crash_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_crash_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.crashDetectionEnabled, onCheckedChange = onCrashDetectionChange)
        }
        OutlinedTextField(
            value = state.emergencyContactName,
            onValueChange = onEmergencyContactChange,
            enabled = state.crashDetectionEnabled,
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.settings_contact_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.emergencyContactPhone,
            onValueChange = onEmergencyPhoneChange,
            enabled = state.crashDetectionEnabled,
            singleLine = true,
            isError = phoneInvalid,
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.settings_contact_phone_label)) },
            supportingText = {
                Text(stringResource(if (phoneInvalid) R.string.settings_contact_phone_error else R.string.settings_contact_phone_hint))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.crashDetectionEnabled && PhoneNumbers.normalize(state.emergencyContactPhone) != null) {
            if (alertsAllowed) {
                Text(
                    stringResource(R.string.settings_alert_permission_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(R.string.settings_alert_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DriveColors.warning,
                )
                Button(onClick = onAllowAlerts) { Text(stringResource(R.string.settings_alert_permission_action)) }
            }
        }
        TextButton(onClick = onTriggerTestCrash) {
            Text(stringResource(R.string.settings_test_crash))
        }
    }
}

@Composable
private fun AboutSection(
    state: SettingsUiState,
    onShowEstimates: () -> Unit,
    showDebugTools: Boolean,
    onSkipToNextDrive: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_section_about)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onShowEstimates),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.estimates_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.settings_privacy_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.privacy_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_version, state.appVersion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showDebugTools) {
            TextButton(onClick = onSkipToNextDrive) { Text(stringResource(R.string.settings_skip_drive)) }
        }
    }
}

private const val BG = 0xFF0F1417
private const val PHONE = "spec:width=411dp,height=1100dp"

@Composable
private fun PreviewScreen(state: SettingsUiState) {
    DriveMetricsTheme {
        SettingsScreen(
            state = state,
            onBack = {},
            onToleranceChange = {},
            onTripEndAfterChange = {},
            onCrashDetectionChange = {},
            onEmergencyContactChange = {},
            onEditVehicle = {},
            onTriggerTestCrash = {},
            onSignOut = {},
            onCreateAccount = {},
            showDebugTools = true,
        )
    }
}

@Preview(name = "Signed in", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SettingsSignedInPreview() = PreviewScreen(PreviewStates.settingsSignedIn)

@Preview(name = "Guest", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SettingsGuestPreview() = PreviewScreen(PreviewStates.settingsDefault)
