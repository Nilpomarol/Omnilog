package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import com.nilpo.contenttracker.ui.theme.OmnilogColors

/**
 * Compact dialog for the UX-13 quick progress actions.
 *
 * The "Ara mateix" tiles stay uncluttered: a single tap opens this sheet, which
 * keeps all the density (stepper / direct entry / complete) off the tile. The
 * concrete control depends on the media type:
 *  - Anime / TV: a −/+ episode stepper (each tap commits immediately, and the sheet
 *    stays open so consecutive taps land on the same control).
 *  - Book / Game: a direct numeric entry whose Desa button commits and closes — the
 *    sheet looks identical after a save, so closing is the only confirmation there is.
 *  - Movie: no progress control, only the complete button.
 *
 * [onSetProgress] receives an absolute target value; the caller (ViewModel) clamps,
 * promotes a Planned/Paused session to In progress, and auto-completes when the
 * value reaches the total.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickProgressSheet(
    trackedMedia: TrackedMedia,
    accent: Color,
    onSetProgress: (Int) -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val session = trackedMedia.currentSession ?: return
    val item = trackedMedia.item
    val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }

    // Close automatically once the item leaves the active states — covers both the
    // explicit Completa button and auto-completion when +1 reaches the total.
    LaunchedEffect(session.status) {
        if (session.status != TrackingStatus.InProgress && session.status != TrackingStatus.Paused) {
            onDismiss()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogColors.AppPanel,
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
                        color = OmnilogColors.AppMuted,
                    )
                    Text(
                        text = displayMediaTitle(item.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.AppInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            when {
                item.type.usesEpisodeStepper() -> QuickStepper(
                    current = session.progressCurrent,
                    total = total,
                    mediaType = item.type,
                    accent = accent,
                    onSetProgress = onSetProgress,
                )
                item.type.usesDirectEntry() -> QuickDirectEntry(
                    current = session.progressCurrent,
                    total = total,
                    mediaType = item.type,
                    accent = accent,
                    onSetProgress = onSetProgress,
                    onSaved = onDismiss,
                )
                // Movie: watch-once, no progress control — only the complete button below.
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.quick_progress_complete),
                    modifier = Modifier.padding(start = 6.dp),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun QuickStepper(
    current: Int,
    total: Int?,
    mediaType: MediaType,
    accent: Color,
    onSetProgress: (Int) -> Unit,
) {
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
            onClick = { onSetProgress(current - 1) },
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
                color = OmnilogColors.AppMuted,
            )
        }
        StepButton(
            symbol = "+",
            enabled = total == null || current < total,
            accent = accent,
            contentDescription = stringResource(R.string.quick_progress_increase),
            onClick = { onSetProgress(current + 1) },
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

@Composable
private fun QuickDirectEntry(
    current: Int,
    total: Int?,
    mediaType: MediaType,
    accent: Color,
    onSetProgress: (Int) -> Unit,
    onSaved: () -> Unit,
) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()
    val maximum = total ?: Int.MAX_VALUE

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() }.take(6) },
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
            if (total != null && total > 0) {
                Text(
                    text = stringResource(
                        R.string.quick_progress_of_total,
                        total,
                        progressUnitLabel(mediaType, total),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = OmnilogColors.AppMuted,
                )
            }
        }
        Button(
            onClick = {
                parsed?.let { onSetProgress(it.coerceIn(0, maximum)) }
                onSaved()
            },
            enabled = parsed != null && parsed != current,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.Black,
            ),
        ) {
            Text(text = stringResource(R.string.save), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun progressUnitLabel(mediaType: MediaType, value: Int): String =
    when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
            -> if (value == 1) {
            stringResource(R.string.progress_unit_episode_one)
        } else {
            stringResource(R.string.progress_unit_episode_many)
        }
        MediaType.Book -> if (value == 1) {
            stringResource(R.string.progress_unit_page_one)
        } else {
            stringResource(R.string.progress_unit_page_many)
        }
        MediaType.Movie -> if (value == 1) {
            stringResource(R.string.progress_unit_minute_one)
        } else {
            stringResource(R.string.progress_unit_minute_many)
        }
        MediaType.Game -> if (value == 1) {
            stringResource(R.string.progress_unit_hour_one)
        } else {
            stringResource(R.string.progress_unit_hour_many)
        }
    }

/** Episodic media where a single +1 tap is the canonical daily update. */
fun MediaType.usesEpisodeStepper(): Boolean =
    this == MediaType.Anime || this == MediaType.TvShow

/** Media measured in pages/hours where a typed value beats incrementing by one. */
fun MediaType.usesDirectEntry(): Boolean =
    this == MediaType.Book || this == MediaType.Game
