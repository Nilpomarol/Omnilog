package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.nilpo.contenttracker.ui.common.logoRes
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

private const val UserRatingScale = 10
private const val GoodScore = 0.75f
private const val FairScore = 0.55f

/**
 * Every score for this work in one place — the user's own included.
 *
 * Each score is a compact horizontal card: its provider mark anchors the left edge, the source and
 * vote count sit in the middle, and the value is aligned on the right. This preserves the useful
 * context for every provider while avoiding a repeated stack of full-width progress meters.
 */
@Composable
fun RatingsSection(
    ratings: List<ExternalRating>,
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
            score = rating.toString(),
            scoreScale = "/$UserRatingScale",
            rankingPosition = null,
            tint = accent,
            isPrimary = false,
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DetailSectionHeader(
            title = stringResource(R.string.detail_ratings),
            accent = accent,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOfNotNull(userDisplay).plus(providerDisplays).forEach { display ->
                RatingCard(display)
            }
        }
    }
}

/** One provider or personal score, drawn as a compact, long card. */
@Composable
private fun RatingCard(display: RatingDisplay) {
    val isPersonalRating = display.markSource == null
    val isEmphasized = isPersonalRating || display.isPrimary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isEmphasized) display.tint.copy(alpha = 0.11f) else OmnilogTheme.colors.appPanel,
        border = BorderStroke(
            1.dp,
            if (isPersonalRating) display.tint.copy(alpha = 0.55f) else OmnilogTheme.colors.appLine,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RatingMark(
                source = display.markSource,
                tint = display.tint,
                height = 30.dp,
            )
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
}

@Composable
private fun ScoreFigure(score: String, scale: String?, rankingPosition: Int?, tint: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = score,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
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
private fun RatingMark(source: ExternalRatingSource?, tint: Color, height: Dp) {
    when {
        source == null -> Icon(
            painter = painterResource(R.drawable.ic_kpi_rating),
            contentDescription = null,
            modifier = Modifier.size(height),
            tint = tint,
        )
        source.logoRes() != null -> ProviderLogo(source = source, height = height)
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
