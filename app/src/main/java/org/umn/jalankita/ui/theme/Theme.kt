package org.umn.jalankita.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import org.umn.jalankita.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = BrightBlue,
    onPrimary = Color.White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = NavyBlue,
    inversePrimary = BrightBlueLight,
    secondary = NavyBlue,
    onSecondary = Color.White,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = NavyBlueDeep,
    tertiary = BrightBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = NavyBlueDeep,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = BrightBlue,
    inverseSurface = NavyBlue,
    inverseOnSurface = Color(0xFFF1F5F9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFD),
    surfaceContainer = Color(0xFFF2F5F9),
    surfaceContainerHigh = Color(0xFFEAEFF6),
    surfaceContainerHighest = Color(0xFFE3EAF3),
    outline = OutlineLight,
    outlineVariant = Color(0xFFE4E9F1),
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedContainer,
    onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0x66000000),
)

private val DarkColors = darkColorScheme(
    primary = BrightBlueLight,
    onPrimary = NavyBlueDeep,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = BlueContainer,
    inversePrimary = BrightBlue,
    secondary = BrightBlueLight,
    onSecondary = NavyBlueDeep,
    secondaryContainer = BlueContainerDark,
    onSecondaryContainer = BlueContainer,
    tertiary = BrightBlueLight,
    onTertiary = NavyBlueDeep,
    tertiaryContainer = Color(0xFF1E293B),
    onTertiaryContainer = BlueContainer,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = BrightBlueLight,
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = NavyBlueDeep,
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF0D1626),
    surfaceContainer = Color(0xFF13203A),
    surfaceContainerHigh = Color(0xFF1A2A47),
    surfaceContainerHighest = Color(0xFF213254),
    outline = OutlineDark,
    outlineVariant = Color(0xFF243146),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = StatusRedContainerDark,
    onErrorContainer = StatusRedContainer,
    scrim = Color(0x99000000),
)

/**
 * Whether the *app* resolved to a dark theme.
 *
 * Components must read this rather than `isSystemInDarkTheme()`: once Pengaturan
 * can force a theme, the system setting no longer answers the question, and
 * status badges picked light containers on a dark screen.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * [themeMode] lets Pengaturan override the system setting; SYSTEM keeps following it.
 * The status and navigation bar icon colours follow the resolved theme, not the
 * system one, so a forced light theme at night still has readable system bars.
 */
@Composable
fun JalanKitaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JalanKitaTypography,
            shapes = JalanKitaShapes,
            content = content,
        )
    }
}
