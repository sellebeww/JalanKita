package org.umn.jalankita.data.worker

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.umn.jalankita.data.local.NotificationDao
import org.umn.jalankita.data.local.NotificationEntity
import org.umn.jalankita.data.local.ReportDao
import org.umn.jalankita.data.local.toDomain
import org.umn.jalankita.data.location.LocationProvider
import org.umn.jalankita.data.media.PhotoStorage
import org.umn.jalankita.data.remote.SupabaseRepository
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.ConnectivityMonitor
import org.umn.jalankita.util.NotificationKind
import org.umn.jalankita.util.StatusNotifier
import java.io.File
import java.util.UUID

data class SyncSummary(
    val attempted: Int = 0,
    val succeeded: Int = 0,
    val failed: Int = 0,
) {
    val shouldRetry: Boolean get() = failed > 0
}

/**
 * The actual upload logic, shared by the WorkManager worker and the immediate
 * "send it now" path so both behave identically.
 *
 * Invariants:
 *  - a local report is never deleted because a sync failed;
 *  - a local photo is never deleted before its upload succeeded;
 *  - the report keeps its original UUID on every retry, which is what makes the
 *    server-side write idempotent.
 */
class ReportSyncEngine(
    private val reportDao: ReportDao,
    private val notificationDao: NotificationDao,
    private val remote: SupabaseRepository,
    private val photoStorage: PhotoStorage,
    private val locationProvider: LocationProvider,
    private val networkMonitor: ConnectivityMonitor,
    private val notificationPresenter: StatusNotifier,
) {

    suspend fun syncAll(): SyncSummary {
        if (!networkMonitor.currentlyOnline()) return SyncSummary()

        val pending = reportDao.getPendingSyncReports()
        var succeeded = 0
        var failed = 0

        pending.forEach { entity ->
            if (syncReport(entity.id)) succeeded++ else failed++
        }

        return SyncSummary(attempted = pending.size, succeeded = succeeded, failed = failed)
    }

    private val syncMutex = Mutex()

    /** Returns true only when the report is confirmed stored on the server. */
    // Immediate sends and background passes share the same engine.
    suspend fun syncReport(reportId: String): Boolean = syncMutex.withLock {
        syncReportLocked(reportId)
    }

    private suspend fun syncReportLocked(reportId: String): Boolean {
        val entity = reportDao.getReportById(reportId) ?: return false
        if (entity.syncStatus == SyncStatus.SYNCED.id) return true
        if (!networkMonitor.currentlyOnline()) return false

        val now = System.currentTimeMillis()
        reportDao.updateSyncStatus(reportId, SyncStatus.SYNCING.id, now)

        return try {
            val report = entity.toDomain()

            // 1. Photo first: the row should never point at an image that is not there.
            val photoUrl = entity.photoRemoteUrl ?: uploadPhotoIfPresent(
                userId = entity.userId,
                reportId = entity.id,
                photoPath = entity.photoPath,
            )

            // 2. Idempotent write, keyed by the report's original UUID.
            remote.upsertReport(report, photoUrl)

            reportDao.markSynced(
                reportId = reportId,
                syncStatus = SyncStatus.SYNCED.id,
                photoRemoteUrl = photoUrl,
                updatedAt = System.currentTimeMillis(),
            )
            notify(
                userId = entity.userId,
                title = "Laporan berhasil dikirim",
                message = "Laporan Anda sudah diterima dan menunggu verifikasi.",
                reportId = reportId,
                isStatusChange = false,
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Sync failed for $reportId", error)
            // Still offline? That is pending, not failed.
            val nextStatus = if (networkMonitor.currentlyOnline()) {
                SyncStatus.FAILED.id
            } else {
                SyncStatus.PENDING_SYNC.id
            }
            reportDao.markSyncFailure(
                reportId = reportId,
                syncStatus = nextStatus,
                error = error.message?.take(200),
                updatedAt = System.currentTimeMillis(),
            )
            false
        }
    }

    private suspend fun uploadPhotoIfPresent(
        userId: String,
        reportId: String,
        photoPath: String?,
    ): String? {
        if (photoPath.isNullOrBlank()) return null
        val file = File(photoPath)
        if (!file.exists() || file.length() == 0L) return null
        return remote.uploadReportPhoto(userId, reportId, file.readBytes())
    }

    /**
     * Pulls authority-side status changes down. Only [ReportEntity.reportStatus] is
     * touched — local sync bookkeeping is ours, not the server's.
     */
    suspend fun pullRemoteStatuses(userId: String) {
        if (!networkMonitor.currentlyOnline()) return

        val remoteReports = runCatching { remote.fetchReports(userId) }.getOrNull() ?: return
        remoteReports.forEach { row ->
            val local = reportDao.getReportById(row.id) ?: return@forEach
            if (local.reportStatus == row.reportStatus) return@forEach

            reportDao.updateReportStatus(row.id, row.reportStatus, System.currentTimeMillis())
            val status = ReportStatus.fromId(row.reportStatus)
            notify(
                userId = userId,
                title = "Status laporan Anda berubah",
                message = "Laporan Anda kini berstatus \"${status.label}\".",
                reportId = row.id,
                isStatusChange = true,
            )
        }
    }

    /**
     * Fills in street names that could not be resolved while offline, so an
     * offline-created report stops showing bare coordinates once we are back online.
     */
    suspend fun resolveMissingStreetNames(userId: String) {
        if (!networkMonitor.currentlyOnline()) return
        reportDao.getReportsWithoutStreetName(userId).take(MAX_GEOCODE_PER_RUN).forEach { entity ->
            val street = locationProvider.reverseGeocode(entity.latitude, entity.longitude)
            if (!street.isNullOrBlank()) {
                reportDao.fillMissingStreetName(entity.id, street)
            }
        }
    }

    private suspend fun notify(
        userId: String,
        title: String,
        message: String,
        reportId: String?,
        isStatusChange: Boolean,
    ) {
        notificationDao.insert(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                message = message,
                reportId = reportId,
                isRead = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
        notificationPresenter.show(
            title,
            message,
            reportId,
            if (isStatusChange) NotificationKind.STATUS else NotificationKind.SYNC,
        )
    }

    private companion object {
        const val TAG = "ReportSyncEngine"
        const val MAX_GEOCODE_PER_RUN = 10
    }
}
