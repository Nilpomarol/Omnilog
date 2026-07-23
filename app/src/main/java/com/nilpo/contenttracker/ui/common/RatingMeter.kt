package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/** The scale every rating in the app is recorded on. */
private const val RatingScale = 10

private val SegmentHeight = 11.dp
private val SegmentGap = 2.dp
private val SegmentShape = RoundedCornerShape(2.dp)

/** How present an unfilled segment is — a track to measure against, not a value. */
private const val EmptySegmentAlpha = 0.12f

/**
 * A rating, as a figure with a ten-segment meter beneath it.
 *
 * Replaces the ten `Icons.Filled.Star` that used to be drawn twice at two sizes, on the session card
 * and again on every past session. Ten glyphs is a lot of ink to carry one integer, and stars read as
 * decoration next to the progress graphic they sat under. The figure states the number outright and
 * the meter gives it a shape to be read at a glance — the same cell language the progress graphics
 * and the timeline recap strip already speak.
 *
 * Callers decide whether an absent rating means "not rated yet" or means nothing worth saying, so
 * this takes a non-null value and is simply not composed when there is none.
 */
@Composable
fun RatingMeter(
    rating: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    label: String? = stringResource(R.string.session_rating_label),
) {
    val clamped = rating.coerceIn(0, RatingScale)
    val description = stringResource(R.string.rating_value, clamped)

    Column(
        modifier = modifier
            .fillMaxWidth()
            // The figure, the slash and every segment describe one value, so they announce once.
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = clamped.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
            Text(
                text = "/$RatingScale",
                modifier = Modifier.padding(start = 3.dp, bottom = 5.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent.copy(alpha = 0.62f),
            )
            label?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, bottom = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }

        RatingSegments(rating = clamped, accent = accent)
    }
}

/**
 * The same value in one line, for the history cards where the rating is a fact about a finished
 * session rather than the thing the card is about.
 */
@Composable
fun RatingMeterCompact(
    rating: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = rating.coerceIn(0, RatingScale)
    val description = stringResource(R.string.rating_value, clamped)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = clamped.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
        )
        Text(
            text = "/$RatingScale",
            modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}

@Composable
private fun RatingSegments(rating: Int, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SegmentGap),
    ) {
        repeat(RatingScale) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SegmentHeight)
                    .clip(SegmentShape)
                    .background(
                        if (index < rating) accent else accent.copy(alpha = EmptySegmentAlpha),
                    ),
            )
        }
    }
}
