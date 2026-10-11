package com.example.gpstracker.ui.model

import com.example.gpstracker.data.fake.PreviewStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiModelsTest {
    @Test
    fun `score bands follow the thresholds`() {
        assertEquals(ScoreBand.Good, scoreBandOf(85))
        assertEquals(ScoreBand.Fair, scoreBandOf(84))
        assertEquals(ScoreBand.Fair, scoreBandOf(60))
        assertEquals(ScoreBand.Risky, scoreBandOf(59))
    }

    @Test
    fun `over limit needs more than the 5 km h tolerance`() {
        val base = DriveUiState(speedLimitKmh = 50)
        assertFalse(base.copy(speedKmh = 55).isOverLimit)
        assertTrue(base.copy(speedKmh = 56).isOverLimit)
        assertEquals(22, PreviewStates.driveOverLimit.overLimitBy)
    }

    @Test
    fun `unknown limit is never over limit`() {
        assertFalse(DriveUiState(speedKmh = 200, speedLimitKmh = null).isOverLimit)
    }
}
