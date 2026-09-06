package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.core.model.RatingHalfPoints

/** The scale every rating in the app is recorded on. */
private const val RatingScale = 10

/**
 * A rating, as a figure and nothing else.
 *
 * It has had a meter twice and given it up twice. Ten stars first, which read as decoration beside
 * the progress graphic; then ten cells, which read as *the same graphic* as the progress cells; then
 * one continuous rail, half their height and round where they are square. That last one should have
 * worked and did not, and the reason is not the shape: two full-width horizontal bars stacked on one
 * card read as a pair no matter what either is made of, and the eye tries to compare them.
 *
 * So the card has exactly one bar on it, and the bar means progress. The rating is the figure, which
 * was always the part carrying the value — a meter for a single integer out of ten was never earning
 * its row. What the meter *was* doing, it turns out, is marking the figure as a rating at all: with
 * it gone the number stood unlabelled beside a progress count. The star does that job in a fraction
 * of the space, and the figure gets the size the row was spending on the meter.
 *
 * Callers decide whether an absent rating means "not rated yet" or means nothing worth saying, so
 * this takes a non-null value and is simply not composed when there is none.
 */
@Composable
fun RatingMeter(
    ratingHalfPoints: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = RatingHalfPoints.coerce(ratingHalfPoints)
    val figure = formatRatingHalfPoints(clamped)
    val description = stringResource(R.string.rating_value, figure)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The star, the figure and the slash describe one value, so they announce once.
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The app's own rating mark, the one the library rows and the timeline already carry. With
        // the meter gone the figure had nothing beside it saying what kind of number it was, and a
        // large bare numeral on a card that also shows a progress count is ambiguous at a glance.
        Icon(
            painter = painterResource(R.drawable.ic_kpi_rating),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = FigureBaseline)
                .size(StarSize),
            tint = accent,
        )
        Text(
            text = figure,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
        )
        Text(
            text = "/$RatingScale",
            modifier = Modifier.padding(bottom = FigureBaseline),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = accent.copy(alpha = 0.62f),
        )
    }
}

/** How far the star, the slash and the label sit off the figure's baseline. */
private val FigureBaseline = 7.dp
private val StarSize = 30.dp

/**
 * The same value in one line, for the history cards where the rating is a fact about a finished
 * session rather than the thing the card is about.
 */
@Composable
fun RatingMeterCompact(
    ratingHalfPoints: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = RatingHalfPoints.coerce(ratingHalfPoints)
    val figure = formatRatingHalfPoints(clamped)
    val description = stringResource(R.string.rating_value, figure)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = figure,
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
