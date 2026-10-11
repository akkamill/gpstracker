package com.example.gpstracker.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.estimate.InsightsCalculator
import com.example.gpstracker.data.estimate.toNumbers
import com.example.gpstracker.data.fake.FakeDriveSimulator
import com.example.gpstracker.ui.model.InsightsPeriod
import com.example.gpstracker.ui.model.InsightsUiState
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Recomputes the insights whenever a trip finishes, the period changes or the
 * vehicle settings change, so a finished demo trip shows up here at once.
 */
class InsightsViewModel(
    simulator: FakeDriveSimulator = AppContainer.simulator,
    settings: StateFlow<SettingsUiState> = AppContainer.settings,
    private val calculator: InsightsCalculator = InsightsCalculator(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val period = MutableStateFlow(InsightsPeriod.Week)

    val state: StateFlow<InsightsUiState> = combine(simulator.records, settings, period) { records, s, p ->
        calculator.compute(records, clock(), p, s.vehicle.toNumbers())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), InsightsUiState())

    fun onPeriodChange(value: InsightsPeriod) = period.update { value }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
