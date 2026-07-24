package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

/**
 * The detail page's side gutter.
 *
 * It belongs to the items rather than to the page's `LazyColumn`, so anything that should reach the
 * screen edge — the backdrop, the cover carousels — can simply decline it.
 */
val DetailGutter = 24.dp

@Composable
fun DetailSectionTitle(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
        )
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
    }
}
