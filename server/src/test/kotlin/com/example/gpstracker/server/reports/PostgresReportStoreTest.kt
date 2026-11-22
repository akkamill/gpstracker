package com.example.gpstracker.server.reports

import com.example.gpstracker.domain.reports.ReportRules
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.server.accounts.Account
import com.example.gpstracker.server.accounts.PostgresAccountRepository
import com.example.gpstracker.server.db.TestPostgres
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shared reports against PostgreSQL in Docker (skipped without Docker). */
class PostgresReportStoreTest {
    private val dataSource by lazy { TestPostgres.dataSource() }
    private val store by lazy { PostgresReportStore(dataSource) }
    private val rules = ReportRules()
    private val now = System.currentTimeMillis()

    /** Each test works in its own spot, far from the others' reports. */
    private val spot = LatLon(10.0 + Math.random() * 50, 10.0 + Math.random() * 50)

    private suspend fun driver(): UUID = UUID.randomUUID().also {
        PostgresAccountRepository(dataSource).insert(Account(it, "$it@example.com", "Driver", "hash", "salt"))
    }

    private fun report(id: String = UUID.randomUUID().toString(), at: LatLon = spot) =
        rules.create(ReportType.Hazard, at, now, headingDeg = 45.0, roadName = "Jonavos g.", id = id)

    @Test
    fun reportsAreStoredOnceAndFoundNearby() = runBlocking {
        val reporter = driver()
        val first = store.insert(report("pg-1"), reporter)
        store.insert(report("pg-1").copy(roadName = "changed"), reporter)
        assertEquals("Jonavos g.", store.find("pg-1")?.report?.roadName)
        assertEquals(reporter, first.reporterId)
        assertEquals(45.0, first.report.headingDeg!!, 1e-9)

        val nearby = store.active(now, Area(spot, 5.0), 100).map { it.report.id }
        assertTrue("pg-1" in nearby)
        assertTrue(store.active(now, Area(LatLon(spot.latitude + 1, spot.longitude), 5.0), 100).none { it.report.id == "pg-1" })
        assertTrue(store.active(first.report.expiresAtMs + 1, Area(spot, 5.0), 100).none { it.report.id == "pg-1" })
    }

    @Test
    fun votesCountOncePerDriverEvenAtTheSameMoment() = runBlocking {
        val id = UUID.randomUUID().toString()
        store.insert(report(id), driver())
        val voters = List(5) { driver() }
        // five drivers answer at once, and one of them twice
        (voters + voters.first()).map { voter ->
            async { store.vote(id, voter, stillThere = true) { rules.vote(it, true, now) } }
        }.awaitAll()
        assertEquals(5, store.find(id)?.report?.stillThereVotes)
        assertNull(store.vote("missing", voters.first(), stillThere = true) { it })
    }

    @Test
    fun aReplacedReportStartsFresh() = runBlocking {
        val id = UUID.randomUUID().toString()
        store.insert(report(id), driver())
        store.vote(id, driver(), stillThere = false) { rules.vote(it, false, now) }
        store.replace(report(id))
        val fresh = store.find(id)!!
        assertEquals(0, fresh.report.goneVotes)
        assertNull(fresh.reporterId)
    }
}
