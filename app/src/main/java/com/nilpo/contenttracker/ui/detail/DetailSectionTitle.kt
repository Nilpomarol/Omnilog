package com.nilpo.contenttracker.ui.detail

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

/**
 * The one heading every detail section carries: type alone, with no rule under it. Sections are
 * separated by whitespace, the way Home's are, rather than boxed off from each other.
 */
@Composable
fun DetailSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = OmnilogTheme.colors.appInk,
        modifier = modifier,
    )
}
