package com.example.gpstracker.ui.crash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.model.AlertOutcome
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.LineWhite
import com.example.gpstracker.ui.theme.SignRed
import com.example.gpstracker.ui.theme.speedValue

private const val COUNTDOWN_SECONDS = 10f

/**
 * Full-screen crash alert with a countdown. Back press counts as "I'm OK".
 * Renders nothing unless [CrashAlertUiState.visible] is true.
 */
@Composable
fun CrashAlertOverlay(
    state: CrashAlertUiState,
    onImOk: () -> Unit,
    onSendNow: () -> Unit,
    modifier: Modifier = Modifier,
    contactName: String = "",
    contactPhone: String = "",
    onCall: () -> Unit = {},
    onText: () -> Unit = {},
) {
    if (!state.visible) return
    BackHandler(onBack = onImOk)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SignRed)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (state.sent) {
            SentContent(state, contactName, contactPhone, onCall, onText, onClose = onImOk)
        } else {
            CountingContent(state, contactName, onImOk, onSendNow)
        }
    }
}

@Composable
private fun CountingContent(
    state: CrashAlertUiState,
    contactName: String,
    onImOk: () -> Unit,
    onSendNow: () -> Unit,
) {
    val who = contactName.ifBlank { stringResource(R.string.crash_default_contact) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.event_crash),
            style = MaterialTheme.typography.headlineSmall,
            color = LineWhite,
        )
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(160.dp)) {
                val stroke = 10.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(LineWhite.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(
                    LineWhite,
                    -90f,
                    360f * (state.secondsLeft / COUNTDOWN_SECONDS).coerceIn(0f, 1f),
                    false,
                    topLeft,
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(
                text = state.secondsLeft.toString(),
                style = MaterialTheme.typography.speedValue,
                color = LineWhite,
            )
        }
        Text(
            text = pluralStringResource(R.plurals.crash_alerting, state.secondsLeft, who, state.secondsLeft),
            style = MaterialTheme.typography.bodyLarge,
            color = LineWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )
        Button(
            onClick = onImOk,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LineWhite, contentColor = SignRed),
            shape = MaterialTheme.shapes.medium,
        ) { Text(stringResource(R.string.crash_im_ok), style = MaterialTheme.typography.titleMedium) }
        OutlinedButton(
            onClick = onSendNow,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(2.dp, LineWhite),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LineWhite),
            shape = MaterialTheme.shapes.medium,
        ) { Text(stringResource(R.string.crash_send_now), style = MaterialTheme.typography.titleMedium) }
    }
}

/** After the countdown: what was done, and buttons to call or text by hand when it was not. */
@Composable
private fun SentContent(
    state: CrashAlertUiState,
    contactName: String,
    contactPhone: String,
    onCall: () -> Unit,
    onText: () -> Unit,
    onClose: () -> Unit,
) {
    val who = contactName.ifBlank { stringResource(R.string.crash_default_contact) }
    val detail = when (state.outcome) {
        AlertOutcome.TextedAndCalled -> stringResource(R.string.crash_outcome_texted_called, who)
        AlertOutcome.TextedOnly -> stringResource(R.string.crash_outcome_texted, who)
        AlertOutcome.CalledOnly -> stringResource(R.string.crash_outcome_called, who)
        AlertOutcome.NotAllowed -> stringResource(R.string.crash_outcome_not_allowed, who)
        AlertOutcome.Failed -> stringResource(R.string.crash_outcome_failed, who)
        AlertOutcome.NoNumber, null -> stringResource(R.string.crash_no_phone)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Icon(
            if (state.reached) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = LineWhite,
            modifier = Modifier.size(96.dp),
        )
        Text(
            text = stringResource(if (state.reached) R.string.crash_sent else R.string.crash_not_sent),
            style = MaterialTheme.typography.headlineSmall,
            color = LineWhite,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )
        Text(text = detail, style = MaterialTheme.typography.bodyLarge, color = LineWhite, textAlign = TextAlign.Center)
        // whatever did not happen by itself can still be done by hand
        if (contactPhone.isNotBlank() && state.outcome != AlertOutcome.TextedAndCalled) {
            if (state.outcome != AlertOutcome.CalledOnly) {
                Button(
                    onClick = onCall,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LineWhite, contentColor = SignRed),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null)
                    Text(
                        stringResource(R.string.crash_call, who),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (state.outcome != AlertOutcome.TextedOnly) {
                OutlinedButton(
                    onClick = onText,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = BorderStroke(2.dp, LineWhite),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LineWhite),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null)
                    Text(
                        stringResource(R.string.crash_text),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(2.dp, LineWhite),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LineWhite),
            shape = MaterialTheme.shapes.medium,
        ) { Text(stringResource(R.string.crash_close), style = MaterialTheme.typography.titleMedium) }
    }
}

@Preview(name = "Counting", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun CrashCountingPreview() {
    DriveMetricsTheme { CrashAlertOverlay(PreviewStates.crashCounting, onImOk = {}, onSendNow = {}, contactName = "Mom") }
}

@Preview(name = "Sent", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun CrashSentPreview() {
    DriveMetricsTheme {
        CrashAlertOverlay(PreviewStates.crashSent, onImOk = {}, onSendNow = {}, contactName = "Mom", contactPhone = "+37060012345")
    }
}
