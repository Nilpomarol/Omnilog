package com.nilpo.contenttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFFFFB347),
    tertiary = Color(0xFF69F0AE),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF121212),
    onPrimary = Color(0xFF001F2A),
    onSecondary = Color(0xFF2A1700),
    onTertiary = Color(0xFF002114),
    onBackground = Color(0xFFF6F6F6),
    onSurface = Color(0xFFF6F6F6),
)

@Composable
fun ContentTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
