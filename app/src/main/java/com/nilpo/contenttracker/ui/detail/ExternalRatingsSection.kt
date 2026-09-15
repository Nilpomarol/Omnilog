package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import com.nilpo.contenttracker.ui.common.PartialStar
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.formatCompactCount
import com.nilpo.contenttracker.ui.common.formatExternalRatingCompact
import com.nilpo.contenttracker.ui.common.formatRatingHalfPoints
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

private const val GoodScore = 0.75f
private const val FairScore = 0.55f

/** A ledger longer than this is the manage page's job. */
private const val MaxLedgerRows = 3

/**
 * The user's verdict beside everyone else's, as two columns split by a hairline.
 *
 * Personal on the left — five large stars in the media accent, or five empty ones over "Sense nota" —
 * and the providers on the right as a short ledger of name, score and audience. The ledger opens the
 * page that manages them.
 */
@Composable
fun RatingsSection(
    ratings: List<ExternalRating>,
    /** Half points; see [RatingHalfPoints]. */
    userRating: Int?,
    mediaType: MediaType,
    primaryRatingId: Long?,
    rankingPosition: Int?,
    accent: Color,
    onManageExternalRatings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ratings.isEmpty() && userRating == null) return

    val sortedRatings = ratings.sortedWith(
        compareByDescending<ExternalRating> { it.id == primaryRatingId }.thenBy { it.source.ordinal },
    )

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RatingHeading(stringResource(R.string.detail_rating_yours))
            RatingStars(halfPoints = userRating, starSize = 26.dp, accent = accent)
            if (userRating == null) {
                Text(
                    text = stringResource(R.string.rating_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }

        if (sortedRatings.isNotEmpty()) {
            VerticalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = OmnilogTheme.colors.appLine,
            )
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onManageExternalRatings),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RatingHeading(stringResource(R.string.detail_external_scores))
                sortedRatings.take(MaxLedgerRows).forEach { rating ->
                    LedgerRow(rating = rating, mediaType = mediaType)
                }
                rankingPosition?.takeIf { it > 0 }?.let { position ->
                    Text(
                        text = "${stringResource(R.string.metadata_ranking)} #$position",
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Normal,
        ),
        color = OmnilogTheme.colors.appInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Five stars on the ten-point scale, each holding four half points; five empty ones for no rating.
 *
 * No rating is not passed through [RatingHalfPoints.coerce]: that lifts zero to the scale's lowest
 * half point, which drew a sliver of accent on the first star of an unrated title.
 */
@Composable
private fun RatingStars(
    halfPoints: Int?,
    starSize: Dp,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = halfPoints?.let(RatingHalfPoints::coerce) ?: 0
    val description = halfPoints
        ?.let { stringResource(R.string.rating_value, formatRatingHalfPoints(clamped)) }
        ?: stringResource(R.string.rating_none)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(5) { index ->
            PartialStar(
                fill = ((clamped - index * 4) / 4f).coerceIn(0f, 1f),
                starSize = starSize,
                accent = accent,
            )
        }
    }
}

/**
 * `Goodreads   8,4 (233,9k)` — the score takes the verdict's colour, everything else stays muted.
 * No scale is printed: every score here is already shown on its own scale.
 */
@Composable
private fun LedgerRow(rating: ExternalRating, mediaType: MediaType) {
    val fraction = (rating.score / rating.maxScore).toFloat().coerceIn(0f, 1f)

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = rating.source.displayName(),
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatExternalRatingCompact(
                score = rating.score,
                maxScore = rating.maxScore,
                mediaType = mediaType,
                source = rating.source,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = verdictTint(fraction),
        )
        rating.voteCount?.let { votes ->
            Text(
                text = " (${formatCompactCount(votes.toDouble())})",
                modifier = Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
            )
        }
    }
}

/**
 * The score's own colour, borrowed from the status accents: green a thing gone well, amber a thing
 * left hanging, red a thing abandoned. The bands are wide on purpose — most scores for things a person
 * chose to track land high, so amber and red carry real information when they appear.
 */
@Composable
private fun verdictTint(fraction: Float): Color = when {
    fraction >= GoodScore -> OmnilogTheme.accents.Completed
    fraction >= FairScore -> OmnilogTheme.accents.Paused
    else -> OmnilogTheme.accents.Dropped
}
