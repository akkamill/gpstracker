package com.example.gpstracker.ui.tripdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TripDetailRoute(
    tripId: String,
    onBack: () -> Unit,
    onSetFuelPrice: () -> Unit,
    viewModel: TripDetailViewModel = koinViewModel(key = tripId) { parametersOf(tripId) },
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
