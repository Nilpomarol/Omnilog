package com.nilpo.contenttracker.ui.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nilpo.contenttracker.core.imports.ImportBatchProgress
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.ImportCompletionSummary
import com.nilpo.contenttracker.core.imports.ImportCoverageItem
import com.nilpo.contenttracker.core.imports.ImportEnrichmentState
import com.nilpo.contenttracker.core.imports.ImportIssueItem
import com.nilpo.contenttracker.core.imports.ImportItemState
import com.nilpo.contenttracker.core.imports.ImportMetadataGap
import com.nilpo.contenttracker.core.imports.ImportReviewApplyOutcome
import com.nilpo.contenttracker.core.imports.ImportReviewDraft
import com.nilpo.contenttracker.core.imports.ImportReviewItem
import com.nilpo.contenttracker.core.imports.ProviderReference
import com.nilpo.contenttracker.core.imports.canDeleteFromHistory
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.ui.common.OmnilogDropdownItem
import com.nilpo.contenttracker.ui.common.OmnilogDropdownMenu
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.OmnilogPrimaryButton
import com.nilpo.contenttracker.ui.common.OmnilogTonalButton
import com.nilpo.contenttracker.ui.common.ProviderLogo
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SheetGutter = 16.dp
private val PanelShape = RoundedCornerShape(14.dp)

/**
 * The strip above the navigation bar while an import completes its metadata, or while one has
 * something left to decide. Tinted with the accent of the section it fills.
 */
@Composable
internal fun ImportProgressBanner(
    progress: ImportBatchProgress,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    concurrentImportCount: Int = 1,
) {
    val active = progress.isActive()
    val accent = progress.source.accent()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = OmnilogTheme.colors.appPanel,
    ) {
        Column {
            if (concurrentImportCount <= 1 && active) {
                LinearProgressIndicator(
                    progress = { progress.fraction() },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                    trackColor = OmnilogTheme.colors.appLine,
                    drawStopIndicator = {},
                )
            } else {
                HorizontalDivider(color = OmnilogTheme.colors.appLine)
            }
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProviderLogo(source = progress.source.ratingSource(), height = 18.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (concurrentImportCount > 1) {
                            "$concurrentImportCount importacions en curs"
                        } else {
                            progress.headline()
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (concurrentImportCount > 1) "Toca per veure-les" else progress.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = if (active && concurrentImportCount <= 1) onToggle else onOpen) {
                    Text(
                        text = when {
                            concurrentImportCount > 1 -> "Obre"
                            active -> if (progress.state == ImportBatchState.Paused) "Continua" else "Pausa"
                            progress.needsReviewCount > 0 -> "Revisa"
                            else -> "Obre"
                        },
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * The import activity: what is waiting for a decision first, then what went wrong, what is still
 * missing, the imports themselves, and a folded history. Every row shows one action; the rest sit
 * behind its ⋯ menu. Confirmations and reviews open in place, with a back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportHubDialog(
    state: ImportEnrichmentState,
    onDismiss: () -> Unit,
    onToggle: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onPrepareReview: suspend (Long, ProviderReference?) -> Result<ImportReviewDraft?>,
    onApplyReview: suspend (ImportReviewDraft, Set<MetadataRefreshField>) -> Result<ImportReviewApplyOutcome>,
    onSkipReview: suspend (Long) -> Result<Unit>,
    onRetryIssue: suspend (Long) -> Result<Unit>,
    onSkipIssue: suspend (Long) -> Result<Unit>,
    onManualMatch: (ImportIssueItem) -> Unit,
    onRetryCoverage: suspend (Long) -> Result<Unit>,
    onDismissCoverage: suspend (Long) -> Result<Unit>,
    onManualMatchCoverage: (ImportCoverageItem) -> Unit,
    onDeleteHistory: suspend (Long) -> Result<Unit>,
    onClearHistory: suspend () -> Result<Int>,
) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<ImportReviewItem?>(null) }
    var draft by remember { mutableStateOf<ImportReviewDraft?>(null) }
    var selectedFields by remember { mutableStateOf<Set<MetadataRefreshField>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var cancellationCandidate by remember { mutableStateOf<ImportBatchProgress?>(null) }
    var historyDeletionCandidate by remember { mutableStateOf<ImportBatchProgress?>(null) }
    var clearHistoryConfirmation by remember { mutableStateOf(false) }
    var busyIssueId by remember { mutableStateOf<Long?>(null) }
    var busyCoverageId by remember { mutableStateOf<Long?>(null) }

    fun prepare(item: ImportReviewItem, reference: ProviderReference?) {
        loading = true
        message = null
        scope.launch {
            val result = onPrepareReview(item.itemId, reference)
            loading = false
            result.fold(
                onSuccess = { prepared ->
                    draft = prepared
                    selectedFields = emptySet()
                    if (prepared == null) selectedItem = null
                },
                onFailure = { message = "No s'han pogut carregar les metadades per revisar." },
            )
        }
    }

    fun runItemAction(
        setBusy: (Long?) -> Unit,
        itemId: Long,
        failure: String,
        action: suspend (Long) -> Result<Unit>,
    ) {
        setBusy(itemId)
        message = null
        scope.launch {
            val result = action(itemId)
            setBusy(null)
            if (result.isFailure) message = failure
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // The lists inside drive their own scroll; leftover at either end is consumed here rather than
    // handed up to the sheet, so scrolling the content never drags the sheet.
    val keepScrollInContent = remember {
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
    val isDrillDown = historyDeletionCandidate != null || clearHistoryConfirmation ||
        cancellationCandidate != null || draft != null || selectedItem != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .nestedScroll(keepScrollInContent),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ImportSheetHeader(
                title = when {
                    historyDeletionCandidate != null -> "Elimina el registre"
                    clearHistoryConfirmation -> "Neteja l'historial"
                    cancellationCandidate != null -> "Deixa de completar"
                    draft != null -> "Revisa els camps"
                    selectedItem != null -> "Tria la coincidència"
                    else -> "Activitat d'importació"
                },
                subtitle = cancellationCandidate?.let { "Importació de ${it.source.label()}" }
                    ?: historyDeletionCandidate?.let { "Importació de ${it.source.label()}" }
                    ?: draft?.title ?: selectedItem?.title,
                isDrillDown = isDrillDown,
                onBack = {
                    when {
                        historyDeletionCandidate != null -> historyDeletionCandidate = null
                        clearHistoryConfirmation -> clearHistoryConfirmation = false
                        cancellationCandidate != null -> cancellationCandidate = null
                        draft != null -> {
                            draft = null
                            selectedFields = emptySet()
                        }
                        selectedItem != null -> selectedItem = null
                        else -> onDismiss()
                    }
                },
            )

            message?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .padding(horizontal = SheetGutter)
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(OmnilogTheme.accents.Dropped.copy(alpha = 0.12f))
                        .padding(14.dp),
                    color = OmnilogTheme.colors.appInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            when {
                historyDeletionCandidate != null -> {
                    val batch = requireNotNull(historyDeletionCandidate)
                    ConfirmationStep(
                        text = "S'eliminarà només aquest registre i els seus detalls. Els títols de la " +
                            "biblioteca, el progrés i les metadades es conservaran.",
                        keepLabel = "Conserva'l",
                        confirmLabel = "Elimina el registre",
                        enabled = !loading,
                        onKeep = { historyDeletionCandidate = null },
                        onConfirm = {
                            loading = true
                            message = null
                            scope.launch {
                                val result = onDeleteHistory(batch.batchId)
                                loading = false
                                historyDeletionCandidate = null
                                if (result.isFailure) message = "No s'ha pogut eliminar aquest registre."
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                clearHistoryConfirmation -> {
                    val removableCount = state.historyBatches.count { it.canDeleteFromHistory() }
                    ConfirmationStep(
                        text = "S'eliminaran $removableCount ${if (removableCount == 1) "registre" else "registres"} " +
                            "d'importacions ja resoltes. Els títols de la biblioteca, el progrés i les metadades es conservaran.",
                        keepLabel = "Conserva'l",
                        confirmLabel = "Neteja l'historial",
                        enabled = !loading,
                        onKeep = { clearHistoryConfirmation = false },
                        onConfirm = {
                            loading = true
                            message = null
                            scope.launch {
                                val result = onClearHistory()
                                loading = false
                                clearHistoryConfirmation = false
                                if (result.isFailure) message = "No s'ha pogut netejar l'historial."
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                cancellationCandidate != null -> {
                    val batch = requireNotNull(cancellationCandidate)
                    ConfirmationStep(
                        text = "Omnilog deixarà de buscar metadades per a aquesta importació i en tancarà les " +
                            "incidències i revisions. Els títols importats i les metadades ja aplicades es conservaran.",
                        keepLabel = "Continua completant",
                        confirmLabel = "Deixa de completar",
                        enabled = true,
                        onKeep = { cancellationCandidate = null },
                        onConfirm = {
                            onCancel(batch.batchId)
                            cancellationCandidate = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = OmnilogTheme.accents.Dashboard,
                        )
                        Text(
                            text = "Carregant metadades…",
                            modifier = Modifier.padding(top = 12.dp),
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
                draft != null -> {
                    val currentDraft = requireNotNull(draft)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = SheetGutter),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = buildString {
                                append("Marca només els camps que vols substituir; la resta conserva el valor d'Omnilog.")
                                currentDraft.reference.evidence.takeIf { it.isNotBlank() }?.let {
                                    append(" Coincidència per: $it.")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogTheme.colors.appMuted,
                        )
                        MetadataDiffFieldList(
                            changes = currentDraft.preview.changes,
                            selectedFields = selectedFields,
                            onSelectionChanged = { selectedFields = it },
                            modifier = Modifier.weight(1f),
                        )
                        OmnilogPrimaryButton(
                            text = if (selectedFields.isEmpty()) "Conserva els valors actuals" else "Aplica la selecció",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                loading = true
                                message = null
                                scope.launch {
                                    val result = onApplyReview(currentDraft, selectedFields)
                                    loading = false
                                    result.fold(
                                        onSuccess = { outcome ->
                                            when (outcome) {
                                                ImportReviewApplyOutcome.Applied -> {
                                                    draft = null
                                                    selectedItem = null
                                                    selectedFields = emptySet()
                                                }
                                                is ImportReviewApplyOutcome.Changed -> {
                                                    draft = outcome.draft
                                                    selectedFields = emptySet()
                                                    message = "L'element ha canviat. Revisa els valors actualitzats abans de desar."
                                                }
                                            }
                                        },
                                        onFailure = { message = "No s'ha pogut desar la decisió." },
                                    )
                                }
                            },
                        )
                    }
                }
                selectedItem != null -> {
                    val item = requireNotNull(selectedItem)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = SheetGutter),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Cap coincidència per títol s'aplica sola. Tria la bona per comparar-la, o omet aquest títol.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogTheme.colors.appMuted,
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(item.candidates, key = { "${it.source.name}:${it.externalId}" }) { candidate ->
                                CandidateRow(candidate = candidate, onClick = { prepare(item, candidate) })
                            }
                        }
                        OmnilogTonalButton(
                            text = "Omet aquest títol",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                loading = true
                                scope.launch {
                                    val result = onSkipReview(item.itemId)
                                    loading = false
                                    if (result.isSuccess) selectedItem = null
                                    else message = "No s'ha pogut ometre aquest títol."
                                }
                            },
                        )
                    }
                }
                else -> ImportOverview(
                    state = state,
                    onToggle = onToggle,
                    onRetry = onRetry,
                    onCancel = { cancellationCandidate = it },
                    onReview = { item ->
                        selectedItem = item
                        if (item.candidates.size <= 1) {
                            prepare(item, item.selectedReference ?: item.candidates.firstOrNull())
                        }
                    },
                    busyIssueId = busyIssueId,
                    onRetryIssue = { item ->
                        runItemAction({ busyIssueId = it }, item.itemId, "No s'ha pogut tornar a cercar aquest títol.", onRetryIssue)
                    },
                    onSkipIssue = { item ->
                        runItemAction({ busyIssueId = it }, item.itemId, "No s'ha pogut tancar aquesta incidència.", onSkipIssue)
                    },
                    onManualMatch = onManualMatch,
                    busyCoverageId = busyCoverageId,
                    onRetryCoverage = { item ->
                        runItemAction(
                            { busyCoverageId = it },
                            item.itemId,
                            "No s'han pogut tornar a consultar les metadades d'aquest títol.",
                            onRetryCoverage,
                        )
                    },
                    onDismissCoverage = { item ->
                        runItemAction({ busyCoverageId = it }, item.itemId, "No s'ha pogut desar aquesta decisió.", onDismissCoverage)
                    },
                    onManualMatchCoverage = onManualMatchCoverage,
                    onDeleteHistory = { historyDeletionCandidate = it },
                    onClearHistory = { clearHistoryConfirmation = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ImportSheetHeader(
    title: String,
    subtitle: String?,
    isDrillDown: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = SheetGutter),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = if (isDrillDown) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                contentDescription = if (isDrillDown) "Enrere" else "Tanca",
                tint = OmnilogTheme.colors.appInk,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A confirmation in place of the list: what will and will not happen, then keep or go ahead. */
@Composable
private fun ConfirmationStep(
    text: String,
    keepLabel: String,
    confirmLabel: String,
    enabled: Boolean,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SheetGutter),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = OmnilogTheme.colors.appInk)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OmnilogTonalButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = enabled,
                accent = OmnilogTheme.accents.Dropped,
            )
            TextButton(onClick = onKeep, enabled = enabled) {
                Text(text = keepLabel, color = OmnilogTheme.colors.appMuted)
            }
        }
    }
}

@Composable
private fun ImportOverview(
    state: ImportEnrichmentState,
    onToggle: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onCancel: (ImportBatchProgress) -> Unit,
    onReview: (ImportReviewItem) -> Unit,
    busyIssueId: Long?,
    onRetryIssue: (ImportIssueItem) -> Unit,
    onSkipIssue: (ImportIssueItem) -> Unit,
    onManualMatch: (ImportIssueItem) -> Unit,
    busyCoverageId: Long?,
    onRetryCoverage: (ImportCoverageItem) -> Unit,
    onDismissCoverage: (ImportCoverageItem) -> Unit,
    onManualMatchCoverage: (ImportCoverageItem) -> Unit,
    onDeleteHistory: (ImportBatchProgress) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val batches = state.recentBatches
    val history = state.historyBatches.filter(ImportBatchProgress::canDeleteFromHistory)
    var showHistory by remember { mutableStateOf(false) }
    val nothingOpen = state.reviewItems.isEmpty() && state.issueItems.isEmpty() &&
        state.coverageItems.isEmpty() && batches.isEmpty()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (nothingOpen) {
            item(key = "empty") {
                Column(
                    modifier = Modifier.padding(horizontal = SheetGutter + 4.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Tot al dia",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = OmnilogTheme.colors.appInk,
                    )
                    Text(
                        text = "No hi ha cap importació en curs ni res per revisar. Quan n'hi hagi, ho trobaràs aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        }

        group(
            key = "review",
            label = "Per revisar",
            count = state.reviewItems.size,
            footnote = "Coincidències dubtoses: tria la bona o omet el títol.",
            items = state.reviewItems,
            itemKey = { "review:${it.itemId}" },
        ) { item ->
            HubRow(
                title = item.title,
                description = if (item.candidates.size > 1) {
                    "${item.candidates.size} coincidències possibles"
                } else {
                    "Hi ha camps diferents per decidir"
                },
                modifier = Modifier.clickable { onReview(item) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = OmnilogTheme.colors.appMuted,
                )
            }
        }

        group(
            key = "issues",
            label = "Incidències",
            count = state.issueItems.size,
            items = state.issueItems,
            itemKey = { "issue:${it.itemId}" },
        ) { issue ->
            HubRow(title = issue.title, description = issue.userFacingReason()) {
                RowActions(
                    busy = busyIssueId == issue.itemId,
                    primaryLabel = "Cerca",
                    onPrimary = { onManualMatch(issue) },
                    overflow = buildList {
                        if (issue.canRetry) {
                            add((if (issue.state == ImportItemState.NoMatch) "Torna a cercar" else "Reintenta") to { onRetryIssue(issue) })
                        }
                        add("Conserva'l així" to { onSkipIssue(issue) })
                    },
                )
            }
        }

        group(
            key = "coverage",
            label = "Falten dades",
            count = state.coverageItems.size,
            footnote = "Sense aquests totals no es pot mesurar el progrés.",
            items = state.coverageItems,
            itemKey = { "coverage:${it.itemId}" },
        ) { coverage ->
            HubRow(
                title = coverage.title,
                description = "Falten: ${coverage.missingFields.joinToString { it.label(coverage.mediaType) }}",
            ) {
                RowActions(
                    busy = busyCoverageId == coverage.itemId,
                    primaryLabel = "Reintenta",
                    onPrimary = { onRetryCoverage(coverage) },
                    overflow = listOf(
                        "Prova una altra coincidència" to { onManualMatchCoverage(coverage) },
                        "Dona-ho per bo" to { onDismissCoverage(coverage) },
                    ),
                )
            }
        }

        group(
            key = "batches",
            label = "Importacions",
            count = batches.size,
            showCount = false,
            items = batches,
            itemKey = { "batch:${it.batchId}" },
        ) { batch ->
            BatchRow(
                progress = batch,
                onToggle = { onToggle(batch.batchId) },
                onRetry = { onRetry(batch.batchId) },
                onCancel = { onCancel(batch) },
            )
        }

        if (history.isNotEmpty()) {
            item(key = "history-toggle") {
                val rotation by animateFloatAsState(if (showHistory) 90f else 0f, label = "historyChevron")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable { showHistory = !showHistory }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = SheetGutter + 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GroupLabel(text = "Historial · ${history.size}", modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (showHistory) "Amaga l'historial" else "Mostra l'historial",
                        tint = OmnilogTheme.colors.appMuted,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
            item(key = "history") {
                AnimatedVisibility(visible = showHistory) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Panel {
                            history.forEachIndexed { index, batch ->
                                if (index > 0) RowDivider()
                                HubRow(
                                    title = "${batch.source.label()} · ${batch.historyDateLabel()}",
                                    description = batch.historySummary(),
                                    leading = { ProviderLogo(source = batch.source.ratingSource(), height = 18.dp) },
                                ) {
                                    RowActions(
                                        busy = false,
                                        primaryLabel = null,
                                        onPrimary = {},
                                        overflow = listOf("Elimina el registre" to { onDeleteHistory(batch) }),
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier.padding(horizontal = SheetGutter - 8.dp),
                        ) {
                            Text("Neteja tot l'historial", color = OmnilogTheme.accents.Dropped)
                        }
                    }
                }
            }
        }
    }
}

/** A named group — label, optional one-line footnote — with its rows on one panel. */
private fun <T> LazyListScope.group(
    key: String,
    label: String,
    count: Int,
    items: List<T>,
    itemKey: (T) -> String,
    footnote: String? = null,
    showCount: Boolean = true,
    row: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "$key-label") {
        Column(
            modifier = Modifier.padding(start = SheetGutter + 4.dp, end = SheetGutter + 4.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            GroupLabel(text = if (showCount) "$label · $count" else label)
            footnote?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = OmnilogTheme.colors.appMuted)
            }
        }
    }
    item(key = "$key-rows") {
        Panel {
            items.forEachIndexed { index, value ->
                if (index > 0) RowDivider()
                androidx.compose.runtime.key(itemKey(value)) { row(value) }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(OmnilogLocale),
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = OmnilogTheme.colors.appMuted,
    )
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = SheetGutter)
            .fillMaxWidth()
            .clip(PanelShape)
            .background(OmnilogTheme.colors.appPanel),
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = OmnilogTheme.colors.appLine)
}

@Composable
private fun HubRow(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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

/** One visible action, the rest behind ⋯; a spinner while the row's request is in flight. */
@Composable
private fun RowActions(
    busy: Boolean,
    primaryLabel: String?,
    onPrimary: () -> Unit,
    overflow: List<Pair<String, () -> Unit>>,
) {
    if (busy) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = OmnilogTheme.accents.Dashboard,
            )
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        primaryLabel?.let { OmnilogTonalButton(text = it, onClick = onPrimary) }
        if (overflow.isNotEmpty()) OverflowMenu(overflow)
    }
}

@Composable
private fun OverflowMenu(actions: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Més accions",
                tint = OmnilogTheme.colors.appMuted,
            )
        }
        OmnilogDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { (label, action) ->
                OmnilogDropdownItem(
                    text = label,
                    onClick = {
                        expanded = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun BatchRow(
    progress: ImportBatchProgress,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val active = progress.isActive()
    val cancellable = active || progress.state == ImportBatchState.CompletedWithIssues
    Column(modifier = Modifier.padding(bottom = if (active) 14.dp else 0.dp)) {
        HubRow(
            title = progress.headline(),
            description = progress.summary(),
            leading = { ProviderLogo(source = progress.source.ratingSource(), height = 18.dp) },
        ) {
            RowActions(
                busy = false,
                primaryLabel = when {
                    !active -> null
                    progress.state == ImportBatchState.Paused -> "Continua"
                    else -> "Pausa"
                },
                onPrimary = onToggle,
                overflow = buildList {
                    if (!active && progress.retryableIssueCount > 0) add("Reintenta les incidències" to onRetry)
                    if (cancellable) add("Deixa de completar" to onCancel)
                },
            )
        }
        if (active) {
            LinearProgressIndicator(
                progress = { progress.fraction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = progress.source.accent(),
                trackColor = OmnilogTheme.colors.appLine,
                drawStopIndicator = {},
            )
        }
    }
}

internal fun ImportIssueItem.userFacingReason(): String = when (state) {
    ImportItemState.NoMatch -> "No s'ha trobat cap coincidència prou segura."
    ImportItemState.Unavailable -> when {
        lastError.orEmpty().contains("authorization", ignoreCase = true) ->
            "No s'ha pogut autenticar amb el proveïdor de metadades."
        lastError.orEmpty().contains("not configured", ignoreCase = true) ->
            "Falta configurar el proveïdor de metadades."
        else -> "El proveïdor de metadades no està disponible ara mateix."
    }
    ImportItemState.Failed -> when {
        lastError.orEmpty().contains("429", ignoreCase = true) ||
            lastError.orEmpty().contains("rate limit", ignoreCase = true) ||
            lastError.orEmpty().contains("too many requests", ignoreCase = true) ->
            "El proveïdor ha limitat temporalment les consultes."
        retryable -> {
            val attempts = if (attemptCount == 1) "1 intent" else "$attemptCount intents"
            "La consulta ha fallat temporalment després de $attempts."
        }
        else -> "S'ha produït un error que no es pot reintentar automàticament."
    }
    else -> "No s'han pogut completar les metadades."
}

private fun ImportMetadataGap.label(mediaType: MediaType): String = when (this) {
    ImportMetadataGap.Cover -> "portada"
    ImportMetadataGap.Synopsis -> "sinopsi"
    ImportMetadataGap.ReleaseYear -> "any"
    ImportMetadataGap.Creators -> when (mediaType) {
        MediaType.Book -> "autoria"
        MediaType.Movie -> "direcció"
        MediaType.TvShow -> "creació"
        MediaType.Anime -> "estudi o autoria"
        MediaType.Game -> "desenvolupador"
    }
    ImportMetadataGap.Genres -> "gèneres"
    ImportMetadataGap.ProgressTotal -> when (mediaType) {
        MediaType.Book -> "pàgines"
        MediaType.Movie -> "durada"
        MediaType.Anime,
        MediaType.TvShow,
        -> "episodis"
        MediaType.Game -> "durada"
    }
}

@Composable
private fun CandidateRow(candidate: ProviderReference, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(OmnilogTheme.colors.appPanel)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        candidate.coverUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = "Portada de ${candidate.title.orEmpty()}",
                modifier = Modifier
                    .size(width = 72.dp, height = 104.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = candidate.title ?: "Coincidència ${candidate.externalId}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            candidate.subtitle
                ?.takeIf { it.isNotBlank() && !it.equals(candidate.title, ignoreCase = true) }
                ?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            candidate.originalTitle
                ?.takeIf { it.isNotBlank() && !it.equals(candidate.title, ignoreCase = true) }
                ?.let { originalTitle -> CandidateDetail(label = "Títol original", value = originalTitle) }
            candidate.creators.takeIf { it.isNotEmpty() }?.let { creators ->
                CandidateDetail(label = "Autoria", value = creators.joinToString())
            }
            candidate.evidence.takeIf { it.isNotBlank() }?.let { evidence ->
                CandidateDetail(label = "Criteri", value = evidence)
            }
            val language = languageLabel(candidate.language).takeIf { it.isNotBlank() }
            val primaryFacts = listOfNotNull(
                candidate.releaseYear?.toString(),
                language,
                candidate.progressTotal?.let { "$it pàgines" },
                candidate.format?.takeUnless { it.equals("BOOK", ignoreCase = true) },
            )
            if (primaryFacts.isNotEmpty()) {
                Text(
                    text = primaryFacts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            candidate.publishers.takeIf { it.isNotEmpty() }?.let { publishers ->
                CandidateDetail(label = "Editorial", value = publishers.joinToString())
            }
            candidate.identifiers.takeIf { it.isNotEmpty() }?.let { identifiers ->
                CandidateDetail(label = "ISBN", value = identifiers.joinToString(" · "))
            }
            candidate.genres.takeIf { it.isNotEmpty() }?.let { genres ->
                CandidateDetail(label = "Gèneres", value = genres.joinToString())
            }
            candidate.ratingScore?.let { score ->
                val maxScore = candidate.ratingMaxScore ?: 5.0
                val votes = candidate.ratingVoteCount?.let { " · $it valoracions" }.orEmpty()
                CandidateDetail(
                    label = "Valoració",
                    value = "${score.compactNumber()}/${maxScore.compactNumber()}$votes",
                )
            }
            plainSynopsis(candidate.synopsis)?.takeIf { it.isNotBlank() }?.let { synopsis ->
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${candidate.source.displayName()} · ${candidate.externalId}",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
        }
    }
}

@Composable
private fun CandidateDetail(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = OmnilogTheme.colors.appMuted,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun Double.compactNumber(): String =
    if (this == toInt().toDouble()) toInt().toString() else String.format("%.1f", this)

private fun MetadataSource.displayName(): String = when (this) {
    MetadataSource.GoogleBooks -> "Google Books"
    MetadataSource.OpenLibrary -> "Open Library"
    else -> name
}

private fun ImportBatchProgress.isActive(): Boolean =
    state == ImportBatchState.Enriching || state == ImportBatchState.Paused

private fun ImportBatchProgress.fraction(): Float = if (totalCount <= 0) 0f
else processedCount.toFloat() / totalCount.toFloat()

private fun ImportBatchProgress.headline(): String = when (state) {
    ImportBatchState.Enriching -> "Completant ${source.label()}…"
    ImportBatchState.Paused -> "${source.label()} en pausa"
    ImportBatchState.Cancelled -> "${source.label()} · aturada"
    else -> "${source.label()} · importada"
}

/** Progress while it runs, then only what still asks for something. */
private fun ImportBatchProgress.summary(): String = buildList {
    if (isActive()) add("$processedCount de $totalCount")
    if (needsReviewCount > 0) add("$needsReviewCount per revisar")
    if (issueCount > 0) add("$issueCount ${if (issueCount == 1) "incidència" else "incidències"}")
    if (coverageGapCount > 0) add("$coverageGapCount amb dades pendents")
    if (isEmpty()) add("$appliedCount de $totalCount completats")
}.joinToString(" · ")

private fun ImportBatchProgress.historySummary(): String = buildString {
    append("$appliedCount de $totalCount completats")
    if (cancelledCount > 0) append(" · $cancelledCount sense completar")
}

private fun ImportBatchProgress.historyDateLabel(): String {
    val timestamp = completedAtEpochMillis ?: createdAtEpochMillis
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(importHistoryDateFormatter)
}

private val importHistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ca"))

/** The completion notice: done, and only what still needs the user, if anything. */
internal fun ImportCompletionSummary.userFacingMessage(): String = buildString {
    append("${source.label()}: metadades completades")
    val pending = buildList {
        if (needsReviewCount > 0) add("$needsReviewCount per revisar")
        if (issueCount > 0) add("$issueCount ${if (issueCount == 1) "incidència" else "incidències"}")
        if (coverageGapCount > 0) add("$coverageGapCount amb dades pendents")
    }
    if (pending.isEmpty()) append('.') else append(" · ${pending.joinToString(" · ")}.")
}
