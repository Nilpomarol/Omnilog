package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectiveSentenceTest {

    @Test
    fun everyUnitOptionInThePickerIsAValidDefinition() {
        val options = objectiveFormatOptions().flatMap { objectiveCountOptions(it) }

        // The whole point of fusing metric and format into one choice: the picker cannot offer a
        // combination the calculator would not know how to measure.
        options.forEach { option ->
            if (option.metric == ObjectiveMetric.ProgressUnits) {
                assertTrue(
                    "progress units need a concrete format: $option",
                    option.mediaType != null,
                )
            }
        }
    }

    @Test
    fun everyFormatOffersAtLeastOneWayToCountIt() {
        objectiveFormatOptions().forEach { format ->
            assertTrue("no count options for $format", objectiveCountOptions(format).isNotEmpty())
        }
        // "Tot" is the only format with a single option, which is why its row is hidden.
        assertEquals(1, objectiveCountOptions(null).size)
    }

    @Test
    fun countLabelsAreUnambiguousWithinTheirFormatRow() {
        // Series and anime episodes both read plain "episodis", which is fine precisely because
        // they can never appear in the same row — the format row above has already been chosen.
        objectiveFormatOptions().forEach { format ->
            val labels = objectiveCountOptions(format).map { objectiveCountLabel(it) }
            assertEquals("duplicate labels for $format", labels.size, labels.toSet().size)
        }
        assertEquals(
            "episodis",
            objectiveCountLabel(ObjectiveUnitOption(ObjectiveMetric.ProgressUnits, MediaType.TvShow)),
        )
    }

    @Test
    fun changingFormatKeepsTheWayOfCountingWhenItStillApplies() {
        // Switching from pages to series should land on episodes, not fall back to completed titles.
        assertEquals(
            ObjectiveUnitOption(ObjectiveMetric.ProgressUnits, MediaType.TvShow),
            objectiveOptionForFormat(MediaType.TvShow, ObjectiveMetric.ProgressUnits),
        )
        // "Tot" has no progress unit, so it has to fall back rather than produce an invalid pair.
        assertEquals(
            ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, null),
            objectiveOptionForFormat(null, ObjectiveMetric.ProgressUnits),
        )
    }

    @Test
    fun theAmbiguousEpisodeUnitIsDisambiguatedByFormat() {
        val series = ObjectiveUnitOption(ObjectiveMetric.ProgressUnits, MediaType.TvShow)
        val anime = ObjectiveUnitOption(ObjectiveMetric.ProgressUnits, MediaType.Anime)

        // Both canonicalise to Episodes, so the words have to carry the difference.
        assertEquals(ObjectiveUnit.Episodes, series.unit)
        assertEquals(ObjectiveUnit.Episodes, anime.unit)
        assertEquals("200 episodis de sèries", objectiveSentenceAmount(series, 200))
        assertEquals("200 episodis d'anime", objectiveSentenceAmount(anime, 200))
    }

    @Test
    fun unambiguousUnitsDoNotRepeatTheirFormat() {
        val pages = ObjectiveUnitOption(ObjectiveMetric.ProgressUnits, MediaType.Book)
        assertEquals("3.000 pàgines", objectiveSentenceAmount(pages, 3_000))

        val books = ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, MediaType.Book)
        assertEquals("40 llibres", objectiveSentenceAmount(books, 40))
    }

    @Test
    fun sentenceAmountUsesSingularFormsForOne() {
        assertEquals(
            "1 llibre",
            objectiveSentenceAmount(ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, MediaType.Book), 1),
        )
        assertEquals(
            "1 pel·lícula",
            objectiveSentenceAmount(ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, MediaType.Movie), 1),
        )
    }

    @Test
    fun verbFollowsTheFormatSoItNeverNeedsAskingFor() {
        assertEquals("llegir", objectiveVerb(MediaType.Book))
        assertEquals("veure", objectiveVerb(MediaType.TvShow))
        assertEquals("jugar", objectiveVerb(MediaType.Game))
        assertEquals("acabar", objectiveVerb(null))
    }

    @Test
    fun theCollapsedChipStaysUnambiguousWithNoPickerOpen() {
        // The sentence chip has no format row above it, so unlike the picker rows it must carry
        // the disambiguator itself.
        val chips = objectiveFormatOptions()
            .flatMap { objectiveCountOptions(it) }
            .map { objectiveSentenceUnitWords(it, 2) }
        assertEquals(chips.size, chips.toSet().size)
    }

    @Test
    fun amountStepsAndQuickPicksFollowTheUnitsGranularity() {
        assertEquals(1, objectiveAmountStep(ObjectiveUnit.Titles))
        assertEquals(100, objectiveAmountStep(ObjectiveUnit.Pages))
        assertEquals(60, objectiveAmountStep(ObjectiveUnit.Minutes))

        assertTrue(objectiveQuickPicks(ObjectiveUnit.Pages).all { it >= 1_000 })
        assertTrue(objectiveQuickPicks(ObjectiveUnit.Titles).all { it <= 52 })
    }

    @Test
    fun largeNumbersAreGroupedSoTheyStayLegible() {
        assertEquals("3.000", formatObjectiveNumber(3_000))
        assertEquals("40", formatObjectiveNumber(40))
    }
}
