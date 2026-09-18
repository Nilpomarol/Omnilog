# Timeline / Cronologia

Status: Current feature specification  
Last reviewed: 2026-09-17

Cronologia, Calendari and Estadístiques are swipeable tabs of one page, "Registre" (`AppRoute.Record`, `ui/record/RecordScreen.kt`). The page owns the filters, shown once under the tabs and shared by all three: format, and period (Aquest any by default; 12 mesos; Tot; past years). The period chip hides on Calendari, whose month picker moves through time. Home's recent-activity link opens it on Cronologia and the rhythm card on Estadístiques.

Timeline is the library-wide chronology of meaningful consumption events. It is not a generic audit log of every database mutation.

## Included Events

Timeline may include:

- progress entries;
- session starts;
- revisits/replays;
- meaningful pause/resume transitions;
- completions;
- dropped/abandoned sessions when a trustworthy date exists.

It should not create standalone events for unrelated metadata edits, collection changes, ownership changes, or other mutations without a trustworthy event timestamp.

## Progress Semantics

`ProgressUpdate` rows store **increments**.

Timeline calculates running totals as:

```text
session baseline + dated increments up to that point
```

Do not reintroduce the old cumulative-value subtraction logic.

Undated rows are never given an invented date. They sort after every dated row and the screen folds them into a collapsed "Data desconeguda" section at the end, where they can be opened and dated.

## Completion / Start Folding

When a progress entry occurs on the same meaningful day as a start or completion milestone, Timeline may fold that progress into the milestone so the chronology reads as one meaningful event rather than duplicate adjacent rows.

Folding is derived once in `core/activity/SessionActivity.kt` and shared with Activity; `TimelineBuilder` only maps those rows. The transition that opened the current run backs the session's start row whatever its day; the repository keeps that transition's day and `startedAt` in step when either is edited. Reopen and back-to-planned transitions are omitted from Timeline, and same-day pause/resume pairs are collapsed there.

## Status History

Repeated terminal/status transitions can survive in the status-event history even if the current session snapshot later changes.

Use immutable/explicit status-event history where available rather than reconstructing old transitions from `TrackingSession.updatedAtEpochMillis`.

The last-modified timestamp is only an ordering fallback for legacy snapshot data and must not be presented as an invented event date.

## Presentation

The screen reads as a diary: months are chapters (serif heading plus a summary of completions and progress per unit), the day sits once in the gutter, and each row hangs off the shared rail. There is no hero/recap card. Rows come in three tiers: endings (completed, abandoned) have the large cover, a heavier title, outcome and rating; changes of state (started, revisited, paused, resumed) a small cover, title and state; progress entries a thumbnail, the amount and a bar of prior ground versus this gain. No tier is a card. The bead grows with the tier. Month totals count every entry, including hidden progress.

Tapping a row opens that session's Activitat sheet over the screen, with a link to the item.

The **Calendari** tab of the Registre page (`ui/timeline/TimelineCalendar.kt`) shows the same chronology as a month grid: one swipeable page per month, from the first recorded month to the current month. Weeks are always complete, starting on the locale's first day, with the neighbouring months' days dimmed; tapping one of those pages to its month and selects it. The calendar shows milestones only: progress entries never appear on it, whatever the visibility setting, since a day square holds one cover. An active day shows the cover of its most telling entry (completion, then abandonment, start/revisit, pause/resume) and "+N" when several titles share the day. Tapping a day lists its entries under the calendar with the same diary rows; landing on another month selects its latest active day. Tapping the month title opens a compact sheet to pick any month and year in range (active months carry a dot). The tab follows the page's format filter; the period word hides there, since the picker already covers time. The tab follows the page's format filter, ignores the period, and honours the same visibility settings as Cronologia; undated entries have no place on it.

Progress rows are off by default and opted into per type; the screen says under the filters when they are hidden and links to the setting. While a type's progress is hidden, its milestones drop their amount and position too, since a lone folded amount would read as the whole story.

Rows should remain compact and readable.

Useful information includes:

- cover thumbnail;
- title;
- event description;
- dated progress delta and running total when meaningful;
- revisit/session context;
- completion rating where appropriate;
- event date.

Avoid synopsis, full metadata, provider rankings, or other detail-page content in chronology rows.

## Filtering

Timeline may provide media/event filters, but filters should operate over already-derived events rather than rebuilding source semantics differently for each screen.

Keep milestone behavior consistent even when progress-history rows are hidden by a filter.

## Social Direction

Timeline/Cronologia is the natural destination for future broad personal + friend activity.

Social additions should not change the semantics of personal consumption events. Keep personal events trustworthy and layer social context explicitly.

## Implementation References

- `core/activity/SessionActivity.kt`
- `core/timeline/TimelineBuilder.kt`
- `core/timeline/TimelineModels.kt`
- Timeline tests under `app/src/test/.../core/timeline`
- Timeline UI under `ui/timeline`

The old implementation plan is archived because it describes the pre-increment progress model and is no longer authoritative.
