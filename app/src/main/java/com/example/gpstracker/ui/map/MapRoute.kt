package com.example.gpstracker.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpstracker.ui.drive.DriveViewModel

/** Connects [MapScreen] to the same drive state as the dashboard. */
@Composable
fun MapRoute(viewModel: DriveViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MapScreen(state = state, onRecenter = {})
}
