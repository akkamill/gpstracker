package com.example.gpstracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.crash.CrashAlertOverlay
import com.example.gpstracker.ui.settings.SettingsScreen
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EmergencyContactUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun afterTheAlertTheContactCanBeCalledOrTexted() {
        val tapped = mutableListOf<String>()
        rule.setContent {
            DriveMetricsTheme {
                CrashAlertOverlay(
                    state = PreviewStates.crashSent,
                    onImOk = {},
                    onSendNow = {},
                    contactName = "Mom",
                    contactPhone = "+37060012345",
                    onCall = { tapped += "call" },
                    onText = { tapped += "text" },
                )
            }
        }
        rule.onNodeWithText("Call Mom").performClick()
        rule.onNodeWithText("Send a text with my position").performClick()
        assertEquals(listOf("call", "text"), tapped)
    }

    @Test
    fun withoutANumberTheAlertSaysWhereToAddOne() {
        rule.setContent {
            DriveMetricsTheme { CrashAlertOverlay(state = PreviewStates.crashSent, onImOk = {}, onSendNow = {}, contactName = "Mom") }
        }
        rule.onNodeWithText("Add your emergency contact's phone number in Settings to call or text them from here.").assertIsDisplayed()
        rule.onNodeWithText("Call Mom").assertDoesNotExist()
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
