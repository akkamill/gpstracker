package com.example.gpstracker.ui.auth.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.gpstracker.ui.components.BannerKind
import com.example.gpstracker.ui.components.StatusBanner
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Red banner for form-level errors; announced assertively to TalkBack. */
@Composable
fun FormErrorBanner(message: String, modifier: Modifier = Modifier) {
    StatusBanner(
        kind = BannerKind.FormError,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun FormErrorBannerPreview() {
    DriveMetricsTheme { FormErrorBanner("Email or password is incorrect.") }
}
