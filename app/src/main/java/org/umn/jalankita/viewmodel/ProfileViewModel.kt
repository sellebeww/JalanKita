package org.umn.jalankita.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.data.repository.ReportStats
import org.umn.jalankita.model.UserProfile

data class ProfileUiState(
    val user: UserProfile? = null,
    val stats: ReportStats = ReportStats(),
    val isLocalOnly: Boolean = false,
    val isLoading: Boolean = true,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    reportRepository: ReportRepository,
) : UserScopedViewModel(authRepository) {

    val uiState: StateFlow<ProfileUiState> = combine(
        userFlow,
        forUser(ReportStats()) { reportRepository.observeStats(it) },
    ) { user, stats ->
        ProfileUiState(
            user = user,
            stats = stats,
            isLocalOnly = !authRepository.isBackedByServer,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(isLocalOnly = !authRepository.isBackedByServer),
    )
}
