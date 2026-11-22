package com.example.gpstracker.data.reports

import com.example.gpstracker.data.remote.ApiClient
import com.example.gpstracker.data.remote.ApiResult
import com.example.gpstracker.data.support.TokenProvider
import com.example.gpstracker.model.api.NewReportRequest
import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.tracing.Tracing
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Road reports shared with other drivers through the server.
 *
 * The phone keeps working on its own copy ([local]), so warnings never wait for the network.
 * Every [intervalMs] it sends what the driver reported or answered and fetches the reports
 * around the car. The server's list then replaces the copy, so reports that expired or were
 * voted away elsewhere disappear here too. Without a connection the copy simply stays, and
 * whatever was not sent yet goes out on the next try.
 *
 * In demo mode (no account) reports stay on this phone, but the other drivers' reports still
 * arrive.
 */
class SharedReportRepository(
    private val local: StoredReportRepository,
    private val api: ApiClient,
    private val tokens: TokenProvider,
    private val scope: CoroutineScope,
    private val position: () -> LatLon?,
    private val intervalMs: Long = SYNC_INTERVAL_MS,
) : ReportRepository {
    private val tracer = Tracing.tracer("app")

    /** Own reports the server has not confirmed yet. */
    private val unsent = ConcurrentHashMap<String, RoadReport>()

    /** "Still there?" answers the server has not confirmed yet, by report id. */
    private val unsentVotes = ConcurrentHashMap<String, Boolean>()

    /** Reports made in demo mode; they are never shared, but stay on this phone. */
    private val localOnly = ConcurrentHashMap.newKeySet<String>()

    private var job: Job? = null

    override val reports: StateFlow<List<RoadReport>> = local.reports

    /** Starts sharing in the background. Call once. */
    fun start() {
        job = scope.launch {
            while (currentCoroutineContext().isActive) {
                sync()
                delay(intervalMs)
            }
        }
    }

    override fun add(report: RoadReport) {
        local.add(report)
        if (tokens.currentToken() == null) {
            localOnly += report.id
        } else {
            unsent[report.id] = report
            scope.launch { sendReports() }
        }
    }

    override fun vote(id: String, stillThere: Boolean) {
        local.vote(id, stillThere)
        if (tokens.currentToken() != null && id !in localOnly) {
            unsentVotes[id] = stillThere
            scope.launch { sendVotes() }
        }
    }

    override fun removeInactive() = local.removeInactive()

    /** One round: send what is waiting, then fetch the reports around the car. */
    suspend fun sync() {
        sendReports()
        sendVotes()
        when (val result = api.reports(position(), tokens.currentToken())) {
            is ApiResult.Success -> local.applyServer(result.value, unsent.keys + localOnly)
            is ApiResult.Failure -> tracer.warn("shared reports refused", "status" to result.status)
            ApiResult.Unreachable -> Unit // offline: keep what we have
        }
    }

    private suspend fun sendReports() {
        val token = tokens.currentToken() ?: return
        for (report in unsent.values) {
            val request = NewReportRequest(report.id, report.type, report.position, report.headingDeg, report.roadName)
            when (val result = api.shareReport(request, token)) {
                is ApiResult.Success -> {
                    unsent.remove(report.id)
                    tracer.info("road report shared", "type" to report.type)
                }

                // refused for good (for example a position the server does not accept): keep it on the phone only
                is ApiResult.Failure -> if (result.status in CLIENT_ERRORS) {
                    unsent.remove(report.id)
                    localOnly += report.id
                }

                ApiResult.Unreachable -> return
            }
        }
    }

    private suspend fun sendVotes() {
        val token = tokens.currentToken() ?: return
        for ((id, stillThere) in unsentVotes) {
            if (unsent.containsKey(id)) continue // the report itself goes first
            when (val result = api.voteOnReport(id, stillThere, token)) {
                is ApiResult.Success -> unsentVotes.remove(id)

                // the report is gone on the server, or the answer was refused: nothing more to send
                is ApiResult.Failure -> if (result.status in CLIENT_ERRORS) unsentVotes.remove(id)

                ApiResult.Unreachable -> return
            }
        }
    }

    companion object {
        const val SYNC_INTERVAL_MS = 30_000L
        private val CLIENT_ERRORS = 400..499
    }
}
