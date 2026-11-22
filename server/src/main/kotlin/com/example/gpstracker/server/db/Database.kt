package com.example.gpstracker.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway

/** Connections to PostgreSQL, shared by all requests. */
object Database {
    private const val MAX_CONNECTIONS = 10

    fun connect(url: String, user: String, password: String): HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = MAX_CONNECTIONS
            poolName = "drivemetrics"
        },
    )

    /**
     * Brings the tables up to date by running the scripts in `resources/db/migration` that have
     * not run yet (V1, V2, ...). A script never changes once released; changes get a new script.
     */
    fun migrate(dataSource: DataSource): Int = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()
        .migrationsExecuted

    /** True when the database answers. */
    fun isUp(dataSource: DataSource): Boolean = runCatching {
        dataSource.connection.use { it.isValid(CHECK_TIMEOUT_S) }
    }.getOrDefault(false)

    private const val CHECK_TIMEOUT_S = 2
}
