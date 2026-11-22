package com.example.gpstracker.ui.tripdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.ui.model.TripDetailUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TripDetailViewModel(
    tripId: String,
    repository: DriveRepository,
) : ViewModel() {
    /**
     * The trip, or null while it is not (or no longer) in the history, for example right after
     * the app was restarted on this screen and the history is still loading.
     */
    val detail: StateFlow<TripDetailUi?> = repository.trips
        .map { repository.tripDetail(tripId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.tripDetail(tripId))

    private val _selectedEventId = MutableStateFlow<String?>(null)
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()

    fun onEventClick(id: String) {
        _selectedEventId.value = id
    }
}
