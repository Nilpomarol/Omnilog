package com.nilpo.contenttracker.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.ui.common.ObjectiveProgressRow
import com.nilpo.contenttracker.ui.common.ObjectiveUnitOption
import com.nilpo.contenttracker.ui.common.formatObjectiveNumber
import com.nilpo.contenttracker.ui.common.objectiveAccent
import com.nilpo.contenttracker.ui.common.objectiveAmountStep
import com.nilpo.contenttracker.ui.common.objectiveCountLabel
import com.nilpo.contenttracker.ui.common.objectiveCountOptions
import com.nilpo.contenttracker.ui.common.objectiveDisplayTitle
import com.nilpo.contenttracker.ui.common.objectiveFormatChipLabel
import com.nilpo.contenttracker.ui.common.objectiveFormatOptions
import com.nilpo.contenttracker.ui.common.objectiveOptionForFormat
import com.nilpo.contenttracker.ui.common.objectiveQuickPicks
import com.nilpo.contenttracker.ui.common.objectiveSentenceTitle
import com.nilpo.contenttracker.ui.common.objectiveSentenceUnitWords
import com.nilpo.contenttracker.ui.common.objectiveVerb
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.detail.DetailDisclosureRow
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.home.EditorialSheet
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The profile's objectives as an editorial section: a serif heading with the one action that adds to
 * it, the objectives still running as list rows, and the finished periods folded away below. Every
 * row opens its editor; deleting lives there, behind its own confirmation.
 */
@Composable
fun ProfileObjectivesSection(
    objectives: List<ObjectiveProgress>,
    /** The objective to bring into view, if any. */
    focusObjectiveId: Long? = null,
    onSave: (Objective) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences.from(context) }
    val today = remember { LocalDate.now() }
    var editorObjective by remember { mutableStateOf<Objective?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var pendingDeletion by remember { mutableStateOf<Objective?>(null) }

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
                .map { objectiveSentenceTitle(it.objective) }
            preferences.edit()
                .putStringSet(
                    ProfilePreferences.CELEBRATED_OBJECTIVES_KEY,
                    celebratedBaseline + newlyCompletedIds,
                )
                .apply()
        }
    }

    val openEditor: (Objective?) -> Unit = { objective ->
        editorObjective = objective
        showEditor = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = DetailGutter),
            color = OmnilogTheme.colors.appLine,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DetailGutter, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailSectionTitle(text = "Objectius", modifier = Modifier.weight(1f))
            TextButton(onClick = { openEditor(null) }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = OmnilogTheme.accents.Dashboard,
                )
                Text(
                    text = "Nou objectiu",
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.accents.Dashboard,
                )
            }
        }

        AnimatedVisibility(visible = celebrationNames.isNotEmpty()) {
            ObjectiveCelebration(
                names = celebrationNames,
                onDismiss = { celebrationNames = emptyList() },
            )
        }

        if (currentObjectives.isEmpty()) {
            Text(
                text = if (pastObjectives.isEmpty()) {
                    "Posa't una fita — llibres per llegir, sèries per veure, hores de joc — i en seguiràs el ritme aquí."
                } else {
                    "Cap objectiu en curs."
                },
                modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 4.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        ObjectiveRows(
            objectives = currentObjectives,
            focusObjectiveId = focusObjectiveId,
            today = today,
            onClick = { openEditor(it.objective) },
        )

        if (pastObjectives.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = DetailGutter),
                color = OmnilogTheme.colors.appLine,
            )
            DetailDisclosureRow(
                icon = rememberVectorPainter(Icons.Filled.DateRange),
                title = "Passats",
                summary = "${pastObjectives.count { it.isComplete }} de ${pastObjectives.size} assolits",
            ) {
                ObjectiveRows(
                    objectives = pastObjectives,
                    focusObjectiveId = focusObjectiveId,
                    today = today,
                    onClick = { openEditor(it.objective) },
                )
            }
        }
    }

    if (showEditor) {
        ObjectiveEditorSheet(
            initial = editorObjective,
            onDismiss = { showEditor = false },
            onSave = {
                onSave(it)
                showEditor = false
            },
            onDelete = {
                pendingDeletion = editorObjective
                showEditor = false
            },
        )
    }

    pendingDeletion?.let { objective ->
        EditorialSheet(
            title = "Eliminar l'objectiu",
            message = "«${objectiveSentenceTitle(objective)}» s'esborrarà del perfil. No es pot desfer.",
            confirmText = "Elimina",
            destructive = true,
            onDismiss = { pendingDeletion = null },
            onConfirm = {
                onDelete(objective.id)
                pendingDeletion = null
            },
        )
    }
}

/** The objectives as list rows, spaced like the library's. */
@Composable
private fun ObjectiveRows(
    objectives: List<ObjectiveProgress>,
    focusObjectiveId: Long?,
    today: LocalDate,
    onClick: (ObjectiveProgress) -> Unit,
) {
    objectives.forEach { progress ->
        val focused = progress.objective.id == focusObjectiveId
        val bringIntoView = remember { BringIntoViewRequester() }
        if (focused) {
            LaunchedEffect(focusObjectiveId) { bringIntoView.bringIntoView() }
        }
        ObjectiveProgressRow(
            progress = progress,
            today = today,
            onClick = { onClick(progress) },
            modifier = Modifier
                .bringIntoViewRequester(bringIntoView)
                .padding(start = DetailGutter, end = DetailGutter, top = 12.dp),
        )
    }
}

/** A reached objective, announced once in the completed colour and dismissed for good. */
@Composable
private fun ObjectiveCelebration(
    names: List<String>,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DetailGutter, end = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = OmnilogTheme.accents.Completed,
            modifier = Modifier.size(26.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (names.size == 1) "Objectiu assolit" else "Objectius assolits",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifFontFamily),
                color = OmnilogTheme.accents.Completed,
            )
            Text(
                text = names.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Tanca", tint = OmnilogTheme.colors.appMuted)
        }
    }
}

/** Which fragment of the sentence the docked picker below it is currently editing. */
private enum class EditorSlot { Unit, Amount, Period }

/** Two rows of serif sentence. A floor, not a cap — large font scales still get the room they need. */
private val SentenceHeight = 96.dp

/**
 * The objective editor as a sentence: `Vull llegir 3.000 pàgines durant el 2026`.
 *
 * Each underlined fragment opens its options in a panel docked below the sentence rather than in a
 * popup. The sentence never moves while you edit one part of it, and the panel is free to be as
 * tall as the option list needs — the old dropdowns had to position a popup against a measured
 * trigger, which clipped badly once the system font scale grew (UX-09).
 *
 * There is no separate metric field: [ObjectiveUnitOption] fuses "what to count" with "which
 * format", so the invalid combination the old form warned about cannot be expressed here. The sheet
 * takes the colour of the format being counted, so the choice shows before it is read.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObjectiveEditorSheet(
    initial: Objective?,
    onDismiss: () -> Unit,
    onSave: (Objective) -> Unit,
    onDelete: () -> Unit,
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
    val accent = option.mediaType.objectiveAccent()

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

    EditorialSheet(
        title = if (initial == null) "Nou objectiu" else "Edita l'objectiu",
        confirmText = if (initial == null) "Crea l'objectiu" else "Desa",
        confirmEnabled = canSave,
        accent = accent,
        // The sentence is the headline here, so the sheet names itself in a small label above it.
        titleAsLabel = true,
        onDismiss = onDismiss,
        onConfirm = { if (canSave) onSave(candidate) },
    ) {
        // Held to two rows' worth: "episodis d'anime" wraps where "llibres" does not, and
        // letting the sentence grow and shrink shifts everything below it as you change units.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SentenceHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val centered = Modifier.align(Alignment.CenterVertically)
            SentenceWord("Vull ${objectiveVerb(option.mediaType)}", centered)
            SentenceChip(
                text = targetValue?.let { formatObjectiveNumber(it) } ?: "quants?",
                accent = accent,
                isPlaceholder = targetValue == null,
                isActive = activeSlot == EditorSlot.Amount,
                onClick = { activeSlot = activeSlot.toggledTo(EditorSlot.Amount) },
                modifier = centered,
            )
            SentenceChip(
                text = objectiveSentenceUnitWords(option, targetValue ?: 2),
                accent = accent,
                isActive = activeSlot == EditorSlot.Unit,
                onClick = { activeSlot = activeSlot.toggledTo(EditorSlot.Unit) },
                modifier = centered,
            )
            SentenceWord("durant", centered)
            SentenceChip(
                text = period.sentenceLabel(today, customStart, customEnd),
                accent = accent,
                isActive = activeSlot == EditorSlot.Period,
                onClick = { activeSlot = activeSlot.toggledTo(EditorSlot.Period) },
                modifier = centered,
            )
        }

        EditorPanel {
            when (activeSlot) {
                // Selecting never moves the open panel: the sentence updates in place and you
                // close or switch slots yourself. Advancing automatically stole the panel away
                // mid-thought, right when you might want to change the choice you just made.
                EditorSlot.Unit -> UnitPickerPanel(selected = option, accent = accent, onSelected = { option = it })
                EditorSlot.Amount -> AmountPanel(
                    option = option,
                    accent = accent,
                    targetText = targetText,
                    onTargetChange = { targetText = it },
                )
                EditorSlot.Period -> PeriodPanel(
                    selected = period,
                    accent = accent,
                    customStart = customStart,
                    customEnd = customEnd,
                    onSelected = { period = it },
                    onPickStart = { pickerTarget = DatePickerTarget.Start },
                    onPickEnd = { pickerTarget = DatePickerTarget.End },
                )
                null -> Text(
                    text = "Toca una part subratllada de la frase per canviar-la.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }

        if (initial != null) {
            TextButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Elimina l'objectiu",
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
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

/** The sentence's type: the serif, set large, since it is the sheet's headline. */
private val SentenceStyle
    @Composable get() = MaterialTheme.typography.headlineSmall.copy(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Normal,
    )

/** Plain connective text between the tappable fragments. */
@Composable
private fun SentenceWord(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = SentenceStyle,
        color = OmnilogTheme.colors.appInk,
    )
}

/**
 * One editable fragment of the sentence: the words in the accent over an accent underline, marked as
 * tappable without the chrome of a form field. The open fragment takes a tint so it stays identifiable
 * while its panel shows. Vertical padding rather than a fixed height keeps the text intact at large
 * font scales.
 */
@Composable
private fun SentenceChip(
    text: String,
    accent: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val contentColor = if (isPlaceholder && !isActive) OmnilogTheme.colors.appMuted else accent
    val underline = contentColor.copy(alpha = if (isActive) 1f else 0.5f)
    val shape = RoundedCornerShape(6.dp)
    // The underline is drawn behind the text rather than stacked under it in a Column: a
    // fillMaxWidth child would expand to the row's full width and each chip would become its own
    // line instead of flowing inside the sentence.
    Text(
        text = text,
        modifier = modifier
            .clip(shape)
            .background(if (isActive) accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(
                    color = underline,
                    topLeft = Offset(0f, size.height - stroke),
                    size = Size(size.width, stroke),
                )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = SentenceStyle,
        color = contentColor,
    )
}

/**
 * Height the slot panel is held to, so switching between format, amount, and period does not resize
 * the sheet under the reader's thumb. Sized to the tallest panel — the period picker with a custom
 * range, whose two date fields sit below its presets. A floor rather than a cap, so nothing clips when
 * the system font scale grows.
 */
private val EditorPanelHeight = 156.dp

/**
 * The region the three slot panels share, docked under the sentence's hairline. No surface of its
 * own: the choices already carry their outlines, and a box around them would be a frame inside the
 * sheet's.
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
 * The unit choice as two short rows — format, then how to count it.
 *
 * A single flat list of all eleven combinations ran about as tall as the whole sheet and pushed the
 * sentence off screen. Splitting the decision also retires the group headers that used to
 * disambiguate `episodis`: series and anime episodes can no longer appear in the same row.
 */
@Composable
private fun ColumnScope.UnitPickerPanel(
    selected: ObjectiveUnitOption,
    accent: Color,
    onSelected: (ObjectiveUnitOption) -> Unit,
) {
    PanelLabel("Format")
    ChipRow {
        objectiveFormatOptions().forEach { format ->
            ChoiceChip(
                label = objectiveFormatChipLabel(format),
                accent = accent,
                isSelected = format == selected.mediaType,
                onClick = { onSelected(objectiveOptionForFormat(format, selected.metric)) },
            )
        }
    }

    val countOptions = objectiveCountOptions(selected.mediaType)
    // "Tot" can only be counted one way, so a second row of one choice would be a decision that
    // isn't one. The sentence already shows what was chosen.
    if (countOptions.size > 1) {
        PanelLabel("Què compto")
        ChipRow {
            countOptions.forEach { countOption ->
                ChoiceChip(
                    label = objectiveCountLabel(countOption),
                    accent = accent,
                    isSelected = countOption == selected,
                    onClick = { onSelected(countOption) },
                )
            }
        }
    }
}

/** The small spaced capitals the editorial headers use for their overline. */
@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** The selectable tile shared by all three panels, tinted like the collection page's position tiles. */
@Composable
private fun ChoiceChip(
    label: String,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        modifier = Modifier
            .clip(shape)
            .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else OmnilogTheme.colors.appLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (isSelected) accent else OmnilogTheme.colors.appInk,
    )
}

/**
 * Target entry. The step and the quick picks both follow the unit — nudging a 5.000-page goal one
 * page at a time would be useless, and `12 · 24 · 40 · 52` are book counts, not page counts.
 */
@Composable
private fun ColumnScope.AmountPanel(
    option: ObjectiveUnitOption,
    accent: Color,
    targetText: String,
    onTargetChange: (String) -> Unit,
) {
    val step = objectiveAmountStep(option.unit)
    val current = targetText.toIntOrNull() ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            glyph = "−",
            accent = accent,
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
            textStyle = SentenceStyle,
            shape = RoundedCornerShape(12.dp),
            colors = omnilogModalTextFieldColors(accent),
        )
        StepButton(
            glyph = "+",
            accent = accent,
            description = "Suma $step",
            enabled = true,
            onClick = { onTargetChange((current + step).toString()) },
        )
    }
    ChipRow {
        objectiveQuickPicks(option.unit).forEach { value ->
            ChoiceChip(
                label = formatObjectiveNumber(value),
                accent = accent,
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
    accent: Color,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) accent else OmnilogTheme.colors.appLine
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, tint),
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(52.dp)) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleLarge,
                color = tint,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

/** Period presets, with the custom range's two date fields revealed only when it is selected. */
@Composable
private fun ColumnScope.PeriodPanel(
    selected: PeriodPreset,
    accent: Color,
    customStart: LocalDate,
    customEnd: LocalDate,
    onSelected: (PeriodPreset) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
) {
    PanelLabel("Període")
    ChipRow {
        PeriodPreset.entries.forEach { preset ->
            ChoiceChip(
                label = preset.label(),
                accent = accent,
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
                accent = accent,
                date = customStart,
                onClick = onPickStart,
                modifier = Modifier.weight(1f),
            )
            DateField(
                label = "Fi",
                accent = accent,
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
// in file order, and this one is read by the formatters that follow.
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
    accent: Color,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = OmnilogTheme.colors.appMuted)
                Text(
                    text = date.format(objectiveDateFormatter),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SerifFontFamily),
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
