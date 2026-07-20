package com.nilpo.contenttracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun SectionHeader(
    section: MediaSection,
    onAddClick: () -> Unit,
) {
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
            color = section.themedAccent(),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.clickable(onClick = onAddClick),
        )
    }
}
