package com.nilpo.contenttracker.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ContributorDirectory
import com.nilpo.contenttracker.core.model.ContributorStats
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaCreditRole
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.plainSynopsis
import com.nilpo.contenttracker.ui.common.ContributorImage
import com.nilpo.contenttracker.ui.common.SynopsisText
import com.nilpo.contenttracker.ui.common.languageLabel
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import coil3.compose.AsyncImage

/**
 * The work's reference page: compact facts first, then the pieces that need room to breathe.
 *
 * This deliberately leaves title, collection, genres and progress in the hero/session area. The
 * values below are the item record rather than a second version of the same introduction.
 *
 * [accent] is the item's own media-type accent. Everything tinted here takes it, so a game's page
 * is not dressed in the books purple — which is what happened while these tints were hard-coded.
 */
@Composable
fun ItemDetailsSection(
    item: MediaItem,
    credits: List<MediaCredit>,
    contributors: ContributorDirectory,
    accent: Color,
    onAuthorClick: (String, MediaCreditRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryFacts = listOfNotNull(
        item.language?.takeUnless { item.type == MediaType.Game }?.let {
            DetailFact(R.string.metadata_language, languageLabel(it))
        },
        item.progressTotal?.takeUnless { item.type == MediaType.Game }?.let {
            DetailFact(item.type.totalUnitLabelRes(), it.toString())
        },
        item.releaseYear?.let { DetailFact(R.string.field_release_year, it.toString()) },
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DetailSectionTitle(text = stringResource(R.string.detail_item_details))

        if (primaryFacts.isNotEmpty()) {
            DetailFacts(primaryFacts, columns = 3)
        }
        item.tags.takeIf { it.isNotEmpty() }?.let { tags ->
            DetailTagList(tags)
        }

        item.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
            DetailSynopsis(synopsis, accent)
        }

        CreditGroups(credits, contributors, item.type, accent, onAuthorClick)
    }
}

/**
 * Label over value, arranged in compact rows on the page itself rather than inside a ruled sheet.
 *
 * The label runs small and letter-spaced so it reads as a field name at a glance and the value
 * below it can carry the weight. An incomplete row leaves its remaining cells empty rather than
 * stretching its last value to full width, so the columns stay aligned.
 */
@Composable
private fun DetailFacts(facts: List<DetailFact>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        facts.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                row.forEach { fact ->
                    DetailFactCell(fact, Modifier.weight(1f))
                }
                repeat(columns - row.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun DetailFactCell(fact: DetailFact, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        DetailFieldLabel(stringResource(fact.labelRes))
        // A size up from the label rather than a shade of bold on the same size: the value is the
        // fact and the label names it, and the old titleMedium left the two nearly indistinguishable.
        Text(
            text = fact.value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tags are reference rather than controls, so they read as one line of text instead of a field of chips. */
@Composable
private fun DetailTagList(tags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DetailFieldLabel(stringResource(R.string.metadata_tags))
        Text(
            text = tags.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = OmnilogTheme.colors.appInk,
        )
    }
}

@Composable
private fun DetailSynopsis(body: String, accent: Color) {
    var expanded by remember(body) { mutableStateOf(false) }
    val plainBody = remember(body) { plainSynopsis(body).orEmpty() }
    val canExpand = plainBody.length > SynopsisCollapseThreshold

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailFieldLabel(stringResource(R.string.metadata_summary))
        SynopsisText(
            body = body,
            style = MaterialTheme.typography.bodyLarge,
            color = OmnilogTheme.colors.appInk.copy(alpha = 0.9f),
            maxLines = if (canExpand && !expanded) 7 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
        if (canExpand) {
            TextButton(
                modifier = Modifier.align(Alignment.Start),
                onClick = { expanded = !expanded },
            ) {
                Text(
                    text = stringResource(if (expanded) R.string.show_less else R.string.show_more),
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun CreditGroups(
    credits: List<MediaCredit>,
    contributors: ContributorDirectory,
    mediaType: MediaType,
    accent: Color,
    onAuthorClick: (String, MediaCreditRole) -> Unit,
) {
    val groupedCredits = credits
        .filter { it.personName.isNotBlank() }
        .groupBy { it.roleType }
        .toSortedMap(compareBy { it.ordinal })

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        groupedCredits.forEach { (role, roleCredits) ->
            CreditGroup(role, roleCredits, contributors, mediaType, accent, onAuthorClick)
        }
    }
}

/**
 * One role's credits, under the role's name.
 *
 * Two shapes, chosen by whether the credits carry a character. A studio or an author is a name and
 * nothing else, so the whole role fits on one wrapped line and a row each would be three quarters
 * whitespace. A cast is a set of pairs, so it gets a row each with the performer at the left margin
 * and the character at the right — the two columns are what makes the pairing readable without a
 * "com a" between every one of them.
 *
 * Either way only [CreditPreviewCount] are drawn until asked: a film's full cast is the single
 * longest thing on this page and it is not what the page is for.
 */
@Composable
private fun CreditGroup(
    role: MediaCreditRole,
    credits: List<MediaCredit>,
    contributors: ContributorDirectory,
    mediaType: MediaType,
    accent: Color,
    onAuthorClick: (String, MediaCreditRole) -> Unit,
) {
    var expanded by remember(role, credits) { mutableStateOf(false) }
    val ordered = remember(credits) {
        credits.sortedWith(compareBy<MediaCredit> { it.sortOrder }.thenBy { it.personName })
    }
    val hasPerformerCarousel = role in PerformerRoles

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailFieldLabel(stringResource(role.labelRes()))

        if (hasPerformerCarousel) {
            CreditCarousel(ordered, contributors, role, accent)
        } else {
            val visibleCredits = if (expanded) ordered else ordered.take(CreditPreviewCount)
            ContributorCreditList(
                credits = visibleCredits,
                contributors = contributors,
                role = role,
                isNavigable = role == mediaType.primaryContributorRole() || role == MediaCreditRole.Publisher,
                accent = accent,
                onAuthorClick = onAuthorClick,
            )

            if (ordered.size > CreditPreviewCount) {
                TextButton(
                    modifier = Modifier.align(Alignment.Start),
                    onClick = { expanded = !expanded },
                ) {
                    Text(
                        text = if (expanded) {
                            stringResource(R.string.show_less)
                        } else {
                            stringResource(
                                R.string.detail_credits_show_more,
                                ordered.size - CreditPreviewCount,
                            )
                        },
                        color = accent,
                    )
                }
            }
        }
    }
}

/**
 * A contributor is a useful route back into the library, not just a line of metadata. Keeping the
 * visual identity large makes authors, directors and companies easy to scan; the two small facts
 * make clear how much of the library they represent and how the user has rated that work.
 */
@Composable
private fun ContributorCreditList(
    credits: List<MediaCredit>,
    contributors: ContributorDirectory,
    role: MediaCreditRole,
    isNavigable: Boolean,
    accent: Color,
    onAuthorClick: (String, MediaCreditRole) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        credits.forEach { credit ->
            ContributorCreditRow(
                credit = credit,
                imageUrl = contributors.imageUrl(role, credit.personName)
                    ?: credit.personImageUrl,
                imageAspectRatio = contributors.imageAspectRatio(role, credit.personName)
                    ?: credit.personImageAspectRatio,
                stats = contributors.stats(role, credit.personName),
                role = role,
                accent = accent,
                onClick = if (isNavigable) {
                    { onAuthorClick(credit.personName, role) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ContributorCreditRow(
    credit: MediaCredit,
    imageUrl: String?,
    imageAspectRatio: Float?,
    stats: ContributorStats,
    role: MediaCreditRole,
    accent: Color,
    onClick: (() -> Unit)?,
) {
    val isCompany = role in CompanyRoles
    val imageHeight = if (isCompany) 58.dp else 98.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ContributorImage(
            imageUrl = imageUrl,
            name = credit.personName,
            isCompany = isCompany,
            accent = accent,
            height = imageHeight,
            logoAspectRatio = imageAspectRatio,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = imageHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = credit.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.collection_item_count, stats.itemCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            stats.averageRating?.let { averageRating ->
                Text(
                    text = stringResource(R.string.collection_average_rating, averageRating),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
        }
    }
}

/**
 * A visual credit card for cast and voice actors, using a portrait when the provider has one.
 *
 * A performer's portrait is shared across the library on the same terms as an author's or a
 * studio's: one film's cast entry carrying a headshot illustrates that performer everywhere. The
 * character's own artwork still wins where a provider supplies it, since that is specific to this
 * title rather than to the person.
 */
@Composable
private fun CreditCarousel(
    credits: List<MediaCredit>,
    contributors: ContributorDirectory,
    role: MediaCreditRole,
    accent: Color,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(credits) { credit ->
            // A bare portrait with the name underneath, like the cover tiles, rather than a bordered card.
            Column(modifier = Modifier.width(112.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = credit.personName.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    val performerImage = credit.characterImageUrl
                        ?: contributors.imageUrl(role, credit.personName)
                        ?: credit.personImageUrl
                    performerImage?.let { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = credit.personName,
                            // Filling both axes, not just the width: with the height left to the
                            // source image, a wide headshot measured shorter than the frame and left
                            // accent-coloured bands above and below it.
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Text(
                    text = credit.personName,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = credit.characterName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A field name: small, letter-spaced and muted, so it never competes with the value under it.
 *
 * Shared by the fact cells, the tag list, the synopsis and every credit role, which is the point —
 * these are all the same kind of thing and used to be set three different ways.
 */
@Composable
internal fun DetailFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = OmnilogTheme.colors.appMuted,
    )
}

private data class DetailFact(
    @StringRes val labelRes: Int,
    val value: String,
)

private const val SynopsisCollapseThreshold = 320
private const val CreditPreviewCount = 6
private val PerformerRoles = setOf(MediaCreditRole.Cast, MediaCreditRole.VoiceActor)
private val CompanyRoles = setOf(
    MediaCreditRole.Studio,
    MediaCreditRole.Developer,
    MediaCreditRole.Publisher,
)

private fun MediaType.primaryContributorRole(): MediaCreditRole = when (this) {
    MediaType.Anime -> MediaCreditRole.Studio
    MediaType.Book -> MediaCreditRole.Author
    MediaType.Movie -> MediaCreditRole.Director
    MediaType.TvShow -> MediaCreditRole.Creator
    MediaType.Game -> MediaCreditRole.Developer
}

@StringRes
private fun MediaType.totalUnitLabelRes(): Int = when (this) {
    MediaType.Anime,
    MediaType.TvShow,
        -> R.string.metadata_total_episodes
    MediaType.Book -> R.string.metadata_total_pages
    MediaType.Movie -> R.string.metadata_total_minutes
    MediaType.Game -> R.string.metadata_total_hours
}

@StringRes
private fun MediaCreditRole.labelRes(): Int = when (this) {
    MediaCreditRole.Author -> R.string.metadata_credits_authors
    MediaCreditRole.Director -> R.string.metadata_credits_directors
    MediaCreditRole.Creator -> R.string.metadata_credits_creators
    MediaCreditRole.Studio -> R.string.metadata_credits_studios
    MediaCreditRole.Developer -> R.string.metadata_credits_developers
    MediaCreditRole.Publisher -> R.string.metadata_credits_publishers
    MediaCreditRole.Cast -> R.string.metadata_credits_cast
    MediaCreditRole.VoiceActor -> R.string.metadata_credits_voice_actors
}
