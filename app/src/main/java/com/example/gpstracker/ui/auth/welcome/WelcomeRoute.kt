package com.example.gpstracker.ui.auth.welcome

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.ui.auth.SessionViewModel

/** "Try the demo" switches the session to guest and then continues into the app. */
@Composable
fun WelcomeRoute(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onGuestStarted: () -> Unit,
    sessionViewModel: SessionViewModel = viewModel(),
) {
    WelcomeScreen(
        onCreateAccount = onCreateAccount,
        onSignIn = onSignIn,
        onTryDemo = {
            sessionViewModel.continueAsGuest()
            onGuestStarted()
        },
    )
}
