package com.nilpo.contenttracker.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.backup.AutoBackupFrequency
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.ImportEnrichmentState
import com.nilpo.contenttracker.core.imports.AnimeTitlePreference
import com.nilpo.contenttracker.core.mal.MalSyncState
import com.nilpo.contenttracker.core.mal.MalSyncChange
import com.nilpo.contenttracker.ui.common.ActiveSectionChips
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

// Settings used to run at 8-10.dp while Home and Perfil breathe at 16, which was the single
// biggest reason the screen read as imported from another app. One pair of constants now, so the
// rows cannot drift apart from each other again.
private val RowPaddingHorizontal = 16.dp
private val RowPaddingVertical = 14.dp

/**
 * Preferences, backups, and imports.
 *
 * Every row here used to carry a coloured icon tile, which left twenty near-identical badges
 * competing for the same attention and spent the media accents on decoration: export was
 * `Dashboard`, import `Tv`, restore `Completed`, though the three are the same kind of action.
 * Colour is now reserved for the two places it carries meaning — the media accents on the Ara
 * mateix chips, which name real shelves, and the backup card's status, which is the one thing on
 * the screen that can be wrong.
 */
@Composable
fun SettingsScreen(
    askForGoodreadsRating: Boolean,
    onAskForGoodreadsRatingChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onImportMyAnimeListAccount: () -> Unit,
    onImportMyAnimeListXml: () -> Unit,
    onImportImdbCsv: () -> Unit,
    onImportStoryGraphCsv: () -> Unit,
    isAutoBackupEnabled: Boolean,
    autoBackupFrequency: AutoBackupFrequency,
    lastAutoBackupAtEpochMillis: Long?,
    onAutoBackupFolderRequested: () -> Unit,
    onAutoBackupFrequencyChange: (AutoBackupFrequency) -> Unit,
    onAutoBackupDisabled: () -> Unit,
    malSyncState: MalSyncState,
    importEnrichmentState: ImportEnrichmentState,
    animeTitlePreference: AnimeTitlePreference,
    onAnimeTitlePreferenceChange: (AnimeTitlePreference) -> Unit,
    onBulkRefreshAnimeTitles: () -> Unit,
    onToggleImportEnrichment: () -> Unit,
    onRetryImportEnrichment: (Long) -> Unit,
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

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SettingsSection(title = "Aparença i inici") {
                    SettingsPanel {
                        SettingsThemeRow(
                            selected = themePreference,
                            onSelect = { themePreferences.writeThemePreference(it) },
                        )
                        SettingsDivider()
                        SettingsChipsRow(
                            title = "Seccions visibles a Ara mateix",
                            description = visibleSectionsSummary(hiddenActiveSections),
                        ) {
                            ActiveSectionChips(
                                hiddenSections = hiddenActiveSections,
                                onToggleSection = { section ->
                                    activeFilterPreferences.writeHiddenActiveSections(
                                        if (section in hiddenActiveSections) {
                                            hiddenActiveSections - section
                                        } else {
                                            hiddenActiveSections + section
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Preferències") {
                    SettingsPanel {
                        SettingsSwitchRow(
                            title = "Nota de Goodreads",
                            description = "Quan obris un llibre que encara no en tingui, " +
                                "Omnilog et demanarà la nota.",
                            checked = askForGoodreadsRating,
                            onCheckedChange = onAskForGoodreadsRatingChange,
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
                            onFolderRequested = onAutoBackupFolderRequested,
                            onFrequencyChange = onAutoBackupFrequencyChange,
                            onDisabled = onAutoBackupDisabled,
                        )
                        SettingsPanel {
                            SettingsActionRow(
                                title = "Exporta la biblioteca",
                                description = stringResource(R.string.settings_export_backup_description),
                                onClick = onExportBackup,
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                title = "Importa una còpia",
                                description = stringResource(R.string.settings_import_backup_description),
                                onClick = onImportBackup,
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                title = "Restaura una còpia anterior",
                                description = stringResource(R.string.settings_restore_backup_description),
                                onClick = onRestoreBackup,
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "MyAnimeList") {
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
                                iconResId = MediaSection.Anime.navIconResId,
                                accent = OmnilogTheme.accents.Anime,
                            )
                            malSyncState.changes.forEach { change ->
                                SettingsDivider()
                                SettingsActionRow(
                                    title = change.animeTitle,
                                    description = malSyncChangeDescription(change),
                                    onClick = {},
                                    enabled = false,
                                )
                            }
                            if (malSyncState.changes.isNotEmpty()) {
                                SettingsDivider()
                                SettingsActionRow(
                                    title = "Reintenta només aquests canvis",
                                    description = "No es tornaran a enviar els animes que ja estan sincronitzats.",
                                    onClick = onRetryMyAnimeList,
                                    enabled = !malSyncState.isSyncing,
                                    accent = OmnilogTheme.accents.Anime,
                                )
                                SettingsDivider()
                                SettingsActionRow(
                                    title = "Cancel·la els canvis pendents",
                                    description = "Elimina aquesta cua sense modificar les dades d'Omnilog.",
                                    onClick = onCancelMyAnimeList,
                                    accent = OmnilogTheme.accents.Dropped,
                                )
                            }
                            SettingsDivider()
                            SettingsActionRow(
                                title = "Desconnecta MyAnimeList",
                                description = "Atura els enviaments. Les dades d'Omnilog no canviaran.",
                                onClick = onDisconnectMyAnimeList,
                                accent = OmnilogTheme.accents.Dropped,
                            )
                        } else {
                            SettingsActionRow(
                                title = if (malSyncState.isAuthorizing) "Connectant…" else "Connecta MyAnimeList",
                                description = malSyncState.error
                                    ?: "Omnilog podrà actualitzar la llista d'anime amb les teves dades locals.",
                                onClick = onConnectMyAnimeList,
                                enabled = malSyncState.isAvailable && !malSyncState.isAuthorizing,
                                iconResId = MediaSection.Anime.navIconResId,
                                accent = OmnilogTheme.accents.Anime,
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Importa d'altres serveis") {
                    SettingsPanel {
                        SettingsActionRow(
                            title = "Idioma dels títols d'anime",
                            description = when (animeTitlePreference) {
                                AnimeTitlePreference.EnglishWithJapaneseOriginal ->
                                    "Títol principal en anglès i títol original en japonès."
                                AnimeTitlePreference.KeepMalTitle ->
                                    "Conserva el títol principal que retorna MyAnimeList."
                            },
                            onClick = {
                                onAnimeTitlePreferenceChange(
                                    if (animeTitlePreference == AnimeTitlePreference.EnglishWithJapaneseOriginal) {
                                        AnimeTitlePreference.KeepMalTitle
                                    } else {
                                        AnimeTitlePreference.EnglishWithJapaneseOriginal
                                    },
                                )
                            },
                            iconResId = MediaSection.Anime.navIconResId,
                            accent = OmnilogTheme.accents.Anime,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Aplica l'idioma a la biblioteca",
                            description = "Actualitza en bloc els animes vinculats a MAL. Els títols editats manualment continuen protegits.",
                            onClick = onBulkRefreshAnimeTitles,
                            iconResId = MediaSection.Anime.navIconResId,
                            accent = OmnilogTheme.accents.Anime,
                        )
                        SettingsDivider()
                        val enrichment = importEnrichmentState.activeBatch
                            ?: importEnrichmentState.recentBatches.firstOrNull {
                                it.issueCount > 0 || it.needsReviewCount > 0 ||
                                    it.coverageGapCount > 0
                            }
                        if (enrichment != null) {
                            val isActive = enrichment.state == ImportBatchState.Enriching ||
                                enrichment.state == ImportBatchState.Paused
                            SettingsActionRow(
                                title = when (enrichment.state) {
                                    ImportBatchState.Enriching -> "Completant metadades…"
                                    ImportBatchState.Paused -> "Metadades en pausa"
                                    ImportBatchState.CompletedWithIssues -> "Metadades completades amb avisos"
                                    else -> "Metadades importades"
                                },
                                description = buildString {
                                    append("${enrichment.processedCount}/${enrichment.totalCount} processats")
                                    append(" · ${enrichment.appliedCount} completats")
                                    if (enrichment.needsReviewCount > 0) {
                                        append(" · ${enrichment.needsReviewCount} per revisar")
                                    }
                                    if (enrichment.issueCount > 0) {
                                        append(" · ${enrichment.issueCount} amb incidències")
                                    }
                                    if (enrichment.coverageGapCount > 0) {
                                        append(" · ${enrichment.coverageGapCount} amb camps buits")
                                    }
                                    when (enrichment.state) {
                                        ImportBatchState.Enriching -> append(". Toca per posar en pausa.")
                                        ImportBatchState.Paused -> append(". Toca per continuar.")
                                        ImportBatchState.CompletedWithIssues -> if (enrichment.issueCount > 0) {
                                            append(". Toca per reintentar les incidències.")
                                        }
                                        else -> Unit
                                    }
                                },
                                onClick = if (isActive) {
                                    onToggleImportEnrichment
                                } else {
                                    { onRetryImportEnrichment(enrichment.batchId) }
                                },
                                enabled = isActive || enrichment.issueCount > 0,
                                iconResId = MediaSection.Anime.navIconResId,
                                accent = OmnilogTheme.accents.Anime,
                            )
                            SettingsDivider()
                        }
                        SettingsActionRow(
                            title = when {
                                malSyncState.isImporting -> "Llegint MyAnimeList…"
                                malSyncState.isConnected -> "Importa des del compte de MyAnimeList"
                                else -> "Connecta MyAnimeList per importar"
                            },
                            description = when {
                                malSyncState.isImporting ->
                                    "${malSyncState.importFetchedCount} animes llegits. Pots continuar usant Omnilog."
                                malSyncState.isConnected ->
                                    "Previsualitza i afegeix la llista del compte ${malSyncState.accountName.orEmpty()}."
                                malSyncState.isAvailable ->
                                    "Connecta el compte per importar la llista sense descarregar cap fitxer."
                                else -> "Cal configurar MAL_CLIENT_ID; l'importador XML continua disponible."
                            },
                            onClick = if (malSyncState.isConnected) {
                                onImportMyAnimeListAccount
                            } else {
                                onConnectMyAnimeList
                            },
                            enabled = malSyncState.isAvailable &&
                                !malSyncState.isAuthorizing &&
                                !malSyncState.isImporting &&
                                !malSyncState.isSyncing,
                            iconResId = MediaSection.Anime.navIconResId,
                            accent = OmnilogTheme.accents.Anime,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Fitxer XML de MyAnimeList",
                            description = "Alternativa per a exportacions desades o comptes desconnectats.",
                            onClick = onImportMyAnimeListXml,
                            enabled = !malSyncState.isImporting,
                            iconResId = MediaSection.Anime.navIconResId,
                            accent = OmnilogTheme.accents.Anime,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "IMDb",
                            description = stringResource(
                                R.string.settings_import_imdb_description,
                                stringResource(R.string.nav_movies_tv),
                            ),
                            onClick = onImportImdbCsv,
                            iconResId = MediaSection.Movies.navIconResId,
                            accent = OmnilogTheme.accents.Tv,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "StoryGraph",
                            description = "Afegeix llibres i lectures des d'un fitxer CSV.",
                            onClick = onImportStoryGraphCsv,
                            iconResId = MediaSection.Books.navIconResId,
                            accent = OmnilogTheme.accents.Books,
                        )
                    }
                }
            }
            item { SettingsAboutFooter() }
        }
    }
}

/**
 * The section heading Home uses: the title, then a hairline running out to the right margin.
 *
 * Settings used to head its groups with small ExtraBold uppercase labels, an idiom that appears
 * nowhere else in Omnilog. Borrowing Home's shape is what makes the screen look like it belongs to
 * the same app; see `DashboardSectionTitle`.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
        content()
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

/** Inset to the rows' own padding, now that no leading icon sets a deeper margin. */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = RowPaddingHorizontal),
        color = OmnilogTheme.colors.appLine,
    )
}

/**
 * Title over an optional description, the shared body of every row on this screen.
 *
 * The description is deliberately unbounded. Every row used to be `maxLines = 1`, which cut the
 * Catalan strings off before they explained anything — the export row read "Desa tota la
 * biblioteca en un fitxer…" and stopped. A settings row that cannot finish its sentence is not
 * worth the line it saves.
 */
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
 * @param iconResId an optional leading mark. The backup rows leave it null — export, import and
 *   restore are the same kind of action and colouring them apart said nothing. The import-service
 *   rows set it to the medium each service brings in, which is the one thing that distinguishes
 *   them. Swap in an official brand logo here if you ever have the rights to ship one.
 */
@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconResId: Int? = null,
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
        if (iconResId != null) {
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
        if (enabled) {
            // Muted, not accented. The arrow says "this opens something", which is the same thing
            // on every row; colouring six of them would spend the accent on an affordance.
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
    description: String,
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = OmnilogTheme.colors.appInk,
                checkedTrackColor = OmnilogTheme.accents.Dashboard,
                checkedBorderColor = OmnilogTheme.accents.Dashboard,
                uncheckedThumbColor = OmnilogTheme.colors.appMuted,
                uncheckedTrackColor = OmnilogTheme.colors.appBackground,
                uncheckedBorderColor = OmnilogTheme.colors.appLine,
            ),
        )
    }
}

/** A row whose control is too wide to sit beside its label, so it sits under it. */
@Composable
private fun SettingsChipsRow(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsRowText(title = title, description = description)
        content()
    }
}

/**
 * Theme selector, as three miniature screens rather than three words.
 *
 * Each swatch is painted from the palette it stands for — [LightPalette] on the left of the
 * Sistema tile, [DarkPalette] on the right — so the choice is shown rather than named. The bars
 * repeat the app's own stack: a panel over the page, then the Dashboard accent, then a line of
 * body text.
 */
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
                // Sistema follows the device, so it is drawn as both at once rather than as a
                // third look the app does not actually have.
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

/** One half — or all — of a swatch, painted in the fixed palette it advertises. */
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

/** "Se'n mostren 4 de 5", so the row says where it stands without opening anything. */
@Composable
private fun visibleSectionsSummary(hiddenSections: Set<MediaSection>): String {
    val total = MediaSection.entries.size
    val visible = total - hiddenSections.size
    return pluralStringResource(R.plurals.settings_visible_sections_summary, visible, visible, total)
}

/**
 * When the last automatic backup actually landed, in the coarsest unit that still says something.
 *
 * Anything older than a week is given as a date instead of a running count: past that point "fa 43
 * dies" is harder to place than the day itself, and the number stops being reassuring anyway.
 */
@Composable
private fun lastAutoBackupSummary(epochMillis: Long?): String {
    if (epochMillis == null) return stringResource(R.string.settings_backup_last_never)

    val elapsed = Duration.between(Instant.ofEpochMilli(epochMillis), Instant.now())
    val minutes = elapsed.toMinutes()
    val hours = elapsed.toHours()
    val days = elapsed.toDays()

    return when {
        // A clock that has moved backwards — a timezone change, a manually set date — would
        // otherwise render "fa -3 hores". Treat any future stamp as just-now.
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

/**
 * The one card on the screen that carries an accent, because it is the one thing here that has a
 * state worth noticing: whether the library is currently being copied anywhere.
 */
@Composable
private fun AutoBackupCard(
    isAutoBackupEnabled: Boolean,
    selectedFrequency: AutoBackupFrequency,
    lastSuccessAtEpochMillis: Long?,
    onFolderRequested: () -> Unit,
    onFrequencyChange: (AutoBackupFrequency) -> Unit,
    onDisabled: () -> Unit,
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
                    // The pill says whether it is on; this says whether it is actually working.
                    // Those are different facts — a backup scheduled weeks ago that never ran
                    // still shows ACTIVA — so the recency line is the one worth the room.
                    description = if (isAutoBackupEnabled) {
                        lastAutoBackupSummary(lastSuccessAtEpochMillis)
                    } else {
                        "Tria una carpeta i Omnilog hi desarà la biblioteca tot sol."
                    },
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusAccent.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = if (isAutoBackupEnabled) "ACTIVA" else "INACTIVA",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusAccent,
                        maxLines = 1,
                    )
                }
            }
            SettingsDivider()
            Row(
                modifier = Modifier.padding(
                    horizontal = RowPaddingHorizontal,
                    vertical = 10.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Freqüència",
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AutoBackupFrequency.entries.forEach { frequency ->
                        val isSelected = frequency == selectedFrequency
                        Text(
                            text = frequency.label,
                            modifier = Modifier
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { onFrequencyChange(frequency) },
                                )
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) statusAccent else OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
            SettingsDivider()
            Row(
                modifier = Modifier.padding(
                    horizontal = RowPaddingHorizontal,
                    vertical = RowPaddingVertical,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isAutoBackupEnabled) "Canvia la carpeta" else "Tria una carpeta",
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onFolderRequested),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.accents.Dashboard,
                    maxLines = 1,
                )
                if (isAutoBackupEnabled) {
                    Text(
                        text = "Desactiva",
                        modifier = Modifier
                            .clickable(onClick = onDisabled)
                            .padding(start = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.accents.Dropped,
                        maxLines = 1,
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

/**
 * The screen's closing line rather than a section of its own.
 *
 * Privacy used to be stated twice — a pill above the first section and an info row down here — so
 * the pill is gone and this is the only place that says it. No panel either: there is nothing to
 * tap, and a card would have promised otherwise.
 */
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
