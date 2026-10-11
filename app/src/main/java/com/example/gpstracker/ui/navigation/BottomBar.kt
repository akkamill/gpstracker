package com.example.gpstracker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.example.gpstracker.R
import com.example.gpstracker.ui.theme.Asphalt
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.LineWhite
import com.example.gpstracker.ui.theme.NightBlue
import kotlin.reflect.KClass

/** The four tabs of the bottom bar. Settings is pushed from the account avatar instead. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Drive(DriveDestination, DriveDestination::class, R.string.nav_drive, Icons.Rounded.Speed),
    Map(MapDestination, MapDestination::class, R.string.nav_map, Icons.Rounded.Map),
    Trips(TripsDestination, TripsDestination::class, R.string.nav_trips, Icons.Rounded.History),
    Insights(InsightsDestination, InsightsDestination::class, R.string.nav_insights, Icons.Rounded.Insights),
}

/** True when this destination, or one of its parents, is the tab's route. */
fun NavDestination?.isInTab(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

/** True on the four tab destinations; the bar is hidden everywhere else. */
fun NavDestination?.isTopLevel(): Boolean = TopLevelDestination.entries.any { isInTab(it) }

/** Switches tab, keeping each tab's saved state and avoiding duplicate entries. */
fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<DriveDestination> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun DriveBottomBar(
    current: NavDestination?,
    onTabSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        TopLevelDestination.entries.forEach { tab ->
            val selected = current.isInTab(tab)
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Asphalt,
                    selectedTextColor = LineWhite,
                    indicatorColor = NightBlue,
                    unselectedIconColor = Concrete,
                    unselectedTextColor = Concrete,
                ),
            )
        }
    }
}
