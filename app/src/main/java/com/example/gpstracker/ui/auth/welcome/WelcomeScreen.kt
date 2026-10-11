package com.example.gpstracker.ui.auth.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.auth.components.PrimaryButton
import com.example.gpstracker.ui.components.SpeedLimitSign
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** First screen when signed out: the app mark, a one-line promise and three ways in. */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SpeedLimitSign(limitKmh = 50, size = 96.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.welcome_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.height(24.dp))
            PrimaryButton(text = stringResource(R.string.welcome_create), onClick = onCreateAccount)
            OutlinedButton(
                onClick = onSignIn,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(stringResource(R.string.welcome_sign_in), style = MaterialTheme.typography.titleMedium) }
            TextButton(onClick = onTryDemo) { Text(stringResource(R.string.welcome_try_demo)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417, device = "spec:width=411dp,height=891dp")
@Composable
private fun WelcomePreview() {
    DriveMetricsTheme { WelcomeScreen(onCreateAccount = {}, onSignIn = {}, onTryDemo = {}) }
}
