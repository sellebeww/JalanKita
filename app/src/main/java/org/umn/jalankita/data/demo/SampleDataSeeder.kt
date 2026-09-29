package org.umn.jalankita.data.demo

import org.umn.jalankita.data.local.NotificationDao
import org.umn.jalankita.data.local.ReportDao
import org.umn.jalankita.data.local.ReporterDao
import org.umn.jalankita.data.local.SessionStore

/**
 * Puts the worked examples into Room the first time an account is used.
 *
 * Seeding writes only through upserts on deterministic ids, so running it twice
 * — or resetting it from Pengaturan — can never produce a duplicate report.
 * A user's own reports are never touched.
 */
class SampleDataSeeder(
    private val reportDao: ReportDao,
    private val notificationDao: NotificationDao,
    private val reporterDao: ReporterDao,
    private val sessionStore: SessionStore,
) {

    suspend fun seedIfNeeded(userId: String, reporterName: String) {
        if (userId.isBlank() || sessionStore.isSampleDataSeeded(userId)) return
        write(userId, reporterName)
        sessionStore.markSampleDataSeeded(userId)
    }

    /** "Muat Ulang Contoh Laporan" in Pengaturan — rewrites them with fresh dates. */
    suspend fun reseed(userId: String, reporterName: String) {
        if (userId.isBlank()) return
        write(userId, reporterName)
        sessionStore.markSampleDataSeeded(userId)
    }

    private suspend fun write(userId: String, reporterName: String) {
        reporterDao.upsertAll(SampleContent.communityReporters())
        reportDao.upsertAll(SampleContent.reportsFor(userId, reporterName))
        notificationDao.insertAll(SampleContent.notificationsFor(userId))
        // Rows written before reports carried an author still belong to this user.
        reportDao.backfillReporterName(userId, reporterName)
        // The seeded rows are what the user's own record is counted from.
        reporterDao.refreshFromReports(userId, reporterName, System.currentTimeMillis())
    }
}
