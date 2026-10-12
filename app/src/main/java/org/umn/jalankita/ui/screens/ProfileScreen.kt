package org.umn.jalankita.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.umn.jalankita.BuildConfig
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTextField
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.LocalStorageBanner
import org.umn.jalankita.ui.components.LoadingView
import org.umn.jalankita.ui.components.RowDivider
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.theme.BrandGradientEnd
import org.umn.jalankita.ui.theme.BrandGradientStart
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.util.Validators
import org.umn.jalankita.viewmodel.ProfileUiState

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onUpdateName: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenPublicProfile: () -> Unit,
    onNewReport: () -> Unit,
    onSignOut: () -> Unit,
) {
    var dialog by remember { mutableStateOf<ProfileDialog?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Profil",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Pengaturan")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IdentityCard(
                name = state.user?.name?.takeIf { it.isNotBlank() } ?: "Pengguna JalanKita",
                email = state.user?.email.orEmpty(),
                initials = state.user?.initials.orEmpty(),
                onEdit = { dialog = ProfileDialog.EditName },
            )

            if (state.isLocalOnly) LocalStorageBanner()

            AppCard {
                Text(
                    text = "Ringkasan Laporan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatTile("Total", state.stats.total, Modifier.weight(1f))
                    StatTile("Menunggu", state.stats.pending, Modifier.weight(1f))
                    StatTile("Ditangani", state.stats.inProgress, Modifier.weight(1f))
                    StatTile("Selesai", state.stats.completed, Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.md))
                SecondaryButton(
                    text = "Buat Laporan Baru",
                    onClick = onNewReport,
                    icon = Icons.Outlined.AddAPhoto,
                )
            }

            AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
                MenuRow("Edit Profil", Icons.Outlined.Edit) { dialog = ProfileDialog.EditName }
                RowDivider(Modifier.padding(start = 56.dp))
                MenuRow(
                    label = "Rekam Jejak Pelapor",
                    icon = Icons.Outlined.Verified,
                    onClick = onOpenPublicProfile,
                )
                RowDivider(Modifier.padding(start = 56.dp))
                MenuRow("Panduan Kerusakan", Icons.AutoMirrored.Outlined.HelpOutline, onClick = onOpenGuide)
                RowDivider(Modifier.padding(start = 56.dp))
                MenuRow("Pengaturan", Icons.Outlined.Settings, onClick = onOpenSettings)
                RowDivider(Modifier.padding(start = 56.dp))
                MenuRow("Tentang Aplikasi", Icons.Outlined.Info) { dialog = ProfileDialog.About }
            }

            AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
                MenuRow(
                    label = "Keluar",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    destructive = true,
                    showChevron = false,
                ) { dialog = ProfileDialog.ConfirmLogout }
            }

            Spacer(Modifier.height(Spacing.xxl))
        }
    }

    when (dialog) {
        ProfileDialog.EditName -> EditNameDialog(
            initialName = state.user?.name.orEmpty(),
            onDismiss = { dialog = null },
            onConfirm = {
                onUpdateName(it)
                dialog = null
            },
        )

        ProfileDialog.About -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Tentang JalanKita") },
            text = {
                Text(
                    text = "Laporkan kerusakan jalan dengan mudah, cepat, dan tepat.\n\n" +
                        "Laporan selalu disimpan di perangkat lebih dulu, lalu dikirim " +
                        "otomatis saat koneksi tersedia.\n\n" +
                        "Versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                        "Backend: " + if (state.isLocalOnly) "penyimpanan lokal" else "Supabase",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Tutup") } },
        )

        ProfileDialog.ConfirmLogout -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Keluar dari akun?") },
            text = { Text("Laporan yang belum tersinkronisasi tetap tersimpan di perangkat.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    onSignOut()
                }) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Batal") } },
        )

        null -> Unit
    }
}

private enum class ProfileDialog { EditName, About, ConfirmLogout }

/** Brand-gradient header so Profil opens with the same identity as Login. */
@Composable
private fun IdentityCard(
    name: String,
    email: String,
    initials: String,
    onEdit: () -> Unit,
) {
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
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.size(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit profil",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
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
        )
    }
}

@Composable
private fun MenuRow(
    label: String,
    icon: ImageVector,
    destructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) contentColor else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.iconLg),
        )
        Spacer(Modifier.size(Spacing.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val error = if (name.isBlank()) null else Validators.nameError(name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profil") },
        text = {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nama Lengkap",
                placeholder = "Nama sesuai identitas",
                errorText = error,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = Validators.nameError(name) == null,
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
