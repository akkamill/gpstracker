package com.example.gpstracker.ui.auth.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.model.SessionState

@Composable
fun SignUpRoute(
    onSignedUp: () -> Unit,
    onGoToSignIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignUpViewModel = viewModel(),
    sessionViewModel: SessionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        if (session is SessionState.SignedIn) onSignedUp()
    }

    SignUpScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onTogglePassword = viewModel::onTogglePassword,
        onAcceptPrivacy = viewModel::onAcceptPrivacy,
        onSubmit = viewModel::onSubmit,
        onGoToSignIn = onGoToSignIn,
        onBack = onBack,
    )
}
