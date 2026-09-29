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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.ReporterLevel
import org.umn.jalankita.model.ReporterProfile
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.EmptyState
import org.umn.jalankita.ui.components.LoadingView
import org.umn.jalankita.ui.components.ReportCard
import org.umn.jalankita.ui.components.SectionHeader
import org.umn.jalankita.ui.components.StatusBadge
import org.umn.jalankita.ui.components.TrustMeter
import org.umn.jalankita.ui.components.badgeTone
import org.umn.jalankita.ui.theme.BrandGradientEnd
import org.umn.jalankita.ui.theme.BrandGradientStart
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.util.asReportDate
import org.umn.jalankita.viewmodel.ReporterUiState

/**
 * Who filed a report, and whether their word has held up.
 *
 * Anyone can post a photo of a hole; what makes a report worth acting on is the
 * reporter's record. This screen shows that record plainly — how much they have
 * reported, how much of it an authority confirmed, and how much was sent back —
 * and refuses to score anyone until there is enough history to score.
 */
@Composable
fun ReporterProfileScreen(
    state: ReporterUiState,
    onReportClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = if (state.isSelf) "Profil Saya" else "Profil Pelapor",
                subtitle = "Rekam jejak pelaporan",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        val profile = state.profile
        when {
            state.isLoading -> LoadingView(modifier = Modifier.padding(innerPadding))

            profile == null -> EmptyState(
                title = "Pelapor Tidak Ditemukan",
                description = "Rekam jejak pelapor ini belum tersedia di perangkat Anda.",
                icon = Icons.Outlined.ErrorOutline,
                actionLabel = "Kembali",
                onAction = onBack,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                IdentityHeader(profile, isSelf = state.isSelf)
                TrustCard(profile)
                TrackRecordCard(profile)

                if (state.reports.isNotEmpty()) {
                    SectionHeader(
                        title = if (state.isSelf) "Laporan Saya" else "Laporan dari pelapor ini",
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    state.visibleReports.forEach { report ->
                        ReportCard(report = report, onClick = { onReportClick(report.id) })
                    }
                    if (state.hasMoreThanVisible) {
                        Text(
                            text = "Menampilkan ${state.visibleReports.size} dari " +
                                "${state.reports.size} laporan yang tersimpan di perangkat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Composable
private fun IdentityHeader(profile: ReporterProfile, isSelf: Boolean) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrandGradientStart, BrandGradientEnd)))
                .padding(Spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Dimens.avatarMd)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (profile.level == ReporterLevel.VERIFIED) {
                            Spacer(Modifier.width(Spacing.xs))
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = profile.level.label,
                                tint = Color.White,
                                modifier = Modifier.size(Dimens.iconMd),
                            )
                        }
                    }
                    Text(
                        text = if (isSelf) {
                            "Begini laporan Anda terlihat oleh warga lain"
                        } else {
                            "Bergabung sejak ${profile.joinedAt.asReportDate()}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustCard(profile: ReporterProfile) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TrustMeter(
                score = profile.trustScore,
                level = profile.level,
                enabled = profile.hasTrackRecord,
            )
            Spacer(Modifier.height(Spacing.md))
            StatusBadge(text = profile.level.label, tone = profile.level.badgeTone())
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = profile.level.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (profile.hasTrackRecord) {
            Spacer(Modifier.height(Spacing.lg))
            AccuracyBar(profile)
        } else {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Skor muncul setelah minimal 3 laporan ditinjau petugas. " +
                    "Sampai saat itu, nilai laporan dari isinya — bukan dari skor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AccuracyBar(profile: ReporterProfile) {
    val color = when {
        profile.accuracyPercent >= 80 -> StatusGreen
        profile.accuracyPercent >= 55 -> StatusOrange
        else -> MaterialTheme.colorScheme.error
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Akurasi laporan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${profile.accuracyPercent}%",
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { profile.accuracy },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "${profile.confirmedReports} dari ${profile.reviewedReports} laporan yang " +
                "ditinjau terbukti benar di lapangan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrackRecordCard(profile: ReporterProfile) {
    AppCard {
        Text(
            text = "Rekam Jejak",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RecordTile("Total", profile.totalReports, Modifier.weight(1f))
            RecordTile("Dikonfirmasi", profile.confirmedReports, Modifier.weight(1f))
            RecordTile("Selesai", profile.completedReports, Modifier.weight(1f))
            RecordTile("Butuh Info", profile.needsInfoReports, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSm),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "\"Dikonfirmasi\" berarti petugas memastikan kerusakannya nyata. " +
                    "\"Butuh Info\" berarti laporan dikembalikan karena belum bisa " +
                    "ditindaklanjuti. ${profile.pendingReports} laporan masih menunggu " +
                    "peninjauan dan tidak dihitung dalam akurasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(vertical = Spacing.md, horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
