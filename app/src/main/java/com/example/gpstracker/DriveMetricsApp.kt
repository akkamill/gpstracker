package com.example.gpstracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gpstracker.ui.navigation.DriveBottomBar
import com.example.gpstracker.ui.navigation.DriveMetricsNavHost
import com.example.gpstracker.ui.navigation.TripDetailDestination
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Root composable: theme, scaffold, bottom bar and the navigation graph. */
@Composable
fun DriveMetricsApp() {
    DriveMetricsTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val showBottomBar = destination?.hasRoute(TripDetailDestination::class) != true

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    DriveBottomBar(
                        current = destination,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            DriveMetricsNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
