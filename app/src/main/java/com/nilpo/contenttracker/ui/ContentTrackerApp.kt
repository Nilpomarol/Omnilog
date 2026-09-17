package com.nilpo.contenttracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.backup.AutoBackupPreferences
import com.nilpo.contenttracker.core.backup.AutoBackupScheduler
import com.nilpo.contenttracker.core.backup.backupFolderLabel
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.repository.BackupPreview
import com.nilpo.contenttracker.core.repository.ImdbCsvPreview
import com.nilpo.contenttracker.core.repository.MalformedProviderCsvException
import com.nilpo.contenttracker.core.repository.ProviderCsvValidationException
import com.nilpo.contenttracker.core.repository.ProviderCsvValidationIssue
import com.nilpo.contenttracker.core.repository.ProviderNoImportableReason
import com.nilpo.contenttracker.core.repository.ProviderImportPreview
import com.nilpo.contenttracker.core.repository.PreparedImdbCsvImport
import com.nilpo.contenttracker.core.repository.PreparedMyAnimeListXmlImport
import com.nilpo.contenttracker.core.repository.PreparedStoryGraphCsvImport
import com.nilpo.contenttracker.core.repository.ProviderRejectedReason
import com.nilpo.contenttracker.core.repository.noImportableReason
import com.nilpo.contenttracker.core.repository.rejectedGroups
import com.nilpo.contenttracker.core.repository.MalformedProviderXmlException
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import com.nilpo.contenttracker.core.repository.defaultSelectedMetadataFields
import com.nilpo.contenttracker.core.repository.requiresMetadataConfirmation
import com.nilpo.contenttracker.core.repository.MyAnimeListXmlPreview
import com.nilpo.contenttracker.core.repository.MyAnimeListAccountImportPreview
import com.nilpo.contenttracker.core.repository.StoryGraphCsvPreview
import com.nilpo.contenttracker.core.repository.ProviderImportEncodingException
import com.nilpo.contenttracker.core.repository.ProviderImportFileTooLargeException
import com.nilpo.contenttracker.core.repository.UnsupportedBackupSchemaException
import com.nilpo.contenttracker.core.repository.readProviderImportText
import com.nilpo.contenttracker.core.imports.AnimeTitlePreference
import com.nilpo.contenttracker.core.imports.AnimeTitlePreferences
import com.nilpo.contenttracker.core.imports.ImportCoverageItem
import com.nilpo.contenttracker.core.imports.ImportIssueItem
import com.nilpo.contenttracker.ui.add.AddMediaScreen
import com.nilpo.contenttracker.ui.add.DashboardStyleSearchBar
import com.nilpo.contenttracker.ui.add.MetadataDuplicateState
import com.nilpo.contenttracker.ui.add.MetadataSuggestionRow
import com.nilpo.contenttracker.ui.detail.DetailScreen
import com.nilpo.contenttracker.ui.detail.ActivitySheet
import com.nilpo.contenttracker.ui.common.EmptyStateAction
import com.nilpo.contenttracker.ui.common.OmnilogAlertDialog
import com.nilpo.contenttracker.ui.common.CelebratedObjectives
import com.nilpo.contenttracker.ui.common.CompletionCelebration
import com.nilpo.contenttracker.ui.common.ObjectiveCelebration
import com.nilpo.contenttracker.ui.common.ObjectiveStep
import com.nilpo.contenttracker.ui.common.withoutCelebrated
import com.nilpo.contenttracker.ui.common.CompletionReaction
import com.nilpo.contenttracker.ui.common.OmnilogSnackbar
import com.nilpo.contenttracker.ui.common.StatusReactionCard
import com.nilpo.contenttracker.ui.common.StatusReactionVisuals
import com.nilpo.contenttracker.ui.common.statusEventDeletionMessageResId
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.home.CollectionDetailScreen
import com.nilpo.contenttracker.ui.home.AuthorDetailScreen
import com.nilpo.contenttracker.ui.home.HomeScreen
import com.nilpo.contenttracker.ui.home.HomeLandingScreen
import com.nilpo.contenttracker.ui.home.HomeUiEvent
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.home.StatusListScreen
import com.nilpo.contenttracker.ui.home.creatorImageIsLogo
import com.nilpo.contenttracker.ui.home.creatorDetailLabelResId
import com.nilpo.contenttracker.ui.home.navIconResId
import com.nilpo.contenttracker.ui.home.themedAccent
import com.nilpo.contenttracker.core.model.creatorImageUrl
import com.nilpo.contenttracker.core.model.creatorImageAspectRatio
import com.nilpo.contenttracker.core.model.hasCreator
import com.nilpo.contenttracker.core.model.hasContributor
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.ui.imports.ImportHubDialog
import com.nilpo.contenttracker.ui.imports.ImportProgressBanner
import com.nilpo.contenttracker.ui.imports.MetadataDiffFieldList
import com.nilpo.contenttracker.ui.imports.userFacingMessage
import com.nilpo.contenttracker.ui.common.formatCollectionOrder
import com.nilpo.contenttracker.ui.profile.ProfileScreen
import com.nilpo.contenttracker.ui.profile.ProfilePreferences
import com.nilpo.contenttracker.ui.profile.latestCompletedCoverUrl
import com.nilpo.contenttracker.ui.navigation.AppRoute
import com.nilpo.contenttracker.ui.navigation.goBack
import com.nilpo.contenttracker.ui.navigation.push
import com.nilpo.contenttracker.ui.navigation.selectHome
import com.nilpo.contenttracker.ui.navigation.selectSection
import com.nilpo.contenttracker.ui.settings.SettingsScreen
import com.nilpo.contenttracker.ui.stats.StatsScreen
import com.nilpo.contenttracker.ui.timeline.TimelineScreen
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentTrackerApp(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timelineEntries by viewModel.timelineEntries.collectAsStateWithLifecycle()
    val contributors by viewModel.contributorDirectory.collectAsStateWithLifecycle()
    val metadataUiState by viewModel.metadataUiState.collectAsStateWithLifecycle()
    val recommendationUiState by viewModel.recommendationUiState.collectAsStateWithLifecycle()
    val malSyncState by viewModel.malSyncState.collectAsStateWithLifecycle()
    val importEnrichmentState by viewModel.importEnrichmentState.collectAsStateWithLifecycle()
    val metadataRefreshState by viewModel.metadataRefreshState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // The open completion celebration and the answer it is waiting for: true when the user undoes.
    var completionCelebration by remember { mutableStateOf<CompletionCelebrationRequest?>(null) }
    var objectiveCelebration by remember { mutableStateOf<ObjectiveCelebrationRequest?>(null) }
    val preferences = remember(context) {
        context.getSharedPreferences("omnilog_preferences", Context.MODE_PRIVATE)
    }
    var askForGoodreadsRating by remember {
        mutableStateOf(preferences.getBoolean("ask_for_goodreads_rating", false))
    }
    var autoBackupConfiguration by remember(context) {
        mutableStateOf(AutoBackupPreferences.read(context))
    }
    val backStack = rememberNavBackStack(AppRoute.Home)
    val currentRoute = backStack.last() as AppRoute
    val selectedRootRoute = backStack.lastOrNull { route ->
        route == AppRoute.Home || route is AppRoute.Section
    } as? AppRoute ?: AppRoute.Home
    var collectionBackRequest by remember { mutableStateOf<(() -> Unit)?>(null) }
    var collectionTopBarTitle by remember { mutableStateOf("") }
    var showRestoreList by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingImportConfirmation by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingImdbCsvImport by remember { mutableStateOf<PendingImdbCsvImport?>(null) }
    var pendingStoryGraphCsvImport by remember { mutableStateOf<PendingStoryGraphCsvImport?>(null) }
    var pendingMyAnimeListXmlImport by remember { mutableStateOf<PendingMyAnimeListXmlImport?>(null) }
    var pendingMyAnimeListAccountImport by remember {
        mutableStateOf<MyAnimeListAccountImportPreview?>(null)
    }
    var isProviderImporting by remember { mutableStateOf(false) }
    var showMalInitialSyncConfirmation by remember { mutableStateOf(false) }
    var metadataLinkTarget by remember { mutableStateOf<TrackedMedia?>(null) }
    var metadataLinkImportIssueId by remember { mutableStateOf<Long?>(null) }
    var metadataLinkQuery by remember { mutableStateOf("") }
    var metadataLinkSuggestions by remember { mutableStateOf<List<MetadataSuggestion>>(emptyList()) }
    var isMetadataLinkLoading by remember { mutableStateOf(false) }
    var hasMetadataLinkError by remember { mutableStateOf(false) }
    var metadataLinkSearchRequestId by remember { mutableStateOf(0) }
    var pendingMetadataChange by remember { mutableStateOf<PendingMetadataChange?>(null) }
    var showImportHub by remember { mutableStateOf(false) }
    var animeTitlePreference by remember(context) {
        mutableStateOf(AnimeTitlePreferences.read(context))
    }
    var showAnimeTitleBulkConfirmation by remember { mutableStateOf(false) }
    var showMetadataBulkConfirmation by remember { mutableStateOf(false) }
    var pendingPossibleDuplicate by remember { mutableStateOf<PendingPossibleDuplicate?>(null) }
    var pendingCreatedMediaId by remember { mutableStateOf<Long?>(null) }
    // A stored photo, or the latest finished cover when the profile follows it.
    val profileImage: Any? = remember(context, currentRoute, uiState.allTrackedItems) {
        val preferences = ProfilePreferences.from(context)
        if (preferences.getBoolean(ProfilePreferences.AVATAR_FOLLOWS_LAST_COMPLETED_KEY, false)) {
            uiState.allTrackedItems.latestCompletedCoverUrl()
        } else {
            preferences.getString(ProfilePreferences.AVATAR_IMAGE_PATH_KEY, null)
                ?.let(::File)
                ?.takeIf { file -> file.isFile }
        }
    }
    var detailActions by remember { mutableStateOf(DetailHeaderActions()) }
    val backupActions = remember { BackupHeaderActions() }
    val profileHeaderActions = remember { ProfileHeaderActions() }
    // The objective a Home tile asked the profile to scroll to; cleared once the profile has.
    var profileFocusObjectiveId by rememberSaveable { mutableStateOf<Long?>(null) }
    val timelineHeaderActions = remember { TimelineHeaderActions() }
    val addMediaHeaderActions = remember { AddMediaHeaderActions() }
    // Opened from the header's search icon; Home closes it when the search is dismissed or used.
    var homeSearchOpen by rememberSaveable { mutableStateOf(false) }
    var homeSearchQuery by rememberSaveable { mutableStateOf("") }
    var homeSearchFocused by remember { mutableStateOf(false) }
    var sectionSearchOpen by rememberSaveable { mutableStateOf(false) }
    // A list keeps its field in the header while a query is live, e.g. one handed over from Home.
    val sectionSearchActive = sectionSearchOpen || uiState.searchQuery.isNotBlank()
    // Closing clears the query, so reopening always starts from an empty field.
    val closeHomeSearch = {
        homeSearchOpen = false
        homeSearchQuery = ""
        homeSearchFocused = false
    }
    val closeSectionSearch = {
        sectionSearchOpen = false
        viewModel.updateSearchQuery("")
        viewModel.updateMetadataSearchQuery("")
    }
    // Changing screen drops an open search, so coming back does not reopen an empty field with the
    // keyboard up. A list's live query survives: it keeps the field showing on its own.
    LaunchedEffect(currentRoute) {
        sectionSearchOpen = false
        if (currentRoute != AppRoute.Home) closeHomeSearch()
    }
    val selectedMedia = (currentRoute as? AppRoute.MediaDetail)?.let { route ->
        uiState.allTrackedItems.firstOrNull { it.item.id == route.mediaItemId }
    }
    val currentSection = when (val route = currentRoute) {
        is AppRoute.Section -> route.section
        is AppRoute.CollectionDetail -> route.section
        is AppRoute.AuthorDetail -> route.section
        is AppRoute.AddMedia -> route.section
        is AppRoute.MediaDetail -> selectedMedia?.item?.type?.homeSection()
            ?: uiState.selectedSection

        else -> uiState.selectedSection
    }

    LaunchedEffect(
        selectedMedia?.item?.id,
        selectedMedia?.item?.metadataSource,
        selectedMedia?.item?.metadataExternalId,
    ) {
        selectedMedia?.let { current ->
            viewModel.loadRecommendations(current, uiState.allTrackedItems)
        }
    }
    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val exportErrorMessage = stringResource(R.string.backup_export_error)
    val importReadErrorMessage = stringResource(R.string.backup_import_read_error)
    val importSuccessMessage = stringResource(R.string.backup_import_success)
    val backupImportSyncMalAction = stringResource(R.string.backup_import_sync_mal_action)
    val importInvalidMessage = stringResource(R.string.backup_import_invalid)
    val importUnsupportedMessage = stringResource(R.string.backup_import_unsupported)
    val imdbImportReadErrorMessage = stringResource(R.string.imdb_import_read_error)
    val imdbImportInvalidMessage = stringResource(R.string.imdb_import_invalid)
    val imdbImportEmptyMessage = stringResource(R.string.imdb_import_empty)
    val imdbImportFileEmptyMessage = stringResource(R.string.imdb_import_file_empty)
    val imdbImportMissingColumnsMessage = stringResource(R.string.imdb_import_missing_columns)
    val imdbImportHeaderOnlyMessage = stringResource(R.string.imdb_import_header_only)
    val imdbImportNoUsableRowsMessage = stringResource(R.string.imdb_import_no_usable_rows)
    val imdbImportUnsupportedOnlyMessage = stringResource(R.string.imdb_import_unsupported_only)
    val imdbImportDuplicatesOnlyMessage = stringResource(R.string.imdb_import_duplicates_only)
    val storyGraphImportReadErrorMessage = stringResource(R.string.storygraph_import_read_error)
    val storyGraphImportInvalidMessage = stringResource(R.string.storygraph_import_invalid)
    val storyGraphImportEmptyMessage = stringResource(R.string.storygraph_import_empty)
    val storyGraphImportFileEmptyMessage = stringResource(R.string.storygraph_import_file_empty)
    val storyGraphImportMissingColumnsMessage = stringResource(R.string.storygraph_import_missing_columns)
    val storyGraphImportHeaderOnlyMessage = stringResource(R.string.storygraph_import_header_only)
    val storyGraphImportNoUsableRowsMessage = stringResource(R.string.storygraph_import_no_usable_rows)
    val storyGraphImportUnsupportedOnlyMessage = stringResource(R.string.storygraph_import_unsupported_only)
    val storyGraphImportDuplicatesOnlyMessage = stringResource(R.string.storygraph_import_duplicates_only)
    val myAnimeListImportReadErrorMessage = stringResource(R.string.mal_import_read_error)
    val myAnimeListImportInvalidMessage = stringResource(R.string.mal_import_invalid)
    val myAnimeListImportEmptyMessage = stringResource(R.string.mal_import_empty)
    val myAnimeListAccountImportErrorMessage = stringResource(R.string.mal_account_import_error)
    val myAnimeListAccountImportEmptyMessage = stringResource(R.string.mal_account_import_empty)
    val providerImportFailedMessage = stringResource(R.string.provider_import_failed)
    val providerImportEncodingErrorMessage = stringResource(R.string.provider_import_encoding_error)
    val providerImportTooLargeMessage = stringResource(R.string.provider_import_too_large)
    val providerImportMalformedCsvMessage = stringResource(R.string.provider_import_malformed_csv)
    val providerImportMalformedXmlMessage = stringResource(R.string.provider_import_malformed_xml)
    val imdbCsvValidationMessages = ProviderCsvValidationMessages(
        invalid = imdbImportInvalidMessage,
        emptyFile = imdbImportFileEmptyMessage,
        missingColumns = imdbImportMissingColumnsMessage,
        headerOnly = imdbImportHeaderOnlyMessage,
        noUsableRows = imdbImportNoUsableRowsMessage,
    )
    val storyGraphCsvValidationMessages = ProviderCsvValidationMessages(
        invalid = storyGraphImportInvalidMessage,
        emptyFile = storyGraphImportFileEmptyMessage,
        missingColumns = storyGraphImportMissingColumnsMessage,
        headerOnly = storyGraphImportHeaderOnlyMessage,
        noUsableRows = storyGraphImportNoUsableRowsMessage,
    )
    val metadataRefreshSuccessMessage = stringResource(R.string.metadata_refresh_success)
    val metadataRefreshUnavailableMessage = stringResource(R.string.metadata_refresh_unavailable)
    val metadataRefreshErrorMessage = stringResource(R.string.metadata_refresh_error)
    val metadataLinkSuccessMessage = stringResource(R.string.metadata_link_success)
    val metadataLinkErrorMessage = stringResource(R.string.metadata_link_error)
    val deletionUndoAction = stringResource(R.string.deletion_undo_action)
    val deletionUndoProgressMessage = stringResource(R.string.deletion_undo_progress_message)
    val deletionRestoredMessage = stringResource(R.string.deletion_restored)
    val deletionRestoreFailedMessage = stringResource(R.string.deletion_restore_failed)
    val navigateBack: () -> Unit = {
        backStack.goBack()
        Unit
    }
    // The add flow steps back through its own pages before leaving, from the bar and system back alike.
    val stepBackFromAddMedia: () -> Unit = {
        if (!addMediaHeaderActions.onStepBack()) {
            viewModel.clearMetadataSearch()
            backStack.goBack()
        }
    }
    val openProfile = {
        backStack.push(AppRoute.Profile)
        viewModel.clearMetadataSearch()
    }
    val openSettings = {
        backStack.push(AppRoute.Settings)
        viewModel.clearMetadataSearch()
    }
    val openTrackedMedia: (TrackedMedia) -> Unit = { trackedMedia ->
        viewModel.clearMetadataSearch()
        if (backStack.lastOrNull() is AppRoute.AddMedia) backStack.goBack()
        backStack.push(AppRoute.MediaDetail(trackedMedia.item.id))
    }
    val openRelatedMedia: (TrackedMedia) -> Unit = { trackedMedia ->
        viewModel.clearMetadataSearch()
        backStack.push(AppRoute.MediaDetail(trackedMedia.item.id))
    }
    val openExternalRecommendation: (MetadataSuggestion) -> Unit = { suggestion ->
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
                backStack.push(AppRoute.AddMedia(suggestion.mediaType.homeSection()))
            }
        }
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
    val autoBackupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                AutoBackupPreferences.saveDirectory(context, uri)
                autoBackupConfiguration = AutoBackupPreferences.read(context)
                AutoBackupScheduler.activate(context, autoBackupConfiguration.frequency)
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) {
                        "Còpia automàtica activada."
                    } else {
                        "No s'ha pogut desar la carpeta de còpies automàtiques."
                    },
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
                val readResult = context.readProviderImportText(uri)
                val csv = readResult.getOrNull()
                if (csv == null) {
                    snackbarHostState.showSnackbar(
                        providerImportReadErrorMessage(
                            error = readResult.exceptionOrNull(),
                            fallback = imdbImportReadErrorMessage,
                            encodingError = providerImportEncodingErrorMessage,
                            tooLarge = providerImportTooLargeMessage,
                        ),
                    )
                    return@launch
                }

                val previewResult = runCatching {
                    PendingImdbCsvImport(viewModel.prepareImdbCsv(csv))
                }
                val pendingCsv = previewResult.getOrNull()
                if (pendingCsv == null) {
                    val error = previewResult.exceptionOrNull()
                    snackbarHostState.showSnackbar(
                        error.providerCsvValidationMessage(
                            context = context,
                            messages = imdbCsvValidationMessages,
                            malformedFallback = providerImportMalformedCsvMessage,
                        ),
                    )
                } else if (pendingCsv.preview.importableRows == 0) {
                    snackbarHostState.showSnackbar(
                        when (pendingCsv.preview.noImportableReason()) {
                            ProviderNoImportableReason.UnsupportedOnly -> imdbImportUnsupportedOnlyMessage
                            ProviderNoImportableReason.DuplicatesOnly -> imdbImportDuplicatesOnlyMessage
                            else -> imdbImportEmptyMessage
                        },
                    )
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
                val readResult = context.readProviderImportText(uri)
                val csv = readResult.getOrNull()
                if (csv == null) {
                    snackbarHostState.showSnackbar(
                        providerImportReadErrorMessage(
                            error = readResult.exceptionOrNull(),
                            fallback = storyGraphImportReadErrorMessage,
                            encodingError = providerImportEncodingErrorMessage,
                            tooLarge = providerImportTooLargeMessage,
                        ),
                    )
                    return@launch
                }

                val previewResult = runCatching {
                    PendingStoryGraphCsvImport(viewModel.prepareStoryGraphCsv(csv))
                }
                val pendingCsv = previewResult.getOrNull()
                if (pendingCsv == null) {
                    val error = previewResult.exceptionOrNull()
                    snackbarHostState.showSnackbar(
                        error.providerCsvValidationMessage(
                            context = context,
                            messages = storyGraphCsvValidationMessages,
                            malformedFallback = providerImportMalformedCsvMessage,
                        ),
                    )
                } else if (pendingCsv.preview.importableRows == 0) {
                    snackbarHostState.showSnackbar(
                        when (pendingCsv.preview.noImportableReason()) {
                            ProviderNoImportableReason.UnsupportedOnly -> storyGraphImportUnsupportedOnlyMessage
                            ProviderNoImportableReason.DuplicatesOnly -> storyGraphImportDuplicatesOnlyMessage
                            else -> storyGraphImportEmptyMessage
                        },
                    )
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
                val readResult = context.readProviderImportText(uri)
                val xml = readResult.getOrNull()
                if (xml == null) {
                    snackbarHostState.showSnackbar(
                        providerImportReadErrorMessage(
                            error = readResult.exceptionOrNull(),
                            fallback = myAnimeListImportReadErrorMessage,
                            encodingError = providerImportEncodingErrorMessage,
                            tooLarge = providerImportTooLargeMessage,
                        ),
                    )
                    return@launch
                }

                val previewResult = runCatching {
                    PendingMyAnimeListXmlImport(viewModel.prepareMyAnimeListXml(xml))
                }
                val pendingXml = previewResult.getOrNull()
                if (pendingXml == null) {
                    snackbarHostState.showSnackbar(
                        if (previewResult.exceptionOrNull() is MalformedProviderXmlException) {
                            providerImportMalformedXmlMessage
                        } else {
                            myAnimeListImportInvalidMessage
                        },
                    )
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
        importImdbCsvLauncher.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/*",
                "*/*"
            )
        )
    }
    backupActions.onImportStoryGraphCsvRequested = {
        importStoryGraphCsvLauncher.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/*",
                "*/*"
            )
        )
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
        metadataLinkImportIssueId = null
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
                preview.requiresMetadataConfirmation() -> {
                    pendingMetadataChange = PendingMetadataChange(
                        preview = preview,
                        operation = MetadataChangeOperation.Refresh,
                    )
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

    suspend fun showDeletionRecovery(deletionToken: Long, message: String) {
        val snackbarResult = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = deletionUndoAction,
            duration = SnackbarDuration.Long,
        )
        if (snackbarResult == SnackbarResult.ActionPerformed) {
            val restoreResult = viewModel.restoreDeletion(deletionToken)
            snackbarHostState.showSnackbar(
                if (restoreResult.getOrDefault(false)) {
                    deletionRestoredMessage
                } else {
                    deletionRestoreFailedMessage
                },
            )
        } else {
            viewModel.expireDeletion(deletionToken)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.MediaItemCreated -> {
                    pendingCreatedMediaId = event.mediaItemId
                }

                HomeUiEvent.ChangeRefused -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.change_refused))
                }

                is HomeUiEvent.MediaItemDeletionAvailable -> {
                    showDeletionRecovery(
                        deletionToken = event.deletionToken,
                        message = context.getString(
                            R.string.deletion_undo_item_message,
                            event.title
                        ),
                    )
                }

                is HomeUiEvent.PastSessionDeletionAvailable -> {
                    showDeletionRecovery(
                        deletionToken = event.deletionToken,
                        message = context.getString(
                            R.string.deletion_undo_session_message,
                            event.visitNumber,
                        ),
                    )
                }

                is HomeUiEvent.ProgressUpdateDeletionAvailable -> {
                    showDeletionRecovery(
                        deletionToken = event.deletionToken,
                        message = deletionUndoProgressMessage,
                    )
                }

                is HomeUiEvent.StatusEventDeletionAvailable -> {
                    showDeletionRecovery(
                        deletionToken = event.deletionToken,
                        message = context.getString(
                            statusEventDeletionMessageResId(event.previousStatus, event.status),
                        ),
                    )
                }

                is HomeUiEvent.SessionCompletedReversible -> {
                    // A goal gets its moment once; a repeat finish shows it as plain progress.
                    val reaction = event.reaction.copy(
                        steps = event.reaction.steps.withoutCelebrated { CelebratedObjectives.contains(context, it) },
                    )
                    val step = reaction.objective
                    val undo = CompletableDeferred<Boolean>()
                    step?.takeIf { it.reached }?.let { CelebratedObjectives.add(context, it.progress.objective) }
                    completionCelebration = CompletionCelebrationRequest(
                        reaction = reaction,
                        canUndo = event.recoveryToken != null,
                        undo = undo,
                    )
                    val undone = try {
                        undo.await()
                    } finally {
                        completionCelebration = null
                    }
                    val token = event.recoveryToken
                    if (undone && token != null) {
                        step?.takeIf { it.reached }?.let { CelebratedObjectives.remove(context, it.progress.objective) }
                        if (!viewModel.restoreDeletion(token).getOrDefault(false)) {
                            snackbarHostState.showSnackbar(deletionRestoreFailedMessage)
                        }
                    } else {
                        token?.let(viewModel::expireDeletion)
                    }
                }

                is HomeUiEvent.ObjectiveReached -> {
                    val step = event.steps
                        .withoutCelebrated { CelebratedObjectives.contains(context, it) }
                        .firstOrNull { it.reached }
                    if (step == null) {
                        event.recoveryToken?.let(viewModel::expireDeletion)
                    } else {
                        val objective = step.progress.objective
                        CelebratedObjectives.add(context, objective)
                        val undo = CompletableDeferred<Boolean>()
                        objectiveCelebration = ObjectiveCelebrationRequest(step, event.recoveryToken != null, undo)
                        val undone = try {
                            undo.await()
                        } finally {
                            objectiveCelebration = null
                        }
                        val token = event.recoveryToken
                        if (undone && token != null) {
                            CelebratedObjectives.remove(context, objective)
                            if (!viewModel.restoreDeletion(token).getOrDefault(false)) {
                                snackbarHostState.showSnackbar(deletionRestoreFailedMessage)
                            }
                        } else {
                            token?.let(viewModel::expireDeletion)
                        }
                    }
                }

                is HomeUiEvent.SessionStatusChangedReversible -> {
                    val result = snackbarHostState.showSnackbar(
                        StatusReactionVisuals(
                            reaction = event.reaction,
                            actionLabel = event.recoveryToken?.let { deletionUndoAction },
                        ),
                    )
                    val token = event.recoveryToken
                    if (result == SnackbarResult.ActionPerformed && token != null) {
                        if (!viewModel.restoreDeletion(token).getOrDefault(false)) {
                            snackbarHostState.showSnackbar(deletionRestoreFailedMessage)
                        }
                    } else {
                        token?.let(viewModel::expireDeletion)
                    }
                }

                HomeUiEvent.MetadataRefreshSucceeded,
                HomeUiEvent.MetadataRefreshUnavailable,
                HomeUiEvent.MetadataRefreshFailed,
                    -> {
                    val message = when (event) {
                        HomeUiEvent.MetadataRefreshSucceeded -> metadataRefreshSuccessMessage
                        HomeUiEvent.MetadataRefreshUnavailable -> metadataRefreshUnavailableMessage
                        HomeUiEvent.MetadataRefreshFailed -> metadataRefreshErrorMessage
                        else -> return@collect
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    val pendingImportCompletion = importEnrichmentState.pendingCompletion
    LaunchedEffect(pendingImportCompletion?.batchId) {
        val initial = pendingImportCompletion ?: return@LaunchedEffect
        // Item and coverage queries are invalidated just before the batch is closed. A short
        // debounce ensures the message uses their final counts rather than an intermediate frame.
        delay(250)
        val completion = viewModel.currentImportCompletion(initial.batchId) ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = completion.userFacingMessage(),
            actionLabel = if (completion.needsAttention) "Revisa" else null,
            duration = if (completion.needsAttention) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            showImportHub = true
        }
        viewModel.acknowledgeImportCompletion(completion.batchId)
    }

    LaunchedEffect(uiState.allTrackedItems, pendingCreatedMediaId) {
        val mediaItemId = pendingCreatedMediaId ?: return@LaunchedEffect
        uiState.allTrackedItems.firstOrNull { it.item.id == mediaItemId } ?: return@LaunchedEffect

        val addRoute = backStack.lastOrNull() as? AppRoute.AddMedia
        if (addRoute != null) backStack.goBack()
        if (addRoute?.collectionId == null) {
            backStack.push(AppRoute.MediaDetail(mediaItemId))
        }
        pendingCreatedMediaId = null
    }

    BackHandler(
        enabled = showImportHub ||
                showAnimeTitleBulkConfirmation ||
                pendingPossibleDuplicate != null ||
                metadataLinkTarget != null ||
                pendingMetadataChange != null ||
                pendingMyAnimeListAccountImport != null ||
                pendingMyAnimeListXmlImport != null ||
                pendingStoryGraphCsvImport != null ||
                pendingImdbCsvImport != null ||
                pendingImportConfirmation != null ||
                pendingImport != null ||
                showRestoreList ||
                detailActions.isManagingExternalRatings,
    ) {
        when {
            showImportHub -> showImportHub = false
            showAnimeTitleBulkConfirmation -> showAnimeTitleBulkConfirmation = false
            pendingPossibleDuplicate != null -> pendingPossibleDuplicate = null
            metadataLinkTarget != null -> {
                metadataLinkTarget = null
                metadataLinkImportIssueId = null
            }
            pendingMetadataChange != null -> pendingMetadataChange = null
            pendingMyAnimeListAccountImport != null && !isProviderImporting -> {
                pendingMyAnimeListAccountImport = null
                coroutineScope.launch { viewModel.discardMyAnimeListAccountImport() }
            }
            pendingMyAnimeListXmlImport != null && !isProviderImporting -> pendingMyAnimeListXmlImport = null
            pendingStoryGraphCsvImport != null && !isProviderImporting -> pendingStoryGraphCsvImport = null
            pendingImdbCsvImport != null && !isProviderImporting -> pendingImdbCsvImport = null
            pendingImportConfirmation != null -> pendingImportConfirmation = null
            pendingImport != null -> pendingImport = null
            showRestoreList -> showRestoreList = false
            detailActions.isManagingExternalRatings -> detailActions.onCloseExternalRatings()
        }
    }

    // Pushes the library back while the completion page is up, so its text cannot compete with the
    // page's own. A no-op below Android 12, where the page's own background carries legibility.
    val celebrationBlur by animateDpAsState(
        targetValue = if (completionCelebration != null || objectiveCelebration != null) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "celebrationBlur",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(celebrationBlur),
            topBar = {
                OmnilogTopBar(
                    accent = when (currentRoute) {
                        AppRoute.Home,
                        AppRoute.Stats,
                        AppRoute.Timeline,
                        AppRoute.Profile,
                        AppRoute.Settings,
                        is AppRoute.StatusList,
                            -> OmnilogTheme.accents.Dashboard

                        else -> currentSection.themedAccent()
                    },
                    title = when (val route = currentRoute) {
                        AppRoute.Stats -> stringResource(R.string.stats_title)
                        AppRoute.Timeline -> stringResource(R.string.timeline_title)
                        // The profile names itself in its header, like the other editorial pages.
                        AppRoute.Profile -> ""
                        AppRoute.Settings -> "Configuració"
                        is AppRoute.Section -> stringResource(route.section.titleResId)
                        // The page's story headline stands in for the bare status name, so the
                        // screen does not open with two titles saying the same thing.
                        is AppRoute.StatusList -> com.nilpo.contenttracker.ui.home.statusStoryHeadline(
                            status = route.status,
                            count = uiState.allTrackedItems.count { it.currentSession?.status == route.status },
                        )
                        is AppRoute.AuthorDetail -> ""
                        // The collection names itself in its header, so the bar stays empty there;
                        // reordering puts the mode's name here instead.
                        is AppRoute.CollectionDetail -> collectionTopBarTitle
                        is AppRoute.MediaDetail -> ""
                        is AppRoute.AddMedia -> addMediaHeaderActions.title
                        else -> null
                    },
                    showBackNavigation = currentRoute is AppRoute.MediaDetail ||
                            currentRoute == AppRoute.Stats ||
                            currentRoute == AppRoute.Timeline ||
                            currentRoute == AppRoute.Profile ||
                            currentRoute == AppRoute.Settings ||
                            currentRoute is AppRoute.AuthorDetail ||
                            currentRoute is AppRoute.CollectionDetail ||
                            currentRoute is AppRoute.StatusList ||
                            currentRoute is AppRoute.AddMedia,
                    showDetailActions = currentRoute is AppRoute.MediaDetail &&
                            !detailActions.isManagingExternalRatings,
                    showProfileAction = currentRoute !is AppRoute.MediaDetail &&
                            currentRoute !is AppRoute.AddMedia &&
                            currentRoute !is AppRoute.Section &&
                            currentRoute !is AppRoute.StatusList &&
                            currentRoute != AppRoute.Stats &&
                            currentRoute != AppRoute.Timeline &&
                            currentRoute != AppRoute.Profile &&
                            currentRoute !is AppRoute.CollectionDetail &&
                            currentRoute !is AppRoute.AuthorDetail,
                    showProfileControls = currentRoute == AppRoute.Profile,
                    // Settings and Profile are a pack: shown together everywhere except the routes
                    // where neither belongs, and each hides on its own screen.
                    showHomeSettingsAction = currentRoute !is AppRoute.MediaDetail &&
                            currentRoute !is AppRoute.AddMedia &&
                            currentRoute !is AppRoute.Section &&
                            currentRoute !is AppRoute.StatusList &&
                            currentRoute != AppRoute.Stats &&
                            currentRoute != AppRoute.Timeline &&
                            currentRoute != AppRoute.Settings &&
                            currentRoute !is AppRoute.CollectionDetail &&
                            currentRoute !is AppRoute.AuthorDetail,
                    showHomeSearchAction = currentRoute == AppRoute.Home,
                    showSectionActions = currentRoute is AppRoute.Section,
                    showTimelineSettingsAction = currentRoute == AppRoute.Timeline,
                    profileImage = profileImage,
                    // Only the detail page draws artwork under the bar, and only while it is
                    // actually showing that page — the external-ratings page it can swap to has an
                    // ordinary background and needs the bar's own surface back.
                    overCover = (currentRoute is AppRoute.MediaDetail &&
                            !detailActions.isManagingExternalRatings) ||
                            currentRoute is AppRoute.CollectionDetail ||
                            currentRoute is AppRoute.AuthorDetail ||
                            currentRoute == AppRoute.Profile,
                    detailActions = detailActions,
                    onProfileRequested = openProfile,
                    onProfileEditRequested = { profileHeaderActions.onEditRequested() },
                    onSettingsRequested = openSettings,
                    onHomeSearchRequested = { homeSearchOpen = true },
                    onSectionSearchRequested = { sectionSearchOpen = true },
                    // Non-null while searching: the field then takes over the whole bar.
                    searchQuery = when {
                        currentRoute == AppRoute.Home && homeSearchOpen -> homeSearchQuery
                        currentRoute is AppRoute.Section && sectionSearchActive -> uiState.searchQuery
                        else -> null
                    },
                    onSearchQueryChange = { query ->
                        if (currentRoute == AppRoute.Home) {
                            homeSearchQuery = query
                        } else {
                            viewModel.updateSearchQuery(query)
                            viewModel.updateMetadataSearchQuery(query)
                        }
                    },
                    onSearchSubmitted = {
                        if (currentRoute is AppRoute.Section) {
                            viewModel.searchMetadataSuggestions(forceShortQuery = true)
                        }
                    },
                    onSearchFocusChange = { homeSearchFocused = it },
                    onSearchClose = {
                        if (currentRoute == AppRoute.Home) closeHomeSearch() else closeSectionSearch()
                    },
                    onSectionAddRequested = {
                        (currentRoute as? AppRoute.Section)?.let { route ->
                            viewModel.clearMetadataSearch()
                            backStack.push(AppRoute.AddMedia(route.section))
                        }
                    },
                    onTimelineSettingsRequested = {
                        timelineHeaderActions.onSettingsRequested()
                    },
                    onBack = if (detailActions.isManagingExternalRatings) {
                        detailActions.onCloseExternalRatings
                    } else if (currentRoute is AppRoute.AddMedia) {
                        stepBackFromAddMedia
                    } else if (currentRoute is AppRoute.CollectionDetail) {
                        collectionBackRequest ?: navigateBack
                    } else {
                        navigateBack
                    },
                )
            },
            snackbarHost = {},
            bottomBar = {
                Column {
                    val visibleImport = importEnrichmentState.activeBatch
                        ?: importEnrichmentState.recentBatches.firstOrNull { progress ->
                            progress.needsReviewCount > 0 || progress.issueCount > 0 ||
                                progress.coverageGapCount > 0
                        }
                    visibleImport?.let { progress ->
                        ImportProgressBanner(
                            progress = progress,
                            concurrentImportCount = importEnrichmentState.activeBatches.size,
                            onOpen = { showImportHub = true },
                            onToggle = { viewModel.toggleImportEnrichment(progress.batchId) },
                        )
                    }
                    OmnilogBottomBar(
                        selectedRootRoute = selectedRootRoute,
                        onHomeClick = {
                            backStack.selectHome()
                            viewModel.clearMetadataSearch()
                        },
                        onSectionClick = { section ->
                            backStack.selectSection(section)
                            viewModel.selectSection(section)
                            viewModel.clearMetadataSearch()
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                // Back closes a list's search before leaving the list. It lives here because this
                // handler outranks a BackHandler registered inside the entry.
                onBack = {
                    if (currentRoute is AppRoute.Section && sectionSearchActive) {
                        closeSectionSearch()
                    } else if (currentRoute is AppRoute.AddMedia) {
                        stepBackFromAddMedia()
                    } else {
                        backStack.goBack()
                    }
                },
                entryProvider = { key ->
                    val route = key as AppRoute
                    NavEntry(key) {
                        if (route is AppRoute.AddMedia) {
                            val targetCollection = route.collectionId?.let { collectionId ->
                                uiState.allTrackedItems
                                    .mapNotNull { it.collection }
                                    .firstOrNull { it.id == collectionId }
                            }
                            val initialAddType = route.collectionId?.let { collectionId ->
                                uiState.allTrackedItems.firstOrNull { it.collection?.id == collectionId }?.item?.type
                            }
                                ?: route.section.defaultType
                            AddMediaScreen(
                                initialMediaType = initialAddType,
                                headerActions = addMediaHeaderActions,
                                availableMediaTypes = route.section.types.toList(),
                                library = uiState.allTrackedItems,
                                initialCollection = targetCollection,
                                initialCollectionName = targetCollection?.name,
                                initialCollectionOrder = route.collectionOrder?.let {
                                    formatCollectionOrder(
                                        it
                                    )
                                },
                                onSave = { request ->
                                    viewModel.addTrackedMedia(request)
                                    viewModel.clearMetadataSearch()
                                },
                                metadataUiState = metadataUiState,
                                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                                onMetadataSearch = { viewModel.searchMetadataSuggestions() },
                                onMetadataSearchSubmitted = {
                                    viewModel.searchMetadataSuggestions(
                                        forceShortQuery = true
                                    )
                                },
                                onMetadataSuggestionSelected = { suggestion ->
                                    when (val duplicate =
                                        uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                                        is DuplicateMatch.Exact -> openTrackedMedia(duplicate.trackedMedia)
                                        is DuplicateMatch.Possible -> {
                                            pendingPossibleDuplicate = PendingPossibleDuplicate(
                                                suggestion = suggestion,
                                                trackedMedia = duplicate.trackedMedia,
                                            )
                                        }

                                        DuplicateMatch.None -> viewModel.selectMetadataSuggestion(
                                            suggestion
                                        )
                                    }
                                },
                                onMetadataDetailsRetry = { viewModel.retryMetadataSuggestionDetails() },
                                duplicateStateForSuggestion = { suggestion ->
                                    when (uiState.allTrackedItems.findDuplicateFor(suggestion)) {
                                        is DuplicateMatch.Exact -> MetadataDuplicateState.Exact
                                        is DuplicateMatch.Possible -> MetadataDuplicateState.Possible
                                        DuplicateMatch.None -> MetadataDuplicateState.None
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route == AppRoute.Home) {
                            HomeLandingScreen(
                                uiState = uiState,
                                timelineEntries = timelineEntries,
                                onMediaClick = openTrackedMedia,
                                onSectionSearch = { section, query ->
                                    viewModel.selectSectionWithSearch(section, query)
                                    backStack.selectSection(section)
                                },
                                onStatsClick = { backStack.push(AppRoute.Stats) },
                                onTimelineClick = { backStack.push(AppRoute.Timeline) },
                                onObjectivesClick = { objectiveId ->
                                    profileFocusObjectiveId = objectiveId
                                    openProfile()
                                },
                                onStatusClick = { status -> backStack.push(AppRoute.StatusList(status)) },
                                onAddToSection = { section ->
                                    viewModel.selectSection(section)
                                    viewModel.clearMetadataSearch()
                                    backStack.push(AppRoute.AddMedia(section))
                                },
                                onImportBackup = { backupActions.onImportBackupRequested() },
                                onQuickCommitProgress = viewModel::quickCommitProgress,
                                onQuickComplete = viewModel::quickComplete,
                                searchOpen = homeSearchOpen,
                                searchQuery = homeSearchQuery,
                                searchFocused = homeSearchFocused,
                                onSearchClose = closeHomeSearch,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route is AppRoute.StatusList) {
                            StatusListScreen(
                                status = route.status,
                                items = uiState.allTrackedItems
                                    .filter { it.currentSession?.status == route.status }
                                    .sortedByDescending { it.currentSession?.updatedAtEpochMillis ?: 0L },
                                onMediaClick = openTrackedMedia,
                                onQuickCommitProgress = viewModel::quickCommitProgress,
                                onQuickComplete = viewModel::quickComplete,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route == AppRoute.Profile) {
                            ProfileScreen(
                                items = uiState.allTrackedItems,
                                objectives = uiState.objectives,
                                onOpenMedia = openTrackedMedia,
                                onSaveObjective = viewModel::addObjective,
                                onDeleteObjective = viewModel::deleteObjective,
                                headerActions = profileHeaderActions,
                                focusObjectiveId = profileFocusObjectiveId,
                                onFocusObjectiveConsumed = { profileFocusObjectiveId = null },
                                topInset = innerPadding.calculateTopPadding(),
                                onTopBarOpacityChange = { detailActions.barOpacity = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding()),
                            )
                        } else if (route == AppRoute.Settings) {
                            // The worker writes its success stamp from the background, where this
                            // state cannot see it. Re-read on entry so the "last backup" line is
                            // not stale for the whole life of the process.
                            LaunchedEffect(Unit) {
                                autoBackupConfiguration = AutoBackupPreferences.read(context)
                            }
                            SettingsScreen(
                                askForGoodreadsRating = askForGoodreadsRating,
                                onAskForGoodreadsRatingChange = { enabled ->
                                    askForGoodreadsRating = enabled
                                    preferences.edit()
                                        .putBoolean("ask_for_goodreads_rating", enabled).apply()
                                },
                                onExportBackup = { backupActions.onExportBackupRequested() },
                                onRestoreBackup = { backupActions.onRestoreBackupRequested() },
                                onImportMyAnimeListAccount = {
                                    coroutineScope.launch {
                                        val result = viewModel.previewMyAnimeListAccount()
                                        val accountImport = result.getOrNull()
                                        when {
                                            accountImport == null -> snackbarHostState.showSnackbar(
                                                myAnimeListAccountImportErrorMessage,
                                            )
                                            accountImport.preview.importableRows == 0 ->
                                                snackbarHostState.showSnackbar(myAnimeListAccountImportEmptyMessage)
                                            else -> pendingMyAnimeListAccountImport = accountImport
                                        }
                                    }
                                },
                                onImportMyAnimeListXml = { backupActions.onImportMyAnimeListXmlRequested() },
                                onImportImdbCsv = { backupActions.onImportImdbCsvRequested() },
                                onImportStoryGraphCsv = { backupActions.onImportStoryGraphCsvRequested() },
                                isAutoBackupEnabled = autoBackupConfiguration.directoryUri != null,
                                autoBackupFrequency = autoBackupConfiguration.frequency,
                                lastAutoBackupAtEpochMillis = autoBackupConfiguration.lastSuccessAtEpochMillis,
                                autoBackupFolderLabel = autoBackupConfiguration.directoryUri
                                    ?.backupFolderLabel(context),
                                maxKeptBackups = autoBackupConfiguration.maxKeptBackups,
                                onAutoBackupFolderRequested = { autoBackupFolderLauncher.launch(null) },
                                onAutoBackupFrequencyChange = { frequency ->
                                    AutoBackupPreferences.saveFrequency(context, frequency)
                                    autoBackupConfiguration = AutoBackupPreferences.read(context)
                                    if (autoBackupConfiguration.directoryUri != null) {
                                        AutoBackupScheduler.schedule(context, frequency)
                                    }
                                },
                                onMaxKeptBackupsChange = { maxKeptBackups ->
                                    AutoBackupPreferences.saveMaxKeptBackups(context, maxKeptBackups)
                                    autoBackupConfiguration = AutoBackupPreferences.read(context)
                                },
                                onAutoBackupToggle = { enabled ->
                                    if (enabled) {
                                        autoBackupFolderLauncher.launch(null)
                                    } else {
                                        AutoBackupScheduler.cancel(context)
                                        AutoBackupPreferences.clear(context)
                                        autoBackupConfiguration = AutoBackupPreferences.read(context)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Còpia automàtica desactivada.")
                                        }
                                    }
                                },
                                malSyncState = malSyncState,
                                importEnrichmentState = importEnrichmentState,
                                metadataRefreshState = metadataRefreshState,
                                onBulkMetadataRefresh = { showMetadataBulkConfirmation = true },
                                onCancelBulkMetadataRefresh = viewModel::cancelBulkMetadataRefresh,
                                animeTitlePreference = animeTitlePreference,
                                onAnimeTitlePreferenceChange = { preference ->
                                    animeTitlePreference = preference
                                    AnimeTitlePreferences.write(context, preference)
                                },
                                onBulkRefreshAnimeTitles = {
                                    showAnimeTitleBulkConfirmation = true
                                },
                                onOpenImportActivity = { showImportHub = true },
                                onConnectMyAnimeList = {
                                    viewModel.beginMalAuthorization()?.let { authorizationUrl ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl)))
                                    }
                                },
                                onSyncMyAnimeList = {
                                    showMalInitialSyncConfirmation = true
                                },
                                onRetryMyAnimeList = viewModel::retryMyAnimeListChanges,
                                onCancelMyAnimeList = viewModel::cancelMyAnimeListChanges,
                                onDisconnectMyAnimeList = viewModel::disconnectMyAnimeList,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route == AppRoute.Timeline) {
                            TimelineScreen(
                                entries = timelineEntries,
                                isLoading = uiState.isLoading,
                                headerActions = timelineHeaderActions,
                                onBrowseLibrary = { backStack.selectHome() },
                                sessionActivitySheet = { sessionId, onDismiss ->
                                    val media = uiState.allTrackedItems
                                        .firstOrNull { tracked -> tracked.sessions.any { it.id == sessionId } }
                                    val session = media?.sessions?.firstOrNull { it.id == sessionId }
                                    if (media == null || session == null) {
                                        // The session was deleted while the sheet was open.
                                        LaunchedEffect(sessionId) { onDismiss() }
                                    } else {
                                        ActivitySheet(
                                            session = session,
                                            mediaType = media.item.type,
                                            progressTotal = media.item.progressTotal,
                                            accent = media.item.type.homeSection().themedAccent(),
                                            onDeleteProgressUpdate = viewModel::deleteProgressUpdate,
                                            onUpdateProgressUpdate = viewModel::updateProgressUpdate,
                                            onDeleteStatusEvent = viewModel::deleteSessionStatusEvent,
                                            onUpdateStatusEventDate = viewModel::updateSessionStatusEventDate,
                                            onDismiss = onDismiss,
                                            itemTitle = displayMediaTitle(media.item.title),
                                            onOpenItem = {
                                                onDismiss()
                                                openTrackedMedia(media)
                                            },
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route == AppRoute.Stats) {
                            StatsScreen(
                                items = uiState.allTrackedItems,
                                onCreatorClick = { creator, mediaType ->
                                    backStack.push(
                                        AppRoute.AuthorDetail(
                                            creator,
                                            mediaType.homeSection()
                                        )
                                    )
                                },
                                onCollectionClick = { collectionId, mediaType ->
                                    backStack.push(
                                        AppRoute.CollectionDetail(
                                            collectionId,
                                            mediaType.homeSection()
                                        ),
                                    )
                                },
                                onMediaClick = openTrackedMedia,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route is AppRoute.AuthorDetail) {
                            AuthorDetailScreen(
                                authorName = route.author,
                                authorImageUrl = uiState.allTrackedItems.creatorImageUrl(route.author),
                                imageIsLogo = route.section.creatorImageIsLogo,
                                creatorLabelResId = route.section.creatorDetailLabelResId,
                                items = uiState.allTrackedItems.filter { trackedMedia ->
                                    trackedMedia.item.type in route.section.types && (
                                            route.contributorRole
                                                ?.let { roleName ->
                                                    runCatching { MediaCreditRole.valueOf(roleName) }
                                                        .getOrNull()
                                                        ?.let { role ->
                                                            trackedMedia.hasContributor(role, route.author)
                                                        }
                                                }
                                                ?: trackedMedia.hasCreator(route.author)
                                            )
                                },
                                accent = route.section.themedAccent(),
                                onMediaClick = openTrackedMedia,
                                topInset = innerPadding.calculateTopPadding(),
                                onTopBarOpacityChange = { detailActions.barOpacity = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding()),
                            )
                        } else if (route is AppRoute.CollectionDetail) {
                            val routeCollection = uiState.allTrackedItems
                                .mapNotNull { it.collection }
                                .firstOrNull { it.id == route.collectionId }
                            val routeCollectionItems = uiState.allTrackedItems
                                .filter { it.collection?.id == route.collectionId }
                                .sortedWith(collectionItemComparator())
                            if (routeCollection != null) {
                                CollectionDetailScreen(
                                    collection = routeCollection,
                                    items = routeCollectionItems,
                                    accent = route.section.themedAccent(),
                                    onBack = navigateBack,
                                    onRegisterBackRequest = { handler ->
                                        collectionBackRequest = handler
                                    },
                                    onTopBarTitleChange = { collectionTopBarTitle = it },
                                    onMediaClick = openTrackedMedia,
                                    onAddToCollection = { collection, nextOrder ->
                                        viewModel.clearMetadataSearch()
                                        backStack.push(
                                            AppRoute.AddMedia(
                                                section = route.section,
                                                collectionId = collection.id,
                                                collectionOrder = nextOrder,
                                            ),
                                        )
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
                                            isOwned = trackedMedia.item.isOwned,
                                        )
                                    },
                                    onCollectionActionMessage = { message ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = innerPadding.calculateBottomPadding()),
                                    topInset = innerPadding.calculateTopPadding(),
                                    onTopBarOpacityChange = { detailActions.barOpacity = it },
                                )
                            }
                        } else if (route is AppRoute.Section) {
                            LaunchedEffect(route.section) {
                                if (uiState.selectedSection != route.section) {
                                    viewModel.selectSection(route.section)
                                }
                            }
                            HomeScreen(
                                uiState = uiState,
                                metadataUiState = metadataUiState,
                                onMediaClick = openTrackedMedia,
                                onCollectionClick = {
                                    backStack.push(AppRoute.CollectionDetail(it.id, route.section))
                                },
                                onAuthorClick = { author ->
                                    backStack.push(AppRoute.AuthorDetail(author, route.section))
                                },
                                onManualAddClick = {
                                    viewModel.clearMetadataSearch()
                                    backStack.push(AppRoute.AddMedia(route.section))
                                },
                                onImportRequested = when (route.section) {
                                    MediaSection.Anime -> {
                                        { backupActions.onImportMyAnimeListXmlRequested() }
                                    }

                                    MediaSection.Books -> {
                                        { backupActions.onImportStoryGraphCsvRequested() }
                                    }

                                    MediaSection.Movies -> {
                                        { backupActions.onImportImdbCsvRequested() }
                                    }

                                    MediaSection.Games -> null
                                },
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onMetadataQueryChange = viewModel::updateMetadataSearchQuery,
                                onMetadataSearch = { viewModel.searchMetadataSuggestions() },
                                onMetadataSearchSubmitted = {
                                    viewModel.searchMetadataSuggestions(
                                        forceShortQuery = true
                                    )
                                },
                                onApiSuggestionSelected = { suggestion ->
                                    when (val duplicate =
                                        uiState.allTrackedItems.findDuplicateFor(suggestion)) {
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
                                            backStack.push(AppRoute.AddMedia(route.section))
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
                                onBrowseModeChange = viewModel::updateBrowseMode,
                                onDisplayModeChange = viewModel::updateDisplayMode,
                                onSortModeChange = viewModel::updateSortMode,
                                onSortDirectionChange = viewModel::updateSortDirection,
                                onAdvancedFiltersChange = viewModel::updateAdvancedFilters,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            )
                        } else if (route is AppRoute.MediaDetail) {
                            val routeMedia =
                                uiState.allTrackedItems.firstOrNull { it.item.id == route.mediaItemId }
                            if (routeMedia == null) {
                                LaunchedEffect(route) { backStack.goBack() }
                            } else {
                                val actions = remember(routeMedia.item.id) {
                                    DetailHeaderActions()
                                }
                                actions.isRefreshingMetadata =
                                    uiState.refreshingMetadataItemId == routeMedia.item.id
                                detailActions = actions
                                DetailScreen(
                                    trackedMedia = routeMedia,
                                    allTrackedMedia = uiState.allTrackedItems,
                                    contributors = contributors,
                                    accent = routeMedia.item.type.homeSection().themedAccent(),
                                    headerActions = actions,
                                    onBack = navigateBack,
                                    onStartNewSession = viewModel::startNewSession,
                                    onUpdateSessionDetails = viewModel::updateSessionDetails,
                                    // The same two entry points the Home tiles use. Promotion,
                                    // start-date stamping and the completion undo all live in the
                                    // view model, so the detail page routes through them rather than
                                    // assembling its own session update and drifting from Home.
                                    onQuickCommitProgress = { progress ->
                                        viewModel.quickCommitProgress(routeMedia, progress)
                                    },
                                    onQuickComplete = { completion ->
                                        viewModel.quickComplete(routeMedia, completion)
                                    },
                                    onDeletePastSession = viewModel::deletePastSession,
                                    onDeleteCurrentSession = viewModel::deleteCurrentSession,
                                    onDeleteProgressUpdate = viewModel::deleteProgressUpdate,
                                    onDeleteStatusEvent = viewModel::deleteSessionStatusEvent,
                                    onUpdateStatusEventDate = viewModel::updateSessionStatusEventDate,
                                    onUpdateProgressUpdate = viewModel::updateProgressUpdate,
                                    onAddExternalRating = viewModel::addExternalRating,
                                    onUpdateExternalRating = viewModel::updateExternalRating,
                                    onSetPrimaryExternalRating = viewModel::setPrimaryExternalRating,
                                    onDeleteExternalRating = viewModel::deleteExternalRating,
                                    onUpdateMediaItemDetails = viewModel::updateMediaItemDetails,
                                    onUpdateMediaItemMetadata = viewModel::updateMediaItemMetadata,
                                    onRefreshMediaItemMetadata = { startMetadataRefresh(routeMedia) },
                                    onLinkMediaMetadata = { startMetadataLink(routeMedia) },
                                    onDeleteMediaItem = { mediaItemId ->
                                        viewModel.deleteMediaItem(mediaItemId)
                                        backStack.goBack()
                                    },
                                    onCollectionClick = {
                                        val collectionId =
                                            routeMedia.collection?.id ?: return@DetailScreen
                                        backStack.push(
                                            AppRoute.CollectionDetail(
                                                collectionId = collectionId,
                                                section = routeMedia.item.type.homeSection(),
                                            ),
                                        )
                                    },
                                    onAuthorClick = { author, role ->
                                        backStack.push(
                                            AppRoute.AuthorDetail(
                                                author = author,
                                                section = routeMedia.item.type.homeSection(),
                                                contributorRole = role.name,
                                            ),
                                        )
                                    },
                                    onRelatedMediaClick = openRelatedMedia,
                                    externalRecommendations = recommendationUiState
                                        .takeIf { it.mediaItemId == routeMedia.item.id }
                                        ?.recommendations
                                        .orEmpty(),
                                    isExternalRecommendationsLoading = recommendationUiState.mediaItemId == routeMedia.item.id &&
                                            recommendationUiState.isLoading,
                                    hasExternalRecommendationsError = recommendationUiState.mediaItemId == routeMedia.item.id &&
                                            recommendationUiState.hasError,
                                    onRefreshExternalRecommendations = {
                                        viewModel.loadRecommendations(
                                            current = routeMedia,
                                            library = uiState.allTrackedItems,
                                            forceRefresh = true,
                                        )
                                    },
                                    onExternalRecommendationClick = openExternalRecommendation,
                                    askForGoodreadsRating = askForGoodreadsRating,
                                    // Not `.padding(innerPadding)`: the backdrop has to reach up
                                    // behind the transparent app bar, so the screen takes the
                                    // insets as content padding and spends them itself.
                                    contentPadding = innerPadding,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, top = 82.dp, end = 16.dp),
        ) { snackbarData ->
            key(snackbarData) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(),
                    backgroundContent = {},
                    onDismiss = { snackbarData.dismiss() },
                ) {
                    val visuals = snackbarData.visuals
                    if (visuals is StatusReactionVisuals) {
                        StatusReactionCard(reaction = visuals.reaction, snackbarData = snackbarData)
                    } else {
                        OmnilogSnackbar(snackbarData = snackbarData)
                    }
                }
            }
        }

        // Last in the root box so the page lies over every screen, the app bar, and the snackbars.
        completionCelebration?.let { request ->
            CompletionCelebration(
                reaction = request.reaction,
                onContinue = { request.undo.complete(false) },
                onUndo = if (request.canUndo) ({ request.undo.complete(true) }) else null,
            )
        }
        objectiveCelebration?.let { request ->
            ObjectiveCelebration(
                step = request.step,
                onContinue = { request.undo.complete(false) },
                onUndo = if (request.canUndo) ({ request.undo.complete(true) }) else null,
            )
        }
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
                Column {
                    TextButton(
                        onClick = {
                            showRestoreList = false
                            backupActions.onImportBackupRequested()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 0.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Text(
                            text = "Tria un fitxer…",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (safetyBackups.isEmpty()) {
                        Text(text = stringResource(R.string.restore_previous_backup_empty))
                    } else {
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
                                backStack.selectHome()
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    message = importSuccessMessage,
                                    actionLabel = if (malSyncState.isConnected) {
                                        backupImportSyncMalAction
                                    } else {
                                        null
                                    },
                                    duration = SnackbarDuration.Long,
                                )
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    showMalInitialSyncConfirmation = true
                                }
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
            onDismissRequest = { if (!isProviderImporting) pendingImdbCsvImport = null },
            title = stringResource(
                R.string.imdb_import_title,
                stringResource(R.string.nav_movies_tv),
            ),
            text = {
                ProviderImportPreviewSummary(
                    preview = imdbImport.preview,
                    summary = stringResource(
                        R.string.imdb_import_message_with_summary,
                        imdbImport.preview.importableRows,
                        imdbImport.preview.totalRows,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = {
                        if (!isProviderImporting) {
                            isProviderImporting = true
                            coroutineScope.launch {
                                val result = runCatching {
                                    viewModel.importPreparedImdbCsv(imdbImport.prepared)
                                }
                                pendingImdbCsvImport = null
                                isProviderImporting = false
                                result.fold(
                                    onSuccess = { importResult ->
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.imdb_import_success_with_summary,
                                                importResult.importedRows,
                                                importResult.skippedDuplicateRows,
                                                importResult.unsupportedRows,
                                                importResult.invalidRows,
                                            ),
                                        )
                                    },
                                    onFailure = {
                                        snackbarHostState.showSnackbar(providerImportFailedMessage)
                                    },
                                )
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (isProviderImporting) R.string.provider_import_in_progress
                            else R.string.imdb_import_confirm,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = { if (!isProviderImporting) pendingImdbCsvImport = null },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingStoryGraphCsvImport?.let { storyGraphImport ->
        OmnilogAlertDialog(
            onDismissRequest = { if (!isProviderImporting) pendingStoryGraphCsvImport = null },
            title = stringResource(R.string.storygraph_import_title),
            text = {
                ProviderImportPreviewSummary(
                    preview = storyGraphImport.preview,
                    summary = stringResource(
                        R.string.storygraph_import_message_with_summary,
                        storyGraphImport.preview.importableRows,
                        storyGraphImport.preview.totalRows,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = {
                        if (!isProviderImporting) {
                            isProviderImporting = true
                            coroutineScope.launch {
                                val result = runCatching {
                                    viewModel.importPreparedStoryGraphCsv(storyGraphImport.prepared)
                                }
                                pendingStoryGraphCsvImport = null
                                isProviderImporting = false
                                result.fold(
                                    onSuccess = { importResult ->
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.storygraph_import_success_with_summary,
                                                importResult.importedRows,
                                                importResult.skippedDuplicateRows,
                                                importResult.unsupportedRows,
                                                importResult.invalidRows,
                                            ),
                                        )
                                    },
                                    onFailure = {
                                        snackbarHostState.showSnackbar(providerImportFailedMessage)
                                    },
                                )
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (isProviderImporting) R.string.provider_import_in_progress
                            else R.string.storygraph_import_confirm,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = { if (!isProviderImporting) pendingStoryGraphCsvImport = null },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingMyAnimeListAccountImport?.let { accountImport ->
        OmnilogAlertDialog(
            onDismissRequest = {
                if (!isProviderImporting) {
                    pendingMyAnimeListAccountImport = null
                    coroutineScope.launch { viewModel.discardMyAnimeListAccountImport() }
                }
            },
            title = stringResource(R.string.mal_account_import_title),
            text = {
                MalTitlePreferencePrompt(
                    summary = stringResource(
                        R.string.mal_account_import_message_with_summary,
                        accountImport.preview.importableRows,
                        accountImport.preview.totalRows,
                        accountImport.preview.skippedDuplicateRows,
                        accountImport.preview.unsupportedRows,
                        accountImport.preview.invalidRows,
                    ),
                    preference = animeTitlePreference,
                    onPreferenceChange = { animeTitlePreference = it },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = {
                        if (!isProviderImporting) {
                            isProviderImporting = true
                            AnimeTitlePreferences.write(context, animeTitlePreference)
                            coroutineScope.launch {
                                val result = runCatching {
                                    viewModel.importMyAnimeListAccount(accountImport)
                                }
                                pendingMyAnimeListAccountImport = null
                                isProviderImporting = false
                                result.fold(
                                    onSuccess = { importResult ->
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.mal_import_success_with_summary,
                                                importResult.importedRows,
                                                importResult.skippedDuplicateRows,
                                                importResult.unsupportedRows,
                                                importResult.invalidRows,
                                            ),
                                        )
                                    },
                                    onFailure = {
                                        snackbarHostState.showSnackbar(providerImportFailedMessage)
                                    },
                                )
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (isProviderImporting) R.string.provider_import_in_progress
                            else R.string.mal_import_confirm,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = {
                        if (!isProviderImporting) {
                            pendingMyAnimeListAccountImport = null
                            coroutineScope.launch { viewModel.discardMyAnimeListAccountImport() }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingMyAnimeListXmlImport?.let { malImport ->
        OmnilogAlertDialog(
            onDismissRequest = { if (!isProviderImporting) pendingMyAnimeListXmlImport = null },
            title = stringResource(R.string.mal_import_title),
            text = {
                MalTitlePreferencePrompt(
                    summary = stringResource(
                        R.string.mal_import_message_with_summary,
                        malImport.preview.importableRows,
                        malImport.preview.totalRows,
                        malImport.preview.skippedDuplicateRows,
                        malImport.preview.unsupportedRows,
                        malImport.preview.invalidRows,
                    ),
                    preference = animeTitlePreference,
                    onPreferenceChange = { animeTitlePreference = it },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = {
                        if (!isProviderImporting) {
                            isProviderImporting = true
                            AnimeTitlePreferences.write(context, animeTitlePreference)
                            coroutineScope.launch {
                                val result = runCatching {
                                    viewModel.importPreparedMyAnimeListXml(malImport.prepared)
                                }
                                pendingMyAnimeListXmlImport = null
                                isProviderImporting = false
                                result.fold(
                                    onSuccess = { importResult ->
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.mal_import_success_with_summary,
                                                importResult.importedRows,
                                                importResult.skippedDuplicateRows,
                                                importResult.unsupportedRows,
                                                importResult.invalidRows,
                                            ),
                                        )
                                    },
                                    onFailure = {
                                        snackbarHostState.showSnackbar(providerImportFailedMessage)
                                    },
                                )
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (isProviderImporting) R.string.provider_import_in_progress
                            else R.string.mal_import_confirm,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProviderImporting,
                    onClick = { if (!isProviderImporting) pendingMyAnimeListXmlImport = null },
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showMalInitialSyncConfirmation) {
        val synchronizableAnimeCount = uiState.allTrackedItems.count { tracked ->
            tracked.item.type == MediaType.Anime && tracked.item.malId != null
        }
        val unmatchedAnimeCount = uiState.allTrackedItems.count { tracked ->
            tracked.item.type == MediaType.Anime && tracked.item.malId == null
        }
        OmnilogAlertDialog(
            onDismissRequest = { showMalInitialSyncConfirmation = false },
            title = "Activa la sincronització amb MyAnimeList?",
            text = {
                Text(
                    text = "S'enviaran $synchronizableAnimeCount animes. $unmatchedAnimeCount no tenen " +
                        "identificador MAL i s'ignoraran. Els animes que només existeixin a MAL no s'esborraran.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMalInitialSyncConfirmation = false
                        viewModel.enableAndSyncMyAnimeList()
                    },
                ) {
                    Text("Envia la biblioteca")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMalInitialSyncConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingMetadataChange?.let { pending ->
        MetadataRefreshConfirmationDialog(
            preview = pending.preview,
            onDismiss = { pendingMetadataChange = null },
            onConfirm = { selectedFields ->
                coroutineScope.launch {
                    val result = viewModel.applyMediaItemMetadataRefresh(pending.preview, selectedFields)
                    pendingMetadataChange = null
                    val isLink = pending.operation == MetadataChangeOperation.Link
                    val issueResult = if (result.getOrDefault(false) && pending.importIssueItemId != null) {
                        viewModel.completeImportIssueAfterManualLink(
                            pending.importIssueItemId,
                            pending.preview.refreshed,
                        )
                    } else {
                        null
                    }
                    snackbarHostState.showSnackbar(
                        if (result.getOrDefault(false) && issueResult?.isFailure == true) {
                            "S'han aplicat les metadades, però no s'ha pogut tancar la incidència."
                        } else if (result.getOrDefault(false)) {
                            if (isLink) metadataLinkSuccessMessage else metadataRefreshSuccessMessage
                        } else {
                            if (isLink) metadataLinkErrorMessage else metadataRefreshErrorMessage
                        },
                    )
                }
            },
        )
    }

    if (showImportHub) {
        ImportHubDialog(
            state = importEnrichmentState,
            onDismiss = { showImportHub = false },
            onToggle = viewModel::toggleImportEnrichment,
            onRetry = viewModel::retryImportEnrichmentIssues,
            onCancel = viewModel::cancelImportEnrichment,
            onPrepareReview = viewModel::prepareImportReview,
            onApplyReview = viewModel::applyImportReview,
            onSkipReview = viewModel::skipImportReview,
            onRetryIssue = viewModel::retryImportEnrichmentIssue,
            onSkipIssue = viewModel::skipImportEnrichmentIssue,
            onManualMatch = { issue: ImportIssueItem ->
                val trackedMedia = uiState.allTrackedItems.firstOrNull {
                    it.item.id == issue.mediaItemId
                }
                if (trackedMedia == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Aquest títol ja no és a la biblioteca.")
                    }
                } else {
                    showImportHub = false
                    startMetadataLink(trackedMedia)
                    metadataLinkImportIssueId = issue.itemId
                }
            },
            onRetryCoverage = viewModel::retryImportMetadataCoverage,
            onDismissCoverage = viewModel::dismissImportMetadataCoverage,
            onManualMatchCoverage = { item: ImportCoverageItem ->
                val trackedMedia = uiState.allTrackedItems.firstOrNull {
                    it.item.id == item.mediaItemId
                }
                if (trackedMedia == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Aquest títol ja no és a la biblioteca.")
                    }
                } else {
                    showImportHub = false
                    startMetadataLink(trackedMedia)
                }
            },
            onDeleteHistory = viewModel::deleteImportHistoryBatch,
            onClearHistory = viewModel::clearFinishedImportHistory,
        )
    }

    if (showAnimeTitleBulkConfirmation) {
        OmnilogAlertDialog(
            onDismissRequest = { showAnimeTitleBulkConfirmation = false },
            title = "Actualitza els títols d'anime?",
            text = {
                Text(
                    text = when (animeTitlePreference) {
                        AnimeTitlePreference.EnglishWithRomajiOriginal ->
                            "S'aplicarà el títol anglès i es desarà el rōmaji com a títol original. " +
                                "Els títols editats manualment no es modificaran. El progrés serà visible a la barra d'importació."
                        AnimeTitlePreference.KeepMalTitle ->
                            "Es tornarà a enriquir la biblioteca conservant els títols de MyAnimeList. " +
                                "Els títols editats manualment no es modificaran."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAnimeTitleBulkConfirmation = false
                        coroutineScope.launch {
                            val result = viewModel.refreshAnimeTitleLanguage()
                            snackbarHostState.showSnackbar(
                                result.fold(
                                    onSuccess = { count -> "$count animes en cua per actualitzar els títols." },
                                    onFailure = { "No s'ha pogut iniciar l'actualització dels títols." },
                                ),
                            )
                        }
                    },
                ) {
                    Text("Actualitza en bloc")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnimeTitleBulkConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showMetadataBulkConfirmation) {
        OmnilogAlertDialog(
            onDismissRequest = { showMetadataBulkConfirmation = false },
            title = "Actualitza totes les metadades?",
            text = {
                Text(
                    "S'actualitzaran en segon pla les dades dels elements vinculats a un proveïdor. " +
                        "Els camps editats manualment, les puntuacions personals i el seguiment no es modificaran.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMetadataBulkConfirmation = false
                        coroutineScope.launch {
                            val result = viewModel.startBulkMetadataRefresh()
                            snackbarHostState.showSnackbar(
                                result.fold(
                                    onSuccess = { start ->
                                        when {
                                            start.alreadyRunning -> "Ja hi ha una actualització de metadades en curs."
                                            start.queuedCount == 0 -> "No hi ha cap element vinculat que es pugui actualitzar."
                                            else -> "${start.queuedCount} elements en cua per actualitzar les metadades."
                                        }
                                    },
                                    onFailure = { "No s'ha pogut iniciar l'actualització de metadades." },
                                ),
                            )
                        }
                    },
                ) { Text("Actualitza en bloc") }
            },
            dismissButton = {
                TextButton(onClick = { showMetadataBulkConfirmation = false }) {
                    Text(text = stringResource(R.string.cancel))
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
            onDismissRequest = {
                metadataLinkTarget = null
                metadataLinkImportIssueId = null
            },
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
                    Text(
                        text = stringResource(R.string.metadata_link_title, providerName),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    Text(
                        text = stringResource(
                            R.string.metadata_link_message,
                            displayMediaTitle(target.item.title)
                        ),
                        color = OmnilogTheme.colors.appMuted,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = metadataLinkQuery,
                            onValueChange = { metadataLinkQuery = it },
                            label = {
                                Text(
                                    text = stringResource(
                                        R.string.metadata_link_search_label,
                                        providerName
                                    )
                                )
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

                    val linkAccent = target.item.type.homeSection().themedAccent()
                    when {
                        isMetadataLinkLoading -> OmnilogStatusPanel(
                            text = stringResource(R.string.metadata_link_loading),
                            accent = linkAccent,
                            showProgressIndicator = true,
                        )

                        hasMetadataLinkError -> OmnilogStatusPanel(
                            text = stringResource(R.string.metadata_link_search_error),
                            accent = linkAccent,
                            textColor = MaterialTheme.colorScheme.error,
                            action = EmptyStateAction(
                                label = stringResource(R.string.retry_action),
                                onClick = { searchMetadataLink(target, metadataLinkQuery) },
                            ),
                        )

                        metadataLinkSuggestions.isEmpty() -> OmnilogStatusPanel(
                            text = stringResource(R.string.metadata_link_empty),
                            accent = linkAccent,
                        )

                        else -> LazyColumn(
                            modifier = Modifier.weight(1f),
                        ) {
                            itemsIndexed(metadataLinkSuggestions) { index, suggestion ->
                                if (index > 0) HorizontalDivider(color = OmnilogTheme.colors.appLine)
                                MetadataSuggestionRow(
                                    suggestion = suggestion,
                                    accent = target.item.type.homeSection().themedAccent(),
                                    duplicateState = MetadataDuplicateState.None,
                                    showSource = false,
                                    horizontalPadding = 0.dp,
                                    onClick = {
                                        coroutineScope.launch {
                                            isMetadataLinkLoading = true
                                            val result = viewModel.previewMediaItemMetadataLink(
                                                target.item.id,
                                                suggestion,
                                            )
                                            isMetadataLinkLoading = false
                                            if (metadataLinkTarget?.item?.id != target.item.id) {
                                                return@launch
                                            }
                                            val preview = result.getOrNull()
                                            when {
                                                result.isFailure || preview == null -> {
                                                    snackbarHostState.showSnackbar(metadataLinkErrorMessage)
                                                }

                                                preview.requiresMetadataConfirmation() -> {
                                                    val importIssueItemId = metadataLinkImportIssueId
                                                    metadataLinkTarget = null
                                                    metadataLinkImportIssueId = null
                                                    pendingMetadataChange = PendingMetadataChange(
                                                        preview = preview,
                                                        operation = MetadataChangeOperation.Link,
                                                        importIssueItemId = importIssueItemId,
                                                    )
                                                }

                                                else -> {
                                                    val applyResult = viewModel.applyMediaItemMetadataRefresh(
                                                        preview = preview,
                                                        selectedFields = preview.defaultSelectedMetadataFields(),
                                                    )
                                                    val issueResult = if (
                                                        applyResult.getOrDefault(false) &&
                                                        metadataLinkImportIssueId != null
                                                    ) {
                                                        viewModel.completeImportIssueAfterManualLink(
                                                            requireNotNull(metadataLinkImportIssueId),
                                                            preview.refreshed,
                                                        )
                                                    } else {
                                                        null
                                                    }
                                                    if (applyResult.getOrDefault(false) && issueResult?.isFailure != true) {
                                                        metadataLinkTarget = null
                                                        metadataLinkImportIssueId = null
                                                    }
                                                    snackbarHostState.showSnackbar(
                                                        if (applyResult.getOrDefault(false) && issueResult?.isFailure == true) {
                                                            "S'han aplicat les metadades, però no s'ha pogut tancar la incidència."
                                                        } else if (applyResult.getOrDefault(false)) {
                                                            metadataLinkSuccessMessage
                                                        } else {
                                                            metadataLinkErrorMessage
                                                        },
                                                    )
                                                }
                                            }
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
                        TextButton(
                            onClick = {
                                metadataLinkTarget = null
                                metadataLinkImportIssueId = null
                            },
                        ) {
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
                            backStack.push(AppRoute.AddMedia(suggestion.mediaType.homeSection()))
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

@Composable
private fun ProviderImportPreviewSummary(
    preview: ProviderImportPreview,
    summary: String,
) {
    val groups = preview.rejectedGroups()
    Column(
        modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = summary)
        if (groups.isNotEmpty()) {
            HorizontalDivider(color = OmnilogTheme.colors.appLine)
            Text(
                text = stringResource(R.string.provider_import_rejected_title),
                fontWeight = FontWeight.Bold,
                color = OmnilogTheme.colors.appInk,
            )
            groups.forEach { group ->
                Text(
                    text = stringResource(
                        when (group.reason) {
                            ProviderRejectedReason.Duplicate -> R.string.provider_import_rejected_duplicate
                            ProviderRejectedReason.UnsupportedType -> R.string.provider_import_rejected_unsupported
                            ProviderRejectedReason.MissingTitle -> R.string.provider_import_rejected_missing_title
                        },
                        group.count,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                group.samples.forEach { row ->
                    Text(
                        text = row.label?.let { label ->
                            stringResource(R.string.provider_import_rejected_sample, row.rowNumber, label)
                        } ?: stringResource(
                            R.string.provider_import_rejected_sample_without_label,
                            row.rowNumber,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                val remaining = group.count - group.samples.size
                if (remaining > 0) {
                    Text(
                        text = stringResource(R.string.provider_import_rejected_more, remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        }
    }
}

private data class PendingImdbCsvImport(
    val prepared: PreparedImdbCsvImport,
) {
    val preview: ImdbCsvPreview get() = prepared.preview
}

private data class PendingStoryGraphCsvImport(
    val prepared: PreparedStoryGraphCsvImport,
) {
    val preview: StoryGraphCsvPreview get() = prepared.preview
}

private data class PendingMyAnimeListXmlImport(
    val prepared: PreparedMyAnimeListXmlImport,
) {
    val preview: MyAnimeListXmlPreview get() = prepared.preview
}

private data class PendingMetadataChange(
    val preview: MetadataRefreshPreview,
    val operation: MetadataChangeOperation,
    val importIssueItemId: Long? = null,
)

private enum class MetadataChangeOperation {
    Refresh,
    Link,
}

@Composable
private fun MalTitlePreferencePrompt(
    summary: String,
    preference: AnimeTitlePreference,
    onPreferenceChange: (AnimeTitlePreference) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(summary)
        Text(
            text = "Com vols desar els títols?",
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
        )
        AnimeTitlePreference.entries.forEach { option ->
            val selected = option == preference
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPreferenceChange(option) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    OmnilogTheme.accents.Anime.copy(alpha = 0.12f)
                } else {
                    OmnilogTheme.colors.appPanel
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) OmnilogTheme.accents.Anime else OmnilogTheme.colors.appLine,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = when (option) {
                            AnimeTitlePreference.EnglishWithRomajiOriginal -> "Anglès + original en rōmaji"
                            AnimeTitlePreference.KeepMalTitle -> "Conserva el títol de MAL"
                        },
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    Text(
                        text = when (option) {
                            AnimeTitlePreference.EnglishWithRomajiOriginal ->
                                "Usa l'anglès com a títol principal i el rōmaji com a títol original."
                            AnimeTitlePreference.KeepMalTitle ->
                                "Manté exactament el títol retornat per la importació."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataRefreshConfirmationDialog(
    preview: MetadataRefreshPreview,
    onDismiss: () -> Unit,
    onConfirm: (Set<MetadataRefreshField>) -> Unit,
) {
    var selectedFields by remember(preview) {
        mutableStateOf(
            preview.defaultSelectedMetadataFields(),
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
            color = OmnilogTheme.colors.appBackground,
            border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
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
                    color = OmnilogTheme.colors.appInk,
                )
                Text(
                    text = stringResource(R.string.metadata_refresh_confirm_message),
                    color = OmnilogTheme.colors.appMuted,
                )
                MetadataDiffFieldList(
                    changes = preview.changes,
                    selectedFields = selectedFields,
                    onSelectionChanged = { selectedFields = it },
                    modifier = Modifier.weight(1f),
                )
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

private data class PendingPossibleDuplicate(
    val suggestion: MetadataSuggestion,
    val trackedMedia: TrackedMedia,
    val requiresAddTransition: Boolean = false,
)

class DetailHeaderActions {
    /**
     * How opaque the top bar should be, 0 while the detail page's backdrop is still behind it and 1
     * once the page has scrolled out from under it.
     *
     * The bar goes transparent over the backdrop, which means scrolled content would otherwise run
     * straight into it and into the status bar. The screen owns the scroll position and the bar
     * owns its own colour, so the screen publishes this and the bar reads it — the same arrangement
     * as the callbacks below.
     */
    var barOpacity by mutableFloatStateOf(0f)

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

/**
 * Lets the Profile screen hand its edit sheet to the top bar, which owns the button but not the
 * state behind it. Same shape as [BackupHeaderActions]: the screen assigns, the bar invokes.
 */
class ProfileHeaderActions {
    var onEditRequested: () -> Unit = {}
}

/** Lets the Activity screen expose its configuration sheet through the shared top bar. */
class TimelineHeaderActions {
    var onSettingsRequested: () -> Unit = {}
}

/**
 * What the add flow tells the top bar: the current step's title, and how to step back from it.
 *
 * [onStepBack] returns false on the first step, where back leaves the page instead.
 */
class AddMediaHeaderActions {
    var title by mutableStateOf("")
    var onStepBack: () -> Boolean = { false }
}

class BackupHeaderActions {
    var onExportBackupRequested: () -> Unit = {}
    var onImportBackupRequested: () -> Unit = {}
    var onImportMyAnimeListXmlRequested: () -> Unit = {}
    var onImportImdbCsvRequested: () -> Unit = {}
    var onImportStoryGraphCsvRequested: () -> Unit = {}
    var onRestoreBackupRequested: () -> Unit = {}
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
    selectedRootRoute: AppRoute,
    onHomeClick: () -> Unit,
    onSectionClick: (MediaSection) -> Unit,
) {
    Surface(
        color = OmnilogTheme.colors.appBackground,
        contentColor = OmnilogTheme.colors.appInk,
        shadowElevation = 12.dp,
    ) {
        Column {
            HorizontalDivider(color = OmnilogTheme.colors.appLine.copy(alpha = 0.72f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OmnilogTheme.colors.appPanel.copy(alpha = 0.74f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OmnilogNavItem(
                    labelResId = R.string.nav_home,
                    iconResId = R.drawable.ic_nav_home,
                    accent = OmnilogTheme.accents.Dashboard,
                    selected = selectedRootRoute == AppRoute.Home,
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f),
                )
                MediaSection.entries.forEach { section ->
                    OmnilogNavItem(
                        labelResId = section.titleResId,
                        iconResId = section.navIconResId,
                        accent = section.themedAccent(),
                        selected = (selectedRootRoute as? AppRoute.Section)?.section == section,
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
    val contentColor = if (selected) accent else OmnilogTheme.colors.appMuted
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Selection and press both go through one animated scale, so the icon never snaps between sizes.
    val iconScale by animateFloatAsState(
        targetValue = (if (selected) 28f / 26f else 1f) * (if (pressed) 1.1f else 1f),
        animationSpec = tween(durationMillis = 220),
        label = "navIconScale",
    )

    // No pill behind the selected tab: the accent on the icon carries the selection on its own, and
    // a tinted rounded square around it was a second, louder signal saying the same thing. The
    // clickable still covers the full cell, so the target is unchanged by the box going away.
    // Press feedback is the icon growing slightly instead of a ripple over the whole cell.
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(start = 4.dp, top = 7.dp, end = 4.dp, bottom = 11.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .scale(iconScale),
            tint = contentColor,
        )
        Text(
            text = stringResource(labelResId),
            color = if (selected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@DrawableRes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OmnilogTopBar(
    accent: Color,
    title: String?,
    showBackNavigation: Boolean,
    showDetailActions: Boolean,
    showProfileAction: Boolean,
    showProfileControls: Boolean,
    showHomeSettingsAction: Boolean,
    showHomeSearchAction: Boolean,
    showSectionActions: Boolean,
    showTimelineSettingsAction: Boolean,
    /** A local file or a cover URL. */
    profileImage: Any?,
    overCover: Boolean,
    detailActions: DetailHeaderActions,
    onProfileRequested: () -> Unit,
    onProfileEditRequested: () -> Unit,
    onSettingsRequested: () -> Unit,
    onHomeSearchRequested: () -> Unit,
    onSectionSearchRequested: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onSearchClose: () -> Unit,
    onSectionAddRequested: () -> Unit,
    onTimelineSettingsRequested: () -> Unit,
    onBack: () -> Unit,
) {
    // Over the detail page's backdrop the bar has no surface of its own. The artwork behind it is
    // washed in the page's own background, so the theme's ink stays legible on it throughout. The
    // surface fades in with the scroll rather than switching, so content never collides with the bar
    // or the status bar on the way up.
    val opacity = if (overCover) detailActions.barOpacity else 1f
    val barInk = OmnilogTheme.colors.appInk
    val barMuted = OmnilogTheme.colors.appMuted

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OmnilogTheme.colors.appBackground.copy(alpha = opacity),
            titleContentColor = barInk,
            navigationIconContentColor = barInk,
            actionIconContentColor = barInk,
        ),
        navigationIcon = {
            if (showBackNavigation) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = barInk,
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
            // Opening search fades the field in with a short slide from the icon's side; closing fades back.
            AnimatedContent(
                targetState = searchQuery != null,
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 }) togetherWith fadeOut(tween(120))
                    } else {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                    }
                },
                label = "topBarSearch",
            ) { searching ->
                if (searching) {
                    val focusRequester = remember { FocusRequester() }
                    // Opening from the icon lands in the field ready to type; a query handed over from
                    // Home is there to read, so it does not pull the keyboard up.
                    LaunchedEffect(Unit) {
                        if (searchQuery.isNullOrEmpty()) focusRequester.requestFocus()
                    }
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        DashboardStyleSearchBar(
                            query = searchQuery.orEmpty(),
                            onQueryChange = onSearchQueryChange,
                            onSearchSubmitted = onSearchSubmitted,
                            isLoading = false,
                            accent = accent,
                            leadingIcon = Icons.Filled.Search,
                            placeholder = stringResource(R.string.search_label),
                            fieldModifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { onSearchFocusChange(it.isFocused) },
                        ) {
                            IconButton(onClick = onSearchClose, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = OmnilogTheme.colors.appMuted,
                                )
                            }
                        }
                    }
                } else if (title == null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)) {
                                append("Omni")
                            }
                            withStyle(
                                SpanStyle(
                                    color = OmnilogTheme.colors.appInk,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append("log")
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                    )
                } else if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            // The icons fade and fold away, handing their room to the search field as it widens.
            AnimatedVisibility(visible = searchQuery == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showDetailActions) {
                        Box {
                            IconButton(onClick = { detailActions.isMenuExpanded = true }) {
                                // No disc under this one. It sits on the blurred backdrop rather than on
                                // the sharp cover, which is dark enough under the scrim that the dots hold
                                // on their own.
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    tint = barMuted,
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
                                containerColor = OmnilogTheme.colors.appPanel,
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
                                        detailActions.isEditingItemDetails =
                                            !detailActions.isEditingItemDetails
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
                    } else if (showTimelineSettingsAction) {
                        IconButton(onClick = onTimelineSettingsRequested) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.timeline_settings_open),
                                tint = OmnilogTheme.accents.Dashboard,
                            )
                        }
                    } else if (showProfileControls) {
                        IconButton(onClick = onProfileEditRequested) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edita el perfil",
                                tint = barInk,
                            )
                        }
                    } else {
                        if (showSectionActions) {
                            IconButton(onClick = onSectionSearchRequested) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search_label),
                                    tint = OmnilogTheme.colors.appMuted,
                                )
                            }
                            IconButton(onClick = onSectionAddRequested) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_item),
                                    tint = accent,
                                )
                            }
                        }
                        if (showHomeSearchAction) {
                            IconButton(onClick = onHomeSearchRequested) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search_label),
                                    tint = OmnilogTheme.colors.appMuted,
                                )
                            }
                        }
                        if (showHomeSettingsAction) {
                            IconButton(onClick = onSettingsRequested) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Configuració",
                                    tint = OmnilogTheme.colors.appMuted,
                                )
                            }
                        }
                        if (showProfileAction) {
                            IconButton(onClick = onProfileRequested) {
                                if (profileImage != null) {
                                    AsyncImage(
                                        model = profileImage,
                                        contentDescription = stringResource(R.string.profile_menu),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = stringResource(R.string.profile_menu),
                                        tint = OmnilogTheme.colors.appMuted,
                                    )
                                }
                            }
                        }
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
        !enabled -> OmnilogTheme.colors.appMuted.copy(alpha = 0.62f)
        destructive -> MaterialTheme.colorScheme.error
        else -> OmnilogTheme.colors.appInk
    }

    Row(
        modifier = Modifier
            .widthIn(min = 148.dp)
            .background(OmnilogTheme.colors.appPanel)
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

private fun providerImportReadErrorMessage(
    error: Throwable?,
    fallback: String,
    encodingError: String,
    tooLarge: String,
): String = when (error) {
    is ProviderImportEncodingException -> encodingError
    is ProviderImportFileTooLargeException -> tooLarge
    else -> fallback
}

private suspend fun Context.readProviderImportText(uri: Uri): Result<String> = runCatching {
    withContext(Dispatchers.IO) {
        checkNotNull(contentResolver.openInputStream(uri)) {
            "Could not open provider import source"
        }.use { inputStream ->
            inputStream.readProviderImportText()
        }
    }
}

private data class ProviderCsvValidationMessages(
    val invalid: String,
    val emptyFile: String,
    val missingColumns: String,
    val headerOnly: String,
    val noUsableRows: String,
)

private fun Throwable?.providerCsvValidationMessage(
    context: Context,
    messages: ProviderCsvValidationMessages,
    malformedFallback: String,
): String = when (this) {
    is MalformedProviderCsvException -> rowNumber?.let { row ->
        context.getString(R.string.provider_import_malformed_csv_at_row, row)
    } ?: malformedFallback
    is ProviderCsvValidationException -> when (issue) {
        ProviderCsvValidationIssue.EmptyFile -> messages.emptyFile
        ProviderCsvValidationIssue.MissingRequiredColumns ->
            messages.missingColumns.format(missingColumns.joinToString())
        ProviderCsvValidationIssue.NoDataRows -> messages.headerOnly
        ProviderCsvValidationIssue.NoUsableRows -> messages.noUsableRows
    }
    else -> messages.invalid
}

/** An open completion page and the answer it waits for: true when the user undoes. */
private class CompletionCelebrationRequest(
    val reaction: CompletionReaction,
    val canUndo: Boolean,
    val undo: CompletableDeferred<Boolean>,
)

/** An open goal page and the answer it waits for: true when the user undoes. */
private class ObjectiveCelebrationRequest(
    val step: ObjectiveStep,
    val canUndo: Boolean,
    val undo: CompletableDeferred<Boolean>,
)
