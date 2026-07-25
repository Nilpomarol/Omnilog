package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
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
 * opening: it says how much story is behind it.
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

    // A control, not a sentence. As a text button this was the only thing on the card that looked
    // like prose you could tap, and it sat immediately beside the edit button without resembling
    // it. Same height, same tonal treatment, same corner — the count stays because it is the reason
    // to open the sheet, but it now reads as a value on a button rather than as a label.
    Surface(
        onClick = { showSheet = true },
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(9.dp),
        color = OmnilogTheme.colors.appInk.copy(alpha = 0.08f),
        contentColor = OmnilogTheme.colors.appMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = stringResource(R.string.activity_open, count),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
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
