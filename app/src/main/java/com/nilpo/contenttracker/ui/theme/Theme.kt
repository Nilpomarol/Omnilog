package com.nilpo.contenttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R

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

    val AppBackground = Color(0xFF141312)
    val AppPanel = Color(0xFF23201D)
    val AppPanelTranslucent = Color(0xEF23201D)
    val AppLine = Color(0x944A443C)
    val AppInk = Color(0xFFF2E9DD)
    val AppMuted = Color(0xFFC8BDAE)
}

private val DarkColors = darkColorScheme(
    primary = OmnilogColors.Dashboard,
    secondary = OmnilogColors.Books,
    tertiary = OmnilogColors.Games,
    background = OmnilogColors.AppBackground,
    surface = OmnilogColors.AppPanel,
    surfaceVariant = Color(0xFF302B25),
    primaryContainer = Color(0xFF25313A),
    secondaryContainer = Color(0xFF2B213A),
    tertiaryContainer = Color(0xFF352F1E),
    onPrimary = Color(0xFF101417),
    onSecondary = Color(0xFF17130B),
    onTertiary = Color(0xFF101714),
    onBackground = OmnilogColors.AppInk,
    onSurface = OmnilogColors.AppInk,
    onSurfaceVariant = OmnilogColors.AppMuted,
    outline = Color(0xFF74695C),
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
    // Keep compact labels readable across chips, legends, metadata, and navigation.
    // User-facing secondary text should not fall below this 11sp baseline.
    labelSmall = BaseTypography.labelSmall.copy(
        fontFamily = BodyFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun ContentTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = OmnilogTypography,
        content = content,
    )
}
