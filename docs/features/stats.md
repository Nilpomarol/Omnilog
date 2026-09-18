# Stats

Status: Current feature specification  
Last reviewed: 2026-09-17

Cronologia, Calendari and Estadístiques are swipeable tabs of one page, "Registre" (`AppRoute.Record`, `ui/record/RecordScreen.kt`). The page owns the filters, shown once under the tabs and shared by all three: format, and period (Aquest any by default; 12 mesos; Tot; past years). The period chip hides on Calendari, whose month picker moves through time. Home's recent-activity link opens it on Cronologia and the rhythm card on Estadístiques.

Stats is a secondary Home/Profile drill-in for understanding the user's own history. It is not a provider-popularity dashboard and should not become a sixth root navigation destination.

## Principles

- Use local tracking history as the source of truth.
- Keep personal ratings more important than provider ratings.
- Prefer explicit, trustworthy definitions over visually impressive but ambiguous charts.
- Do not compare unlike consumption units on one scale.
- Keep implementation in pure Kotlin over existing tracked-media models unless a concrete need justifies schema/dependency changes.

## Filters

Stats supports period and media-type filtering.

Current period concepts include:

- all time;
- this year;
- selected historical year;
- last 12 months.

Comparisons must be like-for-like. Year-to-date compares against the same date range in the previous year, not the full previous calendar year.

## Completion

Distinguish clearly between:

- unique titles completed;
- completion sessions.

A completion belongs to a dated period only when there is a trustworthy completion date/status event.

Do not silently use unrelated `updatedAt` timestamps as completion dates.

## Ratings

Personal-rating statistics use user ratings on tracking sessions, not provider scores.

A rating can be placed on a time axis only when it has a trustworthy completion/date association under the current Stats model. All-time may include otherwise-undated ratings where the implementation explicitly supports that distinction.

## Progress / Consumption

Progress entries are stored as increments.

Do not apply the old cumulative-delta algorithm described in early Stats plans.

Consumption units remain medium-specific:

- books → pages;
- anime / TV → episodes where applicable;
- movies → minutes;
- games → hours.

Do not create charts that visually compare pages vs minutes vs hours as if the numeric magnitudes were comparable.

Use unit-aware tiles, within-medium comparisons, or time comparisons for the same unit.

## Page Structure

The page reads as a short story in chapters, not a dashboard. No bordered cards: serif chapter titles, small uppercase part labels, hairlines, cover shelves and restrained charts, in the same language as Home's `El teu ritme` and the Cronologia tab. A chapter renders only when it has data.

Heading rule: a chapter title may carry one note, and the parts inside a chapter are named only by a `PartLabel` (no description under it). A needed caveat goes after its chart as a footnote.

1. **Lead** — completions for the period in serif with the comparison sentence shared with Home (`RhythmComparison`), stacked columns by format (a year always shows twelve months with future months blank; last 12 months shows those months; all time shows one column per year), then a carousel of figure tiles: average rating, estimated hours, best month, completions per month, distinct titles (only when different from completions), revisits, ratings given.
2. **Les teves notes** — average with delta and rating count, the fixed 1–10 distribution stacked by format, the monthly average line (fitted scale with labelled marks, month initials, the latest or tapped month called out; only adjacent rated months join), best-rated titles and best-rated collections as shelves.
3. **Per format** — one part per question: completions as ranked bars, consumption in each format's own unit, estimated hours as a share bar with legend (the only cross-format scale), average rating as dots on 0–10, average length in each unit.
4. **Els teus gustos** — genres and languages as ranked bars, creators as a shelf of fanned covers.
5. **Hi has tornat** — titles finished again, and the most revisited as a shelf.
6. **La teva biblioteca ara** — current status counts as a share bar and figure strip, explicitly independent of the period.

Each shelf (best rated, collections, creators, most revisited) has a "Veure-ho tot" link to `StatsListScreen` (`AppRoute.StatsList`), which lists every match for the same period and format filter, ranked the same way.

Charts draw in (columns rise, bars and lines grow) the first time they come into view for a given period and filter, and not again when scrolled back to or after returning from a list.


Principles kept from the earlier charts:

- rating distributions keep the full fixed scale rather than deleting zero-count positions;
- genre charts do not imply mutually exclusive shares;
- trend lines do not bridge periods with no data;
- unlike units are never drawn on one scale except as clearly labelled estimated time.

## Current-State vs Selected-Period Data

Do not silently mix current-library state with selected-period history.

Examples:

- `Planned now` / `In progress now` are current-state concepts;
- completions/ratings/activity in 2025 are historical-period concepts.

Label and group them accordingly.

## Implementation References

- `core/stats/StatsCalculator.kt`
- `core/stats/StatsModels.kt`
- Stats UI under `ui/stats`
- Stats tests under `app/src/test/.../core/stats`

Historical system and refinement plans are retained under `docs/archive/` for reasoning/delivery context, not as current specifications.
