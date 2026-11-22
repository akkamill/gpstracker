package com.example.gpstracker.server.support

import com.example.gpstracker.model.api.ChatLimits
import com.example.gpstracker.model.api.ChatMessage
import com.example.gpstracker.model.api.ChatSender
import com.example.gpstracker.model.api.ConversationSummary
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Support conversations and their messages. Each driver account has one conversation. */
interface SupportRepository {
    /** The driver's conversation, created on first use. */
    suspend fun conversationOf(accountId: UUID): String

    /** True when a conversation with this id exists. */
    suspend fun exists(conversationId: String): Boolean

    /**
     * Saves a message. A message with a [clientId] already saved in this conversation is not saved
     * again; the stored one is returned, so a device may safely send again after a lost connection.
     */
    suspend fun add(conversationId: String, sender: ChatSender, text: String, clientId: String?): ChatMessage

    /** The latest [limit] messages, oldest first. */
    suspend fun messages(conversationId: String, limit: Int = ChatLimits.HISTORY_SIZE): List<ChatMessage>

    /** Every conversation, the most recently active first. */
    suspend fun conversations(): List<ConversationSummary>
}

/** Conversations in PostgreSQL. JDBC blocks, so every query runs on the IO threads. */
class PostgresSupportRepository(private val dataSource: DataSource) : SupportRepository {
    override suspend fun conversationOf(accountId: UUID): String = db { c ->
        c.prepareStatement(
            "INSERT INTO support_conversations (id, account_id) VALUES (?, ?) ON CONFLICT (account_id) DO NOTHING",
        ).use {
            it.setObject(1, UUID.randomUUID())
            it.setObject(2, accountId)
            it.executeUpdate()
        }
        c.prepareStatement("SELECT id FROM support_conversations WHERE account_id = ?").use {
            it.setObject(1, accountId)
            it.executeQuery().use { rows ->
                rows.next()
                rows.getString("id")
            }
        }
    }

    override suspend fun exists(conversationId: String): Boolean = db { c ->
        val id = conversationId.toUuidOrNull() ?: return@db false
        c.prepareStatement("SELECT 1 FROM support_conversations WHERE id = ?").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> rows.next() }
        }
    }

    override suspend fun add(conversationId: String, sender: ChatSender, text: String, clientId: String?): ChatMessage = db { c ->
        val id = UUID.fromString(conversationId)
        val inserted = c.prepareStatement(
            "INSERT INTO support_messages (conversation_id, sender, text, client_id) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (conversation_id, client_id) WHERE client_id IS NOT NULL DO NOTHING RETURNING *",
        ).use {
            it.setObject(1, id)
            it.setString(2, sender.name)
            it.setString(3, text)
            it.setString(4, clientId)
            it.executeQuery().use { rows -> if (rows.next()) rows.toMessage() else null }
        }
        val message = inserted ?: c.prepareStatement("SELECT * FROM support_messages WHERE conversation_id = ? AND client_id = ?").use {
            it.setObject(1, id)
            it.setString(2, clientId)
            it.executeQuery().use { rows ->
                rows.next()
                rows.toMessage()
            }
        }
        c.prepareStatement("UPDATE support_conversations SET updated_at = now() WHERE id = ?").use {
            it.setObject(1, id)
            it.executeUpdate()
        }
        message
    }

    override suspend fun messages(conversationId: String, limit: Int): List<ChatMessage> = db { c ->
        val id = conversationId.toUuidOrNull() ?: return@db emptyList()
        c.prepareStatement(
            "SELECT * FROM (SELECT * FROM support_messages WHERE conversation_id = ? ORDER BY id DESC LIMIT ?) latest ORDER BY id",
        ).use {
            it.setObject(1, id)
            it.setInt(2, limit)
            it.executeQuery().use { rows -> generateSequence { if (rows.next()) rows.toMessage() else null }.toList() }
        }
    }

    override suspend fun conversations(): List<ConversationSummary> = db { c ->
        c.prepareStatement(
            """
            SELECT c.id, a.name, m.id AS m_id, m.sender, m.text, m.client_id, m.sent_at
            FROM support_conversations c
            JOIN accounts a ON a.id = c.account_id
            LEFT JOIN LATERAL (
                SELECT * FROM support_messages WHERE conversation_id = c.id ORDER BY id DESC LIMIT 1
            ) m ON true
            ORDER BY c.updated_at DESC
            """.trimIndent(),
        ).use {
            it.executeQuery().use { rows ->
                generateSequence {
                    if (!rows.next()) return@generateSequence null
                    val conversationId = rows.getString("id")
                    val last = rows.getObject("m_id")?.let {
                        ChatMessage(
                            id = rows.getLong("m_id"),
                            conversationId = conversationId,
                            sender = ChatSender.valueOf(rows.getString("sender")),
                            text = rows.getString("text"),
                            sentAtMs = rows.getTimestamp("sent_at").time,
                            clientId = rows.getString("client_id"),
                        )
                    }
                    ConversationSummary(conversationId, rows.getString("name"), last, last?.sender == ChatSender.Driver)
                }.toList()
            }
        }
    }

    private fun ResultSet.toMessage() = ChatMessage(
        id = getLong("id"),
        conversationId = getString("conversation_id"),
        sender = ChatSender.valueOf(getString("sender")),
        text = getString("text"),
        sentAtMs = getTimestamp("sent_at").time,
        clientId = getString("client_id"),
    )

    private suspend fun <T> db(block: (Connection) -> T): T = withContext(Dispatchers.IO) { dataSource.connection.use(block) }

    private fun String.toUuidOrNull() = runCatching { UUID.fromString(this) }.getOrNull()
}

/** Conversations in memory, for tests. [names] gives each account's display name. */
class InMemorySupportRepository(
    private val names: suspend (UUID) -> String = { "Driver" },
    private val clock: () -> Long = System::currentTimeMillis,
) : SupportRepository {
    private class Conversation(val id: String, val accountId: UUID, var updatedAt: Long)

    private val conversations = LinkedHashMap<UUID, Conversation>()
    private val messages = mutableListOf<ChatMessage>()
    private var nextId = 1L

    override suspend fun conversationOf(accountId: UUID): String = synchronized(this) {
        conversations.getOrPut(accountId) { Conversation(UUID.randomUUID().toString(), accountId, clock()) }.id
    }

    override suspend fun exists(conversationId: String): Boolean = synchronized(this) {
        conversations.values.any { it.id == conversationId }
    }

    override suspend fun add(conversationId: String, sender: ChatSender, text: String, clientId: String?): ChatMessage = synchronized(
        this,
    ) {
        messages.firstOrNull { clientId != null && it.conversationId == conversationId && it.clientId == clientId }?.let { return it }
        val now = clock()
        conversations.values.first { it.id == conversationId }.updatedAt = now
        ChatMessage(nextId++, conversationId, sender, text, now, clientId).also { messages += it }
    }

    override suspend fun messages(conversationId: String, limit: Int): List<ChatMessage> =
        synchronized(this) { messages.filter { it.conversationId == conversationId }.takeLast(limit) }

    override suspend fun conversations(): List<ConversationSummary> {
        val list = synchronized(this) {
            conversations.values.sortedByDescending { it.updatedAt }.map {
                it to
                    messages.lastOrNull { m -> m.conversationId == it.id }
            }
        }
        return list.map { (c, last) -> ConversationSummary(c.id, names(c.accountId), last, last?.sender == ChatSender.Driver) }
    }
}
