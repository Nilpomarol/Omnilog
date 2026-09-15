package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ProgressUpdate
import com.nilpo.contenttracker.core.model.SessionStatusEvent
import com.nilpo.contenttracker.core.model.TrackingStatus
import java.time.LocalDate

/**
 * The session card's way into [ActivitySheet].
 *
 * Hidden below two rows. One entry is a fact, not a sequence — there is nothing to read, and the
 * card already says where the session stands. The count on the button is what makes it worth
 * opening: it says how much story is behind it, which is also why it is on the face of the control
 * rather than only in its accessibility label.
 */
@Composable
fun ActivityAction(
    updates: List<ProgressUpdate>,
    statusEvents: List<SessionStatusEvent>,
    baselineProgress: Int,
    sessionStartedAt: LocalDate?,
    sessionFinishedAt: LocalDate?,
    sessionStatus: TrackingStatus,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onDeleteProgressUpdate: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?, Boolean) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateStatusEventDate: (Long, LocalDate) -> Unit,
    // The history cards draw their controls as a row of equal icon buttons in the header, so there
    // the activity entry sheds its label and count and becomes one disc among them.
    iconOnly: Boolean = false,
) {
    val visibleEvents = meaningfulActivityStatuses(statusEvents)
    // Milestones count towards this. A session with one entry but a start and a finish does have a
    // sequence to read — began, advanced, ended — which is exactly what the rule is protecting.
    val milestoneCount = listOfNotNull(
        sessionStartedAt,
        sessionFinishedAt?.takeIf {
            (sessionStatus == TrackingStatus.Completed || sessionStatus == TrackingStatus.Dropped) &&
                visibleEvents.none { event -> event.status == sessionStatus }
        },
    ).size
    if (updates.size + visibleEvents.size + milestoneCount < 2) return

    // Deliberately not keyed on the entries. Keying it on their count closed the surface the moment
    // anything was deleted from it, so correcting three bad rows meant reopening three times.
    var showSheet by rememberSaveable { mutableStateOf(false) }

    val count = updates.size + visibleEvents.size

    if (iconOnly) {
        FilledTonalIconButton(
            onClick = { showSheet = true },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = stringResource(R.string.activity_open, count),
                modifier = Modifier.size(16.dp),
            )
        }
    } else {
        // On the live session card this is a quiet link beside the dates rather than a control: it
        // is more to read about the session, and a tonal pill there competed with the log button
        // and looked like nothing else on the page.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showSheet = true }
                .heightIn(min = 40.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.activity_open, count),
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
            )
        }
    }

    if (showSheet) {
        ActivitySheet(
            updates = updates,
            statusEvents = statusEvents,
            baselineProgress = baselineProgress,
            sessionStartedAt = sessionStartedAt,
            sessionFinishedAt = sessionFinishedAt,
            sessionStatus = sessionStatus,
            progressTotal = progressTotal,
            mediaType = mediaType,
            accent = accent,
            onDeleteProgressUpdate = onDeleteProgressUpdate,
            onUpdateProgressUpdate = onUpdateProgressUpdate,
            onDeleteStatusEvent = onDeleteStatusEvent,
            onUpdateStatusEventDate = onUpdateStatusEventDate,
            onDismiss = { showSheet = false },
        )
    }
}
