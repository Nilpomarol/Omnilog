# Activity

Status: Current feature specification  
Last reviewed: 2026-09-12

`Activitat` is the per-session record of consumption and status changes. It is a user-facing history, not a database audit log.

## Core Model

- One progress row represents one sitting/consumption entry.
- Progress rows store **increments**, not cumulative totals.
- Progress that predates Omnilog tracking belongs in the session baseline, not as a fake activity row.
- Corrections edit/delete the existing entry; they do not create negative activity.
- There are no negative progress entries.
- Status changes such as pause, resume, completion, and drop are part of the session story.

A session's current progress is conceptually:

```text
baselineProgress + sum(progress entry amounts)
```

## Dates

A progress entry's date is the date the user assigns to the logged activity.

Unknown-date entries are allowed when no trustworthy date exists.

An entry may be marked as covering a period. The period is derived from surrounding/session dates; the user does not enter a second date just to log progress.

Default to a normal sitting because it makes the weaker claim. Do not infer a period unless the user explicitly marks it.

## Baselines

Initial progress that existed before tracking started is a baseline.

Examples:

- adding a book already halfway read;
- importing a previously completed title;
- starting Omnilog with existing game hours.

Baselines affect totals but do not appear as consumption activity.

## Status Events

Status transitions belong in Activity when they represent meaningful session history.

Deleting a transition removes only that transition and recalculates the resulting session state according to the surviving history. Do not delete a pause/resume pair merely because they were once related.

Same-day pause/resume pairs may be suppressed from chronology when they represent an accidental tap/correction rather than a meaningful break.

## Ordering

The Activity list preserves a stable user-readable event sequence. Running totals must still be calculated from the dated progress semantics, so editing a date may affect the total shown at a point without turning the history into a database-write log.

## Visibility

Do not expose an Activity surface when there is no meaningful sequence to read.

A lone baseline is not activity. A session with milestones/status events plus progress may still have a meaningful sequence even with few progress rows.

## Corrections

Activity is also where users correct historical progress details.

Corrections must:

- update/remove the affected entry;
- preserve unrelated entries;
- keep totals consistent;
- never manufacture a negative consumption event.

## Shared Semantics

Library-wide Timeline derives from the same underlying progress/status history. Changes to Activity semantics must be checked against Timeline and Stats/objective readers so they do not interpret increment data as cumulative values.

## Implementation References

Primary implementation areas include:

- `core/model/ProgressUpdate`
- `core/model/TrackingSession`
- status-event models
- `ui/detail/ActivitySheet.kt`
- repository session/progress mutations

Historical migration and implementation details are archived under `docs/archive/implementation/`.
