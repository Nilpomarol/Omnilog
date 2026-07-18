package com.nilpo.contenttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R

/**
 * Media and status accents — Omnilog's brand identity, keyed by media type and tracking status.
 *
 * These are theme-invariant today. Light-tuned variants (UX-21) will move into [OmnilogPalette]
 * once the neutral surfaces below are theme-aware, since these values are also baked into enums
 * such as `MediaSection.accent` and flow through the UI as plain [Color] parameters.
 */
object OmnilogColors {
    val Dashboard = Color(0xFFC0693A)
    val Anime = Color(0xFFD88CA8)
    val Books = Color(0xFF9C82D9)
    val Tv = Color(0xFF4FA8A8)
    val Games = Color(0xFFD4B96A)

    val Planned = Color(0xFF9DA7B2)
    val InProgress = Color(0xFF5E8FC4)
    val Completed = Color(0xFF62A87C)
    val Paused = Color(0xFFD9A05B)
    val Dropped = Color(0xFFCF6679)
}

/**
 * The neutral surfaces and text tones that flip between light and dark.
 *
 * Read through [OmnilogTheme.colors] inside composition so a theme change recomposes every reader.
 * Accents stay in [OmnilogColors] because they are theme-invariant and used outside composition.
 */
@Immutable
data class OmnilogPalette(
    val appBackground: Color,
    val appPanel: Color,
    val appPanelTranslucent: Color,
    val appLine: Color,
    val appInk: Color,
    val appMuted: Color,
)

val DarkPalette = OmnilogPalette(
    appBackground = Color(0xFF141312),
    appPanel = Color(0xFF23201D),
    appPanelTranslucent = Color(0xEF23201D),
    appLine = Color(0x944A443C),
    appInk = Color(0xFFF2E9DD),
    appMuted = Color(0xFFC8BDAE),
)

/**
 * Warm-paper light surfaces, the inverse of [DarkPalette]'s warm charcoal rather than a plain white.
 * A parchment background with a lighter card keeps the same "panel lifts off the base" relationship
 * the dark theme has. Accent tuning for light backgrounds is tracked separately (UX-21).
 */
val LightPalette = OmnilogPalette(
    appBackground = Color(0xFFF3ECDF),
    appPanel = Color(0xFFFFFBF4),
    appPanelTranslucent = Color(0xEFFFFBF4),
    appLine = Color(0x1F3A332B),
    appInk = Color(0xFF2A2521),
    appMuted = Color(0xFF6B6154),
)

val LocalOmnilogPalette = staticCompositionLocalOf { DarkPalette }

/** Composition-scoped access to the active [OmnilogPalette]. */
object OmnilogTheme {
    val colors: OmnilogPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalOmnilogPalette.current
}

private val DarkColors = darkColorScheme(
    primary = OmnilogColors.Dashboard,
    secondary = OmnilogColors.Books,
    tertiary = OmnilogColors.Games,
    background = DarkPalette.appBackground,
    surface = DarkPalette.appPanel,
    surfaceVariant = Color(0xFF302B25),
    primaryContainer = Color(0xFF25313A),
    secondaryContainer = Color(0xFF2B213A),
    tertiaryContainer = Color(0xFF352F1E),
    onPrimary = Color(0xFF101417),
    onSecondary = Color(0xFF17130B),
    onTertiary = Color(0xFF101714),
    onBackground = DarkPalette.appInk,
    onSurface = DarkPalette.appInk,
    onSurfaceVariant = DarkPalette.appMuted,
    outline = Color(0xFF74695C),
    error = Color(0xFFE19A94),
)

private val LightColors = lightColorScheme(
    primary = OmnilogColors.Dashboard,
    secondary = OmnilogColors.Books,
    tertiary = OmnilogColors.Games,
    background = LightPalette.appBackground,
    surface = LightPalette.appPanel,
    surfaceVariant = Color(0xFFEDE4D5),
    primaryContainer = Color(0xFFF3E1D6),
    secondaryContainer = Color(0xFFEAE2F5),
    tertiaryContainer = Color(0xFFF3EBD3),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF2A2521),
    onBackground = LightPalette.appInk,
    onSurface = LightPalette.appInk,
    onSurfaceVariant = LightPalette.appMuted,
    outline = Color(0xFF8B8070),
    error = Color(0xFFB3261E),
)

private val DisplayFontFamily = FontFamily(
    Font(R.font.libre_baskerville, weight = FontWeight.Normal),
    Font(R.font.libre_baskerville, weight = FontWeight.Bold),
)

private val BodyFontFamily = FontFamily(
    Font(R.font.lato_regular, weight = FontWeight.Normal),
    Font(R.font.lato_semibold, weight = FontWeight.SemiBold),
    Font(R.font.lato_bold, weight = FontWeight.Bold),
)

private val BaseTypography = Typography()

private val OmnilogTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = BodyFontFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = BodyFontFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = BodyFontFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = BodyFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = BodyFontFamily),
    // Keep compact labels readable across chips, legends, metadata, and navigation.
    // User-facing secondary text should not fall below this 11sp baseline.
    labelSmall = BaseTypography.labelSmall.copy(
        fontFamily = BodyFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun ContentTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalOmnilogPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = OmnilogTypography,
            content = content,
        )
    }
}
