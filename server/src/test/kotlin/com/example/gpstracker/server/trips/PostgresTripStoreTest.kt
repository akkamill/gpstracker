package com.example.gpstracker.server.trips

import com.example.gpstracker.server.accounts.Account
import com.example.gpstracker.server.accounts.PostgresAccountRepository
import com.example.gpstracker.server.db.TestPostgres
import com.example.gpstracker.simulator.history.DemoHistory
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Trip history against PostgreSQL in Docker (skipped without Docker). */
class PostgresTripStoreTest {
    private val dataSource by lazy { TestPostgres.dataSource() }
    private val store by lazy { PostgresTripStore(dataSource) }

    private suspend fun driver(): UUID = UUID.randomUUID().also {
        PostgresAccountRepository(dataSource).insert(Account(it, "$it@example.com", "Driver", "hash", "salt"))
    }

    @Test
    fun tripsAreStoredAsTheyWereAndReplacedBySameId() = runBlocking {
        val ona = driver()
        val history = DemoHistory.build(System.currentTimeMillis())
        history.forEach { store.save(ona, it) }
        assertEquals(history, store.trips(ona, limit = 1_000))
        assertEquals(history.size, store.count(ona))

        store.save(ona, history[0].copy(trip = history[0].trip.copy(score = 1)))
        assertEquals(1, store.trips(ona, limit = 1).single().trip.score)
        assertEquals(0, store.count(driver()))
    }
}
