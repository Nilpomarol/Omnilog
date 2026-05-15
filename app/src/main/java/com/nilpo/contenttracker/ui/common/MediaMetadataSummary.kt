package com.nilpo.contenttracker.ui.common

import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        MediaMetadataHero(metadata = metadata, isLoadingDetails = isLoadingDetails)
        GenreRow(genres = metadata.genres)
        MediaMetadataSecondary(metadata = metadata)
    }
}

@Composable
fun MediaMetadataHero(
    metadata: MediaMetadataUi,
    modifier: Modifier = Modifier,
    isLoadingDetails: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MetadataCoverImage(
            coverUrl = metadata.coverUrl,
            modifier = Modifier.size(width = 132.dp, height = 198.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .height(198.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = metadata.displayTitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                metadata.originalTitle?.let { originalTitle ->
                    Text(
                        text = originalTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    )
                }
                if (metadata.creators.isNotEmpty()) {
                    Text(
                        text = metadata.creators.joinToString(", "),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                metadata.sourceRatingText()?.let { sourceRating ->
                    MetadataPill(value = sourceRating)
                }
                metadata.rankingText()?.let { ranking ->
                    MetadataPill(value = ranking)
                }
                metadata.popularityScore?.let { popularity ->
                    MetadataPill(
                        label = stringResource(R.string.metadata_users),
                        value = formatDecimal(popularity),
                    )
                }
                metadata.progressTotal?.let { total ->
                    MetadataPill(
                        label = stringResource(metadata.totalUnitLabelRes()),
                        value = total.toString(),
                    )
                }
            }
        }
    }
}

@Composable
fun MediaMetadataHeroGenres(
    metadata: MediaMetadataUi,
    modifier: Modifier = Modifier,
) {
    GenreRow(
        genres = metadata.genres,
        modifier = modifier,
    )
}

@Composable
fun MediaMetadataSecondary(
    metadata: MediaMetadataUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        metadata.providerCollectionTitle?.let { collectionTitle ->
            MetadataSection(
                title = stringResource(R.string.metadata_provider_collection),
                body = collectionTitle,
            )
        }

        metadata.synopsis?.let { synopsis ->
            MetadataSection(
                title = stringResource(R.string.metadata_summary),
                body = synopsis,
                collapsible = true,
            )
        }

        CreditGroups(credits = metadata.credits)
    }
}

@Composable
private fun GenreRow(
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.forEach { genre ->
            AssistChip(
                onClick = {},
                label = { Text(text = genre) },
            )
        }
    }
}

@Composable
private fun MetadataPill(
    value: String,
    label: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label?.let { "$it $value" } ?: value,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
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
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
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
private fun MetadataSection(title: String, body: String) {
    MetadataSection(
        title = title,
        body = body,
        collapsible = false,
    )
}

@Composable
private fun MetadataSection(
    title: String,
    body: String,
    collapsible: Boolean,
) {
    var isExpanded by remember(body) { mutableStateOf(false) }
    val shouldCollapse = collapsible && body.length > 260

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
            maxLines = if (shouldCollapse && !isExpanded) 5 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
        if (shouldCollapse) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(
                        text = stringResource(
                            if (isExpanded) R.string.show_less else R.string.show_more,
                        ),
                    )
                }
            }
        }
    }
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
        MetadataSection(
            title = stringResource(role.labelRes()),
            body = values,
        )
    }
}

private fun MediaMetadataUi.displayTitle(): String {
    return listOfNotNull(
        title,
        releaseYear?.let { "($it)" },
    ).joinToString(" ")
}

private fun MediaMetadataUi.sourceRatingText(): String? {
    val source = sourceName
    val score = externalRatingScore?.let(::formatDecimal)
    return when {
        source != null && score != null -> "$source $score"
        source != null -> source
        score != null -> score
        else -> null
    }
}

private fun MediaMetadataUi.rankingText(): String? {
    val position = rankingPosition ?: return null
    return rankingLabel?.let { "#$position $it" } ?: "#$position"
}

@StringRes
private fun MediaMetadataUi.totalUnitLabelRes(): Int {
    return when (mediaType) {
        MediaType.Anime,
        MediaType.TvShow,
        -> R.string.metadata_total_episodes
        MediaType.Book -> R.string.metadata_total_pages
        MediaType.Movie -> R.string.metadata_total_minutes
        MediaType.Game -> R.string.metadata_total_hours
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
