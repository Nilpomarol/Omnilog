package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.creatorNames
import com.nilpo.contenttracker.ui.home.MediaGridCard
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

    val currentCreators = current.creatorNames().normalizedValues()
    val currentGenres = current.item.genres.normalizedValues()
    val currentCollectionId = current.collection?.id ?: current.item.collectionId

    val matches = library
        .asSequence()
        .filter { candidate -> candidate.item.id != current.item.id }
        .mapNotNull { candidate ->
            val sharedCreators = candidate.creatorNames().matchingValues(currentCreators)
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
    relatedMedia: List<TrackedMedia>,
    onMediaClick: (TrackedMedia) -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The title keeps the gutter; the row spends it as content padding instead, so the covers
        // run off the screen edge rather than stopping short of it.
        DetailSectionTitle(
            text = title,
            modifier = Modifier.padding(horizontal = DetailGutter),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = DetailGutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = relatedMedia,
                key = { trackedMedia -> trackedMedia.item.id },
            ) { trackedMedia ->
                // The library grid's own tile, so a related title looks here as it does in its list.
                Box(modifier = Modifier.width(RelatedTileWidth)) {
                    MediaGridCard(
                        trackedMedia = trackedMedia,
                        onClick = { onMediaClick(trackedMedia) },
                        showStatus = showStatus,
                    )
                }
            }
        }
    }
}

private val RelatedTileWidth = 104.dp

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
