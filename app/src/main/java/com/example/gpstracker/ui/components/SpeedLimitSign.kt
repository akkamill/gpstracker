package com.example.gpstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.BarlowCondensed
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.SignRed

private const val RING_FRACTION = 0.12f
private const val NUMBER_FRACTION = 0.40f

/** European-style speed limit sign: white disc, red ring, black number. */
@Composable
fun SpeedLimitSign(limitKmh: Int?, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val description = if (limitKmh == null) {
        stringResource(R.string.sign_unknown_cd)
    } else {
        stringResource(R.string.sign_limit_cd, limitKmh)
    }
    val ringColor = if (limitKmh == null) Concrete else SignRed
    val numberSize = with(LocalDensity.current) { (size * NUMBER_FRACTION).toSp() }

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val ring = this.size.minDimension * RING_FRACTION
            drawCircle(Color.White)
            drawCircle(
                color = ringColor,
                radius = (this.size.minDimension - ring) / 2,
                style = Stroke(width = ring),
            )
        }
        Text(
            text = limitKmh?.toString() ?: stringResource(R.string.sign_unknown_text),
            color = Color.Black,
            style = TextStyle(
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = numberSize,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SpeedLimitSignPreview() {
    DriveMetricsTheme {
        Box(Modifier.size(width = 200.dp, height = 80.dp)) {
            SpeedLimitSign(limitKmh = 50)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun SpeedLimitSignUnknownPreview() {
    DriveMetricsTheme { SpeedLimitSign(limitKmh = null) }
}
