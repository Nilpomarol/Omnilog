package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.ui.common.ContributorImageBox
import com.nilpo.contenttracker.ui.detail.BarPaddingReclaim
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.detail.sessionStateVisual
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

private val StatusOrder = listOf(
    TrackingStatus.Completed,
    TrackingStatus.InProgress,
    TrackingStatus.Planned,
    TrackingStatus.Paused,
    TrackingStatus.Dropped,
)

/**
 * A creator — author, director, studio or developer — as the editorial pages draw a group of works: their
 * portrait or logo beside a serif name, one line on how their works stand for you, then the works
 * themselves as the library lists them, in whichever of its orders you pick.
 */
@Composable
fun AuthorDetailScreen(
    authorName: String,
    authorImageUrl: String?,
    imageIsLogo: Boolean,
    creatorLabelResId: Int,
    items: List<TrackedMedia>,
    accent: Color,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
    /** The app bar's height; the page scrolls under the bar, which draws its own surface. */
    topInset: Dp = 0.dp,
    /** How solid the transparent app bar should be: clear over the header, solid once it scrolls away. */
    onTopBarOpacityChange: (Float) -> Unit = {},
) {
    val listState = rememberLazyListState()
    TopBarOpacityEffect(listState = listState, solid = false, onOpacityChange = onTopBarOpacityChange)

    // Ordered with the library's own sort choices and default, kept for as long as the page is.
    var sortMode by rememberSaveable { mutableStateOf(HomeSortMode.Recent) }
    var sortDirection by rememberSaveable { mutableStateOf(HomeSortMode.Recent.defaultDirection()) }
    val works = items.sortByMode(sortMode, sortDirection)

    Box(modifier = modifier.background(OmnilogTheme.colors.appBackground)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Tucked up under the bar by as much as the item page, so both open at the same height.
            contentPadding = PaddingValues(top = topInset - BarPaddingReclaim, bottom = 24.dp),
        ) {
            item(key = "header") {
                EditorialPageHeader(
                    overline = stringResource(creatorLabelResId),
                    title = authorName,
                    items = items,
                    accent = accent,
                    averageRating = items.collectionAverageRating(),
                    image = {
                        CreatorPicture(
                            imageUrl = authorImageUrl,
                            name = authorName,
                            isLogo = imageIsLogo,
                            items = items,
                            accent = accent,
                        )
                    },
                )
            }
            item(key = "status") {
                StatusSummary(
                    items = items,
                    modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 24.dp),
                )
            }
            item(key = "works_title") {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = DetailGutter),
                        color = OmnilogTheme.colors.appLine,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = DetailGutter, end = DetailGutter, top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DetailSectionTitle(
                            text = stringResource(R.string.author_works_title),
                            modifier = Modifier.weight(1f),
                        )
                        SortMenu(
                            sortMode = sortMode,
                            sortDirection = sortDirection,
                            accent = accent,
                            onSortModeChange = { mode ->
                                sortMode = mode
                                sortDirection = mode.defaultDirection()
                            },
                            onSortDirectionChange = { sortDirection = it },
                        )
                    }
                }
            }
            items(works, key = { it.item.id }) { trackedMedia ->
                Box(modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 12.dp)) {
                    MediaCard(
                        trackedMedia = trackedMedia,
                        accent = accent,
                        onClick = { onMediaClick(trackedMedia) },
                        // The page already names the creator; a row still shows a different first creator,
                        // such as a co-author or the director on an actor's page.
                        showCreator = !trackedMedia.creatorNames().firstOrNull().equals(authorName, ignoreCase = true),
                    )
                }
            }
        }
    }
}

/**
 * A portrait in a cover's shape, or a logo mounted on a white card as the library list shows it. With no
 * picture at all the creator's own covers stand in, stacked.
 */
@Composable
private fun CreatorPicture(
    imageUrl: String?,
    name: String,
    isLogo: Boolean,
    items: List<TrackedMedia>,
    accent: Color,
) {
    val shape = RoundedCornerShape(6.dp)
    when {
        imageUrl == null -> CollectionCoverStack(
            coverStack = items.collectionCoverStack(),
            itemCount = items.size,
            modifier = Modifier.size(width = 128.dp, height = 192.dp),
        )
        isLogo -> Box(
            modifier = Modifier
                .width(128.dp)
                .aspectRatio(4f / 3f)
                .shadow(elevation = 8.dp, shape = shape)
                .background(Color.White)
                .padding(14.dp),
        ) {
            ContributorImageBox(
                imageUrl = imageUrl,
                name = name,
                isCompany = true,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
        }
        else -> ContributorImageBox(
            imageUrl = imageUrl,
            name = name,
            isCompany = false,
            accent = accent,
            modifier = Modifier
                .size(width = 128.dp, height = 192.dp)
                .shadow(elevation = 12.dp, shape = shape),
        )
    }
}

/**
 * Where the creator's works stand for you: each status that holds any of them as its own icon and name in
 * the status colour — the same marks the status picker and the item page use — followed by the count.
 * The pairs wrap as whole pairs when they do not fit on one line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusSummary(
    items: List<TrackedMedia>,
    modifier: Modifier = Modifier,
) {
    val counts = StatusOrder.mapNotNull { status ->
        items.count { it.currentSession?.status == status }
            .takeIf { it > 0 }
            ?.let { status to it }
    }
    if (counts.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        counts.forEach { (status, count) ->
            val visual = sessionStateVisual(status)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(visual.icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = visual.color,
                )
                Text(
                    text = visual.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = visual.color,
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
    }
}
