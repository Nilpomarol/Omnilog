package com.nilpo.contenttracker.ui.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nilpo.contenttracker.core.imports.ImportBatchProgress
import com.nilpo.contenttracker.core.imports.ImportBatchState
import com.nilpo.contenttracker.core.imports.canDeleteFromHistory
import com.nilpo.contenttracker.core.imports.ImportCoverageItem
import com.nilpo.contenttracker.core.imports.ImportCompletionSummary
import com.nilpo.contenttracker.core.imports.ImportEnrichmentState
import com.nilpo.contenttracker.core.imports.ImportIssueItem
import com.nilpo.contenttracker.core.imports.ImportItemState
import com.nilpo.contenttracker.core.imports.ImportMetadataGap
import com.nilpo.contenttracker.core.imports.ImportReviewApplyOutcome
import com.nilpo.contenttracker.core.imports.ImportReviewDraft
import com.nilpo.contenttracker.core.imports.ImportReviewItem
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.imports.ProviderReference
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ImportProgressBanner(
    progress: ImportBatchProgress,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    concurrentImportCount: Int = 1,
) {
    val active = progress.state == ImportBatchState.Enriching || progress.state == ImportBatchState.Paused
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (concurrentImportCount > 1) {
                            "$concurrentImportCount importacions en curs o en pausa"
                        } else {
                            progress.statusTitle()
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    Text(
                        text = if (concurrentImportCount > 1) {
                            "Obre l'activitat per veure i gestionar cada importació."
                        } else {
                            progress.summary()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                if (active && concurrentImportCount <= 1) {
                    TextButton(onClick = onToggle) {
                        Text(if (progress.state == ImportBatchState.Paused) "Continua" else "Pausa")
                    }
                } else {
                    TextButton(onClick = onOpen) {
                        Text(
                            when {
                                concurrentImportCount > 1 -> "Obre"
                                progress.needsReviewCount > 0 -> "Revisa"
                                progress.issueCount > 0 -> "Incidències"
                                progress.coverageGapCount > 0 -> "Completa"
                                else -> "Detalls"
                            },
                        )
                    }
                }
            }
            if (concurrentImportCount <= 1) {
                LinearProgressIndicator(
                    progress = { progress.fraction() },
                    modifier = Modifier.fillMaxWidth(),
                    color = OmnilogTheme.accents.Anime,
                    trackColor = OmnilogTheme.colors.appLine,
                )
            }
        }
    }
}

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(14.dp),
            color = OmnilogTheme.colors.appBackground,
            border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                historyDeletionCandidate != null -> "Elimina el registre"
                                clearHistoryConfirmation -> "Neteja l'historial"
                                cancellationCandidate != null -> "Cancel·la l'enriquiment"
                                draft != null -> "Revisa els camps"
                                selectedItem != null -> "Tria la coincidència"
                                else -> "Importacions"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OmnilogTheme.colors.appInk,
                        )
                        Text(
                            text = cancellationCandidate?.let { "Importació de ${it.source.label()}" }
                                ?: historyDeletionCandidate?.let { "Importació de ${it.source.label()}" }
                                ?: if (clearHistoryConfirmation) "Importacions completades i cancel·lades" else null
                                ?: draft?.title ?: selectedItem?.title ?: when {
                                state.reviewItems.isNotEmpty() ->
                                    "Progrés i decisions pendents de les importacions."
                                state.recentBatches.any { it.issueCount > 0 } ->
                                    "Progrés i incidències dels proveïdors de metadades."
                                state.coverageItems.isNotEmpty() ->
                                    "Metadades aplicades amb alguns camps encara buits."
                                else -> "Progrés de les importacions."
                            },
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                    TextButton(
                        onClick = {
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
                    ) {
                        Text(
                            if (
                                historyDeletionCandidate != null || clearHistoryConfirmation ||
                                cancellationCandidate != null || draft != null || selectedItem != null
                            ) {
                                "Enrere"
                            } else {
                                "Tanca"
                            },
                        )
                    }
                }

                message?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                when {
                    historyDeletionCandidate != null -> {
                        val batch = requireNotNull(historyDeletionCandidate)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(
                                "S'eliminarà només aquest registre d'importació i els seus detalls " +
                                    "d'enriquiment. Els títols de la biblioteca, " +
                                    "el progrés i les metadades es conservaran.",
                                color = OmnilogTheme.colors.appMuted,
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                            ) {
                                TextButton(
                                    enabled = !loading,
                                    onClick = { historyDeletionCandidate = null },
                                ) {
                                    Text("Conserva el registre")
                                }
                                TextButton(
                                    enabled = !loading,
                                    onClick = {
                                        loading = true
                                        message = null
                                        scope.launch {
                                            val result = onDeleteHistory(batch.batchId)
                                            loading = false
                                            historyDeletionCandidate = null
                                            if (result.isFailure) {
                                                message = "No s'ha pogut eliminar aquest registre."
                                            }
                                        }
                                    },
                                ) {
                                    Text("Elimina el registre")
                                }
                            }
                        }
                    }
                    clearHistoryConfirmation -> {
                        val removableCount = state.historyBatches.count { it.canDeleteFromHistory() }
                        val recordLabel = if (removableCount == 1) "registre" else "registres"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(
                                "S'eliminaran $removableCount $recordLabel d'importació ja resolts. " +
                                    "Els títols de la biblioteca, el progrés i les metadades es conservaran.",
                                color = OmnilogTheme.colors.appMuted,
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                            ) {
                                TextButton(
                                    enabled = !loading,
                                    onClick = { clearHistoryConfirmation = false },
                                ) {
                                    Text("Conserva l'historial")
                                }
                                TextButton(
                                    enabled = !loading,
                                    onClick = {
                                        loading = true
                                        message = null
                                        scope.launch {
                                            val result = onClearHistory()
                                            loading = false
                                            clearHistoryConfirmation = false
                                            if (result.isFailure) {
                                                message = "No s'ha pogut netejar l'historial."
                                            }
                                        }
                                    },
                                ) {
                                    Text("Neteja l'historial")
                                }
                            }
                        }
                    }
                    cancellationCandidate != null -> {
                        val batch = requireNotNull(cancellationCandidate)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(
                                "S'aturaran les consultes pendents i es tancaran les incidències i revisions " +
                                    "d'aquesta importació. Els títols importats i les metadades ja aplicades " +
                                    "es conservaran.",
                                color = OmnilogTheme.colors.appMuted,
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                            ) {
                                TextButton(onClick = { cancellationCandidate = null }) {
                                    Text("Continua enriquint")
                                }
                                TextButton(
                                    onClick = {
                                        onCancel(batch.batchId)
                                        cancellationCandidate = null
                                    },
                                ) {
                                    Text("Cancel·la l'enriquiment")
                                }
                            }
                        }
                    }
                    loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = "Carregant metadades…",
                                modifier = Modifier.padding(top = 12.dp),
                                color = OmnilogTheme.colors.appMuted,
                            )
                        }
                    }
                    draft != null -> {
                        val currentDraft = requireNotNull(draft)
                        currentDraft.reference.evidence.takeIf { it.isNotBlank() }?.let { evidence ->
                            Text(
                                text = "Criteri de coincidència: $evidence",
                                color = OmnilogTheme.colors.appMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = "Marca només els camps que vols substituir. Els camps desmarcats conservaran el valor d'Omnilog.",
                            color = OmnilogTheme.colors.appMuted,
                        )
                        MetadataDiffFieldList(
                            changes = currentDraft.preview.changes,
                            selectedFields = selectedFields,
                            onSelectionChanged = { selectedFields = it },
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
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
                            ) {
                                Text(if (selectedFields.isEmpty()) "Conserva els valors actuals" else "Aplica la selecció")
                            }
                        }
                    }
                    selectedItem != null -> {
                        val item = requireNotNull(selectedItem)
                        Text(
                            text = "Cap coincidència per títol s'aplica automàticament. Tria'n una per comparar-la o omet aquest element.",
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
                        TextButton(
                            modifier = Modifier.align(Alignment.End),
                            onClick = {
                                loading = true
                                scope.launch {
                                    val result = onSkipReview(item.itemId)
                                    loading = false
                                    if (result.isSuccess) selectedItem = null
                                    else message = "No s'ha pogut ometre aquest element."
                                }
                            },
                        ) {
                            Text("Omet aquest element")
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
                            busyIssueId = item.itemId
                            message = null
                            scope.launch {
                                val result = onRetryIssue(item.itemId)
                                busyIssueId = null
                                if (result.isFailure) {
                                    message = "No s'ha pogut tornar a cercar aquest títol."
                                }
                            }
                        },
                        onSkipIssue = { item ->
                            busyIssueId = item.itemId
                            message = null
                            scope.launch {
                                val result = onSkipIssue(item.itemId)
                                busyIssueId = null
                                if (result.isFailure) {
                                    message = "No s'ha pogut tancar aquesta incidència."
                                }
                            }
                        },
                        onManualMatch = onManualMatch,
                        busyCoverageId = busyCoverageId,
                        onRetryCoverage = { item ->
                            busyCoverageId = item.itemId
                            message = null
                            scope.launch {
                                val result = onRetryCoverage(item.itemId)
                                busyCoverageId = null
                                if (result.isFailure) {
                                    message = "No s'han pogut tornar a consultar les metadades d'aquest títol."
                                }
                            }
                        },
                        onDismissCoverage = { item ->
                            busyCoverageId = item.itemId
                            message = null
                            scope.launch {
                                val result = onDismissCoverage(item.itemId)
                                busyCoverageId = null
                                if (result.isFailure) {
                                    message = "No s'ha pogut desar aquesta decisió."
                                }
                            }
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
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (batches.isNotEmpty()) {
            item {
                Text(
                    text = "En curs o per resoldre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                )
            }
        }
        items(batches, key = { "batch:${it.batchId}" }) { batch ->
            BatchStatusCard(
                progress = batch,
                onToggle = { onToggle(batch.batchId) },
                onRetry = { onRetry(batch.batchId) },
                onCancel = { onCancel(batch) },
            )
        }
        if (state.issueItems.isNotEmpty()) {
            item {
                Text(
                    text = "Incidències (${state.issueItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(state.issueItems, key = { "issue:${it.itemId}" }) { issue ->
                IssueRow(
                    issue = issue,
                    busy = busyIssueId == issue.itemId,
                    onRetry = { onRetryIssue(issue) },
                    onManualMatch = { onManualMatch(issue) },
                    onSkip = { onSkipIssue(issue) },
                )
            }
        }
        if (state.coverageItems.isNotEmpty()) {
            item {
                Text(
                    text = "Totals de seguiment pendents (${state.coverageItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                Text(
                    text = "Falten pàgines, minuts o episodis necessaris per mesurar el progrés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            items(state.coverageItems, key = { "coverage:${it.itemId}" }) { coverage ->
                CoverageRow(
                    item = coverage,
                    busy = busyCoverageId == coverage.itemId,
                    onRetry = { onRetryCoverage(coverage) },
                    onManualMatch = { onManualMatchCoverage(coverage) },
                    onDismiss = { onDismissCoverage(coverage) },
                )
            }
        }
        if (state.reviewItems.isNotEmpty()) {
            item {
                Text(
                    text = "Per revisar (${state.reviewItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(state.reviewItems, key = { "review:${it.itemId}" }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReview(item) },
                    shape = RoundedCornerShape(10.dp),
                    color = OmnilogTheme.colors.appPanel,
                    border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (item.candidates.size > 1) {
                                "${item.candidates.size} coincidències possibles"
                            } else {
                                "Hi ha camps diferents per decidir"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                }
            }
        } else if (
            state.issueItems.isEmpty() &&
            state.coverageItems.isEmpty() &&
            batches.none { it.state == ImportBatchState.Enriching || it.state == ImportBatchState.Paused }
        ) {
            item {
                Text(
                    text = "No hi ha cap decisió pendent.",
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Historial (${history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(if (showHistory) "Amaga" else "Mostra")
                    }
                }
            }
            if (showHistory) {
                item {
                    Text(
                        text = "Registres de processos ja resolts. Eliminar-los no modifica la biblioteca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                items(history, key = { "history:${it.batchId}" }) { batch ->
                    ImportHistoryCard(
                        progress = batch,
                        onDelete = { onDeleteHistory(batch) },
                    )
                }
                item {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClearHistory,
                    ) {
                        Text("Neteja l'historial resolt")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportHistoryCard(
    progress: ImportBatchProgress,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = when (progress.state) {
                    ImportBatchState.Cancelled -> "${progress.source.label()} · Enriquiment cancel·lat"
                    else -> "${progress.source.label()} · Importació completada"
                },
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
            )
            Text(
                text = "Finalitzada: ${progress.historyDateLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            Text(
                text = progress.summary(),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onDelete,
            ) {
                Text("Elimina el registre")
            }
        }
    }
}

@Composable
private fun CoverageRow(
    item: ImportCoverageItem,
    busy: Boolean,
    onRetry: () -> Unit,
    onManualMatch: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Falten: ${item.missingFields.joinToString { it.label(item.mediaType) }}.",
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            Column(
                modifier = Modifier.align(Alignment.End),
                horizontalAlignment = Alignment.End,
            ) {
                TextButton(enabled = !busy, onClick = onRetry) {
                    Text("Torna a consultar")
                }
                TextButton(enabled = !busy, onClick = onManualMatch) {
                    Text("Prova una altra coincidència")
                }
                TextButton(enabled = !busy, onClick = onDismiss) {
                    Text("Dona-ho per bo")
                }
            }
        }
    }
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
private fun BatchStatusCard(
    progress: ImportBatchProgress,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val active = progress.state == ImportBatchState.Enriching || progress.state == ImportBatchState.Paused
    val cancellable = active || progress.state == ImportBatchState.CompletedWithIssues
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = progress.statusTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
            Text(progress.summary(), color = OmnilogTheme.colors.appMuted)
            LinearProgressIndicator(
                progress = { progress.fraction() },
                modifier = Modifier.fillMaxWidth(),
                color = OmnilogTheme.accents.Anime,
                trackColor = OmnilogTheme.colors.appLine,
            )
            if (active || progress.issueCount > 0 || cancellable) {
                Column(
                    modifier = Modifier.align(Alignment.End),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (active) {
                        TextButton(onClick = onToggle) {
                            Text(if (progress.state == ImportBatchState.Paused) "Continua" else "Pausa")
                        }
                    } else if (progress.retryableIssueCount > 0) {
                        TextButton(onClick = onRetry) {
                            Text("Reintenta les incidències")
                        }
                    }
                    if (cancellable) {
                        TextButton(onClick = onCancel) {
                            Text("Cancel·la l'enriquiment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueRow(
    issue: ImportIssueItem,
    busy: Boolean,
    onRetry: () -> Unit,
    onManualMatch: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = issue.title,
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = issue.userFacingReason(),
                style = MaterialTheme.typography.bodySmall,
                color = OmnilogTheme.colors.appMuted,
            )
            Column(
                modifier = Modifier.align(Alignment.End),
                horizontalAlignment = Alignment.End,
            ) {
                if (issue.canRetry) {
                    TextButton(enabled = !busy, onClick = onRetry) {
                        Text(if (issue.state == ImportItemState.NoMatch) "Torna a cercar" else "Reintenta")
                    }
                }
                TextButton(enabled = !busy, onClick = onManualMatch) {
                    Text("Cerca manualment")
                }
                TextButton(enabled = !busy, onClick = onSkip) {
                    Text("Conserva'l així")
                }
            }
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

@Composable
private fun CandidateRow(candidate: ProviderReference, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            candidate.coverUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Portada de ${candidate.title.orEmpty()}",
                    modifier = Modifier.size(width = 72.dp, height = 104.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = candidate.title ?: "Coincidència ${candidate.externalId}",
                    fontWeight = FontWeight.Bold,
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
                    ?.let { originalTitle ->
                        CandidateDetail(label = "Títol original", value = originalTitle)
                    }
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

private fun ImportBatchProgress.fraction(): Float = if (totalCount <= 0) 0f
else processedCount.toFloat() / totalCount.toFloat()

private fun ImportBatchProgress.statusTitle(): String = when (state) {
    ImportBatchState.Enriching -> "Completant metadades de ${source.label()}…"
    ImportBatchState.Paused -> "Metadades de ${source.label()} en pausa"
    ImportBatchState.CompletedWithIssues -> when {
        needsReviewCount > 0 -> "Importació de ${source.label()} amb decisions pendents"
        issueCount > 0 -> "Importació de ${source.label()} amb incidències"
        coverageGapCount > 0 -> "Importació de ${source.label()} amb metadades incompletes"
        else -> "Importació de ${source.label()} completada"
    }
    ImportBatchState.Completed -> if (coverageGapCount > 0) {
        "Importació de ${source.label()} amb metadades incompletes"
    } else {
        "Importació de ${source.label()} completada"
    }
    ImportBatchState.Cancelled -> "Enriquiment de ${source.label()} cancel·lat"
    else -> "Importació de ${source.label()}"
}

private fun ImportBatchProgress.summary(): String = buildString {
    append("$processedCount/$totalCount processats · $appliedCount enriquits")
    if (needsReviewCount > 0) append(" · $needsReviewCount per revisar")
    if (issueCount > 0) append(" · $issueCount incidències")
    if (coverageGapCount > 0) append(" · $coverageGapCount incomplets")
    if (optionalMetadataGapCount > 0) {
        append(" · $optionalMetadataGapCount amb metadades opcionals parcials")
    }
    if (cancelledCount > 0) append(" · $cancelledCount cancel·lats")
}

private fun ImportBatchProgress.historyDateLabel(): String {
    val timestamp = completedAtEpochMillis ?: createdAtEpochMillis
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(importHistoryDateFormatter)
}

private val importHistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("ca"))

private fun ImportSource.label(): String = when (this) {
    ImportSource.MalApi -> "MyAnimeList"
    ImportSource.MalXml -> "MAL XML"
    ImportSource.ImdbCsv -> "IMDb"
    ImportSource.StoryGraphCsv -> "StoryGraph"
}

internal fun ImportCompletionSummary.userFacingMessage(): String = buildString {
    val processedLabel = if (processedCount == 1) "processat" else "processats"
    val appliedLabel = if (appliedCount == 1) "enriquit" else "enriquits"
    append("Enriquiment de ${source.label()} completat: $processedCount/$totalCount $processedLabel")
    append(" · $appliedCount $appliedLabel")
    if (needsReviewCount > 0) append(" · $needsReviewCount per revisar")
    if (issueCount > 0) append(" · $issueCount ${if (issueCount == 1) "incidència" else "incidències"}")
    if (coverageGapCount > 0) append(" · $coverageGapCount amb camps buits")
    append('.')
}
