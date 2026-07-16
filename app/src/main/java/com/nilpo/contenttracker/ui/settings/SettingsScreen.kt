package com.nilpo.contenttracker.ui.settings

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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.backup.AutoBackupFrequency
import com.nilpo.contenttracker.ui.common.DashboardSectionChips
import com.nilpo.contenttracker.ui.common.rememberDashboardPreferences
import com.nilpo.contenttracker.ui.common.rememberHiddenDashboardSections
import com.nilpo.contenttracker.ui.common.writeHiddenDashboardSections
import com.nilpo.contenttracker.ui.theme.OmnilogColors

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
    val dashboardPreferences = rememberDashboardPreferences()
    val hiddenSections by rememberHiddenDashboardSections(dashboardPreferences)

    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            item {
                SettingsHero()
            }
            item {
                AutoBackupCard(
                    isAutoBackupEnabled = isAutoBackupEnabled,
                    selectedFrequency = autoBackupFrequency,
                    onFolderRequested = onAutoBackupFolderRequested,
                    onFrequencyChange = onAutoBackupFrequencyChange,
                    onDisabled = onAutoBackupDisabled,
                )
            }
            item {
                SettingsGroup(
                    title = "Preferències",
                    description = "Decideix què et pregunta Omnilog i què mostra a l'inici.",
                    accent = OmnilogColors.Books,
                ) {
                    SettingsSwitchRow(
                        icon = Icons.Filled.Star,
                        accent = OmnilogColors.Books,
                        title = "Demanar nota de Goodreads",
                        description = "Pregunta la nota quan deses llibres importats.",
                        checked = askForGoodreadsRating,
                        onCheckedChange = onAskForGoodreadsRatingChange,
                    )
                    SettingsDivider()
                    SettingsChipsRow(
                        icon = Icons.Filled.Home,
                        accent = OmnilogColors.Dashboard,
                        title = stringResource(R.string.settings_dashboard_sections_title),
                        description = stringResource(R.string.settings_dashboard_sections_description),
                    ) {
                        DashboardSectionChips(
                            hiddenSections = hiddenSections,
                            onToggleSection = { section ->
                                dashboardPreferences.writeHiddenDashboardSections(
                                    if (section in hiddenSections) {
                                        hiddenSections - section
                                    } else {
                                        hiddenSections + section
                                    },
                                )
                            },
                        )
                    }
                }
            }
            item {
                SettingsGroup(
                    title = "Dades i còpies",
                    description = stringResource(R.string.settings_backup_group_description),
                    accent = OmnilogColors.Dashboard,
                ) {
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        title = "Exporta una còpia",
                        description = stringResource(R.string.settings_export_backup_description),
                        onClick = onExportBackup,
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.ArrowDropDown,
                        title = "Importa una còpia",
                        description = stringResource(R.string.settings_import_backup_description),
                        onClick = onImportBackup,
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.CheckCircle,
                        title = "Restaura una còpia anterior",
                        description = stringResource(R.string.settings_restore_backup_description),
                        onClick = onRestoreBackup,
                    )
                }
            }
            item {
                SettingsGroup(
                    title = "Importa d'altres serveis",
                    description = "Afegeix el teu historial sense perdre el que ja tens.",
                    accent = OmnilogColors.Anime,
                ) {
                    SettingsActionRow(
                        icon = Icons.Filled.Star,
                        accent = OmnilogColors.Anime,
                        title = "MyAnimeList XML",
                        description = "Afegeix el teu historial d'anime.",
                        onClick = onImportMyAnimeListXml,
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.PlayArrow,
                        accent = OmnilogColors.Tv,
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
                        accent = OmnilogColors.Books,
                        title = "StoryGraph CSV",
                        description = "Afegeix llibres i lectures.",
                        onClick = onImportStoryGraphCsv,
                    )
                }
            }
            item {
                SettingsAboutFooter()
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(15.dp),
                color = OmnilogColors.Dashboard.copy(alpha = 0.22f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = OmnilogColors.Dashboard,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Configuració",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = "Fes que Omnilog s'adapti a la teva manera de fer seguiment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogColors.AppMuted,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = OmnilogColors.Dashboard,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(R.string.settings_local_storage_notice),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmnilogColors.AppMuted,
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    description: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(width = 4.dp, height = 40.dp),
                shape = RoundedCornerShape(8.dp),
                color = accent,
            ) {}
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogColors.AppMuted,
                )
            }
        }
        HorizontalDivider(color = OmnilogColors.AppLine)
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp),
        color = OmnilogColors.AppLine,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OmnilogColors.AppPanel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsLeadingIcon(icon = Icons.Filled.Settings, accent = OmnilogColors.Dashboard)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Còpia automàtica",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogColors.AppInk,
                    )
                    Text(
                        text = if (isAutoBackupEnabled) "Activada" else "Tria una carpeta per activar-la",
                        style = MaterialTheme.typography.labelSmall,
                        color = OmnilogColors.AppMuted,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = OmnilogColors.AppLine)
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Freqüència",
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
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
                                .padding(horizontal = 5.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (frequency == selectedFrequency) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (frequency == selectedFrequency) OmnilogColors.Dashboard else OmnilogColors.AppMuted,
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = OmnilogColors.AppLine)
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                    color = OmnilogColors.Dashboard,
                )
                if (isAutoBackupEnabled) {
                    Text(
                        text = "Desactiva",
                        modifier = Modifier
                            .clickable(onClick = onDisabled)
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.Dropped,
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
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = if (enabled) 0.14f else 0.08f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.copy(alpha = if (enabled) 1f else 0.56f),
                modifier = Modifier.size(18.dp),
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
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLeadingIcon(icon = icon, accent = accent)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogColors.AppMuted,
                )
            }
        }
        // Indented to the label, not the icon, so the chips read as belonging to this row.
        Box(modifier = Modifier.padding(start = 48.dp)) {
            content()
        }
    }
}

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
            .padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon = icon, accent = accent)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogColors.AppInk,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogColors.AppMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OmnilogColors.AppInk,
                checkedTrackColor = accent,
                checkedBorderColor = accent,
                uncheckedThumbColor = OmnilogColors.AppMuted,
                uncheckedTrackColor = OmnilogColors.AppBackground,
                uncheckedBorderColor = OmnilogColors.AppLine,
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
    accent: Color = OmnilogColors.Dashboard,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon = icon, accent = accent, enabled = enabled)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) OmnilogColors.AppInk else OmnilogColors.AppMuted,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogColors.AppMuted,
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsAboutFooter() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = OmnilogColors.AppLine)
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLeadingIcon(icon = Icons.Filled.Info, accent = OmnilogColors.AppMuted)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Omnilog 1.0",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                )
                Text(
                    text = stringResource(R.string.settings_local_storage_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogColors.AppMuted,
                )
            }
        }
    }
}
