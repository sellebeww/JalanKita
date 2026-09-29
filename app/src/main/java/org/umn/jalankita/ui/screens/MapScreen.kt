package org.umn.jalankita.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch
import org.umn.jalankita.data.location.LocationProvider
import org.umn.jalankita.data.location.NearbyState
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.CITY_ZOOM
import org.umn.jalankita.ui.components.DEFAULT_MAP_CENTER
import org.umn.jalankita.ui.components.EmptyState
import org.umn.jalankita.ui.components.MapUnavailablePlaceholder
import org.umn.jalankita.ui.components.ReportCard
import org.umn.jalankita.ui.components.ReportImage
import org.umn.jalankita.ui.components.ReportStatusBadge
import org.umn.jalankita.ui.components.STREET_ZOOM
import org.umn.jalankita.ui.components.SyncStatusBadge
import org.umn.jalankita.ui.components.TertiaryButton
import org.umn.jalankita.ui.components.accentColor
import org.umn.jalankita.ui.components.latLng
import org.umn.jalankita.ui.components.markerDescriptor
import org.umn.jalankita.ui.components.tone
import org.umn.jalankita.ui.permissions.LOCATION_PERMISSIONS
import org.umn.jalankita.ui.permissions.hasAnyLocationPermission
import org.umn.jalankita.ui.permissions.rememberLocationPermissionLauncher
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.util.asRelativeTime
import org.umn.jalankita.util.formatDistance

@Composable
fun MapScreen(
    reports: List<Report>,
    nearby: NearbyState,
    isMapsConfigured: Boolean,
    locationProvider: LocationProvider,
    onNewReport: () -> Unit,
    onReportClick: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Peta Laporan",
                subtitle = if (reports.isEmpty()) {
                    "Belum ada titik kerusakan"
                } else {
                    "${reports.size} titik kerusakan"
                },
            )
        },
    ) { innerPadding ->
        if (!isMapsConfigured) {
            DamageList(
                reports = reports,
                nearby = nearby,
                onNewReport = onNewReport,
                onReportClick = onReportClick,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            return@Scaffold
        }

        MapContent(
            reports = reports,
            locationProvider = locationProvider,
            onNewReport = onNewReport,
            onReportClick = onReportClick,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

/**
 * What the Peta tab shows when no Maps key is configured.
 *
 * A placeholder alone would strand every community report behind a missing
 * credential, so the same data is offered as a list — ordered by distance when
 * the user's position is known, by recency otherwise.
 */
@Composable
private fun DamageList(
    reports: List<Report>,
    nearby: NearbyState,
    onNewReport: () -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val distances = remember(nearby.nearby) {
        nearby.nearby.associate { it.report.id to it.distanceMeters }
    }
    val ordered = remember(reports, distances) {
        if (distances.isEmpty()) {
            reports
        } else {
            reports.sortedWith(
                compareBy(
                    { distances[it.id] ?: Double.MAX_VALUE },
                    { -it.createdAt },
                ),
            )
        }
    }

    if (reports.isEmpty()) {
        Box(modifier.padding(Dimens.screenPadding)) {
            EmptyState(
                title = "Belum Ada Titik Kerusakan",
                description = "Laporan yang Anda dan warga lain kirim akan muncul di sini.",
                icon = Icons.Outlined.PinDrop,
                actionLabel = "Lapor Kerusakan",
                onAction = onNewReport,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = Spacing.sm,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            MapUnavailablePlaceholder(
                modifier = Modifier.fillMaxWidth().height(Dimens.mapPreview),
            )
        }
        item {
            Text(
                text = if (distances.isEmpty()) {
                    "Daftar kerusakan terbaru dari Anda dan warga sekitar."
                } else {
                    "Diurutkan dari yang paling dekat dengan lokasi Anda."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(ordered, key = { it.id }) { report ->
            Column {
                ReportCard(
                    report = report,
                    onClick = { onReportClick(report.id) },
                    showReporter = true,
                )
                distances[report.id]?.let { meters ->
                    Text(
                        text = "${formatDistance(meters)} dari lokasi Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun MapContent(
    reports: List<Report>,
    locationProvider: LocationProvider,
    onNewReport: () -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedReportId by remember { mutableStateOf<String?>(null) }
    var myLocationEnabled by remember { mutableStateOf(context.hasAnyLocationPermission()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            reports.firstOrNull()?.latLng() ?: DEFAULT_MAP_CENTER,
            if (reports.isEmpty()) CITY_ZOOM else STREET_ZOOM - 2f,
        )
    }

    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        myLocationEnabled = granted
        if (granted) scope.launch { moveToCurrentLocation(locationProvider, cameraPositionState) }
    }

    // Re-centre on the newest report the first time one appears.
    LaunchedEffect(reports.firstOrNull()?.id) {
        reports.firstOrNull()?.let {
            cameraPositionState.move(CameraUpdateFactory.newLatLng(it.latLng()))
        }
    }

    val selected = reports.firstOrNull { it.id == selectedReportId }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
            uiSettings = MapUiSettings(
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
            ),
            onMapClick = { selectedReportId = null },
        ) {
            reports.forEach { report ->
                Marker(
                    state = rememberUpdatedMarkerState(position = report.latLng()),
                    title = report.category.label,
                    snippet = report.reportStatus.label,
                    icon = report.reportStatus.markerDescriptor(),
                    onClick = {
                        selectedReportId = report.id
                        false
                    },
                )
            }
        }

        // Marker colours are meaningless without this.
        MapLegend(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = Dimens.screenPadding, vertical = Spacing.md),
        )

        if (reports.isEmpty()) {
            EmptyMapNotice(
                onNewReport = onNewReport,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Dimens.screenPadding),
            )
        }

        FloatingActionButton(
            onClick = {
                if (myLocationEnabled) {
                    scope.launch { moveToCurrentLocation(locationProvider, cameraPositionState) }
                } else {
                    permissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.screenPadding)
                // Lifts clear of the detail card instead of hiding behind it.
                .padding(bottom = if (selected != null) 150.dp else 0.dp),
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = "Lokasi saya")
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            selected?.let { report ->
                MarkerDetailCard(
                    report = report,
                    onOpen = { onReportClick(report.id) },
                    onDismiss = { selectedReportId = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.screenPadding),
                )
            }
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = MaterialTheme.shapes.small,
        shadowElevation = Dimens.raisedElevation,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot("Menunggu", ReportStatus.PENDING)
            LegendDot("Ditangani", ReportStatus.IN_PROGRESS)
            LegendDot("Selesai", ReportStatus.COMPLETED)
        }
    }
}

@Composable
private fun LegendDot(label: String, status: ReportStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(status.tone().accentColor(), CircleShape),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyMapNotice(onNewReport: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconLg),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Belum ada titik di peta",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Laporan yang Anda kirim akan muncul sebagai penanda di sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TertiaryButton(text = "Lapor", onClick = onNewReport)
        }
    }
}

@Composable
private fun MarkerDetailCard(
    report: Report,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.raisedElevation,
        modifier = modifier,
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                ReportImage(
                    report = report,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(68.dp),
                )
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = report.category.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = report.streetName?.takeIf { it.isNotBlank() }
                            ?: report.createdAt.asRelativeTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Dilaporkan oleh ${report.displayReporter}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        ReportStatusBadge(report.reportStatus, showIcon = false)
                        SyncStatusBadge(report.syncStatus, showIcon = false)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.iconMd),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            TertiaryButton(
                text = "Lihat Detail",
                onClick = onOpen,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

private suspend fun moveToCurrentLocation(
    locationProvider: LocationProvider,
    cameraPositionState: CameraPositionState,
) {
    locationProvider.getCurrentLocation().onSuccess { point ->
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(point.latitude, point.longitude),
                STREET_ZOOM,
            ),
        )
    }
}
