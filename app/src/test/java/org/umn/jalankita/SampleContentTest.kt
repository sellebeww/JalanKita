package org.umn.jalankita

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.jalankita.data.local.toDomain
import org.umn.jalankita.data.location.nearbyDamage
import org.umn.jalankita.model.ReporterLevel
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.fixtures.SampleContent
import org.umn.jalankita.fake.FakeNotificationDao
import org.umn.jalankita.fake.FakeReportDao
import org.umn.jalankita.model.DamageCategory

/**
 * The worked examples are the first thing a new account sees, so they must be
 * stable: seeding twice may refresh them but must never duplicate them.
 */
class SampleContentTest {

    private val userId = "user-1"
    private val REPORTER = "Vassel"

    @Test
    fun `sample ids are derived from the user and stable across runs`() {
        val first = SampleContent.reportsFor(userId, REPORTER).map { it.id }
        val second = SampleContent.reportsFor(userId, REPORTER).map { it.id }
        assertEquals(first, second)
    }

    @Test
    fun `different users get different sample reports`() {
        val a = SampleContent.reportsFor("user-a", REPORTER).map { it.id }.toSet()
        val b = SampleContent.reportsFor("user-b", REPORTER).map { it.id }.toSet()
        assertTrue(a.intersect(b).isEmpty())
        assertNotEquals(a, b)
    }

    @Test
    fun `every showcase category has an example report`() {
        val categories = SampleContent.reportsFor(userId, REPORTER).map { it.category }.toSet()
        DamageCategory.showcase.forEach { category ->
            assertTrue(
                "Missing sample for ${category.id}",
                category.id in categories,
            )
        }
    }

    @Test
    fun `reseeding replaces the samples instead of duplicating them`() = runTest {
        val reportDao = FakeReportDao()
        val notificationDao = FakeNotificationDao()

        repeat(3) {
            reportDao.upsertAll(SampleContent.reportsFor(userId, REPORTER))
            notificationDao.insertAll(SampleContent.notificationsFor(userId))
        }

        assertEquals(SampleContent.reportsFor(userId, REPORTER).size, reportDao.rows.value.size)
        assertEquals(SampleContent.notificationsFor(userId).size, notificationDao.rows.value.size)
    }

    @Test
    fun `community reports are laid out across the alert bands`() {
        val nearby = SampleContent.reportsFor(userId, REPORTER)
            .map { it.toDomain() }
            .nearbyDamage(SampleContent.AREA_LATITUDE, SampleContent.AREA_LONGITUDE)

        // At least one inside the warning radius, so the red alert can be seen,
        // and at least one further out, so the "sekitar Anda" list is not empty.
        assertTrue("No sample inside the warning radius", nearby.any { it.isWarning })
        assertTrue("No sample outside the warning radius", nearby.any { !it.isWarning })
    }

    @Test
    fun `repaired community damage is seeded but never alerted`() {
        val reports = SampleContent.reportsFor(userId, REPORTER).map { it.toDomain() }
        val completed = reports.filter { it.reportStatus == ReportStatus.COMPLETED }

        assertTrue("Expected a repaired sample", completed.isNotEmpty())
        val alerted = reports.nearbyDamage(SampleContent.AREA_LATITUDE, SampleContent.AREA_LONGITUDE)
        completed.forEach { fixed ->
            assertTrue(alerted.none { it.report.id == fixed.id })
        }
    }

    @Test
    fun `community reporters span the whole credibility range`() {
        val levels = SampleContent.communityReporters()
            .map { it.toDomain().level }
            .toSet()

        assertEquals(ReporterLevel.entries.toSet(), levels)
    }

    @Test
    fun `community reports name a reporter and are not owned by the user`() {
        val reports = SampleContent.reportsFor(userId, REPORTER)
        val community = reports.filter { it.userId != userId }

        assertTrue(community.isNotEmpty())
        community.forEach { report ->
            assertFalse(report.reporterName.isNullOrBlank())
            assertNotEquals(REPORTER, report.reporterName)
        }
    }

    @Test
    fun `notifications point at reports that actually exist`() {
        val reportIds = SampleContent.reportsFor(userId, REPORTER).map { it.id }.toSet()
        SampleContent.notificationsFor(userId)
            .mapNotNull { it.reportId }
            .forEach { assertTrue("Dangling reportId $it", it in reportIds) }
    }
}
