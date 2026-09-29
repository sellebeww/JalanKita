package org.umn.jalankita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReporterProfile
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.DetailCard
import org.umn.jalankita.ui.components.DetailRow
import org.umn.jalankita.ui.components.EmptyState
import org.umn.jalankita.ui.components.LoadingView
import org.umn.jalankita.ui.components.ReportImage
import org.umn.jalankita.ui.components.ReportLocationMap
import org.umn.jalankita.ui.components.ReportStatusBadge
import org.umn.jalankita.ui.components.RowDivider
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.components.SectionHeader
import org.umn.jalankita.ui.components.StatusBadge
import org.umn.jalankita.ui.components.badgeTone
import org.umn.jalankita.ui.components.SyncStatusBadge
import org.umn.jalankita.ui.components.accentColor
import org.umn.jalankita.ui.components.tone
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.util.asCoordinate
import org.umn.jalankita.util.asReportDateTime
import org.umn.jalankita.viewmodel.ReportDetailUiState

@Composable
fun ReportDetailScreen(
    state: ReportDetailUiState,
    isMapsConfigured: Boolean,
    onRetrySync: () -> Unit,
    onReporterClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Detail Laporan", onBack = onBack) },
    ) { innerPadding ->
        val report = state.report
        when {
            state.isLoading -> LoadingView(modifier = Modifier.padding(innerPadding))

            report == null -> EmptyState(
                title = "Laporan Tidak Ditemukan",
                description = "Laporan ini mungkin sudah dihapus dari perangkat.",
                icon = Icons.Outlined.ErrorOutline,
                actionLabel = "Kembali",
                onAction = onBack,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> ReportDetailContent(
                report = report,
                reporter = state.reporter,
                isMapsConfigured = isMapsConfigured,
                canRetrySync = state.canRetrySync,
                onRetrySync = onRetrySync,
                onReporterClick = { onReporterClick(report.userId) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding),
            )
        }
    }
}

@Composable
private fun ReportDetailContent(
    report: Report,
    reporter: ReporterProfile?,
    isMapsConfigured: Boolean,
    canRetrySync: Boolean,
    onRetrySync: () -> Unit,
    onReporterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ReportImage(
            report = report,
            modifier = Modifier.fillMaxWidth().height(Dimens.heroImage),
            shape = MaterialTheme.shapes.large,
        )

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = report.category.label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = report.category.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ReportStatusBadge(report.reportStatus)
            SyncStatusBadge(report.syncStatus)
        }

        Spacer(Modifier.height(Spacing.lg))
        ReporterRow(report = report, reporter = reporter, onClick = onReporterClick)

        Spacer(Modifier.height(Spacing.md))
        StatusTimeline(report.reportStatus)

        if (report.syncStatus != SyncStatus.SYNCED) {
            Spacer(Modifier.height(Spacing.md))
            SyncNotice(
                report = report,
                canRetrySync = canRetrySync,
                onRetrySync = onRetrySync,
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionHeader(title = "Deskripsi")
        Spacer(Modifier.height(Spacing.sm))
        AppCard {
            Text(
                text = report.description?.takeIf { it.isNotBlank() } ?: "Tidak ada deskripsi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionHeader(title = "Lokasi")
        Spacer(Modifier.height(Spacing.sm))
        ReportLocationMap(
            latitude = report.latitude,
            longitude = report.longitude,
            isMapsConfigured = isMapsConfigured,
            modifier = Modifier.fillMaxWidth().height(Dimens.mapDetail),
        )
        Spacer(Modifier.height(Spacing.sm))
        DetailCard {
            DetailRow(
                label = "Nama Jalan",
                value = report.streetName?.takeIf { it.isNotBlank() }
                    ?: "Tersimpan berdasarkan koordinat GPS.",
            )
            RowDivider()
            DetailRow("Latitude", report.latitude.asCoordinate())
            RowDivider()
            DetailRow("Longitude", report.longitude.asCoordinate())
            RowDivider()
            DetailRow("Dikirim", report.createdAt.asReportDateTime())
        }

        Spacer(Modifier.height(Spacing.xxl))
    }
}

/**
 * Who filed this, and whether their word has held up before.
 *
 * Tapping through matters: a photo of a hole is only as good as the person who
 * took it, so the reporter's record has to be one tap from the report itself.
 */
@Composable
private fun ReporterRow(report: Report, reporter: ReporterProfile?, onClick: () -> Unit) {
    AppCard(onClick = onClick, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = reporter?.initials ?: report.displayReporter.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Dilaporkan oleh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = reporter?.name ?: report.displayReporter,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reporter != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        StatusBadge(
                            text = reporter.level.label,
                            tone = reporter.level.badgeTone(),
                        )
                        if (reporter.hasTrackRecord) {
                            Text(
                                text = "Akurasi ${reporter.accuracyPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
    }
}

/**
 * The authority's workflow as a track, so "Menunggu Verifikasi" is visibly a
 * step in a process rather than a dead end. NEEDS_INFO is off the happy path and
 * gets its own line instead of a position on it.
 */
@Composable
private fun StatusTimeline(status: ReportStatus) {
    if (status == ReportStatus.NEEDS_INFO) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = status.tone().accentColor(),
                    modifier = Modifier.size(Dimens.iconLg),
                )
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = "Butuh Info Tambahan",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Petugas meminta keterangan atau foto tambahan sebelum " +
                            "laporan dapat diproses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    val track = listOf(ReportStatus.PENDING, ReportStatus.VERIFIED, ReportStatus.IN_PROGRESS, ReportStatus.COMPLETED)
    val reachedIndex = track.indexOf(status).coerceAtLeast(0)

    AppCard {
        Text(
            text = "Perjalanan Laporan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.Top) {
            track.forEachIndexed { index, step ->
                val reached = index <= reachedIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                color = if (reached) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = MaterialTheme.shapes.extraSmall,
                            ),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (reached) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = step.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reached) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (index != track.lastIndex) Spacer(Modifier.width(Spacing.xs))
            }
        }
    }
}

@Composable
private fun SyncNotice(report: Report, canRetrySync: Boolean, onRetrySync: () -> Unit) {
    AppCard(contentPadding = PaddingValues(Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CloudSync,
                contentDescription = null,
                tint = report.syncStatus.tone().accentColor(),
                modifier = Modifier.size(Dimens.iconLg),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = report.syncStatus.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when (report.syncStatus) {
                        SyncStatus.PENDING_SYNC ->
                            "Tersimpan di perangkat. Akan dikirim otomatis saat koneksi tersedia."
                        SyncStatus.SYNCING -> "Sedang dikirim ke server..."
                        SyncStatus.FAILED -> report.lastSyncError?.takeIf { it.isNotBlank() }
                            ?: "Pengiriman terakhir gagal. Laporan Anda tetap aman di perangkat."
                        SyncStatus.SYNCED -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (canRetrySync) {
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = "Kirim Sekarang",
                onClick = onRetrySync,
                icon = Icons.Outlined.CloudSync,
            )
        }
    }
}

private fun ReportStatus.shortLabel(): String = when (this) {
    ReportStatus.PENDING -> "Menunggu"
    ReportStatus.VERIFIED -> "Verifikasi"
    ReportStatus.IN_PROGRESS -> "Ditangani"
    ReportStatus.COMPLETED -> "Selesai"
    ReportStatus.NEEDS_INFO -> "Butuh Info"
}
