package org.umn.jalankita.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest

/**
 * Manual location picker: a fixed pin over a pannable map. The point under the
 * pin is always the camera target, so there is nothing to drag or mis-tap.
 */
@Composable
fun LocationPickerMapContent(
    latitude: Double,
    longitude: Double,
    onPositionChange: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), STREET_ZOOM)
    }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.position.target }.collectLatest { target ->
            onPositionChange(target.latitude, target.longitude)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(),
            uiSettings = MapUiSettings(
                mapToolbarEnabled = false,
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false,
            ),
            onMapClick = { clicked ->
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(clicked, cameraPositionState.position.zoom)
            },
        )
        // Centre pin sits above the map and marks the camera target.
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Titik lokasi terpilih",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 30.dp).size(44.dp),
        )
    }
}
