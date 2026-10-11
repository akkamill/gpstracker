package com.example.gpstracker.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.auth.signin.SignInScreen
import com.example.gpstracker.ui.auth.signup.SignUpScreen
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.drive.DriveScreen
import com.example.gpstracker.ui.insights.InsightsScreen
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.trips.TripsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DriveMetricsUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dashboardShowsOverLimitWhenSpeeding() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveOverLimit, {}, {}) }
        }
        rule.onNodeWithContentDescription("22 over limit", substring = true).assertIsDisplayed()
    }

    @Test
    fun dashboardHidesOverLimitWithinTolerance() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveDriving, {}, {}) }
        }
        rule.onNodeWithContentDescription("Speed 48 kilometers per hour, limit 50").assertIsDisplayed()
    }

    @Test
    fun drivingDashboardShowsTheTripStatusAndNoTripControl() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveDriving, {}, {}) }
        }
        rule.onNodeWithContentDescription("Driving. Trip started 14:05").assertIsDisplayed()
        rule.onNodeWithText("Start trip").assertDoesNotExist()
        rule.onNodeWithText("End trip").assertDoesNotExist()
    }

    @Test
    fun parkedDashboardShowsTheLastTripCardAndOpensIt() {
        var opened: String? = null
        rule.setContent {
            DriveMetricsTheme {
                DriveScreen(PreviewStates.driveParked, onOpenTrip = { opened = it }, onOpenSettings = {})
            }
        }
        rule.onNodeWithText("Last trip").assertIsDisplayed()
        rule.onNodeWithContentDescription("Parked. A new trip starts when the car moves").assertIsDisplayed()
        rule.onNodeWithText("Last trip").performClick()
        assertEquals(PreviewStates.driveParked.lastTrip?.id, opened)
    }

    @Test
    fun firstUseDashboardWaitsForTheFirstDrive() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveFirstUse, {}, {}) }
        }
        rule.onNodeWithText("Waiting for your first drive").assertIsDisplayed()
        rule.onNodeWithText("Trips are recorded automatically when the car moves.").assertIsDisplayed()
    }

    @Test
    fun stoppedAtALightKeepsTheTripGoing() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveStopped, {}, {}) }
        }
        rule.onNodeWithContentDescription("Stopped. Trip continues when you drive on").assertIsDisplayed()
    }

    @Test
    fun savedTripBannerOffersToViewTheTrip() {
        var viewed: String? = null
        rule.setContent {
            DriveMetricsTheme {
                DriveScreen(PreviewStates.driveTripSaved, onOpenTrip = { viewed = it }, onOpenSettings = {})
            }
        }
        rule.onNodeWithText("Trip saved").assertIsDisplayed()
        rule.onNodeWithText("View").performClick()
        assertEquals(PreviewStates.driveTripSaved.tripJustSaved?.id, viewed)
    }

    @Test
    fun offlineDashboardShowsTheBannerAndOffersSettings() {
        var openedSettings = false
        rule.setContent {
            DriveMetricsTheme {
                DriveScreen(PreviewStates.driveOffline, {}, onOpenSettings = { openedSettings = true })
            }
        }
        rule.onNodeWithText("Not connected to the car. Check the server address in Settings.").assertIsDisplayed()
        rule.onNodeWithContentDescription("Speed unavailable. Not connected to the car.").assertIsDisplayed()
        rule.onNodeWithText("Settings").performClick()
        assertTrue(openedSettings)
    }

    @Test
    fun emptyTripsShowsGoToDriveButton() {
        var wentToDrive = false
        rule.setContent {
            DriveMetricsTheme {
                TripsScreen(
                    PreviewStates.tripsEmpty,
                    isGuest = false,
                    onTripClick = {},
                    onOpenSettings = {},
                    onCreateAccount = {},
                    onGoToDrive = { wentToDrive = true },
                )
            }
        }
        rule.onNodeWithText("No trips yet").assertIsDisplayed()
        rule.onNodeWithText("Go to Drive").assertIsDisplayed().performClick()
        assertTrue(wentToDrive)
    }

    @Test
    fun crashOverlayImOkHidesIt() {
        rule.setContent {
            var state by remember { mutableStateOf(PreviewStates.crashCounting) }
            DriveMetricsTheme {
                CrashAlertOverlay(
                    state = state,
                    onImOk = { state = CrashAlertUiState() },
                    onSendNow = { state = state.copy(sent = true) },
                )
            }
        }
        rule.onNodeWithText("Possible crash").assertIsDisplayed()
        rule.onNodeWithText("I'm OK").performClick()
        rule.onNodeWithText("Possible crash").assertDoesNotExist()
    }

    @Test
    fun dashboardSurvivesLargeFontScale() {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.3f)) {
                DriveMetricsTheme { DriveScreen(PreviewStates.driveOverLimit, {}, {}) }
            }
        }
        rule.onNodeWithText("Driving score").assertIsDisplayed()
        rule.onNodeWithContentDescription("Driving. Trip started 14:05").assertIsDisplayed()
    }

    @Test
    fun signInShowsWrongPasswordError() {
        rule.setContent {
            DriveMetricsTheme {
                SignInScreen(PreviewStates.signInWrongPassword, {}, {}, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Email or password is incorrect.").assertIsDisplayed()
    }

    @Test
    fun signUpShowsFieldErrors() {
        rule.setContent {
            DriveMetricsTheme {
                SignUpScreen(PreviewStates.signUpErrors, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Enter your name.").assertIsDisplayed()
        rule.onNodeWithText("Passwords do not match.").assertIsDisplayed()
    }

    @Test
    fun insightsAsksForMoreTripsWhenThereAreFewerThanThree() {
        rule.setContent {
            DriveMetricsTheme {
                InsightsScreen(PreviewStates.insightsNotEnoughData, false, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Not enough trips yet").assertIsDisplayed()
    }

    @Test
    fun insightsLabelsTheBenchmarkAsSimulated() {
        rule.setContent {
            DriveMetricsTheme {
                InsightsScreen(PreviewStates.insightsWeekBetter, false, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Simulated benchmark").assertIsDisplayed()
        rule.onNodeWithText("Estimates based on your vehicle settings.").assertExists()
    }
}
