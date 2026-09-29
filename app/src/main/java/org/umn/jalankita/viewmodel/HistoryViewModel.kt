package org.umn.jalankita.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.ConnectivityMonitor

/**
 * The tabs on "Laporan Saya". "Belum Terkirim" is deliberately its own filter:
 * it answers a different question from the workflow ones.
 */
enum class HistoryFilter(val label: String) {
    ALL("Semua"),
    PENDING("Menunggu"),
    IN_PROGRESS("Ditangani"),
    COMPLETED("Selesai"),
    UNSYNCED("Belum Terkirim");

    fun matches(report: Report): Boolean = when (this) {
        ALL -> true
        PENDING -> report.reportStatus == ReportStatus.PENDING ||
            report.reportStatus == ReportStatus.NEEDS_INFO
        IN_PROGRESS -> report.reportStatus == ReportStatus.VERIFIED ||
            report.reportStatus == ReportStatus.IN_PROGRESS
        COMPLETED -> report.reportStatus == ReportStatus.COMPLETED
        UNSYNCED -> report.syncStatus != SyncStatus.SYNCED
    }
}

data class HistoryUiState(
    val reports: List<Report> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val counts: Map<HistoryFilter, Int> = emptyMap(),
    val isOnline: Boolean = true,
    val unsyncedCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val visibleReports: List<Report> get() = reports.filter(filter::matches)

    /** True only when the account has reports but none match the active filter. */
    val isFilteredEmpty: Boolean get() = reports.isNotEmpty() && visibleReports.isEmpty()
}

class HistoryViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
    networkMonitor: ConnectivityMonitor,
) : UserScopedViewModel(authRepository) {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        forUser(emptyList<Report>()) { reportRepository.observeReports(it) },
        networkMonitor.isOnline,
        forUser(0) { reportRepository.observeUnsyncedCount(it) },
        filter,
    ) { reports, isOnline, unsynced, activeFilter ->
        HistoryUiState(
            reports = reports,
            filter = activeFilter,
            counts = HistoryFilter.entries.associateWith { f -> reports.count(f::matches) },
            isOnline = isOnline,
            unsyncedCount = unsynced,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }
}
