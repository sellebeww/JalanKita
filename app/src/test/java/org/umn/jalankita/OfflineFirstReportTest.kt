package org.umn.jalankita

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.fake.FakeConnectivityMonitor
import org.umn.jalankita.fake.FakeNotificationDao
import org.umn.jalankita.fake.FakeReportDao
import org.umn.jalankita.fake.RecordingSyncTrigger
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus

/**
 * The non-negotiable behaviour from the spec: Room is written first, offline is a
 * pending state rather than a failure, and retries never duplicate a report.
 */
class OfflineFirstReportTest {

    private val reportId = "11111111-2222-4333-8444-555555555555"
    private val userId = "user-1"

    private fun repository(
        online: Boolean,
        syncSucceeds: Boolean = false,
    ): Triple<ReportRepository, FakeReportDao, RecordingSyncTrigger> {
        val reportDao = FakeReportDao()
        val trigger = RecordingSyncTrigger(syncSucceeds = syncSucceeds)
        val repo = ReportRepository(
            reportDao = reportDao,
            notificationDao = FakeNotificationDao(),
            networkMonitor = FakeConnectivityMonitor(online),
            syncTrigger = trigger,
        )
        return Triple(repo, reportDao, trigger)
    }

    private suspend fun ReportRepository.submitSample(id: String = "11111111-2222-4333-8444-555555555555") =
        submitReport(
            reportId = id,
            userId = "user-1",
            reporterName = "Vassel",
            photoPath = "/files/report_photos/$id.jpg",
            category = DamageCategory.POTHOLE,
            description = "Lubang di depan sekolah",
            latitude = -6.2088,
            longitude = 106.8456,
            streetName = null,
        )

    @Test
    fun `offline submit stores the report locally and keeps it pending`() = runTest {
        val (repo, dao, trigger) = repository(online = false)

        val outcome = repo.submitSample()

        assertFalse("offline submit must not claim it was synced", outcome.synced)
        val stored = dao.getReportById(reportId)
        assertNotNull("the report must exist in Room before any network call", stored)
        assertEquals(SyncStatus.PENDING_SYNC.id, stored!!.syncStatus)
        assertEquals(ReportStatus.PENDING.id, stored.reportStatus)
        // Offline is not a failure: no attempt is made, but the pass is queued.
        assertTrue(trigger.syncNowCalls.isEmpty())
        assertEquals(1, trigger.requestSyncCalls)
    }

    @Test
    fun `online submit attempts an immediate sync`() = runTest {
        val (repo, dao, trigger) = repository(online = true, syncSucceeds = true)

        val outcome = repo.submitSample()

        assertTrue(outcome.synced)
        assertEquals(listOf(reportId), trigger.syncNowCalls)
        assertNotNull(dao.getReportById(reportId))
        // Already delivered: no background pass needs queuing, and the engine
        // owns the "berhasil dikirim" notification.
        assertEquals(0, trigger.requestSyncCalls)
    }

    @Test
    fun `a failed immediate sync still leaves the report stored and queued`() = runTest {
        val (repo, dao, trigger) = repository(online = true, syncSucceeds = false)

        val outcome = repo.submitSample()

        assertFalse(outcome.synced)
        assertEquals(SyncStatus.PENDING_SYNC.id, dao.getReportById(reportId)!!.syncStatus)
        assertEquals(1, trigger.requestSyncCalls)
    }

    @Test
    fun `resubmitting the same id does not create a duplicate`() = runTest {
        val (repo, dao, _) = repository(online = false)

        repo.submitSample()
        repo.submitSample()

        assertEquals(1, dao.getReportsByUser(userId).size)
    }

    @Test
    fun `unsynced count follows the local rows`() = runTest {
        val (repo, dao, _) = repository(online = false)

        repo.submitSample()
        repo.submitSample(id = "99999999-2222-4333-8444-555555555555")
        assertEquals(2, repo.observeUnsyncedCount(userId).first())

        dao.markSynced(reportId, SyncStatus.SYNCED.id, "https://cdn/a.jpg", System.currentTimeMillis())
        assertEquals(1, repo.observeUnsyncedCount(userId).first())
    }

    @Test
    fun `pending query picks up failed reports so they are retried`() = runTest {
        val (repo, dao, _) = repository(online = false)
        repo.submitSample()

        dao.markSyncFailure(reportId, SyncStatus.FAILED.id, "boom", System.currentTimeMillis())

        val pending = repo.getPendingSyncReports()
        assertEquals(1, pending.size)
        assertEquals(SyncStatus.FAILED, pending.single().syncStatus)
        assertEquals(1, pending.single().syncAttempts)
    }

    @Test
    fun `submitting records an in-app notification worded for the connectivity state`() = runTest {
        val notificationDao = FakeNotificationDao()
        val repo = ReportRepository(
            reportDao = FakeReportDao(),
            notificationDao = notificationDao,
            networkMonitor = FakeConnectivityMonitor(false),
            syncTrigger = RecordingSyncTrigger(),
        )

        repo.submitSample()

        val notification = notificationDao.rows.value.single()
        assertEquals("Laporan tersimpan di perangkat", notification.title)
        assertTrue(notification.message.contains("otomatis"))
        assertFalse(notification.isRead)
    }
}
