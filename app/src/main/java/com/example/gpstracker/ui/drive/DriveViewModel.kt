package com.example.gpstracker.ui.drive

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.ui.model.DriveUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge from the drive data to the Drive and Map screens. Read-only: there is
 * nothing to start or stop, because trips are detected from the car's data.
 */
class DriveViewModel(
    repository: DriveRepository = AppContainer.drive,
) : ViewModel() {
    val state: StateFlow<DriveUiState> = repository.drive
}
