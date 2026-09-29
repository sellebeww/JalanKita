package org.umn.jalankita.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.umn.jalankita.ui.components.AppTextField
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.LocationPickerMapContent
import org.umn.jalankita.ui.components.MapUnavailablePlaceholder
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.util.asCoordinate

/** Default camera target when nothing better is known: Jakarta. */
private const val FALLBACK_LATITUDE = -6.2088
private const val FALLBACK_LONGITUDE = 106.8456

@Composable
fun LocationPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    isMapsConfigured: Boolean,
    onConfirm: (Double, Double) -> Unit,
    onBack: () -> Unit,
) {
    var latitude by remember { mutableStateOf(initialLatitude ?: FALLBACK_LATITUDE) }
    var longitude by remember { mutableStateOf(initialLongitude ?: FALLBACK_LONGITUDE) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Pilih Lokasi",
                subtitle = "Tempatkan penanda pada titik kerusakan",
                onBack = onBack,
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
            if (isMapsConfigured) {
                LocationPickerMapContent(
                    latitude = latitude,
                    longitude = longitude,
                    onPositionChange = { lat, lng ->
                        latitude = lat
                        longitude = lng
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Column(
                    Modifier.padding(horizontal = Dimens.screenPadding, vertical = Spacing.md),
                ) {
                    Text(
                        text = "Geser peta untuk menempatkan penanda pada titik kerusakan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${latitude.asCoordinate()}, ${longitude.asCoordinate()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                horizontal = Spacing.md,
                                vertical = Spacing.sm,
                            ),
                        )
                    }
                }
            } else {
                // No Maps key: fall back to entering coordinates so the user is not
                // stuck when GPS also fails.
                MapUnavailablePlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.mapDetail)
                        .padding(Dimens.screenPadding),
                )
                Text(
                    text = "Peta belum aktif pada build ini. Masukkan koordinat lokasi " +
                        "kerusakan secara manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.screenPadding),
                )
                Spacer(Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding),
                ) {
                    CoordinateField(
                        label = "Latitude",
                        value = latitude,
                        range = -90.0..90.0,
                        onValueChange = { latitude = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    CoordinateField(
                        label = "Longitude",
                        value = longitude,
                        range = -180.0..180.0,
                        onValueChange = { longitude = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            PrimaryButton(
                text = "Gunakan Lokasi Ini",
                onClick = { onConfirm(latitude, longitude) },
                icon = Icons.Outlined.PinDrop,
                modifier = Modifier.padding(
                    horizontal = Dimens.screenPadding,
                    vertical = Spacing.md,
                ),
            )
        }
    }
}

@Composable
private fun CoordinateField(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value.asCoordinate()) }
    val invalid = text.toDoubleOrNull()?.let { it !in range } ?: true
    AppTextField(
        value = text,
        onValueChange = { input ->
            text = input
            input.toDoubleOrNull()?.takeIf { it in range }?.let(onValueChange)
        },
        label = label,
        errorText = if (invalid) "Di luar rentang ${'$'}{range.start}..${'$'}{range.endInclusive}" else null,
        keyboardType = KeyboardType.Decimal,
        modifier = modifier,
    )
}
