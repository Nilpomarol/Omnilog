package com.nilpo.contenttracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

/**
 * The detail page's side gutter.
 *
 * It belongs to the items rather than to the page's `LazyColumn`, so anything that should reach the
 * screen edge — the hairlines' neighbours, the cover carousels — can simply decline it.
 */
val DetailGutter = 24.dp

/**
 * The one heading every detail section carries: serif type alone, with no rule under it. Sections are
 * separated by whitespace and hairlines rather than boxed off from each other.
 */
@Composable
fun DetailSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Normal,
        ),
        color = OmnilogTheme.colors.appInk,
        modifier = modifier,
    )
}

/**
 * A lower-frequency part of the page folded behind one line: a mark, its name, a summary of what is
 * inside, and a chevron that turns as it opens.
 *
 * The page stays short without anything moving to another screen — the reference facts, the credits
 * and the past sessions are all still one tap away, in place.
 */
@Composable
fun DetailDisclosureRow(
    icon: Painter,
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "disclosureChevron",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .heightIn(min = 56.dp)
                .padding(horizontal = DetailGutter),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifFontFamily),
                color = OmnilogTheme.colors.appInk,
                maxLines = 1,
            )
            Text(
                text = summary.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OmnilogTheme.colors.appMuted,
                modifier = Modifier.rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)) {
                content()
            }
        }
    }
}
