package com.nilpo.contenttracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.detail.sessionStateVisual
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlinx.coroutines.delay

/**
 * A status change short of finishing — starting, resuming, pausing, dropping, shelving. The lighter
 * tier beside [CompletionCelebration]: the same cover-and-stamp language, as a card that stays out
 * of the way.
 */
data class StatusReaction(
    val title: String,
    val coverUrl: String?,
    val previousStatus: TrackingStatus,
    val status: TrackingStatus,
)

/** The event records both ends of a transition, so an In progress row can say start or resume. */
@StringRes
internal fun statusEventDeletionMessageResId(
    previousStatus: TrackingStatus?,
    status: TrackingStatus?,
): Int = when (status) {
    TrackingStatus.Completed -> R.string.deletion_undo_status_event_completed
    TrackingStatus.Paused -> R.string.deletion_undo_status_event_paused
    TrackingStatus.Dropped -> R.string.deletion_undo_status_event_dropped
    TrackingStatus.InProgress -> when (previousStatus) {
        TrackingStatus.Planned -> R.string.deletion_undo_status_event_started
        TrackingStatus.Paused -> R.string.deletion_undo_status_event_resumed
        TrackingStatus.Completed,
        TrackingStatus.Dropped,
        -> R.string.deletion_undo_status_event_reopened

        else -> R.string.deletion_undo_status_event_message
    }

    TrackingStatus.Planned,
    null,
    -> R.string.deletion_undo_status_event_message
}

/** Rides the snackbar queue so the card inherits its timing, undo result, and live region. */
class StatusReactionVisuals(
    val reaction: StatusReaction,
    override val actionLabel: String?,
) : SnackbarVisuals {
    override val message: String get() = reaction.title
    override val withDismissAction: Boolean get() = false
    override val duration: SnackbarDuration get() = SnackbarDuration.Long
}

@Composable
fun StatusReactionCard(
    reaction: StatusReaction,
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val visual = sessionStateVisual(reaction.status)
    val accent = visual.color
    val panel = OmnilogTheme.colors.appPanel
    val label = when {
        reaction.status != TrackingStatus.InProgress -> visual.label
        reaction.previousStatus == TrackingStatus.Paused -> stringResource(R.string.status_reaction_resumed)
        reaction.previousStatus.isTerminal() -> stringResource(R.string.status_reaction_reopened)
        else -> stringResource(R.string.status_reaction_started)
    }
    val haptics = LocalHapticFeedback.current
    val settled = LocalInspectionMode.current
    val stamp = remember(reaction) { Animatable(if (settled) 1f else 0f) }
    LaunchedEffect(reaction) {
        if (settled) return@LaunchedEffect
        delay(120)
        // Getting going gets a tick; stepping away stays silent.
        if (reaction.status == TrackingStatus.InProgress) {
            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
        }
        stamp.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = panel,
        contentColor = OmnilogTheme.colors.appInk,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.padding(end = 4.dp, bottom = 4.dp).size(width = 44.dp, height = 64.dp)) {
                MetadataCoverImage(reaction.coverUrl, Modifier.fillMaxSize(), RoundedCornerShape(8.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 10.dp)
                        .graphicsLayer {
                            scaleX = stamp.value
                            scaleY = stamp.value
                            alpha = stamp.value.coerceIn(0f, 1f)
                        }
                        .size(26.dp)
                        .background(accent, CircleShape)
                        .border(2.dp, panel, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(visual.icon),
                        contentDescription = null,
                        tint = contentColorOn(accent),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label.uppercase(),
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = reaction.title,
                    fontFamily = SerifFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(
                        text = actionLabel,
                        color = OmnilogTheme.colors.appMuted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun TrackingStatus.isTerminal() = this == TrackingStatus.Completed || this == TrackingStatus.Dropped
