package com.example.gpstracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.drive.DriveScreen
import com.example.gpstracker.ui.map.MapScreen
import com.example.gpstracker.ui.model.SettingsUiState
import com.example.gpstracker.ui.settings.SettingsScreen
import com.example.gpstracker.ui.tripdetail.TripDetailScreen
import com.example.gpstracker.ui.trips.TripsScreen

/**
 * All destinations. Screens still receive canned preview states here; the
 * simulator-backed routes replace them in a later step.
 */
@Composable
fun DriveMetricsNavHost(
    navController: NavHostController,
    onTriggerTestCrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DriveDestination,
        modifier = modifier,
    ) {
        composable<DriveDestination> {
            DriveScreen(
                state = PreviewStates.driveActive,
                onStartTrip = {},
                onStopTrip = {},
                onOpenSettings = { navController.navigate(SettingsDestination) },
            )
        }
        composable<MapDestination> { MapScreen(PreviewStates.driveActive, onRecenter = {}) }
        composable<TripsDestination> {
            TripsScreen(
                state = PreviewStates.tripsPopulated,
                onTripClick = { navController.navigate(TripDetailDestination(it)) },
                onGoToDrive = { navController.navigate(DriveDestination) },
            )
        }
        composable<SettingsDestination> {
            var settings by remember { mutableStateOf(SettingsUiState()) }
            SettingsScreen(
                state = settings,
                onDataSourceChange = { settings = settings.copy(dataSource = it) },
                onServerUrlChange = { settings = settings.copy(serverUrl = it) },
                onToleranceChange = { settings = settings.copy(speedTolerance = it) },
                onCrashDetectionChange = { settings = settings.copy(crashDetectionEnabled = it) },
                onEmergencyContactChange = { settings = settings.copy(emergencyContactName = it) },
                onTriggerTestCrash = onTriggerTestCrash,
            )
        }
        composable<TripDetailDestination> { entry ->
            val tripId = entry.toRoute<TripDetailDestination>().tripId
            var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }
            TripDetailScreen(
                state = FakeData.tripDetails[tripId] ?: FakeData.tripWithEvents,
                selectedEventId = selectedEventId,
                onEventClick = { selectedEventId = it },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
