package com.example.gpstracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.model.DataSource
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.theme.DriveMetricsTheme

private val TOLERANCE_OPTIONS = listOf(0, 5, 10)

/** True when [url] is a WebSocket address. */
fun isValidServerUrl(url: String): Boolean = url.startsWith("ws://") || url.startsWith("wss://")

/** App settings. Stateless: every change goes out as a callback. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDataSourceChange: (DataSource) -> Unit,
    onServerUrlChange: (String) -> Unit,
    onToleranceChange: (Int) -> Unit,
    onCrashDetectionChange: (Boolean) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onTriggerTestCrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        DataSourceSection(state, onDataSourceChange, onServerUrlChange)
        ScoringSection(state, onToleranceChange)
        SafetySection(state, onCrashDetectionChange, onEmergencyContactChange, onTriggerTestCrash)
        AboutSection(state)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataSourceSection(
    state: SettingsUiState,
    onDataSourceChange: (DataSource) -> Unit,
    onServerUrlChange: (String) -> Unit,
) {
    val live = state.dataSource == DataSource.Live
    val urlInvalid = live && !isValidServerUrl(state.serverUrl)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.settings_section_data_source)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !live,
                onClick = { onDataSourceChange(DataSource.Replay) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.settings_source_replay)) }
            SegmentedButton(
                selected = live,
                onClick = { onDataSourceChange(DataSource.Live) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.settings_source_live)) }
        }
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            enabled = live,
            isError = urlInvalid,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_server_label)) },
            supportingText = {
                Text(
                    stringResource(
                        if (urlInvalid) R.string.settings_server_error else R.string.settings_server_hint,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
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

@Composable
private fun SafetySection(
    state: SettingsUiState,
    onCrashDetectionChange: (Boolean) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onTriggerTestCrash: () -> Unit,
) {
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
            label = { Text(stringResource(R.string.settings_contact_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onTriggerTestCrash) {
            Text(stringResource(R.string.settings_test_crash))
        }
    }
}

@Composable
private fun AboutSection(state: SettingsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_section_about)
        Text(
            text = stringResource(R.string.settings_privacy_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_privacy_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_version, state.appVersion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val BG = 0xFF0F1417

@Preview(name = "Default", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=1000dp")
@Composable
private fun SettingsDefaultPreview() {
    DriveMetricsTheme {
        SettingsScreen(PreviewStates.settingsDefault, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Live, invalid URL", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=1000dp")
@Composable
private fun SettingsInvalidPreview() {
    DriveMetricsTheme {
        SettingsScreen(PreviewStates.settingsInvalidUrl, {}, {}, {}, {}, {}, {})
    }
}
