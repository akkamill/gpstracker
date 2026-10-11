package com.example.gpstracker.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations and the two nested graphs. */

/** Everything shown while signed out. */
@Serializable
data object AuthGraph

/** Everything shown when signed in or in guest demo mode. */
@Serializable
data object MainGraph

@Serializable
data object WelcomeDestination

@Serializable
data object SignInDestination

@Serializable
data object SignUpDestination

/** @property fromSettings true when opened from Settings, so Save returns there. */
@Serializable
data class VehicleSetupDestination(val fromSettings: Boolean = false)

@Serializable
data object DriveDestination

@Serializable
data object MapDestination

@Serializable
data object TripsDestination

@Serializable
data object InsightsDestination

@Serializable
data object SettingsDestination

@Serializable
data class TripDetailDestination(val tripId: String)
