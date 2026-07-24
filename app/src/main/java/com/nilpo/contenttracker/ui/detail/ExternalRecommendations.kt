package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.ui.common.CoverScrim
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.OnCoverMuted
import com.nilpo.contenttracker.ui.theme.OnCoverInk

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
    // so the cards run off the screen edge.
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
                Button(onClick = onRefresh) {
                    Text(text = stringResource(R.string.detail_external_recommendation_retry))
                }
            }
        }

        if (recommendations.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = DetailGutter),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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

@Composable
private fun ExternalRecommendationCard(
    recommendation: ExternalRecommendation,
    accent: Color,
    onClick: () -> Unit,
) {
    val suggestion = recommendation.suggestion
    Surface(
        modifier = Modifier
            .width(144.dp)
            .height(224.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MetadataCoverImage(
                coverUrl = suggestion.coverUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
            )
            CoverScrim()
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp),
                shape = RoundedCornerShape(999.dp),
                color = OmnilogTheme.colors.appPanel,
                border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
            ) {
                Text(
                    text = suggestion.source.displayName(),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnCoverInk,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayMediaTitle(suggestion.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnCoverInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                suggestion.releaseYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnCoverMuted,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.92f)),
                ) {
                    Text(
                        text = stringResource(R.string.detail_external_recommendation_add),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appBackground,
                    )
                }
            }
        }
    }
}
