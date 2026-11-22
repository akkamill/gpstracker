package com.example.gpstracker.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.gpstracker.data.local.ReportDao
import com.example.gpstracker.data.local.RoadReportEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart

/** Stand-ins for the database and preference files, so storage logic runs in plain unit tests. */
class FakeReportDao(initial: List<RoadReportEntity> = emptyList()) : ReportDao {
    var rows: List<RoadReportEntity> = initial
    var writes = 0

    override suspend fun all(): List<RoadReportEntity> = rows.sortedBy { it.createdAtMs }

    override suspend fun insertAll(reports: List<RoadReportEntity>) {
        rows = (rows.filter { r -> reports.none { it.id == r.id } } + reports)
        writes++
    }

    override suspend fun deleteAll() {
        rows = emptyList()
    }
}

/**
 * A preferences file in memory. Reading can be held back with [holdReads], to test what
 * happens when the user acts before the file has been read.
 */
class FakePreferencesStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    private var gate: CompletableDeferred<Unit>? = null

    fun holdReads() {
        gate = CompletableDeferred()
    }

    fun releaseReads() {
        gate?.complete(Unit)
    }

    override val data: Flow<Preferences> = state.onStart { gate?.await() }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}
