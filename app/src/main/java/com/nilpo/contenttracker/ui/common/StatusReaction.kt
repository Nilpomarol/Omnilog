package com.nilpo.contenttracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.nilpo.contenttracker.core.model.MediaType
import kotlinx.coroutines.launch
import kotlin.math.abs
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
 * A session change short of finishing — logging progress, starting, resuming, pausing, dropping,
 * shelving. The lighter tiers beside [CompletionCelebration]: the same cover-and-stamp language, as
 * a card that stays out of the way.
 */
data class StatusReaction(
    val title: String,
    val coverUrl: String?,
    val previousStatus: TrackingStatus,
    val status: TrackingStatus,
    val mediaType: MediaType,
    val previousProgress: Int,
    val progress: Int,
    /** Null when the title has no fixed length to fill, so the card shows a count without a bar. */
    val progressTotal: Int?,
) {
    val statusChanged: Boolean get() = status != previousStatus
    val progressDelta: Int get() = progress - previousProgress

    /** Pausing or dropping is about stepping away; the numbers stay where they were, so no bar. */
    val showsProgress: Boolean
        get() = status == TrackingStatus.InProgress && (progressDelta != 0 || progressTotal != null)
}

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
    // A plain log wears its media's colour; a change of state wears the state's.
    val accent = if (reaction.statusChanged) visual.color else reaction.mediaType.objectiveAccent()
    val panel = OmnilogTheme.colors.appPanel
    val delta = reaction.progressDelta
    val label = when {
        !reaction.statusChanged -> {
            val sign = if (delta < 0) "−" else "+"
            sign + formatObjectiveNumber(abs(delta)) + " " + progressUnitLabel(reaction.mediaType, abs(delta))
        }
        reaction.status != TrackingStatus.InProgress -> visual.label
        reaction.previousStatus == TrackingStatus.Paused -> stringResource(R.string.status_reaction_resumed)
        reaction.previousStatus.isTerminal() -> stringResource(R.string.status_reaction_reopened)
        else -> stringResource(R.string.status_reaction_started)
    }
    val haptics = LocalHapticFeedback.current
    val settled = LocalInspectionMode.current
    val stamp = remember(reaction) { Animatable(if (settled) 1f else 0f) }
    var advanced by remember(reaction) { mutableStateOf(settled) }
    LaunchedEffect(reaction) {
        if (settled) return@LaunchedEffect
        delay(120)
        // Getting going gets a toggle, a logged step a lighter tick; stepping away stays silent.
        when {
            reaction.statusChanged && reaction.status == TrackingStatus.InProgress ->
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
            !reaction.statusChanged -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        launch { stamp.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
        delay(180)
        advanced = true
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
                if (reaction.showsProgress) ProgressStep(reaction, accent, advanced)
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

/** The value rolling from where it was to where it is, over a bar that fills to match. */
@Composable
private fun ProgressStep(reaction: StatusReaction, accent: Color, advanced: Boolean) {
    val value = if (advanced) reaction.progress else reaction.previousProgress
    val total = reaction.progressTotal
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (total != null && total > 0) {
            val fill by animateFloatAsState(
                targetValue = (value.toFloat() / total).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "progressFill",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(OmnilogTheme.colors.appLine, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(4.dp)
                        .background(accent, CircleShape),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val up = targetState >= initialState
                    (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically { if (up) -it else it } + fadeOut())
                },
                label = "progressCount",
            ) { count ->
                Text(
                    text = formatObjectiveNumber(count),
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (total != null) {
                    " / " + formatObjectiveNumber(total) + " " + progressUnitLabel(reaction.mediaType, total)
                } else {
                    " " + progressUnitLabel(reaction.mediaType, reaction.progress)
                },
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
