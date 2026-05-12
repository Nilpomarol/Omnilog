package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun SectionHeader(section: MediaSection) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(section.titleResId),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "+",
            color = section.accent,
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}
