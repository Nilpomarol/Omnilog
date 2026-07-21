package com.nilpo.contenttracker.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.backup.AutoBackupFrequency
import com.nilpo.contenttracker.ui.common.ActiveSectionChips
import com.nilpo.contenttracker.ui.common.rememberActiveFilterPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenActiveSections
import com.nilpo.contenttracker.ui.common.writeHiddenActiveSections
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.ThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreferences
import com.nilpo.contenttracker.ui.theme.writeThemePreference

@Composable
fun SettingsScreen(
    askForGoodreadsRating: Boolean,
    onAskForGoodreadsRatingChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onImportMyAnimeListXml: () -> Unit,
    onImportImdbCsv: () -> Unit,
    onImportStoryGraphCsv: () -> Unit,
    isAutoBackupEnabled: Boolean,
    autoBackupFrequency: AutoBackupFrequency,
    onAutoBackupFolderRequested: () -> Unit,
    onAutoBackupFrequencyChange: (AutoBackupFrequency) -> Unit,
    onAutoBackupDisabled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeFilterPreferences = rememberActiveFilterPreferences()
    val hiddenActiveSections by rememberHiddenActiveSections(activeFilterPreferences)
    val themePreferences = rememberThemePreferences()
    val themePreference by rememberThemePreference(themePreferences)

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SettingsHero() }
            item {
                SettingsSection(title = "Aparença i inici") {
                    SettingsPanel {
                        SettingsThemeRow(
                            selected = themePreference,
                            onSelect = { themePreferences.writeThemePreference(it) },
                        )
                        SettingsDivider()
                        SettingsChipsRow(
                            icon = Icons.Filled.Home,
                            accent = OmnilogTheme.accents.Dashboard,
                            title = "Seccions visibles a Ara mateix",
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
                            icon = Icons.Filled.Star,
                            accent = OmnilogTheme.accents.Books,
                            title = "Nota de Goodreads",
                            description = "Pregunta la nota quan importes llibres.",
                            checked = askForGoodreadsRating,
                            onCheckedChange = onAskForGoodreadsRatingChange,
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Dades i seguretat") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AutoBackupCard(
                            isAutoBackupEnabled = isAutoBackupEnabled,
                            selectedFrequency = autoBackupFrequency,
                            onFolderRequested = onAutoBackupFolderRequested,
                            onFrequencyChange = onAutoBackupFrequencyChange,
                            onDisabled = onAutoBackupDisabled,
                        )
                        SettingsPanel {
                            SettingsActionRow(
                                icon = ImageVector.vectorResource(R.drawable.ic_settings_export),
                                title = "Exporta la biblioteca",
                                description = stringResource(R.string.settings_export_backup_description),
                                onClick = onExportBackup,
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = ImageVector.vectorResource(R.drawable.ic_settings_import),
                                accent = OmnilogTheme.accents.Tv,
                                title = "Importa una còpia",
                                description = stringResource(R.string.settings_import_backup_description),
                                onClick = onImportBackup,
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Filled.CheckCircle,
                                accent = OmnilogTheme.accents.Completed,
                                title = "Restaura una còpia anterior",
                                description = stringResource(R.string.settings_restore_backup_description),
                                onClick = onRestoreBackup,
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Importa d'altres serveis") {
                    SettingsPanel {
                        SettingsActionRow(
                            icon = Icons.Filled.Star,
                            accent = OmnilogTheme.accents.Anime,
                            title = "MyAnimeList XML",
                            description = "Afegeix el teu historial d'anime.",
                            onClick = onImportMyAnimeListXml,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.PlayArrow,
                            accent = OmnilogTheme.accents.Tv,
                            title = stringResource(R.string.import_imdb_csv),
                            description = stringResource(
                                R.string.import_imdb_description,
                                stringResource(R.string.nav_movies_tv),
                            ),
                            onClick = onImportImdbCsv,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Filled.Edit,
                            accent = OmnilogTheme.accents.Books,
                            title = "StoryGraph CSV",
                            description = "Afegeix llibres i lectures.",
                            onClick = onImportStoryGraphCsv,
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Omnilog") {
                    SettingsAboutFooter()
                }
            }
        }
    }
}

@Composable
private fun SettingsHero() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Configuració",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
            maxLines = 1,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OmnilogTheme.accents.Dashboard.copy(alpha = 0.10f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = OmnilogTheme.accents.Dashboard,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "Les dades es queden al dispositiu",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
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

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp, end = 10.dp),
        color = OmnilogTheme.colors.appLine,
    )
}

@Composable
private fun AutoBackupCard(
    isAutoBackupEnabled: Boolean,
    selectedFrequency: AutoBackupFrequency,
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
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsLeadingIcon(
                    icon = if (isAutoBackupEnabled) Icons.Filled.CheckCircle else Icons.Filled.Settings,
                    accent = statusAccent,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = "Còpia automàtica",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isAutoBackupEnabled) {
                            "Activada · ${selectedFrequency.label.lowercase()}"
                        } else {
                            "Tria una carpeta per protegir la biblioteca"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusAccent.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = if (isAutoBackupEnabled) "PROTEGIDA" else "INACTIVA",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusAccent,
                        maxLines = 1,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp), color = OmnilogTheme.colors.appLine)
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Freqüència",
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AutoBackupFrequency.entries.forEach { frequency ->
                        Text(
                            text = if (frequency == selectedFrequency) "• ${frequency.label}" else frequency.label,
                            modifier = Modifier
                                .selectable(
                                    selected = frequency == selectedFrequency,
                                    role = Role.RadioButton,
                                    onClick = { onFrequencyChange(frequency) },
                                )
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (frequency == selectedFrequency) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (frequency == selectedFrequency) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp), color = OmnilogTheme.colors.appLine)
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isAutoBackupEnabled) "Canvia la carpeta" else "Tria una carpeta",
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onFolderRequested)
                        .padding(vertical = 2.dp),
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
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
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
private fun SettingsLeadingIcon(
    icon: ImageVector,
    accent: Color,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = if (enabled) 0.14f else 0.08f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.copy(alpha = if (enabled) 1f else 0.56f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * A settings row whose control is too wide to sit beside its label, so it sits under it — the same
 * shape `AutoBackupCard` uses for its frequency options.
 */
@Composable
private fun SettingsChipsRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLeadingIcon(icon = icon, accent = accent)
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(modifier = Modifier.padding(start = 42.dp)) {
            content()
        }
    }
}

/** Theme selector (Sistema / Clar / Fosc) laid out like the other under-label control rows. */
@Composable
private fun SettingsThemeRow(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLeadingIcon(icon = Icons.Filled.Settings, accent = OmnilogTheme.accents.Tv)
            Text(
                text = "Tema de l'aplicació",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier.padding(start = 42.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ThemePreference.entries.forEach { option ->
                val isSelected = option == selected
                Surface(
                    modifier = Modifier.selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) OmnilogTheme.accents.Tv.copy(alpha = 0.16f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) OmnilogTheme.accents.Tv.copy(alpha = 0.42f) else OmnilogTheme.colors.appLine,
                    ),
                ) {
                    Text(
                        text = option.themeOptionLabel(),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) OmnilogTheme.accents.Tv else OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
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
private fun SettingsSwitchRow(
    icon: ImageVector,
    accent: Color,
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon = icon, accent = accent)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OmnilogTheme.colors.appInk,
                checkedTrackColor = accent,
                checkedBorderColor = accent,
                uncheckedThumbColor = OmnilogTheme.colors.appMuted,
                uncheckedTrackColor = OmnilogTheme.colors.appBackground,
                uncheckedBorderColor = OmnilogTheme.colors.appLine,
            ),
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = OmnilogTheme.accents.Dashboard,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon = icon, accent = accent, enabled = enabled)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SettingsAboutFooter() {
    SettingsPanel {
        SettingsInfoRow(
            icon = Icons.Filled.Lock,
            accent = OmnilogTheme.accents.Dashboard,
            title = "Privacitat",
            description = "Dades locals, sense compte ni sincronització.",
        )
        SettingsDivider()
        SettingsInfoRow(
            icon = Icons.Filled.Info,
            accent = OmnilogTheme.colors.appMuted,
            title = "Sobre l'aplicació",
            description = "Omnilog · Versió 1.0",
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon = icon, accent = accent)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
