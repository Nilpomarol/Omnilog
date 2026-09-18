package com.nilpo.contenttracker.ui.detail

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.ui.add.FloatingPrimaryAction
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.OmnilogAnchoredDropdown
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

/**
 * The item record, editable, laid out like the detail page it edits.
 *
 * The cover sits beside a serif title, and every field is a label over its value over a hairline —
 * the detail page's ruled sheet, not a stack of boxed inputs. The rule turns the accent while its
 * field has focus, and a label turns the accent once its value differs from the record, so a long
 * form still shows at a glance what is about to be saved.
 *
 * Two things are structural rather than cosmetic. Lists that the detail page draws as lists are
 * edited as lists: genres are pills and every credited person is a row, not a comma-separated line
 * in a text box. And each credit row keeps the [MediaCredit] it came from, so renaming a performer
 * does not discard their character and portrait.
 *
 * Saving marks every changed field as a local override, which protects it from provider refreshes.
 * The form therefore only saves once something actually differs, and the button says why when it
 * cannot. A genre or name still sitting in its entry field counts as typed: it is saved with the rest
 * rather than silently dropped for want of a tap on +.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ItemDetailsEditor(
    item: MediaItem,
    credits: List<MediaCredit>,
    accent: Color,
    onDismiss: () -> Unit,
    onSaveMetadata: (
        title: String,
        originalTitle: String?,
        releaseYear: Int?,
        language: String?,
        progressTotal: Int?,
        genres: List<String>,
        creators: List<String>,
        credits: List<MediaCredit>,
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
        steamAppId: String?,
    ) -> Unit,
) {
    val primaryRole = item.type.groupCreatorRole()
    val isGame = item.type == MediaType.Game

    val initialLanguage = ItemLanguage.normalize(item.language) ?: ItemLanguage.Original
    val initialGenres = item.genres.joinToString(GenreSeparator)
    val initialSynopsis = plainSynopsis(item.synopsis).orEmpty()

    // The credits this item already carries, per role, in provider order. A draft points back into
    // this list by index, which is what lets a renamed person keep their character and portrait.
    val creditsByRole = remember(credits, item.creators, primaryRole) {
        MediaCreditRole.entries.associateWith { role ->
            credits.filter { it.roleType == role }
        }
    }
    val initialDrafts = remember(creditsByRole, item.creators, primaryRole) {
        MediaCreditRole.entries.associateWith { role ->
            val roleCredits = creditsByRole.getValue(role)
            when {
                roleCredits.isNotEmpty() ->
                    roleCredits.mapIndexed { index, credit -> CreditDraft(index, credit.personName) }
                role == primaryRole -> item.creators.map { CreditDraft(NoSourceCredit, it) }
                else -> emptyList()
            }.encodeCreditDrafts()
        }
    }

    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var originalTitle by rememberSaveable(item.id) { mutableStateOf(item.originalTitle.orEmpty()) }
    var releaseYearText by rememberSaveable(item.id) { mutableStateOf(item.releaseYear?.toString().orEmpty()) }
    var language by rememberSaveable(item.id) { mutableStateOf(initialLanguage) }
    var totalText by rememberSaveable(item.id) { mutableStateOf(item.progressTotal?.toString().orEmpty()) }
    var genresText by rememberSaveable(item.id) { mutableStateOf(initialGenres) }
    var genreDraft by rememberSaveable(item.id) { mutableStateOf("") }
    var coverUrl by rememberSaveable(item.id) { mutableStateOf(item.coverUrl.orEmpty()) }
    var sourceUrl by rememberSaveable(item.id) { mutableStateOf(item.sourceUrl.orEmpty()) }
    var steamAppId by rememberSaveable(item.id) { mutableStateOf(item.steamAppId.orEmpty()) }
    var synopsis by rememberSaveable(item.id) { mutableStateOf(initialSynopsis) }
    val creditTexts = MediaCreditRole.entries.associateWith { role ->
        rememberSaveable(item.id, role.name) { mutableStateOf(initialDrafts.getValue(role)) }
    }
    val newNames = MediaCreditRole.entries.associateWith { role ->
        rememberSaveable(item.id, "new-${role.name}") { mutableStateOf("") }
    }
    // Only the roles that hold someone are drawn, plus the ones added here; eight empty lists would
    // bury the one that matters.
    var shownRoles by rememberSaveable(item.id) {
        mutableStateOf(
            MediaCreditRole.entries
                .filter { it == primaryRole || initialDrafts.getValue(it).isNotEmpty() }
                .joinToString(RoleSeparator) { it.name },
        )
    }
    val visibleRoles = shownRoles.split(RoleSeparator).mapNotNull { name ->
        MediaCreditRole.entries.firstOrNull { it.name == name }
    }

    var editingCover by rememberSaveable(item.id) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(item.id) { mutableStateOf(false) }

    val genres = (genresText.toMetadataList() + genreDraft.trim())
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
    val roleDrafts = MediaCreditRole.entries.associateWith { role ->
        val pending = newNames.getValue(role).value.trim()
        val drafts = creditTexts.getValue(role).value.decodeCreditDrafts()
        if (pending.isBlank()) drafts else drafts + CreditDraft(NoSourceCredit, pending)
    }
    val rolesChanged = MediaCreditRole.entries.filter {
        roleDrafts.getValue(it).encodeCreditDrafts() != initialDrafts.getValue(it)
    }.toSet()

    val titleChanged = title != item.title
    val originalTitleChanged = originalTitle != item.originalTitle.orEmpty()
    val yearChanged = releaseYearText != item.releaseYear?.toString().orEmpty()
    val languageChanged = language != initialLanguage
    val totalChanged = totalText != item.progressTotal?.toString().orEmpty()
    val genresChanged = genres.joinToString(GenreSeparator) != initialGenres
    val coverChanged = coverUrl != item.coverUrl.orEmpty()
    val sourceChanged = sourceUrl != item.sourceUrl.orEmpty()
    val steamChanged = steamAppId != item.steamAppId.orEmpty()
    val synopsisChanged = synopsis != initialSynopsis
    val hasChanges = titleChanged || originalTitleChanged || yearChanged || languageChanged ||
        totalChanged || genresChanged || coverChanged || sourceChanged || steamChanged ||
        synopsisChanged || rolesChanged.isNotEmpty()

    val saveBlockedReason = when {
        title.isBlank() -> stringResource(R.string.editor_save_needs_title)
        !hasChanges -> stringResource(R.string.editor_save_no_changes)
        else -> null
    }

    val requestDismiss = { if (hasChanges) confirmDiscard = true else onDismiss() }

    BackHandler(enabled = true, onBack = requestDismiss)

    if (confirmDiscard) {
        OmnilogAlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = stringResource(R.string.editor_unsaved_title),
            text = { Text(text = stringResource(R.string.editor_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = requestDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = OmnilogTheme.colors.appInk,
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.edit_metadata_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = OmnilogTheme.colors.appInk,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OmnilogTheme.colors.appBackground),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DetailGutter, vertical = 12.dp),
            ) {
                FloatingPrimaryAction(
                    text = saveBlockedReason ?: stringResource(R.string.editor_save),
                    enabled = saveBlockedReason == null,
                    onClick = {
                        val editedCredits = MediaCreditRole.entries.flatMap { role ->
                            roleDrafts.getValue(role).toCredits(role, creditsByRole.getValue(role))
                        }
                        onSaveMetadata(
                            title.trim(),
                            originalTitle.trim().takeIf { it.isNotBlank() },
                            releaseYearText.toIntOrNull(),
                            ItemLanguage.normalize(language).takeUnless { isGame },
                            totalText.toIntOrNull().takeUnless { isGame },
                            genres,
                            editedCredits.filter { it.roleType == primaryRole }.map { it.personName },
                            editedCredits,
                            coverUrl.trim().takeIf { it.isNotBlank() },
                            synopsis.trim().takeIf { it.isNotBlank() },
                            sourceUrl.trim().takeIf { it.isNotBlank() },
                            steamAppId.trim().takeIf { it.isNotBlank() },
                        )
                    },
                )
            }
        },
        containerColor = OmnilogTheme.colors.appBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DetailGutter)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                EditorCover(
                    coverUrl = coverUrl,
                    accent = accent,
                    onClick = { editingCover = !editingCover },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LedgerField(
                        label = stringResource(R.string.field_title),
                        value = title,
                        onValueChange = { title = it },
                        accent = accent,
                        changed = titleChanged,
                        singleLine = false,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Normal,
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    LedgerField(
                        label = stringResource(R.string.field_original_title),
                        value = originalTitle,
                        onValueChange = { originalTitle = it },
                        accent = accent,
                        changed = originalTitleChanged,
                        singleLine = false,
                    )
                }
            }

            AnimatedVisibility(visible = editingCover) {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    LedgerField(
                        label = stringResource(R.string.field_cover_url),
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        accent = accent,
                        changed = coverChanged,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { editingCover = false }),
                    )
                    if (coverUrl.isNotBlank()) {
                        TextButton(onClick = { coverUrl = "" }, contentPadding = PaddingValues(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.editor_cover_remove),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.editor_override_note),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                modifier = Modifier.padding(top = 20.dp),
            )

            EditorSection(stringResource(R.string.editor_section_facts)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    LedgerField(
                        label = stringResource(R.string.field_release_year),
                        value = releaseYearText,
                        onValueChange = { value -> releaseYearText = value.filter { it.isDigit() }.take(4) },
                        accent = accent,
                        changed = yearChanged,
                        modifier = Modifier.weight(1f),
                        textStyle = NumberStyle(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (isGame) {
                        Spacer(modifier = Modifier.weight(2.4f))
                    } else {
                        LedgerField(
                            label = stringResource(item.type.totalUnitLabelRes()),
                            value = totalText,
                            onValueChange = { value -> totalText = value.filter { it.isDigit() }.take(6) },
                            accent = accent,
                            changed = totalChanged,
                            modifier = Modifier.weight(1f),
                            textStyle = NumberStyle(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        LanguageField(
                            value = language,
                            onValueChange = { language = it },
                            accent = accent,
                            changed = languageChanged,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
                GenreField(
                    committed = genresText.toMetadataList(),
                    onCommittedChange = { genresText = it.joinToString(GenreSeparator) },
                    draft = genreDraft,
                    onDraftChange = { genreDraft = it },
                    accent = accent,
                    changed = genresChanged,
                )
                LedgerField(
                    label = stringResource(R.string.field_synopsis),
                    value = synopsis,
                    onValueChange = { synopsis = it },
                    accent = accent,
                    changed = synopsisChanged,
                    singleLine = false,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            EditorSection(stringResource(R.string.editor_section_people)) {
                visibleRoles.forEach { role ->
                    CreditListField(
                        label = stringResource(role.editorLabelRes()),
                        encodedDrafts = creditTexts.getValue(role).value,
                        sourceCredits = creditsByRole.getValue(role),
                        onDraftsChange = { creditTexts.getValue(role).value = it },
                        newName = newNames.getValue(role).value,
                        onNewNameChange = { newNames.getValue(role).value = it },
                        accent = accent,
                        changed = role in rolesChanged,
                    )
                }
                val hiddenRoles = MediaCreditRole.entries - visibleRoles.toSet()
                if (hiddenRoles.isNotEmpty()) {
                    AddRoleButton(
                        roles = hiddenRoles,
                        accent = accent,
                        onRoleAdded = { role -> shownRoles = shownRoles + RoleSeparator + role.name },
                    )
                }
            }

            EditorSection(stringResource(R.string.editor_section_links)) {
                LedgerField(
                    label = stringResource(R.string.field_source_url),
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    accent = accent,
                    changed = sourceChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                if (isGame) {
                    LedgerField(
                        label = stringResource(R.string.field_steam_app_id),
                        value = steamAppId,
                        onValueChange = { value -> steamAppId = value.filter(Char::isDigit) },
                        accent = accent,
                        changed = steamChanged,
                        helper = stringResource(R.string.field_steam_app_id_help),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        }
    }
}

/** A serif heading and its fields, spaced like the detail page's sections. */
@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    DetailSectionTitle(title, modifier = Modifier.padding(top = 32.dp, bottom = 14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        content()
    }
}

@Composable
private fun NumberStyle() = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifFontFamily)

/**
 * The label and the rule, around whatever value sits between them.
 *
 * The rule turns the accent while [focused]; the label turns the accent once [changed], which is how
 * the page marks what saving will override.
 */
@Composable
private fun LedgerFrame(
    label: String?,
    accent: Color,
    changed: Boolean,
    focused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        label?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.6f,
                color = if (changed) accent else OmnilogTheme.colors.appMuted,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(modifier = Modifier.heightIn(min = 28.dp), contentAlignment = Alignment.CenterStart) {
            content()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focused) 2.dp else 1.dp)
                .background(if (focused) accent else OmnilogTheme.colors.appLine),
        )
    }
}

/**
 * One text field in the ledger. The whole frame is the text field's decoration, so a tap anywhere on
 * the label or the rule lands in the field rather than on dead space around a thin line of text.
 */
@Composable
private fun LedgerField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    changed: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String = stringResource(R.string.editor_value_empty),
    helper: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style = textStyle.copy(color = OmnilogTheme.colors.appInk)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            textStyle = style,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            cursorBrush = SolidColor(accent),
            decorationBox = { field ->
                LedgerFrame(label = label, accent = accent, changed = changed, focused = focused) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = style.copy(color = OmnilogTheme.colors.appMuted))
                    }
                    field()
                }
            },
        )
        helper?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

@Composable
private fun LanguageField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    changed: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = remember(value) {
        if (value in ItemLanguage.Defaults) ItemLanguage.Defaults else ItemLanguage.Defaults + value
    }
    OmnilogAnchoredDropdown(
        selectedOption = value,
        options = options,
        optionLabel = { languageLabel(it) },
        onOptionSelected = onValueChange,
        modifier = modifier,
    ) { option, _, expanded, anchorModifier ->
        LedgerFrame(
            label = stringResource(R.string.field_language),
            accent = accent,
            changed = changed,
            focused = expanded,
            modifier = anchorModifier,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = languageLabel(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = OmnilogTheme.colors.appMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Genres as the paper pills the detail page shows, each one tapped away, with the entry field kept
 * underneath: adding two genres in a row is the common case, and Done keeps the keyboard up for it.
 */
@Composable
private fun GenreField(
    committed: List<String>,
    onCommittedChange: (List<String>) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    accent: Color,
    changed: Boolean,
) {
    val commitDraft = {
        val entry = draft.trim()
        if (entry.isNotBlank() && committed.none { it.equals(entry, ignoreCase = true) }) {
            onCommittedChange(committed + entry)
        }
        onDraftChange("")
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EntryField(
            label = stringResource(R.string.field_genres),
            value = draft,
            onValueChange = onDraftChange,
            placeholder = stringResource(R.string.editor_add_genre),
            accent = accent,
            changed = changed,
            onCommit = commitDraft,
        )
        if (committed.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                committed.forEach { entry ->
                    val removeLabel = stringResource(R.string.editor_remove_entry, entry)
                    Surface(
                        onClick = { onCommittedChange(committed - entry) },
                        shape = RoundedCornerShape(50),
                        color = OmnilogTheme.colors.appPanel,
                        modifier = Modifier.semantics { contentDescription = removeLabel },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.labelLarge,
                                color = OmnilogTheme.colors.appInk,
                            )
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = OmnilogTheme.colors.appMuted,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One role's people, a row each under the role's label.
 *
 * Each row carries the index of the credit it came from, so an edited name is applied to that same
 * [MediaCredit] on save and keeps its character, portrait and provider origin. A name typed into the
 * entry row has no source and becomes a plain new credit.
 */
@Composable
private fun CreditListField(
    label: String,
    encodedDrafts: String,
    sourceCredits: List<MediaCredit>,
    onDraftsChange: (String) -> Unit,
    newName: String,
    onNewNameChange: (String) -> Unit,
    accent: Color,
    changed: Boolean,
) {
    val drafts = encodedDrafts.decodeCreditDrafts()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        drafts.forEachIndexed { index, draft ->
            val character = sourceCredits.getOrNull(draft.sourceIndex)?.characterName
            Row(verticalAlignment = Alignment.Bottom) {
                LedgerField(
                    // The role is named once, over its first row.
                    label = label.takeIf { index == 0 },
                    value = draft.name,
                    onValueChange = { name ->
                        onDraftsChange(
                            drafts.toMutableList()
                                .also { it[index] = draft.copy(name = name) }
                                .encodeCreditDrafts(),
                        )
                    },
                    accent = accent,
                    changed = changed,
                    helper = character?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(R.string.editor_credit_character, it) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        onDraftsChange(drafts.toMutableList().also { it.removeAt(index) }.encodeCreditDrafts())
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.editor_remove_entry, draft.name),
                        tint = OmnilogTheme.colors.appMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        EntryField(
            label = label.takeIf { drafts.isEmpty() },
            value = newName,
            onValueChange = onNewNameChange,
            placeholder = stringResource(R.string.editor_add_person),
            accent = accent,
            changed = changed,
            capitalization = KeyboardCapitalization.Words,
            onCommit = {
                val entry = newName.trim()
                if (entry.isNotBlank()) {
                    onDraftsChange((drafts + CreditDraft(NoSourceCredit, entry)).encodeCreditDrafts())
                }
                onNewNameChange("")
            },
        )
    }
}

/** A ledger field for adding to a list: Done or the trailing + commits what was typed. */
@Composable
private fun EntryField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    changed: Boolean,
    onCommit: () -> Unit,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        LedgerField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            accent = accent,
            changed = changed,
            placeholder = placeholder,
            keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onCommit, enabled = value.isNotBlank()) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = placeholder,
                tint = if (value.isNotBlank()) accent else OmnilogTheme.colors.appMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AddRoleButton(
    roles: List<MediaCreditRole>,
    accent: Color,
    onRoleAdded: (MediaCreditRole) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        // Flush with the fields above it rather than indented by the button's own padding.
        TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(end = 12.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp),
            )
            Text(
                text = stringResource(R.string.editor_add_role),
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = OmnilogTheme.colors.appPanel,
        ) {
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(stringResource(role.editorLabelRes()), color = OmnilogTheme.colors.appInk) },
                    onClick = {
                        expanded = false
                        onRoleAdded(role)
                    },
                )
            }
        }
    }
}

/**
 * The cover at the detail header's proportions, with what tapping it does written underneath. The
 * URL field it opens sits in the page, so the cover updates beside the title as the address is typed.
 */
@Composable
private fun EditorCover(coverUrl: String, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(CoverWidth)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetadataCoverImage(
            coverUrl = coverUrl.takeIf { it.isNotBlank() },
            modifier = Modifier
                .width(CoverWidth)
                .height(CoverWidth * 1.5f),
            shape = RoundedCornerShape(6.dp),
        )
        Text(
            text = stringResource(if (coverUrl.isBlank()) R.string.editor_cover_add else R.string.editor_cover_change),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center,
        )
    }
}

private val CoverWidth = 96.dp
private const val RoleSeparator = ","

/** One person in one role, and the credit it was read from — [NoSourceCredit] when it is new. */
private data class CreditDraft(val sourceIndex: Int, val name: String)

private const val NoSourceCredit = -1
private const val GenreSeparator = ", "
private const val DraftFieldSeparator = '\u0001'
private const val DraftEntrySeparator = '\n'

// Saveable as one string so a rotation mid-edit keeps both the names and their provenance.
private fun List<CreditDraft>.encodeCreditDrafts(): String =
    joinToString(DraftEntrySeparator.toString()) { "${it.sourceIndex}$DraftFieldSeparator${it.name}" }

private fun String.decodeCreditDrafts(): List<CreditDraft> {
    if (isEmpty()) return emptyList()
    return split(DraftEntrySeparator).map { entry ->
        val separator = entry.indexOf(DraftFieldSeparator)
        if (separator < 0) {
            CreditDraft(NoSourceCredit, entry)
        } else {
            CreditDraft(
                sourceIndex = entry.take(separator).toIntOrNull() ?: NoSourceCredit,
                name = entry.substring(separator + 1),
            )
        }
    }
}

private fun List<CreditDraft>.toCredits(
    role: MediaCreditRole,
    sourceCredits: List<MediaCredit>,
): List<MediaCredit> =
    mapNotNull { draft ->
        val name = draft.name.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val source = sourceCredits.getOrNull(draft.sourceIndex)
        source?.copy(id = 0, personName = name) ?: MediaCredit(personName = name, roleType = role)
    }
        .distinctBy { it.personName.lowercase() }
        .mapIndexed { index, credit -> credit.copy(sortOrder = index) }

private fun String.toMetadataList(): List<String> =
    split(",", "\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

private fun MediaType.groupCreatorRole(): MediaCreditRole = when (this) {
    MediaType.Anime -> MediaCreditRole.Studio
    MediaType.Book -> MediaCreditRole.Author
    MediaType.Movie -> MediaCreditRole.Director
    MediaType.TvShow -> MediaCreditRole.Creator
    MediaType.Game -> MediaCreditRole.Developer
}

@StringRes
private fun MediaType.totalUnitLabelRes(): Int = when (this) {
    MediaType.Anime,
    MediaType.TvShow,
        -> R.string.metadata_total_episodes
    MediaType.Book -> R.string.metadata_total_pages
    MediaType.Movie -> R.string.metadata_total_minutes
    MediaType.Game -> R.string.metadata_total_hours
}

@StringRes
private fun MediaCreditRole.editorLabelRes(): Int = when (this) {
    MediaCreditRole.Author -> R.string.metadata_credits_authors
    MediaCreditRole.Director -> R.string.metadata_credits_directors
    MediaCreditRole.Creator -> R.string.metadata_credits_creators
    MediaCreditRole.Studio -> R.string.metadata_credits_studios
    MediaCreditRole.Developer -> R.string.metadata_credits_developers
    MediaCreditRole.Publisher -> R.string.metadata_credits_publishers
    MediaCreditRole.Cast -> R.string.metadata_credits_cast
    MediaCreditRole.VoiceActor -> R.string.metadata_credits_voice_actors
}
