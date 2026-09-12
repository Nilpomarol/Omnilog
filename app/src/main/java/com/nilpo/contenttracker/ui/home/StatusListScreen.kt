package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.QuickCompletion
import com.nilpo.contenttracker.ui.common.QuickProgressSheet
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.objectiveAccent
import com.nilpo.contenttracker.ui.detail.lastActivityDate
import com.nilpo.contenttracker.ui.detail.recencyLabel
import com.nilpo.contenttracker.ui.detail.sessionResumeActionLabel
import com.nilpo.contenttracker.ui.detail.sessionStartActionLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Cross-media destination behind Home's "show all" status sections. */
@Composable
fun StatusListScreen(
    status: TrackingStatus,
    items: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    onQuickCommitProgress: (TrackedMedia, Int) -> Unit,
    onQuickComplete: (TrackedMedia, QuickCompletion) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayMode by rememberSaveable { mutableStateOf(HomeDisplayMode.List) }
    // Held by id and looked up fresh, so the sheet always edits the current session and closes on its
    // own once starting or resuming moves the title off this list.
    var sheetMediaId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sheetMedia = sheetMediaId?.let { id -> items.firstOrNull { it.item.id == id } }

    Surface(modifier = modifier, color = OmnilogTheme.colors.appBackground) {
        LazyVerticalGrid(
            columns = if (displayMode == HomeDisplayMode.Grid) GridCells.Fixed(2) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            // A small top inset: the top bar already carries the page title, so the hero can sit close under it.
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StatusStoryHeader(
                    status = status,
                    items = items,
                    displayMode = displayMode,
                    onModeSelected = { displayMode = it },
                    onMediaClick = onMediaClick,
                    onAction = { sheetMediaId = it.item.id },
                )
            }
            items(items, key = { it.item.id }) { trackedMedia ->
                if (displayMode == HomeDisplayMode.Grid) {
                    MediaGridCard(trackedMedia = trackedMedia, onClick = { onMediaClick(trackedMedia) })
                } else {
                    MediaCard(
                        trackedMedia = trackedMedia,
                        accent = trackedMedia.item.type.statusListAccent(),
                        onClick = { onMediaClick(trackedMedia) },
                    )
                }
            }
        }
    }
    sheetMedia?.let { media ->
        QuickProgressSheet(
            trackedMedia = media,
            accent = OmnilogTheme.accents.Dashboard,
            onCommit = { onQuickCommitProgress(media, it); sheetMediaId = null },
            onComplete = { onQuickComplete(media, it); sheetMediaId = null },
            onDismiss = { sheetMediaId = null },
        )
    }
}

/** The status page's title in your terms (`Tens 7 històries obertes`), shown in the top bar. */
@Composable
internal fun statusStoryHeadline(status: TrackingStatus, count: Int): String = when (status) {
    TrackingStatus.InProgress -> pluralStringResource(R.plurals.status_story_in_progress_headline, count, count)
    TrackingStatus.Planned -> pluralStringResource(R.plurals.status_story_planned_headline, count, count)
    TrackingStatus.Paused -> pluralStringResource(R.plurals.status_story_paused_headline, count, count)
    else -> stringResource(R.string.status_list_count, count)
}

/**
 * The page's opening, under the story headline in the top bar: the one title the status most points
 * at with its next step, and what the pile is made of.
 */
@Composable
private fun StatusStoryHeader(
    status: TrackingStatus,
    items: List<TrackedMedia>,
    displayMode: HomeDisplayMode,
    onModeSelected: (HomeDisplayMode) -> Unit,
    onMediaClick: (TrackedMedia) -> Unit,
    onAction: (TrackedMedia) -> Unit,
) {
    // In progress points at what you touched last; planned and paused at what has waited longest,
    // since those are the titles most easily forgotten.
    val spotlight = when (status) {
        TrackingStatus.InProgress -> items.maxByOrNull { it.storyDate() ?: LocalDate.MIN }
        TrackingStatus.Planned, TrackingStatus.Paused -> items.minByOrNull { it.storyDate() ?: LocalDate.MAX }
        else -> null
    }

    Column(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        spotlight?.let { media ->
            StoryHero(
                status = status,
                trackedMedia = media,
                onClick = { onMediaClick(media) },
                onAction = { onAction(media) },
            )
        }
        // A label over the counts, so a row of icons and numbers reads as the list's make-up.
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_story_types_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                )
                StoryTypeCounts(items)
            }
            StatusDisplayToggle(selectedMode = displayMode, onModeSelected = onModeSelected)
        }
    }
}

/**
 * The spotlighted title as the page's hero: its own artwork, darkened, as the backdrop, the cover in
 * front, and the one action that moves it along. Text sits on the dark scrim in either theme, so it
 * is white rather than themed ink.
 */
@Composable
private fun StoryHero(
    status: TrackingStatus,
    trackedMedia: TrackedMedia,
    onClick: () -> Unit,
    onAction: () -> Unit,
) {
    val item = trackedMedia.item
    val accent = status.storyAccent()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val total = item.progressTotal?.takeIf { it > 0 && item.type != MediaType.Game }
    // A planned title has not begun, so a 0% bar would only say what the headline already does.
    val fraction = total?.takeIf { status != TrackingStatus.Planned }?.let {
        ((trackedMedia.currentSession?.progressCurrent ?: 0).toFloat() / it).coerceIn(0f, 1f)
    }
    val eyebrow = stringResource(
        when (status) {
            TrackingStatus.InProgress -> R.string.status_story_in_progress_spotlight
            TrackingStatus.Planned -> R.string.status_story_planned_spotlight
            else -> R.string.status_story_paused_spotlight
        },
    )
    val detail = listOfNotNull(
        trackedMedia.storyDate()?.let { recencyLabel(it) }?.replaceFirstChar { it.uppercase(OmnilogLocale) },
        fraction?.takeIf { it > 0f }?.let { stringResource(R.string.status_story_percent, (it * 100).roundToInt()) },
    ).joinToString(" · ")
    val actionLabel = when (status) {
        TrackingStatus.Planned -> sessionStartActionLabel(item.type)
        TrackingStatus.Paused -> sessionResumeActionLabel(item.type)
        else -> stringResource(R.string.quick_progress_open)
    }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(212.dp + 90.dp * (fontScale - 1f))
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        MetadataCoverImage(coverUrl = item.coverUrl, modifier = Modifier.fillMaxSize(), shape = shape)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.9f),
                        1f to Color.Black.copy(alpha = 0.6f),
                    ),
                ),
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MetadataCoverImage(
                coverUrl = item.coverUrl,
                modifier = Modifier.width(122.dp).fillMaxHeight(),
                shape = RoundedCornerShape(10.dp),
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = displayMediaTitle(item.title),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                fraction?.let {
                    Box(
                        Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                    ) {
                        Box(Modifier.fillMaxWidth(it).fillMaxHeight().background(accent))
                    }
                }
                Button(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(start = 10.dp, end = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        imageVector = if (status == TrackingStatus.InProgress) Icons.Filled.Add else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = actionLabel,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** What the pile is made of, one icon and count per medium present. */
@Composable
private fun StoryTypeCounts(items: List<TrackedMedia>, modifier: Modifier = Modifier) {
    val counts = items.groupingBy { it.item.type }.eachCount()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaType.entries.filter { it in counts }.forEach { type ->
            val count = counts.getValue(type)
            val tint = type.objectiveAccent()
            val description = "$count ${stringResource(type.labelResId)}"
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ObjectiveMediaIcon(type, tint, 18.dp)
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                )
            }
        }
    }
}

/** When something last happened to the title. Planned titles have no activity, so their last edit stands in. */
private fun TrackedMedia.storyDate(): LocalDate? {
    val session = currentSession ?: return null
    return session.lastActivityDate() ?: session.updatedAtEpochMillis.takeIf { it > 0 }?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}

@Composable
private fun TrackingStatus.storyAccent(): Color = when (this) {
    TrackingStatus.Planned -> OmnilogTheme.accents.Planned
    TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
    TrackingStatus.Completed -> OmnilogTheme.accents.Completed
    TrackingStatus.Paused -> OmnilogTheme.accents.Paused
    TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
}

private val MediaType.labelResId: Int
    get() = when (this) {
        MediaType.Anime -> R.string.media_type_anime
        MediaType.Book -> R.string.media_type_book
        MediaType.Movie -> R.string.media_type_movie
        MediaType.TvShow -> R.string.media_type_tv_show
        MediaType.Game -> R.string.media_type_game
    }

@Composable
private fun StatusDisplayToggle(
    selectedMode: HomeDisplayMode,
    onModeSelected: (HomeDisplayMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row {
            HomeDisplayMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                IconButton(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (selected) OmnilogTheme.accents.Dashboard.copy(alpha = 0.18f)
                            else Color.Transparent,
                        ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (mode == HomeDisplayMode.List) R.drawable.ic_view_list else R.drawable.ic_view_grid,
                        ),
                        contentDescription = stringResource(
                            if (mode == HomeDisplayMode.List) R.string.view_list else R.string.view_grid,
                        ),
                        tint = if (selected) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaType.statusListAccent() = when (this) {
    MediaType.Anime -> MediaSection.Anime.themedAccent()
    MediaType.Book -> MediaSection.Books.themedAccent()
    MediaType.Movie, MediaType.TvShow -> MediaSection.Movies.themedAccent()
    MediaType.Game -> MediaSection.Games.themedAccent()
}
