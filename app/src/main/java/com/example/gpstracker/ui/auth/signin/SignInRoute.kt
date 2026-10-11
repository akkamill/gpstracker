package com.example.gpstracker.ui.auth.signin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.model.SessionState

@Composable
fun SignInRoute(
    onSignedIn: () -> Unit,
    onGoToSignUp: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignInViewModel = viewModel(),
    sessionViewModel: SessionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        if (session is SessionState.SignedIn) onSignedIn()
    }

    SignInScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::onTogglePassword,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = {},
        onGoToSignUp = onGoToSignUp,
        onBack = onBack,
    )
}
