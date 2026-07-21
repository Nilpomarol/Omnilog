package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.endsSession
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.common.progressUnitLabel
import com.nilpo.contenttracker.ui.common.TrackingDateRange
import com.nilpo.contenttracker.ui.common.TrackingNotesField
import com.nilpo.contenttracker.ui.common.TrackingProgressField
import com.nilpo.contenttracker.ui.common.TrackingRatingSelector
import com.nilpo.contenttracker.ui.common.TrackingStatusSelector
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────
// Entry point — swaps between read card and full edit screen
// ─────────────────────────────────────────────────────────────

@Composable
fun CurrentSessionSection(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onUpdateSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?) -> Unit,
) {
    var showEditor by rememberSaveable(session.id) { mutableStateOf(false) }

    SessionCard(
        session = session,
        progressTotal = progressTotal,
        mediaType = mediaType,
        accent = accent,
        onEditClick = { showEditor = true },
        onDeleteProgressUpdate = onDeleteProgressUpdate,
        onDeleteStatusEvent = onDeleteStatusEvent,
        onUpdateProgressUpdate = onUpdateProgressUpdate,
    )

    if (showEditor) {
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SessionEditorScreen(
                session = session,
                progressTotal = progressTotal,
                mediaType = mediaType,
                accent = accent,
                onBack = { showEditor = false },
                onSaveSessionDetails = onUpdateSessionDetails,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Read-only session card
// ─────────────────────────────────────────────────────────────

@Composable
private fun SessionCard(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onEditClick: () -> Unit,
    onDeleteProgressUpdate: (Long) -> Unit,
    onDeleteStatusEvent: (Long) -> Unit,
    onUpdateProgressUpdate: (Long, Int, LocalDate?) -> Unit,
) {
    val visualState = session.visualState(accent = accent)
    val progressFraction = session.progressFraction(progressTotal)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, visualState.color.copy(alpha = 0.30f)),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header row: status pill + edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    label = visualState.label,
                    icon = visualState.icon,
                    color = visualState.color,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressHistoryAction(
                        updates = session.progressUpdates,
                        progressTotal = progressTotal,
                        mediaType = mediaType,
                        accent = visualState.color,
                        onDeleteProgressUpdate = onDeleteProgressUpdate,
                        onUpdateProgressUpdate = onUpdateProgressUpdate,
                        statusEvents = session.statusEvents,
                        onDeleteStatusEvent = onDeleteStatusEvent,
                    )
                    FilledTonalIconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            when (session.status) {
                TrackingStatus.Planned -> PlannedSummary(
                    session = session,
                    mediaType = mediaType,
                    color = visualState.color,
                )
                TrackingStatus.InProgress,
                TrackingStatus.Paused,
                    -> ProgressSummary(
                    session = session,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    progressFraction = progressFraction,
                    color = visualState.color,
                )
                TrackingStatus.Completed -> CompletedSummary(
                    session = session,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    color = visualState.color,
                )
                TrackingStatus.Dropped -> DroppedSummary(
                    session = session,
                    progressTotal = progressTotal,
                    mediaType = mediaType,
                    progressFraction = progressFraction,
                    color = visualState.color,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Full-page edit screen
// ─────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SessionEditorScreen(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    titleResId: Int = R.string.edit_current_session,
    onBack: () -> Unit,
    onSaveSessionDetails: (Long, TrackingStatus, Int, Int?, String?, LocalDate?, LocalDate?) -> Unit,
) {
    var draftStatus by rememberSaveable(session.id) { mutableStateOf(session.status) }
    var draftProgressText by rememberSaveable(session.id) {
        mutableStateOf(
            if (session.status == TrackingStatus.Completed && progressTotal != null && progressTotal > 0) {
                progressTotal.toString()
            } else {
                session.progressCurrent.toString()
            },
        )
    }
    var draftRating by rememberSaveable(session.id) { mutableStateOf(session.rating) }
    var draftNotes by rememberSaveable(session.id) { mutableStateOf(session.notes.orEmpty()) }
    var draftStartedAtText by rememberSaveable(session.id) { mutableStateOf(session.startedAt?.toString().orEmpty()) }
    var draftFinishedAtText by rememberSaveable(session.id) { mutableStateOf(session.finishedAt?.toString().orEmpty()) }
    val maxProgress = progressTotal ?: Int.MAX_VALUE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = accent,
                        ),
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            onSaveSessionDetails(
                                session.id,
                                draftStatus,
                                draftProgressText.toIntOrNull()?.coerceIn(0, maxProgress) ?: 0,
                                draftRating,
                                draftNotes.takeIf { it.isNotBlank() },
                                draftStartedAtText.toLocalDateOrNull(),
                                draftFinishedAtText.toLocalDateOrNull(),
                            )
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val stateColor = statusVisualState(status = draftStatus, accent = accent).color
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Status ───────────────────────────────────────────
            EditSectionHeader(title = stringResource(R.string.field_status))
            TrackingStatusSelector(
                selectedStatus = draftStatus,
                accent = stateColor,
                onStatusSelected = { status ->
                    draftStatus = status
                    if (status == TrackingStatus.InProgress && draftStartedAtText.isBlank()) {
                        draftStartedAtText = LocalDate.now().toString()
                    }
                    // Offered, not imposed — the same way the start date is. Filling the field here
                    // rather than defaulting it on save is what keeps "finished, date unknown"
                    // expressible: the date is visible before saving and can be cleared again.
                    if (status.endsSession && draftFinishedAtText.isBlank()) {
                        draftFinishedAtText = LocalDate.now().toString()
                    }
                    if (status == TrackingStatus.Completed && progressTotal != null && progressTotal > 0) {
                        draftProgressText = progressTotal.toString()
                    }
                },
            )

            EditSectionDivider()

            // ── Progress ─────────────────────────────────────────
            TrackingProgressField(
                value = draftProgressText,
                progressTotal = progressTotal,
                mediaType = mediaType,
                label = stringResource(R.string.field_progress),
                accent = stateColor,
                onValueChange = { value -> draftProgressText = value },
            )

            EditSectionDivider()

            // ── Rating ───────────────────────────────────────────
            EditSectionHeader(title = stringResource(R.string.field_rating))
            TrackingRatingSelector(
                currentRating = draftRating,
                accent = stateColor,
                onRatingSelected = { draftRating = it },
            )

            EditSectionDivider()

            EditSectionHeader(title = stringResource(R.string.session_dates))
            TrackingDateRange(
                startedLabel = stringResource(R.string.session_started_label),
                startedValue = draftStartedAtText,
                accent = stateColor,
                onStartedValueChange = { draftStartedAtText = it },
                finishedLabel = stringResource(R.string.session_finished_label),
                finishedValue = draftFinishedAtText,
                onFinishedValueChange = { draftFinishedAtText = it },
            )

            EditSectionDivider()

            // ── Notes ────────────────────────────────────────────
            EditSectionHeader(title = stringResource(R.string.field_notes))
            TrackingNotesField(
                value = draftNotes,
                accent = stateColor,
                onValueChange = { draftNotes = it },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmnilogTheme.colors.appMuted,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EditSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}

// ── Status selector ──────────────────────────────────────────

@Composable
private fun StatusSelectorRow(
    selectedStatus: TrackingStatus,
    accent: Color,
    onStatusSelected: (TrackingStatus) -> Unit,
) {
    val statuses = TrackingStatus.entries.toList()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(statuses) { status ->
            val isSelected = status == selectedStatus
            val visualState = statusVisualState(status = status, accent = accent)
            Surface(
                onClick = { onStatusSelected(status) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) visualState.color.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) visualState.color.copy(alpha = 0.70f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = visualState.icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isSelected) visualState.color
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                    Text(
                        text = visualState.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) visualState.color
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

// ── Progress editor ──────────────────────────────────────────

@Composable
private fun ProgressEditorRow(
    progressCurrent: Int,
    progressTotal: Int?,
    mediaType: MediaType,
    accent: Color,
    onProgressChange: (Int) -> Unit,
) {
    var rawInput by rememberSaveable(progressCurrent, progressTotal) {
        mutableStateOf(progressCurrent.toString())
    }
    val current = rawInput.toIntOrNull() ?: progressCurrent

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Decrement
            FilledTonalIconButton(
                onClick = {
                    val next = (current - 1).coerceAtLeast(0)
                    rawInput = next.toString()
                    onProgressChange(next)
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                ),
            ) {
                Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Light)
            }

            // Number input + context label
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        rawInput = digits
                        digits.toIntOrNull()?.let {
                            val max = progressTotal ?: Int.MAX_VALUE
                            onProgressChange(it.coerceIn(0, max))
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = accent,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = accent,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (progressTotal != null && progressTotal > 0) {
                    Text(
                        text = "de $progressTotal ${progressUnitLabel(mediaType, progressTotal)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }

            // Increment
            FilledTonalIconButton(
                onClick = {
                    val max = progressTotal ?: Int.MAX_VALUE
                    val next = (current + 1).coerceAtMost(max)
                    rawInput = next.toString()
                    onProgressChange(next)
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                ),
            ) {
                Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

// ── Rating editor ─────────────────────────────────────────────

@Composable
private fun RatingEditorRow(
    currentRating: Int?,
    accent: Color,
    onRatingSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            (1..10).forEach { n ->
                val isSelected = n == currentRating
                val isActive = currentRating != null && n <= currentRating
                Surface(
                    onClick = { onRatingSelected(if (isSelected) null else n) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) accent.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isActive) accent.copy(alpha = if (isSelected) 0.80f else 0.42f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                    ),
                ) {
                    Text(
                        text = n.toString(),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) accent
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }

        // Tap selected again = clear; also show explicit clear link
        if (currentRating != null) {
            Text(
                text = stringResource(R.string.rating_clear),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.70f),
                modifier = Modifier
                    .clickable { onRatingSelected(null) }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

// ── Notes editor ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    var showPicker by rememberSaveable(label) { mutableStateOf(false) }
    val selectedMillis = value.toLocalDateOrNull()
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(10)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        placeholder = { Text(text = "YYYY-MM-DD") },
        singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.clear_date),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                        )
                    }
                }
                TextButton(onClick = { showPicker = true }) {
                    Text(text = stringResource(R.string.pick_date))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = accent,
        ),
        shape = RoundedCornerShape(12.dp),
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .toString(),
                            )
                        }
                        showPicker = false
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun NotesEditorField(
    currentNotes: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = currentNotes,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        placeholder = {
            Text(
                text = stringResource(R.string.notes_placeholder),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = accent,
        ),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        maxLines = 6,
    )
}

// ─────────────────────────────────────────────────────────────
// Read card sub-composables
// ─────────────────────────────────────────────────────────────

@Composable
private fun PlannedSummary(session: TrackingSession, mediaType: MediaType, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.session_planned_prompt),
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        QuickHintRow(
            values = listOf(
                stringResource(R.string.status_in_progress),
                stringResource(R.string.field_progress),
                stringResource(R.string.field_rating),
            ),
            color = color,
        )
        InlineRatingDisplay(rating = session.rating, color = color)
        SessionDates(session = session, mediaType = mediaType)
    }
}

@Composable
private fun ProgressSummary(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    progressFraction: Float,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = progressText(session, progressTotal, mediaType),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (progressTotal != null && progressTotal > 0) {
                Text(
                    text = "· ${(progressFraction * 100).toInt()}%",
                    color = color.copy(alpha = 0.70f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (progressTotal != null && progressTotal > 0) {
            ThickProgressBar(fraction = progressFraction, color = color)
        }
        InlineRatingDisplay(rating = session.rating, color = color)
        SessionDates(
            session = session,
            mediaType = mediaType,
            highlightedStartedAt = true,
            highlightColor = color,
        )
        session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CompletedSummary(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RatingDisplay(rating = session.rating, color = color)
        if (progressTotal != null && progressTotal > 0) {
            ThickProgressBar(fraction = 1f, color = color)
        }
        Text(
            text = progressText(session, progressTotal, mediaType),
            color = OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        SessionDates(session = session, mediaType = mediaType)
    }
}

@Composable
private fun DroppedSummary(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
    progressFraction: Float,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (session.rating != null) {
            RatingDisplay(rating = session.rating, color = color)
        } else {
            Text(
                text = progressText(session, progressTotal, mediaType),
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        // Faded bar signals "abandoned" vs a solid completed bar
        if (progressTotal != null && progressTotal > 0) {
            ThickProgressBar(fraction = progressFraction, color = color.copy(alpha = 0.40f))
        }
        if (session.rating != null) {
            Text(
                text = progressText(session, progressTotal, mediaType),
                color = OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SessionDates(session = session, mediaType = mediaType)
    }
}

// ─────────────────────────────────────────────────────────────
// Shared display components
// ─────────────────────────────────────────────────────────────

@Composable
private fun ThickProgressBar(fraction: Float, color: Color) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "progressBar",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun RatingDisplay(rating: Int?, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.field_rating),
                color = if (rating != null) color
                else OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (rating != null) {
                    stringResource(R.string.rating_value, rating)
                } else {
                    stringResource(R.string.rating_empty)
                },
                color = if (rating != null) color
                else OmnilogTheme.colors.appMuted,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val clampedRating = rating?.coerceIn(0, 10) ?: 0
            repeat(10) { index ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (index < clampedRating) {
                        color
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f)
                    },
                )
            }
        }
    }
}

@Composable
private fun InlineRatingDisplay(rating: Int?, color: Color) {
    if (rating == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.rating_value, rating),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
        ) {
            repeat(10) { index ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (index < rating.coerceIn(0, 10)) {
                        color
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
                    },
                )
            }
        }
    }
}

@Composable
private fun SessionDates(
    session: TrackingSession,
    mediaType: MediaType,
    highlightedStartedAt: Boolean = false,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
) {
    val startedAt = session.startedAt?.let { stringResource(R.string.session_started_at, it.formatDate()) }
    val finishedAt = session.finishedAt?.let {
        stringResource(
            if (startedAt == null) mediaType.finishedOnlyDateLabelRes() else R.string.session_finished_at,
            it.formatDate(),
        )
    }
    val finishedWithoutDate = if (session.status == TrackingStatus.Completed && session.finishedAt == null) {
        stringResource(R.string.session_finished_unknown)
    } else {
        null
    }
    val updatedAt = if (finishedAt == null && finishedWithoutDate == null) {
        session.updatedDate()?.let { stringResource(R.string.session_updated_at, it.formatDate()) }
    } else {
        null
    }
    if (startedAt == null && finishedAt == null && finishedWithoutDate == null && updatedAt == null) return

    val primaryDateColor = if (highlightedStartedAt && startedAt != null) {
        highlightColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        startedAt?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlightedStartedAt) FontWeight.Bold else FontWeight.SemiBold,
                color = primaryDateColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        finishedAt?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        finishedWithoutDate?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        updatedAt?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickHintRow(values: List<String>, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEachIndexed { index, value ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.12f),
                contentColor = color,
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (index < values.lastIndex) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.42f),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, icon: ImageVector, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.32f)),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Visual state helpers
// ─────────────────────────────────────────────────────────────

private data class SessionVisualState(
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

@Composable
private fun TrackingSession.visualState(accent: Color): SessionVisualState =
    statusVisualState(status = status, accent = accent)

// Extracted so both the card and the status selector share the same mapping
@Composable
private fun statusVisualState(status: TrackingStatus, accent: Color): SessionVisualState =
    when (status) {
        TrackingStatus.Planned -> SessionVisualState(
            label = stringResource(R.string.status_planned),
            icon = Icons.Filled.Star,
            color = OmnilogTheme.accents.Planned,
        )
        TrackingStatus.InProgress -> SessionVisualState(
            label = stringResource(R.string.status_in_progress),
            icon = Icons.Filled.PlayArrow,
            color = OmnilogTheme.accents.InProgress,
        )
        TrackingStatus.Completed -> SessionVisualState(
            label = stringResource(R.string.status_completed),
            icon = Icons.Filled.CheckCircle,
            color = OmnilogTheme.accents.Completed,
        )
        TrackingStatus.Paused -> SessionVisualState(
            label = stringResource(R.string.status_paused),
            icon = Icons.Filled.Edit,
            color = OmnilogTheme.accents.Paused,
        )
        TrackingStatus.Dropped -> SessionVisualState(
            label = stringResource(R.string.status_dropped),
            icon = Icons.Filled.Close,
            color = OmnilogTheme.accents.Dropped,
        )
    }

// ─────────────────────────────────────────────────────────────
// Pure helpers (no Compose)
// ─────────────────────────────────────────────────────────────

@Composable
private fun progressText(
    session: TrackingSession,
    progressTotal: Int?,
    mediaType: MediaType,
): String {
    val unit = progressUnitLabel(mediaType = mediaType, value = progressTotal ?: session.progressCurrent)
    return if (progressTotal != null && progressTotal > 0) {
        "${session.progressCurrent.coerceAtMost(progressTotal)} / $progressTotal $unit"
    } else {
        "${session.progressCurrent} $unit"
    }
}

private fun TrackingSession.progressFraction(progressTotal: Int?): Float {
    if (progressTotal == null || progressTotal <= 0) return 0f
    return progressCurrent.toFloat().div(progressTotal.toFloat()).coerceIn(0f, 1f)
}

private fun TrackingSession.updatedDate(): LocalDate? {
    if (updatedAtEpochMillis <= 0L) return null
    return Instant.ofEpochMilli(updatedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun MediaType.finishedOnlyDateLabelRes(): Int =
    when (this) {
        MediaType.Book -> R.string.session_finished_read_at
        MediaType.Game -> R.string.session_finished_played_at
        MediaType.Anime,
        MediaType.Movie,
        MediaType.TvShow,
            -> R.string.session_finished_watched_at
    }

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun String.toLocalDateOrNull(): LocalDate? =
    trim().takeIf { it.isNotBlank() }?.let { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
