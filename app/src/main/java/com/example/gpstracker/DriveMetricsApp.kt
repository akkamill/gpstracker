package com.example.gpstracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.crash.CrashAlertViewModel
import com.example.gpstracker.ui.model.SessionState
import com.example.gpstracker.ui.navigation.AuthGraph
import com.example.gpstracker.ui.navigation.DriveBottomBar
import com.example.gpstracker.ui.navigation.DriveMetricsNavHost
import com.example.gpstracker.ui.navigation.MainGraph
import com.example.gpstracker.ui.navigation.isTopLevel
import com.example.gpstracker.ui.navigation.navigateToTab
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/**
 * Root composable: theme, session check, scaffold with the bottom bar, the
 * navigation graph and the crash overlay.
 */
@Composable
fun DriveMetricsApp() {
    DriveMetricsTheme {
        val sessionViewModel: SessionViewModel = viewModel()
        val session by sessionViewModel.session.collectAsStateWithLifecycle()

        // The start graph is decided once, from the first session that is not Loading.
        var startGraph by remember { mutableStateOf<Any?>(null) }
        LaunchedEffect(session) {
            if (startGraph == null && session !is SessionState.Loading) {
                startGraph = when (session) {
                    is SessionState.SignedIn, SessionState.Guest -> MainGraph
                    else -> AuthGraph
                }
            }
        }

        val start = startGraph
        if (start == null) {
            Splash()
        } else {
            AppContent(startGraph = start)
        }
    }
}

@Composable
private fun Splash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun AppContent(startGraph: Any) {
    val navController = rememberNavController()
    val crashViewModel: CrashAlertViewModel = viewModel()
    val crash by crashViewModel.state.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (destination.isTopLevel()) {
                    DriveBottomBar(
                        current = destination,
                        onTabSelected = navController::navigateToTab,
                    )
                }
            },
        ) { innerPadding ->
            DriveMetricsNavHost(
                navController = navController,
                startGraph = startGraph,
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
