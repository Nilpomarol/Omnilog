package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/** Matches `ActivitySheet`'s rail, widened for the larger node a whole session gets. */
private val RailWidth = 22.dp
private val NodeSize = 13.dp
private val HollowNodeStroke = 2.5.dp
private val CardGap = 10.dp

/**
 * Where the node sits, measured from the top of the row.
 *
 * Chosen to land on the middle of each card's own heading — the filled state chip on the live card,
 * the state dot on a past one — so the thread reads as passing through the state rather than through
 * the panel behind it. They differ because the two cards do not have the same top padding.
 */
private val CurrentNodeCentre = 27.dp
private val PastNodeCentre = 22.dp

/**
 * Every session on this title, strung on one rail.
 *
 * The history used to be a separate `Historial` section further down the page, which meant the fact
 * that a title had been through three passes was something you had to scroll to discover — and once
 * you got there, the past sessions were drawn at exactly the weight of the live one. Threading them
 * makes the relationship structural: the live session is the head of a line that continues into
 * everything that came before it, and reading the node colours downward gives the whole account of a
 * title at a glance.
 *
 * The idiom is not new. `ActivitySheet` already hangs a session's own entries off a rail with this
 * geometry; this is the same drawing one level up, so moving between the two costs nothing to learn.
 *
 * Laid out in a single list item rather than one per session. The rail has to be continuous across
 * the cards, and the detail list puts 20dp of air between its items — which would cut the line into
 * pieces. Sessions per title are counted in single figures, so nothing is lost by composing them
 * together.
 */
@Composable
fun SessionThread(
    modifier: Modifier = Modifier,
    current: (@Composable () -> Unit)? = null,
    currentStateColor: Color = Color.Unspecified,
    past: List<SessionThreadEntry> = emptyList(),
) {
    val rows = past.size + if (current != null) 1 else 0

    // One session and no history is the common case, and a rail with a single node on it draws a
    // thread to nowhere. Below two, the card stands on its own.
    if (rows <= 1) {
        Column(modifier = modifier.fillMaxWidth()) {
            current?.invoke()
            past.firstOrNull()?.content?.invoke()
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        var index = 0
        current?.let { card ->
            ThreadRow(
                nodeColor = currentStateColor,
                filled = true,
                nodeCentre = CurrentNodeCentre,
                isFirst = true,
                isLast = false,
                content = card,
            )
            index++
        }
        past.forEachIndexed { position, entry ->
            ThreadRow(
                nodeColor = entry.stateColor,
                filled = false,
                nodeCentre = PastNodeCentre,
                isFirst = index == 0 && position == 0,
                isLast = position == past.lastIndex,
                content = entry.content,
            )
        }
    }
}

/** One past session's card, and the colour its node is drawn in. */
data class SessionThreadEntry(
    val stateColor: Color,
    val content: @Composable () -> Unit,
)

@Composable
private fun ThreadRow(
    nodeColor: Color,
    filled: Boolean,
    nodeCentre: Dp,
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The rail has no height of its own, so it takes the card's. Without this the line
            // measures against an unbounded constraint inside the detail list and collapses.
            .height(IntrinsicSize.Min),
    ) {
        Rail(
            nodeColor = nodeColor,
            filled = filled,
            nodeCentre = nodeCentre,
            isFirst = isFirst,
            isLast = isLast,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = CardGap),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Rail(
    nodeColor: Color,
    filled: Boolean,
    nodeCentre: Dp,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val line = OmnilogTheme.colors.appLine
    // A hollow node needs the page behind it, not the line — otherwise the rail runs visibly through
    // the middle of the ring.
    val background = OmnilogTheme.colors.appBackground

    Box(modifier = Modifier.width(RailWidth).fillMaxHeight()) {
        // The line starts at the first node and stops at the last, rather than running off the top
        // and trailing away below the final card.
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(2.dp)
                    .height(nodeCentre)
                    .background(line),
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = nodeCentre)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(line),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = nodeCentre - NodeSize / 2)
                .size(NodeSize)
                .background(color = if (filled) nodeColor else background, shape = CircleShape)
                .then(
                    if (filled) {
                        Modifier
                    } else {
                        Modifier.border(width = HollowNodeStroke, color = nodeColor, shape = CircleShape)
                    },
                ),
        )
    }
}
