package com.example.gpstracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.example.gpstracker.ui.auth.SessionViewModel
import com.example.gpstracker.ui.auth.signin.SignInRoute
import com.example.gpstracker.ui.auth.signup.SignUpRoute
import com.example.gpstracker.ui.auth.vehicle.VehicleSetupRoute
import com.example.gpstracker.ui.auth.welcome.WelcomeRoute
import com.example.gpstracker.ui.drive.DriveRoute
import com.example.gpstracker.ui.insights.InsightsRoute
import com.example.gpstracker.ui.map.MapRoute
import com.example.gpstracker.ui.settings.SettingsRoute
import com.example.gpstracker.ui.tripdetail.TripDetailRoute
import com.example.gpstracker.ui.trips.TripsRoute

/**
 * The whole navigation graph: [AuthGraph] while signed out, [MainGraph] otherwise.
 * Starting in the right graph is decided by the caller from the session.
 */
@Composable
fun DriveMetricsNavHost(
    navController: NavHostController,
    startGraph: Any,
    onTriggerTestCrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessionViewModel: SessionViewModel = viewModel()

    /** Clears the whole back stack and opens the main app. */
    fun enterMain() = navController.navigate(MainGraph) {
        popUpTo(navController.graph.id) { inclusive = true }
    }

    fun leaveToAuth(first: Any) = navController.navigate(first) {
        popUpTo(navController.graph.id) { inclusive = true }
    }

    NavHost(
        navController = navController,
        startDestination = startGraph,
        modifier = modifier,
    ) {
        navigation<AuthGraph>(startDestination = WelcomeDestination) {
            composable<WelcomeDestination> {
                WelcomeRoute(
                    onCreateAccount = { navController.navigate(SignUpDestination) },
                    onSignIn = { navController.navigate(SignInDestination) },
                    onGuestStarted = ::enterMain,
                )
            }
            composable<SignInDestination> {
                SignInRoute(
                    onSignedIn = ::enterMain,
                    onGoToSignUp = {
                        navController.navigate(SignUpDestination) { popUpTo<SignInDestination> { inclusive = true } }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<SignUpDestination> {
                SignUpRoute(
                    onSignedUp = {
                        navController.navigate(VehicleSetupDestination(fromSettings = false)) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onGoToSignIn = {
                        navController.navigate(SignInDestination) { popUpTo<SignUpDestination> { inclusive = true } }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable<VehicleSetupDestination> { entry ->
            val fromSettings = entry.toRoute<VehicleSetupDestination>().fromSettings
            VehicleSetupRoute(
                onDone = { if (fromSettings) navController.popBackStack() else enterMain() },
                onBack = if (fromSettings) ({ navController.popBackStack() }) else null,
            )
        }

        navigation<MainGraph>(startDestination = DriveDestination) {
            composable<DriveDestination> {
                DriveRoute(
                    onOpenTrip = { navController.navigate(TripDetailDestination(it)) },
                    onOpenSettings = { navController.navigate(SettingsDestination) },
                )
            }
            composable<MapDestination> { MapRoute() }
            composable<TripsDestination> {
                TripsRoute(
                    onTripClick = { navController.navigate(TripDetailDestination(it)) },
                    onOpenSettings = { navController.navigate(SettingsDestination) },
                    onCreateAccount = {
                        sessionViewModel.signOut()
                        leaveToAuth(SignUpDestination)
                    },
                    onGoToDrive = { navController.navigateToTab(TopLevelDestination.Drive) },
                )
            }
            composable<InsightsDestination> {
                InsightsRoute(
                    onOpenSettings = { navController.navigate(SettingsDestination) },
                    onSetFuelPrice = { navController.navigate(VehicleSetupDestination(fromSettings = true)) },
                    onCreateAccount = {
                        sessionViewModel.signOut()
                        leaveToAuth(SignUpDestination)
                    },
                    onGoToDrive = { navController.navigateToTab(TopLevelDestination.Drive) },
                )
            }
            composable<SettingsDestination> {
                SettingsRoute(
                    onTriggerTestCrash = onTriggerTestCrash,
                    onBack = { navController.popBackStack() },
                    onEditVehicle = { navController.navigate(VehicleSetupDestination(fromSettings = true)) },
                    onSignedOut = { leaveToAuth(AuthGraph) },
                    onCreateAccount = {
                        sessionViewModel.signOut()
                        leaveToAuth(SignUpDestination)
                    },
                )
            }
            composable<TripDetailDestination> { entry ->
                TripDetailRoute(
                    tripId = entry.toRoute<TripDetailDestination>().tripId,
                    onBack = { navController.popBackStack() },
                    onSetFuelPrice = { navController.navigate(VehicleSetupDestination(fromSettings = true)) },
                )
            }
        }
    }
}
