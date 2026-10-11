package com.example.gpstracker.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
@Serializable
data object DriveDestination

@Serializable
data object MapDestination

@Serializable
data object TripsDestination

@Serializable
data object SettingsDestination

@Serializable
data class TripDetailDestination(val tripId: String)
