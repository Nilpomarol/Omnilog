package com.nilpo.contenttracker.ui.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.theme.OmnilogColors

private const val DashboardPreferencesName = "omnilog_dashboard_preferences"

/** Hidden rather than visible sections, so a medium added to the library later shows up by default. */
private const val HiddenDashboardSectionsKey = "dashboard_hidden_sections"

/** The games-only boolean the section filter replaced (UX-18). Read once, to migrate, then removed. */
private const val LegacyHideGamesKey = "hide_games_from_active"

@Composable
fun rememberDashboardPreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember(context) {
        context.getSharedPreferences(DashboardPreferencesName, Context.MODE_PRIVATE)
    }
}

/**
 * Which sections Home leaves out, kept in step with the file.
 *
 * Observed rather than read once at composition: Settings owns the control and Home renders the
 * result, so a cached value would leave the dashboard filtering by a preference the user has
 * already changed on the other screen.
 */
@Composable
fun rememberHiddenDashboardSections(preferences: SharedPreferences): State<Set<MediaSection>> {
    val hidden = remember(preferences) {
        mutableStateOf(preferences.readHiddenDashboardSections())
    }
    DisposableEffect(preferences) {
        // A null key means the whole file changed (a clear()), so re-read on it too.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == null || key == HiddenDashboardSectionsKey) {
                hidden.value = changed.readHiddenDashboardSections()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return hidden
}

fun SharedPreferences.readHiddenDashboardSections(): Set<MediaSection> {
    getStringSet(HiddenDashboardSectionsKey, null)?.let { stored ->
        return stored.mapNotNullTo(mutableSetOf()) { name ->
            MediaSection.entries.firstOrNull { it.name == name }
        }
    }
    // `Amaga jocs` meant exactly "hide the Games section", so it carries over as that and the old
    // key goes away. Anyone who never set it starts with nothing hidden.
    val migrated = if (getBoolean(LegacyHideGamesKey, false)) setOf(MediaSection.Games) else emptySet()
    edit()
        .putStringSet(HiddenDashboardSectionsKey, migrated.mapTo(mutableSetOf()) { it.name })
        .remove(LegacyHideGamesKey)
        .apply()
    return migrated
}

fun SharedPreferences.writeHiddenDashboardSections(sections: Set<MediaSection>) {
    edit()
        .putStringSet(HiddenDashboardSectionsKey, sections.mapTo(mutableSetOf()) { it.name })
        .apply()
}

/**
 * The sections Home draws from, as one chip per shelf.
 *
 * Chips are [MediaSection], not `MediaType`, so the granularity matches the nav and `Cinema i TV`
 * stays a single shelf rather than splitting into two chips nothing else in the app distinguishes.
 * Every section is offered whether or not the library currently holds one: this is a standing
 * preference, not a live filter over the items on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardSectionChips(
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
                    if (isVisible) accent.copy(alpha = 0.42f) else OmnilogColors.AppLine.copy(alpha = 0.70f),
                ),
            ) {
                Text(
                    text = stringResource(section.titleResId),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isVisible) accent else OmnilogColors.AppMuted.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
