# Omnilog Activity Implementation Plan

## Purpose

This document sequences the work needed to satisfy `docs/omnilog-activity-concept.md`. The concept fixes the rules; this fixes the order.

Status: all four phases done. Database version is 20.

## Shape Of The Work

Four phases, in this order:

1. **Storage** — cumulative to increments, baselines onto the session.
2. **Readers** — objectives, timeline, session totals, backup.
3. **Behaviour** — coverage flag, undo, the bugs.
4. **Surface** — the UI overhaul.

The order matters. Phases 1 and 2 must land together — the database cannot be half-migrated across a release. Phase 4 is deliberately last: restyling the current list before the model changes would mean building the wrong screen twice.

Phase 3 could ship before phase 4, but its user-facing parts read oddly against the old UI, so shipping 3 and 4 together is reasonable.

## Phase 1 — Storage

The single largest piece, and the one that must be got right.

### Schema

Migration 19 to 20. **Done** — `core/database/migration/Migration19To20.kt`, registered and the database version bumped.

**`tracking_sessions`** gains `baselineProgress INTEGER NOT NULL DEFAULT 0`.

**`progress_updates`** is rebuilt:

- `progressValue` becomes `amount` and changes meaning from cumulative total to increment. Renamed rather than reused, so that every reader that has not been updated becomes a compile error instead of silently misreading a value whose meaning changed underneath it.
- `coversPeriod INTEGER NOT NULL DEFAULT 0` is added. Default 0 means sitting, per the concept.
- `countsTowardObjectives` is dropped.

The table is rebuilt rather than altered in place because SQLite only supports dropping a column from 3.35, which is later than this app's minSdk — the same reason migration 18→19 rebuilds.

### Where the baseline comes from

`convertSessionToIncrements` computes a baseline from the rows, but the migration does not use it. It derives the baseline as `progressCurrent - sum(entries)` instead, clamped at zero.

The reason is drift. The rows and the session's cached `progressCurrent` are supposed to agree, but they have had many versions of the app to fall out of step, and some sessions may hold progress that no row ever recorded. The number the user has been looking at is `progressCurrent`, so that is the number that must survive the upgrade. Deriving the baseline as the remainder makes the invariant true by construction for every session, including ones with no rows at all.

### Activation

Done together with phase 2, as required: renaming `progressValue` to `amount` breaks compilation everywhere it is read, and bumping the version without updating the entities fails Room's schema validation at runtime. There was no safe intermediate state.

### Data migration

**Done.** The conversion lives in `core/database/migration/ProgressIncrementConversion.kt` as a pure function, with `ProgressIncrementConversionTest` covering it. The migration itself still has to read rows, call it, and write results back.

It was written as a pure function rather than as SQL because the project has no `androidTest` source set, no exported Room schemas and no Robolectric, so a `MigrationTestHelper` test would have meant standing up instrumented-test infrastructure for one test. A pure function is testable with the plain JUnit already in use, and matches how the rest of the core logic is tested.

Per session, ordered by `loggedAtEpochDay`, then `createdAtEpochMillis`, then `id`:

1. **Leading** catch-up rows — those before any real logging — collapse to a baseline, taking the last of them. Those rows are deleted.
2. The remaining rows walk forward, each becoming the difference from the previous cumulative value.
3. Non-positive results are corrections, not sittings. They are dropped, and they do not inflate the row that follows.
4. If the session's last row corrected the total downwards, the surplus is trimmed from the most recent entries backwards until the totals agree.

Two decisions worth noting, both settled by the tests:

**A flagged row means a wrong date, not unreal progress.** `countsTowardObjectives = false` was set whenever a row was written after its session was already complete. That caught two very different things: adding a book finished years ago, and logging a day's reading a day late. Both lost their progress from objectives, silently.

The conversion therefore repairs the date rather than discarding the row. When the session has a finish date, a row stamped after it moves to the finish day and a row stamped at or before it keeps its own date; either way it becomes an ordinary entry. Only a session with no finish date has nothing to date the progress by, and that is the real baseline case — something added part-way through.

Found by comparing a real backup's objective total against the user's own count: seven books, 4265 pages, of which two worth 1033 pages had been silently uncounted for months. `tools/diff_objective_totals.py` reproduces that comparison against any pre-increment backup.

**Catch-up rows that appear after real logging are not baselines.** They come from marking a session complete with a past finish date. Logging had already begun, so they convert to ordinary entries — but with `hasKnownDate = false`, because such rows are stamped with the day they were written rather than the day the consumption happened. That is not a workaround: undated rows are already excluded from objectives, which is exactly the exclusion `countsTowardObjectives` was providing. The flag can be dropped without losing the behaviour it existed for.

**Steps 3 and 4 are lossy, and correctly so.** Progress the user has since revoked does not survive as an entry. The migration comment says this.

### Verification

The invariant is `baselineProgress + sum(entries) == old progressCurrent`, where the old value is the chronologically last row's cumulative value. Every fixture in `ProgressIncrementConversionTest` asserts it, alongside asserting that no entry is ever zero or negative.

Covered: a session of only catch-up rows, leading catch-up rows collapsing, real logging after a baseline, no baseline at all, a single row, rows written out of order, an empty session, a mid-sequence regression, a trailing regression, a regression that empties an entry, a duplicate value, an unknown date surviving, and a catch-up row after real logging.

What remains untested is the SQL around the function — reading rows out and writing them back. That is worth a manual check against a copy of the real database before release.

## Phase 2 — Readers

**Done.** Everything that assumed cumulative values, changed together with phase 1.

**`MediaDao.updateProgressUpdateAndRecalculateSession`** — the session total stops being "the value of the chronologically last update" and becomes `baseline + sum`. The existing doc comment on this function describes the cumulative rule and must be rewritten. The date-edit-changes-which-row-is-last complication disappears entirely, which simplifies the transaction.

**`OfflineMediaRepository.deleteProgressUpdate`** — recalculation becomes a subtraction rather than a search for the surviving maximum.

**`OfflineMediaRepository.updateProgressUpdate`** — writes an increment. The clamp against `progressTotal` now applies to the resulting session total, not to the row.

**`ObjectiveCalculator`** — the running `previousValue` subtraction at line 62 disappears; each row is already a delta. The `countsTowardObjectives` check goes with it, since baselines are no longer rows.

**`TimelineBuilder`** — `ProgressRecord` and its `previousValue` disappear. The rule that a non-positive delta is data maintenance and gets no row can go: non-positive rows no longer exist. The `countsTowardObjectives` filter goes too. The completion-day supersede logic that removes earlier rows when a correction lands should be re-examined — it exists to handle cumulative corrections and may no longer be needed.

**Backup format** — `progressValue` is serialised at `OfflineMediaRepository:2018` and read at `:2147`. Old backups are **not** converted on restore: the app has a single user, who can re-export once after upgrading.

But restore must **detect and reject** a pre-migration backup rather than reading cumulative values as increments. The scenario where an old backup gets restored is the scenario where the migration went wrong, so silently corrupting it there would destroy the only copy of the data at the worst possible moment. A format version field and a clear error is enough — roughly ten lines, and it keeps the recovery path intact.

**`insertProgressUpdateIfNeeded` is gone**, replaced by `applyProgressTarget`. Progress supplied when creating a session or item is written straight to `baselineProgress` and produces no entry, in both the completed and in-progress case. Editing a session's progress afterwards moves the entries instead: advancing logs an entry for the difference, and going backwards trims the newest entries first and only lowers the baseline once nothing is left to trim.

**`OfflineMediaRepository:1905`** — the assertion that values cannot be negative still holds, now against increments.

## Phase 3 — Behaviour

Everything except the coverage toggle's UI is **done**. That one piece is deliberately held for phase 4: the edit surface it belongs on is being rebuilt, and wiring a control into a dialog that is about to be deleted is throwaway work.

### Coverage flag

**Derivation done**, in `core/activity/ActivityWindows.kt` with `ActivityWindowsTest` covering it. Pure, so it tests without Compose.

`activityWindows` returns a window per period entry, measured back to the previous dated entry in the session or to the session's `startedAt` for the first one. Nothing is stored and no second date is ever asked for — the sequence already holds the bound.

Two rules the tests pin down. An entry dated the same day as the one before it gets no window, because a span of zero length is not worth printing. Undated entries neither get a window nor act as a bound for others, so the entry after one measures back past it to the last entry that actually had a date.

It has no caller yet; phase 4 renders it. That is deliberate, not an oversight.

**The toggle itself is phase 4 work.** `coversPeriod` is already carried through the model, the DAO and `updateProgressUpdate`, so the surface only has to set it.

### Undo for status events

**Done.** `deleteSessionStatusEvent` now returns `DeletionRecovery.SessionStatusEvents` carrying both halves of the pause, with a `restoreDeletion` branch and a snackbar, mirroring `deleteProgressUpdate`.

Restoring refuses if the session has gone or if either row is already present: putting half a pair back on top of a surviving half would produce a sequence that never happened.

### Bugs

All **done**. The first two landed during phase 2, since the file was already being changed.

**The surface closes on delete.** `showHistory` was keyed on entry counts, so deleting reset it to false. No longer keyed on the collection it displays.

**Unkeyed row state.** `pendingDelete` now shares `showEditor`'s key, so it cannot attach to the wrong row after a delete.

**Locale dates.** `formatDate` used a hard-coded `dd/MM/yyyy`; it now follows the reader's locale.

The two delta bugs from the original analysis — display order disagreeing with delta order, and hidden rows absorbing part of a visible delta — need no separate fix. Phase 1 removes derived deltas entirely.

## Phase 4 — Surface

**Done.** `ui/detail/ActivitySheet.kt`, `ActivityEntryEditor.kt` and `ActivityAction.kt` replace `ProgressHistoryAction.kt`, which is deleted.

Two choices were made by the user against mockups:

**A miniature of the Timeline.** Entries hang off a rail with beads, exactly as they do on the Timeline page — a status change takes the larger bead in its own accent, an entry a small one in the line's tone. Activitat is the same content at a smaller scale, one title's chronology instead of the library's, so it should not need a second reading vocabulary. This also settled the visual complaint that started the work: the surface now inherits the app's language rather than looking like a plain Material list.

**A bottom sheet.** Activitat is read, and a sheet is put down rather than closed with a button. It also lets a long history scroll without the session card growing to hold it. The card keeps a single `Activitat (n)` trigger.

Rows are acted on by tapping the row. The old surface put a 32dp edit button and a 32dp delete button side by side on every line — two targets below the minimum touch size, one destructive, a thumb's width apart. Delete now lives inside the editor, next to the thing being deleted.

`coversPeriod` gets its control here, as a switch in the editor, and nowhere else. Logging never asks. Turning it on needs no second date: the window derives from the entry before it, and the switch's subtitle names the span it will claim.

The original constraints, all met:

- Activitat is read, not only repaired, which argues for an inline or expandable surface on the session card over a modal.
- Entries show an amount as an increment, a date, and a window when `coversPeriod` is set.
- Consecutive episodes in one entry read as a run.
- Editing acts on the entry itself rather than on per-row icon buttons, which also retires the two 32dp targets sitting next to each other.
- Grouping follows gaps in the data, and is readability only.
- Hidden at fewer than two entries, every type.
- `OmnilogTheme` colours, the app's own card and typography treatment, the session's accent.

### Strings

Done. The whole `progress_history_*` block is gone, replaced by `activity_*`. The trigger keeps its count — it is what says how much story is behind the tap.

## Risks

**The migration is irreversible in practice.** Take a backup before upgrading, and keep it until the new data has been eyeballed. The invariant test covers the conversion logic; it does not cover the SQL that feeds it.

**A rejected old backup is a dead end, not a recovery.** Since old backups are not converted, the pre-migration export is only useful with a pre-migration build. Keep the APK too, or the backup is worth less than it looks.

**Phase 1 and 2 are one release.** They cannot be split, which makes for one large change. Keeping phases 3 and 4 out of it is what keeps that change reviewable.

## Out Of Scope

Objective attribution for period entries. `ObjectiveCalculator` counts a period entry entirely on its logged date, so a week of gaming logged on Sunday lands on Sunday. Period entries make this visible without fixing it. The concept records this as an open question; it needs its own decision and does not block any phase here.
