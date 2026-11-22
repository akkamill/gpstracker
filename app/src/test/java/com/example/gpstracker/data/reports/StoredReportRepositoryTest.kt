package com.example.gpstracker.data.reports

import com.example.gpstracker.data.local.toEntity
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.testing.FakeReportDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoredReportRepositoryTest {
    private var now = 1_000L

    private fun report(id: String, createdAtMs: Long = 0, expiresAtMs: Long = Long.MAX_VALUE, votes: Int = 0) =
        RoadReport(id, ReportType.Hazard, LatLon(54.9, 23.9), createdAtMs, expiresAtMs, ReportSource.OtherDriver, stillThereVotes = votes)

    private fun TestScope.open(dao: FakeReportDao, demo: List<RoadReport> = emptyList()) =
        StoredReportRepository(dao, backgroundScope, demo, InMemoryReportRepository(clock = { now })).also { testScheduler.runCurrent() }

    @Test
    fun `saved reports come back after a restart, without the expired ones`() = runTest {
        val dao = FakeReportDao(listOf(report("old", expiresAtMs = 500).toEntity(), report("live", createdAtMs = 10).toEntity()))
        val repo = open(dao)
        assertEquals(listOf("live"), repo.reports.value.map { it.id })
        // the expired one is gone from the database too
        assertEquals(listOf("live"), dao.rows.map { it.id })
    }

    @Test
    fun `a saved demo report keeps its votes instead of being reset`() = runTest {
        val dao = FakeReportDao(listOf(report("community-hazard-0", votes = 2).toEntity()))
        val repo = open(dao, demo = listOf(report("community-hazard-0"), report("community-police-1", createdAtMs = 5)))
        assertEquals(listOf(2, 0), repo.reports.value.map { it.stillThereVotes })
    }

    @Test
    fun `every change is written to the database`() = runTest {
        val dao = FakeReportDao()
        val repo = open(dao)
        repo.add(report("mine", createdAtMs = 20))
        testScheduler.runCurrent()
        assertEquals(listOf("mine"), dao.rows.map { it.id })

        repo.vote("mine", stillThere = false)
        testScheduler.runCurrent()
        assertEquals(emptyList<String>(), dao.rows.map { it.id })
    }
}
