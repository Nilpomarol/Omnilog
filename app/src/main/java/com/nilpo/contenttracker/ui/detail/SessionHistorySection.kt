package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The past sessions, most recent first, with the tail folded away.
 *
 * The history used to hang off a rail beside the live card, which cost the card 22dp of width for a
 * line it did not need. Here the live card stands at full width and the re-reads live under their own
 * heading. The most recent one is always open — a re-read is usually the thing you came back to check
 * — and anything older sits behind `Veure més`, so a title read five times does not push the rest of
 * the page down until you ask it to.
 *
 * The header borrows `DetailSectionTitle`'s weight and divider so it reads as a peer of `Dades de
 * l'element` and the sections below it, not as a control bolted onto the card.
 */
@Composable
fun SessionHistorySection(
    sessions: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "historyChevron",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            Spacer(modifier = Modifier.width(8.dp))
            CountBadge(count = sessions.size)
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // The most recent re-read is always visible.
            sessions.first().invoke()

            if (sessions.size > 1) {
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sessions.drop(1).forEach { it.invoke() }
                    }
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (expanded) R.string.show_less else R.string.show_more,
                        ),
                        color = OmnilogTheme.colors.appMuted,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = OmnilogTheme.colors.appMuted,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .rotate(chevronRotation),
                    )
                }
            }
        }
    }
}

/** The session count, as a filled pill so the number reads as a tally rather than part of the title. */
@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = OmnilogTheme.colors.appPanel,
    ) {
        Box(
            modifier = Modifier
                .size(width = if (count >= 10) 26.dp else 22.dp, height = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}
