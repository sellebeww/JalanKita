package org.umn.jalankita.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Single-pin map used on the report detail screen. Falls back to the placeholder
 * when Google Maps is not configured.
 */
@Composable
fun ReportLocationMap(
    latitude: Double,
    longitude: Double,
    isMapsConfigured: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isMapsConfigured) {
        MapUnavailablePlaceholder(modifier = modifier)
    } else {
        ReportLocationMapContent(latitude = latitude, longitude = longitude, modifier = modifier)
    }
}
