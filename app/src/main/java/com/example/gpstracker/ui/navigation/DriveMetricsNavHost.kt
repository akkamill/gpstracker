package com.example.gpstracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.gpstracker.ui.drive.DriveRoute
import com.example.gpstracker.ui.map.MapRoute
import com.example.gpstracker.ui.settings.SettingsRoute
import com.example.gpstracker.ui.tripdetail.TripDetailRoute
import com.example.gpstracker.ui.trips.TripsRoute

/** The navigation graph: four tabs plus the pushed trip detail. */
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
            DriveRoute(onOpenSettings = { navController.navigateToTab(TopLevelDestination.Settings) })
        }
        composable<MapDestination> { MapRoute() }
        composable<TripsDestination> {
            TripsRoute(
                onTripClick = { navController.navigate(TripDetailDestination(it)) },
                onGoToDrive = { navController.navigateToTab(TopLevelDestination.Drive) },
            )
        }
        composable<SettingsDestination> {
            SettingsRoute(onTriggerTestCrash = onTriggerTestCrash)
        }
        composable<TripDetailDestination> { entry ->
            TripDetailRoute(
                tripId = entry.toRoute<TripDetailDestination>().tripId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
