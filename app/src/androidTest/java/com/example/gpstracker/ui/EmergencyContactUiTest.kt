package com.example.gpstracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.model.AlertOutcome
import com.example.gpstracker.ui.settings.SettingsScreen
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EmergencyContactUiTest {
    @get:Rule
    val rule = createComposeRule()

    private fun showSent(outcome: AlertOutcome, phone: String = "+37060012345", tapped: MutableList<String> = mutableListOf()) {
        rule.setContent {
            DriveMetricsTheme {
                CrashAlertOverlay(
                    state = PreviewStates.crashSent.copy(outcome = outcome),
                    onImOk = {},
                    onSendNow = {},
                    contactName = "Mom",
                    contactPhone = phone,
                    onCall = { tapped += "call" },
                    onText = { tapped += "text" },
                )
            }
        }
    }

    @Test
    fun whenTheContactWasReachedTheAlertSaysSoAndAsksNothing() {
        showSent(AlertOutcome.TextedAndCalled)
        rule.onNodeWithText("Alert sent").assertIsDisplayed()
        rule.onNodeWithText("Mom got a text with your position, and the phone is calling them now.").assertIsDisplayed()
        rule.onNodeWithText("Call Mom").assertDoesNotExist()
    }

    @Test
    fun withoutPermissionTheContactCanStillBeReachedByHand() {
        val tapped = mutableListOf<String>()
        showSent(AlertOutcome.NotAllowed, tapped = tapped)
        rule.onNodeWithText("Alert not sent").assertIsDisplayed()
        rule.onNodeWithText("Call Mom").performClick()
        rule.onNodeWithText("Send a text with my position").performClick()
        assertEquals(listOf("call", "text"), tapped)
    }

    @Test
    fun withoutANumberTheAlertSaysWhereToAddOne() {
        showSent(AlertOutcome.NoNumber, phone = "")
        rule.onNodeWithText("Add your emergency contact's phone number in Settings to call or text them from here.").assertIsDisplayed()
        rule.onNodeWithText("Call Mom").assertDoesNotExist()
    }

    @Test
    fun settingsChecksTheNumberAndAsksToAllowTextsAndCalls() {
        var asked = false
        rule.setContent {
            DriveMetricsTheme {
                SettingsScreen(
                    state = PreviewStates.settingsSignedIn.copy(emergencyContactPhone = "+370 600 12345"),
                    onBack = {},
                    onToleranceChange = {},
                    onTripEndAfterChange = {},
                    onCrashDetectionChange = {},
                    onEmergencyContactChange = {},
                    onEditVehicle = {},
                    onTriggerTestCrash = {},
                    onSignOut = {},
                    onCreateAccount = {},
                    alertsAllowed = false,
                    onAllowAlerts = { asked = true },
                )
            }
        }
        rule.onNodeWithText("Allow texts and calls").performScrollTo().performClick()
        assertEquals(true, asked)
    }

    @Test
    fun settingsPointsOutANumberThatIsNotOne() {
        rule.setContent {
            DriveMetricsTheme {
                SettingsScreen(
                    state = PreviewStates.settingsSignedIn.copy(emergencyContactPhone = "call me"),
                    onBack = {},
                    onToleranceChange = {},
                    onTripEndAfterChange = {},
                    onCrashDetectionChange = {},
                    onEmergencyContactChange = {},
                    onEditVehicle = {},
                    onTriggerTestCrash = {},
                    onSignOut = {},
                    onCreateAccount = {},
                )
            }
        }
        rule.onNodeWithText("Enter a phone number, for example +370 600 12345.").performScrollTo().assertIsDisplayed()
    }
}
