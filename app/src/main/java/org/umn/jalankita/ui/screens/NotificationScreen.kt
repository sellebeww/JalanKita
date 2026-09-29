package org.umn.jalankita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.AppNotification
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.EmptyState
import org.umn.jalankita.ui.components.LoadingView
import org.umn.jalankita.ui.components.TertiaryButton
import org.umn.jalankita.ui.theme.BrightBlue
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.util.asRelativeTime
import org.umn.jalankita.viewmodel.NotificationUiState

@Composable
fun NotificationScreen(
    state: NotificationUiState,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Notifikasi",
                subtitle = when {
                    state.isLoading -> null
                    state.unreadCount > 0 -> "${state.unreadCount} belum dibaca"
                    state.notifications.isNotEmpty() -> "Semua sudah dibaca"
                    else -> null
                },
                actions = {
                    if (state.unreadCount > 0) {
                        TertiaryButton(text = "Tandai Dibaca", onClick = onMarkAllRead)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> LoadingView()

                state.notifications.isEmpty() -> EmptyState(
                    title = "Belum Ada Notifikasi",
                    description = "Pemberitahuan tentang status laporan Anda akan muncul di sini.",
                    icon = Icons.Outlined.NotificationsNone,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = Dimens.screenPadding,
                        end = Dimens.screenPadding,
                        top = Spacing.sm,
                        bottom = Spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationRow(notification) { onNotificationClick(notification) }
                    }
                }
            }
        }
    }
}

/**
 * Unread is marked with a tinted icon and a dot rather than by flooding the whole
 * card with the primary container colour, which made the list hard to read.
 */
@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val (icon, tint) = notification.visuals()

    AppCard(onClick = onClick, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(Dimens.iconMd),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!notification.isRead) {
                        Spacer(Modifier.width(Spacing.sm))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = notification.createdAt.asRelativeTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** Derived from the wording, so the icon matches what happened. */
private fun AppNotification.visuals(): Pair<ImageVector, Color> = when {
    title.contains("selesai", ignoreCase = true) -> Icons.Outlined.CheckCircle to StatusGreen
    title.contains("terverifikasi", ignoreCase = true) -> Icons.Outlined.Verified to StatusGreen
    title.contains("dikirim", ignoreCase = true) -> Icons.Outlined.CloudDone to StatusGreen
    title.contains("ditangani", ignoreCase = true) -> Icons.Outlined.Engineering to BrightBlue
    title.contains("butuh", ignoreCase = true) -> Icons.Outlined.ErrorOutline to StatusOrange
    title.contains("tersimpan", ignoreCase = true) -> Icons.Outlined.CloudSync to StatusOrange
    else -> Icons.Outlined.NotificationsNone to BrightBlue
}
