package com.nilpo.contenttracker.ui.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.home.navIconResId
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

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

/**
 * The sections Ara mateix draws from, as one chip per shelf.
 *
 * Chips are [MediaSection], not `MediaType`, so the granularity matches the nav and `Cinema i TV`
 * stays a single shelf rather than splitting into two chips nothing else in the app distinguishes.
 * Every section is offered whether or not the library currently holds one: this is a standing
 * preference, not a live filter over the items on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveSectionChips(
    hiddenSections: Set<MediaSection>,
    onToggleSection: (MediaSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaSection.entries.forEach { section ->
            val isVisible = section !in hiddenSections
            val accent = section.accent
            val contentColor = if (isVisible) {
                accent
            } else {
                OmnilogTheme.colors.appMuted.copy(alpha = 0.70f)
            }
            Surface(
                modifier = Modifier.toggleable(
                    value = isVisible,
                    role = Role.Checkbox,
                    onValueChange = { onToggleSection(section) },
                ),
                shape = RoundedCornerShape(999.dp),
                color = if (isVisible) accent.copy(alpha = 0.16f) else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isVisible) accent.copy(alpha = 0.42f) else OmnilogTheme.colors.appLine.copy(alpha = 0.70f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The same mark the bottom bar uses for this section, so a chip and its shelf
                    // are recognisable as the same thing without reading either label.
                    Icon(
                        painter = painterResource(section.navIconResId),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(section.titleResId),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
