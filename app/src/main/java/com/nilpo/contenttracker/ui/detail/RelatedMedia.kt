package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.text.Normalizer

internal data class RelatedMediaMatch(
    val trackedMedia: TrackedMedia,
    val score: Int,
    val isSameCollection: Boolean,
    val sharedCreators: List<String>,
    val sharedGenres: List<String>,
)

internal data class RelatedMediaRecommendations(
    val collection: List<RelatedMediaMatch>,
    val generic: List<RelatedMediaMatch>,
)

internal fun findRelatedMedia(
    current: TrackedMedia,
    library: List<TrackedMedia>,
    limit: Int = 8,
): RelatedMediaRecommendations {
    if (limit <= 0) {
        return RelatedMediaRecommendations(
            collection = emptyList(),
            generic = emptyList(),
        )
    }

    val currentCreators = current.item.creators.normalizedValues()
    val currentGenres = current.item.genres.normalizedValues()
    val currentCollectionId = current.collection?.id ?: current.item.collectionId

    val matches = library
        .asSequence()
        .filter { candidate -> candidate.item.id != current.item.id }
        .mapNotNull { candidate ->
            val sharedCreators = candidate.item.creators.matchingValues(currentCreators)
            val sharedGenres = candidate.item.genres.matchingValues(currentGenres)
            val isSameCollection = currentCollectionId != null &&
                currentCollectionId == (candidate.collection?.id ?: candidate.item.collectionId)

            if (!isSameCollection && sharedCreators.isEmpty() && sharedGenres.isEmpty()) {
                return@mapNotNull null
            }

            val score = (if (isSameCollection) 10_000 else 0) +
                (sharedCreators.size.coerceAtMost(2) * 100) +
                (sharedGenres.size.coerceAtMost(3) * 10) +
                (if (candidate.item.type == current.item.type) 1 else 0)

            RelatedMediaMatch(
                trackedMedia = candidate,
                score = score,
                isSameCollection = isSameCollection,
                sharedCreators = sharedCreators,
                sharedGenres = sharedGenres,
            )
        }
        .toList()

    return RelatedMediaRecommendations(
        collection = matches
            .filter(RelatedMediaMatch::isSameCollection)
            .sortedWith(collectionMatchComparator())
            .take(limit),
        generic = matches
            .filterNot(RelatedMediaMatch::isSameCollection)
            .sortedWith(genericMatchComparator())
            .take(limit),
    )
}

@Composable
internal fun RelatedMediaSection(
    title: String,
    relatedMedia: List<RelatedMediaMatch>,
    accent: Color,
    onMediaClick: (TrackedMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailSectionTitle(text = title)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = relatedMedia,
                key = { match -> match.trackedMedia.item.id },
            ) { match ->
                RelatedMediaCard(
                    match = match,
                    accent = accent,
                    onClick = { onMediaClick(match.trackedMedia) },
                )
            }
        }
    }
}

@Composable
private fun RelatedMediaCard(
    match: RelatedMediaMatch,
    accent: Color,
    onClick: () -> Unit,
) {
    val item = match.trackedMedia.item
    Surface(
        modifier = Modifier
            .width(144.dp)
            .height(224.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MetadataCoverImage(
                coverUrl = item.coverUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF17110D).copy(alpha = 0.10f),
                                Color(0xFF15110E).copy(alpha = 0.96f),
                            ),
                        ),
                    ),
            )
            RelatedStatusMarker(
                status = match.trackedMedia.currentSession?.status,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp),
            )
            item.releaseYear?.let { year ->
                YearMarker(
                    text = year.toString(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayMediaTitle(item.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RowLabel(
                    text = stringResource(match.reasonLabelResId()),
                    accent = accent,
                )
            }
        }
    }
}

@Composable
private fun RowLabel(
    text: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.92f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = OmnilogColors.AppBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RelatedStatusMarker(
    status: TrackingStatus?,
    modifier: Modifier = Modifier,
) {
    val color = status?.stateColor ?: Color.White.copy(alpha = 0.28f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (status == null) 0.10f else 0.92f),
        contentColor = if (status == null) Color.White.copy(alpha = 0.34f) else Color.Black,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (status != null) {
                Icon(
                    painter = painterResource(status.iconResId),
                    contentDescription = status.label(),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun YearMarker(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
    }
}

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

private val TrackingStatus.stateColor: Color
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogColors.Planned
        TrackingStatus.InProgress -> OmnilogColors.InProgress
        TrackingStatus.Completed -> OmnilogColors.Completed
        TrackingStatus.Paused -> OmnilogColors.Paused
        TrackingStatus.Dropped -> OmnilogColors.Dropped
    }

@Composable
private fun TrackingStatus.label(): String = when (this) {
    TrackingStatus.Planned -> stringResource(R.string.status_planned)
    TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
    TrackingStatus.Completed -> stringResource(R.string.status_completed)
    TrackingStatus.Paused -> stringResource(R.string.status_paused)
    TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
}

private fun collectionMatchComparator(): Comparator<RelatedMediaMatch> {
    return compareBy<RelatedMediaMatch> {
        it.trackedMedia.item.collectionSortOrder ?: Double.MAX_VALUE
    }
        .thenBy { it.trackedMedia.item.title.lowercase() }
        .thenBy { it.trackedMedia.item.id }
}

private fun genericMatchComparator(): Comparator<RelatedMediaMatch> {
    return compareByDescending<RelatedMediaMatch> { it.score }
        .thenBy { it.trackedMedia.item.title.lowercase() }
        .thenBy { it.trackedMedia.item.id }
}

private fun RelatedMediaMatch.reasonLabelResId(): Int {
    return when {
        isSameCollection -> R.string.detail_related_same_collection
        sharedCreators.isNotEmpty() -> R.string.detail_related_same_creator
        else -> R.string.detail_related_shared_genres
    }
}

private fun List<String>.normalizedValues(): Set<String> {
    return map(::normalizeRelatedValue)
        .filter(String::isNotBlank)
        .toSet()
}

private fun List<String>.matchingValues(reference: Set<String>): List<String> {
    return asSequence()
        .filter { value -> normalizeRelatedValue(value) in reference }
        .distinctBy(::normalizeRelatedValue)
        .toList()
}

private fun normalizeRelatedValue(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .filter { character -> character.isLetterOrDigit() || character.isWhitespace() }
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
}
