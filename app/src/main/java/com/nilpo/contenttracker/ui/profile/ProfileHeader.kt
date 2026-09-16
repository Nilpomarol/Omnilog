package com.nilpo.contenttracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.common.ObjectiveMediaIcon
import com.nilpo.contenttracker.ui.common.objectiveAccent
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.detail.DetailSectionTitle
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

/** The portrait takes a cover's shape and size, so the profile opens like every other editorial page. */
internal val PortraitShape = RoundedCornerShape(6.dp)

/**
 * Who you are, laid out like the head of every editorial page: your picture in a cover's shape where
 * an item's cover sits, then an overline in your colour, your name in serif, the line you wrote about yourself and
 * the library in one line of facts.
 */
@Composable
fun ProfileHeader(
    displayName: String,
    bio: String,
    accent: Color,
    image: Any?,
    totalTitles: Int,
    activeTitles: Int,
    averageRating: Double?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailGutter),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProfileAvatar(
            displayName = displayName,
            color = accent,
            image = image,
            shape = PortraitShape,
            modifier = Modifier
                .size(width = 128.dp, height = 192.dp)
                .shadow(elevation = 12.dp, shape = PortraitShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "PERFIL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = accent,
            )
            BasicText(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 1.22.em,
                    color = OmnilogTheme.colors.appInk,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 26.sp, stepSize = 1.sp),
            )
            if (bio.isNotBlank()) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = listOfNotNull(
                    "$totalTitles ${if (totalTitles == 1) "títol" else "títols"} a la biblioteca",
                    "$activeTitles en curs".takeIf { activeTitles > 0 },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
            averageRating?.let { rating ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accent,
                    )
                    Text(
                        text = stringResource(R.string.collection_average_short, rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmnilogTheme.colors.appInk,
                    )
                }
            }
            // A padlock rather than the words "Perfil local": the icon carries the meaning faster
            // than a label, which leaves the sentence free to be a reassurance.
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = OmnilogTheme.colors.appMuted,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = stringResource(R.string.profile_local_storage_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
    }
}

/**
 * What you have finished, and only that. The total leads beside the heading; under it every format
 * keeps its own column — the count over its mark and name — so the five always sit on one line in the same
 * order, zeros included. Everything under this heading
 * counts titles finished at least once, so none of it can be read as the size of the library.
 */
@Composable
fun ProfileCompletedSummary(
    completedByType: List<Pair<MediaType, Int>>,
    modifier: Modifier = Modifier,
) {
    val total = completedByType.sumOf { it.second }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = DetailGutter),
            color = OmnilogTheme.colors.appLine,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DetailGutter, end = DetailGutter, top = 20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DetailSectionTitle(text = "Acabats")
                Text(
                    text = "Títols que has acabat almenys una vegada",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                color = OmnilogTheme.colors.appInk,
            )
        }

        FormatCountStrip(
            counts = completedByType,
            modifier = Modifier.padding(start = DetailGutter, end = DetailGutter, top = 20.dp),
        )
    }
}

/**
 * One strip of figures split by hairlines: each format's count in serif and in its colour, over its
 * mark and name. Every format keeps its column, zeros included, so the order never shifts.
 */
@Composable
internal fun FormatCountStrip(
    counts: List<Pair<MediaType, Int>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        counts.forEachIndexed { index, (type, count) ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(0.7f),
                    color = OmnilogTheme.colors.appLine,
                )
            }
            val color = if (count > 0) type.objectiveAccent() else OmnilogTheme.colors.appMuted.copy(alpha = 0.5f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = color,
                    maxLines = 1,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ObjectiveMediaIcon(mediaType = type, accent = color, size = 14.dp)
                    Text(
                        text = type.profileLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmnilogTheme.colors.appInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The accent choice. The chosen colour opens into a wide pill carrying a tick and its name, the others
 * stay small circles, so which one is picked is unmistakable at a glance; the sheet around it takes
 * the colour too.
 */
@Composable
internal fun AccentSwatches(
    options: List<Pair<Color, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, (color, name) ->
            val selected = index == selectedIndex
            val width by animateDpAsState(if (selected) 124.dp else 44.dp, label = "swatchWidth")
            Row(
                modifier = Modifier
                    .width(width)
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
                    .clickable { onSelect(index) }
                    .semantics { contentDescription = name },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = OmnilogTheme.colors.appBackground,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = name,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appBackground,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

private fun MediaType.profileLabel(): String = when (this) {
    MediaType.Anime -> "Anime"
    MediaType.Book -> "Llibres"
    MediaType.Movie -> "Pel·lícules"
    MediaType.TvShow -> "Sèries"
    MediaType.Game -> "Jocs"
}
