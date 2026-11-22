package com.example.gpstracker.server

import com.example.gpstracker.server.accounts.PostgresAccountRepository
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.db.Database
import com.example.gpstracker.server.support.PostgresSupportRepository
import com.example.gpstracker.server.support.SupportHub
import com.example.gpstracker.tracing.ConsoleSink
import com.example.gpstracker.tracing.Tracing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/** Starts the server: settings from the environment, database migrations, then HTTP. */
fun main() {
    Tracing.install(ConsoleSink())
    val tracer = Tracing.tracer("server")
    val config = ServerConfig.fromEnvironment()
    val dataSource = Database.connect(config.databaseUrl, config.databaseUser, config.databasePassword)
    val migrations = Database.migrate(dataSource)
    tracer.info("database ready", "migrationsRun" to migrations)

    val tokens = Tokens(config.jwtSecret, TimeUnit.DAYS.toMillis(config.tokenLifetimeDays))
    val auth = AuthService(PostgresAccountRepository(dataSource), tokens)
    if (config.seedDemoAccount) runBlocking { auth.addDemoAccount() }
    val deps = ServerDependencies(
        auth = auth,
        tokens = tokens,
        databaseUp = { Database.isUp(dataSource) },
        support = SupportHub(PostgresSupportRepository(dataSource)),
        agentKey = config.supportAgentKey,
    )
    tracer.info("server starting", "port" to config.port)
    embeddedServer(Netty, port = config.port) { driveMetrics(deps) }.start(wait = true)
}
