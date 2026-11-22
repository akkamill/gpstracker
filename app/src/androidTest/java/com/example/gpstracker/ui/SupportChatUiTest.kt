package com.example.gpstracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.gpstracker.ui.support.ChatBubbleUi
import com.example.gpstracker.ui.support.SupportChatScreen
import com.example.gpstracker.ui.support.SupportChatUiState
import com.example.gpstracker.ui.support.SupportStatus
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SupportChatUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun messagesShowWhoWroteThemAndWhetherTheyAreStillSending() {
        rule.setContent {
            DriveMetricsTheme {
                SupportChatScreen(
                    state = SupportChatUiState(
                        status = SupportStatus.TeamOnline,
                        bubbles = listOf(
                            ChatBubbleUi("1", "My trip was not saved", mine = true, timeLabel = "08:41"),
                            ChatBubbleUi("2", "Let me check that", mine = false, timeLabel = "08:43"),
                            ChatBubbleUi("3", "Thanks", mine = true, timeLabel = "08:44", sending = true),
                        ),
                    ),
                    onBack = {},
                    onDraftChange = {},
                    onSend = {},
                    onCreateAccount = {},
                    onSignIn = {},
                )
            }
        }
        rule.onNodeWithText("Support team online").assertIsDisplayed()
        rule.onNodeWithContentDescription("You, 08:41: My trip was not saved").assertIsDisplayed()
        rule.onNodeWithContentDescription("Support team, 08:43: Let me check that").assertIsDisplayed()
        rule.onNodeWithContentDescription("You, Sending…: Thanks").assertIsDisplayed()
    }

    @Test
    fun sendIsOnlyPossibleWithText() {
        var sent = 0
        rule.setContent {
            var state by mutableStateOf(SupportChatUiState(status = SupportStatus.TeamAway))
            DriveMetricsTheme {
                SupportChatScreen(
                    state = state,
                    onBack = {},
                    onDraftChange = { state = state.copy(draft = it) },
                    onSend = { sent++ },
                    onCreateAccount = {},
                    onSignIn = {},
                )
            }
        }
        rule.onNodeWithText("The team will answer as soon as they can").assertIsDisplayed()
        rule.onNodeWithContentDescription("Send message").assertIsNotEnabled()
        rule.onNodeWithText("Write a message").performTextInput("Hello")
        rule.onNodeWithContentDescription("Send message").assertIsEnabled().performClick()
        assertEquals(1, sent)
    }

    @Test
    fun demoModeAsksForAnAccount() {
        var createClicked = false
        rule.setContent {
            DriveMetricsTheme {
                SupportChatScreen(
                    state = SupportChatUiState(status = SupportStatus.SignedOut),
                    onBack = {},
                    onDraftChange = {},
                    onSend = {},
                    onCreateAccount = { createClicked = true },
                    onSignIn = {},
                )
            }
        }
        rule.onNodeWithText("Chat with our support team").assertIsDisplayed()
        rule.onNodeWithText("Create account").performClick()
        assertEquals(true, createClicked)
    }
}
