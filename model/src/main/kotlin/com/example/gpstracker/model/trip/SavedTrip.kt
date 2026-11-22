package com.example.gpstracker.model.trip

import kotlinx.serialization.Serializable

/**
 * A finished trip as it is kept in the history.
 *
 * @property savedAtMs when the trip was saved, on the phone's real clock; history is grouped by
 * days and weeks with it, because the demo car's own clock runs faster than real time
 */
@Serializable
data class SavedTrip(val id: String, val savedAtMs: Long, val trip: Trip)
