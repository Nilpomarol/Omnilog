package com.nilpo.contenttracker.ui.record

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.detail.DetailGutter
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

enum class RecordTab(val labelRes: Int) {
    Timeline(R.string.timeline_title),
    Stats(R.string.stats_title),
}

/**
 * Cronologia and Estadístiques as two tabs of one page: the same history, read as a diary or as
 * figures. The pages swipe; [selectedTab] is hoisted so the top bar can show the tab's own actions.
 */
@Composable
fun RecordScreen(
    selectedTab: RecordTab,
    onTabSelected: (RecordTab) -> Unit,
    timeline: @Composable () -> Unit,
    stats: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { RecordTab.entries.size }
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentOnTabSelected(RecordTab.entries[it]) }
    }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) pagerState.animateScrollToPage(selectedTab.ordinal)
    }

    Column(modifier = modifier.background(OmnilogTheme.colors.appBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DetailGutter)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RecordTab.entries.forEach { tab ->
                RecordTabLabel(
                    text = stringResource(tab.labelRes),
                    selected = pagerState.currentPage == tab.ordinal,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
        HorizontalDivider(color = OmnilogTheme.colors.appLine)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { RecordTab.entries[it].name },
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (RecordTab.entries[page]) {
                    RecordTab.Timeline -> timeline()
                    RecordTab.Stats -> stats()
                }
            }
        }
    }
}

/** A word with a rule under it when chosen: the page's type does the work, not a Material tab bar. */
@Composable
private fun RecordTabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    val textColor by animateColorAsState(
        if (selected) OmnilogTheme.colors.appInk else OmnilogTheme.colors.appMuted,
        label = "recordTabText",
    )
    val ruleColor by animateColorAsState(
        if (selected) OmnilogTheme.accents.Dashboard else OmnilogTheme.colors.appBackground.copy(alpha = 0f),
        label = "recordTabRule",
    )
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(top = 8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(bottom = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ruleColor),
        )
    }
}
