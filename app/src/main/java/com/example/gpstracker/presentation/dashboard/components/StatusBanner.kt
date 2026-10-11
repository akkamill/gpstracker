package com.example.gpstracker.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.presentation.dashboard.ConnectionStatus
import com.example.gpstracker.ui.theme.Caution
import com.example.gpstracker.ui.theme.Danger
import com.example.gpstracker.ui.theme.GpstrackerTheme
import com.example.gpstracker.ui.theme.Safe

/** Connection indicator shown at the top of the dashboard. */
@Composable
fun StatusBanner(status: ConnectionStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        ConnectionStatus.LIVE -> R.string.status_live to Safe
        ConnectionStatus.CONNECTING -> R.string.status_connecting to Caution
        ConnectionStatus.RECONNECTING -> R.string.status_reconnecting to Danger
        ConnectionStatus.OFFLINE -> R.string.status_offline to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1116)
@Composable
private fun StatusBannerPreview() {
    GpstrackerTheme { StatusBanner(ConnectionStatus.RECONNECTING) }
}
