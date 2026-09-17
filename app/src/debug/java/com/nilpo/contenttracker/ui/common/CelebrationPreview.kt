package com.nilpo.contenttracker.ui.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.ui.theme.ContentTrackerTheme
import java.time.LocalDate

private val booksGoal = Objective(
    id = 1,
    name = "Llibres del 2026",
    metric = ObjectiveMetric.CompletedTitles,
    unit = ObjectiveUnit.Titles,
    mediaType = MediaType.Book,
    targetValue = 20,
    startDate = LocalDate.of(2026, 1, 1),
    endDate = LocalDate.of(2026, 12, 31),
)

@Preview(name = "Completion reaching a goal", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 380, heightDp = 760)
@Composable
private fun CompletionReachingGoalPreview() {
    ContentTrackerTheme {
        CompletionCelebration(
            reaction = CompletionReaction(
                title = "El mestre i Margarida",
                coverUrl = null,
                daysTaken = 12,
                visitNumber = 2,
                steps = listOf(ObjectiveStep(ObjectiveProgress(booksGoal, currentValue = 20), previousValue = 19)),
            ),
            onContinue = {},
            onUndo = {},
        )
    }
}

@Preview(name = "Goal reached", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 380, heightDp = 760)
@Composable
private fun ObjectiveReachedPreview() {
    val pages = booksGoal.copy(name = "5.000 pàgines", metric = ObjectiveMetric.ProgressUnits, unit = ObjectiveUnit.Pages, targetValue = 5_000)
    ContentTrackerTheme {
        ObjectiveCelebration(
            step = ObjectiveStep(ObjectiveProgress(pages, currentValue = 5_012), previousValue = 4_960),
            onContinue = {},
            onUndo = null,
            today = LocalDate.of(2026, 9, 16),
        )
    }
}
