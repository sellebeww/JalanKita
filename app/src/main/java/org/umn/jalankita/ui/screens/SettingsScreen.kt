package org.umn.jalankita.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import org.umn.jalankita.BuildConfig
import org.umn.jalankita.model.ThemeMode
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.LocalStorageBanner
import org.umn.jalankita.ui.components.DetailRow
import org.umn.jalankita.ui.components.RowDivider
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.permissions.openNotificationSettings
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.viewmodel.SettingsUiState

/**
 * Real settings, rather than the information dialog that used to sit behind
 * "Pengaturan": appearance, sync, notifications and the sample data are all
 * things the user can actually change here.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSyncNow: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Pengaturan", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.isLocalOnly) LocalStorageBanner()

            AppCard {
                SettingHeader(Icons.Outlined.SettingsBrightness, "Tampilan", "Pilih tema aplikasi.")
                Spacer(Modifier.height(Spacing.md))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            icon = {},
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = mode.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.iconSm),
                                    )
                                    Spacer(Modifier.size(Spacing.xs))
                                    Text(mode.shortLabel(), style = MaterialTheme.typography.labelMedium)
                                }
                            },
                        )
                    }
                }
            }

            AppCard {
                SettingHeader(
                    icon = Icons.Outlined.CloudSync,
                    title = "Sinkronisasi",
                    subtitle = if (state.isLocalOnly) "Laporan tersimpan di perangkat. Hubungkan Supabase untuk pengiriman." else "Laporan dikirim otomatis saat perangkat kembali online.",
                )
                Spacer(Modifier.height(Spacing.md))
                DetailRow("Koneksi", if (state.isOnline) "Online" else "Offline")
                RowDivider()
                DetailRow(
                    label = "Menunggu dikirim",
                    value = if (state.unsyncedCount == 0) {
                        "Tidak ada"
                    } else {
                        "${state.unsyncedCount} laporan"
                    },
                )
                Spacer(Modifier.height(Spacing.md))
                SecondaryButton(
                    text = if (state.isOnline) "Sinkronkan Sekarang" else "Menunggu Koneksi",
                    onClick = onSyncNow,
                    icon = Icons.Outlined.CloudSync,
                    enabled = !state.isLocalOnly && state.isOnline && state.unsyncedCount > 0,
                )
            }

            AppCard {
                SettingHeader(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Notifikasi",
                    subtitle = "Pemberitahuan status laporan diatur oleh sistem Android.",
                )
                Spacer(Modifier.height(Spacing.md))
                SecondaryButton(
                    text = "Buka Pengaturan Notifikasi",
                    onClick = { context.openNotificationSettings() },
                )
            }

            AppCard {
                DetailRow("Versi", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                RowDivider()
                DetailRow("Backend", if (state.isLocalOnly) "Penyimpanan lokal" else "Supabase")
            }

            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun SettingHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.iconMd),
        )
        Spacer(Modifier.size(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
}

private fun ThemeMode.shortLabel(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistem"
    ThemeMode.LIGHT -> "Terang"
    ThemeMode.DARK -> "Gelap"
}
