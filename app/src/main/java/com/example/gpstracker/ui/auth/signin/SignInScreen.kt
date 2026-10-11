package com.example.gpstracker.ui.auth.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.auth.components.AuthScaffold
import com.example.gpstracker.ui.auth.components.AuthTextField
import com.example.gpstracker.ui.auth.components.FormErrorBanner
import com.example.gpstracker.ui.auth.components.PasswordField
import com.example.gpstracker.ui.auth.components.PrimaryButton
import com.example.gpstracker.ui.model.SignInUiState
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import kotlinx.coroutines.launch

/** Sign in form. Stateless apart from the one-off "reset not available" snackbar. */
@Composable
fun SignInScreen(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoToSignUp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resetMessage = stringResource(R.string.forgot_snackbar)

    AuthScaffold(
        modifier = modifier,
        title = stringResource(R.string.signin_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        state.formError?.let { FormErrorBanner(it) }
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Rounded.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            visible = state.passwordVisible,
            onToggleVisible = onTogglePassword,
            imeAction = ImeAction.Done,
            onImeAction = { if (state.canSubmit) onSubmit() },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = {
                    onForgotPassword()
                    scope.launch { snackbarHostState.showSnackbar(resetMessage) }
                },
            ) { Text(stringResource(R.string.signin_forgot)) }
        }
        PrimaryButton(
            text = stringResource(R.string.signin_submit),
            onClick = onSubmit,
            loading = state.isLoading,
            enabled = state.canSubmit || state.isLoading,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.signin_new_here),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onGoToSignUp) { Text(stringResource(R.string.signin_create)) }
        }
    }
}

private const val BG = 0xFF0F1417
private const val PHONE = "spec:width=411dp,height=891dp"

@Composable
private fun Preview(state: SignInUiState) {
    DriveMetricsTheme { SignInScreen(state, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Empty", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignInEmptyPreview() = Preview(PreviewStates.signInEmpty)

@Preview(name = "Filled", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignInFilledPreview() = Preview(PreviewStates.signInFilled)

@Preview(name = "Loading", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignInLoadingPreview() = Preview(PreviewStates.signInLoading)

@Preview(name = "Wrong password", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignInWrongPasswordPreview() = Preview(PreviewStates.signInWrongPassword)

@Preview(name = "Invalid email", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignInInvalidEmailPreview() = Preview(PreviewStates.signInInvalidEmail)
