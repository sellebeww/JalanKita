package org.umn.jalankita.data.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.umn.jalankita.data.local.NotificationDao
import org.umn.jalankita.data.local.NotificationEntity
import org.umn.jalankita.data.local.ReportDao
import org.umn.jalankita.data.repository.ReportSyncTrigger
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.ConnectivityMonitor
import org.umn.jalankita.util.NotificationKind
import org.umn.jalankita.util.StatusNotifier
import java.util.UUID

/**
 * Stand-in for [org.umn.jalankita.data.worker.ReportSyncEngine] when the app runs
 * without Supabase credentials.
 *
 * Without it, demo builds left every report stuck on "Menunggu Sinkronisasi"
 * forever and the "Sinkronkan" button did nothing — which looked like a bug
 * rather than a missing backend. This walks a report through the same states the
 * real engine does, and keeps the rule that matters: **offline stays
 * PENDING_SYNC, never FAILED**, and nothing is uploaded anywhere.
 */
class DemoSyncEngine(
    private val reportDao: ReportDao,
    private val notificationDao: NotificationDao,
    private val networkMonitor: ConnectivityMonitor,
    private val notificationPresenter: StatusNotifier,
) {

    /** Returns how many reports reached "SYNCED" in this pass. */
    suspend fun syncAll(): Int {
        if (!networkMonitor.currentlyOnline()) return 0
        return reportDao.getPendingSyncReports().count { syncReport(it.id) }
    }

    suspend fun syncReport(reportId: String): Boolean {
        val entity = reportDao.getReportById(reportId) ?: return false
        if (entity.syncStatus == SyncStatus.SYNCED.id) return true
        if (!networkMonitor.currentlyOnline()) return false

        reportDao.updateSyncStatus(reportId, SyncStatus.SYNCING.id, System.currentTimeMillis())
        // Long enough for the "Sedang Disinkronkan" badge to actually be seen.
        delay(UPLOAD_SIMULATION_MILLIS)

        reportDao.markSynced(
            reportId = reportId,
            syncStatus = SyncStatus.SYNCED.id,
            photoRemoteUrl = null,
            updatedAt = System.currentTimeMillis(),
        )

        val title = "Laporan berhasil dikirim"
        val message = "Laporan Anda sudah tercatat dan menunggu verifikasi petugas."
        notificationDao.insert(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = entity.userId,
                title = title,
                message = message,
                reportId = reportId,
                isRead = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
        notificationPresenter.show(title, message, reportId, NotificationKind.SYNC)
        return true
    }

    private companion object {
        const val UPLOAD_SIMULATION_MILLIS = 900L
    }
}

/**
 * Wires [DemoSyncEngine] into the same seam the WorkManager trigger uses, so
 * `ReportRepository` behaves identically whether or not a backend is configured.
 */
class DemoSyncTrigger(
    private val scope: CoroutineScope,
    private val engine: DemoSyncEngine,
) : ReportSyncTrigger {

    override fun requestSync() {
        scope.launch { engine.syncAll() }
    }

    override suspend fun syncNow(reportId: String): Boolean = engine.syncReport(reportId)
}
