package com.example.gpstracker.data.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gpstracker.testing.FakePreferencesStore
import com.example.gpstracker.ui.model.FuelType
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.model.VehicleProfileUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStorageTest {
    private val changed = SettingsUiState(
        speedTolerance = 10,
        tripEndAfterMinutes = 2,
        crashDetectionEnabled = false,
        emergencyContactName = "Mum",
        vehicle = VehicleProfileUi(FuelType.Diesel, "5.1", "1.62", "1500"),
    )

    @Test
    fun `every setting is written and read back`() {
        val prefs = mutablePreferencesOf()
        SettingsPreferences.write(prefs, changed)
        assertEquals(changed, SettingsPreferences.read(prefs))
    }

    @Test
    fun `an empty file or an unknown value gives the defaults`() {
        assertEquals(SettingsUiState(), SettingsPreferences.read(mutablePreferencesOf()))
        val prefs = mutablePreferencesOf(stringPreferencesKey("vehicle_fuel_type") to "Electric")
        assertEquals(FuelType.Petrol, SettingsPreferences.read(prefs).vehicle.fuelType)
    }

    @Test
    fun `settings are saved and come back after a restart`() = runTest {
        val file = FakePreferencesStore()
        val first = DataStoreSettingsRepository(file, backgroundScope)
        testScheduler.runCurrent()
        first.update { changed }
        testScheduler.runCurrent()

        val second = DataStoreSettingsRepository(file, backgroundScope)
        testScheduler.runCurrent()
        assertEquals(changed, second.settings.value)
    }

    @Test
    fun `a change made before the file is read is not lost`() = runTest {
        val file = FakePreferencesStore()
        file.state.value = mutablePreferencesOf().also { SettingsPreferences.write(it, changed) }
        file.holdReads()

        val repo = DataStoreSettingsRepository(file, backgroundScope)
        testScheduler.runCurrent()
        repo.update { it.copy(speedTolerance = 0) } // the user is quicker than the disk
        assertEquals(0, repo.settings.value.speedTolerance)

        file.releaseReads()
        testScheduler.runCurrent()
        // saved settings are loaded, with the early change on top, and written back
        assertEquals(changed.copy(speedTolerance = 0), repo.settings.value)
        assertEquals(changed.copy(speedTolerance = 0), SettingsPreferences.read(file.state.value))
    }
}
