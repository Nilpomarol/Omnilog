package com.nilpo.contenttracker.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** The three theme options offered in Settings, in display order. */
enum class ThemePreference {
    /** Follow the device's light/dark setting. */
    System,
    Light,
    Dark,
}

private const val ThemePreferencesName = "omnilog_theme_preferences"
private const val ThemeModeKey = "theme_mode"

@Composable
fun rememberThemePreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember(context) {
        context.getSharedPreferences(ThemePreferencesName, Context.MODE_PRIVATE)
    }
}

fun SharedPreferences.readThemePreference(): ThemePreference {
    val stored = getString(ThemeModeKey, null)
    return ThemePreference.entries.firstOrNull { it.name == stored } ?: ThemePreference.System
}

fun SharedPreferences.writeThemePreference(preference: ThemePreference) {
    edit().putString(ThemeModeKey, preference.name).apply()
}

/**
 * The active theme choice, kept in step with the file.
 *
 * Observed rather than read once so that changing the option in Settings immediately re-themes the
 * rest of the app, which reads the same store from a different composition subtree.
 */
@Composable
fun rememberThemePreference(preferences: SharedPreferences): State<ThemePreference> {
    val preference = remember(preferences) {
        mutableStateOf(preferences.readThemePreference())
    }
    DisposableEffect(preferences) {
        // A null key means the whole file changed (a clear()), so re-read on it too.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == null || key == ThemeModeKey) {
                preference.value = changed.readThemePreference()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return preference
}

/** Resolves a [ThemePreference] to a concrete dark/light decision for the current device state. */
@Composable
fun ThemePreference.resolveDarkTheme(): Boolean = when (this) {
    ThemePreference.System -> isSystemInDarkTheme()
    ThemePreference.Light -> false
    ThemePreference.Dark -> true
}
