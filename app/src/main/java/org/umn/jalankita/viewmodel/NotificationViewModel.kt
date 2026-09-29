package org.umn.jalankita.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.AppNotification

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
)

class NotificationViewModel(
    authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
) : UserScopedViewModel(authRepository) {

    val uiState: StateFlow<NotificationUiState> = combine(
        forUser(emptyList<AppNotification>()) { reportRepository.observeNotifications(it) },
        forUser(0) { reportRepository.observeUnreadNotificationCount(it) },
    ) { notifications, unread ->
        NotificationUiState(notifications = notifications, unreadCount = unread, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationUiState(),
    )

    fun markAsRead(notificationId: String) {
        viewModelScope.launch { reportRepository.markNotificationRead(notificationId) }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val user = userFlow.first() ?: return@launch
            reportRepository.markAllNotificationsRead(user.id)
        }
    }
}
