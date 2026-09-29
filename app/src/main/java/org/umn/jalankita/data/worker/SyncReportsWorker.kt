package org.umn.jalankita.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.umn.jalankita.JalanKitaApp
import java.util.concurrent.TimeUnit

/**
 * Background sync pass. Runs only when a network is available, and asks
 * WorkManager to retry with exponential backoff if anything is still outstanding.
 */
class SyncReportsWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? JalanKitaApp)?.container ?: return Result.success()
        val engine = container.syncEngine ?: return Result.success()

        val summary = engine.syncAll()

        container.authRepository.currentUser?.id?.let { userId ->
            runCatching { engine.pullRemoteStatuses(userId) }
            runCatching { engine.resolveMissingStreetNames(userId) }
        }

        // Nothing is ever dropped: unfinished reports simply come back next run.
        return if (summary.shouldRetry) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "jalankita-sync-reports"
        const val PERIODIC_WORK_NAME = "jalankita-sync-reports-periodic"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncReportsWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                // APPEND_OR_REPLACE would queue a pass per report; one pending pass
                // already picks up everything that is outstanding.
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        /** Backstop for status pulls when the user never opens the app. */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncReportsWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
