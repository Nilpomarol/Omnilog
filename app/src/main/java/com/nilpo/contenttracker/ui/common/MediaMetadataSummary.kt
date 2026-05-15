package com.nilpo.contenttracker.ui.common

import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataRatingSuggestion
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class MediaMetadataUi(
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val releaseYear: Int? = null,
    val progressTotal: Int? = null,
    val coverUrl: String? = null,
    val synopsis: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val providerCollectionTitle: String? = null,
    val genres: List<String> = emptyList(),
    val creators: List<String> = emptyList(),
    val credits: List<MediaCredit> = emptyList(),
    val externalRatingScore: Double? = null,
    val externalRatingMax: Double? = null,
    val externalRatingVoteCount: Int? = null,
    val popularityScore: Double? = null,
    val rankingPosition: Int? = null,
    val rankingLabel: String? = null,
)

@Composable
fun MediaMetadataSummary(
    metadata: MediaMetadataUi,
    modifier: Modifier = Modifier,
    isLoadingDetails: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetadataCoverImage(
                coverUrl = metadata.coverUrl,
                modifier = Modifier.size(width = 104.dp, height = 156.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = metadata.displayTitle(),
                    style = MaterialTheme.typography.titleLarge,
                )
                metadata.originalTitle?.let { originalTitle ->
                    MetadataLine(
                        label = stringResource(R.string.metadata_original_title),
                        value = originalTitle,
                    )
                }
                metadata.sourceName?.let { sourceName ->
                    Text(
                        text = stringResource(R.string.metadata_suggestion_source, sourceName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    )
                }
                metadata.progressTotal?.let { total ->
                    Text(
                        text = stringResource(R.string.progress_total_value, total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                metadata.providerCollectionTitle?.let { collectionTitle ->
                    MetadataLine(
                        label = stringResource(R.string.metadata_provider_collection),
                        value = collectionTitle,
                    )
                }
                metadata.externalRatingText()?.let { ratingText ->
                    MetadataLine(
                        label = stringResource(R.string.metadata_external_rating_label),
                        value = ratingText,
                    )
                }
                metadata.popularityScore?.let { popularity ->
                    MetadataLine(
                        label = stringResource(R.string.metadata_popularity),
                        value = formatDecimal(popularity),
                    )
                }
                metadata.rankingText()?.let { ranking ->
                    MetadataLine(
                        label = stringResource(R.string.metadata_ranking),
                        value = ranking,
                    )
                }
                if (isLoadingDetails) {
                    Text(
                        text = stringResource(R.string.metadata_details_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    )
                }
            }
        }

        if (metadata.creators.isNotEmpty()) {
            MetadataLine(
                label = stringResource(metadata.creatorLabelRes()),
                value = metadata.creators.joinToString(", "),
            )
        }

        if (metadata.genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                metadata.genres.forEach { genre ->
                    AssistChip(
                        onClick = {},
                        label = { Text(text = genre) },
                    )
                }
            }
        }

        metadata.synopsis?.let { synopsis ->
            Text(
                text = synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
            )
        }

        CreditGroups(credits = metadata.credits)
    }
}

@Composable
fun MetadataCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    var image by remember(coverUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(coverUrl) {
        image = coverUrl?.let { url ->
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(url).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        image?.let { loadedImage ->
            Image(
                bitmap = loadedImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } ?: Box(modifier = Modifier.fillMaxSize())
    }
}

fun MetadataSuggestion.toMediaMetadataUi(): MediaMetadataUi {
    return MediaMetadataUi(
        mediaType = mediaType,
        title = title,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        progressTotal = progressTotal,
        coverUrl = coverUrl,
        synopsis = synopsis,
        sourceName = source.name,
        sourceUrl = sourceUrl,
        providerCollectionTitle = collectionTitle,
        genres = genres,
        creators = creators,
        credits = credits,
        externalRatingScore = externalRating?.score,
        externalRatingMax = externalRating?.maxScore,
        externalRatingVoteCount = externalRating?.voteCount,
        popularityScore = popularityScore,
        rankingPosition = rankingPosition,
        rankingLabel = rankingLabel,
    )
}

fun MediaItem.toMediaMetadataUi(credits: List<MediaCredit>): MediaMetadataUi {
    return MediaMetadataUi(
        mediaType = type,
        title = title,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        progressTotal = progressTotal,
        coverUrl = coverUrl,
        synopsis = synopsis,
        sourceName = metadataSource?.name,
        sourceUrl = sourceUrl,
        providerCollectionTitle = providerCollectionTitle,
        genres = genres,
        creators = creators,
        credits = credits,
        externalRatingScore = externalRatingScore,
        externalRatingMax = externalRatingMax,
        externalRatingVoteCount = externalRatingVoteCount,
        popularityScore = popularityScore,
        rankingPosition = rankingPosition,
        rankingLabel = rankingLabel,
    )
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
    )
}

@Composable
private fun CreditGroups(credits: List<MediaCredit>) {
    val groupedCredits = credits
        .filter { it.personName.isNotBlank() }
        .groupBy { it.roleType }
        .toSortedMap(compareBy { it.ordinal })

    groupedCredits.forEach { (role, roleCredits) ->
        val values = roleCredits
            .sortedWith(compareBy<MediaCredit> { it.sortOrder }.thenBy { it.personName })
            .take(12)
            .joinToString(", ") { credit ->
                credit.characterName?.let { "${credit.personName} ($it)" } ?: credit.personName
            }
        MetadataLine(
            label = stringResource(role.labelRes()),
            value = values,
        )
    }
}

private fun MediaMetadataUi.displayTitle(): String {
    return listOfNotNull(
        title,
        releaseYear?.let { "($it)" },
    ).joinToString(" ")
}

private fun MediaMetadataUi.externalRatingText(): String? {
    val score = externalRatingScore ?: return null
    val max = externalRatingMax ?: return null
    return externalRatingVoteCount?.let { voteCount ->
        "${formatDecimal(score)}/${formatDecimal(max)} ($voteCount)"
    } ?: "${formatDecimal(score)}/${formatDecimal(max)}"
}

private fun MediaMetadataUi.rankingText(): String? {
    val position = rankingPosition ?: return null
    return rankingLabel?.let { "#$position $it" } ?: "#$position"
}

@StringRes
private fun MediaMetadataUi.creatorLabelRes(): Int {
    return when (mediaType) {
        MediaType.Anime -> R.string.metadata_creator_anime
        MediaType.Book -> R.string.metadata_creator_book
        MediaType.Movie -> R.string.metadata_creator_movie
        MediaType.TvShow -> R.string.metadata_creator_tv
        MediaType.Game -> R.string.metadata_creator_game
    }
}

@StringRes
private fun MediaCreditRole.labelRes(): Int {
    return when (this) {
        MediaCreditRole.Author -> R.string.metadata_credits_authors
        MediaCreditRole.Director -> R.string.metadata_credits_directors
        MediaCreditRole.Creator -> R.string.metadata_credits_creators
        MediaCreditRole.Studio -> R.string.metadata_credits_studios
        MediaCreditRole.Developer -> R.string.metadata_credits_developers
        MediaCreditRole.Cast -> R.string.metadata_credits_cast
        MediaCreditRole.VoiceActor -> R.string.metadata_credits_voice_actors
    }
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}
