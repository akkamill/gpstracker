package com.example.gpstracker.ui.trips

import androidx.lifecycle.ViewModel
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.ui.model.TripsUiState
import kotlinx.coroutines.flow.StateFlow

class TripsViewModel(
    repository: DriveRepository,
) : ViewModel() {
    val state: StateFlow<TripsUiState> = repository.trips
}
