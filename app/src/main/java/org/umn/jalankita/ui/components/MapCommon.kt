package org.umn.jalankita.ui.components

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapUiSettings
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReportStatus

/** Jakarta — only used when there is nothing better to centre on. */
val DEFAULT_MAP_CENTER = LatLng(-6.2088, 106.8456)

const val CITY_ZOOM = 12f
const val STREET_ZOOM = 16f

/** A preview tile is a picture, not a map you can pan. */
val previewUiSettings = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = false,
    scrollGesturesEnabled = false,
    scrollGesturesEnabledDuringRotateOrZoom = false,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = false,
)

fun Report.latLng(): LatLng = LatLng(latitude, longitude)

/** Marker colour carries the same semantics as the status badges. */
fun ReportStatus.markerDescriptor(): BitmapDescriptor = BitmapDescriptorFactory.defaultMarker(
    when (this) {
        ReportStatus.PENDING -> BitmapDescriptorFactory.HUE_ORANGE
        ReportStatus.VERIFIED, ReportStatus.IN_PROGRESS -> BitmapDescriptorFactory.HUE_AZURE
        ReportStatus.COMPLETED -> BitmapDescriptorFactory.HUE_GREEN
        ReportStatus.NEEDS_INFO -> BitmapDescriptorFactory.HUE_RED
    },
)
