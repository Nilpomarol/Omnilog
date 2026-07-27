package com.nilpo.contenttracker.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.backup.AutoBackupFrequency
import com.nilpo.contenttracker.core.backup.AutoBackupRetentionOptions
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.ImportEnrichmentState
import com.nilpo.contenttracker.core.imports.AnimeTitlePreference
import com.nilpo.contenttracker.core.mal.MalSyncState
import com.nilpo.contenttracker.core.mal.MalSyncChange
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.refresh.MetadataRefreshProgress
import com.nilpo.contenttracker.core.refresh.MetadataRefreshRunState
import com.nilpo.contenttracker.core.refresh.MetadataRefreshState
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.common.rememberActiveFilterPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenActiveSections
import com.nilpo.contenttracker.ui.common.writeHiddenActiveSections
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.home.navIconResId
import com.nilpo.contenttracker.ui.theme.DarkAccents
import com.nilpo.contenttracker.ui.theme.DarkPalette
import com.nilpo.contenttracker.ui.theme.LightAccents
import com.nilpo.contenttracker.ui.theme.LightPalette
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.ThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreferences
import com.nilpo.contenttracker.ui.theme.writeThemePreference
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val RowPaddingHorizontal = 16.dp
private val RowPaddingVertical = 14.dp

/** Whether tapping a row opens something else, or acts immediately in place. */
private enum class SettingsRowAffordance { Chevron, None }

/**
 * Preferences, backups, imports and MyAnimeList — grouped by what they do, not by when they were
 * added, with one consistent rule: a chevron means the tap opens another screen, sheet or system
 * picker; no chevron means the tap does the thing right here.
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
    val activeFilterPreferences = rememberActiveFilterPreferences()
    val hiddenActiveSections by rememberHiddenActiveSections(activeFilterPreferences)
    val themePreferences = rememberThemePreferences()
    val themePreference by rememberThemePreference(themePreferences)
    var showMetadataHistory by remember { mutableStateOf(false) }
    var showMalPendingChanges by remember { mutableStateOf(false) }

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SettingsSection(title = "Aparença") {
                    SettingsPanel {
                        SettingsThemeRow(
                            selected = themePreference,
                            onSelect = { themePreferences.writeThemePreference(it) },
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Inici") {
                    SettingsPanel {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SettingsRowText(
                                title = "Seccions a «Ara mateix»",
                                description = "Tria quines seccions apareixen a la llista d'inici. " +
                                    "Toca'n una per amagar-la o tornar-la a mostrar. " +
                                    visibleSectionsSummary(hiddenActiveSections),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MediaSection.entries.forEach { section ->
                                    val isVisible = section !in hiddenActiveSections
                                    SectionVisibilityPill(
                                        section = section,
                                        isVisible = isVisible,
                                        onToggle = {
                                            activeFilterPreferences.writeHiddenActiveSections(
                                                if (isVisible) {
                                                    hiddenActiveSections + section
                                                } else {
                                                    hiddenActiveSections - section
                                                },
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Preferències") {
                    SettingsPanel {
                        SettingsSwitchRow(
                            title = "Nota de Goodreads",
                            description = "Desactivat per defecte. Si l'actives, Omnilog et " +
                                "demanarà la nota quan obris un llibre que encara no en tingui.",
                            checked = askForGoodreadsRating,
                            onCheckedChange = onAskForGoodreadsRatingChange,
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Metadades") {
                    val activeRefresh = metadataRefreshState.activeRun
                    SettingsPanel {
                        SettingsActionRow(
                            title = if (activeRefresh == null) {
                                "Actualitza totes les metadades"
                            } else {
                                "Atura l'actualització de metadades"
                            },
                            description = if (activeRefresh == null) {
                                buildString {
                                    append("Actualitza els elements vinculats a un proveïdor. Els camps editats manualment es conserven.")
                                    metadataRefreshState.latestCompletedRun?.let { lastRun ->
                                        append(" Última actualització: ${lastRun.appliedCount} actualitzats")
                                        if (lastRun.unchangedCount > 0) append(", ${lastRun.unchangedCount} sense canvis")
                                        if (lastRun.skippedCount > 0) append(", ${lastRun.skippedCount} omesos")
                                        if (lastRun.failedCount > 0) append(", ${lastRun.failedCount} amb incidències")
                                        append('.')
                                    }
                                }
                            } else {
                                "${activeRefresh.processedCount} de ${activeRefresh.totalCount} elements processats. " +
                                    "Toca per aturar-la."
                            },
                            onClick = {
                                activeRefresh?.let { refresh ->
                                    onCancelBulkMetadataRefresh(refresh.runId)
                                } ?: onBulkMetadataRefresh()
                            },
                            affordance = SettingsRowAffordance.None,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Veure historial",
                            description = if (metadataRefreshState.history.isEmpty()) {
                                "Encara no s'ha fet cap actualització de metadades."
                            } else {
                                "Consulta les últimes actualitzacions i el que ha canviat en cada una."
                            },
                            onClick = { showMetadataHistory = true },
                            enabled = metadataRefreshState.history.isNotEmpty(),
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Còpies de seguretat") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AutoBackupCard(
                            isAutoBackupEnabled = isAutoBackupEnabled,
                            selectedFrequency = autoBackupFrequency,
                            lastSuccessAtEpochMillis = lastAutoBackupAtEpochMillis,
                            folderLabel = autoBackupFolderLabel,
                            maxKeptBackups = maxKeptBackups,
                            onToggle = onAutoBackupToggle,
                            onFolderRequested = onAutoBackupFolderRequested,
                            onFrequencyChange = onAutoBackupFrequencyChange,
                            onMaxKeptBackupsChange = onMaxKeptBackupsChange,
                        )
                        SettingsPanel {
                            SettingsActionRow(
                                title = "Exporta la biblioteca",
                                description = stringResource(R.string.settings_export_backup_description),
                                onClick = onExportBackup,
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                title = "Restaura una còpia",
                                description = "Tria un fitxer o una còpia de seguretat anterior per recuperar la biblioteca.",
                                onClick = onRestoreBackup,
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(
                    title = "MyAnimeList",
                    leading = { ProviderLogo(source = ExternalRatingSource.Mal, height = 18.dp) },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsPanel {
                            if (malSyncState.isConnected) {
                                SettingsActionRow(
                                    title = malSyncState.accountName?.let { "Compte: $it" } ?: "Compte connectat",
                                    description = malSyncDescription(malSyncState),
                                    onClick = if (malSyncState.isSyncEnabled) {
                                        onRetryMyAnimeList
                                    } else {
                                        onSyncMyAnimeList
                                    },
                                    enabled = !malSyncState.isSyncing,
                                    affordance = SettingsRowAffordance.None,
                                    logoSource = ExternalRatingSource.Mal,
                                )
                                SettingsDivider()
                                SettingsActionRow(
                                    title = "Desconnecta MyAnimeList",
                                    description = "Atura els enviaments. Les dades d'Omnilog no canviaran.",
                                    onClick = onDisconnectMyAnimeList,
                                    affordance = SettingsRowAffordance.None,
                                    accent = OmnilogTheme.accents.Dropped,
                                )
                            } else {
                                SettingsActionRow(
                                    title = if (malSyncState.isAuthorizing) "Connectant…" else "Connecta MyAnimeList",
                                    description = malSyncState.error
                                        ?: "Omnilog podrà actualitzar la llista d'anime amb les teves dades locals.",
                                    onClick = onConnectMyAnimeList,
                                    enabled = malSyncState.isAvailable && !malSyncState.isAuthorizing,
                                    logoSource = ExternalRatingSource.Mal,
                                )
                            }
                        }
                        if (malSyncState.isConnected) {
                            SettingsPanel {
                                SettingsActionRow(
                                    title = "Sincronitza tota la biblioteca",
                                    description = "Envia a MyAnimeList l'estat actual dels animes amb identificador MAL.",
                                    onClick = onSyncMyAnimeList,
                                    enabled = !malSyncState.isSyncing,
                                    affordance = SettingsRowAffordance.None,
                                    accent = OmnilogTheme.accents.Anime,
                                )
                                if (malSyncState.changes.isNotEmpty()) {
                                    SettingsDivider()
                                    SettingsActionRow(
                                        title = "Canvis pendents de sincronitzar",
                                        description = malPendingChangesSummary(malSyncState),
                                        onClick = { showMalPendingChanges = true },
                                        accent = if (malSyncState.failedCount > 0) {
                                            OmnilogTheme.accents.Dropped
                                        } else {
                                            OmnilogTheme.accents.Anime
                                        },
                                    )
                                }
                            }
                            SettingsPanel {
                                SettingsActionRow(
                                    title = when {
                                        malSyncState.isImporting -> "Llegint MyAnimeList…"
                                        else -> "Importa des del compte de MyAnimeList"
                                    },
                                    description = when {
                                        malSyncState.isImporting ->
                                            "${malSyncState.importFetchedCount} animes llegits. Pots continuar usant Omnilog."
                                        else ->
                                            "Previsualitza i afegeix la llista del compte ${malSyncState.accountName.orEmpty()}."
                                    },
                                    onClick = onImportMyAnimeListAccount,
                                    enabled = !malSyncState.isAuthorizing && !malSyncState.isImporting && !malSyncState.isSyncing,
                                    logoSource = ExternalRatingSource.Mal,
                                )
                                SettingsDivider()
                                SettingsActionRow(
                                    title = "Fitxer XML de MyAnimeList",
                                    description = "Alternativa per a exportacions desades o comptes desconnectats.",
                                    onClick = onImportMyAnimeListXml,
                                    enabled = !malSyncState.isImporting,
                                    logoSource = ExternalRatingSource.Mal,
                                )
                            }
                        } else if (!malSyncState.isAvailable) {
                            SettingsPanel {
                                SettingsActionRow(
                                    title = "Fitxer XML de MyAnimeList",
                                    description = "Cal configurar MAL_CLIENT_ID per connectar el compte; l'importador XML continua disponible.",
                                    onClick = onImportMyAnimeListXml,
                                    logoSource = ExternalRatingSource.Mal,
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Preferències d'importació") {
                    SettingsPanel {
                        SettingsActionRow(
                            title = "Idioma dels títols d'anime",
                            description = when (animeTitlePreference) {
                                AnimeTitlePreference.EnglishWithRomajiOriginal ->
                                    "Títol principal en anglès i títol original en rōmaji."
                                AnimeTitlePreference.KeepMalTitle ->
                                    "Conserva el títol principal que retorna MyAnimeList."
                            },
                            onClick = {
                                onAnimeTitlePreferenceChange(
                                    if (animeTitlePreference == AnimeTitlePreference.EnglishWithRomajiOriginal) {
                                        AnimeTitlePreference.KeepMalTitle
                                    } else {
                                        AnimeTitlePreference.EnglishWithRomajiOriginal
                                    },
                                )
                            },
                            affordance = SettingsRowAffordance.None,
                            iconResId = MediaSection.Anime.navIconResId,
                            accent = OmnilogTheme.accents.Anime,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Aplica l'idioma a la biblioteca",
                            description = "Actualitza en bloc els animes vinculats a MAL. Els títols editats manualment continuen protegits.",
                            onClick = onBulkRefreshAnimeTitles,
                            affordance = SettingsRowAffordance.None,
                            iconResId = MediaSection.Anime.navIconResId,
                            accent = OmnilogTheme.accents.Anime,
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Importa d'altres serveis") {
                    SettingsPanel {
                        SettingsActionRow(
                            title = "Activitat d'importació",
                            description = importActivityDescription(importEnrichmentState),
                            onClick = onOpenImportActivity,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "IMDb",
                            description = stringResource(
                                R.string.settings_import_imdb_description,
                                stringResource(R.string.nav_movies_tv),
                            ),
                            onClick = onImportImdbCsv,
                            logoSource = ExternalRatingSource.Imdb,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "StoryGraph",
                            description = "Afegeix llibres i lectures des d'un fitxer CSV.",
                            onClick = onImportStoryGraphCsv,
                            logoSource = ExternalRatingSource.StoryGraph,
                        )
                    }
                }
            }
            item { SettingsAboutFooter() }
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

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = OmnilogTheme.colors.appLine,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
        content()
    }
}

/**
 * One equal-width pill per section, icon over label, so all four fit a single row inside the card.
 * A soft tint of the section's accent when it shows on Ara mateix; a plain muted outline when
 * hidden, so the on/off state reads without a separate switch.
 */
@Composable
private fun SectionVisibilityPill(
    section: MediaSection,
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = section.accent
    val contentColor = if (isVisible) accent else OmnilogTheme.colors.appMuted
    Surface(
        modifier = modifier.toggleable(
            value = isVisible,
            role = Role.Checkbox,
            onValueChange = { onToggle() },
        ),
        shape = RoundedCornerShape(14.dp),
        color = if (isVisible) accent.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isVisible) accent.copy(alpha = 0.42f) else OmnilogTheme.colors.appLine,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(section.navIconResId),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(section.titleResId),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsPanel(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OmnilogTheme.colors.appPanel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = RowPaddingHorizontal),
        color = OmnilogTheme.colors.appLine,
    )
}

@Composable
private fun SettingsRowText(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

/**
 * @param affordance Chevron for rows that open another screen/sheet/system picker, None for rows
 *   that act immediately in place — the only visual language on this screen for that distinction.
 * @param logoSource draws the provider's real mark ([ProviderLogo]) instead of a tinted app icon,
 *   for rows that represent a specific external service.
 */
@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    affordance: SettingsRowAffordance = SettingsRowAffordance.Chevron,
    iconResId: Int? = null,
    logoSource: ExternalRatingSource? = null,
    accent: Color = OmnilogTheme.accents.Dashboard,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (logoSource != null) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                ProviderLogo(source = logoSource, height = 22.dp)
            }
        } else if (iconResId != null) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = if (enabled) 0.14f else 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconResId),
                        contentDescription = null,
                        tint = accent.copy(alpha = if (enabled) 1f else 0.56f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        SettingsRowText(
            title = title,
            description = description,
            modifier = Modifier.weight(1f),
            enabled = enabled,
        )
        if (enabled && affordance == SettingsRowAffordance.Chevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
                modifier = Modifier.size(18.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowText(
            title = title,
            description = description,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = settingsSwitchColors(),
        )
    }
}

@Composable
private fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = OmnilogTheme.colors.appInk,
    checkedTrackColor = OmnilogTheme.accents.Dashboard,
    checkedBorderColor = OmnilogTheme.accents.Dashboard,
    uncheckedThumbColor = OmnilogTheme.colors.appMuted,
    uncheckedTrackColor = OmnilogTheme.colors.appBackground,
    uncheckedBorderColor = OmnilogTheme.colors.appLine,
)

@Composable
private fun SettingsThemeRow(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsRowText(title = "Tema de l'aplicació", description = null)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemePreference.entries.forEach { option ->
                ThemeSwatch(
                    option = option,
                    isSelected = option == selected,
                    onSelect = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
    Column(
        modifier = modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onSelect,
        ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) accent else OmnilogTheme.colors.appLine,
            ),
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
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) accent else OmnilogTheme.colors.appMuted,
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
            .padding(horizontal = 7.dp, vertical = 9.dp),
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
private fun AutoBackupCard(
    isAutoBackupEnabled: Boolean,
    selectedFrequency: AutoBackupFrequency,
    lastSuccessAtEpochMillis: Long?,
    folderLabel: String?,
    maxKeptBackups: Int,
    onToggle: (Boolean) -> Unit,
    onFolderRequested: () -> Unit,
    onFrequencyChange: (AutoBackupFrequency) -> Unit,
    onMaxKeptBackupsChange: (Int) -> Unit,
) {
    val statusAccent = if (isAutoBackupEnabled) {
        OmnilogTheme.accents.Completed
    } else {
        OmnilogTheme.accents.Dashboard
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OmnilogTheme.colors.appPanel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.26f)),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(
                    horizontal = RowPaddingHorizontal,
                    vertical = RowPaddingVertical,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsRowText(
                    title = "Còpia automàtica",
                    description = if (isAutoBackupEnabled) {
                        "${lastAutoBackupSummary(lastSuccessAtEpochMillis)} · " +
                            nextAutoBackupSummary(lastSuccessAtEpochMillis, selectedFrequency)
                    } else {
                        "Tria una carpeta i Omnilog hi desarà la biblioteca tot sol."
                    },
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isAutoBackupEnabled,
                    onCheckedChange = onToggle,
                    colors = settingsSwitchColors(),
                )
            }
            if (isAutoBackupEnabled) {
                SettingsDivider()
                SettingsDropdownRow(
                    label = "Freqüència",
                    options = AutoBackupFrequency.entries,
                    selected = selectedFrequency,
                    optionLabel = { it.label },
                    onSelect = onFrequencyChange,
                    accent = statusAccent,
                )
                SettingsDivider()
                SettingsDropdownRow(
                    label = "Còpies desades",
                    description = "Es conserven les $maxKeptBackups còpies més recents; les més " +
                        "antigues s'esborren soles de la carpeta.",
                    options = AutoBackupRetentionOptions,
                    selected = maxKeptBackups,
                    optionLabel = { "$it còpies" },
                    onSelect = onMaxKeptBackupsChange,
                    accent = statusAccent,
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Carpeta de còpies de seguretat",
                    description = folderLabel ?: "Cap carpeta triada",
                    onClick = onFolderRequested,
                )
            }
        }
    }
}

/**
 * A labelled row that opens a dropdown of options — the compact, unambiguous control for a short
 * fixed list where chips took a whole scrolling row and read as a filter.
 */
@Composable
private fun <T> SettingsDropdownRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    accent: Color,
    description: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        Box {
            Surface(
                modifier = Modifier.clickable { expanded = true },
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = optionLabel(selected),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
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

@Composable
private fun malSyncDescription(state: MalSyncState): String = when {
    state.isSyncing -> "Sincronitzant ${state.pendingCount} canvis…"
    !state.isSyncEnabled -> "Toca per revisar i activar el primer enviament des d'Omnilog."
    state.failedCount > 0 -> "${state.failedCount} canvis necessiten atenció. Toca per reintentar."
    state.pendingCount > 0 -> "${state.pendingCount} canvis pendents. Toca per sincronitzar ara."
    state.lastSuccessAtEpochMillis != null -> "Al dia · ${lastAutoBackupSummary(state.lastSuccessAtEpochMillis)}"
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Historial d'actualitzacions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(
                    text = "Encara no s'ha fet cap actualització de metadades.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
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
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = run.state.label(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
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
                .fillMaxHeight(0.82f)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProviderLogo(source = ExternalRatingSource.Mal, height = 18.dp)
                Text(
                    text = "Canvis pendents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Aquests animes s'enviaran a MyAnimeList amb l'estat que tenen ara a " +
                    "Omnilog. És un enviament en un sol sentit: les dades d'Omnilog no canvien.",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(keepScrollInList)
                    .verticalScroll(rememberScrollState()),
            ) {
                state.changes.forEachIndexed { index, change ->
                    if (index > 0) {
                        HorizontalDivider(color = OmnilogTheme.colors.appLine)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = change.animeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
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
            Spacer(modifier = Modifier.height(12.dp))
            SettingsPanel {
                SettingsActionRow(
                    title = "Reintenta els canvis",
                    description = "Torna a enviar-los a MyAnimeList. Els que ja estan sincronitzats no es tornen a enviar.",
                    onClick = {
                        onDismiss()
                        onRetry()
                    },
                    enabled = !state.isSyncing,
                    affordance = SettingsRowAffordance.None,
                    accent = OmnilogTheme.accents.Anime,
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Cancel·la la cua",
                    description = "Elimina aquests canvis pendents sense modificar les dades d'Omnilog.",
                    onClick = {
                        onDismiss()
                        onCancel()
                    },
                    affordance = SettingsRowAffordance.None,
                    accent = OmnilogTheme.accents.Dropped,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsAboutFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
            text = "Omnilog · Versió 1.0",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}
