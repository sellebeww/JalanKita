package org.umn.jalankita.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.umn.jalankita.data.location.NearbyAlertMonitor
import org.umn.jalankita.data.location.NearbyState
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.data.repository.ReportStats
import org.umn.jalankita.data.repository.stats
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.UserProfile
import org.umn.jalankita.util.ConnectivityMonitor

data class HomeUiState(
    val user: UserProfile? = null,
    val recentReports: List<Report> = emptyList(),
    /** Mine and the community's — what the map and the alerts are about. */
    val communityReports: List<Report> = emptyList(),
    val stats: ReportStats = ReportStats(),
    val nearby: NearbyState = NearbyState(),
    val isOnline: Boolean = true,
    val unreadNotifications: Int = 0,
    val unsyncedCount: Int = 0,
    val isLoading: Boolean = true,
)

private const val RECENT_REPORT_LIMIT = 4

class HomeViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
    networkMonitor: ConnectivityMonitor,
    private val nearbyAlertMonitor: NearbyAlertMonitor,
) : UserScopedViewModel(authRepository) {

    private val mine = combine(
        userFlow,
        forUser(emptyList<Report>()) { reportRepository.observeReports(it) },
        forUser(0) { reportRepository.observeUnreadNotificationCount(it) },
        forUser(0) { reportRepository.observeUnsyncedCount(it) },
    ) { user, reports, unread, unsynced ->
        MineSnapshot(user, reports, unread, unsynced)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        mine,
        reportRepository.observeCommunityReports(),
        networkMonitor.isOnline,
        nearbyAlertMonitor.state,
    ) { snapshot, community, isOnline, nearby ->
        HomeUiState(
            user = snapshot.user,
            recentReports = snapshot.reports.take(RECENT_REPORT_LIMIT),
            communityReports = community,
            // Derived from the list we already have rather than a second query.
            stats = snapshot.reports.stats(),
            nearby = nearby,
            isOnline = isOnline,
            unreadNotifications = snapshot.unread,
            unsyncedCount = snapshot.unsynced,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** Called once the user grants location access from the alert card. */
    fun onLocationPermissionGranted() = nearbyAlertMonitor.refresh()

    private data class MineSnapshot(
        val user: UserProfile?,
        val reports: List<Report>,
        val unread: Int,
        val unsynced: Int,
    )
}
