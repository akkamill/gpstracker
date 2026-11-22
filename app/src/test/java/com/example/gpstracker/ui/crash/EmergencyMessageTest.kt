package com.example.gpstracker.ui.crash

import com.example.gpstracker.ui.model.GeoPointUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyMessageTest {
    private val at = 1_700_000_000_000L
    private val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))

    @Test
    fun `the text says when and where`() {
        assertEquals(
            "I may have been in a car crash at $time. My last known position: 54.90123, 23.93456. Sent with DriveMetrics.",
            EmergencyMessage.build(GeoPointUi(54.901234, 23.934561), at),
        )
    }

    @Test
    fun `without a position the text says so`() {
        assertTrue(EmergencyMessage.build(null, at).contains("My position is not known."))
    }
}
