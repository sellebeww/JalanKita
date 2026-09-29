package org.umn.jalankita.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Google Maps implementation of [ReportLocationMap]. */
@Composable
fun ReportLocationMapContent(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val position = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, STREET_ZOOM)
    }

    LaunchedEffect(latitude, longitude) {
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(position, STREET_ZOOM))
    }

    Box(modifier = modifier.clip(MaterialTheme.shapes.medium)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = previewUiSettings,
            properties = MapProperties(),
        ) {
            Marker(state = rememberUpdatedMarkerState(position = position))
        }
    }
}
