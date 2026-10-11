package com.example.gpstracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.Asphalt
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.NightBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_MS = 1_000
private const val HINT_MS = 2_000L
private const val FILL_ALPHA = 0.3f

/**
 * Start/stop control for a trip. Ending needs a 1 s press-and-hold with a
 * progress fill, so a stray tap while driving cannot end the trip.
 */
@Composable
fun TripButton(
    isTripActive: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHoldHint by remember { mutableStateOf(false) }
    LaunchedEffect(showHoldHint) {
        if (showHoldHint) {
            delay(HINT_MS)
            showHoldHint = false
        }
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isTripActive) {
            EndTripButton(
                enabled = enabled,
                onStop = onStop,
                onShortPress = { showHoldHint = true },
            )
        } else {
            StartTripButton(enabled = enabled, onStart = onStart)
        }
        val hint = when {
            !enabled -> stringResource(R.string.trip_waiting)
            showHoldHint && isTripActive -> stringResource(R.string.trip_hold_hint)
            else -> null
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun StartTripButton(enabled: Boolean, onStart: () -> Unit) {
    val label = stringResource(R.string.trip_start)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (enabled) NightBlue else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onStart),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) Asphalt else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun EndTripButton(enabled: Boolean, onStop: () -> Unit, onShortPress: () -> Unit) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val label = stringResource(R.string.trip_end)
    val holdLabel = stringResource(R.string.trip_hold_action)
    val danger = DriveColors.danger

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(2.dp, danger), MaterialTheme.shapes.medium)
            .drawBehind {
                drawRect(danger.copy(alpha = FILL_ALPHA), size = Size(size.width * progress.value, size.height))
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
                onClick(label = holdLabel) { onStop(); true }
                onLongClick(label = holdLabel) { onStop(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val hold = scope.launch {
                            progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                            onStop()
                            progress.snapTo(0f)
                        }
                        tryAwaitRelease()
                        if (hold.isActive) {
                            hold.cancel()
                            scope.launch { progress.snapTo(0f) }
                            onShortPress()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Stop, contentDescription = null, tint = danger, modifier = Modifier.size(28.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = danger,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun TripButtonPreview() {
    DriveMetricsTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TripButton(isTripActive = false, enabled = true, onStart = {}, onStop = {})
            TripButton(isTripActive = true, enabled = true, onStart = {}, onStop = {})
            TripButton(isTripActive = false, enabled = false, onStart = {}, onStop = {})
        }
    }
}
