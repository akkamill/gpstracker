package com.example.gpstracker.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigTest {
    private val env = mapOf(
        "DATABASE_URL" to "jdbc:postgresql://db:5432/drivemetrics",
        "DATABASE_USER" to "drivemetrics",
        "DATABASE_PASSWORD" to "db-password",
        "JWT_SECRET" to "s".repeat(40),
    )

    @Test
    fun `reads the environment with sensible defaults`() {
        val config = ServerConfig.fromEnvironment(env)
        assertEquals(8080, config.port)
        assertEquals(30L, config.tokenLifetimeDays)
        assertFalse(config.seedDemoAccount)
        assertTrue(ServerConfig.fromEnvironment(env + ("SEED_DEMO_ACCOUNT" to "true")).seedDemoAccount)
        assertEquals(9000, ServerConfig.fromEnvironment(env + ("PORT" to "9000")).port)
    }

    @Test
    fun `refuses to start without a database or with a weak secret`() {
        assertThrows(IllegalStateException::class.java) { ServerConfig.fromEnvironment(env - "DATABASE_URL") }
        assertThrows(IllegalArgumentException::class.java) { ServerConfig.fromEnvironment(env + ("JWT_SECRET" to "short")) }
    }

    @Test
    fun `secrets are not printed`() {
        val text = ServerConfig.fromEnvironment(env).toString()
        assertTrue("db-password" !in text && "sss" !in text)
    }
}
