package com.example.gpstracker.ui.drive

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.fake.FakeDriveSimulator
import com.example.gpstracker.ui.model.DriveUiState
import kotlinx.coroutines.flow.StateFlow

/** Thin bridge from the simulator to the Drive and Map screens. */
class DriveViewModel(
    private val simulator: FakeDriveSimulator = AppContainer.simulator,
) : ViewModel() {
    val state: StateFlow<DriveUiState> = simulator.state

    fun startTrip() = simulator.startTrip()

    fun stopTrip() = simulator.stopTrip()
}
