package com.nilpo.contenttracker.ui.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.theme.ContentTrackerTheme
import java.time.LocalDate

@Preview(
    name = "Home - populated - dark",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun HomePopulatedDarkPreview() {
    HomePreviewContent(populatedHomeState())
}

@Preview(
    name = "Home - populated - light",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Composable
private fun HomePopulatedLightPreview() {
    HomePreviewContent(populatedHomeState())
}

@Preview(
    name = "Home - large text",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.5f,
    showBackground = true,
)
@Composable
private fun HomeLargeTextPreview() {
    HomePreviewContent(populatedHomeState())
}

@Preview(
    name = "Home - empty",
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun HomeEmptyPreview() {
    HomePreviewContent(HomeUiState(isLoading = false))
}

@Composable
private fun HomePreviewContent(uiState: HomeUiState) {
    ContentTrackerTheme {
        HomeLandingScreen(
            uiState = uiState,
            timelineEntries = emptyList(),
            onMediaClick = {},
            onSectionSearch = { _, _ -> },
            onStatsClick = {},
            onTimelineClick = {},
            onObjectivesClick = {},
            onStatusClick = {},
            onOpenSection = {},
            onImportFrom = {},
            onImportBackup = {},
        )
    }
}

private fun populatedHomeState(): HomeUiState {
    val today = LocalDate.of(2026, 9, 12)
    return HomeUiState(
        isLoading = false,
        allTrackedItems = listOf(
            previewMedia(
                id = 1,
                type = MediaType.Book,
                title = "The Left Hand of Darkness",
                creator = "Ursula K. Le Guin",
                status = TrackingStatus.InProgress,
                progress = 148,
                total = 304,
                updatedAt = 6,
                startedAt = today.minusDays(8),
            ),
            previewMedia(
                id = 2,
                type = MediaType.TvShow,
                title = "Severance",
                creator = "Dan Erickson",
                status = TrackingStatus.InProgress,
                progress = 6,
                total = 10,
                updatedAt = 5,
                startedAt = today.minusDays(5),
            ),
            previewMedia(
                id = 3,
                type = MediaType.Game,
                title = "Outer Wilds",
                creator = "Mobius Digital",
                status = TrackingStatus.Planned,
                progress = 0,
                total = null,
                updatedAt = 4,
            ),
            previewMedia(
                id = 4,
                type = MediaType.Anime,
                title = "Frieren: Beyond Journey's End",
                creator = "Madhouse",
                status = TrackingStatus.Planned,
                progress = 0,
                total = 28,
                updatedAt = 3,
            ),
            previewMedia(
                id = 5,
                type = MediaType.Book,
                title = "A Very Long Book Title That Tests How Editorial Cards Behave On Narrow Mobile Screens",
                creator = "Example Author",
                status = TrackingStatus.Paused,
                progress = 220,
                total = 640,
                updatedAt = 1,
                startedAt = today.minusMonths(2),
            ),
            previewMedia(
                id = 6,
                type = MediaType.Movie,
                title = "Perfect Days",
                creator = "Wim Wenders",
                status = TrackingStatus.Completed,
                progress = 124,
                total = 124,
                updatedAt = 2,
                startedAt = today.minusDays(2),
                finishedAt = today.minusDays(1),
            ),
        ),
    )
}

private fun previewMedia(
    id: Long,
    type: MediaType,
    title: String,
    creator: String,
    status: TrackingStatus,
    progress: Int,
    total: Int?,
    updatedAt: Long,
    startedAt: LocalDate? = null,
    finishedAt: LocalDate? = null,
): TrackedMedia = TrackedMedia(
    item = MediaItem(
        id = id,
        type = type,
        title = title,
        progressTotal = total,
        creators = listOf(creator),
    ),
    sessions = listOf(
        TrackingSession(
            id = id,
            mediaItemId = id,
            sessionNumber = 1,
            status = status,
            progressCurrent = progress,
            startedAt = startedAt,
            finishedAt = finishedAt,
            updatedAtEpochMillis = updatedAt,
        ),
    ),
)
