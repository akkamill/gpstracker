package com.example.gpstracker.ui.auth.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/**
 * Outlined text field with an error state that does not rely on color alone:
 * the error text appears below the field and an error icon replaces the trailing slot.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    suffix: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        isError = error != null,
        shape = MaterialTheme.shapes.small,
        prefix = prefix?.let { text -> { Text(text) } },
        suffix = suffix?.let { text -> { Text(text) } },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = when {
            error != null -> {
                { Icon(Icons.Rounded.Error, contentDescription = stringResource(R.string.error_cd)) }
            }
            else -> trailing
        },
        supportingText = (error ?: supportingText)?.let { text -> { Text(text) } },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun AuthTextFieldPreview() {
    DriveMetricsTheme {
        AuthTextField(value = "kamila@", onValueChange = {}, label = "Email", error = "Enter a valid email address.")
    }
}
