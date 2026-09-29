package org.umn.jalankita.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import org.umn.jalankita.model.Report

/**
 * Google Maps rendering of the Home preview. Markers come from the user's own
 * reports — nothing here is hardcoded.
 */
@Composable
fun ReportsMapPreviewContent(
    reports: List<Report>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = reports.firstOrNull()?.latLng() ?: DEFAULT_MAP_CENTER
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            focus,
            if (reports.isEmpty()) CITY_ZOOM else STREET_ZOOM - 2f,
        )
    }

    LaunchedEffect(focus) {
        cameraPositionState.move(CameraUpdateFactory.newLatLng(focus))
    }

    Box(modifier = modifier.clip(MaterialTheme.shapes.medium)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = previewUiSettings,
            properties = MapProperties(),
            onMapClick = { onClick() },
        ) {
            reports.forEach { report ->
                Marker(
                    state = rememberUpdatedMarkerState(position = report.latLng()),
                    title = report.category.label,
                    snippet = report.reportStatus.label,
                    icon = report.reportStatus.markerDescriptor(),
                    onClick = {
                        onClick()
                        true
                    },
                )
            }
        }
    }
}
