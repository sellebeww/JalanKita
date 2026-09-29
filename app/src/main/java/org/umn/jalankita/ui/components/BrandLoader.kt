package org.umn.jalankita.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.theme.BrandGradientStart

/** The shared JalanKita mark surrounded by an indeterminate progress arc. */
@Composable
fun RoadLoader(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    onBrandSurface: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "roadLoader")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
        label = "roadSweep",
    )

    val ringColor = if (onBrandSurface) Color.White else MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val ringStroke = w * 0.045f

            // Orbiting arc: progress without pretending to know a percentage.
            drawArc(
                color = ringColor.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(ringStroke / 2, ringStroke / 2),
                size = Size(w - ringStroke, h - ringStroke),
                style = Stroke(width = ringStroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = sweep,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(ringStroke / 2, ringStroke / 2),
                size = Size(w - ringStroke, h - ringStroke),
                style = Stroke(width = ringStroke, cap = StrokeCap.Round),
            )
        }
        BrandLogo(
            size = size * 0.66f,
            containerColor = if (onBrandSurface) Color.White else null,
            markColor = if (onBrandSurface) BrandGradientStart else Color.White,
        )
    }
}

/** Three dots that fill in turn — pairs with [RoadLoader] under a message. */
@Composable
fun LoadingDots(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "loadingDots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing)),
        label = "dotsPhase",
    )

    Canvas(modifier = modifier.size(width = 44.dp, height = 8.dp)) {
        val radius = size.height / 2f
        repeat(3) { index ->
            val distance = ((phase - index + 3f) % 3f)
            val alpha = if (distance < 1f) 1f - distance * 0.7f else 0.3f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(radius + index * (size.width - radius * 2) / 2f, radius),
            )
        }
    }
}
