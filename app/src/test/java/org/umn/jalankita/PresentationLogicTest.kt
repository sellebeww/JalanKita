package org.umn.jalankita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.jalankita.data.repository.stats
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.asRelativeTime
import org.umn.jalankita.viewmodel.HistoryFilter
import org.umn.jalankita.viewmodel.PasswordStrength
import java.util.concurrent.TimeUnit

/**
 * The small pieces of logic the UI reads directly: filters, counters, the
 * relative clock and the password meter.
 */
class PresentationLogicTest {

    private fun report(
        id: String,
        reportStatus: ReportStatus,
        syncStatus: SyncStatus = SyncStatus.SYNCED,
    ) = Report(
        id = id,
        userId = "user-1",
        photoPath = null,
        photoRemoteUrl = null,
        category = DamageCategory.POTHOLE,
        description = null,
        latitude = -6.2568,
        longitude = 106.6180,
        streetName = null,
        reportStatus = reportStatus,
        syncStatus = syncStatus,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val sample = listOf(
        report("1", ReportStatus.PENDING, SyncStatus.PENDING_SYNC),
        report("2", ReportStatus.VERIFIED),
        report("3", ReportStatus.IN_PROGRESS),
        report("4", ReportStatus.COMPLETED),
        report("5", ReportStatus.NEEDS_INFO),
    )

    @Test
    fun `stats count workflow states without mixing in sync state`() {
        val stats = sample.stats()
        assertEquals(5, stats.total)
        assertEquals(1, stats.pending)
        assertEquals(2, stats.inProgress)
        assertEquals(1, stats.completed)
        // Only the PENDING_SYNC one is missing from the server.
        assertEquals(4, stats.synced)
    }

    @Test
    fun `needs-info reports are surfaced under Menunggu, not hidden`() {
        val pending = sample.filter(HistoryFilter.PENDING::matches).map { it.id }
        assertEquals(listOf("1", "5"), pending)
    }

    @Test
    fun `the unsynced filter follows sync state, not workflow state`() {
        val unsynced = sample.filter(HistoryFilter.UNSYNCED::matches).map { it.id }
        assertEquals(listOf("1"), unsynced)
    }

    @Test
    fun `the all filter keeps every report`() {
        assertEquals(sample.size, sample.count(HistoryFilter.ALL::matches))
    }

    @Test
    fun `relative time reads as minutes, hours and days before falling back to a date`() {
        val now = System.currentTimeMillis()
        assertEquals("Baru saja", now.asRelativeTime(now))
        assertEquals("5 menit lalu", (now - TimeUnit.MINUTES.toMillis(5)).asRelativeTime(now))
        assertEquals("3 jam lalu", (now - TimeUnit.HOURS.toMillis(3)).asRelativeTime(now))
        assertEquals("2 hari lalu", (now - TimeUnit.DAYS.toMillis(2)).asRelativeTime(now))

        val old = now - TimeUnit.DAYS.toMillis(30)
        val formatted = old.asRelativeTime(now)
        assertFalse(formatted.contains("lalu"))
        assertTrue(formatted.isNotBlank())
    }

    @Test
    fun `password strength rises with length and variety`() {
        assertEquals(PasswordStrength.NONE, PasswordStrength.of(""))
        assertEquals(PasswordStrength.WEAK, PasswordStrength.of("abc"))
        assertEquals(PasswordStrength.FAIR, PasswordStrength.of("vassel123"))
        assertEquals(PasswordStrength.STRONG, PasswordStrength.of("Vassel#2026!"))
    }

    @Test
    fun `every damage category carries guide copy`() {
        DamageCategory.entries.forEach { category ->
            assertTrue(category.label.isNotBlank())
            assertTrue("No description for ${category.id}", category.description.isNotBlank())
            assertTrue("No example for ${category.id}", category.example.isNotBlank())
            assertTrue("No photo tip for ${category.id}", category.photoTip.isNotBlank())
        }
    }
}
