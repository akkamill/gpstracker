package com.example.gpstracker.ui.trips

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.fake.FakeDriveSimulator
import com.example.gpstracker.ui.model.TripsUiState
import kotlinx.coroutines.flow.StateFlow

class TripsViewModel(
    simulator: FakeDriveSimulator = AppContainer.simulator,
) : ViewModel() {
    val state: StateFlow<TripsUiState> = simulator.trips
}
