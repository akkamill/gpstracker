package com.example.gpstracker.server

import com.example.gpstracker.server.accounts.PostgresAccountRepository
import com.example.gpstracker.server.auth.AuthService
import com.example.gpstracker.server.auth.Tokens
import com.example.gpstracker.server.db.Database
import com.example.gpstracker.server.reports.PostgresReportStore
import com.example.gpstracker.server.reports.ReportService
import com.example.gpstracker.server.support.PostgresSupportRepository
import com.example.gpstracker.server.support.SupportHub
import com.example.gpstracker.simulator.vehicle.CommunityReports
import com.example.gpstracker.tracing.ConsoleSink
import com.example.gpstracker.tracing.Tracing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val DEMO_REPORT_CHECK = 10.minutes

/** Puts the demo reports back whenever they expired or were voted away. */
private fun keepDemoReports(reports: ReportService) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        while (isActive) {
            runCatching { reports.refreshDemoReports(CommunityReports.demo(System.currentTimeMillis())) }
                .onFailure { Tracing.tracer("server").error("demo reports not refreshed", it) }
            delay(DEMO_REPORT_CHECK)
        }
    }
}

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
        reports = ReportService(PostgresReportStore(dataSource)),
        agentKey = config.supportAgentKey,
    )
    if (config.seedDemoReports) keepDemoReports(deps.reports)
    tracer.info("server starting", "port" to config.port)
    embeddedServer(Netty, port = config.port) { driveMetrics(deps) }.start(wait = true)
}
