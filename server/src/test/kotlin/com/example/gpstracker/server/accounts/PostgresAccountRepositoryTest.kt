package com.example.gpstracker.server.accounts

import com.example.gpstracker.server.db.Database
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Runs the real migrations and queries against a throwaway PostgreSQL in Docker. Skipped when
 * Docker is not running on this computer; it always runs on CI.
 */
class PostgresAccountRepositoryTest {
    companion object {
        private var postgres: PostgreSQLContainer? = null
        private lateinit var dataSource: HikariDataSource

        @BeforeClass
        @JvmStatic
        fun start() {
            assumeTrue("Docker is not running", runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false))
            postgres = PostgreSQLContainer("postgres:17-alpine").also { it.start() }
            dataSource = Database.connect(postgres!!.jdbcUrl, postgres!!.username, postgres!!.password)
            assertEquals(1, Database.migrate(dataSource))
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            if (postgres == null) return
            dataSource.close()
            postgres?.stop()
        }
    }

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
    fun migrationsRunOnlyOnce() {
        assertEquals(0, Database.migrate(dataSource))
        assertTrue(Database.isUp(dataSource))
    }
}
