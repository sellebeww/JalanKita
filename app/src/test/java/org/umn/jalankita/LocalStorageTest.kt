package org.umn.jalankita

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.fake.FakeConnectivityMonitor
import org.umn.jalankita.fake.FakeNotificationDao
import org.umn.jalankita.fake.FakeReportDao
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.SyncStatus

class LocalStorageTest {
    @Test
    fun `internet alone never marks a report delivered without a server`() = runTest {
        val dao = FakeReportDao()
        val notifications = FakeNotificationDao()
        val repository = ReportRepository(dao, notifications, FakeConnectivityMonitor(true))

        val result = repository.submitReport(
            reportId = "local-report", userId = "local-user", reporterName = "Warga",
            photoPath = "/files/road.jpg", category = DamageCategory.POTHOLE,
            description = "Lubang di jalan", latitude = -6.2, longitude = 106.8,
            streetName = null,
        )

        assertFalse(result.synced)
        assertEquals(SyncStatus.PENDING_SYNC, result.report.syncStatus)
        assertEquals("/files/road.jpg", dao.getReportById("local-report")!!.photoPath)
        assertEquals("Laporan tersimpan di perangkat", notifications.rows.value.single().title)
    }
}
