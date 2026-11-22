package com.example.gpstracker.server.accounts

import com.example.gpstracker.server.db.Database
import com.example.gpstracker.server.db.TestPostgres
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The real migrations and queries against PostgreSQL in Docker (skipped without Docker). */
class PostgresAccountRepositoryTest {
    private val dataSource by lazy { TestPostgres.dataSource() }
    private val repo by lazy { PostgresAccountRepository(dataSource) }

    private fun account(email: String) = Account(UUID.randomUUID(), email, "Ona", "hash", "salt")

    @Test
    fun accountsAreStoredAndFound() = runBlocking {
        val ona = account("ona-${UUID.randomUUID()}@example.com")
        assertTrue(repo.insert(ona))
        assertEquals(ona, repo.findByEmail(ona.email))
        assertEquals(ona, repo.findById(ona.id))
        assertNull(repo.findByEmail("nobody@example.com"))
    }

    @Test
    fun anEmailIsStoredOnlyOnce() = runBlocking {
        val email = "twice-${UUID.randomUUID()}@example.com"
        assertTrue(repo.insert(account(email)))
        assertFalse(repo.insert(account(email)))
    }

    @Test
    fun everyMigrationRanOnceOnAFreshDatabase() {
        assertTrue(Database.isUp(dataSource))
        val scripts = javaClass.classLoader.getResource("db/migration")!!.let { java.io.File(it.toURI()).list()!!.size }
        assertEquals(scripts, TestPostgres.migrationsRun)
        assertEquals(0, Database.migrate(dataSource))
    }
}
