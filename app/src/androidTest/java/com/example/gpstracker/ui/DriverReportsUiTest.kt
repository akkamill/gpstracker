package com.example.gpstracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.drive.DriveScreen
import com.example.gpstracker.ui.model.ReportKind
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DriverReportsUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun reportingTakesOneTapAfterOpeningThePicker() {
        var reported: ReportKind? = null
        rule.setContent {
            DriveMetricsTheme {
                DriveScreen(PreviewStates.driveReportAhead, {}, {}, onReport = { reported = it })
            }
        }
        rule.onNodeWithContentDescription("Report something on the road").performClick()
        rule.onNodeWithText("Speed camera").performClick()
        assertEquals(ReportKind.SpeedCamera, reported)
        rule.onNodeWithText("Speed camera reported. Thank you!").assertIsDisplayed()
    }

    @Test
    fun aReportAheadIsAnnouncedWithItsDistance() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveReportAhead, {}, {}) }
        }
        rule.onNodeWithText("Police ahead").assertIsDisplayed()
        rule.onNodeWithText("400 m, Savanorių pr.").assertIsDisplayed()
    }

    @Test
    fun afterPassingAReportTheDriverCanSayItIsGone() {
        var answer: Pair<String, Boolean>? = null
        rule.setContent {
            DriveMetricsTheme {
                DriveScreen(PreviewStates.driveStillThere, {}, {}, onAnswerStillThere = { id, yes -> answer = id to yes })
            }
        }
        rule.onNodeWithText("Is it still there?").assertIsDisplayed()
        rule.onNodeWithText("No, it's gone").performClick()
        assertEquals("r1" to false, answer)
    }

    @Test
    fun theReportButtonWaitsForAKnownPosition() {
        rule.setContent {
            DriveMetricsTheme { DriveScreen(PreviewStates.driveDriving, {}, {}) }
        }
        rule.onNodeWithContentDescription("Report something on the road").assertDoesNotExist()
    }
}
