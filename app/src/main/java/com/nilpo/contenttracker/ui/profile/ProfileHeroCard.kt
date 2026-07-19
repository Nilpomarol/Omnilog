package com.nilpo.contenttracker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.stats.statsColor
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

private val BannerHeight = 186.dp
private val AvatarSize = 84.dp
// How far the avatar hangs past the banner, and therefore how much top padding the content below
// has to reserve to clear it.
private val AvatarOverhang = AvatarSize / 2
private const val BannerCovers = 8

/**
 * The profile's one identity card: who you are, and what your library adds up to.
 *
 * It absorbs what used to be three separate cards — the hero, "La teva biblioteca", and "La teva
 * col·lecció". Those three restated the same total twice (`Títols` is by definition the sum of the
 * per-type counts) and spread one idea across three panels.
 *
 * The banner is your own cover art, dimmed far enough that the name and avatar stay legible; it is
 * decoration drawn from real data rather than an ornament. Falls back to a plain accent wash when
 * there are not enough covers to look deliberate.
 */
@Composable
fun ProfileHeroCard(
    displayName: String,
    bio: String,
    avatarColor: Color,
    imagePath: String?,
    bannerItems: List<TrackedMedia>,
    totalTitles: Int,
    completedTitles: Int,
    activeTitles: Int,
    averageRating: Double?,
    mediaTypeCounts: List<Pair<MediaType, Int>>,
    editing: Boolean = false,
    accentOptions: List<Color> = emptyList(),
    selectedAccentIndex: Int = 0,
    onNameChange: (String) -> Unit = {},
    onBioChange: (String) -> Unit = {},
    onAccentChange: (Int) -> Unit = {},
    onPhotoClick: () -> Unit = {},
) {
    // No surface, no border, no corners: the banner runs to the screen edges and the content sits
    // straight on the page. A panel here would have been a card inside a scroll of cards, and the
    // identity block is the page's header rather than one more item in it.
    // The banner is drawn first and the content laid over it, rather than stacked after it in a
    // Column. That is what lets the identity row ride up onto the collage: an `offset` would have
    // moved the row without shrinking the space it reserved, leaving a hole beneath the card.
    Box(modifier = Modifier.fillMaxWidth()) {
        CoverBanner(items = bannerItems, accent = avatarColor)

        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                // Pulls the row up so the avatar straddles the banner's lower edge; the text beside
                // it comes along, which is the point — the two read as one line of identity.
                top = BannerHeight - AvatarOverhang,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    ProfileAvatar(
                        displayName = displayName,
                        color = avatarColor,
                        imagePath = imagePath,
                        modifier = Modifier
                            .size(AvatarSize)
                            .then(
                                if (editing) Modifier.clickable(onClick = onPhotoClick)
                                else Modifier
                            ),
                    )
                    if (editing) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clickable(onClick = onPhotoClick),
                            shape = CircleShape,
                            color = OmnilogTheme.colors.appPanel,
                            border = BorderStroke(1.dp, avatarColor),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Canvia la imatge",
                                    tint = avatarColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Editing happens on the hero itself rather than in a form below it. There is
                    // no preview to build because the thing being edited is already the thing you
                    // are looking at — and the old form sat off-screen under the fold, so tapping
                    // "edit" left you looking at a page that had not visibly changed.
                    if (editing) {
                        HeroField(
                            value = displayName,
                            onValueChange = onNameChange,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = OmnilogTheme.colors.appInk,
                            ),
                            placeholder = "El teu nom",
                            accent = avatarColor,
                            singleLine = true,
                        )
                        HeroField(
                            value = bio,
                            onValueChange = onBioChange,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = OmnilogTheme.colors.appMuted,
                            ),
                            placeholder = "Una descripció curta",
                            accent = OmnilogTheme.colors.appLine,
                            singleLine = false,
                        )
                    } else {
                        // The name is the largest thing on the page and is allowed to wrap: a long
                        // one ellipsised on a single line was the one piece of text here that is
                        // genuinely personal, so it gets the room the genre chips used to take.
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OmnilogTheme.colors.appInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmnilogTheme.colors.appMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // A padlock rather than the words "Perfil local": the icon carries the
                        // meaning faster than a label, which leaves the sentence free to be a
                        // reassurance instead of a description.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = OmnilogColors.Dashboard,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = stringResource(R.string.profile_local_storage_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogColors.Dashboard,
                            )
                        }
                    }
                }
            }

            if (editing && accentOptions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accentOptions.forEachIndexed { index, color ->
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { onAccentChange(index) },
                            shape = CircleShape,
                            color = color,
                            border = if (index == selectedAccentIndex) {
                                BorderStroke(3.dp, OmnilogTheme.colors.appInk)
                            } else {
                                null
                            },
                        ) {}
                    }
                }
            }

            LibraryCard(
                totalTitles = totalTitles,
                completedTitles = completedTitles,
                activeTitles = activeTitles,
                averageRating = averageRating,
                mediaTypeCounts = mediaTypeCounts,
            )
        }
    }
}

/**
 * Cover art across the top of the profile.
 *
 * The covers are heavily dimmed on purpose: they are texture, not content. Anything legible enough
 * to read would compete with the name that rides up onto the banner's lower edge.
 *
 * That overlap is why the bottom third carries a scrim fading to the page background. Without it
 * the name would be white type over whatever cover art happened to land there — legible against a
 * dark poster, invisible against a pale one.
 */
@Composable
private fun CoverBanner(
    items: List<TrackedMedia>,
    accent: Color,
) {
    val covers = items.mapNotNull { it.item.coverUrl }.take(BannerCovers)
    val background = OmnilogTheme.colors.appBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BannerHeight)
            .background(accent.copy(alpha = 0.10f)),
    ) {
        // Below a few covers the strip reads as a broken layout rather than a collage, so a
        // plain accent wash stands in until the library is big enough to fill it.
        if (covers.size >= 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .alpha(0.55f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                covers.forEach { coverUrl ->
                    MetadataCoverImage(
                        coverUrl = coverUrl,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(0.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BannerHeight * 0.46f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, background.copy(alpha = 0.72f), background),
                    ),
                ),
        )
    }
}

/**
 * A text field that looks exactly like the text it replaces, marked as editable only by the rule
 * beneath it. A boxed `OutlinedTextField` here would have changed the hero's shape the moment you
 * started editing, which defeats the point of editing in place.
 */
@Composable
private fun HeroField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    placeholder: String,
    accent: Color,
    singleLine: Boolean,
) {
    Box {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = accent,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
                .padding(bottom = 4.dp),
            textStyle = textStyle,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 2,
            cursorBrush = SolidColor(accent),
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = textStyle,
                color = OmnilogTheme.colors.appMuted.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OmnilogTheme.colors.appLine),
    )
}

@Composable
private fun HeroMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.Dashboard,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
            maxLines = 1,
        )
    }
}

/**
 * One column per media type: its navigation icon, the count, the name.
 *
 * The breakdown is deliberately not a chart. Every entry labels itself, so there is no legend. What
 * it gives up is proportion — which matters less here than knowing the actual counts.
 *
 * This is the one panel in an otherwise seamless header: the identity block above it is page
 * furniture, while these numbers are data, and the panel is what says so. The totals and the
 * per-type breakdown share it because they are the same fact at two resolutions — `Títols` is by
 * definition the sum of the row beneath it — and splitting them across two panels implied they were
 * separate things.
 */
@Composable
private fun LibraryCard(
    totalTitles: Int,
    completedTitles: Int,
    activeTitles: Int,
    averageRating: Double?,
    mediaTypeCounts: List<Pair<MediaType, Int>>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OmnilogTheme.colors.appPanel,
        border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                HeroMetric(totalTitles.toString(), "Títols", Modifier.weight(1f))
                HeroMetric(completedTitles.toString(), "Completats", Modifier.weight(1f))
                HeroMetric(activeTitles.toString(), "En curs", Modifier.weight(1f))
                HeroMetric(
                    averageRating?.let { "%.1f".format(it) } ?: "—",
                    "Nota",
                    Modifier.weight(1f),
                )
            }

            if (mediaTypeCounts.isNotEmpty()) {
                HeroDivider()

                Row(modifier = Modifier.fillMaxWidth()) {
                    mediaTypeCounts.forEach { (type, count) ->
                        val accent = type.statsColor()
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            ObjectiveMediaIcon(mediaType = type, accent = accent, size = 26.dp)
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                maxLines = 1,
                            )
                            Text(
                                text = type.heroLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = OmnilogTheme.colors.appMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun MediaType.heroLabel(): String = when (this) {
    MediaType.Anime -> "Anime"
    MediaType.Book -> "Llibres"
    MediaType.Movie -> "Pel·lícules"
    MediaType.TvShow -> "Sèries"
    MediaType.Game -> "Jocs"
}

// Colours come from Stats' own mapping rather than a copy of it, so the two pages cannot drift —
// notably films and series, which Stats separates into teal and blue where the rest of the app
// treats them as one "Cinema i TV" group.
