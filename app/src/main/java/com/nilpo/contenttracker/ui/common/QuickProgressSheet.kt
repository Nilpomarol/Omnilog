package com.nilpo.contenttracker.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.detail.sessionResumeActionLabel
import com.nilpo.contenttracker.ui.detail.sessionStartActionLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Compact dialog for the UX-13 quick progress actions.
 */
data class QuickCompletion(
    val progress: Int,
    /** Half points; see [RatingHalfPoints]. */
    val ratingHalfPoints: Int?,
    val finishedAt: LocalDate,
)

private enum class QuickProgressPhase {
    Progress,
    CompletionDetails,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickProgressSheet(
    trackedMedia: TrackedMedia,
    accent: Color,
    onCommit: (Int) -> Unit,
    onComplete: (QuickCompletion) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = trackedMedia.currentSession ?: return
    val item = trackedMedia.item
    val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }?.takeIf { it > 0 }

    LaunchedEffect(session.status) {
        if (session.status == TrackingStatus.Completed || session.status == TrackingStatus.Dropped) {
            onDismiss()
        }
    }

    var draftText by remember(session.id, session.progressCurrent) {
        mutableStateOf(session.progressCurrent.toString())
    }
    val draft = draftText.toIntOrNull()?.coerceIn(0, total ?: Int.MAX_VALUE)
    val changed = draft != null && draft != session.progressCurrent
    val promotes = session.status == TrackingStatus.Planned ||
        session.status == TrackingStatus.Paused

    val completedAccent = OmnilogTheme.accents.Completed
    var manualFinishing by remember(session.id) { mutableStateOf(false) }
    var preFinishDraftText by remember(session.id) { mutableStateOf<String?>(null) }
    var phase by remember(session.id) { mutableStateOf(QuickProgressPhase.Progress) }

    val isFinishing = manualFinishing || (total != null && draft != null && draft >= total)

    var ratingDraft by remember(session.id) { mutableStateOf(session.ratingHalfPoints) }
    var finishDate by remember(session.id) {
        mutableStateOf(session.finishedAt ?: LocalDate.now())
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        when (phase) {
            QuickProgressPhase.Progress -> {
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

                    // The rail and the switch read as one control, so they sit closer to each
                    // other than to the header above or the button below.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickProgressRail(
                            text = draftText,
                            total = total,
                            mediaType = item.type,
                            accent = if (isFinishing) completedAccent else accent,
                            onTextChange = { draftText = it },
                        )

                        FinishSwitch(
                            checked = isFinishing,
                            accent = completedAccent,
                            trailing = total?.let { "$it / $it" },
                            onCheckedChange = { checked ->
                                if (checked) {
                                    manualFinishing = true
                                    if (total != null && (draft == null || draft < total)) {
                                        preFinishDraftText = draftText
                                        draftText = total.toString()
                                    }
                                } else {
                                    manualFinishing = false
                                    if (preFinishDraftText != null) {
                                        draftText = preFinishDraftText!!
                                        preFinishDraftText = null
                                    } else if (total != null && draft != null && draft >= total) {
                                        draftText = (total - 1).coerceAtLeast(0).toString()
                                    }
                                }
                            },
                        )
                    }

                    val buttonColor by animateColorAsState(
                        targetValue = if (isFinishing) completedAccent else accent,
                        label = "buttonColor",
                    )
                    Button(
                        onClick = {
                            draft?.let { value ->
                                if (isFinishing) {
                                    phase = QuickProgressPhase.CompletionDetails
                                } else {
                                    onCommit(value)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = draft != null && (isFinishing || changed || promotes),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = contentColorOn(buttonColor),
                        ),
                    ) {
                        Text(
                            text = when {
                                isFinishing -> stringResource(R.string.quick_progress_next)
                                !changed && session.status == TrackingStatus.Planned ->
                                    sessionStartActionLabel(item.type)
                                !changed && session.status == TrackingStatus.Paused ->
                                    sessionResumeActionLabel(item.type)
                                else -> stringResource(R.string.quick_progress_save)
                            },
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            QuickProgressPhase.CompletionDetails -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                                text = stringResource(R.string.quick_progress_completion_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OmnilogTheme.colors.appInk,
                            )
                            Text(
                                text = stringResource(R.string.quick_progress_completion_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                    }

                    CompletionForm(
                        rating = ratingDraft,
                        finishedAt = finishDate,
                        accent = completedAccent,
                        onRatingChange = { ratingDraft = it },
                        onFinishedAtChange = { finishDate = it },
                    )

                    Button(
                        onClick = {
                            draft?.let { value ->
                                onComplete(QuickCompletion(value, ratingDraft, finishDate))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = draft != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = completedAccent,
                            contentColor = contentColorOn(completedAccent),
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

                    TextButton(
                        onClick = { phase = QuickProgressPhase.Progress },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.back))
                    }
                }
            }
        }
    }
}

/**
 * Totals at or below this get a tick per step drawn on the rail; above it the marks would be
 * closer together than a fingertip and the rail runs unmarked instead.
 */
private const val MaxTickedTotal = 40

/** Diameter of the rail's thumb, and so the width the track is inset by at each end. */
private val RailThumbSize = 22.dp

/**
 * Vertical slack trimmed off the rail. A [Slider] claims a 48 dp interactive height around a
 * 10 dp track, which left the rail marooned in whitespace between the figure and the switch.
 * Trimming only changes the space the slider *claims* — it is still drawn and still hit-tested at
 * full height, so the touch target survives.
 */
private val RailSlackTrim = 14.dp

/**
 * Lets the rail bleed to the full content width and gives back its vertical slack.
 *
 * M3 insets the track by half a thumb at each end so the thumb stays inside the slider's bounds,
 * which left the rail visibly narrower than the figure above it and the switch below. Measuring
 * one thumb wider than the slot and centring the overhang puts the track's ends exactly on the
 * content edges, so all three line up.
 */
private fun Modifier.railBleed(): Modifier = layout { measurable, constraints ->
    val overhang = RailThumbSize.roundToPx()
    val trim = RailSlackTrim.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.maxWidth + overhang,
            maxWidth = constraints.maxWidth + overhang,
        ),
    )
    layout(constraints.maxWidth, (placeable.height - trim).coerceAtLeast(0)) {
        placeable.place(-overhang / 2, -trim / 2)
    }
}

/**
 * The single progress control: an editable figure sitting over a draggable rail.
 *
 * This replaces the old stepper/direct-entry split. One control covers every media type because
 * the rail scales with the total instead of the type — it marks the individual steps while there
 * are few enough to aim at and runs unmarked above that, with the figure as the exact path either
 * way. Items with no known total (games, anything missing `progressTotal`) have no scale to draw
 * a rail against, so they keep plain step buttons.
 *
 * The slider is deliberately continuous rather than stepped even when the ticks are drawn: a
 * stepped [Slider] coerces its value to the nearest tick, which turns the run to the total on
 * finishing into a march between episodes instead of a glide. Whole values come from rounding in
 * `onValueChange` instead, which holds the drag to steps without quantising the animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickProgressRail(
    text: String,
    total: Int?,
    mediaType: MediaType,
    accent: Color,
    onTextChange: (String) -> Unit,
) {
    val current = text.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val unit = progressUnitLabel(mediaType, total ?: current)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            ProgressFigure(
                text = text,
                accent = accent,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(min = 44.dp),
                onTextChange = onTextChange,
            )
            Text(
                text = if (total != null) "/ $total $unit" else unit,
                modifier = Modifier.padding(start = 8.dp, bottom = 7.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (total != null && total > 0) {
                Spacer(modifier = Modifier.weight(1f))
                val pct = ((current.toFloat() / total) * 100).toInt().coerceIn(0, 100)
                Text(
                    text = "$pct%",
                    modifier = Modifier.padding(bottom = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }

        if (total != null && total > 0) {
            val stateLabel = "$current / $total $unit"
            val interactionSource = remember { MutableInteractionSource() }
            val dragged by interactionSource.collectIsDraggedAsState()
            // The rail glides when something else moves the figure — the finish switch jumping it
            // to the total, or a typed value — but tracks the finger exactly while it is dragged,
            // where a spring would only ever lag behind.
            val railValue by animateFloatAsState(
                targetValue = current.coerceAtMost(total).toFloat(),
                animationSpec = if (dragged) {
                    snap()
                } else {
                    spring(dampingRatio = 0.85f, stiffness = 200f)
                },
                label = "railValue",
            )

            val trackColors = SliderDefaults.colors(
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.16f),
            )
            val passedTickColor = OmnilogTheme.colors.appPanel.copy(alpha = 0.55f)
            val comingTickColor = accent.copy(alpha = 0.45f)
            val showTicks = total in 2..MaxTickedTotal

            Slider(
                value = railValue,
                onValueChange = { raw ->
                    // A continuous slider's accessibility increment is a hundredth of the range,
                    // which rounds straight back to where it started on a scale as short as an
                    // episode count. Off-drag changes therefore move a whole step instead.
                    val next = if (!dragged && raw != current.toFloat() && abs(raw - current) < 1f) {
                        if (raw > current) current + 1 else current - 1
                    } else {
                        raw.roundToInt()
                    }
                    onTextChange(next.coerceIn(0, total).toString())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .railBleed()
                    .semantics { stateDescription = stateLabel },
                interactionSource = interactionSource,
                valueRange = 0f..total.toFloat(),
                colors = trackColors,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(RailThumbSize)
                            .clip(CircleShape)
                            .background(OmnilogTheme.colors.appPanel)
                            .border(2.5.dp, accent, CircleShape),
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier
                            .height(8.dp)
                            .drawWithContent {
                                drawContent()
                                if (!showTicks) return@drawWithContent
                                val reached = railValue / total
                                val radius = 1.5.dp.toPx()
                                for (step in 1 until total) {
                                    val at = step.toFloat() / total
                                    drawCircle(
                                        color = if (at <= reached) passedTickColor else comingTickColor,
                                        radius = radius,
                                        center = Offset(size.width * at, size.height / 2f),
                                    )
                                }
                            },
                        colors = trackColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 0.dp,
                    )
                },
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StepButton(
                    symbol = "−",
                    enabled = current > 0,
                    accent = accent,
                    contentDescription = stringResource(R.string.quick_progress_decrease),
                    modifier = Modifier.weight(1f),
                    onClick = { onTextChange((current - 1).coerceAtLeast(0).toString()) },
                )
                StepButton(
                    symbol = "+",
                    enabled = true,
                    accent = accent,
                    contentDescription = stringResource(R.string.quick_progress_increase),
                    modifier = Modifier.weight(1f),
                    onClick = { onTextChange((current + 1).toString()) },
                )
            }
        }
    }
}

/**
 * The figure, edited in place. No border until it is touched: a faint accent rule on rest, solid
 * on focus, so it reads as editable without becoming another box on the sheet.
 */
@Composable
private fun ProgressFigure(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val underline by animateColorAsState(
        targetValue = if (focused) accent else accent.copy(alpha = 0.28f),
        label = "figureUnderline",
    )

    BasicTextField(
        value = text,
        onValueChange = { input -> onTextChange(input.filter { it.isDigit() }.take(6)) },
        modifier = modifier.drawBehind {
            val y = size.height - 1.dp.toPx()
            drawLine(
                color = underline,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
            )
        },
        textStyle = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.ExtraBold,
            color = accent,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(accent),
    )
}

@Composable
private fun StepButton(
    symbol: String,
    enabled: Boolean,
    accent: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(46.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(14.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = accent,
        ),
    ) {
        Text(
            text = symbol,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * Full-width finish toggle: the row itself is the switch.
 *
 * The knob travels the whole width while [accent] washes in behind it, so the label ends up
 * sitting on the finished colour — the state change carries more weight than the primary button
 * below it, which is right for the one thing on this sheet that opens the completion step. It
 * replaces a bordered card whose trailing chevron made it read as navigation.
 */
@Composable
private fun FinishSwitch(
    checked: Boolean,
    accent: Color,
    trailing: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = OmnilogTheme.colors
    val onAccent = contentColorOn(accent)
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 220f),
        label = "finishSwitch",
    )

    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .semantics { role = Role.Switch },
        shape = RoundedCornerShape(16.dp),
        color = palette.appBackground,
        border = BorderStroke(1.dp, lerp(palette.appLine, accent, progress)),
    ) {
        BoxWithConstraints {
            val knobWidth = 44.dp
            val inset = 5.dp
            val travel = maxWidth - knobWidth - inset * 2

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(accent),
            )
            Text(
                text = stringResource(R.string.quick_progress_mark_done),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = lerpDp(62.dp, 20.dp, progress)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = lerp(palette.appInk, onAccent, progress),
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = lerpDp(18.dp, 62.dp, progress)),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = lerp(palette.appMuted, onAccent.copy(alpha = 0.72f), progress),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = inset + travel * progress)
                    .size(width = knobWidth, height = 42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(lerp(palette.appMuted.copy(alpha = 0.22f), palette.appPanel, progress)),
                contentAlignment = Alignment.Center,
            ) {
                // The tick belongs to the finished state, so off it is simply an empty well —
                // a greyed check reads as an action already half taken.
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(progress),
                )
            }
        }
    }
}

/**
 * Ink for text and icons drawn on top of a filled accent.
 *
 * The threshold is sRGB's own mid-point rather than a naive 0.5: the media accents sit between
 * 0.13 and 0.35 relative luminance, where 0.5 would put white on every one of them and drop
 * several — the dark theme's Books violet among them — below 3.5:1.
 */
internal fun contentColorOn(accent: Color): Color =
    if (accent.luminance() > 0.179f) Color(0xFF12100E) else Color(0xFFFFF9F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionForm(
    rating: Int?,
    finishedAt: LocalDate,
    accent: Color,
    onRatingChange: (Int?) -> Unit,
    onFinishedAtChange: (LocalDate) -> Unit,
    dateLabel: String = stringResource(R.string.session_finished_label),
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.quick_progress_rating_label),
            style = MaterialTheme.typography.labelMedium,
            color = OmnilogTheme.colors.appMuted,
        )
        TrackingRatingSelector(
            currentRatingHalfPoints = rating,
            accent = accent,
            onRatingSelected = onRatingChange,
        )
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        CompletionDateRow(
            date = finishedAt,
            label = dateLabel,
            accent = accent,
            onDateChange = onFinishedAtChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionDateRow(
    date: LocalDate,
    label: String,
    accent: Color,
    onDateChange: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OmnilogTheme.colors.appMuted,
            )
            Text(
                text = date.format(quickDateFormatter),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = OmnilogTheme.colors.appMuted,
            modifier = Modifier.size(20.dp),
        )
    }
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate(),
                            )
                        }
                        showPicker = false
                    },
                ) { Text(text = stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        ) { DatePicker(state = state) }
    }
}

private val quickDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM ''yy", Locale.getDefault())

/** A status picked from the detail page's session card, with what that change asked for. */
data class StatusChange(
    val status: TrackingStatus,
    val progress: Int,
    /** Half points; see [RatingHalfPoints]. */
    val ratingHalfPoints: Int?,
    /** The day a Completed or Dropped session ended; null for the states that do not end one. */
    val date: LocalDate?,
)

/**
 * The short form behind each state in the session card's menu, built from this sheet's own parts.
 *
 * - In progress and Paused ask where you are: the progress rail.
 * - Completed asks for a verdict and a day, with progress run to the total. An item with no total
 *   (a game's hours) keeps the rail, since there is no total to run it to.
 * - Dropped asks all three: an abandoned session stops somewhere and still earns an opinion.
 *
 * Paused carries no date because the repository keeps finish dates for ended sessions only; a pause
 * is recorded as that day's transition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusChangeSheet(
    trackedMedia: TrackedMedia,
    targetStatus: TrackingStatus,
    targetLabel: String,
    accent: Color,
    onConfirm: (StatusChange) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = trackedMedia.currentSession ?: return
    val item = trackedMedia.item
    val total = item.progressTotal?.takeUnless { item.type == MediaType.Game }?.takeIf { it > 0 }
    val ends = targetStatus == TrackingStatus.Completed || targetStatus == TrackingStatus.Dropped
    val asksProgress = targetStatus != TrackingStatus.Completed || total == null

    var draftText by remember(session.id, targetStatus) {
        mutableStateOf(
            if (targetStatus == TrackingStatus.Completed && total != null) {
                total.toString()
            } else {
                session.progressCurrent.toString()
            },
        )
    }
    val draft = draftText.toIntOrNull()?.coerceIn(0, total ?: Int.MAX_VALUE)
    var ratingDraft by remember(session.id) { mutableStateOf(session.ratingHalfPoints) }
    var endDate by remember(session.id) { mutableStateOf(session.finishedAt ?: LocalDate.now()) }
    // The repository refuses an end before the start, so the sheet says no up front instead of the
    // save silently doing nothing.
    val startedAt = session.startedAt
    val dateValid = !ends || startedAt == null || !endDate.isBefore(startedAt)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        text = stringResource(R.string.status_change_title),
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

            if (asksProgress) {
                QuickProgressRail(
                    text = draftText,
                    total = total,
                    mediaType = item.type,
                    accent = accent,
                    onTextChange = { draftText = it },
                )
            }

            if (ends) {
                CompletionForm(
                    rating = ratingDraft,
                    finishedAt = endDate,
                    accent = accent,
                    onRatingChange = { ratingDraft = it },
                    onFinishedAtChange = { endDate = it },
                    dateLabel = stringResource(
                        if (targetStatus == TrackingStatus.Dropped) {
                            R.string.session_date_dropped
                        } else {
                            R.string.session_finished_label
                        },
                    ),
                )
            }

            Button(
                onClick = {
                    draft?.let { value ->
                        onConfirm(
                            StatusChange(
                                status = targetStatus,
                                progress = value,
                                ratingHalfPoints = ratingDraft,
                                date = endDate.takeIf { ends },
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = draft != null && dateValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = contentColorOn(accent),
                ),
            ) {
                Text(
                    text = stringResource(R.string.status_change_confirm, targetLabel),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
