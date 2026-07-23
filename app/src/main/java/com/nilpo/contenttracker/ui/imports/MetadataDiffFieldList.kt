package com.nilpo.contenttracker.ui.imports

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.repository.MetadataRefreshChange
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@Composable
internal fun MetadataDiffFieldList(
    changes: List<MetadataRefreshChange>,
    selectedFields: Set<MetadataRefreshField>,
    onSelectionChanged: (Set<MetadataRefreshField>) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(changes, key = { it.field.name }) { change ->
            val isSelected = change.field in selectedFields
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnilogTheme.colors.appPanel,
                border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectionChanged(
                                if (isSelected) selectedFields - change.field
                                else selectedFields + change.field,
                            )
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            onSelectionChanged(
                                if (checked) selectedFields + change.field
                                else selectedFields - change.field,
                            )
                        },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(change.field.labelResId()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        if (change.isLocallyOverridden) {
                            Text(
                                text = stringResource(R.string.metadata_refresh_locally_edited),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        MetadataChangeValue(
                            label = stringResource(R.string.metadata_refresh_current_value),
                            value = change.currentValue,
                        )
                        MetadataChangeValue(
                            label = stringResource(R.string.metadata_refresh_new_value),
                            value = change.newValue,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChangeValue(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                append(label)
                append(": ")
            }
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = OmnilogTheme.colors.appMuted,
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
