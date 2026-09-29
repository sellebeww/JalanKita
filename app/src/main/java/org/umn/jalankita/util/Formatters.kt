package org.umn.jalankita.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private val indonesian = Locale.forLanguageTag("id-ID")

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", indonesian).withZone(ZoneId.systemDefault())

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", indonesian).withZone(ZoneId.systemDefault())

fun Long.asReportDate(): String = dateFormatter.format(Instant.ofEpochMilli(this))

fun Long.asReportDateTime(): String = dateTimeFormatter.format(Instant.ofEpochMilli(this))

/**
 * "2 jam lalu" for anything recent, an absolute date once that stops being
 * useful — a list of reports is easier to scan by recency than by date.
 */
fun Long.asRelativeTime(now: Long = System.currentTimeMillis()): String {
    val elapsed = now - this
    if (elapsed < 0) return asReportDate()

    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)

    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit lalu"
        hours < 24 -> "$hours jam lalu"
        days < 7 -> "$days hari lalu"
        else -> asReportDate()
    }
}

/** Six decimals is roughly 0.1 m — plenty for a pothole. */
fun Double.asCoordinate(): String = String.format(Locale.US, "%.6f", this)

fun formatCoordinates(latitude: Double, longitude: Double): String =
    "${latitude.asCoordinate()}, ${longitude.asCoordinate()}"
