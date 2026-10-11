package com.example.gpstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/**
 * Round account button: initials for a signed-in user, a person icon for a guest.
 * The visible circle is 36 dp; the touch target is 48 dp.
 */
@Composable
fun AccountAvatar(initials: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.avatar_cd)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (initials.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun AccountAvatarPreview() {
    DriveMetricsTheme {
        androidx.compose.foundation.layout.Row {
            AccountAvatar(initials = "KA", onClick = {})
            AccountAvatar(initials = null, onClick = {})
        }
    }
}
