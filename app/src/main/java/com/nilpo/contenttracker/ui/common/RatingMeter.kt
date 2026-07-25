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

/** Half the height of a progress cell, and round rather than square. See [RatingRail]. */
private val RailHeight = 6.dp
private val RailShape = RoundedCornerShape(percent = 50)

/** How present the unearned part of the rail is — a track to measure against, not a value. */
private const val EmptyRailAlpha = 0.14f

/**
 * A rating, as a figure with a continuous rail beneath it.
 *
 * Replaces the ten `Icons.Filled.Star` that used to be drawn twice at two sizes, on the session card
 * and again on every past session. Ten glyphs is a lot of ink to carry one integer, and stars read as
 * decoration next to the progress graphic they sat under. The figure states the number outright and
 * the rail gives it a shape to be read at a glance — one deliberately unlike the cells the progress
 * graphics count in.
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
            // The figure, the slash and the rail describe one value, so they announce once.
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

        RatingRail(rating = clamped, accent = accent)
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

/**
 * The rating as one continuous rail, deliberately unlike anything that counts.
 *
 * This used to be ten cells, which put two rows of cells on the same card: the progress graphic
 * below counts episodes in exactly that language, and the two were being read as the same kind of
 * fact. They are not. Progress is a tally — discrete units you have got through, and the cells are
 * doing real work saying how many. A rating is a judgement on a continuum, where the ninth tenth
 * means nothing on its own.
 *
 * So the form carries the difference three ways at once: unbroken instead of segmented, half the
 * height, and fully rounded where the cells are square. None of that needs a caption to be read.
 */
@Composable
private fun RatingRail(rating: Int, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RailHeight)
            .clip(RailShape)
            .background(accent.copy(alpha = EmptyRailAlpha)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(rating.toFloat() / RatingScale)
                .height(RailHeight)
                .clip(RailShape)
                .background(accent),
        )
    }
}
