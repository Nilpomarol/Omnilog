package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.detail.sessionResumeActionLabel
import com.nilpo.contenttracker.ui.detail.sessionStartActionLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * Compact dialog for the UX-13 quick progress actions.
 *
 * The "Ara mateix" tiles stay uncluttered: a single tap opens this sheet, which keeps all the
 * density off the tile. Every startable status routes here — Planned and Paused included — so the
 * same gesture always means "tell me where you are", never a blind status flip.
 *
 * Editing is deliberately deferred: the stepper and the text field both write to a local draft,
 * and nothing is persisted until the primary button is pressed. That costs episodic media one tap
 * versus the old commit-on-every-tap stepper, but it is what lets a single button describe the
 * outcome honestly — and it stops a `+` onto the final episode from auto-completing the item
 * before you have even looked at the button.
 *
 * Finishing is a checkbox rather than a second button, because "fill the progress to the total"
 * and "mark it completed" are the same act whenever a total exists — offering them separately made
 * the user perform it twice. Ticking the box fills the draft to the total and turns the single
 * button into the completion action; unticking restores the draft you had. On a total-less item
 * (any game) there is nothing to fill, so the tick simply means "completed at whatever the draft
 * says", which is what keeps games from needing a layout of their own.
 *
 * That leaves exactly one call to action at all times. Its label is derived from the draft, not
 * the status alone:
 *  - box ticked               -> "Marca com a completat", in the Completat green
 *  - Planned, draft untouched -> "Comença"
 *  - Paused, draft untouched  -> "Reprèn"
 *  - otherwise                -> "Desa el progrés"
 *
 * [onCommit] receives an absolute target value; the caller (ViewModel) clamps, promotes a
 * Planned/Paused session to In progress, and completes when the value reaches the total.
 * [onComplete] receives the same draft, so a game completed at 42 hours records the 42.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickProgressSheet(
    trackedMedia: TrackedMedia,
    accent: Color,
    onCommit: (Int) -> Unit,
    onComplete: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = trackedMedia.currentSession ?: return
    val item = trackedMedia.item
    // Games measure open-ended hours, so they never carry a total. Every other type does when its
    // metadata supplied one — including movies, whose total is the TMDB runtime in minutes.
    val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }?.takeIf { it > 0 }

    // Close once the item lands somewhere the sheet can no longer act on. Planned, In progress and
    // Paused are all editable here; Completed and Dropped are not.
    LaunchedEffect(session.status) {
        if (session.status == TrackingStatus.Completed || session.status == TrackingStatus.Dropped) {
            onDismiss()
        }
    }

    // Re-seeded whenever the persisted value moves under us, so an external edit is not silently
    // overwritten by a stale draft.
    var draftText by remember(session.id, session.progressCurrent) {
        mutableStateOf(session.progressCurrent.toString())
    }
    val draft = draftText.toIntOrNull()?.coerceIn(0, total ?: Int.MAX_VALUE)
    val changed = draft != null && draft != session.progressCurrent
    val promotes = session.status == TrackingStatus.Planned ||
        session.status == TrackingStatus.Paused

    // With a total the tick is just a view of the draft, so editing the number back down unticks
    // the box on its own. Without one there is nothing to read it from, so it holds its own state.
    var doneChecked by remember(session.id) { mutableStateOf(false) }
    var draftBeforeDone by remember(session.id) { mutableStateOf<String?>(null) }
    val done = if (total != null) draft == total else doneChecked
    val completedAccent = OmnilogTheme.accents.Completed
    val onDoneChange: (Boolean) -> Unit = { checked ->
        doneChecked = checked
        if (total != null) {
            if (checked) {
                draftBeforeDone = draftText
                draftText = total.toString()
            } else {
                draftText = draftBeforeDone ?: session.progressCurrent.toString()
                draftBeforeDone = null
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetadataCoverImage(
                    coverUrl = item.coverUrl,
                    modifier = Modifier.size(width = 40.dp, height = 60.dp),
                    shape = RoundedCornerShape(6.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.quick_progress_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appMuted,
                    )
                    Text(
                        text = displayMediaTitle(item.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // The control takes the accent while you are editing and the Completat green once the
            // box is ticked, so the whole sheet — control, tick and button — agrees on what the
            // next tap will do.
            val controlAccent = if (done) completedAccent else accent
            if (item.type.usesEpisodeStepper()) {
                QuickStepper(
                    draft = draft,
                    total = total,
                    mediaType = item.type,
                    accent = controlAccent,
                    onDraftChange = { draftText = it.toString() },
                )
            } else {
                QuickDirectEntry(
                    text = draftText,
                    total = total,
                    mediaType = item.type,
                    accent = controlAccent,
                    onTextChange = { draftText = it },
                )
            }

            DoneToggleRow(
                checked = done,
                accent = completedAccent,
                onCheckedChange = onDoneChange,
            )

            Button(
                onClick = { draft?.let { if (done) onComplete(it) else onCommit(it) } },
                modifier = Modifier.fillMaxWidth(),
                // A Planned or Paused item always has something to commit — the promotion itself —
                // even when the number never moved.
                enabled = draft != null && (done || changed || promotes),
                colors = ButtonDefaults.buttonColors(
                    containerColor = controlAccent,
                    contentColor = Color.Black,
                ),
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = when {
                        done -> stringResource(R.string.quick_progress_complete)
                        !changed && session.status == TrackingStatus.Planned ->
                            sessionStartActionLabel(item.type)
                        !changed && session.status == TrackingStatus.Paused ->
                            sessionResumeActionLabel(item.type)
                        else -> stringResource(R.string.quick_progress_save)
                    },
                    modifier = Modifier.padding(start = if (done) 6.dp else 0.dp),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun QuickStepper(
    draft: Int?,
    total: Int?,
    mediaType: MediaType,
    accent: Color,
    onDraftChange: (Int) -> Unit,
) {
    val current = draft ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepButton(
            symbol = "−",
            enabled = current > 0,
            accent = accent,
            contentDescription = stringResource(R.string.quick_progress_decrease),
            onClick = { onDraftChange(current - 1) },
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (total != null) "$current / $total" else "$current",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
            Text(
                text = progressUnitLabel(mediaType, total ?: current),
                style = MaterialTheme.typography.labelMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        StepButton(
            symbol = "+",
            enabled = total == null || current < total,
            accent = accent,
            contentDescription = stringResource(R.string.quick_progress_increase),
            onClick = { onDraftChange(current + 1) },
        )
    }
}

@Composable
private fun StepButton(
    symbol: String,
    enabled: Boolean,
    accent: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(52.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = accent,
        ),
    ) {
        // The glyph is decorative; the button carries the accessibility label.
        Text(
            text = symbol,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * The finish affordance. A divider sets it apart from the progress control above, because it is
 * the one thing in the sheet that changes what the button will do rather than what it will write.
 * The whole row is the target, not just the box.
 */
@Composable
private fun DoneToggleRow(
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(
                checked = checked,
                // The row already carries the click and the label; a nested target would only
                // give TalkBack a second, unlabelled way in.
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = accent,
                    checkmarkColor = Color.Black,
                    uncheckedColor = OmnilogTheme.colors.appMuted,
                ),
            )
            Text(
                text = stringResource(R.string.quick_progress_mark_done),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.SemiBold,
                color = if (checked) accent else OmnilogTheme.colors.appInk,
            )
        }
    }
}

@Composable
private fun QuickDirectEntry(
    text: String,
    total: Int?,
    mediaType: MediaType,
    accent: Color,
    onTextChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { input -> onTextChange(input.filter { it.isDigit() }.take(6)) },
                modifier = Modifier
                    .widthIn(min = 72.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = accent,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(accent),
            )
        }
        if (total != null) {
            Text(
                text = stringResource(
                    R.string.quick_progress_of_total,
                    total,
                    progressUnitLabel(mediaType, total),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/**
 * Episodic media where counting up one at a time is the natural gesture. Everything else is
 * measured in pages, hours or minutes and gets the typed field instead — movies included, since
 * their total is the runtime, so a paused film can record where you stopped rather than being an
 * all-or-nothing toggle.
 */
fun MediaType.usesEpisodeStepper(): Boolean =
    this == MediaType.Anime || this == MediaType.TvShow
