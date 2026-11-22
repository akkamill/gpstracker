package com.example.gpstracker.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.data.auth.DataStoreSessionStore
import com.example.gpstracker.data.auth.LocalAuthRepository
import com.example.gpstracker.data.auth.SessionStore
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.data.drive.TelemetryDriveRepository
import com.example.gpstracker.data.fake.DemoHistory
import com.example.gpstracker.data.local.DriveMetricsDatabase
import com.example.gpstracker.data.local.RoomTripHistory
import com.example.gpstracker.data.reports.ReportRepository
import com.example.gpstracker.data.reports.RoadReportService
import com.example.gpstracker.data.reports.StoredReportRepository
import com.example.gpstracker.data.settings.DataStoreSettingsRepository
import com.example.gpstracker.data.settings.SettingsRepository
import com.example.gpstracker.data.telematics.DemoControls
import com.example.gpstracker.data.telematics.SimulatedTelemetrySource
import com.example.gpstracker.data.telematics.SimulatorMapLookup
import com.example.gpstracker.domain.telemetry.TelemetrySource
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.simulator.vehicle.CommunityReports
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.auth.signin.SignInViewModel
import com.example.gpstracker.ui.auth.signup.SignUpViewModel
import com.example.gpstracker.ui.auth.vehicle.VehicleSetupViewModel
import com.example.gpstracker.ui.crash.CrashAlertViewModel
import com.example.gpstracker.ui.drive.DriveViewModel
import com.example.gpstracker.ui.insights.InsightsViewModel
import com.example.gpstracker.ui.settings.SettingsViewModel
import com.example.gpstracker.ui.tripdetail.TripDetailViewModel
import com.example.gpstracker.ui.trips.TripsViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/*
 * Who creates what. Screens never see these modules; each ViewModel gets what it needs
 * through its constructor, so tests can hand in fakes instead.
 *
 * Data path: simulated car + tracker (:simulator) â†’ AVL records â†’ TelemetryPoint
 * â†’ DriveTracker (:domain) â†’ DriveRepository â†’ ViewModels â†’ screens.
 */

/** The scope of work that lives as long as the app: the drive pipeline and background writes. */
val AppScope = named("appScope")

private val Context.settingsStore by preferencesDataStore(name = "settings")
private val Context.sessionStore by preferencesDataStore(name = "session")

/** Where data is kept on the phone. */
val storageModule = module {
    single { DriveMetricsDatabase.create(androidContext()) }
    single { get<DriveMetricsDatabase>().trips() }
    single { get<DriveMetricsDatabase>().reports() }
    single { get<DriveMetricsDatabase>().accounts() }
    single<TripHistory> { RoomTripHistory(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext().settingsStore, get(AppScope)) }
    single<SessionStore> { DataStoreSessionStore(androidContext().sessionStore) }
}

/** Accounts, the car's data and everything computed from it. */
val dataModule = module {
    single(AppScope) {
        val tracer = Tracing.tracer("app")
        // a failed background job (a database write, say) is logged instead of closing the app
        val onError = CoroutineExceptionHandler { _, error -> tracer.error("background work failed", error) }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + onError)
    }
    single<AuthRepository> { LocalAuthRepository(get(), get(), get(AppScope)) }

    single { SimulatedTelemetrySource(SimulatedCar(startMs = System.currentTimeMillis())) }
    single<TelemetrySource> { get<SimulatedTelemetrySource>() }
    single<DemoControls> { get<SimulatedTelemetrySource>() }

    // seeded with reports from "other drivers" until the server shares real ones
    single<ReportRepository> { StoredReportRepository(get(), get(AppScope), demo = CommunityReports.demo(System.currentTimeMillis())) }
    single { RoadReportService(get()) }

    single<DriveRepository> {
        TelemetryDriveRepository(
            source = get(),
            tracker = DriveTracker(map = SimulatorMapLookup()),
            settings = get<SettingsRepository>().settings,
            history = get(),
            demoHistory = DemoHistory::build,
            reports = get(),
        ).also { it.start(get(AppScope)) }
    }
}

val viewModelModule = module {
    viewModel { SessionViewModel(get()) }
    viewModel { SignInViewModel(androidApplication(), get()) }
    viewModel { SignUpViewModel(androidApplication(), get()) }
    viewModel { VehicleSetupViewModel(androidApplication(), get()) }
    viewModel { DriveViewModel(get(), get()) }
    viewModel { TripsViewModel(get()) }
    viewModel { params -> TripDetailViewModel(params.get(), get()) }
    viewModel { InsightsViewModel(get(), get<SettingsRepository>().settings) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { CrashAlertViewModel(get<SettingsRepository>().settings) }
}

val appModules = listOf(storageModule, dataModule, viewModelModule)
