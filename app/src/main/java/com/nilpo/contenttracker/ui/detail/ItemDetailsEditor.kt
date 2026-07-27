package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import coil3.compose.AsyncImage
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.OmnilogAnchoredDropdown
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The item record, editable.
 *
 * This is a working form and nothing else: no accent rules, no hero, no second rendering of the
 * detail page. What it borrows from the rest of the app is the form idiom — a panel per field with
 * its label above its value — and it drops the stock outlined fields that made the old version read
 * as a Material dialog pasted into Omnilog.
 *
 * Two things are structural rather than cosmetic. Lists that the detail page draws as lists are
 * edited as lists: genres are chips and every credited person is a row, not a comma-separated line
 * in a text box. And each credit row keeps the [MediaCredit] it came from, so renaming a performer
 * no longer discards their character and portrait — the old form rebuilt credits from text and only
 * recovered the extras when the typed name still matched exactly.
 *
 * Saving marks every changed field as a local override, which protects it from provider refreshes.
 * The form therefore only enables Desa once something actually differs, so opening the page and
 * closing it cannot silently freeze the record.
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
    var coverUrl by rememberSaveable(item.id) { mutableStateOf(item.coverUrl.orEmpty()) }
    var sourceUrl by rememberSaveable(item.id) { mutableStateOf(item.sourceUrl.orEmpty()) }
    var steamAppId by rememberSaveable(item.id) { mutableStateOf(item.steamAppId.orEmpty()) }
    var synopsis by rememberSaveable(item.id) { mutableStateOf(initialSynopsis) }
    val creditTexts = MediaCreditRole.entries.associateWith { role ->
        rememberSaveable(item.id, role.name) { mutableStateOf(initialDrafts.getValue(role)) }
    }

    var showCoverDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    var showOtherCredits by rememberSaveable(item.id) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(item.id) { mutableStateOf(false) }

    val hasChanges = title != item.title ||
        originalTitle != item.originalTitle.orEmpty() ||
        releaseYearText != item.releaseYear?.toString().orEmpty() ||
        language != initialLanguage ||
        totalText != item.progressTotal?.toString().orEmpty() ||
        genresText != initialGenres ||
        coverUrl != item.coverUrl.orEmpty() ||
        sourceUrl != item.sourceUrl.orEmpty() ||
        steamAppId != item.steamAppId.orEmpty() ||
        synopsis != initialSynopsis ||
        MediaCreditRole.entries.any { creditTexts.getValue(it).value != initialDrafts.getValue(it) }

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

    if (showCoverDialog) {
        CoverUrlDialog(
            coverUrl = coverUrl,
            accent = accent,
            onConfirm = {
                coverUrl = it
                showCoverDialog = false
            },
            onDismiss = { showCoverDialog = false },
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    Button(
                        enabled = title.isNotBlank() && hasChanges,
                        onClick = {
                            val editedCredits = MediaCreditRole.entries.flatMap { role ->
                                creditTexts.getValue(role).value
                                    .decodeCreditDrafts()
                                    .toCredits(role, creditsByRole.getValue(role))
                            }
                            onSaveMetadata(
                                title.trim(),
                                originalTitle.trim().takeIf { it.isNotBlank() },
                                releaseYearText.toIntOrNull(),
                                ItemLanguage.normalize(language).takeUnless { isGame },
                                totalText.toIntOrNull().takeUnless { isGame },
                                genresText.toMetadataList(),
                                editedCredits.filter { it.roleType == primaryRole }.map { it.personName },
                                editedCredits,
                                coverUrl.trim().takeIf { it.isNotBlank() },
                                synopsis.trim().takeIf { it.isNotBlank() },
                                sourceUrl.trim().takeIf { it.isNotBlank() },
                                steamAppId.trim().takeIf { it.isNotBlank() },
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                            disabledContainerColor = OmnilogTheme.colors.appPanel,
                            disabledContentColor = OmnilogTheme.colors.appMuted,
                        ),
                    ) {
                        Text(text = stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OmnilogTheme.colors.appBackground),
            )
        },
        containerColor = OmnilogTheme.colors.appBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EditorSectionTitle(stringResource(R.string.editor_section_identity))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CoverThumbnail(
                    coverUrl = coverUrl,
                    accent = accent,
                    onClick = { showCoverDialog = true },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    EditorField(
                        label = stringResource(R.string.field_title),
                        value = title,
                        onValueChange = { title = it },
                        accent = accent,
                    )
                    EditorField(
                        label = stringResource(R.string.field_original_title),
                        value = originalTitle,
                        onValueChange = { originalTitle = it },
                        accent = accent,
                    )
                }
            }

            EditorSectionTitle(stringResource(R.string.editor_section_facts))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EditorField(
                    label = stringResource(R.string.field_release_year),
                    value = releaseYearText,
                    onValueChange = { value -> releaseYearText = value.filter { it.isDigit() }.take(4) },
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    emphasised = true,
                )
                if (!isGame) {
                    EditorField(
                        label = stringResource(item.type.totalUnitLabelRes()),
                        value = totalText,
                        onValueChange = { value -> totalText = value.filter { it.isDigit() } },
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        emphasised = true,
                    )
                }
            }
            if (!isGame) {
                LanguageField(
                    value = language,
                    onValueChange = { language = it },
                    accent = accent,
                )
            }
            ChipListField(
                label = stringResource(R.string.field_genres),
                values = genresText.toMetadataList(),
                onValuesChange = { genresText = it.joinToString(GenreSeparator) },
                addLabel = stringResource(R.string.editor_add_genre),
                accent = accent,
            )
            EditorField(
                label = stringResource(R.string.field_synopsis),
                value = synopsis,
                onValueChange = { synopsis = it },
                accent = accent,
                singleLine = false,
                minLines = 4,
            )

            EditorSectionTitle(stringResource(R.string.editor_section_people))
            CreditListField(
                label = stringResource(primaryRole.editorLabelRes()),
                encodedDrafts = creditTexts.getValue(primaryRole).value,
                sourceCredits = creditsByRole.getValue(primaryRole),
                onDraftsChange = { creditTexts.getValue(primaryRole).value = it },
                accent = accent,
            )
            TextButton(onClick = { showOtherCredits = !showOtherCredits }) {
                Text(
                    text = stringResource(R.string.editor_other_credits),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = if (showOtherCredits) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent,
                )
            }
            AnimatedVisibility(visible = showOtherCredits) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MediaCreditRole.entries.filter { it != primaryRole }.forEach { role ->
                        CreditListField(
                            label = stringResource(role.editorLabelRes()),
                            encodedDrafts = creditTexts.getValue(role).value,
                            sourceCredits = creditsByRole.getValue(role),
                            onDraftsChange = { creditTexts.getValue(role).value = it },
                            accent = accent,
                        )
                    }
                }
            }

            EditorSectionTitle(stringResource(R.string.editor_section_links))
            EditorField(
                label = stringResource(R.string.field_source_url),
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                accent = accent,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            if (isGame) {
                EditorField(
                    label = stringResource(R.string.field_steam_app_id),
                    value = steamAppId,
                    onValueChange = { value -> steamAppId = value.filter(Char::isDigit) },
                    accent = accent,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    helper = stringResource(R.string.field_steam_app_id_help),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * A section break, not a section card.
 *
 * The fields below it are already panels, so nesting them inside a second panel only added an
 * outline and 16dp of padding on both sides of every value.
 */
@Composable
private fun EditorSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = OmnilogTheme.colors.appInk,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun EditorPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            content = content,
        )
    }
}

/**
 * One field: its name above its value, both inside the panel.
 *
 * [emphasised] is for the numbers — a year or a page count is the whole content of its panel, so it
 * carries the accent and a size up rather than sitting at body weight like a title.
 */
@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    emphasised: Boolean = false,
    helper: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    EditorPanel(modifier) {
        DetailFieldLabel(label)
        val textStyle = if (emphasised) {
            MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
        } else {
            MaterialTheme.typography.bodyLarge.copy(color = OmnilogTheme.colors.appInk)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(accent),
            decorationBox = { field ->
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_value_empty),
                        style = textStyle.copy(color = OmnilogTheme.colors.appMuted),
                    )
                }
                field()
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
) {
    val options = remember(value) {
        if (value in ItemLanguage.Defaults) ItemLanguage.Defaults else ItemLanguage.Defaults + value
    }
    OmnilogAnchoredDropdown(
        selectedOption = value,
        options = options,
        optionLabel = { languageLabel(it) },
        onOptionSelected = onValueChange,
        modifier = Modifier.fillMaxWidth(),
    ) { option, _, _, anchorModifier ->
        EditorPanel(anchorModifier) {
            DetailFieldLabel(stringResource(R.string.field_language))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = languageLabel(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OmnilogTheme.colors.appInk,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * A list of short values, edited as the chips the detail page draws them as.
 *
 * The entry row stays at the bottom rather than appearing on demand: adding two genres in a row is
 * the common case, and a plus that swaps itself for a field costs a tap each time.
 */
@Composable
private fun ChipListField(
    label: String,
    values: List<String>,
    onValuesChange: (List<String>) -> Unit,
    addLabel: String,
    accent: Color,
) {
    var draft by rememberSaveable(label) { mutableStateOf("") }
    val commitDraft = {
        val entry = draft.trim()
        if (entry.isNotBlank() && values.none { it.equals(entry, ignoreCase = true) }) {
            onValuesChange(values + entry)
        }
        draft = ""
    }

    EditorPanel {
        DetailFieldLabel(label)
        if (values.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.labelMedium,
                                color = OmnilogTheme.colors.appInk,
                            )
                            IconButton(
                                onClick = { onValuesChange(values - entry) },
                                modifier = Modifier.size(26.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.editor_remove_entry, entry),
                                    tint = OmnilogTheme.colors.appMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        InlineEntryRow(
            value = draft,
            onValueChange = { draft = it },
            placeholder = addLabel,
            accent = accent,
            onCommit = commitDraft,
        )
    }
}

/**
 * One role's people, a row each.
 *
 * Each row carries the index of the credit it came from, so an edited name is applied to that same
 * [MediaCredit] on save and keeps its character, portrait and provider origin. A row typed into the
 * empty entry field has no source and becomes a plain new credit.
 */
@Composable
private fun CreditListField(
    label: String,
    encodedDrafts: String,
    sourceCredits: List<MediaCredit>,
    onDraftsChange: (String) -> Unit,
    accent: Color,
) {
    val drafts = encodedDrafts.decodeCreditDrafts()
    var newName by rememberSaveable(label) { mutableStateOf("") }

    EditorPanel {
        DetailFieldLabel(label)
        drafts.forEachIndexed { index, draft ->
            val character = sourceCredits.getOrNull(draft.sourceIndex)?.characterName
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = draft.name,
                        onValueChange = { name ->
                            onDraftsChange(
                                drafts.toMutableList()
                                    .also { it[index] = draft.copy(name = name) }
                                    .encodeCreditDrafts(),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = OmnilogTheme.colors.appInk),
                        cursorBrush = SolidColor(accent),
                    )
                    character?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = stringResource(R.string.editor_credit_character, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        onDraftsChange(
                            drafts.toMutableList().also { it.removeAt(index) }.encodeCreditDrafts(),
                        )
                    },
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.editor_remove_entry, draft.name),
                        tint = OmnilogTheme.colors.appMuted,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        InlineEntryRow(
            value = newName,
            onValueChange = { newName = it },
            placeholder = stringResource(R.string.editor_add_person),
            accent = accent,
            onCommit = {
                val entry = newName.trim()
                if (entry.isNotBlank()) {
                    onDraftsChange((drafts + CreditDraft(NoSourceCredit, entry)).encodeCreditDrafts())
                }
                newName = ""
            },
        )
    }
}

/** The "type a value, press add" row shared by the chip and credit lists. */
@Composable
private fun InlineEntryRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OmnilogTheme.colors.appInk),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            decorationBox = { field ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                field()
            },
        )
        IconButton(
            onClick = onCommit,
            enabled = value.isNotBlank(),
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = placeholder,
                tint = if (value.isNotBlank()) accent else OmnilogTheme.colors.appMuted,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun CoverThumbnail(coverUrl: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(84.dp)
            .height(122.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, OmnilogTheme.colors.appLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.field_cover_url),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(R.string.editor_cover_change).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(vertical = 4.dp),
        )
    }
}

/**
 * The cover URL, with its own preview.
 *
 * Removing the cover is its own button. It used to be the dialog's dismiss action, so "Cancel·la"
 * wiped the artwork instead of leaving it alone.
 */
@Composable
private fun CoverUrlDialog(
    coverUrl: String,
    accent: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable(coverUrl) { mutableStateOf(coverUrl) }
    OmnilogAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.field_cover_url),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(145.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (draft.isNotBlank()) {
                        AsyncImage(
                            model = draft,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                EditorField(
                    label = stringResource(R.string.field_cover_url),
                    value = draft,
                    onValueChange = { draft = it },
                    accent = accent,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                if (draft.isNotBlank()) {
                    TextButton(onClick = { draft = "" }) {
                        Text(
                            text = stringResource(R.string.editor_cover_remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(draft) },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = OmnilogTheme.colors.appMuted)
            }
        },
    )
}

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
