# Stats

Status: Current feature specification  
Last reviewed: 2026-09-17

Cronologia and Estadístiques are two swipeable tabs of one page, "Registre" (`AppRoute.Record`, `ui/record/RecordScreen.kt`). Home's recent-activity link opens it on Cronologia and the rhythm card on Estadístiques.

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

## Charts And Hierarchy

The top of Stats should communicate the selected period quickly:

- completion result;
- personal rating summary;
- revisits;
- one useful unit-aware consumption highlight;
- concise observation when the data supports one.

Current chart principles:

- monthly/yearly activity should reflect the selected scope honestly;
- rating distributions keep the full fixed scale rather than deleting zero-count positions when that would distort it;
- genre charts should not imply mutually exclusive shares when genres overlap;
- trend lines should not bridge periods with no data as though observations existed;
- hide modules that have too little data to say anything useful.

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
