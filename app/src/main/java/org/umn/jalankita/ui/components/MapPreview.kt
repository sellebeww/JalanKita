package org.umn.jalankita.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MapsHomeWork
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import org.umn.jalankita.model.Report
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing

/**
 * Shown in place of a map when no Google Maps API key is configured, so the
 * screen still reads correctly instead of rendering an empty grey tile.
 *
 * The wording stays honest about the map being unavailable without turning into
 * a build instruction aimed at the person holding the phone.
 */
@Composable
fun MapUnavailablePlaceholder(
    modifier: Modifier = Modifier,
    message: String = "Peta belum tersedia pada versi ini. Lokasi tetap tercatat " +
        "lewat koordinat GPS.",
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        ) {
            Icon(
                imageVector = Icons.Outlined.MapsHomeWork,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconXl),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Map preview used on Home. Rendering is delegated to the Google Maps
 * implementation when a key is present; otherwise the placeholder keeps the
 * layout honest.
 */
@Composable
fun ReportsMapPreview(
    reports: List<Report>,
    isMapsConfigured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isMapsConfigured) {
        MapUnavailablePlaceholder(modifier = modifier, onClick = onClick)
    } else {
        ReportsMapPreviewContent(reports = reports, onClick = onClick, modifier = modifier)
    }
}
