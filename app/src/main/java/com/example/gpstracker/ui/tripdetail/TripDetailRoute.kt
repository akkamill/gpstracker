package com.example.gpstracker.ui.tripdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TripDetailRoute(
    tripId: String,
    onBack: () -> Unit,
    onSetFuelPrice: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(key = tripId) { TripDetailViewModel(tripId) },
) {
    val selected by viewModel.selectedEventId.collectAsStateWithLifecycle()
    TripDetailScreen(
        state = viewModel.detail,
        selectedEventId = selected,
        onEventClick = viewModel::onEventClick,
        onSetFuelPrice = onSetFuelPrice,
        onBack = onBack,
    )
}
