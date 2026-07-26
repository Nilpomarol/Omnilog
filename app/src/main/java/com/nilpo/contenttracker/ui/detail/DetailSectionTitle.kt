package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * The heading the detail page's own sections carry: a short accent tick, then the title.
 *
 * The tick is the media type's colour, so «Dades de l'element» and «Valoracions» read as belonging
 * to this particular work at a glance and the section titles stop being undifferentiated bold text.
 * It replaces the hairline that used to run out to the right margin — a rule doing the job the 20dp
 * of background between sections already does.
 */
@Composable
fun DetailSectionHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogTheme.colors.appInk,
        )
    }
}
