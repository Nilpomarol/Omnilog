package com.nilpo.contenttracker.ui.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nilpo.contenttracker.ui.home.MediaSection

private const val ActiveFilterPreferencesName = "omnilog_dashboard_preferences"

/** Hidden rather than visible sections, so a medium added to Ara mateix later shows up by default. */
private const val HiddenActiveSectionsKey = "active_hidden_sections"

/** The former global-dashboard key. Its value now applies only to Ara mateix. */
private const val LegacyDashboardSectionsKey = "dashboard_hidden_sections"

/** The games-only boolean the section filter replaced (UX-18). Read once, to migrate, then removed. */
private const val LegacyHideGamesKey = "hide_games_from_active"

@Composable
fun rememberActiveFilterPreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember(context) {
        context.getSharedPreferences(ActiveFilterPreferencesName, Context.MODE_PRIVATE)
    }
}

/**
 * Which sections Ara mateix leaves out, kept in step with the file.
 *
 * Observed rather than read once at composition: Settings owns the control and Home renders the
 * result, so a cached value would leave Ara mateix filtering by a preference the user has
 * already changed on the other screen.
 */
@Composable
fun rememberHiddenActiveSections(preferences: SharedPreferences): State<Set<MediaSection>> {
    val hidden = remember(preferences) {
        mutableStateOf(preferences.readHiddenActiveSections())
    }
    DisposableEffect(preferences) {
        // A null key means the whole file changed (a clear()), so re-read on it too.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == null || key == HiddenActiveSectionsKey) {
                hidden.value = changed.readHiddenActiveSections()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return hidden
}

fun SharedPreferences.readHiddenActiveSections(): Set<MediaSection> {
    getStringSet(HiddenActiveSectionsKey, null)?.let { stored ->
        return stored.mapNotNullTo(mutableSetOf()) { name ->
            MediaSection.entries.firstOrNull { it.name == name }
        }
    }
    // Carry the former global section filter over to Ara mateix. If that never existed, migrate the
    // original games-only setting. Anyone who set neither starts with nothing hidden.
    val migrated = getStringSet(LegacyDashboardSectionsKey, null)?.mapNotNullTo(mutableSetOf()) { name ->
        MediaSection.entries.firstOrNull { it.name == name }
    } ?: if (getBoolean(LegacyHideGamesKey, false)) setOf(MediaSection.Games) else emptySet()
    edit()
        .putStringSet(HiddenActiveSectionsKey, migrated.mapTo(mutableSetOf()) { it.name })
        .remove(LegacyDashboardSectionsKey)
        .remove(LegacyHideGamesKey)
        .apply()
    return migrated
}

fun SharedPreferences.writeHiddenActiveSections(sections: Set<MediaSection>) {
    edit()
        .putStringSet(HiddenActiveSectionsKey, sections.mapTo(mutableSetOf()) { it.name })
        .apply()
}
