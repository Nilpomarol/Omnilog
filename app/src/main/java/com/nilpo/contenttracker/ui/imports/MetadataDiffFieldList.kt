package com.nilpo.contenttracker.ui.imports

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.repository.MetadataRefreshChange
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * One row per field the provider would change: tick to take the new value, untick to keep yours.
 * Rows are split by hairlines, and the new value reads in ink while the one it replaces steps back.
 */
@Composable
internal fun MetadataDiffFieldList(
    changes: List<MetadataRefreshChange>,
    selectedFields: Set<MetadataRefreshField>,
    onSelectionChanged: (Set<MetadataRefreshField>) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = OmnilogTheme.accents.Dashboard,
    horizontalPadding: Dp = 0.dp,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(changes, key = { _, change -> change.field.name }) { index, change ->
            val isSelected = change.field in selectedFields
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    color = OmnilogTheme.colors.appLine,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { checked ->
                            onSelectionChanged(
                                if (checked) selectedFields + change.field else selectedFields - change.field,
                            )
                        },
                    )
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = accent,
                        uncheckedColor = OmnilogTheme.colors.appMuted,
                        checkmarkColor = OmnilogTheme.colors.appBackground,
                    ),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(change.field.labelResId()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    if (change.isLocallyOverridden) {
                        Text(
                            text = stringResource(R.string.metadata_refresh_locally_edited),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.accents.Paused,
                        )
                    }
                    MetadataChangeValue(
                        label = stringResource(R.string.metadata_refresh_current_value),
                        // The repository marks an empty field with a placeholder word; show a dash instead.
                        value = if (change.overwritesExistingValue) change.currentValue else "",
                        emphasized = !isSelected,
                    )
                    MetadataChangeValue(
                        label = stringResource(R.string.metadata_refresh_new_value),
                        value = change.newValue,
                        emphasized = isSelected,
                    )
                }
            }
        }
    }
}

/** The value that will end up on the item reads in ink; the other one steps back to muted. */
@Composable
private fun MetadataChangeValue(label: String, value: String, emphasized: Boolean) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = OmnilogTheme.colors.appMuted)) {
                append(label.uppercase(OmnilogLocale))
                append("  ")
            }
            append(value.ifBlank { "—" })
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (emphasized) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
    )
}

@StringRes
private fun MetadataRefreshField.labelResId(): Int = when (this) {
    MetadataRefreshField.Title -> R.string.metadata_refresh_field_title
    MetadataRefreshField.OriginalTitle -> R.string.metadata_refresh_field_original_title
    MetadataRefreshField.ReleaseYear -> R.string.metadata_refresh_field_release_year
    MetadataRefreshField.Language -> R.string.metadata_refresh_field_language
    MetadataRefreshField.ProgressTotal -> R.string.metadata_refresh_field_progress_total
    MetadataRefreshField.Genres -> R.string.metadata_refresh_field_genres
    MetadataRefreshField.Creators -> R.string.metadata_refresh_field_creators
    MetadataRefreshField.Credits -> R.string.metadata_refresh_field_credits
    MetadataRefreshField.Cover -> R.string.metadata_refresh_field_cover
    MetadataRefreshField.Synopsis -> R.string.metadata_refresh_field_synopsis
    MetadataRefreshField.SourceUrl -> R.string.metadata_refresh_field_source_url
    MetadataRefreshField.ExternalRating -> R.string.metadata_refresh_field_external_rating
    MetadataRefreshField.ExternalRatings -> R.string.metadata_refresh_field_external_ratings
    MetadataRefreshField.ProviderStats -> R.string.metadata_refresh_field_provider_stats
}
