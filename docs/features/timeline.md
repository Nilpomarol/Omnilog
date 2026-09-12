# Timeline / Cronologia

Status: Current feature specification  
Last reviewed: 2026-09-12

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

Unknown-date progress remains editable in the item's Activity surface but cannot honestly be placed in a library-wide chronology, so it should not be assigned an invented date.

## Completion / Start Folding

When a progress entry occurs on the same meaningful day as a start or completion milestone, Timeline may fold that progress into the milestone so the chronology reads as one meaningful event rather than duplicate adjacent rows.

The implementation already handles this in `TimelineBuilder`; preserve its behavior unless product requirements change explicitly.

## Status History

Repeated terminal/status transitions can survive in the status-event history even if the current session snapshot later changes.

Use immutable/explicit status-event history where available rather than reconstructing old transitions from `TrackingSession.updatedAtEpochMillis`.

The last-modified timestamp is only an ordering fallback for legacy snapshot data and must not be presented as an invented event date.

## Presentation

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

- `core/timeline/TimelineBuilder.kt`
- `core/timeline/TimelineModels.kt`
- Timeline tests under `app/src/test/.../core/timeline`
- Timeline UI under `ui/timeline`

The old implementation plan is archived because it describes the pre-increment progress model and is no longer authoritative.
