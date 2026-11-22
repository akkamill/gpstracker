package com.example.gpstracker.ui.auth.welcome

import androidx.compose.runtime.Composable
import com.example.gpstracker.ui.auth.SessionViewModel
import org.koin.compose.viewmodel.koinViewModel

/** "Try the demo" switches the session to guest and then continues into the app. */
@Composable
fun WelcomeRoute(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onGuestStarted: () -> Unit,
    sessionViewModel: SessionViewModel = koinViewModel(),
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
