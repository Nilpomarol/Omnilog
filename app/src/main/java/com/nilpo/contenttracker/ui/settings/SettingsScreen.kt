package com.nilpo.contenttracker.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.BuildConfig
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.backup.AutoBackupFrequency
import com.nilpo.contenttracker.core.backup.AutoBackupRetentionOptions
import com.nilpo.contenttracker.core.imports.AnimeTitlePreference
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.ImportEnrichmentState
import com.nilpo.contenttracker.core.mal.MalSyncChange
import com.nilpo.contenttracker.core.mal.MalSyncState
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.refresh.MetadataRefreshProgress
import com.nilpo.contenttracker.core.refresh.MetadataRefreshRunState
import com.nilpo.contenttracker.core.refresh.MetadataRefreshState
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.OmnilogPrimaryButton
import com.nilpo.contenttracker.ui.common.OmnilogTonalButton
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.common.rememberActiveFilterPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenActiveSections
import com.nilpo.contenttracker.ui.common.writeHiddenActiveSections
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.home.navIconResId
import com.nilpo.contenttracker.ui.home.themedAccent
import com.nilpo.contenttracker.ui.theme.DarkAccents
import com.nilpo.contenttracker.ui.theme.DarkPalette
import com.nilpo.contenttracker.ui.theme.LightAccents
import com.nilpo.contenttracker.ui.theme.LightPalette
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import com.nilpo.contenttracker.ui.theme.ThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreferences
import com.nilpo.contenttracker.ui.theme.writeThemePreference
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The inset of everything on the page — titles, labels and rows — so they share one left edge. */
private val HeaderGutter = 20.dp
private val PanelMargin = 16.dp
private val PanelPadding = 16.dp

/** Whether tapping a row opens something else, or acts immediately in place. */
private enum class SettingsRowAffordance { Chevron, None }

/**
 * Preferences, backups, imports, MyAnimeList and metadata, set as chapters: a serif title, then named
 * groups, ruled rows set straight on the page. Three kinds of thing never look alike — a chapter's serif title, a
 * setting's plain row, and an action, which is always a button. A chevron on a row means it opens
 * another screen, sheet or system picker.
 */
@Composable
fun SettingsScreen(
    askForGoodreadsRating: Boolean,
    onAskForGoodreadsRatingChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onImportMyAnimeListAccount: () -> Unit,
    onImportMyAnimeListXml: () -> Unit,
    onImportImdbCsv: () -> Unit,
    onImportStoryGraphCsv: () -> Unit,
    isAutoBackupEnabled: Boolean,
    autoBackupFrequency: AutoBackupFrequency,
    lastAutoBackupAtEpochMillis: Long?,
    autoBackupFolderLabel: String?,
    maxKeptBackups: Int,
    onAutoBackupFolderRequested: () -> Unit,
    onAutoBackupFrequencyChange: (AutoBackupFrequency) -> Unit,
    onMaxKeptBackupsChange: (Int) -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    malSyncState: MalSyncState,
    importEnrichmentState: ImportEnrichmentState,
    metadataRefreshState: MetadataRefreshState,
    onBulkMetadataRefresh: () -> Unit,
    onCancelBulkMetadataRefresh: (Long) -> Unit,
    animeTitlePreference: AnimeTitlePreference,
    onAnimeTitlePreferenceChange: (AnimeTitlePreference) -> Unit,
    onBulkRefreshAnimeTitles: () -> Unit,
    onOpenImportActivity: () -> Unit,
    onConnectMyAnimeList: () -> Unit,
    onSyncMyAnimeList: () -> Unit,
    onRetryMyAnimeList: () -> Unit,
    onCancelMyAnimeList: () -> Unit,
    onDisconnectMyAnimeList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMetadataHistory by remember { mutableStateOf(false) }
    var showMalPendingChanges by remember { mutableStateOf(false) }

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(44.dp),
        ) {
            item(key = "appearance") { AppearanceChapter() }
            item(key = "backups") {
                BackupsChapter(
                    isAutoBackupEnabled = isAutoBackupEnabled,
                    frequency = autoBackupFrequency,
                    lastSuccessAtEpochMillis = lastAutoBackupAtEpochMillis,
                    folderLabel = autoBackupFolderLabel,
                    maxKeptBackups = maxKeptBackups,
                    onToggle = onAutoBackupToggle,
                    onFolderRequested = onAutoBackupFolderRequested,
                    onFrequencyChange = onAutoBackupFrequencyChange,
                    onMaxKeptBackupsChange = onMaxKeptBackupsChange,
                    onExport = onExportBackup,
                    onRestore = onRestoreBackup,
                )
            }
            item(key = "mal") {
                MyAnimeListChapter(
                    state = malSyncState,
                    onConnect = onConnectMyAnimeList,
                    onSync = onSyncMyAnimeList,
                    onRetry = onRetryMyAnimeList,
                    onDisconnect = onDisconnectMyAnimeList,
                    onImportAccount = onImportMyAnimeListAccount,
                    onShowPendingChanges = { showMalPendingChanges = true },
                )
            }
            item(key = "imports") {
                ImportsChapter(
                    importEnrichmentState = importEnrichmentState,
                    isMalImporting = malSyncState.isImporting,
                    onOpenImportActivity = onOpenImportActivity,
                    onImportImdb = onImportImdbCsv,
                    onImportStoryGraph = onImportStoryGraphCsv,
                    onImportMalXml = onImportMyAnimeListXml,
                )
            }
            item(key = "metadata") {
                MetadataChapter(
                    refreshState = metadataRefreshState,
                    onRefreshAll = onBulkMetadataRefresh,
                    onCancelRefresh = onCancelBulkMetadataRefresh,
                    onShowHistory = { showMetadataHistory = true },
                    animeTitlePreference = animeTitlePreference,
                    onAnimeTitlePreferenceChange = onAnimeTitlePreferenceChange,
                    onApplyAnimeTitles = onBulkRefreshAnimeTitles,
                    askForGoodreadsRating = askForGoodreadsRating,
                    onAskForGoodreadsRatingChange = onAskForGoodreadsRatingChange,
                )
            }
            item(key = "about") { SettingsAboutFooter() }
        }
    }

    if (showMetadataHistory) {
        MetadataRefreshHistorySheet(
            history = metadataRefreshState.history,
            onDismiss = { showMetadataHistory = false },
        )
    }

    // Once the queue drains — a sync finishes or the user clears it — there is nothing left to
    // review, so drop the sheet rather than leave it open on an empty list.
    if (showMalPendingChanges && malSyncState.changes.isEmpty()) {
        showMalPendingChanges = false
    }
    if (showMalPendingChanges) {
        MalPendingChangesSheet(
            state = malSyncState,
            onRetry = onRetryMyAnimeList,
            onCancel = onCancelMyAnimeList,
            onDismiss = { showMalPendingChanges = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Chapters
// ─────────────────────────────────────────────────────────────

/** How the app looks: the theme, and which sections Home's «Ara mateix» shows. */
@Composable
private fun AppearanceChapter() {
    val themePreferences = rememberThemePreferences()
    val themePreference by rememberThemePreference(themePreferences)
    val activeFilterPreferences = rememberActiveFilterPreferences()
    val hiddenSections by rememberHiddenActiveSections(activeFilterPreferences)

    SettingsChapter(title = "Aparença") {
        SettingsGroup(label = "Tema") {
            Row(
                modifier = Modifier.padding(horizontal = HeaderGutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemePreference.entries.forEach { option ->
                    ThemeSwatch(
                        option = option,
                        isSelected = option == themePreference,
                        onSelect = { themePreferences.writeThemePreference(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        SettingsGroup(
            label = "Pantalla d'inici",
            footnote = "Toca una secció per amagar-la o mostrar-la a «Ara mateix». " +
                visibleSectionsSummary(hiddenSections),
        ) {
            Row(modifier = Modifier.padding(horizontal = HeaderGutter - 8.dp, vertical = 8.dp)) {
                MediaSection.entries.forEach { section ->
                    val isVisible = section !in hiddenSections
                    SectionVisibilityToggle(
                        section = section,
                        isVisible = isVisible,
                        onToggle = {
                            activeFilterPreferences.writeHiddenActiveSections(
                                if (isVisible) hiddenSections + section else hiddenSections - section,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The chapter's note says where the library's safety stands, so the state of the automatic copy
 * reads at a glance; its controls follow, and the one-off copies close the chapter as two tiles.
 */
@Composable
private fun BackupsChapter(
    isAutoBackupEnabled: Boolean,
    frequency: AutoBackupFrequency,
    lastSuccessAtEpochMillis: Long?,
    folderLabel: String?,
    maxKeptBackups: Int,
    onToggle: (Boolean) -> Unit,
    onFolderRequested: () -> Unit,
    onFrequencyChange: (AutoBackupFrequency) -> Unit,
    onMaxKeptBackupsChange: (Int) -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
) {
    SettingsChapter(
        title = "Còpies de seguretat",
        note = if (isAutoBackupEnabled) {
            "${lastAutoBackupSummary(lastSuccessAtEpochMillis).removeSuffix(".")} · " +
                nextAutoBackupSummary(lastSuccessAtEpochMillis, frequency)
        } else {
            "Ara només es fan còpies quan les demanes."
        },
    ) {
        SettingsGroup(label = "Automàtica") {
            SettingsSwitchRow(
                title = "Còpia automàtica",
                description = "Omnilog desa la biblioteca tot sol a la carpeta que triïs.",
                checked = isAutoBackupEnabled,
                onCheckedChange = onToggle,
            )
            if (isAutoBackupEnabled) {
                SettingsDivider()
                SettingsDropdownRow(
                    label = "Freqüència",
                    options = AutoBackupFrequency.entries,
                    selected = frequency,
                    optionLabel = { it.label },
                    onSelect = onFrequencyChange,
                )
                SettingsDivider()
                SettingsDropdownRow(
                    label = "Còpies desades",
                    description = "Les més antigues s'esborren soles.",
                    options = AutoBackupRetentionOptions,
                    selected = maxKeptBackups,
                    optionLabel = { "$it còpies" },
                    onSelect = onMaxKeptBackupsChange,
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Carpeta",
                    description = folderLabel ?: "Cap carpeta triada",
                    onClick = onFolderRequested,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel(text = "Manual")
            Row(
                modifier = Modifier.padding(horizontal = PanelMargin),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackupTile(
                    iconResId = R.drawable.ic_settings_export,
                    title = "Exporta",
                    description = "Desa-la en un fitxer",
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                )
                BackupTile(
                    iconResId = R.drawable.ic_settings_import,
                    title = "Restaura",
                    description = "Substitueix-la per una còpia",
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The account and its sync first, with the one strong action of the chapter; then what comes from
 * the account. Disconnecting sits under the panels, away from the everyday controls.
 */
@Composable
private fun MyAnimeListChapter(
    state: MalSyncState,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit,
    onImportAccount: () -> Unit,
    onShowPendingChanges: () -> Unit,
) {
    SettingsChapter(
        title = "MyAnimeList",
        note = "Un enviament en un sol sentit: les dades d'Omnilog no canvien mai.",
        leading = { ProviderLogo(source = ExternalRatingSource.Mal, height = 20.dp) },
    ) {
        if (!state.isConnected) {
            SettingsGroup(label = "Compte") {
                SettingsRow(
                    title = if (state.isAuthorizing) "Connectant…" else "Sense connectar",
                    description = state.error ?: if (state.isAvailable) {
                        "Connecta el compte perquè Omnilog hi enviï la teva llista d'anime."
                    } else {
                        "Cal configurar MAL_CLIENT_ID per connectar el compte."
                    },
                )
                SettingsButtons {
                    OmnilogPrimaryButton(
                        text = "Connecta",
                        onClick = onConnect,
                        enabled = state.isAvailable && !state.isAuthorizing,
                    )
                }
            }
            return@SettingsChapter
        }

        SettingsGroup(label = "Sincronització") {
            SettingsRow(
                title = state.accountName?.let { "Connectat com a $it" } ?: "Compte connectat",
                description = malSyncDescription(state),
            )
            SettingsButtons {
                OmnilogPrimaryButton(
                    text = if (state.isSyncEnabled) "Sincronitza ara" else "Activa la sincronització",
                    onClick = if (state.isSyncEnabled) onRetry else onSync,
                    enabled = !state.isSyncing,
                )
                OmnilogTonalButton(
                    text = "Envia-ho tot",
                    onClick = onSync,
                    enabled = !state.isSyncing,
                )
            }
            if (state.changes.isNotEmpty()) {
                SettingsDivider()
                SettingsActionRow(
                    title = "Canvis pendents",
                    description = malPendingChangesSummary(state),
                    onClick = onShowPendingChanges,
                    tone = if (state.failedCount > 0) OmnilogTheme.accents.Dropped else null,
                )
            }
        }
        SettingsGroup(label = "Importació") {
            SettingsActionRow(
                title = if (state.isImporting) "Llegint MyAnimeList…" else "Importa des del compte",
                description = if (state.isImporting) {
                    "${state.importFetchedCount} animes llegits. Pots continuar usant Omnilog."
                } else {
                    "Previsualitza i afegeix la llista de ${state.accountName ?: "el compte"}."
                },
                onClick = onImportAccount,
                enabled = !state.isAuthorizing && !state.isImporting && !state.isSyncing,
            )
        }
        SettingsSecondaryButton(
            text = "Desconnecta MyAnimeList",
            onClick = onDisconnect,
            tone = OmnilogTheme.accents.Dropped,
            modifier = Modifier.padding(horizontal = HeaderGutter - 12.dp),
        )
    }
}

/** What is running now, then every file importer in one place, MyAnimeList's XML included. */
@Composable
private fun ImportsChapter(
    importEnrichmentState: ImportEnrichmentState,
    isMalImporting: Boolean,
    onOpenImportActivity: () -> Unit,
    onImportImdb: () -> Unit,
    onImportStoryGraph: () -> Unit,
    onImportMalXml: () -> Unit,
) {
    SettingsChapter(
        title = "Importacions",
        note = "Les importacions només afegeixen; no esborren res del que ja tens.",
    ) {
        SettingsGroup(label = "Activitat") {
            SettingsActionRow(
                title = "Activitat d'importació",
                description = importActivityDescription(importEnrichmentState),
                onClick = onOpenImportActivity,
            )
        }
        SettingsGroup(label = "Des d'un fitxer") {
            SettingsActionRow(
                title = "IMDb",
                description = stringResource(
                    R.string.settings_import_imdb_description,
                    stringResource(R.string.nav_movies_tv),
                ),
                onClick = onImportImdb,
                logoSource = ExternalRatingSource.Imdb,
            )
            SettingsDivider()
            SettingsActionRow(
                title = "StoryGraph",
                description = "Afegeix llibres i lectures des d'un fitxer CSV.",
                onClick = onImportStoryGraph,
                logoSource = ExternalRatingSource.StoryGraph,
            )
            SettingsDivider()
            SettingsActionRow(
                title = "MyAnimeList",
                description = "Una exportació XML desada, amb compte o sense.",
                onClick = onImportMalXml,
                enabled = !isMalImporting,
                logoSource = ExternalRatingSource.Mal,
            )
        }
    }
}

/**
 * Everything that shapes what providers put into the library: the bulk refresh, then the per-format
 * preferences — anime titles and Goodreads ratings — that decide how their data lands.
 */
@Composable
private fun MetadataChapter(
    refreshState: MetadataRefreshState,
    onRefreshAll: () -> Unit,
    onCancelRefresh: (Long) -> Unit,
    onShowHistory: () -> Unit,
    animeTitlePreference: AnimeTitlePreference,
    onAnimeTitlePreferenceChange: (AnimeTitlePreference) -> Unit,
    onApplyAnimeTitles: () -> Unit,
    askForGoodreadsRating: Boolean,
    onAskForGoodreadsRatingChange: (Boolean) -> Unit,
) {
    val activeRefresh = refreshState.activeRun
    SettingsChapter(
        title = "Metadades",
        note = "Els camps que has editat a mà sempre es conserven.",
    ) {
        SettingsGroup(label = "Actualització") {
            SettingsRow(
                title = if (activeRefresh == null) "Totes les metadades" else "Actualitzant…",
                description = if (activeRefresh == null) {
                    refreshState.latestCompletedRun?.let { "Última vegada: ${it.summary()}." }
                        ?: "Torna a llegir els elements vinculats a un proveïdor."
                } else {
                    "${activeRefresh.processedCount} de ${activeRefresh.totalCount} elements processats."
                },
            )
            if (activeRefresh != null && activeRefresh.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { activeRefresh.processedCount.toFloat() / activeRefresh.totalCount },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeaderGutter),
                    color = OmnilogTheme.accents.Dashboard,
                    trackColor = OmnilogTheme.colors.appLine,
                    drawStopIndicator = {},
                )
            }
            SettingsButtons {
                if (activeRefresh == null) {
                    OmnilogPrimaryButton(text = "Actualitza-ho tot", onClick = onRefreshAll)
                } else {
                    OmnilogTonalButton(
                        text = "Atura",
                        onClick = { onCancelRefresh(activeRefresh.runId) },
                        accent = OmnilogTheme.accents.Dropped,
                    )
                }
                if (refreshState.history.isNotEmpty()) {
                    OmnilogTonalButton(text = "Historial", onClick = onShowHistory)
                }
            }
        }
        SettingsGroup(label = stringResource(MediaSection.Anime.titleResId)) {
            SettingsDropdownRow(
                label = "Idioma dels títols",
                description = when (animeTitlePreference) {
                    AnimeTitlePreference.EnglishWithRomajiOriginal -> "Títol en anglès; el rōmaji queda com a original."
                    AnimeTitlePreference.KeepMalTitle -> "El títol principal que retorna MyAnimeList."
                },
                options = AnimeTitlePreference.entries,
                selected = animeTitlePreference,
                optionLabel = {
                    when (it) {
                        AnimeTitlePreference.EnglishWithRomajiOriginal -> "Anglès"
                        AnimeTitlePreference.KeepMalTitle -> "Com a MAL"
                    }
                },
                onSelect = onAnimeTitlePreferenceChange,
            )
            SettingsButtons {
                OmnilogTonalButton(text = "Aplica a tota la biblioteca", onClick = onApplyAnimeTitles)
            }
        }
        SettingsGroup(label = stringResource(MediaSection.Books.titleResId)) {
            SettingsSwitchRow(
                title = "Demana la nota de Goodreads",
                description = "En obrir un llibre que encara no en tingui.",
                checked = askForGoodreadsRating,
                onCheckedChange = onAskForGoodreadsRatingChange,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Building blocks
// ─────────────────────────────────────────────────────────────

/** A chapter: a large serif title and at most one line under it, then its groups. */
@Composable
private fun SettingsChapter(
    title: String,
    note: String? = null,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = HeaderGutter),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = OmnilogTheme.colors.appInk,
                )
            }
            note?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = OmnilogTheme.colors.appMuted)
            }
        }
        content()
    }
}

/** The small capitals that name a group, set just outside its panel. */
@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text.uppercase(OmnilogLocale),
        modifier = Modifier
            .padding(horizontal = HeaderGutter)
            .semantics { heading() },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = OmnilogTheme.colors.appMuted,
    )
}

/**
 * A named group of rows set straight on the page, ruled from the label and from each other; only
 * tiles that are things to tap in their own right ([BackupTile]) get a panel.
 */
@Composable
private fun SettingsGroup(
    label: String,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingsGroupLabel(text = label)
        SettingsDivider()
        Column(modifier = Modifier.fillMaxWidth(), content = content)
        footnote?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = HeaderGutter),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

private val PanelShape = RoundedCornerShape(14.dp)

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = HeaderGutter),
        color = OmnilogTheme.colors.appLine,
    )
}

/**
 * The shared row: optional mark, title and description, and whatever control sits at the end. The
 * title stays in the regular weight so it never competes with a chapter's heading.
 */
@Composable
private fun SettingsRow(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = HeaderGutter, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !enabled -> OmnilogTheme.colors.appMuted
                    tone != null -> tone
                    else -> OmnilogTheme.colors.appInk
                },
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        trailing()
    }
}

/**
 * @param affordance Chevron for rows that open another screen/sheet/system picker, None for rows
 *   that act immediately in place.
 * @param logoSource draws the provider's real mark ([ProviderLogo]) for rows that stand for a
 *   specific external service.
 * @param tone colours the title, for the rows whose consequence deserves a second look.
 */
@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    affordance: SettingsRowAffordance = SettingsRowAffordance.Chevron,
    logoSource: ExternalRatingSource? = null,
    tone: Color? = null,
) {
    SettingsRow(
        title = title,
        description = description,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        enabled = enabled,
        tone = tone,
        leading = logoSource?.let { source ->
            {
                Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    ProviderLogo(source = source, height = 22.dp)
                }
            }
        },
    ) {
        if (enabled && affordance == SettingsRowAffordance.Chevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/** The actions a group offers, under the row they act on. */
@Composable
private fun SettingsButtons(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.padding(start = HeaderGutter, end = HeaderGutter, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A text-only action outside the panels, for the rare step that should not draw the eye. */
@Composable
private fun SettingsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: Color = OmnilogTheme.accents.Dashboard,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = tone),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** A one-off backup: the mark, a verb and a line, as a tile rather than another row. */
@Composable
private fun BackupTile(
    iconResId: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = OmnilogTheme.accents.Dashboard
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(OmnilogTheme.colors.appPanel)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(PanelPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        title = title,
        description = description,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OmnilogTheme.colors.appInk,
                checkedTrackColor = OmnilogTheme.accents.Dashboard,
                checkedBorderColor = OmnilogTheme.accents.Dashboard,
                uncheckedThumbColor = OmnilogTheme.colors.appMuted,
                uncheckedTrackColor = OmnilogTheme.colors.appPanel,
                uncheckedBorderColor = OmnilogTheme.colors.appLine,
            ),
        )
    }
}

/**
 * A row whose current value sits at its end in the accent, opening a dropdown of the few options —
 * the value reads as text, not as another chip.
 */
@Composable
private fun <T> SettingsDropdownRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    description: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = OmnilogTheme.accents.Dashboard
    SettingsRow(
        title = label,
        description = description,
        modifier = Modifier.clickable(role = Role.DropdownList) { expanded = true },
    ) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = optionLabel(selected),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            OmnilogDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    OmnilogDropdownItem(
                        text = optionLabel(option),
                        selected = option == selected,
                        accent = accent,
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

/**
 * One toggle per section: the section's mark in a filled disc of its accent when it shows on
 * «Ara mateix», an empty outlined ring when hidden — fill versus outline, so the state does not rest
 * on colour alone.
 */
@Composable
private fun SectionVisibilityToggle(
    section: MediaSection,
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = section.themedAccent()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // As on the navigation bar: the mark grows slightly under the finger, and the fill, ring and
    // tint cross-fade rather than snap when the section is shown or hidden.
    val discScale by animateFloatAsState(if (pressed) 1.1f else 1f, tween(220), label = "sectionDiscScale")
    val fill by animateColorAsState(if (isVisible) accent.copy(alpha = 0.16f) else Color.Transparent, tween(220), label = "sectionFill")
    val ring by animateColorAsState(if (isVisible) Color.Transparent else OmnilogTheme.colors.appLine, tween(220), label = "sectionRing")
    val tint by animateColorAsState(if (isVisible) accent else OmnilogTheme.colors.appMuted, tween(220), label = "sectionTint")
    Column(
        modifier = modifier
            // No ripple: it would wash a rectangle over the disc and label alike. The disc answers
            // the tap instead.
            .toggleable(
                value = isVisible,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(discScale)
                .background(fill, CircleShape)
                .border(1.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(section.navIconResId),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(section.titleResId),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isVisible) FontWeight.Bold else FontWeight.Normal,
            color = if (isVisible) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThemeSwatch(
    option: ThemePreference,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = OmnilogTheme.accents.Dashboard
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // The preview gives a little under the finger, like a card pressed into the page, and the outline
    // eases onto the new choice.
    val previewScale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(220), label = "swatchScale")
    val outline by animateColorAsState(if (isSelected) accent else OmnilogTheme.colors.appLine, tween(220), label = "swatchOutline")
    val outlineWidth by animateDpAsState(if (isSelected) 2.dp else 1.dp, tween(220), label = "swatchOutlineWidth")
    Column(
        // No ripple, as with the section toggles: the preview answers the tap instead.
        modifier = modifier.selectable(
            selected = isSelected,
            interactionSource = interactionSource,
            indication = null,
            role = Role.RadioButton,
            onClick = onSelect,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .scale(previewScale),
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            border = BorderStroke(outlineWidth, outline),
        ) {
            when (option) {
                ThemePreference.Light -> ThemePreviewPane(isDark = false)
                ThemePreference.Dark -> ThemePreviewPane(isDark = true)
                ThemePreference.System -> Row(Modifier.fillMaxWidth()) {
                    ThemePreviewPane(isDark = false, modifier = Modifier.weight(1f))
                    ThemePreviewPane(isDark = true, modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = option.themeOptionLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThemePreviewPane(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = if (isDark) DarkPalette else LightPalette
    val accent = if (isDark) DarkAccents.Dashboard else LightAccents.Dashboard
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBackground)
            .padding(horizontal = 7.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ThemePreviewBar(color = palette.appPanel, widthFraction = 1f)
        ThemePreviewBar(color = accent, widthFraction = 0.5f)
        ThemePreviewBar(color = palette.appMuted, widthFraction = 0.8f)
    }
}

@Composable
private fun ThemePreviewBar(color: Color, widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(6.dp)
            .background(color, RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun ThemePreference.themeOptionLabel(): String = stringResource(
    when (this) {
        ThemePreference.System -> R.string.theme_option_system
        ThemePreference.Light -> R.string.theme_option_light
        ThemePreference.Dark -> R.string.theme_option_dark
    },
)

@Composable
private fun visibleSectionsSummary(hiddenSections: Set<MediaSection>): String {
    val total = MediaSection.entries.size
    val visible = total - hiddenSections.size
    return pluralStringResource(R.plurals.settings_visible_sections_summary, visible, visible, total)
}

@Composable
private fun lastAutoBackupSummary(epochMillis: Long?): String {
    if (epochMillis == null) return stringResource(R.string.settings_backup_last_never)

    val elapsed = Duration.between(Instant.ofEpochMilli(epochMillis), Instant.now())
    val minutes = elapsed.toMinutes()
    val hours = elapsed.toHours()
    val days = elapsed.toDays()

    return when {
        elapsed.isNegative || minutes < 1L -> stringResource(R.string.settings_backup_last_just_now)
        minutes < 60L -> pluralStringResource(
            R.plurals.settings_backup_last_minutes,
            minutes.toInt(),
            minutes.toInt(),
        )
        hours < 24L -> pluralStringResource(
            R.plurals.settings_backup_last_hours,
            hours.toInt(),
            hours.toInt(),
        )
        days <= 7L -> pluralStringResource(
            R.plurals.settings_backup_last_days,
            days.toInt(),
            days.toInt(),
        )
        else -> {
            val date = Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            stringResource(
                R.string.settings_backup_last_on,
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            )
        }
    }
}

/** Absolute date rather than a running countdown — a wrong "d'aquí a 12 dies" is worse than none. */
private fun nextAutoBackupSummary(lastSuccessAtEpochMillis: Long?, frequency: AutoBackupFrequency): String {
    if (lastSuccessAtEpochMillis == null) return "Es calcularà després de la primera còpia."
    val nextEpochMillis = lastSuccessAtEpochMillis + Duration.ofDays(frequency.intervalDays).toMillis()
    val date = Instant.ofEpochMilli(nextEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return "Propera còpia: ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
}

@Composable
private fun malSyncDescription(state: MalSyncState): String = when {
    state.isSyncing -> "Sincronitzant ${state.pendingCount} canvis…"
    !state.isSyncEnabled -> "Toca per revisar i activar el primer enviament des d'Omnilog."
    state.failedCount > 0 -> "${state.failedCount} canvis necessiten atenció. Toca per reintentar."
    state.pendingCount > 0 -> "${state.pendingCount} canvis pendents. Toca per sincronitzar ara."
    state.lastSuccessAtEpochMillis != null -> "Al dia · última sincronització: ${formatRunTimestamp(state.lastSuccessAtEpochMillis)}"
    else -> "Connectat. Toca per enviar la biblioteca d'anime."
}

private fun malSyncChangeDescription(change: MalSyncChange): String = if (change.isFailed) {
    buildString {
        append("Error · MAL #${change.malId}")
        change.error?.takeIf { it.isNotBlank() }?.let { append(" · ${it.take(120)}") }
    }
} else {
    "Pendent · MAL #${change.malId}"
}

/** One-line count for the row that opens the pending-changes sheet. */
private fun malPendingChangesSummary(state: MalSyncState): String = buildString {
    append("${state.changes.size} a enviar a MyAnimeList")
    if (state.failedCount > 0) append(" · ${state.failedCount} amb incidències")
    append(". Toca per revisar-los i reintentar.")
}

internal fun importActivityDescription(state: ImportEnrichmentState): String {
    val active = state.activeBatches.ifEmpty { listOfNotNull(state.activeBatch) }
    if (active.isNotEmpty()) {
        val processed = active.sumOf { it.processedCount }
        val total = active.sumOf { it.totalCount }
        val activity = when {
            active.size > 1 -> "${active.size} importacions en curs o en pausa"
            active.single().state == ImportBatchState.Paused -> "1 importació en pausa"
            else -> "1 importació en curs"
        }
        return "$activity · $processed/$total processats. Obre per veure i gestionar cada importació."
    }

    val reviewCount = state.reviewItems.size
    val issueCount = state.issueItems.size
    val coverageCount = state.coverageItems.size
    if (reviewCount + issueCount + coverageCount > 0) {
        return buildString {
            append("Hi ha accions pendents")
            if (reviewCount > 0) append(" · $reviewCount per revisar")
            if (issueCount > 0) append(" · $issueCount incidències")
            if (coverageCount > 0) append(" · $coverageCount totals de seguiment")
            append('.')
        }
    }
    return "Consulta el progrés, les decisions pendents i l'historial de les importacions."
}

private fun MetadataRefreshRunState.label(): String = when (this) {
    MetadataRefreshRunState.Refreshing -> "En curs"
    MetadataRefreshRunState.Completed -> "Completada"
    MetadataRefreshRunState.CompletedWithIssues -> "Completada amb incidències"
    MetadataRefreshRunState.Cancelled -> "Cancel·lada"
}

private fun MetadataRefreshProgress.summary(): String = buildString {
    append("$appliedCount actualitzats")
    if (unchangedCount > 0) append(", $unchangedCount sense canvis")
    if (skippedCount > 0) append(", $skippedCount omesos")
    if (failedCount > 0) append(", $failedCount amb incidències")
    append(" de $totalCount")
}

private fun formatRunTimestamp(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataRefreshHistorySheet(
    history: List<MetadataRefreshProgress>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = OmnilogTheme.colors.appPanel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DetailGutter),
        ) {
            DetailSectionTitle(text = "Historial d'actualitzacions", modifier = Modifier.semantics { heading() })
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                history.forEachIndexed { index, run ->
                    if (index > 0) {
                        HorizontalDivider(color = OmnilogTheme.colors.appLine)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = run.state.label(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = OmnilogTheme.colors.appInk,
                            )
                            Text(
                                text = formatRunTimestamp(
                                    run.completedAtEpochMillis ?: run.createdAtEpochMillis,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                        Text(
                            text = run.summary(),
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * The MAL sync queue, moved off the settings surface into its own sheet. Inline, the list needed a
 * fixed-height scroll box wedged between two action rows, which fought the page's own scroll and
 * capped how much of the queue you could see. Here it gets the room, and the retry/cancel actions
 * sit under it where they act on what you have just read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MalPendingChangesSheet(
    state: MalSyncState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // The list drives its own scroll; leftover at either end is consumed here rather than handed
    // up to the sheet, so scrolling the queue never drags the sheet or its pinned actions.
    val keepScrollInList = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogTheme.colors.appPanel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = DetailGutter),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProviderLogo(source = ExternalRatingSource.Mal, height = 18.dp)
                    DetailSectionTitle(text = "Canvis pendents", modifier = Modifier.semantics { heading() })
                }
                Text(
                    text = "S'enviaran a MyAnimeList amb l'estat que tenen ara a Omnilog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(keepScrollInList)
                    .verticalScroll(rememberScrollState()),
            ) {
                state.changes.forEachIndexed { index, change ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = DetailGutter), color = OmnilogTheme.colors.appLine)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DetailGutter, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = change.animeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        Text(
                            text = malSyncChangeDescription(change),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (change.isFailed) {
                                OmnilogTheme.accents.Dropped
                            } else {
                                OmnilogTheme.colors.appMuted
                            },
                        )
                    }
                }
            }
            HorizontalDivider(color = OmnilogTheme.colors.appLine)
            Column(modifier = Modifier.padding(horizontal = DetailGutter - HeaderGutter)) {
                SettingsActionRow(
                    title = "Reintenta els canvis",
                    description = "Els que ja estan sincronitzats no es tornen a enviar.",
                    onClick = {
                        onDismiss()
                        onRetry()
                    },
                    enabled = !state.isSyncing,
                    affordance = SettingsRowAffordance.None,
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Cancel·la la cua",
                    description = "Elimina aquests canvis sense modificar les dades d'Omnilog.",
                    onClick = {
                        onDismiss()
                        onCancel()
                    },
                    affordance = SettingsRowAffordance.None,
                    tone = OmnilogTheme.accents.Dropped,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * The page closes on what Omnilog promises about the library, then who supplies the metadata, then
 * the colophon in serif. TMDB's terms prescribe its notice word for word, so that line stays in English.
 */
@Composable
private fun SettingsAboutFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HeaderGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.25f),
            color = OmnilogTheme.colors.appLine,
        )
        Text(
            text = "La biblioteca es queda en aquest telèfon. Si connectes MyAnimeList, " +
                "només s'hi envia l'estat actual dels teus animes.",
            style = MaterialTheme.typography.bodySmall,
            color = OmnilogTheme.colors.appMuted,
        )
        Text(
            text = "Les metadades, valoracions i portades vénen d'AniList, MyAnimeList, TMDB, OMDb, " +
                "Open Library, Google Books, RAWG, IGDB i Steam. Pertanyen als seus propietaris.",
            style = MaterialTheme.typography.bodySmall,
            color = OmnilogTheme.colors.appMuted,
        )
        Text(
            text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
            style = MaterialTheme.typography.bodySmall,
            color = OmnilogTheme.colors.appMuted,
        )
        Text(
            text = "Omnilog · Versió ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SerifFontFamily),
            color = OmnilogTheme.colors.appMuted,
        )
    }
}
