package com.example.gpstracker.data.trips

import com.example.gpstracker.data.local.TripSyncStore
import com.example.gpstracker.data.remote.ApiClient
import com.example.gpstracker.data.remote.ApiResult
import com.example.gpstracker.data.support.TokenProvider
import com.example.gpstracker.tracing.Tracing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps a signed-in driver's trips on the server, so they follow the account to another phone.
 *
 * While someone is signed in it sends the trips the server does not have yet, then fetches the
 * account's trips from the server into the phone's history. It runs when the account changes,
 * right after a trip is saved ([requestSync]) and every [intervalMs] otherwise, so trips made
 * without a connection go out later. Demo mode never syncs.
 */
class TripSync(
    private val api: ApiClient,
    private val store: TripSyncStore,
    private val accountId: StateFlow<String?>,
    private val tokens: TokenProvider,
    private val scope: CoroutineScope,
    private val intervalMs: Long = SYNC_INTERVAL_MS,
) {
    private val tracer = Tracing.tracer("app")
    private val wake = Channel<Unit>(Channel.CONFLATED)

    fun start() {
        scope.launch {
            accountId.collectLatest { owner ->
                if (owner == null) return@collectLatest
                while (currentCoroutineContext().isActive) {
                    sync(owner)
                    withTimeoutOrNull(intervalMs) { wake.receive() }
                }
            }
        }
    }

    /** Syncs soon, for example because a trip was just saved. */
    fun requestSync() {
        wake.trySend(Unit)
    }

    /** One round for [owner]. Returns false when the server could not be reached. */
    suspend fun sync(owner: String): Boolean {
        val token = tokens.currentToken() ?: return false
        for (trip in store.unsynced(owner)) {
            when (val result = api.uploadTrip(trip, token)) {
                is ApiResult.Success -> store.markSynced(listOf(trip.id))

                is ApiResult.Failure -> {
                    tracer.warn("trip not accepted by the server", "status" to result.status)
                    // refused for good: stop sending it, it stays on this phone
                    if (result.status in CLIENT_ERRORS) store.markSynced(listOf(trip.id))
                }

                ApiResult.Unreachable -> return false
            }
        }
        return when (val result = api.trips(token)) {
            is ApiResult.Success -> {
                store.saveFromServer(owner, result.value)
                true
            }

            is ApiResult.Failure -> {
                tracer.warn("trips could not be fetched", "status" to result.status)
                true
            }

            ApiResult.Unreachable -> false
        }
    }

    companion object {
        const val SYNC_INTERVAL_MS = 120_000L
        private val CLIENT_ERRORS = 400..499
    }
}
