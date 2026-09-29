package org.umn.jalankita.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.DamageSeverity
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.ui.theme.BrightBlue
import org.umn.jalankita.ui.theme.LocalIsDarkTheme
import org.umn.jalankita.ui.theme.PillShape
import org.umn.jalankita.ui.theme.BrightBlueLight
import org.umn.jalankita.ui.theme.StatusBlueContainer
import org.umn.jalankita.ui.theme.StatusBlueContainerDark
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusGreenContainer
import org.umn.jalankita.ui.theme.StatusGreenContainerDark
import org.umn.jalankita.ui.theme.StatusGrey
import org.umn.jalankita.ui.theme.StatusGreyContainer
import org.umn.jalankita.ui.theme.StatusGreyContainerDark
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.ui.theme.StatusOrangeContainer
import org.umn.jalankita.ui.theme.StatusOrangeContainerDark
import org.umn.jalankita.ui.theme.StatusRed
import org.umn.jalankita.ui.theme.StatusRedContainer
import org.umn.jalankita.ui.theme.StatusRedContainerDark

enum class BadgeTone { Green, Orange, Red, Blue, Neutral }

@Composable
private fun BadgeTone.containerColor(): Color {
    val dark = LocalIsDarkTheme.current
    return when (this) {
        BadgeTone.Green -> if (dark) StatusGreenContainerDark else StatusGreenContainer
        BadgeTone.Orange -> if (dark) StatusOrangeContainerDark else StatusOrangeContainer
        BadgeTone.Red -> if (dark) StatusRedContainerDark else StatusRedContainer
        BadgeTone.Blue -> if (dark) StatusBlueContainerDark else StatusBlueContainer
        BadgeTone.Neutral -> if (dark) StatusGreyContainerDark else StatusGreyContainer
    }
}

@Composable
private fun BadgeTone.contentColor(): Color {
    val dark = LocalIsDarkTheme.current
    return when (this) {
        BadgeTone.Green -> if (dark) Color(0xFF86EFAC) else Color(0xFF166534)
        BadgeTone.Orange -> if (dark) Color(0xFFFCD34D) else Color(0xFF92400E)
        BadgeTone.Red -> if (dark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
        BadgeTone.Blue -> if (dark) BrightBlueLight else Color(0xFF1E3A8A)
        BadgeTone.Neutral -> if (dark) Color(0xFFCBD5E1) else Color(0xFF334155)
    }
}

/** Solid accent for the same semantics — used for dots and icons outside badges. */
fun BadgeTone.accentColor(): Color = when (this) {
    BadgeTone.Green -> StatusGreen
    BadgeTone.Orange -> StatusOrange
    BadgeTone.Red -> StatusRed
    BadgeTone.Blue -> BrightBlue
    BadgeTone.Neutral -> StatusGrey
}

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        color = tone.containerColor(),
        contentColor = tone.contentColor(),
        shape = PillShape,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 5.dp),
                )
            } else {
                Text(text = text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

fun ReportStatus.tone(): BadgeTone = when (this) {
    ReportStatus.PENDING -> BadgeTone.Orange
    ReportStatus.VERIFIED -> BadgeTone.Blue
    ReportStatus.IN_PROGRESS -> BadgeTone.Blue
    ReportStatus.COMPLETED -> BadgeTone.Green
    ReportStatus.NEEDS_INFO -> BadgeTone.Red
}

private fun ReportStatus.icon(): ImageVector = when (this) {
    ReportStatus.PENDING -> Icons.Outlined.HourglassEmpty
    ReportStatus.VERIFIED -> Icons.Outlined.Verified
    ReportStatus.IN_PROGRESS -> Icons.Outlined.Info
    ReportStatus.COMPLETED -> Icons.Outlined.CheckCircle
    ReportStatus.NEEDS_INFO -> Icons.Outlined.ErrorOutline
}

fun SyncStatus.tone(): BadgeTone = when (this) {
    SyncStatus.PENDING_SYNC -> BadgeTone.Orange
    SyncStatus.SYNCING -> BadgeTone.Blue
    SyncStatus.SYNCED -> BadgeTone.Green
    SyncStatus.FAILED -> BadgeTone.Red
}

private fun SyncStatus.icon(): ImageVector = when (this) {
    SyncStatus.PENDING_SYNC -> Icons.Outlined.CloudOff
    SyncStatus.SYNCING -> Icons.Outlined.CloudUpload
    SyncStatus.SYNCED -> Icons.Outlined.CheckCircle
    SyncStatus.FAILED -> Icons.Outlined.ErrorOutline
}

@Composable
fun ReportStatusBadge(status: ReportStatus, modifier: Modifier = Modifier, showIcon: Boolean = true) {
    StatusBadge(
        text = status.label,
        tone = status.tone(),
        icon = if (showIcon) status.icon() else null,
        modifier = modifier,
    )
}

@Composable
fun SyncStatusBadge(status: SyncStatus, modifier: Modifier = Modifier, showIcon: Boolean = true) {
    StatusBadge(
        text = status.label,
        tone = status.tone(),
        icon = if (showIcon) status.icon() else null,
        modifier = modifier,
    )
}

/** How urgent a category of damage usually is — used by the example guide. */
fun DamageSeverity.tone(): BadgeTone = when (this) {
    DamageSeverity.LOW -> BadgeTone.Blue
    DamageSeverity.MEDIUM -> BadgeTone.Orange
    DamageSeverity.HIGH -> BadgeTone.Red
}

@Composable
fun SeverityBadge(severity: DamageSeverity, modifier: Modifier = Modifier) {
    StatusBadge(
        text = "Dampak ${severity.label}",
        tone = severity.tone(),
        modifier = modifier,
    )
}
