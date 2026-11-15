package com.example.gpstracker.ui.tripdetail

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.model.TripDetailUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripDetailViewModel(
    tripId: String,
    repository: DriveRepository = AppContainer.drive,
) : ViewModel() {
    /** Falls back to a sample trip if the id is unknown, so the screen never crashes. */
    val detail: TripDetailUi = repository.tripDetail(tripId) ?: FakeData.tripWithEvents

    private val _selectedEventId = MutableStateFlow<String?>(null)
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()

    fun onEventClick(id: String) {
        _selectedEventId.value = id
    }
}
