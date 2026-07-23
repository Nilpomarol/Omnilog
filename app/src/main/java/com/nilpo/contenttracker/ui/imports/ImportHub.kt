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
import com.nilpo.contenttracker.core.imports.ImportEnrichmentState
import com.nilpo.contenttracker.core.imports.ImportReviewApplyOutcome
import com.nilpo.contenttracker.core.imports.ImportReviewDraft
import com.nilpo.contenttracker.core.imports.ImportReviewItem
import com.nilpo.contenttracker.core.imports.ImportSource
import com.nilpo.contenttracker.core.imports.ProviderReference
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlinx.coroutines.launch

@Composable
internal fun ImportProgressBanner(
    progress: ImportBatchProgress,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
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
                        text = progress.statusTitle(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    Text(
                        text = progress.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                if (active) {
                    TextButton(onClick = onToggle) {
                        Text(if (progress.state == ImportBatchState.Paused) "Continua" else "Pausa")
                    }
                } else {
                    TextButton(onClick = onOpen) {
                        Text(
                            when {
                                progress.needsReviewCount > 0 -> "Revisa"
                                progress.issueCount > 0 -> "Incidències"
                                else -> "Detalls"
                            },
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress.fraction() },
                modifier = Modifier.fillMaxWidth(),
                color = OmnilogTheme.accents.Anime,
                trackColor = OmnilogTheme.colors.appLine,
            )
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
) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<ImportReviewItem?>(null) }
    var draft by remember { mutableStateOf<ImportReviewDraft?>(null) }
    var selectedFields by remember { mutableStateOf<Set<MetadataRefreshField>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var cancellationCandidate by remember { mutableStateOf<ImportBatchProgress?>(null) }

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
                                ?: draft?.title ?: selectedItem?.title ?: when {
                                state.reviewItems.isNotEmpty() ->
                                    "Progrés i decisions pendents de les importacions."
                                state.recentBatches.any { it.issueCount > 0 } ->
                                    "Progrés i incidències dels proveïdors de metadades."
                                else -> "Progrés de les importacions."
                            },
                            color = OmnilogTheme.colors.appMuted,
                        )
                    }
                    TextButton(
                        onClick = {
                            when {
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
                            if (cancellationCandidate != null || draft != null || selectedItem != null) {
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
    modifier: Modifier = Modifier,
) {
    val batches = state.recentBatches
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(batches, key = { "batch:${it.batchId}" }) { batch ->
            BatchStatusCard(
                progress = batch,
                onToggle = { onToggle(batch.batchId) },
                onRetry = { onRetry(batch.batchId) },
                onCancel = { onCancel(batch) },
            )
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
        } else if (batches.none { it.state == ImportBatchState.Enriching || it.state == ImportBatchState.Paused }) {
            item {
                Text(
                    text = if (batches.any { it.issueCount > 0 }) {
                        "No hi ha camps per revisar. Les incidències es poden reintentar des de la importació corresponent."
                    } else {
                        "No hi ha cap decisió pendent."
                    },
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
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
                    } else if (progress.issueCount > 0) {
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
        else -> "Importació de ${source.label()} completada"
    }
    ImportBatchState.Completed -> "Importació de ${source.label()} completada"
    ImportBatchState.Cancelled -> "Enriquiment de ${source.label()} cancel·lat"
    else -> "Importació de ${source.label()}"
}

private fun ImportBatchProgress.summary(): String = buildString {
    append("$processedCount/$totalCount processats · $appliedCount enriquits")
    if (needsReviewCount > 0) append(" · $needsReviewCount per revisar")
    if (issueCount > 0) append(" · $issueCount incidències")
    if (cancelledCount > 0) append(" · $cancelledCount cancel·lats")
}

private fun ImportSource.label(): String = when (this) {
    ImportSource.MalApi -> "MyAnimeList"
    ImportSource.MalXml -> "MAL XML"
    ImportSource.ImdbCsv -> "IMDb"
    ImportSource.StoryGraphCsv -> "StoryGraph"
}
