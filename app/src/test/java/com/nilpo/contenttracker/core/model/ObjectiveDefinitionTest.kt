package com.nilpo.contenttracker.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ObjectiveDefinitionTest {
    @Test
    fun completedObjectivesAlwaysUseTitleUnitsAndAllowAllFormats() {
        val definition = ObjectiveDefinition(ObjectiveMetric.CompletedTitles, mediaType = null)

        assertEquals(ObjectiveUnit.Titles, definition.canonicalUnit)
        assertTrue(definition.isValid)
    }

    @Test
    fun progressObjectivesRequireAConcreteMediaType() {
        val definition = ObjectiveDefinition(ObjectiveMetric.ProgressUnits, mediaType = null)

        assertEquals(null, definition.canonicalUnit)
        assertFalse(definition.isValid)
    }

    @Test
    fun progressUnitsMatchTheSelectedMediaType() {
        val expectedUnits = mapOf(
            MediaType.Book to ObjectiveUnit.Pages,
            MediaType.Anime to ObjectiveUnit.Episodes,
            MediaType.TvShow to ObjectiveUnit.Episodes,
            MediaType.Movie to ObjectiveUnit.Minutes,
            MediaType.Game to ObjectiveUnit.Hours,
        )

        expectedUnits.forEach { (mediaType, expectedUnit) ->
            assertEquals(
                expectedUnit,
                ObjectiveDefinition(ObjectiveMetric.ProgressUnits, mediaType).canonicalUnit,
            )
        }
    }

    @Test
    fun canonicalUnitRepairsLegacyStoredUnitsForDisplay() {
        val completed = Objective(
            name = "7000 llibres registrats",
            metric = ObjectiveMetric.CompletedTitles,
            unit = ObjectiveUnit.Pages,
            mediaType = MediaType.Book,
            targetValue = 7000,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
        )
        val progress = completed.copy(
            name = "7000 llibres registrats",
            metric = ObjectiveMetric.ProgressUnits,
            unit = ObjectiveUnit.Titles,
        )

        assertEquals(ObjectiveUnit.Titles, completed.canonicalUnit())
        assertEquals(ObjectiveUnit.Pages, progress.canonicalUnit())
    }
}
