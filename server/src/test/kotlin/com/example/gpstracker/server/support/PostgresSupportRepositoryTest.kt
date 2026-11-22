package com.example.gpstracker.server.support

import com.example.gpstracker.model.api.ChatSender
import com.example.gpstracker.server.accounts.Account
import com.example.gpstracker.server.accounts.PostgresAccountRepository
import com.example.gpstracker.server.db.TestPostgres
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Support chat storage against PostgreSQL in Docker (skipped without Docker). */
class PostgresSupportRepositoryTest {
    private val dataSource by lazy { TestPostgres.dataSource() }
    private val repo by lazy { PostgresSupportRepository(dataSource) }

    private suspend fun newDriver(name: String): UUID {
        val id = UUID.randomUUID()
        PostgresAccountRepository(dataSource).insert(Account(id, "$id@example.com", name, "hash", "salt"))
        return id
    }

    @Test
    fun eachDriverHasOneConversation() = runBlocking {
        val driver = newDriver("Ona")
        val conversation = repo.conversationOf(driver)
        assertEquals(conversation, repo.conversationOf(driver))
        assertTrue(repo.exists(conversation))
        assertFalse(repo.exists(UUID.randomUUID().toString()))
        assertFalse(repo.exists("not-an-id"))
    }

    @Test
    fun messagesComeBackInOrderWithTheirSender() = runBlocking {
        val conversation = repo.conversationOf(newDriver("Ona"))
        repo.add(conversation, ChatSender.Driver, "first", "c1")
        val answer = repo.add(conversation, ChatSender.Agent, "second", null)
        assertEquals(listOf("first", "second"), repo.messages(conversation).map { it.text })
        assertEquals(listOf("second"), repo.messages(conversation, limit = 1).map { it.text })
        assertEquals(ChatSender.Agent, answer.sender)
        assertEquals("c1", repo.messages(conversation).first().clientId)
    }

    @Test
    fun aMessageSentAgainIsStoredOnce() = runBlocking {
        val conversation = repo.conversationOf(newDriver("Ona"))
        val first = repo.add(conversation, ChatSender.Driver, "hello", "same-id")
        val again = repo.add(conversation, ChatSender.Driver, "hello", "same-id")
        assertEquals(first, again)
        assertEquals(1, repo.messages(conversation).size)
    }

    @Test
    fun theTeamSeesTheLatestActivityFirstAndWhoWaits() = runBlocking {
        val older = repo.conversationOf(newDriver("Older"))
        val newer = repo.conversationOf(newDriver("Newer"))
        repo.add(older, ChatSender.Driver, "hello", null)
        repo.add(newer, ChatSender.Driver, "hi", null)
        repo.add(newer, ChatSender.Agent, "hi, how can we help?", null)

        val list = repo.conversations()
        val newerSummary = list.first { it.id == newer }
        val olderSummary = list.first { it.id == older }
        assertTrue(list.indexOf(newerSummary) < list.indexOf(olderSummary))
        assertEquals("Newer", newerSummary.driverName)
        assertFalse(newerSummary.waitingForAgent)
        assertTrue(olderSummary.waitingForAgent)
        assertEquals("hello", olderSummary.lastMessage?.text)
    }
}
