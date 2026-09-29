package org.umn.jalankita

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.umn.jalankita.data.demo.DemoSyncEngine
import org.umn.jalankita.data.local.ReportEntity
import org.umn.jalankita.fake.FakeConnectivityMonitor
import org.umn.jalankita.fake.FakeNotificationDao
import org.umn.jalankita.fake.FakeReportDao
import org.umn.jalankita.fake.RecordingStatusNotifier
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.junit.Test

/**
 * The demo engine stands in for Supabase, so it has to honour the same rule the
 * real one does: **offline is PENDING_SYNC, never FAILED**, and nothing is
 * dropped because a send did not happen.
 */
class DemoSyncEngineTest {

    private val reportId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
    private val userId = "user-1"

    private class Fixture(online: Boolean) {
        val reportDao = FakeReportDao()
        val notificationDao = FakeNotificationDao()
        val network = FakeConnectivityMonitor(online)
        val notifier = RecordingStatusNotifier()
        val engine = DemoSyncEngine(reportDao, notificationDao, network, notifier)
    }

    private suspend fun FakeReportDao.seedPending(id: String, userId: String) {
        val now = System.currentTimeMillis()
        insertReport(
            ReportEntity(
                id = id,
                userId = userId,
                photoPath = null,
                photoRemoteUrl = null,
                category = DamageCategory.POTHOLE.id,
                description = "Lubang di depan sekolah",
                latitude = -6.2568,
                longitude = 106.6180,
                streetName = null,
                reportStatus = ReportStatus.PENDING.id,
                syncStatus = SyncStatus.PENDING_SYNC.id,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    @Test
    fun `offline sync leaves the report pending rather than failed`() = runTest {
        val fixture = Fixture(online = false)
        fixture.reportDao.seedPending(reportId, userId)

        assertFalse(fixture.engine.syncReport(reportId))

        val stored = fixture.reportDao.getReportById(reportId)!!
        assertEquals(SyncStatus.PENDING_SYNC.id, stored.syncStatus)
        assertTrue(fixture.notifier.posted.isEmpty())
    }

    @Test
    fun `online sync marks the report synced and records a notification`() = runTest {
        val fixture = Fixture(online = true)
        fixture.reportDao.seedPending(reportId, userId)

        assertTrue(fixture.engine.syncReport(reportId))

        val stored = fixture.reportDao.getReportById(reportId)!!
        assertEquals(SyncStatus.SYNCED.id, stored.syncStatus)
        assertEquals(1, fixture.notificationDao.rows.value.size)
        assertEquals(reportId, fixture.notificationDao.rows.value.single().reportId)
        assertEquals(1, fixture.notifier.posted.size)
    }

    @Test
    fun `report status set by the authority is left untouched by syncing`() = runTest {
        val fixture = Fixture(online = true)
        fixture.reportDao.seedPending(reportId, userId)

        fixture.engine.syncReport(reportId)

        assertEquals(ReportStatus.PENDING.id, fixture.reportDao.getReportById(reportId)!!.reportStatus)
    }

    @Test
    fun `syncing an already synced report is a no-op`() = runTest {
        val fixture = Fixture(online = true)
        fixture.reportDao.seedPending(reportId, userId)
        fixture.engine.syncReport(reportId)
        val notificationsAfterFirst = fixture.notificationDao.rows.value.size

        assertTrue(fixture.engine.syncReport(reportId))

        assertEquals(notificationsAfterFirst, fixture.notificationDao.rows.value.size)
    }

    @Test
    fun `syncAll returns nothing while offline and everything once online`() = runTest {
        val fixture = Fixture(online = false)
        fixture.reportDao.seedPending(reportId, userId)
        fixture.reportDao.seedPending("11111111-2222-4333-8444-555555555555", userId)

        assertEquals(0, fixture.engine.syncAll())

        fixture.network.setOnline(true)
        assertEquals(2, fixture.engine.syncAll())
        assertTrue(
            fixture.reportDao.rows.value.values.all { it.syncStatus == SyncStatus.SYNCED.id },
        )
    }
}
