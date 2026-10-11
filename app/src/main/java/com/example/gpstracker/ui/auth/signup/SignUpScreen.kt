package com.example.gpstracker.ui.auth.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.auth.components.AuthScaffold
import com.example.gpstracker.ui.auth.components.AuthTextField
import com.example.gpstracker.ui.auth.components.FormErrorBanner
import com.example.gpstracker.ui.auth.components.PasswordField
import com.example.gpstracker.ui.auth.components.PrimaryButton
import com.example.gpstracker.ui.components.InfoBottomSheet
import com.example.gpstracker.ui.model.SignUpUiState
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Create-account form with validation messages, privacy consent and a loading state. */
@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onAcceptPrivacy: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoToSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPrivacy by remember { mutableStateOf(false) }

    AuthScaffold(
        modifier = modifier,
        title = stringResource(R.string.signup_title),
        onBack = onBack,
    ) {
        state.formError?.let { FormErrorBanner(it) }
        AuthTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_name),
            error = state.nameError,
            leadingIcon = Icons.Rounded.Person,
        )
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
            error = state.passwordError,
            supportingText = stringResource(R.string.password_hint),
        )
        PasswordField(
            value = state.confirmPassword,
            onValueChange = onConfirmChange,
            label = stringResource(R.string.field_confirm),
            visible = state.passwordVisible,
            onToggleVisible = onTogglePassword,
            error = state.confirmError,
            imeAction = ImeAction.Done,
            onImeAction = { if (state.canSubmit) onSubmit() },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = state.acceptedPrivacy,
                    role = Role.Checkbox,
                    onValueChange = onAcceptPrivacy,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = state.acceptedPrivacy, onCheckedChange = null)
            Text(
                text = stringResource(R.string.signup_privacy_checkbox),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showPrivacy = true }) { Text(stringResource(R.string.signup_privacy_link)) }
        }
        PrimaryButton(
            text = stringResource(R.string.signup_submit),
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
                text = stringResource(R.string.signup_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onGoToSignIn) { Text(stringResource(R.string.signup_sign_in)) }
        }
    }

    if (showPrivacy) {
        InfoBottomSheet(
            title = stringResource(R.string.privacy_title),
            text = stringResource(R.string.privacy_body),
            onDismiss = { showPrivacy = false },
        )
    }
}

private const val BG = 0xFF0F1417
private const val PHONE = "spec:width=411dp,height=1000dp"

@Composable
private fun Preview(state: SignUpUiState) {
    DriveMetricsTheme { SignUpScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Empty", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignUpEmptyPreview() = Preview(PreviewStates.signUpEmpty)

@Preview(name = "Field errors", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignUpErrorsPreview() = Preview(PreviewStates.signUpErrors)

@Preview(name = "Email exists", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignUpEmailExistsPreview() = Preview(PreviewStates.signUpEmailExists)

@Preview(name = "Loading", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun SignUpLoadingPreview() = Preview(PreviewStates.signUpLoading)
