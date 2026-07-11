package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.theme.OmnilogColors

@Composable
fun OwnedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = OmnilogColors.AppPanel.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
        contentColor = OmnilogColors.Dashboard,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_owned_badge),
            contentDescription = stringResource(R.string.owned_label),
            modifier = Modifier
                .padding(5.dp)
                .size(14.dp),
        )
    }
}
