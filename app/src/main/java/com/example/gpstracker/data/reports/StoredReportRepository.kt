package com.example.gpstracker.data.reports

import com.example.gpstracker.data.local.ReportDao
import com.example.gpstracker.data.local.toEntity
import com.example.gpstracker.data.local.toModel
import com.example.gpstracker.model.reports.RoadReport
import com.example.gpstracker.tracing.Tracing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Road reports that survive an app restart.
 *
 * The list in memory is what the drive uses on every sample, so reading stays instant.
 * The database gets a copy after every change: the list is written as a whole, and only
 * the latest list counts, so a burst of changes ends in one write.
 *
 * @param demo reports of "other drivers" added on every start until the server shares real ones
 */
class StoredReportRepository(
    private val dao: ReportDao,
    scope: CoroutineScope,
    demo: List<RoadReport> = emptyList(),
    private val memory: InMemoryReportRepository = InMemoryReportRepository(),
) : ReportRepository by memory {
    private val tracer = Tracing.tracer("app")

    init {
        scope.launch {
            val saved = dao.all().map { it.toModel() }
            memory.restore(saved + demo)
            tracer.info("road reports loaded", "saved" to saved.size, "active" to memory.reports.value.size)
            // the first list written is the one just restored, then every later change
            memory.reports.collect { reports -> dao.replaceAll(reports.map { it.toEntity() }) }
        }
    }
}
