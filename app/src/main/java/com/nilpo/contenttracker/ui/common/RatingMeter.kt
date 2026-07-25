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

/**
 * A rating, as a figure and nothing else.
 *
 * It has had a meter twice and given it up twice. Ten stars first, which read as decoration beside
 * the progress graphic; then ten cells, which read as *the same graphic* as the progress cells; then
 * one continuous rail, half their height and round where they are square. That last one should have
 * worked and did not, and the reason is not the shape: two full-width horizontal bars stacked on one
 * card read as a pair no matter what either is made of, and the eye tries to compare them.
 *
 * So the card now has exactly one bar on it, and the bar means progress. The rating is the figure,
 * which was always the part carrying the value — a meter for a single integer out of ten was never
 * earning its row.
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The figure and the slash describe one value, so they announce once.
            .clearAndSetSemantics { contentDescription = description },
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
