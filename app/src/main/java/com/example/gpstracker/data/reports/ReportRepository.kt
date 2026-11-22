package com.example.gpstracker.data.reports

import com.example.gpstracker.domain.reports.ReportRules
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.tracing.Tracing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Road reports from this driver and others. The server shares them between drivers later. */
interface ReportRepository {
    /** Reports that are still active, oldest first. */
    val reports: StateFlow<List<RoadReport>>

    fun add(report: RoadReport)

    /** Records one driver's "still there" or "gone" answer. */
    fun vote(id: String, stillThere: Boolean)

    /** Drops reports that expired or were voted away. */
    fun removeInactive()
}

/** Keeps reports in memory on this phone. */
class InMemoryReportRepository(
    initial: List<RoadReport> = emptyList(),
    private val rules: ReportRules = ReportRules(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ReportRepository {
    private val tracer = Tracing.tracer("app")
    private val _reports = MutableStateFlow(initial)
    override val reports: StateFlow<List<RoadReport>> = _reports.asStateFlow()

    override fun add(report: RoadReport) {
        _reports.update { it + report }
        tracer.info("road report added", "type" to report.type, "road" to report.roadName, "source" to report.source)
    }

    /**
     * Adds reports loaded from storage. When two have the same id, the one already in memory
     * wins, then the one earlier in [saved].
     */
    fun restore(saved: List<RoadReport>) {
        _reports.update { current -> (current + saved).distinctBy { it.id }.sortedBy { it.createdAtMs } }
        removeInactive()
    }

    override fun vote(id: String, stillThere: Boolean) {
        val now = clock()
        _reports.update { list -> list.map { if (it.id == id) rules.vote(it, stillThere, now) else it } }
        tracer.info("road report answered", "id" to id, "stillThere" to stillThere)
        removeInactive()
    }

    override fun removeInactive() {
        val now = clock()
        val current = _reports.value
        val active = current.filter { it.isActive(now) }
        if (active.size != current.size) {
            _reports.value = active
            tracer.info("road reports removed", "count" to current.size - active.size)
        }
    }
}
