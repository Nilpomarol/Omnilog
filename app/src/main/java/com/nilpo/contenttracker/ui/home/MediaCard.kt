package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingSession
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.OwnedBadge
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.common.formatCollectionDisplayName
import com.nilpo.contenttracker.ui.common.formatExternalRating
import com.nilpo.contenttracker.ui.common.progressLabel
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CardPadding = 6.dp
private const val CoverAspectRatio = 2f / 3f
// Every card is pinned to this height so lists read as an even stack. It has to fit the
// tallest the information column can get: a two-line title, collection, creator, one genre
// row, and the full progress footer.
private val BaseCardBodyHeight = 156.dp
private const val MaxVisibleGenres = 2

@Composable
fun MediaCard(
    trackedMedia: TrackedMedia,
    accent: Color,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val item = trackedMedia.item
    val session = trackedMedia.currentSession
    val primaryExternalRating = trackedMedia.primaryExternalRating
    val creator = item.creators.firstOrNull()
    val collection = formatCollectionDisplayName(
        trackedMedia.collection?.name,
        item.collectionSortOrder,
    )
    val genres = item.genres.take(MaxVisibleGenres)
    val additionalGenreCount = (item.genres.size - genres.size).coerceAtLeast(0)
    // Text is measured in sp and the card in dp, so the pin has to track the system font
    // setting or larger text would clip. fontScale is global, so cards stay uniform.
    val bodyHeight = BaseCardBodyHeight * LocalDensity.current.fontScale

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Row(
            modifier = Modifier
                .padding(CardPadding)
                .height(bodyHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The row is a fixed height, so the cover fills it and derives its width from
            // the 2:3 poster ratio. Nothing is cropped and no gap opens up underneath.
            MetadataCoverImage(
                coverUrl = item.coverUrl,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(CoverAspectRatio),
                shape = RoundedCornerShape(6.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = displayMediaTitle(item.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (collection != null) {
                            Text(
                                text = collection,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (creator != null) {
                            Text(
                                text = creator,
                                style = MaterialTheme.typography.bodySmall,
                                color = OmnilogTheme.colors.appMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (genres.isNotEmpty()) {
                            GenreChips(
                                genres = genres,
                                additionalGenreCount = additionalGenreCount,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (item.ownership.isOwned) {
                            OwnedBadge()
                        }
                        if (session != null) {
                            CardStateIconBadge(status = session.status)
                        }
                        trailingAction?.invoke()
                    }
                }

                CardProgressFooter(
                    session = session,
                    progressTotal = item.progressTotal.takeUnless { item.type == MediaType.Game },
                    mediaType = item.type,
                    progressColor = session?.status?.stateColor ?: accent,
                    accent = accent,
                    externalRatingScore = item.externalRatingScore,
                    externalRatingMax = item.externalRatingMax,
                    externalRatingSource = primaryExternalRating?.source,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(
    genres: List<String>,
    additionalGenreCount: Int,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        maxItemsInEachRow = MaxVisibleGenres + 1,
        maxLines = 1,
    ) {
        genres.forEach { genre ->
            GenreChip(text = genre)
        }
        if (additionalGenreCount > 0) {
            GenreChip(text = "+$additionalGenreCount")
        }
    }
}

@Composable
private fun GenreChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = OmnilogTheme.colors.appLine,
        contentColor = OmnilogTheme.colors.appMuted,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CardProgressFooter(
    session: TrackingSession?,
    progressTotal: Int?,
    mediaType: MediaType,
    progressColor: Color,
    accent: Color,
    externalRatingScore: Double?,
    externalRatingMax: Double?,
    externalRatingSource: ExternalRatingSource?,
) {
    val isGame = mediaType == MediaType.Game
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = session.progressLabel(progressTotal, mediaType),
                modifier = Modifier.weight(1f, fill = false),
                style = if (isGame) {
                    MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                fontWeight = if (isGame) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isGame) accent else OmnilogTheme.colors.appMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CardRatings(
                personalRating = session?.rating,
                externalRatingScore = externalRatingScore,
                externalRatingMax = externalRatingMax,
                externalRatingSource = externalRatingSource,
                mediaType = mediaType,
                accent = accent,
            )
        }
        if (progressTotal != null && progressTotal > 0) {
            CardProgressBar(
                fraction = session.progressFraction(progressTotal),
                color = progressColor,
            )
        }
        CardDates(session = session)
    }
}

@Composable
private fun CardRatings(
    personalRating: Int?,
    externalRatingScore: Double?,
    externalRatingMax: Double?,
    externalRatingSource: ExternalRatingSource?,
    mediaType: MediaType,
    accent: Color,
) {
    val externalRating = if (externalRatingScore != null && externalRatingMax != null) {
        formatExternalRating(
            score = externalRatingScore,
            maxScore = externalRatingMax,
            mediaType = mediaType,
            source = externalRatingSource,
        ).let { formatted ->
            if (mediaType == MediaType.Game && externalRatingSource == ExternalRatingSource.Steam) {
                formatted
            } else {
                formatted.removeSuffix("/10")
            }
        }
    } else {
        null
    }
    if (personalRating == null && externalRating == null) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        personalRating?.let { rating ->
            val description = stringResource(R.string.library_row_personal_rating, rating)
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_kpi_rating),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "$rating/10",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
            }
        }
        externalRating?.let { value ->
            val description = if (
                mediaType == MediaType.Game && externalRatingSource == ExternalRatingSource.Steam
            ) {
                stringResource(R.string.library_row_external_rating_percentage, value)
            } else {
                stringResource(R.string.library_row_external_rating, value)
            }
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_external_rating),
                    contentDescription = null,
                    tint = OmnilogTheme.colors.appMuted,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
    }
}

@Composable
private fun CardStateIconBadge(
    status: TrackingStatus,
    modifier: Modifier = Modifier,
) {
    val color = status.stateColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color,
        contentColor = Color.Black,
    ) {
        Icon(
            painter = painterResource(status.iconResId),
            contentDescription = status.label(),
            modifier = Modifier
                .padding(4.dp)
                .size(13.dp),
        )
    }
}

@Composable
private fun CardProgressBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(OmnilogTheme.colors.appLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun CardDates(session: TrackingSession?) {
    val parts = buildList {
        session?.startedAt?.let { add(stringResource(R.string.session_started_at, it.formatDate())) }
        session?.finishedAt?.let { add(stringResource(R.string.session_finished_at, it.formatDate())) }
    }
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = OmnilogTheme.colors.appMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private val TrackingStatus.stateColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        TrackingStatus.Planned -> OmnilogTheme.accents.Planned
        TrackingStatus.InProgress -> OmnilogTheme.accents.InProgress
        TrackingStatus.Completed -> OmnilogTheme.accents.Completed
        TrackingStatus.Paused -> OmnilogTheme.accents.Paused
        TrackingStatus.Dropped -> OmnilogTheme.accents.Dropped
    }

private val TrackingStatus.iconResId: Int
    get() = when (this) {
        TrackingStatus.Planned -> R.drawable.ic_state_planned
        TrackingStatus.InProgress -> R.drawable.ic_state_in_progress
        TrackingStatus.Completed -> R.drawable.ic_state_completed
        TrackingStatus.Paused -> R.drawable.ic_state_paused
        TrackingStatus.Dropped -> R.drawable.ic_state_dropped
    }

@Composable
private fun TrackingStatus.label(): String = when (this) {
    TrackingStatus.Planned -> stringResource(R.string.status_planned)
    TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
    TrackingStatus.Completed -> stringResource(R.string.status_completed)
    TrackingStatus.Paused -> stringResource(R.string.status_paused)
    TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
}

private fun TrackingSession?.progressFraction(progressTotal: Int?): Float {
    val current = this?.progressCurrent ?: 0
    if (progressTotal == null || progressTotal <= 0) return 0f
    return current.toFloat().div(progressTotal.toFloat()).coerceIn(0f, 1f)
}

private fun LocalDate.formatDate(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yy"))
