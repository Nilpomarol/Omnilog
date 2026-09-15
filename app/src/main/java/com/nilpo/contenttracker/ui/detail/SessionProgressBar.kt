package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How far a session has got, as one plain rounded bar.
 *
 * Every medium draws the same bar. The per-medium graphics this replaced — episode cells, reading
 * sittings, a film strip, a weekly activity chart — each said something true, but side by side they
 * made one fact look different on every page. The figure beside the bar carries the detail.
 *
 * A session with no known total, such as a game's open-ended hours, has no scale, so nothing is drawn.
 */
@Composable
fun SessionProgressBar(
    progressCurrent: Int,
    progressTotal: Int?,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
) {
    val total = progressTotal?.takeIf { it > 0 } ?: return
    val fraction by animateFloatAsState(
        targetValue = (progressCurrent.toFloat() / total).coerceIn(0f, 1f),
        label = "sessionProgress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}
