package com.example.gpstracker.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.gpstracker.BuildConfig
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.data.auth.DataStoreSessionStore
import com.example.gpstracker.data.auth.RemoteAuthRepository
import com.example.gpstracker.data.auth.SessionStore
import com.example.gpstracker.data.drive.DriveRepository
import com.example.gpstracker.data.drive.TelemetryDriveRepository
import com.example.gpstracker.data.local.DriveMetricsDatabase
import com.example.gpstracker.data.local.RoomTripHistory
import com.example.gpstracker.data.remote.ApiClient
import com.example.gpstracker.data.reports.ReportRepository
import com.example.gpstracker.data.reports.RoadReportService
import com.example.gpstracker.data.reports.SharedReportRepository
import com.example.gpstracker.data.reports.StoredReportRepository
import com.example.gpstracker.data.settings.DataStoreSettingsRepository
import com.example.gpstracker.data.settings.SettingsRepository
import com.example.gpstracker.data.support.SupportChatRepository
import com.example.gpstracker.data.support.WebSocketChatTransport
import com.example.gpstracker.data.telematics.DemoControls
import com.example.gpstracker.data.telematics.SimulatedTelemetrySource
import com.example.gpstracker.data.telematics.SimulatorMapLookup
import com.example.gpstracker.data.trips.TripSync
import com.example.gpstracker.domain.telemetry.TelemetrySource
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.domain.trip.TripHistory
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.simulator.history.DemoHistory
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
import com.example.gpstracker.ui.support.SupportChatViewModel
import com.example.gpstracker.ui.tripdetail.TripDetailViewModel
import com.example.gpstracker.ui.trips.TripsViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    single { RoomTripHistory(get()) }
    single<TripHistory> { get<RoomTripHistory>() }
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
    single { ApiClient.httpClient() }
    single { ApiClient(get(), BuildConfig.SERVER_URL) }
    single { RemoteAuthRepository(get(), get(), get(AppScope)) }
    single<AuthRepository> { get<RemoteAuthRepository>() }
    // started with the app, so a signed-in driver's trips sync without waiting for a new one
    single(createdAtStart = true) {
        val auth = get<RemoteAuthRepository>()
        TripSync(get(), get<RoomTripHistory>(), auth.accountId, auth, get(AppScope)).also { it.start() }
    }
    single { SupportChatRepository(WebSocketChatTransport(get(), BuildConfig.SERVER_URL), get<RemoteAuthRepository>(), get(AppScope)) }

    single { SimulatedTelemetrySource(SimulatedCar(startMs = System.currentTimeMillis())) }
    single<TelemetrySource> { get<SimulatedTelemetrySource>() }
    single<DemoControls> { get<SimulatedTelemetrySource>() }

    // Shared through the server. The simulated "other drivers" reports are there from the start,
    // so the demo also works without the server; the server sends the same ones (same ids).
    single<ReportRepository> {
        val koin = getKoin()
        SharedReportRepository(
            local = StoredReportRepository(get(), get(AppScope), demo = CommunityReports.demo(System.currentTimeMillis())),
            api = get(),
            tokens = get<RemoteAuthRepository>(),
            scope = get(AppScope),
            // read when syncing; the drive repository is created after this one
            position = { koin.get<DriveRepository>().drive.value.position?.let { LatLon(it.lat, it.lon) } },
        ).also { it.start() }
    }
    single { RoadReportService(get()) }

    single<DriveRepository> {
        TelemetryDriveRepository(
            source = get(),
            tracker = DriveTracker(map = SimulatorMapLookup()),
            settings = get<SettingsRepository>().settings,
            history = get(),
            demoHistory = DemoHistory::build,
            reports = get(),
            // each account's trips; demo mode has its own
            owner = get<RemoteAuthRepository>().accountId
                .map { it ?: TripHistory.GUEST }
                .stateIn(get(AppScope), SharingStarted.Eagerly, TripHistory.GUEST),
            onTripSaved = { get<TripSync>().requestSync() },
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
    viewModel { SupportChatViewModel(get()) }
}

val appModules = listOf(storageModule, dataModule, viewModelModule)
