package com.example.gpstracker.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.drive.DriveScreen
import com.example.gpstracker.ui.model.CrashAlertUiState
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.trips.TripsScreen
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
            DriveMetricsTheme { DriveScreen(PreviewStates.driveOverLimit, {}, {}, {}) }
        }
        rule.onNodeWithContentDescription("22 over limit", substring = true).assertIsDisplayed()
    }

    @Test
    fun dashboardHidesOverLimitWithinTolerance() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveActive, {}, {}, {}) }
        }
        rule.onNodeWithContentDescription("Speed 48 kilometers per hour, limit 50").assertIsDisplayed()
    }

    @Test
    fun offlineDashboardDisablesTripButtonAndOffersSettings() {
        var openedSettings = false
        rule.setContent {
            DriveMetricsTheme {
                DriveScreen(PreviewStates.driveOffline, {}, {}, onOpenSettings = { openedSettings = true })
            }
        }
        rule.onNodeWithText("Waiting for connection").assertIsDisplayed()
        rule.onNodeWithText("Settings").performClick()
        assertTrue(openedSettings)
    }

    @Test
    fun emptyTripsShowsGoToDriveButton() {
        var wentToDrive = false
        rule.setContent {
            DriveMetricsTheme {
                TripsScreen(PreviewStates.tripsEmpty, onTripClick = {}, onGoToDrive = { wentToDrive = true })
            }
        }
        rule.onNodeWithText("No trips yet").assertIsDisplayed()
        rule.onNodeWithText("Go to Drive").assertIsDisplayed().assertIsEnabled().performClick()
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
                DriveMetricsTheme { DriveScreen(PreviewStates.driveOverLimit, {}, {}, {}) }
            }
        }
        rule.onNodeWithText("End trip").assertIsDisplayed()
        rule.onNodeWithText("Driving score").assertIsDisplayed()
    }
}

