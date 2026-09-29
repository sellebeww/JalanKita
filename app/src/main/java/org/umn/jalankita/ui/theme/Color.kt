package org.umn.jalankita.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * JalanKita visual identity: navy + bright blue + white.
 *
 * Every colour the app draws comes from here or from [JalanKitaTheme]'s scheme.
 * Screens never hardcode a hex value — that is what kept the old surfaces from
 * matching each other.
 */

// --- Brand ---
val BrightBlue = Color(0xFF2563EB)
val BrightBlueDark = Color(0xFF1D4ED8)
val BrightBlueLight = Color(0xFF60A5FA)
val NavyBlue = Color(0xFF0F2A54)
val NavyBlueDeep = Color(0xFF0B1A33)
val BlueContainer = Color(0xFFDBEAFE)
val BlueContainerDark = Color(0xFF1E3A8A)

/** Ends of the brand gradient used on the splash, login header and hero cards. */
val BrandGradientStart = Color(0xFF1D4ED8)
val BrandGradientEnd = Color(0xFF0F2A54)

// --- Neutrals, light ---
val SurfaceLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF4F6FB)
val SurfaceVariantLight = Color(0xFFEDF1F8)
val OutlineLight = Color(0xFFD6DEE8)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF5A6B82)

// --- Neutrals, dark ---
val SurfaceDark = Color(0xFF111B2E)
val BackgroundDark = Color(0xFF0A111F)
val SurfaceVariantDark = Color(0xFF1B2942)
val OutlineDark = Color(0xFF334155)
val TextPrimaryDark = Color(0xFFE2E8F0)
val TextSecondaryDark = Color(0xFF94A3B8)

// --- Status semantics: green = done/synced, orange = waiting, red = failed ---
val StatusGreen = Color(0xFF16A34A)
val StatusGreenContainer = Color(0xFFDCFCE7)
val StatusOrange = Color(0xFFF59E0B)
val StatusOrangeContainer = Color(0xFFFEF3C7)
val StatusRed = Color(0xFFDC2626)
val StatusRedContainer = Color(0xFFFEE2E2)
val StatusBlueContainer = Color(0xFFDBEAFE)
val StatusGrey = Color(0xFF64748B)
val StatusGreyContainer = Color(0xFFE2E8F0)

// Dark-theme containers for the same semantics
val StatusGreenContainerDark = Color(0xFF14532D)
val StatusOrangeContainerDark = Color(0xFF713F12)
val StatusRedContainerDark = Color(0xFF7F1D1D)
val StatusBlueContainerDark = Color(0xFF1E3A8A)
val StatusGreyContainerDark = Color(0xFF334155)

// --- Illustration palette ---
// Used by the drawn road-damage examples so they read the same in both themes.
val AsphaltLight = Color(0xFF56627A)
val AsphaltDark = Color(0xFF2A3448)
val AsphaltEdge = Color(0xFF7C8AA3)
val RoadMarking = Color(0xFFF8FAFC)
val DamageShadow = Color(0xFF141B2A)
val WaterBlue = Color(0xFF38BDF8)
val WaterBlueDeep = Color(0xFF0369A1)
