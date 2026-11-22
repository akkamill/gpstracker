package com.example.gpstracker.server.accounts

import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A stored account. [email] is always lower-case. */
data class Account(
    val id: UUID,
    val email: String,
    val name: String,
    val passwordHash: String,
    val salt: String,
) {
    override fun toString() = "Account(id=$id, email=***, name=***, secrets=***)"
}

interface AccountRepository {
    suspend fun findByEmail(email: String): Account?

    suspend fun findById(id: UUID): Account?

    /** Returns false when the email is already registered. */
    suspend fun insert(account: Account): Boolean
}

/** Accounts in PostgreSQL. JDBC blocks, so every query runs on the IO threads. */
class PostgresAccountRepository(private val dataSource: DataSource) : AccountRepository {
    override suspend fun findByEmail(email: String): Account? = query("SELECT * FROM accounts WHERE email = ?", email)

    override suspend fun findById(id: UUID): Account? = query("SELECT * FROM accounts WHERE id = ?", id)

    override suspend fun insert(account: Account): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO accounts (id, email, name, password_hash, salt) VALUES (?, ?, ?, ?, ?) ON CONFLICT (email) DO NOTHING",
            ).use { statement ->
                statement.setObject(1, account.id)
                statement.setString(2, account.email)
                statement.setString(3, account.name)
                statement.setString(4, account.passwordHash)
                statement.setString(5, account.salt)
                statement.executeUpdate() == 1
            }
        }
    }

    private suspend fun query(sql: String, value: Any): Account? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, value)
                statement.executeQuery().use { rows ->
                    if (!rows.next()) {
                        null
                    } else {
                        Account(
                            id = rows.getObject("id", UUID::class.java),
                            email = rows.getString("email"),
                            name = rows.getString("name"),
                            passwordHash = rows.getString("password_hash"),
                            salt = rows.getString("salt"),
                        )
                    }
                }
            }
        }
    }
}

/** Accounts in memory, for tests and for running the server without a database. */
class InMemoryAccountRepository : AccountRepository {
    private val accounts = LinkedHashMap<UUID, Account>()

    override suspend fun findByEmail(email: String): Account? = synchronized(accounts) { accounts.values.firstOrNull { it.email == email } }

    override suspend fun findById(id: UUID): Account? = synchronized(accounts) { accounts[id] }

    override suspend fun insert(account: Account): Boolean = synchronized(accounts) {
        if (accounts.values.any { it.email == account.email }) return false
        accounts[account.id] = account
        true
    }
}
