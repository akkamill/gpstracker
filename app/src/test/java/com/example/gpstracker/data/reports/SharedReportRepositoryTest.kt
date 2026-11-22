package com.example.gpstracker.data.reports

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.testing.FakeReportDao
import com.example.gpstracker.testing.FakeServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedReportRepositoryTest {
    private val server = FakeServer().apply { addAccount("Ona", "ona@example.com", "abcd1234") }
    private var token: String? = "token-ona@example.com"
    private val here = LatLon(54.9, 23.9)

    private fun report(id: String, source: ReportSource = ReportSource.Me) =
        RoadReport(id, ReportType.Hazard, here, createdAtMs = 0, expiresAtMs = Long.MAX_VALUE, source = source, roadName = "Jonavos g.")

    /** The repository with its local copy in memory; syncing is started by the test. */
    private fun TestScope.repository(demo: List<RoadReport> = emptyList()): SharedReportRepository {
        val local = StoredReportRepository(FakeReportDao(), backgroundScope, demo, InMemoryReportRepository(clock = { 0 }))
        testScheduler.runCurrent()
        return SharedReportRepository(local, server.api, { token }, backgroundScope, position = { here })
    }

    @Test
    fun `other drivers' reports arrive and replace the simulated ones`() = runTest {
        server.reports["community-police-0"] = report("community-police-0", ReportSource.OtherDriver)
        val repo = repository(demo = listOf(report("community-police-0", ReportSource.OtherDriver), report("community-hazard-1")))
        repo.sync()
        // the server's list is the truth: the simulated report it does not have is gone
        assertEquals(listOf("community-police-0"), repo.reports.value.map { it.id })
    }

    @Test
    fun `a report made here is shared and comes back as the driver's own`() = runTest {
        val repo = repository()
        repo.add(report("mine"))
        testScheduler.runCurrent()
        assertTrue("mine" in server.reports)
        repo.sync()
        assertEquals(ReportSource.Me, repo.reports.value.single().source)
    }

    @Test
    fun `without a connection reports wait on the phone and are sent later`() = runTest {
        val repo = repository()
        server.reachable = false
        repo.add(report("offline"))
        repo.vote("offline", stillThere = true)
        testScheduler.runCurrent()
        repo.sync()
        assertEquals(listOf("offline"), repo.reports.value.map { it.id })

        server.reachable = true
        repo.sync()
        assertTrue("offline" in server.reports)
        assertEquals(listOf("offline:true"), server.votes)
        assertEquals(listOf("offline"), repo.reports.value.map { it.id })
    }

    @Test
    fun `answers to other drivers' reports are sent to the server`() = runTest {
        server.reports["theirs"] = report("theirs", ReportSource.OtherDriver)
        val repo = repository()
        repo.sync()
        repo.vote("theirs", stillThere = false)
        testScheduler.runCurrent()
        assertEquals(listOf("theirs:false"), server.votes)
    }

    @Test
    fun `in demo mode reports stay on the phone but others' still arrive`() = runTest {
        token = null
        server.reports["theirs"] = report("theirs", ReportSource.OtherDriver)
        val repo = repository()
        repo.add(report("guest-report"))
        repo.vote("theirs", stillThere = true)
        testScheduler.runCurrent()
        repo.sync()
        assertTrue("guest-report" !in server.reports)
        assertTrue(server.votes.isEmpty())
        assertEquals(setOf("theirs", "guest-report"), repo.reports.value.map { it.id }.toSet())
    }

    @Test
    fun `syncing repeats in the background`() = runTest {
        val repo = repository()
        repo.start()
        testScheduler.runCurrent()
        server.reports["later"] = report("later", ReportSource.OtherDriver)
        testScheduler.advanceTimeBy(SharedReportRepository.SYNC_INTERVAL_MS + 1)
        assertEquals(listOf("later"), repo.reports.value.map { it.id })
    }
}
