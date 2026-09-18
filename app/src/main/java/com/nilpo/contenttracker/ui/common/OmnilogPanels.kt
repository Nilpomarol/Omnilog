package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily

/** Inner padding of an [OmnilogPanelGroup]'s rows. */
val OmnilogPanelPadding = 16.dp

/** A serif page heading with at most one muted line under it, as Configuració opens its chapters. */
@Composable
fun OmnilogChapterTitle(title: String, note: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Normal,
            ),
            color = OmnilogTheme.colors.appInk,
        )
        note?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = OmnilogTheme.colors.appMuted)
        }
    }
}

/**
 * Small capitals over one soft panel, the grouping Configuració uses. [labelGutter] aligns the label
 * with the page's text; the panel itself sits [panelMargin] from the edge.
 */
@Composable
fun OmnilogPanelGroup(
    label: String,
    modifier: Modifier = Modifier,
    labelGutter: Dp = 20.dp,
    panelMargin: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(OmnilogLocale),
            modifier = Modifier
                .padding(horizontal = labelGutter)
                .semantics { heading() },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = OmnilogTheme.colors.appMuted,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = panelMargin)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(OmnilogTheme.colors.appPanel),
            content = content,
        )
    }
}

/** A panel row that opens something: a mark, title and description, and a chevron. */
@Composable
fun OmnilogPanelRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = OmnilogPanelPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = OmnilogTheme.colors.appInk)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = OmnilogTheme.colors.appMuted)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = OmnilogTheme.colors.appMuted,
        )
    }
}

@Composable
fun OmnilogPanelDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = OmnilogPanelPadding), color = OmnilogTheme.colors.appLine)
}
