package com.example.gpstracker.server.reports

import com.example.gpstracker.model.geo.LatLon
import com.example.gpstracker.model.reports.ReportSource
import com.example.gpstracker.model.reports.ReportType
import com.example.gpstracker.model.reports.RoadReport
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A report as the server keeps it: who made it is stored, but never sent to other drivers. */
data class StoredReport(val report: RoadReport, val reporterId: UUID?)

/** An area to look for reports in: [radiusKm] around [center]. */
data class Area(val center: LatLon, val radiusKm: Double)

interface ReportStore {
    /** Saves a new report. A report with the same id is kept as it is and returned. */
    suspend fun insert(report: RoadReport, reporterId: UUID?): StoredReport

    suspend fun find(id: String): StoredReport?

    /** Replaces a report completely, with its votes (for the demo reports). */
    suspend fun replace(report: RoadReport)

    /** Active reports at [nowMs], newest first. */
    suspend fun active(nowMs: Long, area: Area?, limit: Int): List<StoredReport>

    /**
     * Records [accountId]'s vote and lets [change] update the report, in one step. A second vote
     * by the same account changes nothing. Returns null when the report does not exist.
     */
    suspend fun vote(id: String, accountId: UUID, stillThere: Boolean, change: (RoadReport) -> RoadReport): StoredReport?
}

private const val KM_PER_DEGREE = 111.2

private fun Area.box(): DoubleArray {
    val dLat = radiusKm / KM_PER_DEGREE
    val dLon = radiusKm / (KM_PER_DEGREE * cos(Math.toRadians(center.latitude)).coerceAtLeast(MIN_COS))
    return doubleArrayOf(center.latitude - dLat, center.latitude + dLat, center.longitude - dLon, center.longitude + dLon)
}

private const val MIN_COS = 0.01

/** True inside a box of [south, north, west, east]. */
private fun LatLon.isIn(box: DoubleArray) = latitude in box[0]..box[1] && longitude in box[2]..box[3]

/** Reports in PostgreSQL. JDBC blocks, so every query runs on the IO threads. */
class PostgresReportStore(private val dataSource: DataSource) : ReportStore {
    override suspend fun insert(report: RoadReport, reporterId: UUID?): StoredReport = db { c ->
        c.prepareStatement(
            "INSERT INTO road_reports (id, reporter_id, type, latitude, longitude, heading_deg, road_name, created_at, expires_at, " +
                "still_there_votes, gone_votes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
        ).use {
            it.setString(1, report.id)
            it.setObject(2, reporterId)
            it.setString(3, report.type.name)
            it.setDouble(4, report.position.latitude)
            it.setDouble(5, report.position.longitude)
            it.setObject(6, report.headingDeg)
            it.setString(7, report.roadName)
            it.setTimestamp(8, Timestamp(report.createdAtMs))
            it.setTimestamp(9, Timestamp(report.expiresAtMs))
            it.setInt(10, report.stillThereVotes)
            it.setInt(11, report.goneVotes)
            it.executeUpdate()
        }
        c.find(report.id)!!
    }

    override suspend fun find(id: String): StoredReport? = db { it.find(id) }

    override suspend fun replace(report: RoadReport) {
        // its votes go with it (ON DELETE CASCADE)
        db { c ->
            c.prepareStatement("DELETE FROM road_reports WHERE id = ?").use {
                it.setString(1, report.id)
                it.executeUpdate()
            }
        }
        insert(report, null)
    }

    override suspend fun active(nowMs: Long, area: Area?, limit: Int): List<StoredReport> = db { c ->
        val where = "expires_at > ? AND gone_votes <= still_there_votes" +
            if (area != null) " AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?" else ""
        c.prepareStatement("SELECT * FROM road_reports WHERE $where ORDER BY created_at DESC LIMIT ?").use {
            var i = 1
            it.setTimestamp(i++, Timestamp(nowMs))
            area?.box()?.forEach { value -> it.setDouble(i++, value) }
            it.setInt(i, limit)
            it.executeQuery().use { rows -> generateSequence { if (rows.next()) rows.toStored() else null }.toList() }
        }
    }

    override suspend fun vote(
        id: String,
        accountId: UUID,
        stillThere: Boolean,
        change: (RoadReport) -> RoadReport,
    ): StoredReport? = db { c ->
        c.autoCommit = false
        try {
            // lock the report, so two votes at the same moment are both counted
            val current = c.prepareStatement("SELECT * FROM road_reports WHERE id = ? FOR UPDATE").use {
                it.setString(1, id)
                it.executeQuery().use { rows -> if (rows.next()) rows.toStored() else null }
            }
            val result = current?.let { stored ->
                val counted = c.prepareStatement(
                    "INSERT INTO road_report_votes (report_id, account_id, still_there) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                ).use {
                    it.setString(1, id)
                    it.setObject(2, accountId)
                    it.setBoolean(3, stillThere)
                    it.executeUpdate() == 1
                }
                if (counted) stored.copy(report = change(stored.report)).also { c.save(it.report) } else stored
            }
            c.commit()
            result
        } catch (e: java.sql.SQLException) {
            c.rollback()
            throw e
        } finally {
            c.autoCommit = true
        }
    }

    private fun Connection.save(report: RoadReport) {
        prepareStatement("UPDATE road_reports SET expires_at = ?, still_there_votes = ?, gone_votes = ? WHERE id = ?").use {
            it.setTimestamp(1, Timestamp(report.expiresAtMs))
            it.setInt(2, report.stillThereVotes)
            it.setInt(3, report.goneVotes)
            it.setString(4, report.id)
            it.executeUpdate()
        }
    }

    private fun Connection.find(id: String): StoredReport? = prepareStatement("SELECT * FROM road_reports WHERE id = ?").use {
        it.setString(1, id)
        it.executeQuery().use { rows -> if (rows.next()) rows.toStored() else null }
    }

    private fun ResultSet.toStored() = StoredReport(
        report = RoadReport(
            id = getString("id"),
            type = ReportType.valueOf(getString("type")),
            position = LatLon(getDouble("latitude"), getDouble("longitude")),
            createdAtMs = getTimestamp("created_at").time,
            expiresAtMs = getTimestamp("expires_at").time,
            source = ReportSource.OtherDriver,
            headingDeg = getDouble("heading_deg").takeUnless { wasNull() },
            roadName = getString("road_name"),
            stillThereVotes = getInt("still_there_votes"),
            goneVotes = getInt("gone_votes"),
        ),
        reporterId = getObject("reporter_id") as UUID?,
    )

    private suspend fun <T> db(block: (Connection) -> T): T = withContext(Dispatchers.IO) { dataSource.connection.use(block) }
}

/** Reports in memory, for tests. */
class InMemoryReportStore : ReportStore {
    private val reports = LinkedHashMap<String, StoredReport>()
    private val votes = HashSet<Pair<String, UUID>>()

    override suspend fun insert(report: RoadReport, reporterId: UUID?): StoredReport =
        synchronized(this) { reports.getOrPut(report.id) { StoredReport(report.copy(source = ReportSource.OtherDriver), reporterId) } }

    override suspend fun find(id: String): StoredReport? = synchronized(this) { reports[id] }

    override suspend fun replace(report: RoadReport) = synchronized(this) {
        votes.removeAll { it.first == report.id }
        reports[report.id] = StoredReport(report.copy(source = ReportSource.OtherDriver), null)
    }

    override suspend fun active(nowMs: Long, area: Area?, limit: Int): List<StoredReport> = synchronized(this) {
        val box = area?.box()
        reports.values
            .filter { it.report.isActive(nowMs) }
            .filter { box == null || it.report.position.isIn(box) }
            .sortedByDescending { it.report.createdAtMs }
            .take(limit)
    }

    override suspend fun vote(id: String, accountId: UUID, stillThere: Boolean, change: (RoadReport) -> RoadReport): StoredReport? =
        synchronized(this) {
            val stored = reports[id] ?: return null
            if (!votes.add(id to accountId)) return stored
            stored.copy(report = change(stored.report)).also { reports[id] = it }
        }
}
