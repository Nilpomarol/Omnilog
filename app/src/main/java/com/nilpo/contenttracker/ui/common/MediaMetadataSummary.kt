package com.nilpo.contenttracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import coil3.compose.AsyncImage

data class MediaMetadataUi(
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val releaseYear: Int? = null,
    val language: String? = null,
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
    val externalRatingSourceName: String? = null,
    val popularityScore: Double? = null,
    val rankingPosition: Int? = null,
    val rankingLabel: String? = null,
    val collectionName: String? = null,
    val collectionSortOrder: Double? = null,
    val isOwned: Boolean = false,
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
    onCollectionClick: (() -> Unit)? = null,
    onCreatorClick: ((String) -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box {
            MetadataCoverImage(
                coverUrl = metadata.coverUrl,
                modifier = Modifier.size(width = 156.dp, height = 234.dp),
            )
            if (metadata.isOwned) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (metadata.isOwned) {
                        CoverBadge(
                            iconResId = R.drawable.ic_owned_badge,
                            contentDescription = stringResource(R.string.owned_label),
                            tint = OmnilogColors.Dashboard,
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .height(234.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                metadata.collectionDisplayName()?.let { collectionName ->
                    Text(
                        text = collectionName,
                        modifier = if (onCollectionClick != null) {
                            Modifier.clickable(onClick = onCollectionClick)
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = metadata.displayTitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
        metadata.originalTitle?.let { originalTitle ->
            Text(
                text = originalTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogColors.AppMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                )
            }
                metadata.language?.let { language ->
                    Text(
                        text = stringResource(R.string.metadata_language_value, languageLabel(language)),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (metadata.creators.isNotEmpty()) {
                    Column {
                        metadata.creators.forEach { creator ->
                            Text(
                                text = creator,
                                modifier = if (onCreatorClick != null) {
                                    Modifier.clickable { onCreatorClick(creator) }
                                } else Modifier,
                                style = MaterialTheme.typography.titleSmall,
                                color = OmnilogColors.AppInk.copy(alpha = 0.84f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (isLoadingDetails) {
                    Text(
                        text = stringResource(R.string.metadata_details_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogColors.AppMuted,
                    )
                }
            }

            HeroMetrics(metadata = metadata)
        }
    }
}

@Composable
private fun CoverBadge(
    iconResId: Int,
    contentDescription: String,
    tint: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = tint,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(6.dp)
                .size(15.dp),
        )
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

        metadata.language?.let { language ->
            MetadataSection(
                title = stringResource(R.string.metadata_language),
                body = languageLabel(language),
            )
        }

        metadata.synopsis?.let { synopsis ->
            MetadataSection(
                title = stringResource(R.string.metadata_summary),
                body = synopsis,
                collapsible = true,
                renderAsSynopsis = true,
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
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = OmnilogColors.AppPanel,
                border = BorderStroke(1.dp, OmnilogColors.AppLine),
                contentColor = OmnilogColors.AppMuted,
            ) {
                Text(
                    text = genre,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HeroMetrics(metadata: MediaMetadataUi) {
    val rating = metadata.externalRatingScore
    val users = metadata.externalRatingVoteCount?.toDouble() ?: metadata.popularityScore
    val length = metadata.progressTotal.takeUnless { metadata.mediaType == MediaType.Game }
    val supportingStats = listOfNotNull(
        metadata.sourceName?.takeIf { rating == null },
        metadata.rankingText(),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            HeroMetric(
                label = metadata.externalRatingSourceName
                    ?: metadata.sourceName
                    ?: stringResource(R.string.field_rating),
                value = rating?.let { score ->
                    metadata.externalRatingMax?.let { maxScore ->
                        formatExternalRatingOnTen(score, maxScore)
                    } ?: "${formatDecimal(score)}/10"
                } ?: "-",
                color = OmnilogColors.Dashboard,
                modifier = Modifier.weight(1f),
            )
            if (metadata.mediaType != MediaType.Game) {
                HeroMetric(
                    label = stringResource(metadata.totalUnitLabelRes()),
                    value = length?.toString() ?: "-",
                    color = OmnilogColors.AppInk,
                    modifier = Modifier.weight(1f),
                )
            }
            HeroMetric(
                label = stringResource(R.string.metadata_users),
                value = users?.let(::formatCompactCount) ?: "-",
                color = OmnilogColors.AppInk,
                modifier = Modifier.weight(1f),
            )
        }

        if (supportingStats.isNotEmpty()) {
            Text(
                text = supportingStats.joinToString("  |  "),
                color = OmnilogColors.AppMuted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            color = OmnilogColors.AppMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MetadataCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        coverUrl?.let { url ->
            AsyncImage(
                model = url,
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
        language = language,
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
        externalRatingSourceName = externalRating?.let { rating ->
            externalRatings.firstOrNull { externalRating ->
                externalRating.score == rating.score && externalRating.maxScore == rating.maxScore
            }?.source?.displayName()
        },
        popularityScore = popularityScore,
        rankingPosition = rankingPosition,
        rankingLabel = rankingLabel,
    )
}

fun ExternalRatingSource.displayName(): String {
    return when (this) {
        ExternalRatingSource.AniList -> "AniList"
        ExternalRatingSource.Mal -> "MAL"
        ExternalRatingSource.Imdb -> "IMDb"
        ExternalRatingSource.Metacritic -> "Metacritic"
        ExternalRatingSource.Goodreads -> "Goodreads"
        ExternalRatingSource.GoogleBooks -> "Google Books"
        ExternalRatingSource.OpenLibrary -> "Open Library"
        ExternalRatingSource.Tmdb -> "TMDb"
        ExternalRatingSource.Rawg -> "RAWG"
        ExternalRatingSource.RottenTomatoes -> "Rotten Tomatoes"
        ExternalRatingSource.Steam -> "Steam"
        ExternalRatingSource.FilmAffinity -> "FilmAffinity"
        ExternalRatingSource.StoryGraph -> "StoryGraph"
    }
}

fun MediaItem.toMediaMetadataUi(credits: List<MediaCredit>): MediaMetadataUi {
    return MediaMetadataUi(
        mediaType = type,
        title = title,
        originalTitle = originalTitle,
        releaseYear = releaseYear,
        language = language,
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
    renderAsSynopsis: Boolean = false,
) {
    var isExpanded by remember(body) { mutableStateOf(false) }
    val plainBody = remember(body) { plainSynopsis(body).orEmpty() }
    val shouldCollapse = collapsible && plainBody.length > 260

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = OmnilogColors.AppMuted,
            fontWeight = FontWeight.SemiBold,
        )
        if (renderAsSynopsis) {
            SynopsisText(
                body = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogColors.AppInk.copy(alpha = 0.84f),
                maxLines = if (shouldCollapse && !isExpanded) 5 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogColors.AppInk.copy(alpha = 0.84f),
                maxLines = if (shouldCollapse && !isExpanded) 5 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    return displayMediaTitle(title)
}

fun formatCollectionDisplayName(name: String?, sortOrder: Double?): String? {
    val collectionName = name?.takeIf { it.isNotBlank() } ?: return null
    return sortOrder?.let { "$collectionName #${formatCollectionOrder(it)}" } ?: collectionName
}

private fun MediaMetadataUi.collectionDisplayName(): String? {
    return formatCollectionDisplayName(collectionName, collectionSortOrder)
}

fun formatCollectionOrder(sortOrder: Double): String {
    return if (sortOrder % 1.0 == 0.0) {
        sortOrder.toInt().toString()
    } else {
        sortOrder.toString().trimEnd('0').trimEnd('.')
    }
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

private fun formatCompactCount(value: Double): String {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue >= 1_000_000_000 -> "${formatDecimal(value / 1_000_000_000)}B"
        absValue >= 1_000_000 -> "${formatDecimal(value / 1_000_000)}M"
        absValue >= 1_000 -> "${formatDecimal(value / 1_000)}k"
        else -> formatDecimal(value)
    }
}
