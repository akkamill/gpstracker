package com.example.gpstracker.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.VehicleProfileUi

/**
 * How settings are written to the preferences file. A missing or unreadable value falls back
 * to the default, so an older file or a renamed option never breaks the app.
 */
object SettingsPreferences {
    private val speedTolerance = intPreferencesKey("speed_tolerance_kmh")
    private val tripEndAfter = intPreferencesKey("trip_end_after_min")
    private val crashDetection = booleanPreferencesKey("crash_detection")
    private val emergencyContact = stringPreferencesKey("emergency_contact_name")
    private val fuelType = stringPreferencesKey("vehicle_fuel_type")
    private val consumption = stringPreferencesKey("vehicle_consumption_l100")
    private val fuelPrice = stringPreferencesKey("vehicle_fuel_price")
    private val monthlyKm = stringPreferencesKey("vehicle_monthly_km")

    fun read(prefs: Preferences, defaults: SettingsUiState = SettingsUiState()): SettingsUiState = defaults.copy(
        speedTolerance = prefs[speedTolerance] ?: defaults.speedTolerance,
        tripEndAfterMinutes = prefs[tripEndAfter] ?: defaults.tripEndAfterMinutes,
        crashDetectionEnabled = prefs[crashDetection] ?: defaults.crashDetectionEnabled,
        emergencyContactName = prefs[emergencyContact] ?: defaults.emergencyContactName,
        vehicle = VehicleProfileUi(
            fuelType = prefs[fuelType].toEnum(defaults.vehicle.fuelType),
            consumptionL100 = prefs[consumption] ?: defaults.vehicle.consumptionL100,
            fuelPricePerL = prefs[fuelPrice] ?: defaults.vehicle.fuelPricePerL,
            monthlyKm = prefs[monthlyKm] ?: defaults.vehicle.monthlyKm,
        ),
    )

    fun write(prefs: MutablePreferences, settings: SettingsUiState) {
        prefs[speedTolerance] = settings.speedTolerance
        prefs[tripEndAfter] = settings.tripEndAfterMinutes
        prefs[crashDetection] = settings.crashDetectionEnabled
        prefs[emergencyContact] = settings.emergencyContactName
        prefs[fuelType] = settings.vehicle.fuelType.name
        prefs[consumption] = settings.vehicle.consumptionL100
        prefs[fuelPrice] = settings.vehicle.fuelPricePerL
        prefs[monthlyKm] = settings.vehicle.monthlyKm
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        enumValues<E>().firstOrNull { it.name == this } ?: default
}
