package org.umn.jalankita.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.permissions.hasNotificationPermission
import org.umn.jalankita.ui.permissions.rememberSinglePermissionLauncher
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusOrange

/**
 * Two honest outcomes: stored locally (offline) or already delivered.
 * Offline is never presented as a failure.
 */
@Composable
fun ReportSuccessScreen(
    synced: Boolean,
    onViewReports: () -> Unit,
    onBackToHome: () -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberSinglePermissionLauncher { }
    var shown by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.7f,
        animationSpec = tween(durationMillis = 420),
        label = "successScale",
    )

    // Asked here, not at startup: this is the first moment status updates matter.
    LaunchedEffect(Unit) {
        shown = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasNotificationPermission()
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenPadding, vertical = Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .scale(iconScale)
                    .background(
                        color = if (synced) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (synced) Icons.Outlined.CheckCircle else Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = if (synced) StatusGreen else StatusOrange,
                    modifier = Modifier.size(52.dp),
                )
            }

            Spacer(Modifier.height(Spacing.xxl))
            Text(
                text = if (synced) "Laporan Berhasil Terkirim" else "Laporan Berhasil Disimpan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = if (synced) {
                    "Laporan Anda sudah sampai di server dan menunggu verifikasi petugas."
                } else {
                    "Laporan Anda tersimpan di perangkat dan akan otomatis dikirim " +
                        "saat internet tersedia."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp),
            )

            Spacer(Modifier.height(Spacing.xxl))
            AppCard(modifier = Modifier.widthIn(max = 420.dp)) {
                Text(
                    text = "Apa yang terjadi selanjutnya?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Petugas memverifikasi laporan, lalu statusnya berubah menjadi " +
                        "Sedang Ditangani dan Selesai. Anda akan menerima notifikasi " +
                        "pada setiap perubahan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.xxl))
            PrimaryButton(
                text = "Lihat Laporan Saya",
                onClick = onViewReports,
                modifier = Modifier.widthIn(max = 420.dp),
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = "Kembali ke Beranda",
                onClick = onBackToHome,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }
    }
}
