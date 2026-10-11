package com.example.gpstracker.ui.insights

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.auth.initialsOf
import com.example.gpstracker.ui.model.SessionState

@Composable
fun InsightsRoute(
    onOpenSettings: () -> Unit,
    onSetFuelPrice: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: InsightsViewModel = viewModel(),
    sessionViewModel: SessionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    InsightsScreen(
        state = state,
        isGuest = session is SessionState.Guest,
        onPeriodChange = viewModel::onPeriodChange,
        onOpenSettings = onOpenSettings,
        onSetFuelPrice = onSetFuelPrice,
        onCreateAccount = onCreateAccount,
        onAboutBenchmark = {},
        avatarInitials = (session as? SessionState.SignedIn)?.let { initialsOf(it.name) },
    )
}
