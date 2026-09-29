package org.umn.jalankita.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import org.umn.jalankita.ui.theme.BrandGradientEnd
import org.umn.jalankita.ui.theme.BrandGradientStart
import kotlin.math.cos
import kotlin.math.sin

/**
 * The brand gradient, slowly moving.
 *
 * Three soft light pools drift on long, mutually prime cycles, so the loop never
 * lands in an obviously repeating pattern. It is deliberately low-contrast: this
 * sits behind a form and a loader, and anything busier would fight them.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "auroraDrift",
    )

    Canvas(modifier = if (shape != null) modifier.clip(shape) else modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to BrandGradientStart,
                0.55f to Color(0xFF15398F),
                1f to BrandGradientEnd,
            ),
        )

        val w = size.width
        val h = size.height

        data class Pool(val color: Color, val phase: Float, val speed: Float, val radius: Float)

        listOf(
            Pool(Color(0xFF3B82F6), 0f, 1.0f, 0.75f),
            Pool(Color(0xFF22D3EE), 2.1f, 0.62f, 0.55f),
            Pool(Color(0xFF1E1B4B), 4.2f, 0.81f, 0.85f),
        ).forEach { pool ->
            val angle = drift * pool.speed + pool.phase
            val centre = Offset(
                x = w * (0.5f + 0.34f * cos(angle)),
                y = h * (0.42f + 0.30f * sin(angle * 1.3f)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(pool.color.copy(alpha = 0.34f), Color.Transparent),
                    center = centre,
                    radius = size.minDimension * pool.radius,
                ),
                radius = size.minDimension * pool.radius,
                center = centre,
            )
        }
    }
}
