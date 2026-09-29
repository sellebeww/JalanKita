package org.umn.jalankita.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.umn.jalankita.data.local.NotificationDao
import org.umn.jalankita.data.local.NotificationEntity
import org.umn.jalankita.data.local.ReportDao
import org.umn.jalankita.data.local.ReportEntity
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.ConnectivityMonitor

/** In-memory stand-ins so the offline-first rules can be tested off-device. */
class FakeReportDao : ReportDao {

    val rows = MutableStateFlow<Map<String, ReportEntity>>(emptyMap())

    private fun mutate(block: (MutableMap<String, ReportEntity>) -> Unit) {
        rows.value = rows.value.toMutableMap().apply(block)
    }

    override suspend fun insertReport(report: ReportEntity) = mutate { it[report.id] = report }

    override suspend fun upsertAll(reports: List<ReportEntity>) =
        mutate { map -> reports.forEach { map[it.id] = it } }

    override suspend fun updateReport(report: ReportEntity) = mutate { it[report.id] = report }

    override suspend fun deleteReport(reportId: String) = mutate { it.remove(reportId); Unit }

    override suspend fun getReportById(reportId: String): ReportEntity? = rows.value[reportId]

    override suspend fun getReportsByUser(userId: String): List<ReportEntity> =
        rows.value.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override suspend fun getPendingSyncReports(pendingStatuses: List<String>): List<ReportEntity> =
        rows.value.values.filter { it.syncStatus in pendingStatuses }.sortedBy { it.createdAt }

    override fun observeReportsByUser(userId: String): Flow<List<ReportEntity>> =
        rows.map { map -> map.values.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override fun observeAllReports(): Flow<List<ReportEntity>> =
        rows.map { map -> map.values.sortedByDescending { it.createdAt } }

    override fun observeReportsByReporter(reporterId: String): Flow<List<ReportEntity>> =
        observeReportsByUser(reporterId)

    override fun observeReportById(reportId: String): Flow<ReportEntity?> =
        rows.map { it[reportId] }

    override fun observeUnsyncedCount(userId: String, syncedStatus: String): Flow<Int> =
        rows.map { map -> map.values.count { it.userId == userId && it.syncStatus != syncedStatus } }

    override suspend fun updateSyncStatus(reportId: String, syncStatus: String, updatedAt: Long) =
        mutate { map ->
            map[reportId]?.let { map[reportId] = it.copy(syncStatus = syncStatus, updatedAt = updatedAt) }
        }

    override suspend fun markSyncFailure(
        reportId: String,
        syncStatus: String,
        error: String?,
        updatedAt: Long,
    ) = mutate { map ->
        map[reportId]?.let {
            map[reportId] = it.copy(
                syncStatus = syncStatus,
                syncAttempts = it.syncAttempts + 1,
                lastSyncError = error,
                updatedAt = updatedAt,
            )
        }
    }

    override suspend fun markSynced(
        reportId: String,
        syncStatus: String,
        photoRemoteUrl: String?,
        updatedAt: Long,
    ) = mutate { map ->
        map[reportId]?.let {
            map[reportId] = it.copy(
                syncStatus = syncStatus,
                photoRemoteUrl = photoRemoteUrl,
                lastSyncError = null,
                updatedAt = updatedAt,
            )
        }
    }

    override suspend fun updateReportStatus(reportId: String, reportStatus: String, updatedAt: Long) =
        mutate { map ->
            map[reportId]?.let {
                map[reportId] = it.copy(reportStatus = reportStatus, updatedAt = updatedAt)
            }
        }

    override suspend fun backfillReporterName(userId: String, name: String) = mutate { map ->
        map.values.filter { it.userId == userId && it.reporterName.isNullOrBlank() }
            .forEach { map[it.id] = it.copy(reporterName = name) }
    }

    override suspend fun fillMissingStreetName(reportId: String, streetName: String) = mutate { map ->
        map[reportId]?.let { if (it.streetName == null) map[reportId] = it.copy(streetName = streetName) }
    }

    override suspend fun getReportsWithoutStreetName(userId: String): List<ReportEntity> =
        rows.value.values.filter { it.userId == userId && it.streetName == null }
}

class FakeNotificationDao : NotificationDao {

    val rows = MutableStateFlow<List<NotificationEntity>>(emptyList())

    // Room's OnConflictStrategy.REPLACE: same id overwrites rather than appends.
    override suspend fun insert(notification: NotificationEntity) = insertAll(listOf(notification))

    override suspend fun insertAll(notifications: List<NotificationEntity>) {
        val incoming = notifications.associateBy { it.id }
        rows.value = rows.value.filterNot { it.id in incoming } + notifications
    }

    override fun observeByUser(userId: String): Flow<List<NotificationEntity>> =
        rows.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override fun observeUnreadCount(userId: String): Flow<Int> =
        rows.map { list -> list.count { it.userId == userId && !it.isRead } }

    override suspend fun markAsRead(notificationId: String) {
        rows.value = rows.value.map { if (it.id == notificationId) it.copy(isRead = true) else it }
    }

    override suspend fun markAllAsRead(userId: String) {
        rows.value = rows.value.map { if (it.userId == userId) it.copy(isRead = true) else it }
    }

    override suspend fun clearForUser(userId: String) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }
}

/** In-memory reporter store with the same recompute-from-rows semantics as Room. */
class FakeReporterDao(private val reportDao: FakeReportDao) : org.umn.jalankita.data.local.ReporterDao {

    val rows = MutableStateFlow<Map<String, org.umn.jalankita.data.local.ReporterEntity>>(emptyMap())

    override suspend fun upsert(reporter: org.umn.jalankita.data.local.ReporterEntity) =
        upsertAll(listOf(reporter))

    override suspend fun upsertAll(reporters: List<org.umn.jalankita.data.local.ReporterEntity>) {
        rows.value = rows.value + reporters.associateBy { it.id }
    }

    override fun observeReporter(reporterId: String): Flow<org.umn.jalankita.data.local.ReporterEntity?> =
        rows.map { it[reporterId] }

    override suspend fun getReporter(reporterId: String) = rows.value[reporterId]

    override suspend fun refreshFromReports(reporterId: String, name: String, now: Long) {
        val mine = reportDao.rows.value.values.filter { it.userId == reporterId }
        rows.value = rows.value + (
            reporterId to org.umn.jalankita.data.local.ReporterEntity(
                id = reporterId,
                name = name,
                joinedAt = rows.value[reporterId]?.joinedAt ?: now,
                totalReports = mine.size,
                confirmedReports = mine.count {
                    it.reportStatus in setOf("VERIFIED", "IN_PROGRESS", "COMPLETED")
                },
                completedReports = mine.count { it.reportStatus == "COMPLETED" },
                needsInfoReports = mine.count { it.reportStatus == "NEEDS_INFO" },
            )
            )
    }
}

class FakeConnectivityMonitor(private var online: Boolean) : ConnectivityMonitor {
    private val state = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = state
    override fun currentlyOnline(): Boolean = online

    fun setOnline(value: Boolean) {
        online = value
        state.value = value
    }
}

/** Records what the repository asked for without performing any network work. */
class RecordingSyncTrigger(
    private val syncSucceeds: Boolean = false,
) : org.umn.jalankita.data.repository.ReportSyncTrigger {

    var requestSyncCalls = 0
        private set
    val syncNowCalls = mutableListOf<String>()

    override fun requestSync() {
        requestSyncCalls++
    }

    override suspend fun syncNow(reportId: String): Boolean {
        syncNowCalls += reportId
        return syncSucceeds
    }
}

val SyncStatusPending: String = SyncStatus.PENDING_SYNC.id
