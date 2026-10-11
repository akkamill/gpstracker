package com.example.gpstracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.navigation.DriveBottomBar
import com.example.gpstracker.ui.navigation.DriveMetricsNavHost
import com.example.gpstracker.ui.navigation.TripDetailDestination
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Root composable: theme, scaffold, bottom bar, navigation graph and the crash overlay. */
@Composable
fun DriveMetricsApp() {
    DriveMetricsTheme {
        val navController = rememberNavController()
        var crash by remember { mutableStateOf(CrashAlertUiState()) }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val showBottomBar = destination?.hasRoute(TripDetailDestination::class) != true

        Box(Modifier.fillMaxSize()) {
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
                    onTriggerTestCrash = { crash = CrashAlertUiState(visible = true, secondsLeft = 10) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            CrashAlertOverlay(
                state = crash,
                onImOk = { crash = CrashAlertUiState() },
                onSendNow = { crash = crash.copy(sent = true) },
            )
        }
    }
}
