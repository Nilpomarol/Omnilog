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

Every transition that changed the session's state is shown in Activity (start, pause, resume, reopen, back to planned, completion, drop), because a transition the user cannot see is one they cannot correct. A transition into the state it left is not a row.

Same-day pause/resume pairs are suppressed from the library Timeline only; Activity keeps them so they stay correctable.

## Ordering

Activity reads newest day first, grouped by month, with rows of one day in the order they were recorded. Undated rows close the list under their own heading.

Ordering by day keeps running totals in sequence, and a re-dated entry moves to the day it now claims.

## Presentation

Every row that holds progress shows both the increment and the running total it reached (against the item's total when it has one). Status changes carry more weight than progress entries; a completion shows the session's rating.

A row that holds both a transition and a folded progress entry is edited in one editor, with a section and a delete for each part.

## Visibility

Expose the Activity surface whenever the session holds something it can correct: a progress entry or a status transition.

A lone baseline or a bare start date is not activity.

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

- `core/activity/SessionActivity.kt` — the single derivation (rows, folding, running totals) shared with Timeline
- `core/model/ProgressUpdate`
- `core/model/TrackingSession`
- status-event models
- `ui/detail/ActivitySheet.kt`
- repository session/progress mutations

Historical migration and implementation details are archived under `docs/archive/implementation/`.
