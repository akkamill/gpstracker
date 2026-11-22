package com.example.gpstracker.server

/**
 * Settings of the server, read from environment variables so the same build runs on a laptop,
 * in Docker and on a cloud host. `compose.yaml` sets them for the local containers.
 */
data class ServerConfig(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    /** Signs the login tokens. Anyone who knows it can make tokens, so it is never committed for real use. */
    val jwtSecret: String,
    val tokenLifetimeDays: Long,
    /** Creates the demo account at start, for local development and demos. */
    val seedDemoAccount: Boolean = false,
    /** The support team's key for the chat page; without one, the page cannot join chats. */
    val supportAgentKey: String? = null,
    /** Keeps demo road reports along the demo routes, for local development and demos. */
    val seedDemoReports: Boolean = false,
) {
    override fun toString() = "ServerConfig(port=$port, databaseUrl=$databaseUrl, databaseUser=$databaseUser, secrets=***)"

    companion object {
        const val MIN_SECRET_LENGTH = 32
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_TOKEN_DAYS = 30L

        fun fromEnvironment(env: Map<String, String> = System.getenv()): ServerConfig {
            fun required(name: String) = env[name]?.takeIf { it.isNotBlank() } ?: error("Environment variable $name is not set")
            val secret = required("JWT_SECRET")
            require(secret.length >= MIN_SECRET_LENGTH) { "JWT_SECRET must be at least $MIN_SECRET_LENGTH characters" }
            return ServerConfig(
                port = env["PORT"]?.toIntOrNull() ?: DEFAULT_PORT,
                databaseUrl = required("DATABASE_URL"),
                databaseUser = required("DATABASE_USER"),
                databasePassword = required("DATABASE_PASSWORD"),
                jwtSecret = secret,
                tokenLifetimeDays = env["TOKEN_LIFETIME_DAYS"]?.toLongOrNull() ?: DEFAULT_TOKEN_DAYS,
                seedDemoAccount = env["SEED_DEMO_ACCOUNT"].toBoolean(),
                supportAgentKey = env["SUPPORT_AGENT_KEY"]?.takeIf { it.isNotBlank() },
                seedDemoReports = env["SEED_DEMO_REPORTS"].toBoolean(),
            )
        }
    }
}
