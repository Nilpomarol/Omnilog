package com.nilpo.contenttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nilpo.contenttracker.R

object OmnilogColors {
    val Anime = Color(0xFFD88CA8)
    val Books = Color(0xFFC9A86A)
    val Tv = Color(0xFF7FA6C9)
    val Games = Color(0xFF8DB7A2)

    val Planned = Color(0xFF9DA7B2)
    val InProgress = Color(0xFF7FA6C9)
    val Completed = Color(0xFF8DB7A2)
    val Paused = Color(0xFFC9A86A)
    val Dropped = Color(0xFFC47F7D)
}

private val DarkColors = darkColorScheme(
    primary = OmnilogColors.Tv,
    secondary = OmnilogColors.Books,
    tertiary = OmnilogColors.Games,
    background = Color(0xFF151719),
    surface = Color(0xFF202327),
    surfaceVariant = Color(0xFF2A2E33),
    primaryContainer = Color(0xFF25313A),
    secondaryContainer = Color(0xFF372F21),
    tertiaryContainer = Color(0xFF24342E),
    onPrimary = Color(0xFF101417),
    onSecondary = Color(0xFF17130B),
    onTertiary = Color(0xFF101714),
    onBackground = Color(0xFFEDEAE4),
    onSurface = Color(0xFFEDEAE4),
    onSurfaceVariant = Color(0xFFC7C2BA),
    outline = Color(0xFF696D72),
    error = Color(0xFFE19A94),
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
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = BodyFontFamily),
)

@Composable
fun ContentTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = OmnilogTypography,
        content = content,
    )
}
