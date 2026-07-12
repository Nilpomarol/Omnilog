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
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRecommendation
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
internal fun ExternalRecommendationsSection(
    recommendations: List<ExternalRecommendation>,
    accent: Color,
    isLoading: Boolean,
    hasError: Boolean,
    onRefresh: () -> Unit,
    onRecommendationClick: (MetadataSuggestion) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailSectionTitle(text = stringResource(R.string.detail_external_recommendations))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.detail_external_recommendation_loading),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogColors.AppMuted,
            )
        }

        if (hasError && recommendations.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.detail_external_recommendation_error),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogColors.AppMuted,
                )
                Button(onClick = onRefresh) {
                    Text(text = stringResource(R.string.detail_external_recommendation_retry))
                }
            }
        }

        if (recommendations.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(end = 4.dp),
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
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MetadataCoverImage(
                coverUrl = suggestion.coverUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF17110D).copy(alpha = 0.12f),
                                Color(0xFF15110E).copy(alpha = 0.96f),
                            ),
                        ),
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp),
                shape = RoundedCornerShape(999.dp),
                color = OmnilogColors.AppPanel,
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
            ) {
                Text(
                    text = suggestion.source.name,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
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
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                suggestion.releaseYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
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
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.AppBackground,
                    )
                }
            }
        }
    }
}
