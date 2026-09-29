package org.umn.jalankita.data.repository

/**
 * Seam between "a report was saved" and "something uploads it".
 * Implemented by the WorkManager-backed coordinator; [NoOpSyncTrigger] is used
 * when no remote backend is configured.
 */
interface ReportSyncTrigger {

    /** Queue a background sync pass; the worker decides when it actually runs. */
    fun requestSync()

    /**
     * Try to upload one report right now.
     * Returns true only when the report reached the server.
     */
    suspend fun syncNow(reportId: String): Boolean
}

object NoOpSyncTrigger : ReportSyncTrigger {
    override fun requestSync() = Unit
    override suspend fun syncNow(reportId: String): Boolean = false
}
