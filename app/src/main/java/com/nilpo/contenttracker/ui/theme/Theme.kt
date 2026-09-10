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
 * These are the **dark-theme** values. Prefer `OmnilogTheme.accents` inside composition, which
 * resolves to the light-tuned set on paper surfaces. This object remains the source of truth for
 * the dark values and the fallback for the places that cannot read a CompositionLocal: enum
 * properties such as `MediaSection.accent`, and plain functions called outside composition.
 */
object OmnilogColors {
    val Dashboard = Color(0xFFA6B58B)
    val Anime = Color(0xFFC99A85)
    val Books = Color(0xFFB9AC87)
    val Tv = Color(0xFF4FA8A8)
    val Games = Color(0xFFD4B96A)

    // "Cinema i TV" is one navigation section but two media types, and Stats and Profile both show
    // them apart. Films keep the section's own colour; series get their own so the pair never has
    // to be told apart by icon alone.
    val Movie = Tv
    val Series = Color(0xFF5E8FC4)

    val Planned = Color(0xFF9DA7B2)
    val InProgress = Color(0xFF5E8FC4)
    val Completed = Color(0xFF62A87C)
    val Paused = Color(0xFFD9A05B)
    val Dropped = Color(0xFFCF6679)
}

/**
 * The same accents as [OmnilogColors], resolved for the active theme.
 *
 * The dark values are tuned to glow on charcoal, which makes most of them illegible as ink on
 * paper: measured against the light background, `Games` fell to 1.86:1 and six of the ten were
 * under 3:1. The light set holds each hue and lowers lightness until it clears 4.5:1 against
 * [LightPalette]'s panel — the darker of the two light surfaces, so one value is safe on both
 * the page and the cards that sit on it.
 */
@Immutable
data class OmnilogAccents(
    val Dashboard: Color,
    val Anime: Color,
    val Books: Color,
    val Tv: Color,
    val Games: Color,
    val Movie: Color,
    val Series: Color,
    val Planned: Color,
    val InProgress: Color,
    val Completed: Color,
    val Paused: Color,
    val Dropped: Color,
)

val DarkAccents = OmnilogAccents(
    Dashboard = OmnilogColors.Dashboard,
    Anime = OmnilogColors.Anime,
    Books = OmnilogColors.Books,
    Tv = OmnilogColors.Tv,
    Games = OmnilogColors.Games,
    Movie = OmnilogColors.Movie,
    Series = OmnilogColors.Series,
    Planned = OmnilogColors.Planned,
    InProgress = OmnilogColors.InProgress,
    Completed = OmnilogColors.Completed,
    Paused = OmnilogColors.Paused,
    Dropped = OmnilogColors.Dropped,
)

val LightAccents = OmnilogAccents(
    Dashboard = Color(0xFF526345),
    Anime = Color(0xFF92553F),
    Books = Color(0xFF76613E),
    Tv = Color(0xFF326E6E),
    Games = Color(0xFF796222),
    Movie = Color(0xFF326E6E),
    Series = Color(0xFF38689A),
    Planned = Color(0xFF596672),
    InProgress = Color(0xFF38689A),
    Completed = Color(0xFF3D6F4F),
    Paused = Color(0xFF955407),
    Dropped = Color(0xFFB4374D),
)

val LocalOmnilogAccents = staticCompositionLocalOf { DarkAccents }


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
 * An ivory background with a slightly deeper cream card provides quiet tonal separation, echoing
 * the warm surfaces of the dark theme. Accents are tuned against the darker cream panel.
 */
val LightPalette = OmnilogPalette(
    appBackground = Color(0xFFF5F0E6),
    appPanel = Color(0xFFEEE7DA),
    appPanelTranslucent = Color(0xEFEEE7DA),
    appLine = Color(0x1F3A332B),
    appInk = Color(0xFF2A2521),
    appMuted = Color(0xFF6B6154),
)

/**
 * Ink for text drawn on top of a cover-image scrim (`CoverScrim`).
 *
 * The scrims stay dark in both themes — they exist to darken artwork, and a pale scrim would not —
 * so text on them must stay pale in both themes too. Reading `appInk` here inverted the text to
 * near-black on light while the scrim beneath it stayed dark, which made the covers unreadable.
 */
val OnCoverInk = DarkPalette.appInk

/** Secondary text over a cover scrim; see [OnCoverInk]. */
val OnCoverMuted = DarkPalette.appMuted

val LocalOmnilogPalette = staticCompositionLocalOf { DarkPalette }

/** Composition-scoped access to the active [OmnilogPalette]. */
object OmnilogTheme {
    val colors: OmnilogPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalOmnilogPalette.current

    val accents: OmnilogAccents
        @Composable
        @ReadOnlyComposable
        get() = LocalOmnilogAccents.current
}

private val DarkColors = darkColorScheme(
    primary = OmnilogColors.Dashboard,
    secondary = OmnilogColors.Books,
    tertiary = OmnilogColors.Games,
    background = DarkPalette.appBackground,
    surface = DarkPalette.appPanel,
    surfaceVariant = Color(0xFF302B25),
    primaryContainer = Color(0xFF303A29),
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
    primary = LightAccents.Dashboard,
    secondary = LightAccents.Books,
    tertiary = LightAccents.Games,
    background = LightPalette.appBackground,
    surface = LightPalette.appPanel,
    surfaceVariant = Color(0xFFEDE4D5),
    primaryContainer = Color(0xFFE1E6D7),
    secondaryContainer = Color(0xFFEAE1CE),
    tertiaryContainer = Color(0xFFF3EBD3),
    onPrimary = Color(0xFFF5F0E6),
    onSecondary = Color(0xFFF5F0E6),
    onTertiary = Color(0xFF2A2521),
    onBackground = LightPalette.appInk,
    onSurface = LightPalette.appInk,
    onSurfaceVariant = LightPalette.appMuted,
    outline = Color(0xFF8B8070),
    error = Color(0xFFB3261E),
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
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold),
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
    val accents = if (darkTheme) DarkAccents else LightAccents
    CompositionLocalProvider(
        LocalOmnilogPalette provides palette,
        LocalOmnilogAccents provides accents,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = OmnilogTypography,
            content = content,
        )
    }
}
