package com.example.gpstracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.ui.model.ConnectionStatus
import com.example.gpstracker.ui.model.GpsStatus
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveMetricsTheme

private const val SPIN_MS = 1_200
private const val FULL_TURN = 360f

/** Pill showing the data connection state: icon plus text. */
@Composable
fun ConnectionChip(status: ConnectionStatus, modifier: Modifier = Modifier) {
    StatusChip(
        icon = status.icon(),
        color = status.color(),
        text = stringResource(status.labelRes()),
        spinning = status == ConnectionStatus.Reconnecting,
        showProgress = status == ConnectionStatus.Connecting,
        small = status == ConnectionStatus.Live,
        modifier = modifier,
    )
}

/** Pill showing the GPS fix quality: icon plus text. */
@Composable
fun GpsChip(status: GpsStatus, modifier: Modifier = Modifier) {
    StatusChip(
        icon = status.icon(),
        color = status.color(),
        text = stringResource(status.labelRes()),
        modifier = modifier,
    )
}

@Composable
private fun StatusChip(
    icon: ImageVector?,
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
    spinning: Boolean = false,
    showProgress: Boolean = false,
    small: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            showProgress -> CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = Concrete,
                strokeWidth = 2.dp,
            )
            icon != null -> {
                val angle = if (spinning) spinAngle() else 0f
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(if (small) 10.dp else 18.dp).rotate(angle),
                )
            }
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun spinAngle(): Float {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "spin-angle",
    )
    return -angle
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun StatusChipsPreview() {
    DriveMetricsTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConnectionChip(ConnectionStatus.Live)
            ConnectionChip(ConnectionStatus.Reconnecting)
            GpsChip(GpsStatus.Weak)
        }
    }
}
