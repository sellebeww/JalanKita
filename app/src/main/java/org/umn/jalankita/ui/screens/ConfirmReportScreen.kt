package org.umn.jalankita.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.DamageIllustration
import org.umn.jalankita.ui.components.DetailCard
import org.umn.jalankita.ui.components.DetailRow
import org.umn.jalankita.ui.components.OfflineBanner
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.RowDivider
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.util.asCoordinate
import org.umn.jalankita.viewmodel.ReportDraftState
import java.io.File

@Composable
fun ConfirmReportScreen(
    draft: ReportDraftState,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Konfirmasi Laporan",
                subtitle = "Langkah 2 dari 2 · Periksa sebelum mengirim",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            OfflineBanner(
                visible = !draft.isOnline,
                text = "Mode offline — laporan disimpan di perangkat dan dikirim otomatis nanti.",
            )

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding, vertical = Spacing.md),
            ) {
                if (draft.photoPath != null) {
                    AsyncImage(
                        model = File(draft.photoPath),
                        contentDescription = "Foto kerusakan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.heroImage)
                            .clip(MaterialTheme.shapes.large),
                    )
                    Spacer(Modifier.height(Spacing.lg))
                }

                draft.category?.let { category ->
                    AppCard(contentPadding = PaddingValues(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DamageIllustration(
                                category = category,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(60.dp),
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                }

                DetailCard {
                    DetailRow(
                        label = "Deskripsi",
                        value = draft.description.takeIf { it.isNotBlank() } ?: "Tidak diisi",
                    )
                    RowDivider()
                    DetailRow(
                        label = "Nama Jalan",
                        value = draft.streetName?.takeIf { it.isNotBlank() }
                            ?: "Tersimpan berdasarkan koordinat GPS.",
                    )
                    RowDivider()
                    DetailRow("Latitude", draft.location?.latitude?.asCoordinate() ?: "-")
                    RowDivider()
                    DetailRow("Longitude", draft.location?.longitude?.asCoordinate() ?: "-")
                }

                if (draft.submitError != null) {
                    Spacer(Modifier.height(Spacing.md))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconMd),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(draft.submitError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.md))
                AppCard(contentPadding = PaddingValues(Spacing.md)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconMd),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = "Laporan disimpan di perangkat terlebih dahulu, lalu dikirim " +
                                "ke server saat koneksi tersedia. Tidak ada yang hilang " +
                                "walau Anda sedang offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = Dimens.raisedElevation,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPadding, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(
                        text = "Edit",
                        onClick = onEdit,
                        enabled = !draft.isSubmitting,
                        fillWidth = false,
                        modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
                    )
                    PrimaryButton(
                        text = if (draft.isOnline) "Kirim Laporan" else "Simpan Laporan",
                        onClick = onSubmit,
                        icon = Icons.AutoMirrored.Outlined.Send,
                        enabled = draft.canSubmit,
                        loading = draft.isSubmitting,
                        fillWidth = false,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}
