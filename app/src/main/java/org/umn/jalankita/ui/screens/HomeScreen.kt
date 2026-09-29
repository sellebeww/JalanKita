package org.umn.jalankita.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.ui.components.BrandLogo
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.DamageIllustration
import org.umn.jalankita.ui.components.EmptyState
import org.umn.jalankita.ui.components.NearbyAlertCard
import org.umn.jalankita.ui.components.OfflineBanner
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.ReportCard
import org.umn.jalankita.ui.components.ReportsMapPreview
import org.umn.jalankita.ui.components.SectionHeader
import org.umn.jalankita.ui.components.TertiaryButton
import org.umn.jalankita.ui.permissions.LOCATION_PERMISSIONS
import org.umn.jalankita.ui.permissions.rememberLocationPermissionLauncher
import org.umn.jalankita.ui.theme.BrandGradientEnd
import org.umn.jalankita.ui.theme.BrandGradientStart
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.viewmodel.HomeUiState
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: HomeUiState,
    isMapsConfigured: Boolean,
    onNewReport: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGuide: () -> Unit,
    onSyncNow: () -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onReportClick: (String) -> Unit,
) {
    // Location is asked for here, when the user opts into proximity warnings —
    // never at startup.
    val locationPermissionLauncher = rememberLocationPermissionLauncher { granted ->
        if (granted) onLocationPermissionGranted()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {

            GreetingBar(
                name = state.user?.name?.takeIf { it.isNotBlank() } ?: "Warga",
                unreadNotifications = state.unreadNotifications,
                onOpenNotifications = onOpenNotifications,
            )

            OfflineBanner(visible = !state.isOnline)

            LazyColumn(
                contentPadding = PaddingValues(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    top = Spacing.md,
                    bottom = Spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Immediate hazards keep priority; location onboarding comes after the main action.
                if (state.nearby.warnings.isNotEmpty()) {
                    item(key = "nearby-warning") {
                        NearbyAlertCard(
                            state = state.nearby,
                            onRequestLocation = { locationPermissionLauncher.launch(LOCATION_PERMISSIONS) },
                            onReportClick = onReportClick,
                        )
                    }
                }

                item(key = "report-action") { ReportCallToAction(onNewReport = onNewReport) }

                item(key = "quick-actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        QuickAction("Jelajahi peta", "Kondisi jalan sekitar", Icons.Outlined.Map, onOpenMap, Modifier.weight(1f))
                        QuickAction("Cara melapor", "Kenali kerusakannya", Icons.AutoMirrored.Outlined.MenuBook, onOpenGuide, Modifier.weight(1f))
                    }
                }

                item {
                    SectionHeader(
                        title = "Kontribusi Anda",
                        action = { TertiaryButton(text = "Laporan saya", onClick = onOpenHistory) },
                    )
                }

                item {
                    StatsRow(
                        total = state.stats.total,
                        inProgress = state.stats.inProgress,
                        completed = state.stats.completed,
                        onOpenHistory = onOpenHistory,
                    )
                }

                if (state.nearby.warnings.isEmpty()) {
                    item(key = "nearby-info") {
                        NearbyAlertCard(
                            state = state.nearby,
                            onRequestLocation = { locationPermissionLauncher.launch(LOCATION_PERMISSIONS) },
                            onReportClick = onReportClick,
                        )
                    }
                }

                if (state.unsyncedCount > 0) {
                    item {
                        PendingSyncCard(
                            count = state.unsyncedCount,
                            isOnline = state.isOnline,
                            onSyncNow = onSyncNow,
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "Peta Kerusakan",
                        action = { TertiaryButton(text = "Buka Peta", onClick = onOpenMap) },
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                item {
                    ReportsMapPreview(
                        reports = state.communityReports,
                        isMapsConfigured = isMapsConfigured,
                        onClick = onOpenMap,
                        modifier = Modifier.fillMaxWidth().height(Dimens.mapPreview),
                    )
                }

                item {
                    SectionHeader(
                        title = "Contoh Kerusakan Jalan",
                        action = { TertiaryButton(text = "Panduan", onClick = onOpenGuide) },
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                item {
                    Text(
                        text = "Kenali kondisi jalan yang perlu dilaporkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { DamageExampleStrip(onOpenGuide = onOpenGuide) }

                item {
                    SectionHeader(
                        title = "Laporan Terbaru",
                        action = {
                            if (state.recentReports.isNotEmpty()) {
                                TertiaryButton(text = "Lihat Semua", onClick = onOpenHistory)
                            }
                        },
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }

                if (state.recentReports.isEmpty()) {
                    item {
                        AppCard(contentPadding = PaddingValues(0.dp)) {
                            EmptyState(
                                title = "Belum Ada Laporan",
                                description = "Mulai dengan melaporkan kerusakan jalan " +
                                    "di sekitar Anda.",
                                icon = Icons.Outlined.AddLocationAlt,
                                actionLabel = "Lapor Kerusakan",
                                onAction = onNewReport,
                                secondaryActionLabel = "Lihat Contoh Kerusakan",
                                onSecondaryAction = onOpenGuide,
                            )
                        }
                    }
                } else {
                    items(state.recentReports, key = { it.id }) { report ->
                        ReportCard(report = report, onClick = { onReportClick(report.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingBar(
    name: String,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.screenPadding, end = Spacing.sm, top = Spacing.md, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandLogo(size = 44.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${greeting()} ${name.substringBefore(' ')}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "JalanKita",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onOpenNotifications) {
            if (unreadNotifications > 0) {
                BadgedBox(
                    badge = { Badge { Text(unreadNotifications.coerceAtMost(99).toString()) } },
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifikasi, $unreadNotifications belum dibaca")
                }
            } else {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifikasi")
            }
        }
    }
}

/** A clear first action, with a high-contrast button on the brand surface. */
@Composable
private fun ReportCallToAction(onNewReport: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(BrandGradientStart, BrandGradientEnd))),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val route = Path().apply {
                moveTo(size.width * 0.96f, -20f)
                cubicTo(size.width * 0.62f, size.height * 0.22f,
                    size.width * 1.2f, size.height * 0.34f,
                    size.width * 0.86f, size.height * 0.65f)
                cubicTo(size.width * 0.65f, size.height * 0.84f,
                    size.width * 0.94f, size.height, size.width * 1.1f, size.height)
            }
            drawPath(route, Color.White.copy(alpha = 0.06f), style = Stroke(width = 38.dp.toPx()))
            drawPath(route, Color.White.copy(alpha = 0.12f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = 0.14f), 8.dp.toPx(), Offset(size.width * 0.85f, size.height * 0.18f))
        }
        Column(Modifier.padding(Spacing.xl)) {
            Text(
                text = "BERSAMA, KITA BISA",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Jalan lebih baik,\nmulai dari kita.",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Temukan kerusakan? Foto dan tandai lokasinya. Satu laporan Anda berarti.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = "Lapor Kerusakan",
                onClick = onNewReport,
                icon = Icons.Outlined.AddAPhoto,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BrandGradientStart,
                ),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Tanpa sinyal? Laporan tetap tersimpan.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(Spacing.md)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(Spacing.sm))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsRow(total: Int, inProgress: Int, completed: Int, onOpenHistory: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatTile("Total", total, Icons.Outlined.Description, onOpenHistory, Modifier.weight(1f))
        StatTile("Ditangani", inProgress, Icons.Outlined.Build, onOpenHistory, Modifier.weight(1f))
        StatTile("Selesai", completed, Icons.Outlined.CheckCircle, onOpenHistory, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: Int, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(Spacing.md)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Pending sync is normal, not an error — hence the neutral card and the plain
 * explanation, with the manual trigger only offered when it could do something.
 */
@Composable
private fun PendingSyncCard(count: Int, isOnline: Boolean, onSyncNow: () -> Unit) {
    AppCard(contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = StatusOrange,
                    modifier = Modifier.size(Dimens.iconMd),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "$count laporan menunggu dikirim",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isOnline) {
                        "Akan dikirim otomatis. Anda juga bisa memicunya sekarang."
                    } else {
                        "Tersimpan aman di perangkat sampai koneksi tersedia."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isOnline) {
                TertiaryButton(text = "Kirim", onClick = onSyncNow)
            }
        }
    }
}

@Composable
private fun DamageExampleStrip(onOpenGuide: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        items(DamageCategory.showcase, key = { it.id }) { category ->
            DamageExampleTile(category = category, onClick = onOpenGuide)
        }
        item {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(164.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onOpenGuide),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "Lihat Semua",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * A card rather than a bare column: clipping the column to a rounded shape cut
 * the first letter of the caption out of the bottom-left corner, and a card also
 * matches every other surface in the app.
 */
@Composable
private fun DamageExampleTile(category: DamageCategory, onClick: () -> Unit) {
    Box(Modifier.width(150.dp)) {
        AppCard(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            DamageIllustration(
                category = category,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(Spacing.xs),
            )
            Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Dampak ${category.severity.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 4..10 -> "Selamat pagi,"
    in 11..14 -> "Selamat siang,"
    in 15..18 -> "Selamat sore,"
    else -> "Selamat malam,"
}
