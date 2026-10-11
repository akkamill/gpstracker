package com.example.gpstracker.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.auth.initialsOf
import com.example.gpstracker.ui.model.SessionState

@Composable
fun TripsRoute(
    onTripClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateAccount: () -> Unit,
    onGoToDrive: () -> Unit,
    viewModel: TripsViewModel = viewModel(),
    sessionViewModel: SessionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    TripsScreen(
        state = state,
        isGuest = session is SessionState.Guest,
        onTripClick = onTripClick,
        onOpenSettings = onOpenSettings,
        onCreateAccount = onCreateAccount,
        onGoToDrive = onGoToDrive,
        avatarInitials = (session as? SessionState.SignedIn)?.let { initialsOf(it.name) },
    )
}
