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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.DamageIllustration
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.SectionHeader
import org.umn.jalankita.ui.components.SeverityBadge
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing

/**
 * Worked examples of road damage.
 *
 * The point of the app is hard to grasp from an empty form, so this screen shows
 * what each category actually looks like, a realistic case for it, and what to
 * get in the photo. It is reachable from Beranda, from Profil → Bantuan, and from
 * the category picker in the report form.
 */
@Composable
fun DamageGuideScreen(
    onBack: () -> Unit,
    onStartReport: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Panduan Kerusakan",
                subtitle = "Contoh kondisi jalan dan cara melaporkannya",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = Dimens.screenPadding,
                end = Dimens.screenPadding,
                top = Spacing.sm,
                bottom = Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { HowItWorksCard() }

            item {
                SectionHeader(
                    title = "Contoh Kondisi Jalan Rusak",
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }

            items(DamageCategory.showcase, key = { it.id }) { category ->
                DamageExampleCard(category)
            }

            item {
                SectionHeader(
                    title = "Kategori Lainnya",
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }

            items(
                items = DamageCategory.entries.filterNot { it in DamageCategory.showcase },
                key = { it.id },
            ) { category ->
                CompactCategoryRow(category)
            }

            item {
                Spacer(Modifier.height(Spacing.sm))
                PrimaryButton(
                    text = "Lapor Kerusakan Sekarang",
                    onClick = onStartReport,
                    icon = Icons.Outlined.CameraAlt,
                )
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    AppCard {
        Text(
            text = "Tiga langkah melapor",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Laporan tersimpan di perangkat lebih dulu, lalu terkirim sendiri " +
                "saat koneksi tersedia. Anda tidak perlu menunggu sinyal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        StepRow(1, Icons.Outlined.PhotoCamera, "Foto kerusakan", "Ambil satu foto yang jelas dari jarak aman.")
        Spacer(Modifier.height(Spacing.md))
        StepRow(2, Icons.Outlined.PinDrop, "Tandai lokasi", "Titik GPS terdeteksi otomatis, bisa digeser manual.")
        Spacer(Modifier.height(Spacing.md))
        StepRow(3, Icons.AutoMirrored.Outlined.Send, "Kirim laporan", "Pilih kategori, tambahkan keterangan, lalu kirim.")
    }
}

@Composable
private fun StepRow(number: Int, icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
        Spacer(Modifier.size(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = "$number. $title",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DamageExampleCard(category: DamageCategory) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        DamageIllustration(
            category = category,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(Spacing.xs),
        )
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                SeverityBadge(category.severity)
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))
            HintBlock(label = "Contoh kasus", body = category.example)
            Spacer(Modifier.height(Spacing.sm))
            HintBlock(label = "Tips foto", body = category.photoTip)
        }
    }
}

@Composable
private fun HintBlock(label: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactCategoryRow(category: DamageCategory) {
    AppCard(contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DamageIllustration(
                category = category,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
