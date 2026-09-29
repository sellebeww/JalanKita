package org.umn.jalankita.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.components.AuroraBackground
import org.umn.jalankita.ui.components.LoadingDots
import org.umn.jalankita.ui.components.RoadLoader
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing

/**
 * The branded wait.
 *
 * Used for the launch splash and for any wait long enough that a bare spinner
 * would feel like a hang. The copy explains what is being waited on, because
 * "loading" on its own tells the user nothing.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    title: String = "JalanKita",
    message: String = "Menyiapkan laporan Anda...",
    footer: String? = null,
) {
    var entered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = tween(durationMillis = 650),
        label = "loadingScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "loadingAlpha",
    )
    LaunchedEffect(Unit) { entered = true }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AuroraBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.scale(scale).alpha(alpha)) {
                RoadLoader(size = 108.dp, onBrandSurface = true)
            }
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = alpha),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f * alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
            Spacer(Modifier.height(Spacing.xl))
            LoadingDots(color = Color.White.copy(alpha = alpha))
        }

        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f * alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = Dimens.screenPadding, vertical = Spacing.xxxl),
            )
        }
    }
}

/** Kept as its own entry point so the launch wait can differ in copy only. */
@Composable
fun SplashLoading(modifier: Modifier = Modifier) {
    LoadingScreen(
        modifier = modifier.background(Color.Transparent),
        message = "Laporkan kerusakan jalan,\nuntuk jalan yang lebih baik.",
        footer = "Dari warga, untuk jalan yang lebih baik",
    )
}
