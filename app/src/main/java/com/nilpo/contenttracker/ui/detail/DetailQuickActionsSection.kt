package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.AddTrackingSessionRequest
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.CollectionPickerSheet
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.toCollectionPickerOptions
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The page's one strong action, and the quiet ones beside it.
 *
 * The log button takes the room; ownership, collection and a new session recede to an icon with a
 * one-word caption under it, tinted with the accent once they are on. The caption stays one word —
 * the collection's full name already heads the title. With nothing to log, the quiet actions share
 * the row between them.
 */
@Composable
fun DetailQuickActionsSection(
    item: MediaItem,
    collection: MediaCollection?,
    library: List<TrackedMedia>,
    currentSession: TrackingSession?,
    accent: Color,
    onLogProgress: (() -> Unit)?,
    onSaveItemDetails: (String, Long?, String?, Double?, Int?, Boolean) -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCollectionDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    var showNewSessionDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    val logSession = currentSession?.takeIf { onLogProgress != null }
    // Beside the log button each action is a narrow icon-over-caption column; on their own they
    // share the row as icon-and-label pairs, which read as actions rather than floating glyphs.
    val stacked = logSession != null

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val quietModifier = if (stacked) Modifier else Modifier.weight(1f)

        if (logSession != null && onLogProgress != null) {
            Button(
                onClick = onLogProgress,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = logActionLabel(status = logSession.status, mediaType = item.type),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            QuietDivider(modifier = Modifier.padding(start = 8.dp))
        }

        QuietQuickAction(
            text = stringResource(if (item.isOwned) R.string.owned_label else R.string.owned_caption),
            icon = painterResource(R.drawable.ic_owned_badge),
            accent = accent,
            selected = item.isOwned,
            stacked = stacked,
            modifier = quietModifier,
            onClick = {
                onSaveItemDetails(
                    item.title,
                    item.collectionId,
                    null,
                    item.collectionSortOrder,
                    item.effectiveProgressTotal(),
                    !item.isOwned,
                )
            },
        )
        QuietDivider()
        QuietQuickAction(
            text = stringResource(R.string.collection_action_add),
            icon = painterResource(R.drawable.ic_group_collections),
            accent = accent,
            selected = collection != null,
            stacked = stacked,
            modifier = quietModifier,
            onClick = { showCollectionDialog = true },
        )
        if (currentSession?.status != TrackingStatus.Planned) {
            QuietDivider()
            QuietQuickAction(
                text = stringResource(R.string.new_session_title),
                icon = rememberVectorPainter(Icons.Filled.Add),
                accent = accent,
                selected = false,
                stacked = stacked,
                modifier = quietModifier,
                onClick = { showNewSessionDialog = true },
            )
        }
    }

    if (showCollectionDialog) {
        val pickerOptions = remember(library, item.type) {
            library.toCollectionPickerOptions(forType = item.type)
        }
        CollectionPickerSheet(
            itemTitle = item.title,
            providerCollectionTitle = item.providerCollectionTitle,
            options = pickerOptions,
            initialCollectionId = collection?.id,
            initialSortOrder = item.collectionSortOrder,
            accent = accent,
            onDismiss = { showCollectionDialog = false },
            onConfirm = { result ->
                onSaveItemDetails(
                    item.title,
                    result.collectionId,
                    result.newCollectionName,
                    result.sortOrder,
                    item.effectiveProgressTotal(),
                    item.isOwned,
                )
                showCollectionDialog = false
            },
        )
    }

    if (showNewSessionDialog) {
        NewSessionSheet(
            item = item,
            currentSession = currentSession,
            onDismiss = { showNewSessionDialog = false },
            onStartNewSession = {
                onStartNewSession(it)
                showNewSessionDialog = false
            },
        )
    }
}

/**
 * A secondary action with no surface of its own, taking the accent once it is on: an icon over a
 * one-word caption when [stacked], an icon beside its label otherwise. Both are the log button's
 * 48dp tall, so the row adds no whitespace of its own above or below.
 */
@Composable
internal fun QuietQuickAction(
    text: String,
    icon: Painter,
    accent: Color,
    selected: Boolean,
    stacked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) accent else OmnilogTheme.colors.appMuted
    val actionModifier = modifier
        .heightIn(min = 48.dp)
        .widthIn(min = 64.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 4.dp)

    if (stacked) {
        Column(
            modifier = actionModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = tint,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Row(
            modifier = actionModifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint,
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) accent else OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun QuietDivider(modifier: Modifier = Modifier) {
    VerticalDivider(
        modifier = modifier.height(28.dp),
        color = OmnilogTheme.colors.appLine,
    )
}


@Composable
private fun NewSessionSheet(
    item: MediaItem,
    currentSession: TrackingSession?,
    onDismiss: () -> Unit,
    onStartNewSession: (AddTrackingSessionRequest) -> Unit,
) {
    val draftSession = TrackingSession(
        id = -item.id,
        mediaItemId = item.id,
        sessionNumber = (currentSession?.sessionNumber ?: 0) + 1,
        status = TrackingStatus.Planned,
        progressCurrent = 0,
    )

    SessionEditorSheet(
        item = item,
        session = draftSession,
        progressTotal = item.effectiveProgressTotal(),
        titleResId = R.string.new_session_title,
        onDismiss = onDismiss,
        onSaveSessionDetails = { _, status, progress, rating, notes, startedAt, finishedAt ->
            onStartNewSession(
                AddTrackingSessionRequest(
                    mediaItemId = item.id,
                    status = status,
                    progressCurrent = progress,
                    ratingHalfPoints = rating,
                    notes = notes,
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                ),
            )
        },
    )
}

private fun MediaItem.effectiveProgressTotal(): Int? {
    return progressTotal.takeUnless { type == MediaType.Game }
}
