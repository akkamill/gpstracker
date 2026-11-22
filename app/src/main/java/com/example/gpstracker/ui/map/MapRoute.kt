package com.example.gpstracker.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpstracker.ui.drive.DriveViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Connects [MapScreen] to the same drive state as the dashboard. */
@Composable
fun MapRoute(viewModel: DriveViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MapScreen(state = state, onRecenter = {})
}
