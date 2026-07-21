package com.nilpo.contenttracker.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineConfigurationSheet(
    visibility: TimelineVisibility,
    onVisibilityChange: (TimelineVisibility) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.timeline_settings_title),
                    modifier = Modifier.weight(1f),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (visibility.hiddenMediaTypes.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onVisibilityChange(visibility.copy(hiddenMediaTypes = emptySet()))
                        },
                    ) {
                        Text(text = stringResource(R.string.timeline_settings_show_all))
                    }
                }
            }
            Text(
                text = stringResource(R.string.timeline_settings_body),
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
            ) {
                items(MediaType.entries, key = { it.name }) { mediaType ->
                    TimelineMediaTypeRow(
                        mediaType = mediaType,
                        visibility = visibility,
                        onVisibilityChange = onVisibilityChange,
                    )
                    HorizontalDivider(color = OmnilogTheme.colors.appLine)
                }
            }
        }
    }
}

@Composable
private fun TimelineMediaTypeRow(
    mediaType: MediaType,
    visibility: TimelineVisibility,
    onVisibilityChange: (TimelineVisibility) -> Unit,
) {
    val isVisible = visibility.isVisible(mediaType)
    val showsHistory = visibility.showsHistory(mediaType)

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isVisible,
                    role = Role.Switch,
                    onValueChange = { visible ->
                        onVisibilityChange(
                            visibility.copy(
                                hiddenMediaTypes = if (visible) {
                                    visibility.hiddenMediaTypes - mediaType
                                } else {
                                    visibility.hiddenMediaTypes + mediaType
                                },
                            ),
                        )
                    },
                )
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = mediaType.timelineSettingsLabel(),
                modifier = Modifier.weight(1f),
                color = OmnilogTheme.colors.appInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Switch(checked = isVisible, onCheckedChange = null)
        }

        // Indented and dimmed while the type is hidden: history is a property of a shown type, and
        // toggling it for something invisible would have no observable effect.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = showsHistory,
                    enabled = isVisible,
                    role = Role.Switch,
                    onValueChange = { enabled ->
                        onVisibilityChange(
                            visibility.copy(
                                historyMediaTypes = if (enabled) {
                                    visibility.historyMediaTypes + mediaType
                                } else {
                                    visibility.historyMediaTypes - mediaType
                                },
                            ),
                        )
                    },
                )
                .padding(start = 12.dp, top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.timeline_settings_history),
                modifier = Modifier.weight(1f),
                color = if (isVisible) {
                    OmnilogTheme.colors.appMuted
                } else {
                    OmnilogTheme.colors.appMuted.copy(alpha = 0.45f)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = showsHistory && isVisible,
                onCheckedChange = null,
                enabled = isVisible,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = OmnilogTheme.accents.Dashboard,
                ),
            )
        }
    }
}

@Composable
private fun MediaType.timelineSettingsLabel(): String = when (this) {
    MediaType.Anime -> stringResource(R.string.stats_media_plural_anime)
    MediaType.Book -> stringResource(R.string.stats_media_plural_book)
    MediaType.Movie -> stringResource(R.string.stats_media_plural_movie)
    MediaType.TvShow -> stringResource(R.string.stats_media_plural_tv_show)
    MediaType.Game -> stringResource(R.string.stats_media_plural_game)
}
