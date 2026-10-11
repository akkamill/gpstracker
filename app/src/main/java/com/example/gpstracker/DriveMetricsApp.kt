package com.example.gpstracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.crash.CrashAlertViewModel
import com.example.gpstracker.ui.navigation.DriveBottomBar
import com.example.gpstracker.ui.navigation.DriveMetricsNavHost
import com.example.gpstracker.ui.navigation.TripDetailDestination
import com.example.gpstracker.ui.navigation.navigateToTab
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** Root composable: theme, scaffold, bottom bar, navigation graph and the crash overlay. */
@Composable
fun DriveMetricsApp() {
    DriveMetricsTheme {
        val navController = rememberNavController()
        val crashViewModel: CrashAlertViewModel = viewModel()
        val crash by crashViewModel.state.collectAsStateWithLifecycle()
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
                            onTabSelected = navController::navigateToTab,
                        )
                    }
                },
            ) { innerPadding ->
                DriveMetricsNavHost(
                    navController = navController,
                    onTriggerTestCrash = crashViewModel::trigger,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            CrashAlertOverlay(
                state = crash,
                onImOk = crashViewModel::onImOk,
                onSendNow = crashViewModel::onSendNow,
                contactName = crashViewModel.contactName,
            )
        }
    }
}
