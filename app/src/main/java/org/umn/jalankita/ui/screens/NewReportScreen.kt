package org.umn.jalankita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.umn.jalankita.data.location.LocationFailure
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTextField
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.DamageIllustration
import org.umn.jalankita.ui.components.OfflineBanner
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.ReportLocationMap
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.components.SectionCard
import org.umn.jalankita.ui.permissions.LOCATION_PERMISSIONS
import org.umn.jalankita.ui.permissions.hasAnyLocationPermission
import org.umn.jalankita.ui.permissions.openAppSettings
import org.umn.jalankita.ui.permissions.openLocationSettings
import org.umn.jalankita.ui.permissions.rememberLocationPermissionLauncher
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.util.asCoordinate
import org.umn.jalankita.viewmodel.LocationUiStatus
import org.umn.jalankita.viewmodel.ReportDraftState
import java.io.File

private const val DESCRIPTION_LIMIT = 300

@Composable
fun NewReportScreen(
    draft: ReportDraftState,
    isMapsConfigured: Boolean,
    onPermissionResult: (Boolean) -> Unit,
    onRetryLocation: () -> Unit,
    onPickLocationOnMap: () -> Unit,
    onTakePhoto: () -> Unit,
    onCategoryChange: (DamageCategory) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        permissionAsked = true
        onPermissionResult(granted)
    }

    // Location permission is requested here — when the report actually starts —
    // and never at app startup.
    LaunchedEffect(Unit) {
        if (context.hasAnyLocationPermission()) {
            onPermissionResult(true)
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Lapor Kerusakan",
                subtitle = "Langkah 1 dari 2 · Isi detail laporan",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenGuide) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Panduan kerusakan",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            OfflineBanner(visible = !draft.isOnline)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding, vertical = Spacing.md),
            ) {
                SectionCard(
                    title = "Lokasi Kerusakan",
                    icon = Icons.Outlined.PinDrop,
                    subtitle = "Titik ini yang akan dikunjungi petugas.",
                ) {
                    LocationSection(
                        draft = draft,
                        isMapsConfigured = isMapsConfigured,
                        onRetry = onRetryLocation,
                        onRequestPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                        onOpenAppSettings = { context.openAppSettings() },
                        onOpenLocationSettings = { context.openLocationSettings() },
                        onPickOnMap = onPickLocationOnMap,
                        permissionAsked = permissionAsked,
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                SectionCard(
                    title = "Foto Kerusakan",
                    icon = Icons.Outlined.PhotoCamera,
                    subtitle = "Satu foto yang jelas sudah cukup.",
                ) {
                    PhotoSection(
                        photoPath = draft.photoPath,
                        category = draft.category,
                        onTakePhoto = onTakePhoto,
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                SectionCard(
                    title = "Kategori Kerusakan",
                    icon = Icons.Outlined.Category,
                    subtitle = "Pilih yang paling mendekati kondisi di lapangan.",
                ) {
                    CategoryPicker(selected = draft.category, onSelect = onCategoryChange)
                }

                Spacer(Modifier.height(Spacing.md))

                SectionCard(
                    title = "Deskripsi",
                    icon = Icons.Outlined.EditNote,
                    subtitle = "Opsional, tetapi sangat membantu petugas.",
                ) {
                    AppTextField(
                        value = draft.description,
                        onValueChange = { if (it.length <= DESCRIPTION_LIMIT) onDescriptionChange(it) },
                        label = "Keterangan tambahan",
                        placeholder = draft.category?.photoTip
                            ?: "Contoh: lubang selebar 50 cm di lajur kiri, dekat halte.",
                        helperText = "${draft.description.length}/$DESCRIPTION_LIMIT karakter",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
            }

            SubmitBar(draft = draft, onContinue = onContinue)
        }
    }
}

/** Keeps the reason a disabled button is disabled next to the button itself. */
@Composable
private fun SubmitBar(draft: ReportDraftState, onContinue: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.raisedElevation,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPadding, vertical = Spacing.md),
        ) {
            val blocker = draft.blockingHint
            if (blocker != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.iconSm),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = blocker,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PrimaryButton(
                text = "Lanjutkan",
                onClick = onContinue,
                enabled = draft.canSubmit,
            )
        }
    }
}

@Composable
private fun PhotoSection(
    photoPath: String?,
    category: DamageCategory?,
    onTakePhoto: () -> Unit,
) {
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = "Foto kerusakan",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(Spacing.md))
        SecondaryButton(
            text = "Ambil Ulang Foto",
            onClick = onTakePhoto,
            icon = Icons.Outlined.AddAPhoto,
        )
        return
    }

    // Empty slot doubles as guidance: it shows what a good photo looks like.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onTakePhoto),
    ) {
        if (category != null) {
            DamageIllustration(
                category = category,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AddAPhoto,
                contentDescription = null,
                tint = if (category != null) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(Dimens.iconXl),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Ketuk untuk mengambil foto",
                style = MaterialTheme.typography.labelLarge,
                color = if (category != null) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            if (category != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = category.photoTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.xl),
                )
            }
        }
    }
}

/**
 * A grid of illustrated cards rather than a dropdown: the picture is what makes
 * the difference between "Retak Jalan" and "Aspal Rusak" obvious.
 */
@Composable
private fun CategoryPicker(selected: DamageCategory?, onSelect: (DamageCategory) -> Unit) {
    val rows = DamageCategory.entries.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { category ->
                    CategoryTile(
                        category = category,
                        selected = selected == category,
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps the last odd tile at half width instead of stretching it.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: DamageCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
    ) {
        DamageIllustration(
            category = category,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LocationSection(
    draft: ReportDraftState,
    isMapsConfigured: Boolean,
    permissionAsked: Boolean,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onPickOnMap: () -> Unit,
) {
    val location = draft.location

    if (location != null) {
        ReportLocationMap(
            latitude = location.latitude,
            longitude = location.longitude,
            isMapsConfigured = isMapsConfigured,
            modifier = Modifier.fillMaxWidth().height(150.dp),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = if (location.isManual) "Lokasi dipilih manual" else "Lokasi terdeteksi",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (draft.isResolvingStreet) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = draft.streetName?.takeIf { it.isNotBlank() }
                    ?: "Tersimpan berdasarkan koordinat GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "${location.latitude.asCoordinate()}, ${location.longitude.asCoordinate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        SecondaryButton(
            text = "Ubah Lokasi",
            onClick = onPickOnMap,
            icon = Icons.Outlined.EditLocationAlt,
        )
        return
    }

    when (val status = draft.locationStatus) {
        is LocationUiStatus.Failed -> LocationFailureBlock(
            failure = status.failure,
            permissionAsked = permissionAsked,
            onRetry = onRetry,
            onRequestPermission = onRequestPermission,
            onOpenAppSettings = onOpenAppSettings,
            onOpenLocationSettings = onOpenLocationSettings,
            onPickOnMap = onPickOnMap,
        )

        LocationUiStatus.Loading -> LocationProgress("Mendeteksi lokasi Anda...")
        LocationUiStatus.Idle, LocationUiStatus.Ready -> LocationProgress("Menyiapkan lokasi...")
    }
}

@Composable
private fun LocationProgress(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(Dimens.iconMd), strokeWidth = 2.dp)
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocationFailureBlock(
    failure: LocationFailure,
    permissionAsked: Boolean,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onPickOnMap: () -> Unit,
) {
    val message = when (failure) {
        LocationFailure.PermissionDenied ->
            "Izin lokasi belum diberikan. JalanKita memerlukannya untuk menandai titik kerusakan."
        LocationFailure.LocationDisabled ->
            "Layanan lokasi perangkat sedang nonaktif."
        LocationFailure.Timeout ->
            "Lokasi tidak dapat diperoleh. Sinyal GPS mungkin lemah."
        LocationFailure.Unavailable ->
            "Lokasi tidak dapat diperoleh."
    }

    AppCard(contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.iconMd),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(Spacing.md))

    when (failure) {
        LocationFailure.PermissionDenied -> PrimaryButton(
            text = if (permissionAsked) "Buka Pengaturan" else "Izinkan Lokasi",
            onClick = if (permissionAsked) onOpenAppSettings else onRequestPermission,
        )

        LocationFailure.LocationDisabled -> PrimaryButton(
            text = "Aktifkan Lokasi",
            onClick = onOpenLocationSettings,
        )

        else -> PrimaryButton(
            text = "Coba Lagi",
            onClick = onRetry,
            icon = Icons.Outlined.Refresh,
        )
    }

    Spacer(Modifier.height(Spacing.sm))
    SecondaryButton(
        text = "Pilih Lokasi di Peta",
        onClick = onPickOnMap,
        icon = Icons.Outlined.EditLocationAlt,
    )
}
