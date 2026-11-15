package com.example.gpstracker.domain.reports

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRulesTest {
    private val rules = ReportRules()
    private val minute = 60_000L
    private val here = LatLon(54.9, 23.9)

    @Test
    fun `each type lives as long as such things usually last`() {
        val police = rules.create(ReportType.Police, here, nowMs = 0)
        val closed = rules.create(ReportType.RoadClosed, here, nowMs = 0)
        assertEquals(30 * minute, police.expiresAtMs)
        assertEquals(180 * minute, closed.expiresAtMs)
        assertEquals(ReportSource.Me, police.source)
        assertTrue(police.isActive(29 * minute))
        assertFalse(police.isActive(30 * minute))
    }

    @Test
    fun `still there keeps a report alive a while longer`() {
        val jam = rules.create(ReportType.TrafficJam, here, nowMs = 0) // 20 minutes
        val confirmed = rules.vote(jam, stillThere = true, nowMs = 18 * minute)
        assertEquals(1, confirmed.stillThereVotes)
        assertEquals(33 * minute, confirmed.expiresAtMs)
        // a confirmation never shortens a report
        assertEquals(confirmed.expiresAtMs, rules.vote(confirmed, stillThere = true, nowMs = 0).expiresAtMs)
    }

    @Test
    fun `a report is hidden when more drivers say it is gone than still there`() {
        val police = rules.create(ReportType.Police, here, nowMs = 0)
        assertFalse(rules.vote(police, stillThere = false, nowMs = minute).isActive(minute))

        val confirmed = rules.vote(police, stillThere = true, nowMs = minute)
        val disputed = rules.vote(confirmed, stillThere = false, nowMs = 2 * minute)
        assertTrue(disputed.isActive(2 * minute))
        assertFalse(rules.vote(disputed, stillThere = false, nowMs = 3 * minute).isActive(3 * minute))
    }

    @Test
    fun `new reports get unique ids`() {
        assertTrue(rules.create(ReportType.Hazard, here, 0).id != rules.create(ReportType.Hazard, here, 0).id)
    }
}
