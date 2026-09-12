package com.nilpo.contenttracker.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.ContentTrackerTheme
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@Preview(name = "Display toggle - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Display toggle - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DisplayModeTogglePreview() {
    ContentTrackerTheme {
        Surface(color = OmnilogTheme.colors.appBackground) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val accent = OmnilogTheme.accents.Dashboard
                DisplayModeToggle(HomeDisplayMode.List, accent, onModeSelected = {})
                DisplayModeToggle(HomeDisplayMode.Grid, accent, onModeSelected = {})
                DisplayModeToggle(HomeDisplayMode.Grid, accent, onModeSelected = {}, enabled = false)
            }
        }
    }
}
