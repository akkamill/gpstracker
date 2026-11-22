package com.example.gpstracker.data.trips

import com.example.gpstracker.data.local.TripSyncStore
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.simulator.history.DemoHistory
import com.example.gpstracker.testing.FakeServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripSyncTest {
    /** The phone's side, in memory: trips per owner and whether the server has them. */
    private class FakeStore : TripSyncStore {
        val trips = HashMap<String, MutableMap<String, Pair<SavedTrip, Boolean>>>()

        fun add(owner: String, trip: SavedTrip, synced: Boolean = false) {
            trips.getOrPut(owner) { LinkedHashMap() }[trip.id] = trip to synced
        }

        fun ids(owner: String) = trips[owner].orEmpty().keys

        override suspend fun unsynced(owner: String) = trips[owner].orEmpty().values.filter { !it.second }.map { it.first }

        override suspend fun markSynced(ids: List<String>) = trips.values.forEach { m ->
            ids.forEach { id ->
                m[id]?.let {
                    m[id] =
                        it.first to true
                }
            }
        }

        override suspend fun saveFromServer(owner: String, trips: List<SavedTrip>) =
            trips.forEach { if (it.id !in ids(owner)) add(owner, it, synced = true) }
    }

    private val server = FakeServer().apply { addAccount("Ona", "ona@example.com", "abcd1234") }
    private val store = FakeStore()
    private val account = MutableStateFlow<String?>("id-1")
    private var token: String? = "token-ona@example.com"
    private val history = DemoHistory.build(nowMs = 1_700_000_000_000L)

    private fun TestScope.sync() = TripSync(server.api, store, account, { token }, backgroundScope)

    @Test
    fun `trips made on this phone go to the server once`() = runTest {
        store.add("id-1", history[0])
        store.add("id-1", history[1])
        assertTrue(sync().sync("id-1"))
        assertEquals(setOf(history[0].id, history[1].id), server.trips.getValue("ona@example.com").keys)
        assertTrue(store.unsynced("id-1").isEmpty())

        server.calls.clear()
        sync().sync("id-1")
        assertTrue("nothing sent again", server.calls.none { it.startsWith("/api/trips/") })
    }

    @Test
    fun `trips from another phone arrive`() = runTest {
        server.trips["ona@example.com"] = LinkedHashMap(history.take(3).associateBy { it.id })
        sync().sync("id-1")
        assertEquals(history.take(3).map { it.id }.toSet(), store.ids("id-1"))
    }

    @Test
    fun `without a connection trips wait and go out later`() = runTest {
        store.add("id-1", history[0])
        server.reachable = false
        assertFalse(sync().sync("id-1"))
        assertEquals(1, store.unsynced("id-1").size)
        server.reachable = true
        assertTrue(sync().sync("id-1"))
        assertTrue(store.unsynced("id-1").isEmpty())
    }

    @Test
    fun `demo mode never syncs, and signing in starts at once`() = runTest {
        account.value = null
        token = null
        store.add("guest", history[0])
        val sync = sync()
        sync.start()
        runCurrent()
        assertTrue(server.calls.isEmpty())

        token = "token-ona@example.com"
        store.add("id-1", history[1])
        account.value = "id-1"
        runCurrent()
        assertEquals(setOf(history[1].id), server.trips.getValue("ona@example.com").keys)
    }

    @Test
    fun `a saved trip is sent right away, and the rest on a timer`() = runTest {
        val sync = sync()
        sync.start()
        runCurrent()
        store.add("id-1", history[0])
        sync.requestSync()
        runCurrent()
        assertTrue(history[0].id in server.trips.getValue("ona@example.com"))

        store.add("id-1", history[1])
        advanceTimeBy(TripSync.SYNC_INTERVAL_MS + 1)
        assertTrue(history[1].id in server.trips.getValue("ona@example.com"))
    }
}
