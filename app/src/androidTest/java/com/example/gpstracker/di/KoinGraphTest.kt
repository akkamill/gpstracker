package com.example.gpstracker.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.data.reports.RoadReportService
import com.example.gpstracker.data.settings.SettingsRepository
import com.example.gpstracker.domain.trip.TripHistory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** The app starts Koin itself; every shared object it wires up can be created, and only once. */
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {
    private val koin = GlobalContext.get()

    @Test
    fun everyRepositoryCanBeCreated() {
        assertNotNull(koin.get<SettingsRepository>())
        assertNotNull(koin.get<AuthRepository>())
        assertNotNull(koin.get<TripHistory>())
        assertNotNull(koin.get<RoadReportService>())
        assertNotNull(koin.get<DriveRepository>())
    }

    @Test
    fun repositoriesAreShared() {
        assertSame(koin.get<DriveRepository>(), koin.get<DriveRepository>())
        assertSame(koin.get<SettingsRepository>(), koin.get<SettingsRepository>())
    }
}
