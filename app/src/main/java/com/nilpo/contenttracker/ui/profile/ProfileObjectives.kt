package com.nilpo.contenttracker.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.ui.common.ObjectiveProgressCard
import com.nilpo.contenttracker.ui.common.ObjectiveUnitOption
import com.nilpo.contenttracker.ui.common.formatObjectiveNumber
import com.nilpo.contenttracker.ui.common.objectiveAmountStep
import com.nilpo.contenttracker.ui.common.objectiveCountLabel
import com.nilpo.contenttracker.ui.common.objectiveCountOptions
import com.nilpo.contenttracker.ui.common.objectiveDisplayTitle
import com.nilpo.contenttracker.ui.common.objectiveFormatChipLabel
import com.nilpo.contenttracker.ui.common.objectiveFormatOptions
import com.nilpo.contenttracker.ui.common.objectiveOptionForFormat
import com.nilpo.contenttracker.ui.common.objectivePresentation
import com.nilpo.contenttracker.ui.common.objectiveQuickPicks
import com.nilpo.contenttracker.ui.common.objectiveSentenceUnitWords
import com.nilpo.contenttracker.ui.common.objectiveVerb
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
                .map { objectivePresentation(it.objective).title }
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
                color = OmnilogTheme.colors.appInk,
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
                color = OmnilogTheme.colors.appPanel,
                border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
            ) {
                Text(
                    text = "Defineix un objectiu per fer seguiment del que vols aconseguir.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
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
        color = OmnilogTheme.accents.Completed.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, OmnilogTheme.accents.Completed.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = OmnilogTheme.accents.Completed,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (names.size == 1) "Objectiu assolit!" else "Objectius assolits!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                )
                Text(
                    text = names.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Tanca", tint = OmnilogTheme.colors.appMuted)
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
            color = OmnilogTheme.colors.appMuted,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Amaga" else "Mostra",
            tint = OmnilogTheme.colors.appMuted,
        )
    }
}

/** Which fragment of the sentence the docked picker below it is currently editing. */
private enum class EditorSlot { Unit, Amount, Period }

/** Two rows of sentence chips. A floor, not a cap — large font scales still get the room they need. */
private val SentenceHeight = 78.dp

/**
 * The objective editor as a sentence: `Vull llegir 3.000 pàgines durant aquest any`.
 *
 * Each underlined fragment opens its options in a panel docked below the sentence rather than in a
 * popup. The sentence never moves while you edit one part of it, and the panel is free to be as
 * tall as the option list needs — the old dropdowns had to position a [Popup] against a measured
 * trigger, which clipped badly once the system font scale grew (UX-09).
 *
 * There is no separate metric field: [ObjectiveUnitOption] fuses "what to count" with "which
 * format", so the invalid combination the old form warned about cannot be expressed here.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ObjectiveEditorDialog(
    initial: Objective?,
    onDismiss: () -> Unit,
    onSave: (Objective) -> Unit,
) {
    val today = LocalDate.now()
    var option by remember(initial) {
        mutableStateOf(
            initial?.let { ObjectiveUnitOption(it.metric, it.mediaType) }
                ?: ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, MediaType.Book),
        )
    }
    var targetText by remember(initial) { mutableStateOf(initial?.targetValue?.toString().orEmpty()) }
    var period by remember(initial) {
        mutableStateOf(initial?.let { detectPeriodPreset(it.startDate, it.endDate, today) } ?: PeriodPreset.ThisYear)
    }
    var customStart by remember(initial) { mutableStateOf(initial?.startDate ?: today.withDayOfYear(1)) }
    var customEnd by remember(initial) { mutableStateOf(initial?.endDate ?: today.withMonth(12).withDayOfMonth(31)) }
    var pickerTarget by remember(initial) { mutableStateOf<DatePickerTarget?>(null) }
    // A new objective opens on the unit picker because that is the first decision; editing an
    // existing one opens with nothing expanded, so the sentence is readable before it is changed.
    var activeSlot by remember(initial) {
        mutableStateOf<EditorSlot?>(if (initial == null) EditorSlot.Unit else null)
    }

    val targetValue = targetText.toIntOrNull()
    val canSave = targetValue != null && targetValue > 0
    val range = if (period == PeriodPreset.Custom) customStart to customEnd else period.range(today)
    val startDate = range.first
    val endDate = maxOf(range.second, range.first)

    val candidate = Objective(
        id = initial?.id ?: 0,
        name = objectiveDisplayTitle(option.metric, option.mediaType, targetValue ?: 0, option.unit),
        metric = option.metric,
        unit = option.unit,
        mediaType = option.mediaType,
        targetValue = targetValue ?: 0,
        startDate = startDate,
        endDate = endDate,
        createdAtEpochMillis = initial?.createdAtEpochMillis ?: System.currentTimeMillis(),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (initial == null) "Nou objectiu" else "Editar objectiu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )

            // Held to two rows' worth: "episodis d'anime" wraps where "llibres" does not, and
            // letting the sentence grow and shrink shifts everything below it as you change units.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SentenceHeight),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val centered = Modifier.align(Alignment.CenterVertically)
                SentenceWord("Vull ${objectiveVerb(option.mediaType)}", centered)
                SentenceChip(
                    text = targetValue?.let { formatObjectiveNumber(it) } ?: "quants?",
                    isPlaceholder = targetValue == null,
                    isActive = activeSlot == EditorSlot.Amount,
                    onClick = { activeSlot = activeSlot.toggledTo(EditorSlot.Amount) },
                    modifier = centered,
                )
                SentenceChip(
                    text = objectiveSentenceUnitWords(option, targetValue ?: 2),
                    isActive = activeSlot == EditorSlot.Unit,
                    onClick = { activeSlot = activeSlot.toggledTo(EditorSlot.Unit) },
                    modifier = centered,
                )
                SentenceWord("durant", centered)
                SentenceChip(
                    text = period.sentenceLabel(today, customStart, customEnd),
                    isActive = activeSlot == EditorSlot.Period,
                    onClick = { activeSlot = activeSlot.toggledTo(EditorSlot.Period) },
                    modifier = centered,
                )
            }

            when (activeSlot) {
                EditorSlot.Unit -> EditorPanel {
                    // Selecting never moves the open panel: the sentence updates in place and you
                    // close or switch slots yourself. Advancing automatically stole the panel away
                    // mid-thought, right when you might want to change the choice you just made.
                    UnitPickerPanel(selected = option, onSelected = { option = it })
                }
                EditorSlot.Amount -> EditorPanel {
                    AmountPanel(
                        option = option,
                        targetText = targetText,
                        onTargetChange = { targetText = it },
                    )
                }
                EditorSlot.Period -> EditorPanel {
                    PeriodPanel(
                        selected = period,
                        customStart = customStart,
                        customEnd = customEnd,
                        onSelected = { period = it },
                        onPickStart = { pickerTarget = DatePickerTarget.Start },
                        onPickEnd = { pickerTarget = DatePickerTarget.End },
                    )
                }
                null -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel·la") }
                Button(
                    enabled = canSave,
                    onClick = { if (canSave) onSave(candidate) },
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

/** Tapping the slot that is already open closes it, so the sentence can be read unobstructed. */
private fun EditorSlot?.toggledTo(slot: EditorSlot): EditorSlot? = if (this == slot) null else slot

/** Plain connective text between the tappable fragments. */
@Composable
private fun SentenceWord(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = OmnilogTheme.colors.appMuted,
    )
}

/**
 * One editable fragment of the sentence. The dashed underline marks it as tappable without the
 * chrome of a form field; the active fragment inverts so it stays identifiable while its panel is
 * open. Vertical padding rather than a fixed height keeps the text intact at large font scales.
 */
@Composable
private fun SentenceChip(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val background = when {
        isActive -> OmnilogTheme.accents.Dashboard
        isPlaceholder -> OmnilogTheme.colors.appBackground
        else -> OmnilogTheme.accents.Dashboard.copy(alpha = 0.14f)
    }
    val contentColor = when {
        isActive -> OmnilogTheme.colors.appBackground
        isPlaceholder -> OmnilogTheme.colors.appMuted
        else -> OmnilogTheme.accents.Dashboard
    }
    // The underline is drawn behind the text rather than stacked under it in a Column: a
    // fillMaxWidth child would expand to the row's full width and each chip would become its own
    // line instead of flowing inside the sentence.
    val underline = if (isActive) Color.Transparent else contentColor.copy(alpha = 0.55f)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    drawRect(
                        color = underline,
                        topLeft = Offset(0f, size.height - stroke),
                        size = Size(size.width, stroke),
                    )
                }
                .padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

/**
 * Height every slot panel is held to, so switching between format, amount, and period does not
 * resize the sheet under the reader's thumb. Sized to the tallest panel — the period picker with a
 * custom range, whose two date fields sit below its presets. A floor rather than a cap, so nothing
 * clips when the system font scale grows.
 */
private val EditorPanelHeight = 150.dp

/**
 * The region the three slot panels share, docked directly under the sentence.
 *
 * Deliberately has no surface, border, or inset of its own: the chips already carry their own
 * outlines, so a box around them only added a second frame inside the sheet's. Without it the
 * controls read as part of the same form as the sentence, and the height reserved by
 * [EditorPanelHeight] is simply empty sheet rather than a visibly half-filled panel.
 */
@Composable
private fun EditorPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = EditorPanelHeight),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/**
 * The unit choice as two short chip rows — format, then how to count it.
 *
 * A single flat list of all eleven combinations ran about as tall as the whole sheet and pushed the
 * live preview off screen. Splitting the decision also retires the group headers that used to
 * disambiguate `episodis`: series and anime episodes can no longer appear in the same row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.UnitPickerPanel(
    selected: ObjectiveUnitOption,
    onSelected: (ObjectiveUnitOption) -> Unit,
) {
    PanelLabel("Format")
    ChipRow {
        objectiveFormatOptions().forEach { format ->
            ChoiceChip(
                label = objectiveFormatChipLabel(format),
                isSelected = format == selected.mediaType,
                onClick = { onSelected(objectiveOptionForFormat(format, selected.metric)) },
            )
        }
    }

    val countOptions = objectiveCountOptions(selected.mediaType)
    // "Tot" can only be counted one way, so a second row of one chip would be a decision that
    // isn't one. The sentence already shows what was chosen.
    if (countOptions.size > 1) {
        PanelLabel("Què compto")
        ChipRow {
            countOptions.forEach { countOption ->
                ChoiceChip(
                    label = objectiveCountLabel(countOption),
                    isSelected = countOption == selected,
                    onClick = { onSelected(countOption) },
                )
            }
        }
    }
}

// No horizontal inset anywhere below: the controls line up with the sentence above them, which is
// what makes the panel read as part of the form rather than a thing sitting inside it.
@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = OmnilogTheme.colors.appMuted,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** The selectable pill shared by all three panels, so format, amount, and period stay consistent. */
@Composable
private fun ChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) OmnilogTheme.accents.Dashboard.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appLine,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appMuted,
        )
    }
}

/**
 * Target entry. The step and the quick picks both follow the unit — nudging a 5.000-page goal one
 * page at a time would be useless, and `12 · 24 · 40 · 52` are book counts, not page counts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.AmountPanel(
    option: ObjectiveUnitOption,
    targetText: String,
    onTargetChange: (String) -> Unit,
) {
    val step = objectiveAmountStep(option.unit)
    val current = targetText.toIntOrNull() ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            glyph = "−",
            description = "Resta $step",
            enabled = current > step,
            onClick = { onTargetChange((current - step).coerceAtLeast(step).toString()) },
        )
        OutlinedTextField(
            value = targetText,
            onValueChange = { onTargetChange(it.filter(Char::isDigit).take(6)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("0") },
        )
        StepButton(
            glyph = "+",
            description = "Suma $step",
            enabled = true,
            onClick = { onTargetChange((current + step).toString()) },
        )
    }
    ChipRow {
        objectiveQuickPicks(option.unit).forEach { value ->
            ChoiceChip(
                label = formatObjectiveNumber(value),
                isSelected = value == current,
                onClick = { onTargetChange(value.toString()) },
            )
        }
    }
}

/** Uses glyphs rather than icons: the project ships material-icons-core, which has no minus. */
@Composable
private fun StepButton(
    glyph: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appLine
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, tint),
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

/** Period presets, with the custom range's two date fields revealed only when it is selected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.PeriodPanel(
    selected: PeriodPreset,
    customStart: LocalDate,
    customEnd: LocalDate,
    onSelected: (PeriodPreset) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
) {
    ChipRow {
        PeriodPreset.entries.forEach { preset ->
            ChoiceChip(
                label = preset.label(),
                isSelected = preset == selected,
                onClick = { onSelected(preset) },
            )
        }
    }
    if (selected == PeriodPreset.Custom) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateField(
                label = "Inici",
                date = customStart,
                onClick = onPickStart,
                modifier = Modifier.weight(1f),
            )
            DateField(
                label = "Fi",
                date = customEnd,
                onClick = onPickEnd,
                modifier = Modifier.weight(1f),
            )
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

/**
 * The period as it reads inside the sentence — lowercase, and naming the concrete year where the
 * preset label would say "aquest", because `durant el 2026` survives being read months later in a
 * way `durant aquest any` does not.
 */
private fun PeriodPreset.sentenceLabel(
    today: LocalDate,
    customStart: LocalDate,
    customEnd: LocalDate,
): String = when (this) {
    PeriodPreset.ThisMonth -> today.month.getDisplayName(TextStyle.FULL, catalanLocale)
    PeriodPreset.NextMonth -> today.plusMonths(1).month.getDisplayName(TextStyle.FULL, catalanLocale)
    PeriodPreset.ThisYear -> "el ${today.year}"
    PeriodPreset.NextYear -> "el ${today.year + 1}"
    PeriodPreset.Custom -> if (customStart.year == customEnd.year) {
        "${customStart.format(sentenceDayMonth)} – ${customEnd.format(sentenceDayMonth)}"
    } else {
        "${customStart.format(objectiveDateFormatter)} – ${customEnd.format(objectiveDateFormatter)}"
    }
}

// Declared here rather than beside the other formatters below: top-level property initialisers run
// in file order, and this one is read by sentenceLabel above.
private val catalanLocale = Locale("ca")

private val sentenceDayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", catalanLocale)

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
        color = OmnilogTheme.colors.appBackground,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = OmnilogTheme.accents.Dashboard,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = OmnilogTheme.colors.appMuted)
                Text(
                    date.format(objectiveDateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appInk,
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
    DateTimeFormatter.ofPattern("d MMM yyyy", catalanLocale)

private fun LocalDate.toObjectivePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toObjectiveLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
