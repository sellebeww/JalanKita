package org.umn.jalankita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.jalankita.model.ReporterLevel
import org.umn.jalankita.model.ReporterProfile

/**
 * "Apakah pelapor ini bisa dipercaya?" — the scoring has to refuse to answer
 * until it has grounds to, and must not let one lucky report look like a record.
 */
class ReporterCredibilityTest {

    private fun profile(
        total: Int,
        confirmed: Int,
        completed: Int = 0,
        needsInfo: Int = 0,
    ) = ReporterProfile(
        id = "reporter-1",
        name = "Rina Kusuma",
        joinedAt = 0L,
        totalReports = total,
        confirmedReports = confirmed,
        completedReports = completed,
        needsInfoReports = needsInfo,
    )

    @Test
    fun `a reporter with nothing reviewed has no score at all`() {
        val newcomer = profile(total = 2, confirmed = 0)

        assertFalse(newcomer.hasTrackRecord)
        assertEquals(0, newcomer.trustScore)
        assertEquals(ReporterLevel.NEW, newcomer.level)
    }

    @Test
    fun `one confirmed report does not make a verified reporter`() {
        val lucky = profile(total = 1, confirmed = 1)

        assertFalse(lucky.hasTrackRecord)
        assertEquals(ReporterLevel.NEW, lucky.level)
    }

    @Test
    fun `pending reports are excluded from accuracy`() {
        // 8 filed, 4 reviewed: 3 confirmed, 1 sent back.
        val reporter = profile(total = 8, confirmed = 3, needsInfo = 1)

        assertEquals(4, reporter.reviewedReports)
        assertEquals(4, reporter.pendingReports)
        assertEquals(75, reporter.accuracyPercent)
    }

    @Test
    fun `a long accurate record reaches the verified badge`() {
        val veteran = profile(total = 24, confirmed = 19, completed = 12, needsInfo = 1)

        assertEquals(95, veteran.accuracyPercent)
        assertTrue(veteran.trustScore >= 90)
        assertEquals(ReporterLevel.VERIFIED, veteran.level)
    }

    @Test
    fun `volume alone cannot lift a poor accuracy into trusted`() {
        val noisy = profile(total = 40, confirmed = 10, needsInfo = 20)

        assertEquals(33, noisy.accuracyPercent)
        assertTrue(noisy.trustScore < 70)
        assertEquals(ReporterLevel.ACTIVE, noisy.level)
    }

    @Test
    fun `a short flawless record ranks below a long flawless one`() {
        val short = profile(total = 3, confirmed = 3)
        val long = profile(total = 20, confirmed = 20)

        assertEquals(100, short.accuracyPercent)
        assertTrue(short.trustScore < long.trustScore)
        assertEquals(ReporterLevel.TRUSTED, short.level)
        assertEquals(ReporterLevel.VERIFIED, long.level)
    }

    @Test
    fun `initials fall back gracefully for a single-word name`() {
        assertEquals("V", profile(0, 0).copy(name = "Vassel").initials)
        assertEquals("RK", profile(0, 0).copy(name = "Rina Kusuma").initials)
    }
}
