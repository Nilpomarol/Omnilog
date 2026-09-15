package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.formatCompactCount
import com.nilpo.contenttracker.ui.common.formatExternalRatingCompact
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
import com.nilpo.contenttracker.ui.common.logoRes
import com.nilpo.contenttracker.ui.common.logoWidth
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

private const val UserRatingScale = 10
private const val GoodScore = 0.75f
private const val FairScore = 0.55f

/**
 * Every score for this work in one place — the user's own included.
 *
 * Each score is a row — provider mark, source and vote count, the value on the right — split from the
 * next by a hairline, the way the library rows are, rather than a stack of bordered cards.
 */
@Composable
fun RatingsSection(
    ratings: List<ExternalRating>,
    /** Half points; see [RatingHalfPoints]. */
    userRating: Int?,
    mediaType: MediaType,
    primaryRatingId: Long?,
    rankingPosition: Int?,
    rankingLabel: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (ratings.isEmpty() && userRating == null) return

    val sortedRatings = ratings.sortedWith(
        compareByDescending<ExternalRating> { it.id == primaryRatingId }.thenBy { it.source.ordinal },
    )
    val labeledRankingSource = rankingLabel?.lowercase()?.let { label ->
        if ("mal" in label) ExternalRatingSource.Mal else ExternalRatingSource.AniList
    }
    val rankingSource = labeledRankingSource
        ?.takeIf { source -> sortedRatings.any { it.source == source } }
        ?: sortedRatings.firstOrNull { it.id == primaryRatingId }?.source
        ?: sortedRatings.firstOrNull()?.source
    val providerDisplays = sortedRatings
        .map { rating ->
            val fraction = (rating.score / rating.maxScore).toFloat().coerceIn(0f, 1f)
            RatingDisplay(
                markSource = rating.source,
                name = rating.source.displayName(),
                subtitle = rating.voteCount?.let { voteCount ->
                    stringResource(R.string.metadata_users) + " · " + formatCompactCount(voteCount.toDouble())
                },
                score = formatExternalRatingCompact(
                    score = rating.score,
                    maxScore = rating.maxScore,
                    mediaType = mediaType,
                    source = rating.source,
                ),
                scoreScale = "/10".takeUnless {
                    mediaType == MediaType.Game && rating.source == ExternalRatingSource.Steam
                },
                rankingPosition = rankingPosition?.takeIf { it > 0 && rating.source == rankingSource },
                tint = verdictTint(fraction),
                isPrimary = rating.id == primaryRatingId,
            )
        }

    val userDisplay = userRating?.let { rating ->
        RatingDisplay(
            markSource = null,
            name = stringResource(R.string.detail_rating_yours),
            subtitle = null,
            score = formatRatingHalfPoints(rating),
            scoreScale = "/$UserRatingScale",
            rankingPosition = null,
            tint = accent,
            isPrimary = false,
        )
    }

    val allDisplays = listOfNotNull(userDisplay).plus(providerDisplays)
    val maxLogoWidth = allDisplays
        .maxOfOrNull { it.markSource.logoWidth(28.dp) }
        ?: 28.dp

    Column(modifier = modifier) {
        DetailSectionTitle(
            text = stringResource(R.string.detail_ratings),
            modifier = Modifier.padding(bottom = 4.dp),
        )
        allDisplays.forEachIndexed { index, display ->
            if (index > 0) {
                HorizontalDivider(color = OmnilogTheme.colors.appLine)
            }
            RatingRow(display, maxLogoWidth)
        }
    }
}

/** One provider or personal score. */
@Composable
private fun RatingRow(display: RatingDisplay, maxLogoWidth: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(maxLogoWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            RatingMark(
                source = display.markSource,
                tint = display.tint,
                height = 28.dp,
                maxWidth = maxLogoWidth,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = display.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (display.isPrimary) {
                    Text(
                        text = stringResource(R.string.primary_external_rating),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            display.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ScoreFigure(
            score = display.score,
            scale = display.scoreScale,
            rankingPosition = display.rankingPosition,
            tint = display.tint,
        )
    }
}

@Composable
private fun ScoreFigure(score: String, scale: String?, rankingPosition: Int?, tint: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = score,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            scale?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        rankingPosition?.let { position ->
            Text(
                text = "${stringResource(R.string.metadata_ranking)} #$position",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/** Draws the provider logo or Omnilog's own rating mark. */
@Composable
private fun RatingMark(source: ExternalRatingSource?, tint: Color, height: Dp, maxWidth: Dp) {
    when {
        source == null -> Icon(
            painter = painterResource(R.drawable.ic_kpi_rating),
            contentDescription = null,
            modifier = Modifier.size(height),
            tint = tint,
        )
        source.logoRes() != null -> ProviderLogo(source = source, height = height, maxWidth = maxWidth)
        else -> Unit
    }
}

@Composable
private fun verdictTint(fraction: Float): Color = when {
    fraction >= GoodScore -> OmnilogTheme.accents.Completed
    fraction >= FairScore -> OmnilogTheme.accents.Paused
    else -> OmnilogTheme.accents.Dropped
}

private data class RatingDisplay(
    val markSource: ExternalRatingSource?,
    val name: String,
    val subtitle: String?,
    val score: String,
    val scoreScale: String?,
    val rankingPosition: Int?,
    val tint: Color,
    val isPrimary: Boolean,
)
