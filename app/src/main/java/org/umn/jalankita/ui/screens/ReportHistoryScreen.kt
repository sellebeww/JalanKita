package org.umn.jalankita.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.EmptyState
import org.umn.jalankita.ui.components.LoadingView
import org.umn.jalankita.ui.components.OfflineBanner
import org.umn.jalankita.ui.components.ReportCard
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.PillShape
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.viewmodel.HistoryFilter
import org.umn.jalankita.viewmodel.HistoryUiState

@Composable
fun ReportHistoryScreen(
    state: HistoryUiState,
    onReportClick: (String) -> Unit,
    onNewReport: () -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onSyncNow: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Laporan Saya",
                subtitle = summaryLine(state),
                actions = {
                    if (state.unsyncedCount > 0) {
                        IconButton(onClick = onSyncNow, enabled = state.isOnline) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = "Sinkronkan sekarang",
                                tint = if (state.isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OfflineBanner(visible = !state.isOnline)

            when {
                state.isLoading -> LoadingView(message = "Memuat laporan...")

                state.reports.isEmpty() -> EmptyState(
                    title = "Belum Ada Laporan",
                    description = "Laporan yang Anda kirim akan muncul di sini, " +
                        "termasuk yang masih menunggu sinkronisasi.",
                    icon = Icons.Outlined.AddLocationAlt,
                    actionLabel = "Lapor Kerusakan",
                    onAction = onNewReport,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> {
                    FilterRow(state = state, onFilterChange = onFilterChange)

                    if (state.isFilteredEmpty) {
                        EmptyState(
                            title = "Tidak Ada Laporan",
                            description = "Belum ada laporan pada kategori " +
                                "\"${state.filter.label}\".",
                            icon = Icons.Outlined.FilterListOff,
                            actionLabel = "Tampilkan Semua",
                            onAction = { onFilterChange(HistoryFilter.ALL) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = Dimens.screenPadding,
                                end = Dimens.screenPadding,
                                top = Spacing.sm,
                                bottom = Spacing.xxl,
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            items(state.visibleReports, key = { it.id }) { report ->
                                ReportCard(report = report, onClick = { onReportClick(report.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: HistoryUiState, onFilterChange: (HistoryFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(HistoryFilter.entries, key = { it.name }) { filter ->
            val count = state.counts[filter] ?: 0
            FilterChip(
                selected = state.filter == filter,
                onClick = { onFilterChange(filter) },
                shape = PillShape,
                label = {
                    Text(
                        text = if (count > 0) "${filter.label} ($count)" else filter.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = state.filter == filter,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

private fun summaryLine(state: HistoryUiState): String? = when {
    state.isLoading || state.reports.isEmpty() -> null
    state.unsyncedCount > 0 -> "${state.reports.size} laporan · ${state.unsyncedCount} menunggu dikirim"
    else -> "${state.reports.size} laporan · semua tersinkron"
}
