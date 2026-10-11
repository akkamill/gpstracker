package com.example.gpstracker.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TripsRoute(
    onTripClick: (String) -> Unit,
    onGoToDrive: () -> Unit,
    viewModel: TripsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TripsScreen(state = state, onTripClick = onTripClick, onGoToDrive = onGoToDrive)
}
