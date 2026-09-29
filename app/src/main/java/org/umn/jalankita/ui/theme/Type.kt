package org.umn.jalankita.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Default = FontFamily.Default

/**
 * Keeps the full line box instead of trimming it.
 *
 * Compose trims the leading above the first line by default, which clipped the
 * top of tall glyphs — "Dampak" rendered with the D's cap cut off wherever a
 * style set a tight lineHeight. Centring the text in an untrimmed box also makes
 * single-line labels sit optically straight next to icons.
 */
private val FullLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun textStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = FullLineHeight,
)

/**
 * The whole Material scale is defined, not just the styles that happened to be
 * used first: anything left out falls back to Material's default weights and
 * breaks the rhythm as soon as a new screen reaches for it.
 *
 * Weight ladder: Bold for display/headline, SemiBold for titles and labels,
 * Normal for body. Letter spacing is tightened on the large sizes only.
 */
val JalanKitaTypography = Typography(
    displayLarge = textStyle(FontWeight.Bold, 44, 52, -0.5),
    displayMedium = textStyle(FontWeight.Bold, 36, 44, -0.4),
    displaySmall = textStyle(FontWeight.Bold, 32, 40, -0.3),
    headlineLarge = textStyle(FontWeight.Bold, 28, 36, -0.2),
    headlineMedium = textStyle(FontWeight.Bold, 25, 32, -0.2),
    headlineSmall = textStyle(FontWeight.Bold, 21, 28),
    titleLarge = textStyle(FontWeight.SemiBold, 19, 26),
    titleMedium = textStyle(FontWeight.SemiBold, 16, 22),
    titleSmall = textStyle(FontWeight.SemiBold, 14, 20),
    bodyLarge = textStyle(FontWeight.Normal, 16, 24),
    bodyMedium = textStyle(FontWeight.Normal, 14, 21),
    bodySmall = textStyle(FontWeight.Normal, 12, 18),
    labelLarge = textStyle(FontWeight.SemiBold, 15, 20, 0.1),
    labelMedium = textStyle(FontWeight.SemiBold, 12, 16, 0.2),
    labelSmall = textStyle(FontWeight.Medium, 11, 15, 0.3),
)
