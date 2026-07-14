package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveProgress
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ObjectivePresentationTest {
    @Test
    fun progressTitleUsesTheObjectiveUnitInsteadOfTheMediaTitle() {
        assertEquals(
            "7000 p\u00e0gines",
            objectiveDisplayTitle(
                metric = ObjectiveMetric.ProgressUnits,
                mediaType = MediaType.Book,
                targetValue = 7000,
                unit = ObjectiveUnit.Pages,
            ),
        )
    }

    @Test
    fun presentationIgnoresAStalePersistedName() {
        val objective = Objective(
            name = "7000 llibres registrats",
            metric = ObjectiveMetric.ProgressUnits,
            unit = ObjectiveUnit.Pages,
            mediaType = MediaType.Book,
            targetValue = 7000,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
        )

        assertEquals("7000 p\u00e0gines", objectivePresentation(objective).title)
    }

    @Test
    fun completedTitleCopyMatchesTheTitlesMetric() {
        assertEquals(
            "1 llibre completat",
            objectiveDisplayTitle(
                metric = ObjectiveMetric.CompletedTitles,
                mediaType = MediaType.Book,
                targetValue = 1,
                unit = ObjectiveUnit.Titles,
            ),
        )
    }
    @Test
    fun progressLabelDistinguishesIncompleteProgressFromTheTarget() {
        val objective = objective(targetValue = 8, metric = ObjectiveMetric.CompletedTitles)
        assertEquals("6 de 8 llibres", objectiveProgressLabel(ObjectiveProgress(objective, 6)))
    }

    @Test
    fun progressLabelRemainsExplicitWhenTheTargetIsMet() {
        val objective = objective(targetValue = 8, metric = ObjectiveMetric.CompletedTitles)
        assertEquals("8 de 8 llibres", objectiveProgressLabel(ObjectiveProgress(objective, 8)))
    }

    @Test
    fun progressLabelShowsValuesAboveTheTarget() {
        val objective = objective(targetValue = 8, metric = ObjectiveMetric.CompletedTitles)
        assertEquals("12 de 8 llibres", objectiveProgressLabel(ObjectiveProgress(objective, 12)))
    }

    @Test
    fun progressUnitLabelUsesTheObjectiveUnit() {
        val objective = objective(targetValue = 7000, metric = ObjectiveMetric.ProgressUnits, unit = ObjectiveUnit.Pages)
        assertEquals("3842 de 7000 p\u00e0gines", objectiveProgressLabel(ObjectiveProgress(objective, 3842)))
    }

    @Test
    fun objectiveMediaLabelsUseNaturalCatalanPluralForms() {
        assertEquals("Pel·lícules", objectiveMediaLabelFor(MediaType.Movie))
        assertEquals("Sèries", objectiveMediaLabelFor(MediaType.TvShow))
    }

    private fun objective(
        targetValue: Int,
        metric: ObjectiveMetric,
        unit: ObjectiveUnit = ObjectiveUnit.Titles,
    ) = Objective(
        name = "Test",
        metric = metric,
        unit = unit,
        mediaType = MediaType.Book,
        targetValue = targetValue,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 12, 31),
    )

}
