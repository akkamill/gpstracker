package com.example.gpstracker.model.map

import kotlinx.serialization.Serializable

/**
 * The road under the car.
 *
 * @property speedLimitKmh null when the limit is not known
 */
@Serializable
data class RoadInfo(val name: String, val speedLimitKmh: Int?)
