package com.nilpo.contenttracker.ui.timeline

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nilpo.contenttracker.core.model.MediaType

private const val TimelinePreferencesName = "omnilog_timeline_preferences"
private const val HiddenTimelineMediaTypesKey = "hidden_media_types"
private const val HistoryTimelineMediaTypesKey = "history_media_types"

/**
 * What the activity screens show, per media type.
 *
 * Two independent axes: whether the type appears at all, and whether its per-update progress rows
 * appear alongside its milestones. History defaults to off so the timeline reads as a record of
 * what was started and finished, with the granular rows opted into per type. The screen says when
 * they are hidden.
 */
data class TimelineVisibility(
    val hiddenMediaTypes: Set<MediaType> = emptySet(),
    val historyMediaTypes: Set<MediaType> = emptySet(),
) {
    fun isVisible(mediaType: MediaType): Boolean = mediaType !in hiddenMediaTypes

    fun showsHistory(mediaType: MediaType): Boolean = mediaType in historyMediaTypes
}

@Composable
fun rememberTimelinePreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember(context) {
        context.getSharedPreferences(TimelinePreferencesName, Context.MODE_PRIVATE)
    }
}

/** Observed so Home and the full screen both update without an app restart. */
@Composable
fun rememberTimelineVisibility(preferences: SharedPreferences): State<TimelineVisibility> {
    val visibility = remember(preferences) {
        mutableStateOf(preferences.readTimelineVisibility())
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == null ||
                key == HiddenTimelineMediaTypesKey ||
                key == HistoryTimelineMediaTypesKey
            ) {
                visibility.value = changed.readTimelineVisibility()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return visibility
}

fun SharedPreferences.readTimelineVisibility(): TimelineVisibility = TimelineVisibility(
    hiddenMediaTypes = readMediaTypes(HiddenTimelineMediaTypesKey),
    historyMediaTypes = readMediaTypes(HistoryTimelineMediaTypesKey),
)

fun SharedPreferences.writeTimelineVisibility(visibility: TimelineVisibility) {
    edit()
        .putStringSet(HiddenTimelineMediaTypesKey, visibility.hiddenMediaTypes.toStoredNames())
        .putStringSet(HistoryTimelineMediaTypesKey, visibility.historyMediaTypes.toStoredNames())
        .apply()
}

/** Unknown names are dropped rather than failing, so a removed media type cannot break the screen. */
private fun SharedPreferences.readMediaTypes(key: String): Set<MediaType> =
    getStringSet(key, emptySet())
        .orEmpty()
        .mapNotNullTo(mutableSetOf()) { storedName ->
            MediaType.entries.firstOrNull { it.name == storedName }
        }

private fun Set<MediaType>.toStoredNames(): MutableSet<String> =
    mapTo(mutableSetOf()) { it.name }
