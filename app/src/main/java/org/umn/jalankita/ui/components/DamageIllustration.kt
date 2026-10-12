package org.umn.jalankita.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.umn.jalankita.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import org.umn.jalankita.model.DamageCategory
import kotlin.math.cos
import kotlin.math.sin

/**
 * A drawn example of each kind of road damage.
 *
 * These are illustrations, not photographs: they show a resident what "jalan
 * berlubang" or "genangan" means before they have taken a single photo, and they
 * give every report a recognisable image even when no photo is attached.
 *
 * They are built like a photograph of a road rather than like an icon —
 * perspective, aggregate texture, polished wheel tracks, worn markings, one
 * light source from the upper left and a cast shadow for anything with depth.
 * All of that is shared, so the damage itself is the only thing that changes
 * between categories.
 *
 * Everything is drawn from a deterministic hash rather than `Random`, so a tile
 * looks identical on every recomposition, and the amount of texture scales with
 * the size — a 64dp thumbnail would only turn to mud under full detail.
 */
@Composable
fun DamageIllustration(
    category: DamageCategory,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    if (category == DamageCategory.POTHOLE) {
        Image(
            painter = painterResource(R.drawable.jalan_berlubang),
            contentDescription = "Ilustrasi kategori jalan berlubang",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
        return
    }
    Box(modifier = modifier.clip(shape)) {
        Canvas(Modifier.fillMaxSize()) {
            drawAsphalt()
            when (category) {
                DamageCategory.POTHOLE -> drawPothole()
                DamageCategory.CRACK -> drawCracks()
                DamageCategory.DAMAGED_ASPHALT -> drawStrippedAsphalt()
                DamageCategory.SUBSIDENCE -> drawSubsidence()
                DamageCategory.DAMAGED_SHOULDER -> drawBrokenShoulder()
                DamageCategory.FLOODING -> drawFlooding()
                DamageCategory.OTHER -> drawTrafficCone()
            }
            drawVignette()
        }
    }
}

// --- Palette -----------------------------------------------------------------

private val AsphaltFar = Color(0xFF79828F)
private val AsphaltMid = Color(0xFF59616E)
private val AsphaltNear = Color(0xFF3B424C)
private val AggregateLight = Color(0xFFB9BFC7)
private val AggregateDark = Color(0xFF2A2F37)
private val BaseCourse = Color(0xFF8A7F6B)
private val BaseCourseDark = Color(0xFF6B6153)
private val Soil = Color(0xFF6E4E2E)
private val SoilLight = Color(0xFF8E6A42)
private val Marking = Color(0xFFEDEFF1)
private val Shadow = Color(0xFF0B0E13)
private val WaterSurface = Color(0xFF6E8FA6)
private val WaterDeep = Color(0xFF243A4C)
private val ConeOrange = Color(0xFFE2622A)

/** Less texture on small tiles, where full detail reads as noise. */
private val DrawScope.detail: Float
    get() = (size.minDimension / 300f).coerceIn(0.35f, 1f)

// --- The shared stage --------------------------------------------------------

private fun DrawScope.drawAsphalt() {
    val w = size.width
    val h = size.height

    // Distance haze: the surface further away catches more sky light.
    drawRect(
        brush = Brush.verticalGradient(
            0f to AsphaltFar,
            0.45f to AsphaltMid,
            1f to AsphaltNear,
        ),
    )

    // Wheel tracks: the two strips every vehicle polishes darker and smoother.
    listOf(0.24f, 0.76f).forEach { centre ->
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Shadow.copy(alpha = 0.16f),
                1f to Color.Transparent,
                startX = w * (centre - 0.17f),
                endX = w * (centre + 0.17f),
            ),
            topLeft = Offset(w * (centre - 0.17f), 0f),
            size = Size(w * 0.34f, h),
        )
    }

    drawAggregate()
    drawLaneMarkings()
}

/**
 * Exposed stone in the asphalt. Three passes at different tones and sizes, with
 * the grain growing towards the bottom of the frame because that surface is
 * nearer the camera.
 */
private fun DrawScope.drawAggregate(clip: Path? = null, extraDensity: Float = 1f) {
    val w = size.width
    val h = size.height
    val count = (170 * detail * extraDensity).toInt()

    val paint: DrawScope.() -> Unit = {
        repeat(count) { index ->
            val x = w * hash(index * 3)
            val y = h * hash(index * 3 + 1)
            val depth = 0.45f + (y / h) * 0.55f
            val tone = hash(index * 3 + 2)
            val radius = size.minDimension * 0.004f * depth * (0.6f + tone * 1.8f)

            val color = when {
                tone > 0.82f -> AggregateLight.copy(alpha = 0.55f)
                tone > 0.55f -> AggregateLight.copy(alpha = 0.22f)
                else -> AggregateDark.copy(alpha = 0.35f)
            }
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            // A stone catching light also throws a speck of shade beside it.
            if (tone > 0.82f) {
                drawCircle(
                    color = AggregateDark.copy(alpha = 0.3f),
                    radius = radius * 0.8f,
                    center = Offset(x + radius * 0.9f, y + radius * 0.9f),
                )
            }
        }
    }

    if (clip != null) clipPath(clip) { paint() } else paint()
}

/**
 * Centre line in perspective: dashes are shorter and narrower towards the top,
 * and worn thin where the wheels cross them.
 *
 * [dipCentre]/[dipDepth] bend the line through a depression.
 */
private fun DrawScope.drawLaneMarkings(dipCentre: Float? = null, dipDepth: Float = 0f) {
    val w = size.width
    val h = size.height
    val baseline = h * 0.52f

    listOf(0.04f, 0.30f, 0.56f, 0.82f).forEachIndexed { index, fraction ->
        val perspective = 0.7f + fraction * 0.6f
        val dashWidth = w * 0.10f * perspective
        val dashHeight = h * 0.028f * perspective
        val x = w * fraction
        val centre = x + dashWidth / 2f

        val offsetY = dipCentre?.let { c ->
            val distance = ((centre - c) / (w * 0.34f)).coerceIn(-1f, 1f)
            dipDepth * (1f - distance * distance)
        } ?: 0f

        // Shadow under the paint, then the paint, worn unevenly.
        drawRoundRect(
            color = Shadow.copy(alpha = 0.22f),
            topLeft = Offset(x, baseline - dashHeight / 2f + offsetY + dashHeight * 0.35f),
            size = Size(dashWidth, dashHeight),
            cornerRadius = CornerRadius(dashHeight / 2f),
        )
        drawRoundRect(
            color = Marking.copy(alpha = 0.6f + hash(index + 40) * 0.32f),
            topLeft = Offset(x, baseline - dashHeight / 2f + offsetY),
            size = Size(dashWidth, dashHeight),
            cornerRadius = CornerRadius(dashHeight / 2f),
        )
    }
}

/** Corner falloff — what makes the frame read as a photograph rather than a fill. */
private fun DrawScope.drawVignette() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Shadow.copy(alpha = 0.34f)),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.maxDimension * 0.75f,
        ),
    )
}

// --- The damage --------------------------------------------------------------

private fun DrawScope.drawPothole() {
    val centre = Offset(size.width * 0.5f, size.height * 0.62f)
    val radius = size.minDimension * 0.26f

    castShadow(centre.copy(x = centre.x + radius * 0.16f, y = centre.y + radius * 0.3f), radius * 1.25f)

    // Crumbling rim: the asphalt has broken back to the pale base course.
    val rim = blob(centre, radius * 1.22f, seed = 3, points = 15)
    drawPath(rim, BaseCourse.copy(alpha = 0.92f))
    drawAggregate(clip = rim, extraDensity = 2.6f)

    // Lit edge on the upper-left, shaded on the lower-right — one light source.
    drawPath(rim, Marking.copy(alpha = 0.18f), style = Stroke(width = size.minDimension * 0.012f))

    // The hole itself: darkest where the near wall undercuts, opening up further in.
    val hole = blob(centre.copy(y = centre.y + radius * 0.06f), radius, seed = 11, points = 13)
    drawPath(
        path = hole,
        brush = Brush.verticalGradient(
            0f to Shadow,
            0.55f to Color(0xFF15191F),
            1f to Color(0xFF2B3039),
            startY = centre.y - radius,
            endY = centre.y + radius,
        ),
    )
    // Wall thickness: a thin bright line where the surface layer is cut through.
    drawPath(
        path = hole,
        color = BaseCourseDark.copy(alpha = 0.75f),
        style = Stroke(width = size.minDimension * 0.016f),
    )

    debris(centre, radius, seed = 21, count = (9 * detail).toInt().coerceAtLeast(3))
}

private fun DrawScope.drawCracks() {
    val w = size.width
    val h = size.height
    val main = size.minDimension * 0.020f
    val hair = size.minDimension * 0.010f

    val spine = listOf(
        Offset(w * 0.08f, h * 0.26f),
        Offset(w * 0.28f, h * 0.42f),
        Offset(w * 0.22f, h * 0.58f),
        Offset(w * 0.46f, h * 0.70f),
        Offset(w * 0.64f, h * 0.60f),
        Offset(w * 0.90f, h * 0.80f),
    )
    val branches = listOf(
        listOf(Offset(w * 0.28f, h * 0.42f), Offset(w * 0.44f, h * 0.32f), Offset(w * 0.60f, h * 0.36f)),
        listOf(Offset(w * 0.22f, h * 0.58f), Offset(w * 0.12f, h * 0.76f)),
        listOf(Offset(w * 0.46f, h * 0.70f), Offset(w * 0.52f, h * 0.88f)),
        listOf(Offset(w * 0.64f, h * 0.60f), Offset(w * 0.78f, h * 0.46f)),
        listOf(Offset(w * 0.60f, h * 0.36f), Offset(w * 0.72f, h * 0.24f)),
    )

    // A crack is a gap: a lit lip on one side, darkness in the middle.
    (listOf(spine) + branches).forEachIndexed { index, points ->
        val width = if (index == 0) main else hair
        drawPolyline(points, Marking.copy(alpha = 0.16f), width, offset = Offset(-width, -width))
        drawPolyline(points, Shadow.copy(alpha = 0.92f), width)
    }

    // Spalling: the surface crumbles along the widest part of the crack.
    repeat((14 * detail).toInt().coerceAtLeast(4)) { index ->
        val t = hash(index + 71)
        val anchor = spine[(t * (spine.size - 1)).toInt()]
        drawCircle(
            color = BaseCourse.copy(alpha = 0.45f),
            radius = size.minDimension * (0.006f + hash(index + 5) * 0.012f),
            center = Offset(
                anchor.x + (hash(index + 11) - 0.5f) * w * 0.08f,
                anchor.y + (hash(index + 17) - 0.5f) * h * 0.06f,
            ),
        )
    }
}

private fun DrawScope.drawStrippedAsphalt() {
    val w = size.width
    val h = size.height

    val patch = Path().apply {
        moveTo(w * 0.10f, h * 0.46f)
        lineTo(w * 0.26f, h * 0.36f)
        lineTo(w * 0.40f, h * 0.42f)
        lineTo(w * 0.56f, h * 0.33f)
        lineTo(w * 0.74f, h * 0.39f)
        lineTo(w * 0.92f, h * 0.50f)
        lineTo(w * 0.84f, h * 0.72f)
        lineTo(w * 0.62f, h * 0.86f)
        lineTo(w * 0.38f, h * 0.80f)
        lineTo(w * 0.18f, h * 0.68f)
        close()
    }

    // Exposed base course, lighter and warmer than the bitumen above it.
    drawPath(
        path = patch,
        brush = Brush.verticalGradient(
            0f to BaseCourseDark,
            1f to BaseCourse,
            startY = h * 0.33f,
            endY = h * 0.86f,
        ),
    )
    // The thickness of the layer that has peeled away.
    drawPath(patch, Shadow.copy(alpha = 0.55f), style = Stroke(width = size.minDimension * 0.022f))
    drawPath(patch, Marking.copy(alpha = 0.10f), style = Stroke(width = size.minDimension * 0.008f))

    drawAggregate(clip = patch, extraDensity = 3.4f)
    debris(Offset(w * 0.5f, h * 0.6f), size.minDimension * 0.42f, seed = 33, count = (11 * detail).toInt().coerceAtLeast(4))
}

private fun DrawScope.drawSubsidence() {
    val w = size.width
    val h = size.height
    val centreX = w * 0.5f
    val depth = h * 0.15f

    // The dip reads as shading, not outline: dark where it sinks, a lit lip near.
    drawOval(
        brush = Brush.verticalGradient(
            0f to Shadow.copy(alpha = 0.62f),
            0.7f to Shadow.copy(alpha = 0.22f),
            1f to Color.Transparent,
        ),
        topLeft = Offset(w * 0.08f, h * 0.36f),
        size = Size(w * 0.84f, h * 0.48f),
    )
    drawOval(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Marking.copy(alpha = 0.10f),
        ),
        topLeft = Offset(w * 0.12f, h * 0.60f),
        size = Size(w * 0.76f, h * 0.30f),
    )

    drawLaneMarkings(dipCentre = centreX, dipDepth = depth)

    // Contour cracks that open along the rim of a settlement.
    listOf(0.36f to 0.5f, 0.84f to 0.34f).forEach { (baseline, alpha) ->
        val contour = Path().apply {
            moveTo(w * 0.08f, h * baseline)
            cubicTo(
                w * 0.32f, h * baseline + depth * 1.15f,
                w * 0.68f, h * baseline + depth * 1.15f,
                w * 0.92f, h * baseline,
            )
        }
        drawPath(contour, Shadow.copy(alpha = alpha), style = Stroke(width = size.minDimension * 0.016f))
    }
}

private fun DrawScope.drawBrokenShoulder() {
    val w = size.width
    val h = size.height

    // Painted edge line, about to fall into the void with the rest of the edge.
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.70f)
            lineTo(w * 0.16f, h * 0.64f)
            lineTo(w * 0.44f, h * 0.62f)
            lineTo(w, h * 0.68f)
            lineTo(w, h * 0.72f)
            lineTo(w * 0.44f, h * 0.66f)
            lineTo(w * 0.16f, h * 0.68f)
            lineTo(0f, h * 0.74f)
            close()
        },
        color = Marking.copy(alpha = 0.55f),
    )

    val broken = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.80f)
        lineTo(w * 0.15f, h * 0.73f)
        lineTo(w * 0.29f, h * 0.85f)
        lineTo(w * 0.45f, h * 0.71f)
        lineTo(w * 0.61f, h * 0.83f)
        lineTo(w * 0.79f, h * 0.69f)
        lineTo(w, h * 0.78f)
        lineTo(w, h)
        close()
    }

    drawPath(
        path = broken,
        brush = Brush.verticalGradient(0f to Soil, 1f to SoilLight, startY = h * 0.69f, endY = h),
    )
    // The overhanging asphalt lip throws a hard shadow onto the soil below it.
    drawPath(broken, Shadow.copy(alpha = 0.75f), style = Stroke(width = size.minDimension * 0.026f))

    clipPath(broken) {
        repeat((26 * detail).toInt().coerceAtLeast(8)) { index ->
            drawCircle(
                color = if (index % 3 == 0) Shadow.copy(alpha = 0.4f) else SoilLight.copy(alpha = 0.7f),
                radius = size.minDimension * (0.006f + hash(index + 3) * 0.016f),
                center = Offset(w * hash(index), h * (0.70f + hash(index + 23) * 0.30f)),
            )
        }
    }
}

private fun DrawScope.drawFlooding() {
    val w = size.width
    val h = size.height
    val topLeft = Offset(w * 0.08f, h * 0.40f)
    val area = Size(w * 0.84f, h * 0.48f)

    // The hazard the water is hiding.
    drawPath(blob(Offset(w * 0.44f, h * 0.66f), size.minDimension * 0.19f, seed = 9), Shadow.copy(alpha = 0.9f))

    // Water: reflected sky at the far edge, dark and deep at the near one.
    drawOval(
        brush = Brush.verticalGradient(
            0f to WaterSurface.copy(alpha = 0.92f),
            0.45f to Color(0xFF3E5D75).copy(alpha = 0.92f),
            1f to WaterDeep.copy(alpha = 0.95f),
            startY = topLeft.y,
            endY = topLeft.y + area.height,
        ),
        topLeft = topLeft,
        size = area,
    )
    // Wet rim where the water meets dry asphalt.
    drawOval(
        color = Shadow.copy(alpha = 0.35f),
        topLeft = topLeft,
        size = area,
        style = Stroke(width = size.minDimension * 0.02f),
    )

    // Reflections: long, thin, and horizontal — that is what sells standing water.
    listOf(
        Triple(0.20f, 0.50f, 0.30f),
        Triple(0.54f, 0.46f, 0.18f),
        Triple(0.34f, 0.72f, 0.24f),
        Triple(0.62f, 0.78f, 0.14f),
    ).forEachIndexed { index, (x, y, width) ->
        drawOval(
            color = Marking.copy(alpha = 0.38f - index * 0.06f),
            topLeft = Offset(w * x, h * y),
            size = Size(w * width, h * 0.022f),
        )
    }

    // Ripple rings, concentric and flattened by the viewing angle.
    repeat(3) { ring ->
        val inset = ring * size.minDimension * 0.05f
        drawOval(
            color = Marking.copy(alpha = 0.12f),
            topLeft = Offset(topLeft.x + inset, topLeft.y + inset * 0.6f),
            size = Size(area.width - inset * 2, area.height - inset * 1.2f),
            style = Stroke(width = size.minDimension * 0.006f),
        )
    }
}

/** A cone reads as "hazard on the road" far faster than an abstract symbol. */
private fun DrawScope.drawTrafficCone() {
    val w = size.width
    val h = size.height
    val baseY = h * 0.78f
    val apexY = h * 0.30f
    val halfBase = w * 0.16f
    val centreX = w * 0.5f

    castShadow(Offset(centreX + halfBase * 0.5f, baseY + h * 0.02f), halfBase * 1.5f)

    val body = Path().apply {
        moveTo(centreX, apexY)
        lineTo(centreX + halfBase, baseY)
        lineTo(centreX - halfBase, baseY)
        close()
    }
    drawPath(
        path = body,
        brush = Brush.horizontalGradient(
            0f to ConeOrange,
            0.35f to Color(0xFFF07C42),
            1f to Color(0xFF9E3D17),
            startX = centreX - halfBase,
            endX = centreX + halfBase,
        ),
    )

    // Retro-reflective bands follow the taper, so they narrow towards the apex.
    clipPath(body) {
        listOf(0.42f to 0.10f, 0.62f to 0.09f).forEach { (top, height) ->
            drawRect(
                color = Marking.copy(alpha = 0.88f),
                topLeft = Offset(centreX - halfBase, h * top),
                size = Size(halfBase * 2, h * height),
            )
        }
    }

    // Base flange.
    drawRoundRect(
        color = Color(0xFFB4491C),
        topLeft = Offset(centreX - halfBase * 1.35f, baseY - h * 0.03f),
        size = Size(halfBase * 2.7f, h * 0.055f),
        cornerRadius = CornerRadius(h * 0.02f),
    )
}

// --- Drawing helpers ---------------------------------------------------------

/** Layered translucent ellipses — a soft shadow without a blur filter. */
private fun DrawScope.castShadow(centre: Offset, radius: Float) {
    repeat(4) { layer ->
        val scale = 1f - layer * 0.16f
        drawOval(
            color = Shadow.copy(alpha = 0.10f + layer * 0.05f),
            topLeft = Offset(centre.x - radius * scale, centre.y - radius * scale * 0.42f),
            size = Size(radius * 2 * scale, radius * 0.84f * scale),
        )
    }
}

/** Small stones thrown clear of the damage, each with its own speck of shade. */
private fun DrawScope.debris(centre: Offset, radius: Float, seed: Int, count: Int) {
    repeat(count) { index ->
        val angle = hash(seed + index) * 2f * Math.PI.toFloat()
        val distance = radius * (1.15f + hash(seed + index + 50) * 0.7f)
        val position = Offset(centre.x + cos(angle) * distance, centre.y + sin(angle) * distance * 0.55f)
        val r = radius * (0.05f + hash(seed + index + 90) * 0.07f)
        drawCircle(Shadow.copy(alpha = 0.4f), r, position.copy(x = position.x + r * 0.5f, y = position.y + r * 0.5f))
        drawCircle(BaseCourse.copy(alpha = 0.9f), r, position)
        drawCircle(AggregateLight.copy(alpha = 0.35f), r * 0.45f, position.copy(x = position.x - r * 0.3f, y = position.y - r * 0.3f))
    }
}

/**
 * An irregular closed shape around [centre]. Damage is never round, and a circle
 * immediately reads as a graphic rather than as a hole in a road.
 */
private fun blob(centre: Offset, radius: Float, seed: Int, points: Int = 11): Path {
    val path = Path()
    repeat(points) { index ->
        val angle = (index.toFloat() / points) * (2f * Math.PI).toFloat()
        val wobble = 0.74f + hash(seed * 31 + index) * 0.48f
        val x = centre.x + cos(angle) * radius * wobble
        val y = centre.y + sin(angle) * radius * wobble * 0.70f
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawPolyline(
    points: List<Offset>,
    color: Color,
    width: Float,
    offset: Offset = Offset.Zero,
) {
    val path = Path().apply {
        moveTo(points.first().x + offset.x, points.first().y + offset.y)
        points.drop(1).forEach { lineTo(it.x + offset.x, it.y + offset.y) }
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
}

/** Deterministic 0..1 noise — stable across recompositions, unlike Random. */
private fun hash(value: Int): Float {
    var x = value * 374761393 + 668265263
    x = (x xor (x shr 13)) * 1274126177
    return ((x xor (x shr 16)) and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat()
}
