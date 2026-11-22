package com.example.gpstracker.server.db

import com.zaxxer.hikari.HikariDataSource
import org.junit.Assume.assumeTrue
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * One throwaway PostgreSQL in Docker for all database tests, migrated once. Tests that use it
 * are skipped when Docker is not running on this computer; on CI it always runs.
 */
object TestPostgres {
    /** How many migrations ran on the fresh database. */
    var migrationsRun = 0
        private set

    private val container: PostgreSQLContainer? by lazy {
        if (!runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)) return@lazy null
        PostgreSQLContainer("postgres:17-alpine").also { it.start() }
    }

    private val source: HikariDataSource? by lazy {
        container?.let { db ->
            Database.connect(db.jdbcUrl, db.username, db.password).also { migrationsRun = Database.migrate(it) }
        }
    }

    /** The migrated database, or skips the calling test when Docker is not available. */
    fun dataSource(): HikariDataSource {
        assumeTrue("Docker is not running", source != null)
        return source!!
    }
}
