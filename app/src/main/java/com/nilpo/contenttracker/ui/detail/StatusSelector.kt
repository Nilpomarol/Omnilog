package com.nilpo.contenttracker.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.common.OptionSelector

@Composable
fun StatusSelector(
    selectedStatus: TrackingStatus,
    onStatusSelected: (TrackingStatus) -> Unit,
) {
    OptionSelector(
        label = stringResource(R.string.field_status),
        options = TrackingStatus.entries,
        selectedOption = selectedStatus,
        optionLabel = { it.label() },
        onOptionSelected = onStatusSelected,
    )
}

@Composable
private fun TrackingStatus.label(): String {
    return when (this) {
        TrackingStatus.Planned -> stringResource(R.string.status_planned)
        TrackingStatus.InProgress -> stringResource(R.string.status_in_progress)
        TrackingStatus.Completed -> stringResource(R.string.status_completed)
        TrackingStatus.Paused -> stringResource(R.string.status_paused)
        TrackingStatus.Dropped -> stringResource(R.string.status_dropped)
    }
}
