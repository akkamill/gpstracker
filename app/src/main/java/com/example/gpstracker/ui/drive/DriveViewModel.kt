package com.example.gpstracker.ui.drive

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.fake.FakeDriveSimulator
import com.example.gpstracker.ui.model.DriveUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge from the simulator to the Drive and Map screens. Read-only: there is
 * nothing to start or stop, because trips are detected from the car's data.
 */
class DriveViewModel(
    simulator: FakeDriveSimulator = AppContainer.simulator,
) : ViewModel() {
    val state: StateFlow<DriveUiState> = simulator.state
}
