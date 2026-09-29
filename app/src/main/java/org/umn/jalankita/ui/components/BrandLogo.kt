package org.umn.jalankita.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.umn.jalankita.R
import org.umn.jalankita.ui.theme.BrandGradientEnd
import org.umn.jalankita.ui.theme.BrandGradientStart
import org.umn.jalankita.ui.theme.Spacing

/** Shared vector mark: a J-shaped road and a waypoint, identical to the launcher. */
@Composable
fun BrandLogo(
    size: Dp = 88.dp,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    markColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(
                    if (containerColor == null) listOf(BrandGradientStart, BrandGradientEnd)
                    else listOf(containerColor, containerColor),
                ),
                shape = RoundedCornerShape(percent = 26),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            tint = markColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Mark plus wordmark and tagline — the splash and login headers use this. */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    logoSize: Dp = 72.dp,
    tagline: String? = "Laporkan kerusakan jalan, untuk jalan yang lebih baik.",
    onBrandSurface: Boolean = false,
) {
    val titleColor = if (onBrandSurface) Color.White else MaterialTheme.colorScheme.secondary
    val taglineColor = if (onBrandSurface) {
        Color.White.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLogo(
            size = logoSize,
            containerColor = if (onBrandSurface) Color.White else null,
            markColor = if (onBrandSurface) BrandGradientStart else Color.White,
        )
        Text(
            text = "JalanKita",
            style = MaterialTheme.typography.headlineMedium,
            color = titleColor,
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
        )
        if (tagline != null) {
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = taglineColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
