package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.repository.defaultSelectedMetadataFields
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OmnilogPrimaryButton
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.imports.MetadataDiffFieldList
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

private val SheetGutter = 24.dp

/**
 * Search a provider and pick the record an item should be built on. The sheet keeps a fixed height
 * so it does not jump while results arrive; picking a row hands off to the review sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetadataLinkSheet(
    itemTitle: String,
    providerName: String,
    accent: Color,
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<MetadataSuggestion>,
    isSearching: Boolean,
    isPreparing: Boolean,
    hasError: Boolean,
    onSearch: () -> Unit,
    onPick: (MetadataSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = SheetGutter),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SheetHeader(
                    title = stringResource(R.string.metadata_link_title, providerName),
                    note = stringResource(R.string.metadata_link_message, itemTitle),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.metadata_link_search_label, providerName)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.metadata_link_clear))
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    colors = omnilogModalTextFieldColors(accent),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Reserve the bar's height so the list does not shift when it appears.
            Row(modifier = Modifier.padding(top = 12.dp).heightIn(min = 2.dp)) {
                if (isSearching || isPreparing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetGutter),
                        color = accent,
                        trackColor = OmnilogTheme.colors.appLine,
                    )
                }
            }

            val isQueryTooShort = query.trim().length < 2
            when {
                hasError -> StatusSlot(
                    text = stringResource(R.string.metadata_link_search_error),
                    accent = accent,
                    textColor = MaterialTheme.colorScheme.error,
                    action = EmptyStateAction(stringResource(R.string.retry_action), onSearch),
                )

                suggestions.isEmpty() && isSearching -> StatusSlot(stringResource(R.string.metadata_link_loading), accent)

                suggestions.isEmpty() -> StatusSlot(
                    text = stringResource(
                        if (isQueryTooShort) R.string.metadata_link_query_hint else R.string.metadata_link_empty,
                    ),
                    accent = accent,
                )

                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(suggestions) { index, suggestion ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = SheetGutter),
                                color = OmnilogTheme.colors.appLine,
                            )
                        }
                        MetadataSuggestionRow(
                            suggestion = suggestion,
                            accent = accent,
                            duplicateState = MetadataDuplicateState.None,
                            showSource = true,
                            horizontalPadding = SheetGutter,
                            // One pick at a time: a second tap would race the first preview.
                            onClick = { if (!isPreparing) onPick(suggestion) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Review what a refresh or a new link would change, field by field. Everything the provider would
 * overwrite is listed with the current and the new value; the user keeps any field by unticking it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetadataReviewSheet(
    preview: MetadataRefreshPreview,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (Set<MetadataRefreshField>) -> Unit,
) {
    var selectedFields by remember(preview) { mutableStateOf(preview.defaultSelectedMetadataFields()) }
    var isApplying by remember(preview) { mutableStateOf(false) }
    val allFields = remember(preview) { preview.changes.map { it.field }.toSet() }
    val source = preview.refreshed

    ModalBottomSheet(
        onDismissRequest = { if (!isApplying) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = SheetGutter),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SheetHeader(
                    title = stringResource(R.string.metadata_refresh_confirm_title),
                    note = stringResource(R.string.metadata_refresh_confirm_message),
                )
                // The record the values come from, so a wrong match is caught before anything changes.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetadataCoverImage(
                        coverUrl = source.coverUrl,
                        modifier = Modifier.size(width = 36.dp, height = 54.dp),
                        shape = RoundedCornerShape(4.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = SerifFontFamily,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(source.source.displayName(), source.releaseYear?.toString())
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.metadata_refresh_selected_count,
                            selectedFields.size,
                            allFields.size,
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                    val allSelected = selectedFields.containsAll(allFields)
                    TextButton(onClick = { selectedFields = if (allSelected) emptySet() else allFields }) {
                        Text(
                            text = stringResource(
                                if (allSelected) R.string.metadata_refresh_select_none else R.string.metadata_refresh_select_all,
                            ),
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            MetadataDiffFieldList(
                changes = preview.changes,
                selectedFields = selectedFields,
                onSelectionChanged = { selectedFields = it },
                accent = accent,
                horizontalPadding = SheetGutter,
                // Grow with the list, but leave the footer on screen when it is long.
                modifier = Modifier.weight(1f, fill = false),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SheetGutter, end = SheetGutter, top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OmnilogPrimaryButton(
                    text = when {
                        isApplying -> stringResource(R.string.refresh_metadata_loading)
                        selectedFields.isEmpty() -> stringResource(R.string.metadata_refresh_nothing_selected)
                        else -> stringResource(R.string.metadata_refresh_apply_count, selectedFields.size)
                    },
                    onClick = {
                        isApplying = true
                        onConfirm(selectedFields)
                    },
                    enabled = selectedFields.isNotEmpty() && !isApplying,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                )
                TextButton(onClick = onDismiss, enabled = !isApplying) {
                    Text(text = stringResource(R.string.cancel), color = OmnilogTheme.colors.appMuted)
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Normal,
            ),
            color = OmnilogTheme.colors.appInk,
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}

@Composable
private fun StatusSlot(
    text: String,
    accent: Color,
    textColor: Color = OmnilogTheme.colors.appMuted,
    action: EmptyStateAction? = null,
) {
    OmnilogStatusPanel(
        text = text,
        accent = accent,
        modifier = Modifier.padding(horizontal = SheetGutter, vertical = 12.dp),
        textColor = textColor,
        action = action,
    )
}
