package com.example.gpstracker.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.drive.DriveScreen

/**
 * All destinations. The screens are placeholders in this step and are
 * replaced one by one by the real screens.
 */
@Composable
fun DriveMetricsNavHost(
    navController: NavHostController,
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
        composable<MapDestination> { PlaceholderScreen("Map") }
        composable<TripsDestination> {
            PlaceholderScreen("Trips") {
                OutlinedButton(onClick = { navController.navigate(TripDetailDestination("trip-3")) }) {
                    Text("Open trip detail")
                }
            }
        }
        composable<SettingsDestination> { PlaceholderScreen("Settings") }
        composable<TripDetailDestination> {
            PlaceholderScreen("Trip detail") {
                OutlinedButton(onClick = { navController.popBackStack() }) { Text("Back") }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, content: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}
