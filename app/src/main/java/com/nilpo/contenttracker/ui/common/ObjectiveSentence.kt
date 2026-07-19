package com.nilpo.contenttracker.ui.common

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.ObjectiveMetric
import com.nilpo.contenttracker.core.model.ObjectiveUnit
import com.nilpo.contenttracker.core.model.progressObjectiveUnit
import java.util.Locale

/**
 * Grammar for the sentence-shaped objective editor: `Vull llegir 40 llibres durant aquest any`.
 *
 * The editor asks one question where the old form asked two ("què vols mesurar" plus "format"),
 * because a metric without a media type is not a thing you can measure. [ObjectiveUnitOption] is
 * that single answer, so an invalid definition is unrepresentable rather than something the form
 * has to warn about after the fact.
 */
data class ObjectiveUnitOption(
    val metric: ObjectiveMetric,
    val mediaType: MediaType?,
) {
    val unit: ObjectiveUnit
        get() = when (metric) {
            ObjectiveMetric.CompletedTitles -> ObjectiveUnit.Titles
            // Every ProgressUnits option below is built with a non-null media type.
            ObjectiveMetric.ProgressUnits -> mediaType?.progressObjectiveUnit() ?: ObjectiveUnit.Titles
        }
}

/**
 * The picker's first row. `null` is "Tot" — every format at once, which only completed titles can
 * express, since pages and episodes across different media cannot be added together.
 */
fun objectiveFormatOptions(): List<MediaType?> = listOf(
    MediaType.Book,
    MediaType.TvShow,
    MediaType.Anime,
    MediaType.Movie,
    MediaType.Game,
    null,
)

fun objectiveFormatChipLabel(mediaType: MediaType?): String = when (mediaType) {
    MediaType.Book -> "Llibres"
    MediaType.TvShow -> "Sèries"
    MediaType.Anime -> "Anime"
    MediaType.Movie -> "Pel·lícules"
    MediaType.Game -> "Jocs"
    null -> "Tot"
}

/**
 * The picker's second row: the ways the chosen format can be counted.
 *
 * Splitting the choice in two is what keeps the panel short, and it also removes the reason the
 * options ever had to be grouped — `episodis` appears under Sèries and under Anime, but never in
 * the same row, so the word alone is unambiguous here.
 */
fun objectiveCountOptions(mediaType: MediaType?): List<ObjectiveUnitOption> = when (mediaType) {
    null -> listOf(ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, null))
    else -> listOf(
        ObjectiveUnitOption(ObjectiveMetric.CompletedTitles, mediaType),
        ObjectiveUnitOption(ObjectiveMetric.ProgressUnits, mediaType),
    )
}

/** Label for a second-row chip. The format row above supplies the context, so it is not repeated. */
fun objectiveCountLabel(option: ObjectiveUnitOption): String = when (option.metric) {
    ObjectiveMetric.CompletedTitles -> when (option.mediaType) {
        MediaType.Book -> "llibres acabats"
        MediaType.TvShow -> "sèries acabades"
        MediaType.Anime -> "animes acabats"
        MediaType.Movie -> "pel·lícules acabades"
        MediaType.Game -> "jocs acabats"
        null -> "títols acabats"
    }
    ObjectiveMetric.ProgressUnits -> when (option.mediaType) {
        MediaType.Book -> "pàgines"
        MediaType.TvShow, MediaType.Anime -> "episodis"
        MediaType.Movie -> "minuts"
        MediaType.Game -> "hores"
        null -> "unitats"
    }
}

/**
 * The option to land on when the format changes, keeping the way of counting where possible.
 *
 * Switching from `pàgines` to Sèries should give `episodis`, not silently drop back to completed
 * titles. "Tot" is the exception — it has no progress unit, so the metric has to fall back.
 */
fun objectiveOptionForFormat(
    mediaType: MediaType?,
    preferredMetric: ObjectiveMetric,
): ObjectiveUnitOption {
    val options = objectiveCountOptions(mediaType)
    return options.firstOrNull { it.metric == preferredMetric } ?: options.first()
}

/**
 * The verb that opens the sentence. Derived from the format rather than chosen separately: picking
 * `pàgines` already tells us the sentence reads "llegir", so there is nothing left to ask.
 */
fun objectiveVerb(mediaType: MediaType?): String = when (mediaType) {
    MediaType.Book -> "llegir"
    MediaType.Anime, MediaType.TvShow, MediaType.Movie -> "veure"
    MediaType.Game -> "jugar"
    null -> "acabar"
}

/**
 * The `40 llibres` / `200 episodis de sèries` fragment.
 *
 * The trailing `de …` only appears where the unit word alone is ambiguous. Everywhere else the unit
 * already names its format, and repeating it ("40 llibres de llibres") would read as noise.
 */
fun objectiveSentenceAmount(option: ObjectiveUnitOption, value: Int): String =
    "${formatObjectiveNumber(value)} ${objectiveSentenceUnitWords(option, value)}"

/** Just the unit words, for the editor where the amount sits in its own separately tappable chip. */
fun objectiveSentenceUnitWords(option: ObjectiveUnitOption, value: Int): String =
    when (option.metric) {
        ObjectiveMetric.CompletedTitles -> completedSentenceUnit(option.mediaType, value)
        ObjectiveMetric.ProgressUnits -> {
            val base = objectiveUnitLabel(option.unit, value)
            when (option.mediaType) {
                MediaType.TvShow -> "$base de sèries"
                MediaType.Anime -> "$base d'anime"
                else -> base
            }
        }
    }

private fun completedSentenceUnit(mediaType: MediaType?, value: Int): String = when (mediaType) {
    MediaType.Book -> if (value == 1) "llibre" else "llibres"
    MediaType.TvShow -> if (value == 1) "sèrie" else "sèries"
    MediaType.Anime -> if (value == 1) "anime" else "animes"
    MediaType.Movie -> if (value == 1) "pel·lícula" else "pel·lícules"
    MediaType.Game -> if (value == 1) "joc" else "jocs"
    null -> if (value == 1) "títol" else "títols"
}

/**
 * Step for the +/- controls. One page at a time would be absurd on a 5.000-page goal, and one
 * minute at a time on a film-watching goal, so the step follows the unit's natural granularity.
 */
fun objectiveAmountStep(unit: ObjectiveUnit): Int = when (unit) {
    ObjectiveUnit.Titles -> 1
    ObjectiveUnit.Pages -> 100
    ObjectiveUnit.Episodes -> 10
    ObjectiveUnit.Minutes -> 60
    ObjectiveUnit.Hours -> 5
}

/** Plausible targets offered as one-tap chips, so most goals never need the keyboard. */
fun objectiveQuickPicks(unit: ObjectiveUnit): List<Int> = when (unit) {
    ObjectiveUnit.Titles -> listOf(12, 24, 40, 52)
    ObjectiveUnit.Pages -> listOf(1_000, 2_500, 5_000, 10_000)
    ObjectiveUnit.Episodes -> listOf(50, 100, 200, 365)
    ObjectiveUnit.Minutes -> listOf(600, 1_800, 3_000, 6_000)
    ObjectiveUnit.Hours -> listOf(20, 50, 100, 200)
}

private val catalanNumbers = Locale("ca")

/** Thousands separators, because `3.000 pàgines` is legible where `3000 pàgines` is a blur. */
fun formatObjectiveNumber(value: Int): String = String.format(catalanNumbers, "%,d", value)
