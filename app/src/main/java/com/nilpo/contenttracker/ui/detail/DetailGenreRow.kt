package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** How much of the accent is mixed into the panel to tint a pill's fill. */
private const val FillTint = 0.16f

/** How much of the accent is mixed into the panel to draw a pill's edge. */
private const val EdgeTint = 0.55f

/**
 * The item's genres, directly under the cover and still on its artwork.
 *
 * They read as the tail of the title block rather than as a section of the page, which is why they
 * carry the item's own accent instead of the neutral panel the rest of the metadata uses: sitting
 * this close to the collection pill, a grey chip looked like a control that had lost its label.
 *
 * The fill is a solid blend of [panel] and [accent] rather than a translucent accent. This row sits
 * on the artwork, and a wash there picks up whatever is behind each pill — the same thing that made
 * the collection pill unreadable before it went solid. The caller supplies the base so a row on
 * artwork can blend against a dark panel whatever the theme, matching the scrim it is drawn on.
 *
 * Scrolls sideways, so a long genre list stays one line rather than reflowing into a block that
 * competes with the title.
 */
@Composable
fun DetailGenreRow(
    genres: List<String>,
    accent: Color,
    panel: Color,
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) return

    val fill = lerp(panel, accent, FillTint)
    val edge = lerp(panel, accent, EdgeTint)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.forEach { genre ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = fill,
                border = BorderStroke(1.dp, edge),
                contentColor = accent,
            ) {
                Text(
                    text = genre,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
