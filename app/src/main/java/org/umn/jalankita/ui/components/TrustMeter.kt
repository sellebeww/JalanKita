package org.umn.jalankita.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.umn.jalankita.model.ReporterLevel
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.ui.theme.StatusRed

/**
 * A reporter's trust score as a gauge.
 *
 * A number on its own ("72") says nothing; the arc gives it a scale, and the
 * colour says whether that is good. Deliberately a three-quarter arc rather than
 * a full ring, so it reads as a measurement and not as a progress spinner.
 */
@Composable
fun TrustMeter(
    score: Int,
    level: ReporterLevel,
    modifier: Modifier = Modifier,
    size: Dp = 136.dp,
    enabled: Boolean = true,
) {
    val sweep by animateFloatAsState(
        targetValue = if (enabled) score.coerceIn(0, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "trustSweep",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val scoreColor = when {
        !enabled -> MaterialTheme.colorScheme.outline
        score >= 80 -> StatusGreen
        score >= 55 -> StatusOrange
        else -> StatusRed
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.10f
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val startAngle = 135f
            val fullSweep = 270f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(scoreColor.copy(alpha = 0.6f), scoreColor)),
                    startAngle = startAngle,
                    sweepAngle = fullSweep * sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (enabled) "$score" else "—",
                style = MaterialTheme.typography.displaySmall,
                color = scoreColor,
            )
            Text(
                text = if (enabled) "dari 100" else "belum dinilai",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tone for the badge next to a reporter's name. */
fun ReporterLevel.badgeTone(): BadgeTone = when (this) {
    ReporterLevel.NEW -> BadgeTone.Neutral
    ReporterLevel.ACTIVE -> BadgeTone.Orange
    ReporterLevel.TRUSTED -> BadgeTone.Blue
    ReporterLevel.VERIFIED -> BadgeTone.Green
}
