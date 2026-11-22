package com.example.gpstracker.ui.drive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** Connects [DriveScreen] to its ViewModel. The screen itself never sees the ViewModel. */
@Composable
fun DriveRoute(
    onOpenTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DriveViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DriveScreen(
        state = state,
        onOpenTrip = onOpenTrip,
        onOpenSettings = onOpenSettings,
        onReport = viewModel::onReport,
        onAnswerStillThere = viewModel::onAnswerStillThere,
    )
}
