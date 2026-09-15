package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The past sessions, most recent first, with the tail folded away.
 *
 * The most recent one is always open — a re-read is usually the thing you came back to check — and
 * anything older sits behind `Veure més`, so a title read five times does not push the rest of the
 * page down until you ask it to. Rows are split by hairlines, the way the library lists are.
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailSectionTitle(text = stringResource(R.string.detail_history))
            Text(
                text = sessions.size.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = OmnilogTheme.colors.appMuted,
            )
        }

        // The most recent re-read is always visible.
        sessions.first().invoke()

        if (sessions.size > 1) {
            AnimatedVisibility(visible = expanded) {
                Column {
                    sessions.drop(1).forEach { session ->
                        HorizontalDivider(color = OmnilogTheme.colors.appLine)
                        session()
                    }
                }
            }
            TextButton(onClick = { expanded = !expanded }) {
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
