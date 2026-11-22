package com.example.gpstracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.gpstracker.data.fake.FakeData
import com.example.gpstracker.ui.components.LocalStreetMap
import com.example.gpstracker.ui.components.RouteMap
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import org.junit.Rule
import org.junit.Test

/** The real street map starts on the device; its tiles may still be loading, which is fine here. */
class StreetMapUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theStreetMapShowsWithTheRouteAndEvents() {
        rule.setContent {
            DriveMetricsTheme {
                CompositionLocalProvider(LocalStreetMap provides true) {
                    RouteMap(
                        route = FakeData.route,
                        events = FakeData.events,
                        currentPosition = FakeData.route.last(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Map of the route, with driving events and road reports").assertIsDisplayed()
    }
}
