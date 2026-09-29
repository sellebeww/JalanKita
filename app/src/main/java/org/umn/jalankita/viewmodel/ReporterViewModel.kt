package org.umn.jalankita.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReporterProfile

data class ReporterUiState(
    val profile: ReporterProfile? = null,
    val reports: List<Report> = emptyList(),
    val isSelf: Boolean = false,
    val isLoading: Boolean = true,
) {
    /** Only their reports this device actually holds — stated as such in the UI. */
    val visibleReports: List<Report> get() = reports.take(MAX_VISIBLE_REPORTS)

    val hasMoreThanVisible: Boolean get() = reports.size > MAX_VISIBLE_REPORTS

    private companion object {
        const val MAX_VISIBLE_REPORTS = 5
    }
}

/**
 * Who filed a report, and whether their word has held up before.
 *
 * The counts come from [ReporterProfile] (the reporter's whole record, which may
 * be larger than what this device has stored), while the list below it is only
 * the reports present locally.
 */
class ReporterViewModel(
    reportRepository: ReportRepository,
    reporterId: String,
    currentUserId: String?,
) : ViewModel() {

    val uiState: StateFlow<ReporterUiState> = combine(
        reportRepository.observeReporter(reporterId),
        reportRepository.observeReportsByReporter(reporterId),
    ) { profile, reports ->
        ReporterUiState(
            profile = profile,
            reports = reports,
            isSelf = reporterId == currentUserId,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReporterUiState(isSelf = reporterId == currentUserId),
    )
}
