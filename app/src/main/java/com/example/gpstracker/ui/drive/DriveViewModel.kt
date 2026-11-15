package com.example.gpstracker.ui.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.data.reports.ReportUiMapper
import com.example.gpstracker.data.reports.RoadReportService
import com.example.gpstracker.ui.model.DriveUiState
import com.example.gpstracker.ui.model.ReportKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Bridge from the drive data to the Drive and Map screens. Read-only: there is
 * nothing to start or stop, because trips are detected from the car's data.
 */
class DriveViewModel(
    repository: DriveRepository = AppContainer.drive,
    private val reports: RoadReportService = AppContainer.reports,
    private val mapper: ReportUiMapper = ReportUiMapper(),
) : ViewModel() {
    val state: StateFlow<DriveUiState> = combine(repository.drive, reports.state, mapper::apply)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), repository.drive.value)

    /** One tap: reports [kind] where the car is now. */
    fun onReport(kind: ReportKind) {
        reports.report(mapper.type(kind))
    }

    fun onAnswerStillThere(id: String, stillThere: Boolean) = reports.answer(id, stillThere)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
