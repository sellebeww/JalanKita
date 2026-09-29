package org.umn.jalankita.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.umn.jalankita.data.location.NearbyAlertMonitor
import org.umn.jalankita.data.location.NearbyDamage
import org.umn.jalankita.data.location.NearbyState
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.ui.theme.StatusRed
import org.umn.jalankita.util.formatDistance

/**
 * "Is the road I am on right now damaged?"
 *
 * Four honest states, because a proximity warning that cannot tell "nothing
 * nearby" from "we have no idea where you are" is worse than no warning at all.
 */
@Composable
fun NearbyAlertCard(
    state: NearbyState,
    onRequestLocation: () -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        !state.hasPermission -> PermissionPrompt(onRequestLocation, modifier)
        state.origin == null -> LocatingCard(modifier)
        state.warnings.isNotEmpty() -> WarningCard(state.warnings, onReportClick, modifier)
        state.nearby.isNotEmpty() -> NearbyListCard(state.nearby, onReportClick, modifier)
        else -> ClearCard(modifier)
    }
}

@Composable
private fun PermissionPrompt(onRequestLocation: () -> Unit, modifier: Modifier) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            AlertIcon(Icons.Outlined.MyLocation, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Peringatan jalan rusak",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Izinkan akses lokasi agar JalanKita memberi tahu saat ada " +
                        "kerusakan jalan di dekat Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        SecondaryButton(
            text = "Izinkan Lokasi",
            onClick = onRequestLocation,
            icon = Icons.Outlined.MyLocation,
        )
    }
}

@Composable
private fun LocatingCard(modifier: Modifier) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlertIcon(Icons.Outlined.NearMe, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Mencari lokasi Anda...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Peringatan kerusakan di sekitar akan muncul di sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Damage within [NearbyAlertMonitor.WARNING_RADIUS_METERS]. Tinted red and given
 * a slow pulse — this is the one state that earns the user's attention.
 */
@Composable
private fun WarningCard(
    warnings: List<NearbyDamage>,
    onReportClick: (String) -> Unit,
    modifier: Modifier,
) {
    val transition = rememberInfiniteTransition(label = "nearbyPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Reverse),
        label = "nearbyPulseAlpha",
    )
    val closest = warnings.first()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .border(1.dp, StatusRed.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .clickable { onReportClick(closest.report.id) }
            .padding(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(pulse)
                        .background(StatusRed.copy(alpha = 0.22f), CircleShape),
                )
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(Dimens.iconLg),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Hati-hati · ${formatDistance(closest.distanceMeters)} dari Anda",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusRed,
                )
                Text(
                    text = closest.report.category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = closest.report.streetName?.takeIf { it.isNotBlank() }
                        ?: "Dilaporkan oleh ${closest.report.displayReporter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
            )
        }

        if (warnings.size > 1) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Juga dalam radius 300 m:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
            // Listed rather than counted: a hazard the user cannot open is a
            // hazard they cannot check.
            warnings.drop(1).forEach { item ->
                NearbyRow(item, onClick = { onReportClick(item.report.id) })
            }
        }
    }
}

@Composable
private fun NearbyListCard(
    nearby: List<NearbyDamage>,
    onReportClick: (String) -> Unit,
    modifier: Modifier,
) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlertIcon(Icons.Outlined.NearMe, StatusOrange)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${nearby.size} kerusakan di sekitar Anda",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Dalam radius 2 km, belum ada yang sangat dekat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        nearby.take(3).forEach { item ->
            NearbyRow(item, onClick = { onReportClick(item.report.id) })
        }
    }
}

@Composable
private fun NearbyRow(item: NearbyDamage, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        DamageIllustration(
            category = item.report.category,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.size(36.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.report.category.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.report.streetName?.takeIf { it.isNotBlank() } ?: "Lokasi GPS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDistance(item.distanceMeters),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ClearCard(modifier: Modifier) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlertIcon(Icons.Outlined.CheckCircle, StatusGreen)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Belum ada kerusakan terdata",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tidak ada kerusakan terdata dalam radius 2 km.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlertIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Dimens.iconMd))
    }
}
