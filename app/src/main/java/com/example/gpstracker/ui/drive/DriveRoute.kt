package com.example.gpstracker.ui.drive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Connects [DriveScreen] to its ViewModel. The screen itself never sees the ViewModel. */
@Composable
fun DriveRoute(
    onOpenTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DriveViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DriveScreen(
        state = state,
        onOpenTrip = onOpenTrip,
        onOpenSettings = onOpenSettings,
    )
}
