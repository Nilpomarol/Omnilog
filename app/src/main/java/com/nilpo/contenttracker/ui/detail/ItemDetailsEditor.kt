package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ItemLanguage
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.ui.common.LanguageDropdown

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ItemDetailsEditor(
    item: MediaItem,
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
        coverUrl: String?,
        synopsis: String?,
        sourceUrl: String?,
    ) -> Unit,
) {
    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var originalTitle by rememberSaveable(item.id) { mutableStateOf(item.originalTitle.orEmpty()) }
    var releaseYearText by rememberSaveable(item.id) { mutableStateOf(item.releaseYear?.toString().orEmpty()) }
    var language by rememberSaveable(item.id) {
        mutableStateOf(ItemLanguage.normalize(item.language) ?: ItemLanguage.Original)
    }
    var totalText by rememberSaveable(item.id) { mutableStateOf(item.progressTotal?.toString().orEmpty()) }
    var genresText by rememberSaveable(item.id) { mutableStateOf(item.genres.joinToString(", ")) }
    var creatorsText by rememberSaveable(item.id) { mutableStateOf(item.creators.joinToString(", ")) }
    var coverUrl by rememberSaveable(item.id) { mutableStateOf(item.coverUrl.orEmpty()) }
    var sourceUrl by rememberSaveable(item.id) { mutableStateOf(item.sourceUrl.orEmpty()) }
    var synopsis by rememberSaveable(item.id) { mutableStateOf(plainSynopsis(item.synopsis).orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_metadata_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel), color = accent)
                    }
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onSaveMetadata(
                                title,
                                originalTitle.trim().takeIf { it.isNotBlank() },
                                releaseYearText.toIntOrNull(),
                                ItemLanguage.normalize(language),
                                totalText.toIntOrNull().takeUnless { item.type == MediaType.Game },
                                genresText.toMetadataList(),
                                creatorsText.toMetadataList(),
                                coverUrl.trim().takeIf { it.isNotBlank() },
                                synopsis.trim().takeIf { it.isNotBlank() },
                                sourceUrl.trim().takeIf { it.isNotBlank() },
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetadataEditorField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.field_title),
                accent = accent,
                singleLine = true,
            )
            MetadataEditorField(
                value = originalTitle,
                onValueChange = { originalTitle = it },
                label = stringResource(R.string.field_original_title),
                accent = accent,
                singleLine = true,
            )
            MetadataEditorField(
                value = releaseYearText,
                onValueChange = { value -> releaseYearText = value.filter { it.isDigit() }.take(4) },
                label = stringResource(R.string.field_release_year),
                accent = accent,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            LanguageDropdown(
                value = language,
                onValueChange = { language = ItemLanguage.normalize(it) ?: ItemLanguage.Original },
                label = stringResource(R.string.field_language),
                accent = accent,
            )
            if (item.type != MediaType.Game) {
                MetadataEditorField(
                    value = totalText,
                    onValueChange = { value -> totalText = value.filter { it.isDigit() } },
                    label = stringResource(R.string.field_total_progress),
                    accent = accent,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            MetadataEditorField(
                value = creatorsText,
                onValueChange = { creatorsText = it },
                label = stringResource(R.string.field_creators),
                accent = accent,
                minLines = 2,
            )
            MetadataEditorField(
                value = genresText,
                onValueChange = { genresText = it },
                label = stringResource(R.string.field_genres),
                accent = accent,
                minLines = 2,
            )
            MetadataEditorField(
                value = coverUrl,
                onValueChange = { coverUrl = it },
                label = stringResource(R.string.field_cover_url),
                accent = accent,
                singleLine = true,
            )
            MetadataEditorField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                label = stringResource(R.string.field_source_url),
                accent = accent,
                singleLine = true,
            )
            MetadataEditorField(
                value = synopsis,
                onValueChange = { synopsis = it },
                label = stringResource(R.string.field_synopsis),
                accent = accent,
                minLines = 5,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetadataEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accent: Color,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            cursorColor = accent,
        ),
    )
}

private fun String.toMetadataList(): List<String> =
    split(",", "\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
