package com.example.gpstracker.model.geo

import kotlinx.serialization.Serializable

/** A latitude/longitude pair in degrees. */
@Serializable
data class LatLon(val latitude: Double, val longitude: Double)
