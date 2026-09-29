package org.umn.jalankita.di

import android.content.Context
import android.content.pm.PackageManager
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.umn.jalankita.data.demo.DemoSyncEngine
import org.umn.jalankita.data.demo.DemoSyncTrigger
import org.umn.jalankita.data.demo.SampleDataSeeder
import org.umn.jalankita.data.local.AppDatabase
import org.umn.jalankita.data.local.SessionStore
import org.umn.jalankita.data.location.LocationProvider
import org.umn.jalankita.data.location.NearbyAlertMonitor
import org.umn.jalankita.data.media.PhotoStorage
import org.umn.jalankita.data.remote.SupabaseClientProvider
import org.umn.jalankita.data.remote.SupabaseRepository
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.DemoAuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.data.repository.ReportSyncTrigger
import org.umn.jalankita.data.repository.SupabaseAuthRepository
import org.umn.jalankita.data.worker.ReportSyncEngine
import org.umn.jalankita.data.worker.SyncReportsWorker
import org.umn.jalankita.data.worker.WorkManagerSyncTrigger
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.util.NetworkMonitor
import org.umn.jalankita.util.NotificationPresenter

/**
 * Hand-wired dependency graph. The app is small enough that a DI framework would
 * cost more than it saves, and this keeps the build free of an extra annotation
 * processor.
 *
 * Everything degrades rather than crashes when a backend is not configured:
 * without Supabase credentials the app falls back to a clearly-labelled local
 * demo mode, and without a Maps key the map surfaces show a placeholder. Demo
 * mode is still a *working* app — it has its own sync engine, so a report does
 * not sit on "Menunggu Sinkronisasi" forever.
 */
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

    val sampleDataSeeder: SampleDataSeeder by lazy {
        SampleDataSeeder(
            reportDao = database.reportDao(),
            notificationDao = database.notificationDao(),
            reporterDao = database.reporterDao(),
            sessionStore = sessionStore,
        )
    }

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

    /** Only built when there is no real backend to talk to. */
    private val demoSyncEngine: DemoSyncEngine? by lazy {
        if (syncEngine != null) null else DemoSyncEngine(
            reportDao = database.reportDao(),
            notificationDao = database.notificationDao(),
            networkMonitor = networkMonitor,
            notificationPresenter = notificationPresenter,
        )
    }

    private val syncTrigger: ReportSyncTrigger by lazy {
        syncEngine?.let { WorkManagerSyncTrigger(context, it) }
            ?: DemoSyncTrigger(applicationScope, requireNotNull(demoSyncEngine))
    }

    // --- Repositories ---

    val authRepository: AuthRepository by lazy {
        val client = supabaseClient
        val remote = supabaseRepository
        if (client != null && remote != null) {
            SupabaseAuthRepository(client, remote, sessionStore, applicationScope)
        } else {
            DemoAuthRepository(sessionStore)
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

    /**
     * Startup work, once a session exists: seed the worked examples for a new
     * account, then hand any outstanding reports to the sync layer. Offline simply
     * leaves them pending.
     */
    fun startBackgroundSync() {
        applicationScope.launch {
            authRepository.authState.collect { state ->
                if (state !is AuthState.SignedIn) return@collect
                // Samples belong to demo mode only: with Supabase configured the
                // account's real reports are the content.
                if (!authRepository.isBackedByServer) {
                    sampleDataSeeder.seedIfNeeded(state.user.id, state.user.name)
                } else {
                    reportRepository.refreshReporterStats(state.user.id, state.user.name)
                }
                if (syncEngine != null) {
                    SyncReportsWorker.enqueueOnce(context)
                    SyncReportsWorker.enqueuePeriodic(context)
                } else {
                    demoSyncEngine?.syncAll()
                }
            }
        }

        // Demo mode has no WorkManager constraint watching the network for it, so
        // reconnecting has to be observed here for pending reports to go out.
        if (syncEngine == null) {
            applicationScope.launch {
                networkMonitor.isOnline.distinctUntilChanged().collect { online ->
                    if (online && authRepository.currentUser != null) demoSyncEngine?.syncAll()
                }
            }
        }
    }

    /** Backs the manual "Sinkronkan Sekarang" action. */
    fun requestManualSync() {
        val engine = syncEngine
        if (engine == null) {
            applicationScope.launch { demoSyncEngine?.syncAll() }
            return
        }
        SyncReportsWorker.enqueueOnce(context)
        applicationScope.launch {
            engine.syncAll()
            authRepository.currentUser?.id?.let { runCatching { engine.pullRemoteStatuses(it) } }
        }
    }

    /** "Muat Ulang Contoh Laporan" in Pengaturan — demo mode only. */
    fun reseedSampleData() {
        if (authRepository.isBackedByServer) return
        val user = authRepository.currentUser ?: return
        applicationScope.launch { sampleDataSeeder.reseed(user.id, user.name) }
    }
}
