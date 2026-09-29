package org.umn.jalankita.data.worker

import android.content.Context
import org.umn.jalankita.data.repository.ReportSyncTrigger

/** Wires the repository's sync seam to WorkManager and the shared sync engine. */
class WorkManagerSyncTrigger(
    private val context: Context,
    private val engine: ReportSyncEngine,
) : ReportSyncTrigger {

    override fun requestSync() {
        SyncReportsWorker.enqueueOnce(context)
    }

    override suspend fun syncNow(reportId: String): Boolean = engine.syncReport(reportId)
}
