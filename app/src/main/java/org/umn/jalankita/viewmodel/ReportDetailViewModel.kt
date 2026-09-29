package org.umn.jalankita.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReporterProfile
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.ConnectivityMonitor

data class ReportDetailUiState(
    val report: Report? = null,
    val reporter: ReporterProfile? = null,
    val isOnline: Boolean = true,
    val isLoading: Boolean = true,
) {
    /** Offering a retry only makes sense when it could actually do something. */
    val canRetrySync: Boolean
        get() = report != null && report.syncStatus != SyncStatus.SYNCED &&
            report.syncStatus != SyncStatus.SYNCING && isOnline
}

class ReportDetailViewModel(
    private val reportRepository: ReportRepository,
    reportId: String,
    networkMonitor: ConnectivityMonitor,
) : ViewModel() {

    private val report = reportRepository.observeReport(reportId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reporter = report
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) flowOf(null) else reportRepository.observeReporter(userId)
        }

    val uiState: StateFlow<ReportDetailUiState> = combine(
        report,
        reporter,
        networkMonitor.isOnline,
    ) { report, reporter, isOnline ->
        ReportDetailUiState(
            report = report,
            reporter = reporter,
            isOnline = isOnline,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportDetailUiState(),
    )

    fun retrySync() = reportRepository.requestSync()
}
