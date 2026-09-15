package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@Composable
internal fun ExternalRecommendationsSection(
    recommendations: List<ExternalRecommendation>,
    accent: Color,
    isLoading: Boolean,
    hasError: Boolean,
    onRefresh: () -> Unit,
    onRecommendationClick: (MetadataSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Everything but the carousel keeps the page gutter; the carousel spends it as content padding
    // so the covers run off the screen edge.
    val gutter = Modifier.padding(horizontal = DetailGutter)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSectionTitle(
            text = stringResource(R.string.detail_external_recommendations),
            modifier = gutter,
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().then(gutter))
            Text(
                text = stringResource(R.string.detail_external_recommendation_loading),
                modifier = gutter,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }

        if (hasError && recommendations.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().then(gutter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_external_recommendation_error),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
                TextButton(onClick = onRefresh) {
                    Text(
                        text = stringResource(R.string.detail_external_recommendation_retry),
                        color = accent,
                    )
                }
            }
        }

        if (recommendations.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = DetailGutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = recommendations,
                    key = { recommendation ->
                        "${recommendation.suggestion.source}:${recommendation.suggestion.externalId}"
                    },
                ) { recommendation ->
                    ExternalRecommendationCard(
                        recommendation = recommendation,
                        accent = accent,
                        onClick = { onRecommendationClick(recommendation.suggestion) },
                    )
                }
            }
        }
    }
}

/** A suggestion as a bare cover with its details underneath, like the library grid's tiles. */
@Composable
private fun ExternalRecommendationCard(
    recommendation: ExternalRecommendation,
    accent: Color,
    onClick: () -> Unit,
) {
    val suggestion = recommendation.suggestion
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier = Modifier
            .width(RecommendationTileWidth)
            .clip(shape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MetadataCoverImage(
            coverUrl = suggestion.coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = shape,
        )
        Text(
            text = displayMediaTitle(suggestion.title),
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(suggestion.source.displayName(), suggestion.releaseYear?.toString())
                .joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.detail_external_recommendation_add),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
    }
}

private val RecommendationTileWidth = 112.dp
