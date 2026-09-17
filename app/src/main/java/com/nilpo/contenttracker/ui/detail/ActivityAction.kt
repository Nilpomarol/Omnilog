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
import com.nilpo.contenttracker.core.activity.activity
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import java.time.LocalDate

/**
 * The session card's way into [ActivitySheet].
 *
 * Shown whenever the session holds something the sheet can correct: an entry or a transition. A
 * bare start date is a fact the card already states, not history. Hiding the surface below a count
 * made a single mistaken pause impossible to remove. The count on the control is the number of rows
 * the sheet will show, so it says how much story is behind it.
 */
@Composable
fun ActivityAction(
    session: TrackingSession,
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
    val rows = session.activity()
    if (rows.none { it.progress != null || it.statusEvent != null }) return

    // Deliberately not keyed on the entries. Keying it on their count closed the surface the moment
    // anything was deleted from it, so correcting three bad rows meant reopening three times.
    var showSheet by rememberSaveable { mutableStateOf(false) }

    val count = rows.size

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
            session = session,
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
