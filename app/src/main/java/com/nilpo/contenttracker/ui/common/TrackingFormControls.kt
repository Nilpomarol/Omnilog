package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import kotlin.math.ceil
import kotlin.math.roundToInt
import com.nilpo.contenttracker.core.model.RatingHalfPoints
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect

@Composable
@Suppress("UNUSED_PARAMETER")
fun TrackingStatusSelector(
    selectedStatus: TrackingStatus,
    accent: Color,
    onStatusSelected: (TrackingStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    OmnilogDropdownField(
        selectedOption = selectedStatus,
        options = TrackingStatus.entries,
        optionLabel = { stringResource(it.labelResId()) },
        onOptionSelected = onStatusSelected,
        modifier = modifier,
        optionColor = { statusColor(it) },
        optionIcon = { status, tint ->
            Icon(
                painter = painterResource(status.iconResId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint,
            )
        },
    )
}

@Composable
private fun LegacyTrackingProgressField(
    value: String,
    progressTotal: Int?,
    mediaType: MediaType,
    label: String,
    accent: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = value.toIntOrNull() ?: 0
    val maximum = progressTotal ?: Int.MAX_VALUE
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalIconButton(
                onClick = { onValueChange((current - 1).coerceAtLeast(0).toString()) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = accent.copy(alpha = 0.14f), contentColor = accent),
            ) { Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Light) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = OmnilogTheme.colors.appMuted)
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        onValueChange(digits.toIntOrNull()?.coerceIn(0, maximum)?.toString() ?: digits)
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = trackingTextFieldColors(accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                progressTotal?.takeIf { it > 0 }?.let { total ->
                    Text(text = "de $total ${progressUnitLabel(mediaType, total)}", style = MaterialTheme.typography.labelMedium, color = OmnilogTheme.colors.appMuted)
                }
            }
            FilledTonalIconButton(
                onClick = { onValueChange((current + 1).coerceAtMost(maximum).toString()) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = accent.copy(alpha = 0.14f), contentColor = accent),
            ) { Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Light) }
        }
    }
}

@Composable
fun TrackingProgressField(
    value: String,
    progressTotal: Int?,
    mediaType: MediaType,
    label: String,
    accent: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = value.toIntOrNull() ?: 0
    val maximum = progressTotal ?: Int.MAX_VALUE
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                    if (progressTotal != null && progressTotal > 0) {
                        val percentage = ((current.toFloat() / progressTotal).coerceIn(0f, 1f) * 100).toInt()
                        Text(
                            text = "$current / $progressTotal ${progressUnitLabel(mediaType, progressTotal)} • $percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    } else {
                        Text(
                            text = "$current ${progressUnitLabel(mediaType, current)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            onValueChange(digits.toIntOrNull()?.coerceIn(0, maximum)?.toString() ?: digits)
                        },
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = accent,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(accent),
                    )
                }
            }
            if (progressTotal != null && progressTotal > 0) {
                val fraction = (current.toFloat() / progressTotal).coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 150f
                    ),
                    label = "progressAnimation"
                )
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
            }
        }
    }
}

/**
 * Ten stars, rated by where the finger lands: the left half of a star is the half point, the right
 * half is the whole one.
 *
 * A tap alone would put each of the twenty values behind a target around 16dp wide, so the row also
 * takes a drag — press anywhere and slide, and the figure under the stars follows the finger. That
 * is what makes the small zones workable: landing exactly on 7,5 by touch is hard, sliding onto it
 * while watching the number is not.
 *
 * For assistive technology the row is a slider rather than twenty targets: it carries the range,
 * the step count and a `setProgress` action, so TalkBack and switch access adjust it the way they
 * adjust any other slider.
 */
@Composable
fun TrackingRatingSelector(
    currentRatingHalfPoints: Int?,
    accent: Color,
    onRatingSelected: (Int?) -> Unit,
) {
    val figure = currentRatingHalfPoints?.let(::formatRatingHalfPoints)
    val description = when (figure) {
        null -> stringResource(R.string.rating_none)
        else -> stringResource(R.string.library_row_personal_rating, figure)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val starSize = minOf(30.dp, maxWidth / 10)
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = (currentRatingHalfPoints ?: 0).toFloat(),
                            range = 0f..RatingHalfPoints.Max.toFloat(),
                            // Zero included, since clearing the rating is a position too.
                            steps = RatingHalfPoints.Max - 1,
                        )
                        setProgress { target ->
                            val value = target.roundToInt()
                            onRatingSelected(value.takeIf { it >= RatingHalfPoints.Min })
                            true
                        }
                    }
                    .pointerInput(currentRatingHalfPoints, widthPx) {
                        detectTapGestures { offset ->
                            val value = ratingHalfPointsAt(offset.x, widthPx)
                            // Tapping the value it already holds clears it, the way the row has
                            // always behaved.
                            onRatingSelected(value.takeIf { it != currentRatingHalfPoints })
                        }
                    }
                    .pointerInput(widthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset -> onRatingSelected(ratingHalfPointsAt(offset.x, widthPx)) },
                            onHorizontalDrag = { change, _ -> onRatingSelected(ratingHalfPointsAt(change.position.x, widthPx)) },
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                (1..10).forEach { star ->
                    val starHalfPoints = star * 2
                    val fill = when {
                        currentRatingHalfPoints == null -> 0f
                        currentRatingHalfPoints >= starHalfPoints -> 1f
                        currentRatingHalfPoints == starHalfPoints - 1 -> 0.5f
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PartialStar(fill = fill, starSize = starSize, accent = accent)
                    }
                }
            }
        }
        if (figure != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$figure / 10",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Text(
                    text = stringResource(R.string.rating_clear),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.78f),
                    modifier = Modifier.clickable { onRatingSelected(null) }.padding(vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * Which of the twenty rating positions a touch at [x] across a row [width] wide is asking for.
 *
 * The row is measured edge to edge, so the first half-star starts at zero rather than at the first
 * star's centre, and every position gets the same slice of the row. A drag that runs off either end
 * holds at the nearest value instead of clearing the rating, which is what a finger sliding past
 * the last star means.
 */
internal fun ratingHalfPointsAt(x: Float, width: Float): Int {
    if (width <= 0f) return RatingHalfPoints.Min
    return ceil(x / width * RatingHalfPoints.Max)
        .toInt()
        .coerceIn(RatingHalfPoints.Min, RatingHalfPoints.Max)
}

/**
 * A star filled from the left by [fill], for the half-lit one.
 *
 * The fill clips the *painting* of a full-size star, not its layout. Sizing a box to half the width
 * and putting the icon inside it does not draw half a star: the vector scales to fit whatever box it
 * is given, so a half-width box yields a whole star at half the size, centred in the slot.
 *
 * Drawn as the empty star with the accent star clipped over it, because icons-core has no half star
 * and the fill is the only thing that varies. The row announces itself as a whole, so the stars
 * carry no descriptions of their own.
 */
@Composable
internal fun PartialStar(
    fill: Float,
    starSize: Dp,
    accent: Color,
) {
    val emptyTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    Box(modifier = Modifier.size(starSize)) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(starSize),
            tint = emptyTint,
        )
        if (fill > 0f) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(starSize)
                    .drawWithContent {
                        clipRect(right = size.width * fill) {
                            this@drawWithContent.drawContent()
                        }
                    },
                tint = accent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingDateRange(
    startedLabel: String,
    startedValue: String,
    accent: Color,
    onStartedValueChange: (String) -> Unit,
    finishedLabel: String? = null,
    finishedValue: String? = null,
    onFinishedValueChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackingDateRangeItem(
                label = startedLabel,
                value = startedValue,
                accent = accent,
                onValueChange = onStartedValueChange,
                modifier = Modifier.weight(1f),
            )
            if (finishedLabel != null && finishedValue != null && onFinishedValueChange != null) {
                VerticalDivider(
                    modifier = Modifier.size(width = 1.dp, height = 44.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                TrackingDateRangeItem(
                    label = finishedLabel,
                    value = finishedValue,
                    accent = accent,
                    onValueChange = onFinishedValueChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackingDateRangeItem(
    label: String,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    var showPicker by remember(label) { mutableStateOf(false) }
    val selectedMillis = value.toLocalDateOrNull()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    Surface(
        onClick = { showPicker = true },
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value.toTrackingDateLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_date),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                showPicker = false
            }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingDateField(label: String, value: String, accent: Color, onValueChange: (String) -> Unit) {
    var showPicker by remember(label) { mutableStateOf(false) }
    val selectedMillis = value.toLocalDateOrNull()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    OutlinedTextField(
        value = value, onValueChange = { onValueChange(it.take(10)) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotBlank()) IconButton(onClick = { onValueChange("") }) { Icon(Icons.Filled.Close, stringResource(R.string.clear_date), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)) }
                TextButton(onClick = { showPicker = true }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text(stringResource(R.string.pick_date)) }
            }
        }, colors = trackingTextFieldColors(accent), shape = RoundedCornerShape(12.dp),
    )
    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                showPicker = false
            }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun TrackingNotesField(value: String, accent: Color, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.notes_placeholder)) }, colors = trackingTextFieldColors(accent), shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium, maxLines = 6)
}

@Composable
private fun trackingTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, cursorColor = accent)
private fun TrackingStatus.labelResId(): Int = when (this) {
    TrackingStatus.Planned -> R.string.status_planned
    TrackingStatus.InProgress -> R.string.status_in_progress
    TrackingStatus.Completed -> R.string.status_completed
    TrackingStatus.Paused -> R.string.status_paused
    TrackingStatus.Dropped -> R.string.status_dropped
}
private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }
@Composable
@ReadOnlyComposable
private fun statusColor(status: TrackingStatus): Color = when (status) {
    TrackingStatus.Planned -> OmnilogTheme.accents.Planned
    TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
    TrackingStatus.Completed -> OmnilogTheme.accents.Completed
    TrackingStatus.Paused -> OmnilogTheme.accents.Paused
    TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
}
private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
@Composable
private fun String.toTrackingDateLabel(): String = toLocalDateOrNull()
    ?.format(DateTimeFormatter.ofPattern("d MMM ''yy", Locale.getDefault()))
    ?: stringResource(R.string.pick_date)
