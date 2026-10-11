package com.example.gpstracker.ui.auth.vehicle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.ui.auth.AuthMessages
import com.example.gpstracker.ui.auth.VehicleValidator
import com.example.gpstracker.ui.model.FuelType
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.VehicleErrorsUi
import com.example.gpstracker.ui.model.VehicleProfileUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VehicleSetupViewModel @JvmOverloads constructor(
    application: Application,
    private val store: MutableStateFlow<SettingsUiState> = AppContainer.settings,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(store.value.vehicle)
    val state: StateFlow<VehicleProfileUi> = _state.asStateFlow()

    private val _errors = MutableStateFlow(VehicleErrorsUi())
    val errors: StateFlow<VehicleErrorsUi> = _errors.asStateFlow()

    fun onFuelTypeChange(value: FuelType) = _state.update { it.copy(fuelType = value) }

    fun onConsumptionChange(value: String) {
        _state.update { it.copy(consumptionL100 = value) }
        _errors.update { it.copy(consumption = null) }
    }

    fun onPriceChange(value: String) {
        _state.update { it.copy(fuelPricePerL = value) }
        _errors.update { it.copy(price = null) }
    }

    fun onMonthlyKmChange(value: String) {
        _state.update { it.copy(monthlyKm = value) }
        _errors.update { it.copy(monthlyKm = null) }
    }

    /** Validates and stores the profile. Returns true when it was saved. */
    fun save(): Boolean {
        val context = getApplication<Application>()
        val profile = _state.value
        val errors = VehicleErrorsUi(
            consumption = VehicleValidator.validateConsumption(profile.consumptionL100)?.let { AuthMessages.field(context, it) },
            price = VehicleValidator.validatePrice(profile.fuelPricePerL)?.let { AuthMessages.field(context, it) },
            monthlyKm = VehicleValidator.validateMonthlyKm(profile.monthlyKm)?.let { AuthMessages.field(context, it) },
        )
        _errors.value = errors
        if (errors.consumption != null || errors.price != null || errors.monthlyKm != null) return false
        store.update { it.copy(vehicle = profile) }
        return true
    }
}
