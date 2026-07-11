package com.nilpo.contenttracker.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCollection
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.ImdbCsvPreview
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.repository.MyAnimeListXmlPreview
import com.nilpo.contenttracker.core.repository.StoryGraphCsvPreview
import com.nilpo.contenttracker.core.repository.UnsupportedBackupSchemaException
import com.nilpo.contenttracker.ui.add.AddMediaScreen
import com.nilpo.contenttracker.ui.add.AddCollectionOption
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import com.nilpo.contenttracker.ui.detail.DetailScreen
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.home.CollectionDetailScreen
import com.nilpo.contenttracker.ui.home.AuthorDetailScreen
import com.nilpo.contenttracker.ui.home.HomeScreen
import com.nilpo.contenttracker.ui.home.HomeLandingScreen
import com.nilpo.contenttracker.ui.home.HomeUiEvent
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.stats.StatsScreen
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentTrackerApp(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metadataUiState by viewModel.metadataUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val preferences = remember(context) {
        context.getSharedPreferences("omnilog_preferences", Context.MODE_PRIVATE)
    }
    var askForGoodreadsRating by remember {
        mutableStateOf(preferences.getBoolean("ask_for_goodreads_rating", true))
    }
    var selectedMediaId by remember { mutableStateOf<Long?>(null) }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    var showRestoreList by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingImportConfirmation by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingImdbCsvImport by remember { mutableStateOf<PendingImdbCsvImport?>(null) }
    var pendingStoryGraphCsvImport by remember { mutableStateOf<PendingStoryGraphCsvImport?>(null) }
    var pendingMyAnimeListXmlImport by remember { mutableStateOf<PendingMyAnimeListXmlImport?>(null) }
    var metadataLinkTarget by remember { mutableStateOf<TrackedMedia?>(null) }
    var metadataLinkQuery by remember { mutableStateOf("") }
    var metadataLinkSuggestions by remember { mutableStateOf<List<MetadataSuggestion>>(emptyList()) }
    var isMetadataLinkLoading by remember { mutableStateOf(false) }
    var hasMetadataLinkError by remember { mutableStateOf(false) }
    var metadataLinkSearchRequestId by remember { mutableStateOf(0) }
    var pendingMetadataRefreshPreview by remember { mutableStateOf<MetadataRefreshPreview?>(null) }
    var pendingPossibleDuplicate by remember { mutableStateOf<PendingPossibleDuplicate?>(null) }
    var pendingCreatedMediaId by remember { mutableStateOf<Long?>(null) }
    var addTargetCollection by remember { mutableStateOf<MediaCollection?>(null) }
    var addTargetCollectionOrder by remember { mutableStateOf<Double?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<AppDestination>(AppDestination.Home) }
    var detailReturnTarget by remember { mutableStateOf<DetailReturnTarget>(DetailReturnTarget.Section) }
    var collectionReturnTarget by remember { mutableStateOf<CollectionReturnTarget>(CollectionReturnTarget.Section) }
    var authorReturnTarget by remember { mutableStateOf<AuthorReturnTarget>(AuthorReturnTarget.Section) }
    var detailActions by remember { mutableStateOf(DetailHeaderActions()) }
    val backupActions = remember { BackupHeaderActions() }
    val selectedMedia = uiState.allTrackedItems.firstOrNull { it.item.id == selectedMediaId }
    val selectedCollection = uiState.trackedItems
        .mapNotNull { it.collection }
        .firstOrNull { it.id == selectedCollectionId }
    val selectedCollectionItems = uiState.trackedItems
        .filter { it.collection?.id == selectedCollectionId }
        .sortedWith(collectionItemComparator())
    val selectedAuthorItems = selectedAuthor?.let { author ->
        uiState.trackedItems.filter { trackedMedia ->
            trackedMedia.item.creators.any { it.trim().equals(author.trim(), ignoreCase = true) }
        }
    }.orEmpty()
    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val exportErrorMessage = stringResource(R.string.backup_export_error)
    val importReadErrorMessage = stringResource(R.string.backup_import_read_error)
    val importSuccessMessage = stringResource(R.string.backup_import_success)
    val importInvalidMessage = stringResource(R.string.backup_import_invalid)
    val importUnsupportedMessage = stringResource(R.string.backup_import_unsupported)
    val imdbImportReadErrorMessage = stringResource(R.string.imdb_import_read_error)
    val imdbImportInvalidMessage = stringResource(R.string.imdb_import_invalid)
    val imdbImportEmptyMessage = stringResource(R.string.imdb_import_empty)
    val storyGraphImportReadErrorMessage = stringResource(R.string.storygraph_import_read_error)
    val storyGraphImportInvalidMessage = stringResource(R.string.storygraph_import_invalid)
    val storyGraphImportEmptyMessage = stringResource(R.string.storygraph_import_empty)
    val myAnimeListImportReadErrorMessage = stringResource(R.string.mal_import_read_error)
    val myAnimeListImportInvalidMessage = stringResource(R.string.mal_import_invalid)
    val myAnimeListImportEmptyMessage = stringResource(R.string.mal_import_empty)
    val metadataRefreshSuccessMessage = stringResource(R.string.metadata_refresh_success)
    val metadataRefreshUnavailableMessage = stringResource(R.string.metadata_refresh_unavailable)
    val metadataRefreshErrorMessage = stringResource(R.string.metadata_refresh_error)
    val metadataLinkSuccessMessage = stringResource(R.string.metadata_link_success)
    val metadataLinkErrorMessage = stringResource(R.string.metadata_link_error)
    val navigateBackFromDetail = {
        when (val returnTarget = detailReturnTarget) {
            DetailReturnTarget.Home -> {
                selectedDestination = AppDestination.Home
                selectedMediaId = null
                selectedCollectionId = null
            }
            is DetailReturnTarget.Collection -> {
                selectedDestination = AppDestination.Section
                selectedMediaId = null
                selectedCollectionId = returnTarget.collectionId
            }
            is DetailReturnTarget.Author -> {
                selectedMediaId = null
                selectedAuthor = returnTarget.author
            }
            DetailReturnTarget.Section -> {
                selectedMediaId = null
            }
        }
        detailReturnTarget = DetailReturnTarget.Section
    }
    val navigateBackFromCollection = {
        when (val returnTarget = collectionReturnTarget) {
            is CollectionReturnTarget.Detail -> {
                selectedCollectionId = null
                selectedMediaId = returnTarget.mediaItemId
            }
            CollectionReturnTarget.Section -> {
                selectedCollectionId = null
            }
        }
        collectionReturnTarget = CollectionReturnTarget.Section
    }
    val navigateBackFromAuthor = {
        when (val returnTarget = authorReturnTarget) {
            is AuthorReturnTarget.Detail -> {
                selectedAuthor = null
                selectedMediaId = returnTarget.mediaItemId
            }
            AuthorReturnTarget.Section -> selectedAuthor = null
        }
        authorReturnTarget = AuthorReturnTarget.Section
    }
    val navigateBackFromStats = {
        selectedDestination = AppDestination.Home
        selectedMediaId = null
        selectedCollectionId = null
        collectionReturnTarget = CollectionReturnTarget.Section
        detailReturnTarget = DetailReturnTarget.Section
        isAdding = false
    }
    val openTrackedMedia: (TrackedMedia) -> Unit = { trackedMedia ->
        viewModel.clearMetadataSearch()
        viewModel.selectSection(trackedMedia.item.type.homeSection())
        selectedDestination = AppDestination.Section
        selectedCollectionId = null
        collectionReturnTarget = CollectionReturnTarget.Section
        detailReturnTarget = DetailReturnTarget.Section
        selectedMediaId = trackedMedia.item.id
        isAdding = false
    }
    val openRelatedMedia: (TrackedMedia) -> Unit = { trackedMedia ->
        viewModel.clearMetadataSearch()
        viewModel.selectSection(trackedMedia.item.type.homeSection())
        selectedDestination = AppDestination.Section
        selectedCollectionId = null
        selectedMediaId = trackedMedia.item.id
        isAdding = false
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val result = runCatching {
                    val backupJson = viewModel.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        val bytes = backupJson.toByteArray(Charsets.UTF_8)
                        checkNotNull(context.contentResolver.openOutputStream(uri)) {
                            "Could not open backup destination"
                        }.use { outputStream ->
                            outputStream.write(bytes)
                        }
                    }
                }
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) exportSuccessMessage else exportErrorMessage,
                )
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val readResult = runCatching {
                    withContext(Dispatchers.IO) {
                        checkNotNull(context.contentResolver.openInputStream(uri)) {
                            "Could not open backup source"
                        }.use { inputStream ->
                            inputStream.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
                val backupJson = readResult.getOrNull()
                if (backupJson == null) {
                    snackbarHostState.showSnackbar(importReadErrorMessage)
                    return@launch
                }

                val previewResult = runCatching {
                    PendingBackupImport(
                        json = backupJson,
                        preview = viewModel.previewBackupJson(backupJson),
                    )
                }
                pendingImport = previewResult.getOrNull()
                previewResult.exceptionOrNull()?.let { error ->
                    val message = when (error) {
                        is UnsupportedBackupSchemaException -> importUnsupportedMessage
                        else -> importInvalidMessage
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
    val importImdbCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val readResult = runCatching {
                    withContext(Dispatchers.IO) {
                        checkNotNull(context.contentResolver.openInputStream(uri)) {
                            "Could not open IMDb CSV source"
                        }.use { inputStream ->
                            inputStream.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
                val csv = readResult.getOrNull()
                if (csv == null) {
                    snackbarHostState.showSnackbar(imdbImportReadErrorMessage)
                    return@launch
                }

                val previewResult = runCatching {
                    PendingImdbCsvImport(
                        csv = csv,
                        preview = viewModel.previewImdbCsv(csv),
                    )
                }
                val pendingCsv = previewResult.getOrNull()
                if (pendingCsv == null) {
                    snackbarHostState.showSnackbar(imdbImportInvalidMessage)
                } else if (pendingCsv.preview.importableRows == 0) {
                    snackbarHostState.showSnackbar(imdbImportEmptyMessage)
                } else {
                    pendingImdbCsvImport = pendingCsv
                }
            }
        }
    }
    val importStoryGraphCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val readResult = runCatching {
                    withContext(Dispatchers.IO) {
                        checkNotNull(context.contentResolver.openInputStream(uri)) {
                            "Could not open StoryGraph CSV source"
                        }.use { inputStream ->
                            inputStream.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
                val csv = readResult.getOrNull()
                if (csv == null) {
                    snackbarHostState.showSnackbar(storyGraphImportReadErrorMessage)
                    return@launch
                }

                val previewResult = runCatching {
                    PendingStoryGraphCsvImport(
                        csv = csv,
                        preview = viewModel.previewStoryGraphCsv(csv),
                    )
                }
                val pendingCsv = previewResult.getOrNull()
                if (pendingCsv == null) {
                    snackbarHostState.showSnackbar(storyGraphImportInvalidMessage)
                } else if (pendingCsv.preview.importableRows == 0) {
                    snackbarHostState.showSnackbar(storyGraphImportEmptyMessage)
                } else {
                    pendingStoryGraphCsvImport = pendingCsv
                }
            }
        }
    }
    val importMyAnimeListXmlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val readResult = runCatching {
                    withContext(Dispatchers.IO) {
                        checkNotNull(context.contentResolver.openInputStream(uri)) {
                            "Could not open MyAnimeList XML source"
                        }.use { inputStream ->
                            inputStream.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
                val xml = readResult.getOrNull()
                if (xml == null) {
                    snackbarHostState.showSnackbar(myAnimeListImportReadErrorMessage)
                    return@launch
                }

                val previewResult = runCatching {
                    PendingMyAnimeListXmlImport(
                        xml = xml,
                        preview = viewModel.previewMyAnimeListXml(xml),
                    )
                }
                val pendingXml = previewResult.getOrNull()
                if (pendingXml == null) {
                    snackbarHostState.showSnackbar(myAnimeListImportInvalidMessage)
                } else if (pendingXml.preview.importableRows == 0) {
                    snackbarHostState.showSnackbar(myAnimeListImportEmptyMessage)
                } else {
                    pendingMyAnimeListXmlImport = pendingXml
                }
            }
        }
    }
    backupActions.onExportBackupRequested = {
        exportBackupLauncher.launch("omnilog-backup-${LocalDate.now()}.json")
    }
    backupActions.onImportBackupRequested = {
        importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
    }
    backupActions.onImportImdbCsvRequested = {
        importImdbCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*"))
    }
    backupActions.onImportStoryGraphCsvRequested = {
        importStoryGraphCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*"))
    }
    backupActions.onImportMyAnimeListXmlRequested = {
        importMyAnimeListXmlLauncher.launch(arrayOf("text/xml", "application/xml", "text/*", "*/*"))
    }
    backupActions.onRestoreBackupRequested = {
        showRestoreList = true
    }
    val searchMetadataLink: (TrackedMedia, String) -> Unit = { trackedMedia, query ->
        val trimmedQuery = query.trim()
        metadataLinkSearchRequestId += 1
        val requestId = metadataLinkSearchRequestId
        if (trimmedQuery.isBlank()) {
            metadataLinkSuggestions = emptyList()
            isMetadataLinkLoading = false
            hasMetadataLinkError = false
        } else {
            metadataLinkSuggestions = emptyList()
            isMetadataLinkLoading = true
            hasMetadataLinkError = false
            coroutineScope.launch {
                val result = runCatching {
                    viewModel.searchMetadataLinkSuggestions(
                        title = trimmedQuery,
                        type = trackedMedia.item.type,
                    )
                }
                if (requestId == metadataLinkSearchRequestId && metadataLinkTarget?.item?.id == trackedMedia.item.id) {
                    metadataLinkSuggestions = result.getOrDefault(emptyList())
                    isMetadataLinkLoading = false
                    hasMetadataLinkError = result.isFailure
                }
            }
        }
    }
    val startMetadataLink: (TrackedMedia) -> Unit = { trackedMedia ->
        metadataLinkSearchRequestId += 1
        metadataLinkTarget = trackedMedia
        metadataLinkQuery = trackedMedia.item.title
        metadataLinkSuggestions = emptyList()
        isMetadataLinkLoading = false
        hasMetadataLinkError = false
    }
    val startMetadataRefresh: (TrackedMedia) -> Unit = { trackedMedia ->
        coroutineScope.launch {
            val result = viewModel.previewMediaItemMetadataRefresh(trackedMedia.item.id)
            val preview = result.getOrNull()
            when {
                result.isFailure -> snackbarHostState.showSnackbar(metadataRefreshErrorMessage)
                preview == null -> snackbarHostState.showSnackbar(metadataRefreshUnavailableMessage)
                preview.changes.any { it.overwritesExistingValue || it.isLocallyOverridden } -> {
                    pendingMetadataRefreshPreview = preview
                }
                else -> {
                    val applyResult = viewModel.applyMediaItemMetadataRefresh(
                        preview = preview,
                        selectedFields = preview.changes.map { it.field }.toSet(),
                    )
                    snackbarHostState.showSnackbar(
                        if (applyResult.getOrDefault(false)) {
                            metadataRefreshSuccessMessage
                        } else {
                            metadataRefreshErrorMessage
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.MediaItemCreated -> {
                    pendingCreatedMediaId = event.mediaItemId
                }
                HomeUiEvent.MetadataRefreshSucceeded,
                HomeUiEvent.MetadataRefreshUnavailable,
                HomeUiEvent.MetadataRefreshFailed,
                    -> {
                    val message = when (event) {
                        HomeUiEvent.MetadataRefreshSucceeded -> metadataRefreshSuccessMessage
                        HomeUiEvent.MetadataRefreshUnavailable -> metadataRefreshUnavailableMessage
                        HomeUiEvent.MetadataRefreshFailed -> metadataRefreshErrorMessage
                        is HomeUiEvent.MediaItemCreated -> return@collect
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    LaunchedEffect(uiState.allTrackedItems, pendingCreatedMediaId) {
        val mediaItemId = pendingCreatedMediaId ?: return@LaunchedEffect
        val trackedMedia = uiState.allTrackedItems.firstOrNull { it.item.id == mediaItemId }
            ?: return@LaunchedEffect

        val targetCollection = addTargetCollection
        viewModel.selectSection(trackedMedia.item.type.homeSection())
        selectedDestination = AppDestination.Section
        if (targetCollection != null) {
            selectedCollectionId = targetCollection.id
            selectedMediaId = null
            collectionReturnTarget = CollectionReturnTarget.Section
        } else {
            selectedCollectionId = null
            detailReturnTarget = DetailReturnTarget.Section
            selectedMediaId = mediaItemId
        }
        isAdding = false
        addTargetCollection = null
        addTargetCollectionOrder = null
        pendingCreatedMediaId = null
    }

    BackHandler(
        enabled = pendingPossibleDuplicate != null ||
            metadataLinkTarget != null ||
            pendingMetadataRefreshPreview != null ||
            pendingMyAnimeListXmlImport != null ||
            pendingStoryGraphCsvImport != null ||
            pendingImdbCsvImport != null ||
            pendingImportConfirmation != null ||
            pendingImport != null ||
            showRestoreList ||
            selectedMediaId != null ||
            isAdding ||
            selectedCollectionId != null ||
            selectedAuthor != null ||
            selectedDestination != AppDestination.Home,
    ) {
        when {
            pendingPossibleDuplicate != null -> pendingPossibleDuplicate = null
            metadataLinkTarget != null -> metadataLinkTarget = null
            pendingMetadataRefreshPreview != null -> pendingMetadataRefreshPreview = null
            pendingMyAnimeListXmlImport != null -> pendingMyAnimeListXmlImport = null
            pendingStoryGraphCsvImport != null -> pendingStoryGraphCsvImport = null
            pendingImdbCsvImport != null -> pendingImdbCsvImport = null
            pendingImportConfirmation != null -> pendingImportConfirmation = null
            pendingImport != null -> pendingImport = null
            showRestoreList -> showRestoreList = false
            selectedMediaId != null -> navigateBackFromDetail()
            isAdding -> {
                viewModel.clearMetadataSearch()
                isAdding = false
                addTargetCollection = null
                addTargetCollectionOrder = null
                selectedCollectionId = null
                collectionReturnTarget = CollectionReturnTarget.Section
            }
            selectedCollectionId != null -> navigateBackFromCollection()
            selectedAuthor != null -> navigateBackFromAuthor()
            selectedDestination != AppDestination.Home -> {
                selectedDestination = AppDestination.Home
                selectedCollectionId = null
                selectedMediaId = null
                collectionReturnTarget = CollectionReturnTarget.Section
                isAdding = false
                viewModel.clearMetadataSearch()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                OmnilogTopBar(
                    accent = when (selectedDestination) {
                        AppDestination.Home,
                        AppDestination.Stats,
                            -> OmnilogColors.Dashboard
                        AppDestination.Section -> uiState.selectedSection.accent
                    },
                    showBackNavigation = selectedMedia != null && !isAdding ||
                        selectedDestination == AppDestination.Stats,
                    showDetailActions = selectedMedia != null && !isAdding && !detailActions.isManagingExternalRatings,
                    showBackupActions = selectedMedia == null &&
                        !isAdding &&
                        selectedDestination != AppDestination.Stats,
                    showMyAnimeListImport = selectedDestination == AppDestination.Section &&
                        uiState.selectedSection == MediaSection.Anime,
                    showImdbImport = selectedDestination == AppDestination.Section &&
                        uiState.selectedSection == MediaSection.Movies,
                    showStoryGraphImport = selectedDestination == AppDestination.Section &&
                        uiState.selectedSection == MediaSection.Books,
                    detailActions = detailActions,
                    backupActions = backupActions,
                    askForGoodreadsRating = askForGoodreadsRating,
                    onAskForGoodreadsRatingChange = { enabled ->
                        askForGoodreadsRating = enabled
                        preferences.edit().putBoolean("ask_for_goodreads_rating", enabled).apply()
                    },
                    onBack = if (detailActions.isManagingExternalRatings) {
                        detailActions.onCloseExternalRatings
                    } else if (selectedDestination == AppDestination.Stats) {
                        navigateBackFromStats
                    } else {
                        navigateBackFromDetail
                    },
                )
            },
            snackbarHost = {},
            bottomBar = {
                OmnilogBottomBar(
                    selectedDestination = selectedDestination,
                    selectedSection = uiState.selectedSection,
                    onHomeClick = {
                        selectedDestination = AppDestination.Home
                        selectedMediaId = null
                selectedCollectionId = null
                collectionReturnTarget = CollectionReturnTarget.Section
                detailReturnTarget = DetailReturnTarget.Section
                isAdding = false
                addTargetCollection = null
                addTargetCollectionOrder = null
                viewModel.clearMetadataSearch()
            },
                    onSectionClick = { section ->
                        selectedDestination = AppDestination.Section
                        selectedMediaId = null
                        selectedCollectionId = null
                        collectionReturnTarget = CollectionReturnTarget.Section
                        detailReturnTarget = DetailReturnTarget.Section
                        isAdding = false
                        addTargetCollection = null
                        addTargetCollectionOrder = null
                        viewModel.selectSection(section)
                        viewModel.clearMetadataSearch()
                    },
                )
            },
        ) { innerPadding ->
        if (isAdding) {
            val targetCollection = addTargetCollection
            val initialAddType = selectedCollectionItems.firstOrNull()?.item?.type
                ?: uiState.selectedSection.defaultType
            AddMediaScreen(
                initialMediaType = initialAddType,
                availableMediaTypes = uiState.selectedSection.types.toList(),
                availableCollections = uiState.allTrackedItems.toAddCollectionOptions(),
                initialCollection = targetCollection,
                initialCollectionName = targetCollection?.name,
                initialCollectionOrder = addTargetCollectionOrder?.let { formatCollectionOrder(it) },
                onSave = { request ->
                    viewModel.addTrackedMedia(request)
                    viewModel.clearMetadataSearch()
                    isAdding = false
                },
                metadataUiState = metadataUiState,
                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                onMetadataSearch = { viewModel.searchMetadataSuggestions() },
                onMetadataSearchSubmitted = { viewModel.searchMetadataSuggestions(forceShortQuery = true) },
                onMetadataSuggestionSelected = { suggestion ->
                    when (val duplicate = uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> openTrackedMedia(duplicate.trackedMedia)
                        is DuplicateMatch.Possible -> {
                            pendingPossibleDuplicate = PendingPossibleDuplicate(
                                suggestion = suggestion,
                                trackedMedia = duplicate.trackedMedia,
                            )
                        }
                        DuplicateMatch.None -> viewModel.selectMetadataSuggestion(suggestion)
                    }
                },
                duplicateStateForSuggestion = { suggestion ->
                    when (uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> MetadataDuplicateState.Exact
                        is DuplicateMatch.Possible -> MetadataDuplicateState.Possible
                        DuplicateMatch.None -> MetadataDuplicateState.None
                    }
                },
                onCancel = {
                    viewModel.clearMetadataSearch()
                    isAdding = false
                    addTargetCollection = null
                    addTargetCollectionOrder = null
                    selectedCollectionId = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedDestination == AppDestination.Home) {
            HomeLandingScreen(
                uiState = uiState,
                onMediaClick = { trackedMedia ->
                    val section = trackedMedia.item.type.homeSection()
                    viewModel.selectSection(section)
                    selectedDestination = AppDestination.Section
                    selectedCollectionId = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                    detailReturnTarget = DetailReturnTarget.Home
                    selectedMediaId = trackedMedia.item.id
                },
                onSectionSearch = { section, query ->
                    viewModel.selectSectionWithSearch(section, query)
                    selectedDestination = AppDestination.Section
                    selectedMediaId = null
                    selectedCollectionId = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                    isAdding = false
                },
                onStatsClick = {
                    selectedDestination = AppDestination.Stats
                    selectedMediaId = null
                    selectedCollectionId = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                    detailReturnTarget = DetailReturnTarget.Home
                    isAdding = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedDestination == AppDestination.Stats) {
            StatsScreen(
                items = uiState.allTrackedItems,
                onMediaClick = { trackedMedia ->
                    val section = trackedMedia.item.type.homeSection()
                    viewModel.selectSection(section)
                    selectedDestination = AppDestination.Section
                    selectedCollectionId = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                    detailReturnTarget = DetailReturnTarget.Home
                    selectedMediaId = trackedMedia.item.id
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedAuthor != null && selectedMedia == null) {
            AuthorDetailScreen(
                author = selectedAuthor.orEmpty(),
                items = selectedAuthorItems,
                accent = uiState.selectedSection.accent,
                onBack = navigateBackFromAuthor,
                onMediaClick = { trackedMedia ->
                    detailReturnTarget = DetailReturnTarget.Author(selectedAuthor.orEmpty())
                    selectedAuthor = null
                    selectedMediaId = trackedMedia.item.id
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedCollection != null && selectedMedia == null) {
            CollectionDetailScreen(
                collection = selectedCollection,
                items = selectedCollectionItems,
                accent = uiState.selectedSection.accent,
                onBack = navigateBackFromCollection,
                onMediaClick = {
                    detailReturnTarget = DetailReturnTarget.Collection(selectedCollection.id)
                    selectedMediaId = it.item.id
                },
                onAddToCollection = { collection, nextOrder ->
                    viewModel.clearMetadataSearch()
                    addTargetCollection = collection
                    addTargetCollectionOrder = nextOrder
                    isAdding = true
                },
                onRenameCollection = viewModel::updateMediaCollectionName,
                onDeleteCollection = viewModel::deleteMediaCollection,
                onUpdateCollectionItemOrder = viewModel::updateCollectionItemOrder,
                onUpdateMediaItemCollection = { trackedMedia, collectionId, collectionSortOrder ->
                    viewModel.updateMediaItemDetails(
                        mediaItemId = trackedMedia.item.id,
                        title = trackedMedia.item.title,
                        collectionId = collectionId,
                        newCollectionName = null,
                        collectionSortOrder = collectionSortOrder,
                        progressTotal = trackedMedia.item.progressTotal,
                        ownershipType = trackedMedia.item.ownership.type,
                    )
                },
                onCollectionActionMessage = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (selectedMedia == null) {
            HomeScreen(
                uiState = uiState,
                metadataUiState = metadataUiState,
                onMediaClick = {
                    detailReturnTarget = DetailReturnTarget.Section
                    collectionReturnTarget = CollectionReturnTarget.Section
                    selectedMediaId = it.item.id
                },
                onCollectionClick = {
                    selectedCollectionId = it.id
                    selectedAuthor = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                    isAdding = false
                },
                onAuthorClick = { author ->
                    selectedAuthor = author
                    selectedCollectionId = null
                    authorReturnTarget = AuthorReturnTarget.Section
                    isAdding = false
                },
                onManualAddClick = {
                    viewModel.clearMetadataSearch()
                    isAdding = true
                    addTargetCollection = null
                    addTargetCollectionOrder = null
                    selectedCollectionId = null
                    collectionReturnTarget = CollectionReturnTarget.Section
                },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                onMetadataSearch = { viewModel.searchMetadataSuggestions() },
                onMetadataSearchSubmitted = { viewModel.searchMetadataSuggestions(forceShortQuery = true) },
                onApiSuggestionSelected = { suggestion ->
                    when (val duplicate = uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> openTrackedMedia(duplicate.trackedMedia)
                        is DuplicateMatch.Possible -> {
                            pendingPossibleDuplicate = PendingPossibleDuplicate(
                                suggestion = suggestion,
                                trackedMedia = duplicate.trackedMedia,
                                requiresAddTransition = true,
                            )
                        }
                        DuplicateMatch.None -> {
                            viewModel.selectMetadataSuggestion(suggestion)
                            isAdding = true
                            addTargetCollection = null
                            addTargetCollectionOrder = null
                            selectedCollectionId = null
                            collectionReturnTarget = CollectionReturnTarget.Section
                        }
                    }
                },
                duplicateStateForSuggestion = { suggestion ->
                    when (uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                        is DuplicateMatch.Exact -> MetadataDuplicateState.Exact
                        is DuplicateMatch.Possible -> MetadataDuplicateState.Possible
                        DuplicateMatch.None -> MetadataDuplicateState.None
                    }
                },
                onStatusFilterChange = viewModel::updateStatusFilter,
                onGroupModeChange = viewModel::updateGroupMode,
                onSortModeChange = viewModel::updateSortMode,
                onSortDirectionChange = viewModel::updateSortDirection,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            val actions = remember(selectedMedia.item.id) {
                DetailHeaderActions()
            }
            actions.isRefreshingMetadata = uiState.refreshingMetadataItemId == selectedMedia.item.id
            detailActions = actions
            DetailScreen(
                trackedMedia = selectedMedia,
                allTrackedMedia = uiState.allTrackedItems,
                accent = uiState.selectedSection.accent,
                headerActions = actions,
                onBack = navigateBackFromDetail,
                onStartNewSession = viewModel::startNewSession,
                onUpdateSessionDetails = viewModel::updateSessionDetails,
                onDeletePastSession = viewModel::deletePastSession,
                onDeleteProgressUpdate = viewModel::deleteProgressUpdate,
                onAddExternalRating = viewModel::addExternalRating,
                onUpdateExternalRating = viewModel::updateExternalRating,
                onSetPrimaryExternalRating = viewModel::setPrimaryExternalRating,
                onDeleteExternalRating = viewModel::deleteExternalRating,
                onUpdateMediaItemDetails = viewModel::updateMediaItemDetails,
                onUpdateMediaItemMetadata = viewModel::updateMediaItemMetadata,
                onRefreshMediaItemMetadata = { startMetadataRefresh(selectedMedia) },
                onLinkMediaMetadata = { startMetadataLink(selectedMedia) },
                onDeleteMediaItem = { mediaItemId ->
                    viewModel.deleteMediaItem(mediaItemId)
                    selectedMediaId = null
                },
                onCollectionClick = {
                    val collectionId = selectedMedia.collection?.id ?: return@DetailScreen
                    selectedDestination = AppDestination.Section
                    selectedMediaId = null
                    selectedCollectionId = collectionId
                    collectionReturnTarget = CollectionReturnTarget.Detail(selectedMedia.item.id)
                },
                onAuthorClick = { author ->
                    selectedDestination = AppDestination.Section
                    selectedMediaId = null
                    selectedCollectionId = null
                    selectedAuthor = author
                    authorReturnTarget = AuthorReturnTarget.Detail(selectedMedia.item.id)
                },
                onRelatedMediaClick = openRelatedMedia,
                askForGoodreadsRating = askForGoodreadsRating,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, top = 82.dp, end = 16.dp),
        )
    }

    if (showRestoreList) {
        val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm") }
        val safetyBackups = remember {
            context.getExternalFilesDir(null)
                ?.listFiles { _, name ->
                    name.startsWith("omnilog-pre-import-") && name.endsWith(".json")
                }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
        OmnilogAlertDialog(
            onDismissRequest = { showRestoreList = false },
            title = stringResource(R.string.restore_previous_backup_title),
            text = {
                if (safetyBackups.isEmpty()) {
                    Text(text = stringResource(R.string.restore_previous_backup_empty))
                } else {
                    Column {
                        safetyBackups.forEach { file ->
                            val label = remember(file) {
                                val dt = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(file.lastModified()),
                                    ZoneId.systemDefault(),
                                )
                                dt.format(displayFormatter)
                            }
                            TextButton(
                                onClick = {
                                    showRestoreList = false
                                    coroutineScope.launch {
                                        val readResult = runCatching {
                                            withContext(Dispatchers.IO) {
                                                file.readText(Charsets.UTF_8)
                                            }
                                        }
                                        val backupJson = readResult.getOrNull()
                                        if (backupJson == null) {
                                            snackbarHostState.showSnackbar(importReadErrorMessage)
                                            return@launch
                                        }
                                        val previewResult = runCatching {
                                            PendingBackupImport(
                                                json = backupJson,
                                                preview = viewModel.previewBackupJson(backupJson),
                                            )
                                        }
                                        pendingImport = previewResult.getOrNull()
                                        previewResult.exceptionOrNull()?.let { error ->
                                            val message = when (error) {
                                                is UnsupportedBackupSchemaException -> importUnsupportedMessage
                                                else -> importInvalidMessage
                                            }
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 0.dp,
                                    vertical = 4.dp,
                                ),
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRestoreList = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImport?.let { backupImport ->
        OmnilogAlertDialog(
            onDismissRequest = { pendingImport = null },
            title = stringResource(R.string.import_backup_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.import_backup_message_with_summary,
                        backupImport.preview.schemaVersion,
                        backupImport.preview.collectionCount,
                        backupImport.preview.mediaItemCount,
                        backupImport.preview.mediaCreditCount,
                        backupImport.preview.trackingSessionCount,
                        backupImport.preview.progressUpdateCount,
                        backupImport.preview.externalRatingCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        pendingImportConfirmation = backupImport
                    },
                ) {
                    Text(text = stringResource(R.string.import_backup_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImportConfirmation?.let { backupImport ->
        OmnilogAlertDialog(
            onDismissRequest = { pendingImportConfirmation = null },
            title = stringResource(R.string.import_backup_final_title),
            text = { Text(text = stringResource(R.string.import_backup_final_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            runCatching {
                                val safetyJson = viewModel.exportBackupJson()
                                val timestamp = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
                                val file = File(
                                    context.getExternalFilesDir(null),
                                    "omnilog-pre-import-$timestamp.json",
                                )
                                withContext(Dispatchers.IO) {
                                    file.writeText(safetyJson, Charsets.UTF_8)
                                }
                            }
                            val result = runCatching {
                                viewModel.importBackupJson(backupImport.json)
                            }
                            pendingImportConfirmation = null
                            if (result.isSuccess) {
                                selectedMediaId = null
                                selectedCollectionId = null
                                isAdding = false
                                snackbarHostState.showSnackbar(importSuccessMessage)
                            } else {
                                val message = when (result.exceptionOrNull()) {
                                    is UnsupportedBackupSchemaException -> importUnsupportedMessage
                                    else -> importInvalidMessage
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.import_backup_final_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportConfirmation = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImdbCsvImport?.let { imdbImport ->
        OmnilogAlertDialog(
            onDismissRequest = { pendingImdbCsvImport = null },
            title = stringResource(R.string.imdb_import_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.imdb_import_message_with_summary,
                        imdbImport.preview.importableRows,
                        imdbImport.preview.totalRows,
                        imdbImport.preview.skippedDuplicateRows,
                        imdbImport.preview.unsupportedRows,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = runCatching {
                                viewModel.importImdbCsv(imdbImport.csv)
                            }
                            pendingImdbCsvImport = null
                            result.fold(
                                onSuccess = { importResult ->
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.imdb_import_success_with_summary,
                                            importResult.importedRows,
                                            importResult.skippedDuplicateRows,
                                            importResult.unsupportedRows,
                                        ),
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(imdbImportInvalidMessage)
                                },
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.imdb_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImdbCsvImport = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingStoryGraphCsvImport?.let { storyGraphImport ->
        OmnilogAlertDialog(
            onDismissRequest = { pendingStoryGraphCsvImport = null },
            title = stringResource(R.string.storygraph_import_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.storygraph_import_message_with_summary,
                        storyGraphImport.preview.importableRows,
                        storyGraphImport.preview.totalRows,
                        storyGraphImport.preview.skippedDuplicateRows,
                        storyGraphImport.preview.unsupportedRows,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = runCatching {
                                viewModel.importStoryGraphCsv(storyGraphImport.csv)
                            }
                            pendingStoryGraphCsvImport = null
                            result.fold(
                                onSuccess = { importResult ->
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.storygraph_import_success_with_summary,
                                            importResult.importedRows,
                                            importResult.skippedDuplicateRows,
                                            importResult.unsupportedRows,
                                        ),
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(storyGraphImportInvalidMessage)
                                },
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.storygraph_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStoryGraphCsvImport = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingMyAnimeListXmlImport?.let { malImport ->
        OmnilogAlertDialog(
            onDismissRequest = { pendingMyAnimeListXmlImport = null },
            title = stringResource(R.string.mal_import_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.mal_import_message_with_summary,
                        malImport.preview.importableRows,
                        malImport.preview.totalRows,
                        malImport.preview.skippedDuplicateRows,
                        malImport.preview.unsupportedRows,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = runCatching {
                                viewModel.importMyAnimeListXml(malImport.xml)
                            }
                            pendingMyAnimeListXmlImport = null
                            result.fold(
                                onSuccess = { importResult ->
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.mal_import_success_with_summary,
                                            importResult.importedRows,
                                            importResult.skippedDuplicateRows,
                                            importResult.unsupportedRows,
                                        ),
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(myAnimeListImportInvalidMessage)
                                },
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.mal_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMyAnimeListXmlImport = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingMetadataRefreshPreview?.let { preview ->
        MetadataRefreshConfirmationDialog(
            preview = preview,
            onDismiss = { pendingMetadataRefreshPreview = null },
            onConfirm = { selectedFields ->
                coroutineScope.launch {
                    val result = viewModel.applyMediaItemMetadataRefresh(preview, selectedFields)
                    pendingMetadataRefreshPreview = null
                    snackbarHostState.showSnackbar(
                        if (result.getOrDefault(false)) {
                            metadataRefreshSuccessMessage
                        } else {
                            metadataRefreshErrorMessage
                        },
                    )
                }
            },
        )
    }

    metadataLinkTarget?.let { target ->
        val providerName = target.item.type.metadataLinkProviderName()
        LaunchedEffect(target.item.id, metadataLinkQuery) {
            val query = metadataLinkQuery.trim()
            if (query.length < 2) {
                metadataLinkSearchRequestId += 1
                metadataLinkSuggestions = emptyList()
                isMetadataLinkLoading = false
                hasMetadataLinkError = false
            } else {
                delay(250)
                searchMetadataLink(target, query)
            }
        }
        Dialog(
            onDismissRequest = { metadataLinkTarget = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                shape = RoundedCornerShape(14.dp),
                color = OmnilogColors.AppBackground,
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.metadata_link_title, providerName),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeaderInk,
                    )
                    Text(
                        text = stringResource(R.string.metadata_link_message, displayMediaTitle(target.item.title)),
                        color = HeaderMuted,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = metadataLinkQuery,
                            onValueChange = { metadataLinkQuery = it },
                            label = {
                                Text(text = stringResource(R.string.metadata_link_search_label, providerName))
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            enabled = !isMetadataLinkLoading,
                            onClick = { searchMetadataLink(target, metadataLinkQuery) },
                        ) {
                            Text(text = stringResource(R.string.metadata_link_search))
                        }
                    }

                    when {
                        isMetadataLinkLoading -> Text(
                            text = stringResource(R.string.metadata_link_loading),
                            color = HeaderMuted,
                        )
                        hasMetadataLinkError -> Text(
                            text = stringResource(R.string.metadata_link_search_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                        metadataLinkSuggestions.isEmpty() -> Text(
                            text = stringResource(R.string.metadata_link_empty),
                            color = HeaderMuted,
                        )
                        else -> LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(metadataLinkSuggestions) { suggestion ->
                                MetadataSuggestionRow(
                                    suggestion = suggestion,
                                    accent = target.item.type.homeSection().accent,
                                    duplicateState = MetadataDuplicateState.None,
                                    showSourceChip = false,
                                    onClick = {
                                        coroutineScope.launch {
                                            val result = runCatching {
                                                viewModel.linkMediaItemMetadata(target.item.id, suggestion)
                                            }
                                            metadataLinkTarget = null
                                            snackbarHostState.showSnackbar(
                                                if (result.getOrDefault(false)) {
                                                    metadataLinkSuccessMessage
                                                } else {
                                                    metadataLinkErrorMessage
                                                },
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { metadataLinkTarget = null }) {
                            Text(text = stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }

    pendingPossibleDuplicate?.let { duplicate ->
        OmnilogAlertDialog(
            onDismissRequest = { pendingPossibleDuplicate = null },
            title = stringResource(R.string.possible_duplicate_title),
            text = {
                Text(
                    text = stringResource(
                        R.string.possible_duplicate_message,
                        duplicate.trackedMedia.item.title,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPossibleDuplicate = null
                        openTrackedMedia(duplicate.trackedMedia)
                    },
                ) {
                    Text(text = stringResource(R.string.possible_duplicate_open_existing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val suggestion = duplicate.suggestion
                        val needsTransition = duplicate.requiresAddTransition
                        pendingPossibleDuplicate = null
                        viewModel.selectMetadataSuggestion(suggestion)
                        if (needsTransition) {
                            isAdding = true
                            selectedCollectionId = null
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.possible_duplicate_continue_create))
                }
            },
        )
    }

}

private data class PendingBackupImport(
    val json: String,
    val preview: BackupPreview,
)

private data class PendingImdbCsvImport(
    val csv: String,
    val preview: ImdbCsvPreview,
)

private data class PendingStoryGraphCsvImport(
    val csv: String,
    val preview: StoryGraphCsvPreview,
)

private data class PendingMyAnimeListXmlImport(
    val xml: String,
    val preview: MyAnimeListXmlPreview,
)

@Composable
private fun MetadataRefreshConfirmationDialog(
    preview: MetadataRefreshPreview,
    onDismiss: () -> Unit,
    onConfirm: (Set<MetadataRefreshField>) -> Unit,
) {
    var selectedFields by remember(preview) {
        mutableStateOf(
            preview.changes
                .filterNot { change -> change.isLocallyOverridden }
                .map { change -> change.field }
                .toSet(),
        )
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
            color = OmnilogColors.AppBackground,
            border = BorderStroke(1.dp, OmnilogColors.AppLine),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.metadata_refresh_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeaderInk,
                )
                Text(
                    text = stringResource(R.string.metadata_refresh_confirm_message),
                    color = HeaderMuted,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(preview.changes) { change ->
                        val isSelected = change.field in selectedFields
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OmnilogColors.AppPanel,
                            border = BorderStroke(1.dp, OmnilogColors.AppLine),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFields = if (isSelected) {
                                            selectedFields - change.field
                                        } else {
                                            selectedFields + change.field
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedFields = if (checked) {
                                            selectedFields + change.field
                                        } else {
                                            selectedFields - change.field
                                        }
                                    },
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = stringResource(change.field.labelResId()),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HeaderInk,
                                    )
                                    if (change.isLocallyOverridden) {
                                        Text(
                                            text = stringResource(R.string.metadata_refresh_locally_edited),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                    MetadataChangeValue(
                                        label = stringResource(R.string.metadata_refresh_current_value),
                                        value = change.currentValue,
                                    )
                                    MetadataChangeValue(
                                        label = stringResource(R.string.metadata_refresh_new_value),
                                        value = change.newValue,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = selectedFields.isNotEmpty(),
                        onClick = { onConfirm(selectedFields) },
                    ) {
                        Text(text = stringResource(R.string.metadata_refresh_apply_selected))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChangeValue(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                append(label)
                append(": ")
            }
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = HeaderMuted,
    )
}

@StringRes
private fun MetadataRefreshField.labelResId(): Int {
    return when (this) {
        MetadataRefreshField.Title -> R.string.metadata_refresh_field_title
        MetadataRefreshField.OriginalTitle -> R.string.metadata_refresh_field_original_title
        MetadataRefreshField.ReleaseYear -> R.string.metadata_refresh_field_release_year
        MetadataRefreshField.Language -> R.string.metadata_refresh_field_language
        MetadataRefreshField.ProgressTotal -> R.string.metadata_refresh_field_progress_total
        MetadataRefreshField.Genres -> R.string.metadata_refresh_field_genres
        MetadataRefreshField.Creators -> R.string.metadata_refresh_field_creators
        MetadataRefreshField.Credits -> R.string.metadata_refresh_field_credits
        MetadataRefreshField.Cover -> R.string.metadata_refresh_field_cover
        MetadataRefreshField.Synopsis -> R.string.metadata_refresh_field_synopsis
        MetadataRefreshField.SourceUrl -> R.string.metadata_refresh_field_source_url
        MetadataRefreshField.ExternalRating -> R.string.metadata_refresh_field_external_rating
        MetadataRefreshField.ExternalRatings -> R.string.metadata_refresh_field_external_ratings
        MetadataRefreshField.ProviderStats -> R.string.metadata_refresh_field_provider_stats
    }
}

private data class PendingPossibleDuplicate(
    val suggestion: MetadataSuggestion,
    val trackedMedia: TrackedMedia,
    val requiresAddTransition: Boolean = false,
)

class DetailHeaderActions {
    var isEditingItemDetails by mutableStateOf(false)
    var isMenuExpanded by mutableStateOf(false)
    var isRefreshingMetadata by mutableStateOf(false)
    var isManagingExternalRatings by mutableStateOf(false)
    var onCloseExternalRatings: () -> Unit = {}
    var showLinkMetadata by mutableStateOf(false)
    var linkMetadataLabelResId by mutableStateOf(R.string.link_metadata_movie)
    var onDeleteRequested: () -> Unit = {}
    var onManageExternalRatingsRequested: () -> Unit = {}
    var onRefreshMetadataRequested: () -> Unit = {}
    var onLinkMetadataRequested: () -> Unit = {}
}

class BackupHeaderActions {
    var isMenuExpanded by mutableStateOf(false)
    var onExportBackupRequested: () -> Unit = {}
    var onImportBackupRequested: () -> Unit = {}
    var onImportMyAnimeListXmlRequested: () -> Unit = {}
    var onImportImdbCsvRequested: () -> Unit = {}
    var onImportStoryGraphCsvRequested: () -> Unit = {}
    var onRestoreBackupRequested: () -> Unit = {}
}

private enum class AppDestination {
    Home,
    Stats,
    Section,
}

private sealed interface DetailReturnTarget {
    data object Home : DetailReturnTarget
    data object Section : DetailReturnTarget
    data class Collection(val collectionId: Long) : DetailReturnTarget
    data class Author(val author: String) : DetailReturnTarget
}

private sealed interface CollectionReturnTarget {
    data object Section : CollectionReturnTarget
    data class Detail(val mediaItemId: Long) : CollectionReturnTarget
}

private sealed interface AuthorReturnTarget {
    data object Section : AuthorReturnTarget
    data class Detail(val mediaItemId: Long) : AuthorReturnTarget
}

private sealed interface DuplicateMatch {
    data object None : DuplicateMatch
    data class Exact(val trackedMedia: TrackedMedia) : DuplicateMatch
    data class Possible(val trackedMedia: TrackedMedia) : DuplicateMatch
}

private fun List<TrackedMedia>.findDuplicateFor(suggestion: MetadataSuggestion): DuplicateMatch {
    val exactMatch = firstOrNull { trackedMedia ->
        trackedMedia.item.type == suggestion.mediaType &&
            trackedMedia.item.metadataSource == suggestion.source &&
            trackedMedia.item.metadataExternalId == suggestion.externalId
    }
    if (exactMatch != null) {
        return DuplicateMatch.Exact(exactMatch)
    }

    val possibleMatch = firstOrNull { trackedMedia ->
        trackedMedia.item.type == suggestion.mediaType &&
            trackedMedia.hasCompatibleReleaseYear(suggestion) &&
            trackedMedia.titleCandidates().intersect(suggestion.titleCandidates()).isNotEmpty()
    }

    return possibleMatch?.let(DuplicateMatch::Possible) ?: DuplicateMatch.None
}

private fun TrackedMedia.hasCompatibleReleaseYear(suggestion: MetadataSuggestion): Boolean {
    val existingYear = item.releaseYear
    val suggestionYear = suggestion.releaseYear
    return existingYear == null || suggestionYear == null || existingYear == suggestionYear
}

private fun TrackedMedia.titleCandidates(): Set<String> {
    return listOf(item.title, item.originalTitle)
        .mapNotNull { it?.normalizedDuplicateTitle()?.takeIf(String::isNotBlank) }
        .toSet()
}

private fun MetadataSuggestion.titleCandidates(): Set<String> {
    return listOf(title, originalTitle)
        .mapNotNull { it?.normalizedDuplicateTitle()?.takeIf(String::isNotBlank) }
        .toSet()
}

private fun String.normalizedDuplicateTitle(): String {
    val withoutMarks = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutMarks
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private fun com.nilpo.contenttracker.core.model.MediaType.homeSection(): MediaSection =
    when (this) {
        com.nilpo.contenttracker.core.model.MediaType.Anime -> MediaSection.Anime
        com.nilpo.contenttracker.core.model.MediaType.Book -> MediaSection.Books
        com.nilpo.contenttracker.core.model.MediaType.Movie,
        com.nilpo.contenttracker.core.model.MediaType.TvShow,
            -> MediaSection.Movies
        com.nilpo.contenttracker.core.model.MediaType.Game -> MediaSection.Games
    }

private fun com.nilpo.contenttracker.core.model.MediaType.metadataLinkProviderName(): String =
    when (this) {
        com.nilpo.contenttracker.core.model.MediaType.Anime -> "AniList / MyAnimeList"
        com.nilpo.contenttracker.core.model.MediaType.Book -> "OpenLibrary / Google Books"
        com.nilpo.contenttracker.core.model.MediaType.Movie,
        com.nilpo.contenttracker.core.model.MediaType.TvShow,
            -> "TMDB"
        com.nilpo.contenttracker.core.model.MediaType.Game,
            -> ""
    }

@Composable
private fun OmnilogBottomBar(
    selectedDestination: AppDestination,
    selectedSection: MediaSection,
    onHomeClick: () -> Unit,
    onSectionClick: (MediaSection) -> Unit,
) {
    Surface(
        color = HeaderBackground,
        contentColor = HeaderInk,
        shadowElevation = 12.dp,
    ) {
        Column {
            HorizontalDivider(color = HeaderLine.copy(alpha = 0.72f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderPanel.copy(alpha = 0.74f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OmnilogNavItem(
                    labelResId = R.string.nav_home,
                    iconResId = R.drawable.ic_nav_home,
                    accent = OmnilogColors.Dashboard,
                    selected = selectedDestination == AppDestination.Home ||
                        selectedDestination == AppDestination.Stats,
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f),
                )
                MediaSection.entries.forEach { section ->
                    OmnilogNavItem(
                        labelResId = section.titleResId,
                        iconResId = section.navIconResId(),
                        accent = section.accent,
                        selected = selectedDestination == AppDestination.Section && selectedSection == section,
                        onClick = { onSectionClick(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OmnilogNavItem(
    @StringRes labelResId: Int,
    @DrawableRes iconResId: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) accent else HeaderMuted.copy(alpha = 0.76f)
    val containerColor = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (selected) accent.copy(alpha = 0.36f) else Color.Transparent

    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(start = 4.dp, top = 7.dp, end = 4.dp, bottom = 11.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(if (selected) 28.dp else 26.dp),
                tint = contentColor,
            )
            Text(
                text = stringResource(labelResId),
                color = if (selected) HeaderInk else HeaderMuted.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@DrawableRes
private fun MediaSection.navIconResId(): Int {
    return when (this) {
        MediaSection.Anime -> R.drawable.ic_nav_anime
        MediaSection.Books -> R.drawable.ic_nav_books
        MediaSection.Movies -> R.drawable.ic_nav_tv
        MediaSection.Games -> R.drawable.ic_nav_games
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OmnilogTopBar(
    accent: Color,
    showBackNavigation: Boolean,
    showDetailActions: Boolean,
    showBackupActions: Boolean,
    showMyAnimeListImport: Boolean,
    showImdbImport: Boolean,
    showStoryGraphImport: Boolean,
    detailActions: DetailHeaderActions,
    backupActions: BackupHeaderActions,
    askForGoodreadsRating: Boolean,
    onAskForGoodreadsRatingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HeaderBackground,
            titleContentColor = HeaderInk,
            navigationIconContentColor = HeaderInk,
            actionIconContentColor = HeaderInk,
        ),
        navigationIcon = {
            if (showBackNavigation) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = HeaderInk,
                    )
                    Text(
                        text = "‹",
                        color = Color.Transparent,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        title = {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) {
                        append("Omni")
                    }
                    withStyle(SpanStyle(color = HeaderInk, fontWeight = FontWeight.ExtraBold)) {
                        append("log")
                    }
                },
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                )
            },
        actions = {
            if (showDetailActions) {
                Box {
                    IconButton(onClick = { detailActions.isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = HeaderMuted,
                        )
                        Text(
                            text = "⋮",
                            color = Color.Transparent,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    DropdownMenu(
                        expanded = detailActions.isMenuExpanded,
                        onDismissRequest = { detailActions.isMenuExpanded = false },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = HeaderPanel,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                    ) {
                        HeaderMenuItem(
                            text = stringResource(
                                if (detailActions.isEditingItemDetails) {
                                    R.string.done_editing
                                } else {
                                    R.string.edit
                                },
                            ),
                            onClick = {
                                detailActions.isEditingItemDetails = !detailActions.isEditingItemDetails
                                detailActions.isMenuExpanded = false
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(R.string.detail_external_scores),
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onManageExternalRatingsRequested()
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(
                                if (detailActions.isRefreshingMetadata) {
                                    R.string.refresh_metadata_loading
                                } else {
                                    R.string.refresh_metadata
                                },
                            ),
                            enabled = !detailActions.isRefreshingMetadata,
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onRefreshMetadataRequested()
                            },
                        )
                        if (detailActions.showLinkMetadata) {
                            HeaderMenuItem(
                                text = stringResource(detailActions.linkMetadataLabelResId),
                                enabled = !detailActions.isRefreshingMetadata,
                                onClick = {
                                    detailActions.isMenuExpanded = false
                                    detailActions.onLinkMetadataRequested()
                                },
                            )
                        }
                        HeaderMenuItem(
                            text = stringResource(R.string.delete),
                            destructive = true,
                            onClick = {
                                detailActions.isMenuExpanded = false
                                detailActions.onDeleteRequested()
                            },
                        )
                    }
                }
            } else if (showBackupActions) {
                Box {
                    IconButton(onClick = { backupActions.isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(R.string.account_menu),
                            tint = HeaderMuted,
                        )
                    }
                    DropdownMenu(
                        expanded = backupActions.isMenuExpanded,
                        onDismissRequest = { backupActions.isMenuExpanded = false },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = HeaderPanel,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                    ) {
                        HeaderMenuItem(
                            text = stringResource(R.string.export_backup),
                            onClick = {
                                backupActions.isMenuExpanded = false
                                backupActions.onExportBackupRequested()
                            },
                        )
                        HeaderMenuItem(
                            text = stringResource(R.string.import_backup),
                            onClick = {
                                backupActions.isMenuExpanded = false
                                backupActions.onImportBackupRequested()
                            },
                        )
                        if (showMyAnimeListImport) {
                            HeaderMenuItem(
                                text = stringResource(R.string.import_mal_xml),
                                onClick = {
                                    backupActions.isMenuExpanded = false
                                    backupActions.onImportMyAnimeListXmlRequested()
                                },
                            )
                        }
                        if (showImdbImport) {
                            HeaderMenuItem(
                                text = stringResource(R.string.import_imdb_csv),
                                onClick = {
                                    backupActions.isMenuExpanded = false
                                    backupActions.onImportImdbCsvRequested()
                                },
                            )
                        }
                        if (showStoryGraphImport) {
                            HeaderMenuItem(
                                text = stringResource(R.string.import_storygraph_csv),
                                onClick = {
                                    backupActions.isMenuExpanded = false
                                    backupActions.onImportStoryGraphCsvRequested()
                                },
                            )
                        }
                        HeaderMenuItem(
                            text = stringResource(R.string.restore_previous_backup),
                            onClick = {
                                backupActions.isMenuExpanded = false
                                backupActions.onRestoreBackupRequested()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "Ask for Goodreads rating") },
                            onClick = { onAskForGoodreadsRatingChange(!askForGoodreadsRating) },
                            trailingIcon = {
                                Switch(
                                    checked = askForGoodreadsRating,
                                    onCheckedChange = onAskForGoodreadsRatingChange,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun HeaderMenuItem(
    text: String,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val textColor = when {
        !enabled -> HeaderMuted.copy(alpha = 0.62f)
        destructive -> MaterialTheme.colorScheme.error
        else -> HeaderInk
    }

    Row(
        modifier = Modifier
            .widthIn(min = 148.dp)
            .background(HeaderPanel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun collectionItemComparator(): Comparator<TrackedMedia> =
    compareBy<TrackedMedia> { it.item.collectionSortOrder ?: Double.MAX_VALUE }
        .thenBy { it.item.releaseYear ?: Int.MAX_VALUE }
        .thenBy { it.item.title.lowercase() }

private fun List<TrackedMedia>.toAddCollectionOptions(): List<AddCollectionOption> {
    return filter { trackedMedia -> trackedMedia.collection != null }
        .groupBy { trackedMedia -> trackedMedia.collection!!.id }
        .mapNotNull { (_, trackedItems) ->
            val collection = trackedItems.firstNotNullOfOrNull { trackedMedia -> trackedMedia.collection }
                ?: return@mapNotNull null
            AddCollectionOption(
                collection = collection,
                mediaTypes = trackedItems.map { trackedMedia -> trackedMedia.item.type }.toSet(),
            )
        }
        .sortedBy { option -> option.collection.name.lowercase() }
}

private val HeaderBackground = OmnilogColors.AppBackground
private val HeaderPanel = OmnilogColors.AppPanel
private val HeaderLine = OmnilogColors.AppLine
private val HeaderInk = OmnilogColors.AppInk
private val HeaderMuted = OmnilogColors.AppMuted
