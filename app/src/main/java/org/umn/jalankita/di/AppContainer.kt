package org.umn.jalankita.di

import android.content.Context
import android.content.pm.PackageManager
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.umn.jalankita.data.local.AppDatabase
import org.umn.jalankita.data.local.SessionStore
import org.umn.jalankita.data.location.LocationProvider
import org.umn.jalankita.data.location.NearbyAlertMonitor
import org.umn.jalankita.data.media.PhotoStorage
import org.umn.jalankita.data.remote.SupabaseClientProvider
import org.umn.jalankita.data.remote.SupabaseRepository
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.LocalAuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.data.repository.NoOpSyncTrigger
import org.umn.jalankita.data.repository.ReportSyncTrigger
import org.umn.jalankita.data.repository.SupabaseAuthRepository
import org.umn.jalankita.data.worker.ReportSyncEngine
import org.umn.jalankita.data.worker.SyncReportsWorker
import org.umn.jalankita.data.worker.WorkManagerSyncTrigger
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.util.NetworkMonitor
import org.umn.jalankita.util.NotificationPresenter

/** Application dependencies. Reports remain local until Supabase is configured. */
class AppContainer(private val context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isSupabaseConfigured: Boolean = SupabaseClientProvider.isConfigured

    val isMapsConfigured: Boolean = runCatching {
        val info = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        )
        !info.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
    }.getOrDefault(false)

    // --- Local ---

    val database: AppDatabase by lazy { AppDatabase.get(context) }

    val sessionStore: SessionStore by lazy { SessionStore(context) }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }

    val locationProvider: LocationProvider by lazy { LocationProvider(context) }

    val photoStorage: PhotoStorage by lazy { PhotoStorage(context) }

    val notificationPresenter: NotificationPresenter by lazy { NotificationPresenter(context) }

    /**
     * Proximity warnings. Only active while a screen collects its state, so the
     * app never tracks location in the background.
     */
    val nearbyAlertMonitor: NearbyAlertMonitor by lazy {
        NearbyAlertMonitor(
            locationProvider = locationProvider,
            reportRepository = reportRepository,
            notifier = notificationPresenter,
            scope = applicationScope,
        )
    }

    // --- Remote ---

    private val supabaseClient: SupabaseClient? by lazy { SupabaseClientProvider.createOrNull() }

    val supabaseRepository: SupabaseRepository? by lazy {
        supabaseClient?.let(::SupabaseRepository)
    }

    val syncEngine: ReportSyncEngine? by lazy {
        val remote = supabaseRepository ?: return@lazy null
        ReportSyncEngine(
            reportDao = database.reportDao(),
            notificationDao = database.notificationDao(),
            remote = remote,
            photoStorage = photoStorage,
            locationProvider = locationProvider,
            networkMonitor = networkMonitor,
            notificationPresenter = notificationPresenter,
        )
    }

    private val syncTrigger: ReportSyncTrigger by lazy {
        syncEngine?.let { WorkManagerSyncTrigger(context, it) } ?: NoOpSyncTrigger
    }

    // --- Repositories ---

    val authRepository: AuthRepository by lazy {
        val client = supabaseClient
        val remote = supabaseRepository
        if (client != null && remote != null) {
            SupabaseAuthRepository(client, remote, sessionStore, applicationScope)
        } else {
            LocalAuthRepository(sessionStore)
        }
    }

    val reportRepository: ReportRepository by lazy {
        ReportRepository(
            reportDao = database.reportDao(),
            notificationDao = database.notificationDao(),
            networkMonitor = networkMonitor,
            syncTrigger = syncTrigger,
            reporterDao = database.reporterDao(),
        )
    }

    /** Resume server-backed synchronization once an account is signed in. */
    fun startBackgroundSync() {
        applicationScope.launch {
            authRepository.authState.collect { state ->
                if (state !is AuthState.SignedIn) return@collect
                reportRepository.refreshReporterStats(state.user.id, state.user.name)
                if (syncEngine != null) {
                    SyncReportsWorker.enqueueOnce(context)
                    SyncReportsWorker.enqueuePeriodic(context)
                }
            }
        }
    }

    fun requestManualSync() {
        val engine = syncEngine ?: return
        SyncReportsWorker.enqueueOnce(context)
        applicationScope.launch {
            engine.syncAll()
            authRepository.currentUser?.id?.let { runCatching { engine.pullRemoteStatuses(it) } }
        }
    }
}
