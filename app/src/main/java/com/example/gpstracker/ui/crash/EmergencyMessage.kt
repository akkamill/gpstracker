package com.example.gpstracker.ui.crash

import com.example.gpstracker.ui.model.GeoPointUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The text prepared for the emergency contact after a suspected crash. */
object EmergencyMessage {
    fun build(position: GeoPointUi?, atMs: Long): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(atMs))
        val where = position?.let { "My last known position: %.5f, %.5f.".format(Locale.US, it.lat, it.lon) }
            ?: "My position is not known."
        return "I may have been in a car crash at $time. $where Sent with DriveMetrics."
    }
}
