package com.nilpo.contenttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaCredit
import com.nilpo.contenttracker.core.model.MediaItem
import com.nilpo.contenttracker.ui.common.MediaMetadataSecondary
import com.nilpo.contenttracker.ui.common.toMediaMetadataUi
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@Composable
fun ItemDetailsSection(
    item: MediaItem,
    credits: List<MediaCredit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_item_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogTheme.colors.appInk,
            )
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)

        ItemDetailsSummary(
            item = item,
            credits = credits,
        )
    }
}

@Composable
private fun ItemDetailsSummary(
    item: MediaItem,
    credits: List<MediaCredit>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MediaMetadataSecondary(metadata = item.toMediaMetadataUi(credits))
    }
}
