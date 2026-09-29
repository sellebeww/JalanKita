package org.umn.jalankita.util

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Great-circle distance in metres.
 *
 * The haversine form is used rather than the flat-earth approximation because
 * the proximity alerts care about tens of metres, where the error of the flat
 * approximation is no longer negligible near the poles — and because it costs
 * nothing at this scale.
 */
fun distanceMeters(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double,
): Double {
    val dLat = Math.toRadians(toLatitude - fromLatitude)
    val dLon = Math.toRadians(toLongitude - fromLongitude)
    val lat1 = Math.toRadians(fromLatitude)
    val lat2 = Math.toRadians(toLatitude)

    val a = sin(dLat / 2).let { it * it } +
        cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
}

/**
 * "120 m" / "1,4 km" — Indonesian decimal comma, and never more precision than a
 * phone's GPS can justify.
 *
 * Anything under 10 m is reported as "<10 m" rather than rounded to "0 m": a
 * consumer GPS fix is not accurate to the metre, and "0 m" claims a precision
 * the reading does not have while also reading like a bug.
 */
fun formatDistance(meters: Double): String = when {
    meters < 10 -> "<10 m"
    meters < 1_000 -> "${(meters / 10).toInt() * 10} m"
    meters < 10_000 -> String.format(Locale.forLanguageTag("id-ID"), "%.1f km", meters / 1_000)
    else -> "${(meters / 1_000).toInt()} km"
}
