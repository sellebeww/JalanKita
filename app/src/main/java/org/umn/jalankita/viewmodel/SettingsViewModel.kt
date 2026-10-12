package org.umn.jalankita.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.jalankita.data.local.SessionStore
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.ThemeMode
import org.umn.jalankita.util.ConnectivityMonitor

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLocalOnly: Boolean = false,
    val isOnline: Boolean = true,
    val unsyncedCount: Int = 0,
)

class SettingsViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
    private val sessionStore: SessionStore,
    networkMonitor: ConnectivityMonitor,
) : UserScopedViewModel(authRepository) {

    private val localOnly = !authRepository.isBackedByServer

    val uiState: StateFlow<SettingsUiState> = combine(
        sessionStore.themeMode,
        networkMonitor.isOnline,
        forUser(0) { reportRepository.observeUnsyncedCount(it) },
    ) { theme, online, unsynced ->
        SettingsUiState(
            themeMode = theme,
            isLocalOnly = localOnly,
            isOnline = online,
            unsyncedCount = unsynced,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(isLocalOnly = localOnly),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { sessionStore.setThemeMode(mode) }
    }
}
