package com.example.gpstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** What a [StatusBanner] is reporting; decides its accent color and icon. */
enum class BannerKind(val color: Color, val icon: ImageVector) {
    Reconnecting(DriveColors.warning, Icons.Rounded.Sync),
    GpsLost(DriveColors.danger, Icons.Rounded.GpsOff),
    Offline(DriveColors.danger, Icons.Rounded.CloudOff),
    FormError(DriveColors.danger, Icons.Rounded.Error),
}

/** Full-width strip with an accent bar, an icon, one line of text and an optional action. */
@Composable
fun StatusBanner(
    kind: BannerKind,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(kind.color))
        Icon(
            imageVector = kind.icon,
            contentDescription = null,
            tint = kind.color,
            modifier = Modifier.padding(start = 12.dp).size(20.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun StatusBannerPreview() {
    DriveMetricsTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBanner(BannerKind.Offline, "Not connected. Check the server address.", actionLabel = "Settings", onAction = {})
        }
    }
}
