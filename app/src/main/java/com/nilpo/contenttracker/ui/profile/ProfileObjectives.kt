package com.nilpo.contenttracker.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.ui.common.ObjectiveProgressCard
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileObjectivesSection(
    objectives: List<ObjectiveProgress>,
    onSave: (Objective) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences.from(context) }
    val today = remember { LocalDate.now() }
    var editorObjective by remember { mutableStateOf<Objective?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var pastExpanded by rememberSaveable { mutableStateOf(false) }

    val allObjectives = objectives.filter { it.objective.archivedAtEpochMillis == null }
    val currentObjectives = allObjectives.filter { !it.isExpired(today) }
    val pastObjectives = allObjectives.filter { it.isExpired(today) }

    val celebratedBaseline = remember {
        preferences.getStringSet(ProfilePreferences.CELEBRATED_OBJECTIVES_KEY, emptySet()).orEmpty()
    }
    val newlyCompletedIds = remember(allObjectives) {
        allObjectives
            .filter { it.isComplete }
            .map { it.objective.id.toString() }
            .toSet()
            .minus(celebratedBaseline)
    }
    var celebrationNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(newlyCompletedIds) {
        if (newlyCompletedIds.isNotEmpty()) {
            celebrationNames = allObjectives
                .filter { it.objective.id.toString() in newlyCompletedIds }
                .map { it.objective.name }
            preferences.edit()
                .putStringSet(
                    ProfilePreferences.CELEBRATED_OBJECTIVES_KEY,
                    celebratedBaseline + newlyCompletedIds,
                )
                .apply()
        }
    }

    val editObjective: (ObjectiveProgress) -> Unit = { progress ->
        editorObjective = progress.objective
        showEditor = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Objectius personals",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            TextButton(onClick = { editorObjective = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Afegir")
            }
        }

        AnimatedVisibility(visible = celebrationNames.isNotEmpty()) {
            ObjectiveCelebrationBanner(
                names = celebrationNames,
                onDismiss = { celebrationNames = emptyList() },
            )
        }

        if (allObjectives.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = OmnilogColors.AppPanel,
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
            ) {
                Text(
                    text = "Defineix un objectiu per fer seguiment del que vols aconseguir.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogColors.AppMuted,
                )
            }
        } else {
            currentObjectives.forEach { progress ->
                ObjectiveProgressCard(
                    progress = progress,
                    onEdit = { editObjective(progress) },
                    onDelete = { onDelete(progress.objective.id) },
                )
            }

            if (pastObjectives.isNotEmpty()) {
                PastObjectivesHeader(
                    count = pastObjectives.size,
                    expanded = pastExpanded,
                    onToggle = { pastExpanded = !pastExpanded },
                )
                AnimatedVisibility(visible = pastExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pastObjectives.forEach { progress ->
                            ObjectiveProgressCard(
                                progress = progress,
                                onEdit = { editObjective(progress) },
                                onDelete = { onDelete(progress.objective.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ObjectiveEditorDialog(
            initial = editorObjective,
            onDismiss = { showEditor = false },
            onSave = {
                onSave(it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun ObjectiveCelebrationBanner(
    names: List<String>,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = OmnilogColors.Completed.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, OmnilogColors.Completed.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = OmnilogColors.Completed,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (names.size == 1) "Objectiu assolit!" else "Objectius assolits!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = names.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogColors.AppMuted,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Tanca", tint = OmnilogColors.AppMuted)
            }
        }
    }
}

@Composable
private fun PastObjectivesHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Objectius passats · $count",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = OmnilogColors.AppMuted,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Amaga" else "Mostra",
            tint = OmnilogColors.AppMuted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectiveEditorDialog(
    initial: Objective?,
    onDismiss: () -> Unit,
    onSave: (Objective) -> Unit,
) {
    val today = LocalDate.now()
    var metric by remember(initial) { mutableStateOf(initial?.metric ?: ObjectiveMetric.CompletedTitles) }
    var mediaType by remember(initial) { mutableStateOf(initial?.mediaType) }
    var targetText by remember(initial) { mutableStateOf(initial?.targetValue?.toString().orEmpty()) }
    var period by remember(initial) {
        mutableStateOf(initial?.let { detectPeriodPreset(it.startDate, it.endDate, today) } ?: PeriodPreset.ThisYear)
    }
    var customStart by remember(initial) { mutableStateOf(initial?.startDate ?: today.withDayOfYear(1)) }
    var customEnd by remember(initial) { mutableStateOf(initial?.endDate ?: today.withMonth(12).withDayOfMonth(31)) }
    var pickerTarget by remember(initial) { mutableStateOf<DatePickerTarget?>(null) }

    val effectiveMediaType = if (metric == ObjectiveMetric.CompletedTitles) mediaType else mediaType ?: MediaType.Book
    val unit = if (metric == ObjectiveMetric.CompletedTitles) ObjectiveUnit.Titles else effectiveMediaType.objectiveUnit()
    val range = if (period == PeriodPreset.Custom) customStart to customEnd else period.range(today)
    val startDate = range.first
    val endDate = range.second

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogColors.AppPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initial == null) "Afegir objectiu" else "Editar objectiu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            EditorDropdown(
                label = "Què vols mesurar",
                selected = metric,
                options = ObjectiveMetric.entries,
                optionLabel = { it.label() },
                onSelected = { metric = it },
            )
            EditorDropdown(
                label = "Format",
                selected = mediaType,
                options = formatOptions,
                optionLabel = { it.formatLabel() },
                onSelected = { mediaType = it },
            )
            EditorDropdown(
                label = "Període",
                selected = period,
                options = PeriodPreset.entries,
                optionLabel = { it.label() },
                onSelected = { period = it },
            )
            if (period == PeriodPreset.Custom) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(
                        label = "Inici",
                        date = customStart,
                        onClick = { pickerTarget = DatePickerTarget.Start },
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = "Fi",
                        date = customEnd,
                        onClick = { pickerTarget = DatePickerTarget.End },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it.filter(Char::isDigit) },
                label = { Text("Objectiu en ${unit.label(2)}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel·la") }
                Button(
                    onClick = {
                        val target = targetText.toIntOrNull() ?: return@Button
                        if (target <= 0) return@Button
                        onSave(
                            Objective(
                                id = initial?.id ?: 0,
                                name = "$target ${effectiveMediaType.objectiveMediaLabel()} ${if (metric == ObjectiveMetric.CompletedTitles) "completats" else "registrats"}",
                                metric = metric,
                                unit = unit,
                                mediaType = if (metric == ObjectiveMetric.CompletedTitles) mediaType else effectiveMediaType,
                                targetValue = target,
                                startDate = startDate,
                                endDate = maxOf(endDate, startDate),
                                createdAtEpochMillis = initial?.createdAtEpochMillis ?: System.currentTimeMillis(),
                            ),
                        )
                    },
                ) { Text("Desa") }
            }
        }
    }

    pickerTarget?.let { target ->
        val current = if (target == DatePickerTarget.Start) customStart else customEnd
        ObjectiveDatePickerDialog(
            initialDate = current,
            onDismiss = { pickerTarget = null },
            onConfirm = { picked ->
                when (target) {
                    DatePickerTarget.Start -> {
                        customStart = picked
                        if (customEnd.isBefore(picked)) customEnd = picked
                    }
                    DatePickerTarget.End -> {
                        customEnd = picked
                        if (customStart.isAfter(picked)) customStart = picked
                    }
                }
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun <T> EditorDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "objectiveDropdownChevron",
    )
    val borderColor = if (expanded) OmnilogColors.Dashboard else OmnilogColors.AppLine

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { triggerSize = it.size },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = OmnilogColors.AppBackground,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
                    )
                    Text(
                        text = optionLabel(selected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OmnilogColors.AppInk,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) OmnilogColors.Dashboard else OmnilogColors.AppMuted,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerSize.height + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.width(with(density) { triggerSize.width.toDp() }),
                    shape = RoundedCornerShape(12.dp),
                    color = OmnilogColors.AppPanel,
                    border = BorderStroke(1.dp, OmnilogColors.AppLine),
                    shadowElevation = 10.dp,
                ) {
                    Column {
                        options.forEach { option ->
                            val isSelected = option == selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(option)
                                        expanded = false
                                    }
                                    .background(
                                        if (isSelected) OmnilogColors.Dashboard.copy(alpha = 0.12f)
                                        else Color.Transparent,
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = optionLabel(option),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) OmnilogColors.Dashboard else OmnilogColors.AppInk,
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = OmnilogColors.Dashboard,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DatePickerTarget { Start, End }

private enum class PeriodPreset { ThisMonth, NextMonth, ThisYear, NextYear, Custom }

private fun PeriodPreset.label(): String = when (this) {
    PeriodPreset.ThisMonth -> "Aquest mes"
    PeriodPreset.NextMonth -> "Mes vinent"
    PeriodPreset.ThisYear -> "Aquest any"
    PeriodPreset.NextYear -> "Any vinent"
    PeriodPreset.Custom -> "Personalitzat"
}

private fun PeriodPreset.range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
    PeriodPreset.ThisMonth -> {
        val start = today.withDayOfMonth(1)
        start to start.plusMonths(1).minusDays(1)
    }
    PeriodPreset.NextMonth -> {
        val start = today.withDayOfMonth(1).plusMonths(1)
        start to start.plusMonths(1).minusDays(1)
    }
    PeriodPreset.ThisYear -> today.withDayOfYear(1) to today.withMonth(12).withDayOfMonth(31)
    PeriodPreset.NextYear -> {
        val start = today.plusYears(1).withDayOfYear(1)
        start to start.withMonth(12).withDayOfMonth(31)
    }
    PeriodPreset.Custom -> today.withDayOfYear(1) to today.withMonth(12).withDayOfMonth(31)
}

private fun detectPeriodPreset(
    start: LocalDate,
    end: LocalDate,
    today: LocalDate,
): PeriodPreset = PeriodPreset.entries
    .filter { it != PeriodPreset.Custom }
    .firstOrNull { it.range(today) == (start to end) }
    ?: PeriodPreset.Custom

private fun ObjectiveMetric.label(): String = when (this) {
    ObjectiveMetric.CompletedTitles -> "Completats"
    ObjectiveMetric.ProgressUnits -> "Progrés"
}

private val formatOptions: List<MediaType?> = listOf(null) + MediaType.entries

private fun MediaType?.formatLabel(): String = this?.label() ?: "Tot"

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogColors.AppBackground,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = OmnilogColors.Dashboard,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = OmnilogColors.AppMuted)
                Text(
                    date.format(objectiveDateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogColors.AppInk,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectiveDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toObjectivePickerMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(millis.toObjectiveLocalDate())
                    } ?: onDismiss()
                },
            ) { Text("D'acord") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·la") } },
    ) {
        DatePicker(state = state)
    }
}

private val objectiveDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ca"))

private fun LocalDate.toObjectivePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toObjectiveLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun ObjectiveUnit.label(value: Int): String = when (this) {
    ObjectiveUnit.Titles -> if (value == 1) "títol" else "títols"
    ObjectiveUnit.Pages -> if (value == 1) "pàgina" else "pàgines"
    ObjectiveUnit.Episodes -> if (value == 1) "episodi" else "episodis"
    ObjectiveUnit.Minutes -> if (value == 1) "minut" else "minuts"
    ObjectiveUnit.Hours -> if (value == 1) "hora" else "hores"
}

private fun MediaType?.objectiveUnit(): ObjectiveUnit = when (this) {
    MediaType.Book -> ObjectiveUnit.Pages
    MediaType.Anime, MediaType.TvShow -> ObjectiveUnit.Episodes
    MediaType.Movie -> ObjectiveUnit.Minutes
    MediaType.Game -> ObjectiveUnit.Hours
    null -> ObjectiveUnit.Titles
}

private fun MediaType?.objectiveMediaLabel(): String = when (this) {
    MediaType.Anime -> "anime"
    MediaType.Book -> "llibres"
    MediaType.Movie -> "pel·lícules"
    MediaType.TvShow -> "sèries"
    MediaType.Game -> "jocs"
    null -> "títols"
}

private fun MediaType.label(): String = when (this) {
    MediaType.Anime -> "Anime"
    MediaType.Book -> "Llibres"
    MediaType.Movie -> "Pel·lícules"
    MediaType.TvShow -> "Sèries"
    MediaType.Game -> "Jocs"
}
