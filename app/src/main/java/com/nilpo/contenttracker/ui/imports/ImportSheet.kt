package com.nilpo.contenttracker.ui.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.imports.AnimeTitlePreference
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.repository.ProviderImportPreview
import com.nilpo.contenttracker.core.repository.ProviderImportResult
import com.nilpo.contenttracker.core.repository.ProviderRejectedReason
import com.nilpo.contenttracker.core.repository.rejectedGroups
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.OmnilogPrimaryButton
import com.nilpo.contenttracker.ui.common.OmnilogTonalButton
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.home.themedAccent
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

private val SheetGutter = 24.dp

/**
 * One sheet in front of every provider import, in two steps.
 *
 * Without a [preview] it is the guide: what the source brings in, where its export lives and how to
 * get it, then the file picker (and, for MyAnimeList with a connected account, the account import
 * as the easier road). With a [preview] it is the confirmation: how many titles are new, what is
 * left out and why, and one button that says exactly what it will do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSheet(
    source: ImportSource,
    preview: ProviderImportPreview?,
    error: String?,
    isReadingSource: Boolean,
    isImporting: Boolean,
    malAccountName: String?,
    animeTitlePreference: AnimeTitlePreference,
    onAnimeTitlePreferenceChange: (AnimeTitlePreference) -> Unit,
    onChooseFile: () -> Unit,
    onImportMalAccount: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = source.accent()
    ModalBottomSheet(
        onDismissRequest = { if (!isImporting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = SheetGutter, end = SheetGutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProviderLogo(source = source.ratingSource(), height = 22.dp)
                    Text(
                        text = when {
                            source == ImportSource.MalApi -> "Importa el compte"
                            // Catalan elides "de" before a vowel: d'IMDb, de StoryGraph.
                            source.label().first().lowercaseChar() in "aeiou" -> "Importa d'${source.label()}"
                            else -> "Importa de ${source.label()}"
                        },
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = OmnilogTheme.colors.appInk,
                    )
                }
                Text(
                    text = source.whatComesIn(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }

            if (preview == null) {
                GuideStep(
                    source = source,
                    accent = accent,
                    error = error,
                    isReadingSource = isReadingSource,
                    malAccountName = malAccountName,
                    onChooseFile = onChooseFile,
                    onImportMalAccount = onImportMalAccount,
                )
            } else {
                PreviewStep(
                    source = source,
                    preview = preview,
                    accent = accent,
                    isImporting = isImporting,
                    animeTitlePreference = animeTitlePreference,
                    onAnimeTitlePreferenceChange = onAnimeTitlePreferenceChange,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun GuideStep(
    source: ImportSource,
    accent: Color,
    error: String?,
    isReadingSource: Boolean,
    malAccountName: String?,
    onChooseFile: () -> Unit,
    onImportMalAccount: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val guide = source.fileGuide()

    // A connected account needs no file at all, so it comes first and takes the strong button.
    if (malAccountName != null) {
        SheetPanel {
            Text(
                text = "Connectat com a $malAccountName",
                style = MaterialTheme.typography.bodyLarge,
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = "Llegeix la llista directament del compte, sense cap fitxer.",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            OmnilogPrimaryButton(
                text = if (isReadingSource) "Llegint el compte…" else "Importa des del compte",
                onClick = onImportMalAccount,
                enabled = !isReadingSource,
                accent = accent,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SheetLabel(if (malAccountName != null) "O des d'un fitxer" else "Com obtenir el fitxer")
        guide.steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "${index + 1}",
                    modifier = Modifier.width(16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = accent,
                )
                Text(
                    text = step,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appInk,
                )
            }
        }
        Text(
            text = guide.note,
            style = MaterialTheme.typography.bodySmall,
            color = OmnilogTheme.colors.appMuted,
        )
    }

    error?.let {
        Text(
            text = it,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OmnilogTheme.accents.Dropped.copy(alpha = 0.12f))
                .padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appInk,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // With an account on offer the file is the second road, so it steps down to tonal.
        if (malAccountName != null) {
            OmnilogTonalButton("Tria el fitxer", onChooseFile, Modifier.weight(1f), enabled = !isReadingSource, accent = accent)
        } else {
            OmnilogPrimaryButton(
                text = if (isReadingSource) "Llegint el fitxer…" else "Tria el fitxer",
                onClick = onChooseFile,
                modifier = Modifier.weight(1f),
                enabled = !isReadingSource,
                accent = accent,
            )
        }
        OmnilogTonalButton(
            text = "Obre ${guide.siteName}",
            onClick = { runCatching { uriHandler.openUri(guide.url) } },
            modifier = Modifier.weight(1f),
            accent = accent,
        )
    }
}

@Composable
private fun PreviewStep(
    source: ImportSource,
    preview: ProviderImportPreview,
    accent: Color,
    isImporting: Boolean,
    animeTitlePreference: AnimeTitlePreference,
    onAnimeTitlePreferenceChange: (AnimeTitlePreference) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val importable = preview.importableRows
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = importable.toString(),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = "${source.noun(importable)} ${if (importable == 1) "nou" else "nous"}",
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        Text(
            text = "de ${preview.totalRows} ${if (preview.totalRows == 1) "entrada" else "entrades"} " +
                if (source == ImportSource.MalApi) "del compte" else "del fitxer",
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appMuted,
        )
    }

    if (preview.totalRows > importable) SkippedRows(preview)

    if (source.isAnime()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SheetLabel("Títols")
            AnimeTitlePreference.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .selectable(
                            selected = option == animeTitlePreference,
                            role = Role.RadioButton,
                            onClick = { onAnimeTitlePreferenceChange(option) },
                        )
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == animeTitlePreference,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = accent,
                            unselectedColor = OmnilogTheme.colors.appMuted,
                        ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (option) {
                                AnimeTitlePreference.EnglishWithRomajiOriginal -> "En anglès, amb el rōmaji com a original"
                                AnimeTitlePreference.KeepMalTitle -> "Tal com els escriu MyAnimeList"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = OmnilogTheme.colors.appInk,
                        )
                    }
                }
            }
        }
    }

    Text(
        text = "Només s'afegeixen títols nous: res del que ja tens canvia. Després, Omnilog completarà " +
            "portades, sinopsis i totals en segon pla, i el que no sigui segur quedarà per revisar a " +
            "l'activitat d'importació.",
        style = MaterialTheme.typography.bodySmall,
        color = OmnilogTheme.colors.appMuted,
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OmnilogPrimaryButton(
            text = if (isImporting) "Important…" else "Importa $importable ${source.noun(importable)}",
            onClick = onConfirm,
            enabled = !isImporting,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        )
        TextButton(onClick = onDismiss, enabled = !isImporting) {
            Text(text = stringResource(R.string.cancel), color = OmnilogTheme.colors.appMuted)
        }
    }
}

/** What stays out, folded behind one line; each reason opens onto a few example rows. */
@Composable
private fun SkippedRows(preview: ProviderImportPreview) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "skippedChevron")
    val skipped = preview.totalRows - preview.importableRows
    Column {
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val groups = preview.rejectedGroups()
            // One reason says it all on its own; several get the total and the split beside it.
            Text(
                text = groups.singleOrNull()?.let { "${it.count} ${it.reason.shortLabel()}" }
                    ?: "$skipped no s'importaran",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = OmnilogTheme.colors.appInk,
            )
            if (groups.size > 1) {
                Text(
                    text = groups.joinToString(" · ") { "${it.count} ${it.reason.shortLabel()}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                preview.rejectedGroups().forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(
                                when (group.reason) {
                                    ProviderRejectedReason.Duplicate -> R.string.provider_import_rejected_duplicate
                                    ProviderRejectedReason.UnsupportedType -> R.string.provider_import_rejected_unsupported
                                    ProviderRejectedReason.MissingTitle -> R.string.provider_import_rejected_missing_title
                                },
                                group.count,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        group.samples.forEach { row ->
                            Text(
                                text = row.label?.let { label ->
                                    stringResource(R.string.provider_import_rejected_sample, row.rowNumber, label)
                                } ?: stringResource(R.string.provider_import_rejected_sample_without_label, row.rowNumber),
                                style = MaterialTheme.typography.bodySmall,
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                        val remaining = group.count - group.samples.size
                        if (remaining > 0) {
                            Text(
                                text = stringResource(R.string.provider_import_rejected_more, remaining),
                                style = MaterialTheme.typography.bodySmall,
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
    }
}

@Composable
private fun SheetPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OmnilogTheme.colors.appPanel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text.uppercase(OmnilogLocale),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = OmnilogTheme.colors.appMuted,
    )
}

private class FileGuide(val steps: List<String>, val note: String, val siteName: String, val url: String)

// ponytail: the steps describe each provider's export page as of September 2026; they will need a
// touch-up whenever a provider moves its menus.
private fun ImportSource.fileGuide(): FileGuide = when (this) {
    ImportSource.ImdbCsv -> FileGuide(
        steps = listOf(
            "Obre IMDb al navegador amb la sessió iniciada.",
            "Ves a les teves valoracions o a una llista i toca ⋯ › Exporta.",
            "Quan l'exportació estigui a punt, descarrega el CSV.",
        ),
        note = "Tria el CSV tal com el descarregues, sense editar-lo. Les sèries i les pel·lícules van a Cinema i TV; els episodis s'ometen.",
        siteName = "IMDb",
        url = "https://www.imdb.com/list/ratings",
    )
    ImportSource.StoryGraphCsv -> FileGuide(
        steps = listOf(
            "Obre StoryGraph al navegador amb la sessió iniciada.",
            "Ves al perfil › Manage Account › Export StoryGraph Library.",
            "Toca Generate export i descarrega el CSV quan aparegui.",
        ),
        note = "Tria el CSV tal com el descarregues, sense editar-lo.",
        siteName = "StoryGraph",
        url = "https://app.thestorygraph.com/user-export",
    )
    ImportSource.MalXml, ImportSource.MalApi -> FileGuide(
        steps = listOf(
            "Obre la pàgina d'exportació de MyAnimeList amb la sessió iniciada.",
            "Tria Anime List i toca Export My List.",
            "Descarrega el fitxer .xml.gz.",
        ),
        note = "No cal descomprimir-lo: Omnilog llegeix el .xml.gz directament.",
        siteName = "MyAnimeList",
        url = "https://myanimelist.net/panel.php?go=export",
    )
}

private fun ImportSource.whatComesIn(): String = when (this) {
    ImportSource.ImdbCsv -> "Pel·lícules i sèries, amb les teves notes i dates, a Cinema i TV."
    ImportSource.StoryGraphCsv -> "Llibres amb l'estat de lectura, les dates, les notes i les ressenyes."
    ImportSource.MalXml, ImportSource.MalApi -> "Animes amb l'estat, els episodis vistos, les dates i les notes."
}

private fun ProviderRejectedReason.shortLabel(): String = when (this) {
    ProviderRejectedReason.Duplicate -> "ja hi són"
    ProviderRejectedReason.UnsupportedType -> "no compatibles"
    ProviderRejectedReason.MissingTitle -> "sense títol"
}

private fun ImportSource.isAnime(): Boolean = this == ImportSource.MalXml || this == ImportSource.MalApi

internal fun ImportSource.label(): String = when (this) {
    ImportSource.MalApi, ImportSource.MalXml -> "MyAnimeList"
    ImportSource.ImdbCsv -> "IMDb"
    ImportSource.StoryGraphCsv -> "StoryGraph"
}

internal fun ImportSource.ratingSource(): ExternalRatingSource = when (this) {
    ImportSource.MalApi, ImportSource.MalXml -> ExternalRatingSource.Mal
    ImportSource.ImdbCsv -> ExternalRatingSource.Imdb
    ImportSource.StoryGraphCsv -> ExternalRatingSource.StoryGraph
}

/** The accent of the section the source fills, so an import reads as belonging to its shelf. */
@Composable
internal fun ImportSource.accent(): Color = when (this) {
    ImportSource.MalApi, ImportSource.MalXml -> MediaSection.Anime
    ImportSource.ImdbCsv -> MediaSection.Movies
    ImportSource.StoryGraphCsv -> MediaSection.Books
}.themedAccent()

internal fun ImportSource.noun(count: Int): String = when (this) {
    ImportSource.MalApi, ImportSource.MalXml -> if (count == 1) "anime" else "animes"
    ImportSource.ImdbCsv -> if (count == 1) "títol" else "títols"
    ImportSource.StoryGraphCsv -> if (count == 1) "llibre" else "llibres"
}

/** The result in one line, leaving out every counter that is zero. */
internal fun ProviderImportResult.userFacingMessage(source: ImportSource): String = buildString {
    append("$importedRows ${source.noun(importedRows)} ${if (importedRows == 1) "importat" else "importats"}")
    if (skippedDuplicateRows > 0) append(" · $skippedDuplicateRows ja hi eren")
    val left = unsupportedRows + invalidRows
    if (left > 0) append(" · $left ${if (left == 1) "omès" else "omesos"}")
    append(". Les metadades es completen en segon pla.")
}
