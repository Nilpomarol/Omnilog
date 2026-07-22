# Omnilog Activity Concept

## Purpose

This document defines what the per-session progress history is for, and records the decisions behind it. It replaces the feature currently shipped as `ProgressHistoryAction` and named `Historial de progrés`.

It is a concept document, not an implementation plan. It fixes the rules; the work needed to satisfy them is listed at the end but not sequenced.

Status: agreed, not yet implemented.

## The Problem With The Current Feature

The shipped history is a log of edits to the database rather than a log of consumption. It presents rows of `progress_updates` in the order they were written, labelled with the arithmetic difference between neighbouring cumulative values.

That produces entries like `-3 pàgines · total 180`. Nothing of the kind happened to the user. A correction is being reported as an event.

The same confusion shows up elsewhere in the feature:

- Rows are displayed by insertion instant but deltas are computed by logged date, so back-dating a row makes the arithmetic disagree with the visible sequence.
- The delta baseline is taken from all rows including import snapshots, while rendering filters those out, so a hidden row silently absorbs part of a visible delta.
- The surface is reached through a button that reads as a repair tool, so it is only opened when something is wrong.
- It is offered even when a single entry exists, where there is no sequence to read.

These are symptoms of one thing: the feature has no product definition, so its presentation defaulted to its storage shape.

## Product Definition

**Activitat is the record of a session's progress: what was logged, how much, and when.**

It is a thing the user reads, not only a thing the user repairs. Everything below follows from that.

### Naming

The feature is `Activitat`. `Historial` is retired — it reads as an audit trail, which is what the feature must stop being.

### Rules

**One row is one sitting.** An entry records a single occasion of consuming the thing. This is the unit the user recognises, and it is the unit they were already trying to express when logging.

**Rows store increments, not cumulative totals.** An entry means `+32 pàgines`, not `progress is now 212`. Under cumulative storage every row's meaning depends on its neighbours: correcting one entry silently changes the apparent size of the next, and a corrected value that dips below its predecessor produces a negative. Increments make each row self-contained, which is what makes the rules below possible.

**Corrections are invisible.** Fixing a mistake edits or deletes the entry in place. It never appends a row, and it never produces a negative. A wrong entry gets made right or removed; it does not leave a scar in the record.

**There are no negative entries.** Consuming less than recorded is a correction, not an event. If progress genuinely needs to go backwards, the user edits the offending entry down or deletes it. This case is rare enough that it does not justify modelling a reversal.

**Pause and resume belong in the same list.** They are part of the session's story and are read in sequence with progress. They are a different kind of row and must look like one. They remain append-only and delete-only, and deleting either half of a pause still removes both, as documented in `MediaRepository.deleteSessionStatusEvent`.

**A single entry is not activity.** With one row there is no sequence, so the surface is not offered. This applies to every media type without exception.

**Where you started is not activity either.** Progress that predates tracking — adding something already finished, or already part-way through — is a starting position, not something that happened. It belongs to the session as a baseline and never becomes a row. This is the same rule as the one above applied to storage: if there is nothing to sequence, it is not a record. See *Baselines* below.

## Dates And Coverage

Dates were the hardest part of this concept, because logging cadence is not regular. The same book may be logged daily for a week, then not for two months, then in a lump. A week of gaming is often logged on the day the user happens to remember.

The resolution is to keep the entry's date meaning exactly one thing, and to make coverage an opt-in detail.

### An entry's date means when it was logged

That statement is true at any cadence, so it never has to be defended. The presentation must not phrase an entry as a claim about what happened on that day. `15 de març · 10 h` is a record. "You played 10 hours on 15 March" is an assertion the app cannot support.

The existing unknown-date state (`hasKnownDate`, shown as `Data desconeguda`) stays as the escape hatch for entries with no meaningful date at all.

### An entry may be marked as covering a period

Some entries genuinely accumulate: a week of play logged in one go. For these, an entry can be marked as covering a period rather than a sitting.

When it is, the window is **derived from the previous entry in the session**, not entered by the user. If the previous entry was 2 March and this one is 15 March, the progress happened somewhere in that window, and the entry reads:

```
15 de març · 10 h
des del 2 de març
```

The first entry of a session falls back to the session's start date. Nothing is typed, and no second date field exists.

### Sitting is the default

Entries default to sitting, for two reasons.

The first is safety. Under-claiming is better than over-claiming. Defaulting to sitting on an entry that was really accumulated shows a date and omits a span — less informative, but nothing false. Defaulting to period on an entry that was really one evening prints a span that never happened, which is the exact failure this concept exists to remove.

The second is frequency. Sittings are logged every time there is a sitting, sometimes several a day. Periods are logged occasionally by definition. The common action should need no decision.

### The flag is a correction, not a question

The user is never asked which kind an entry is at log time. Logging stays one value and a save.

Marking an entry as covering a period is done in Activitat, on the entry itself, with the derived window shown as the result. This follows the rule that corrections happen in the record and not in the log flow, and it means the distinction only has to be understood by users who care about it.

Existing entries need no migration: they become sittings, which is the claim the app can actually support for them.

## Baselines

Some progress was never logged as it happened. Adding a book you finished last year, or one you are already half-way through, gives the session a progress value for consumption that occurred before Omnilog knew about it.

Today the app manufactures a progress row for this and flags it `countsTowardObjectives = false` — see `OfflineMediaRepository.addTrackedMedia` and `updateSessionDetails`. The flag is set when a session is Completed with a finish date that is not today.

That single boolean is doing three jobs at once: keep this out of objectives, keep it out of Activitat, and keep it out of the Timeline. Under increment storage a fourth question appears — does it count toward the session total? It must, or the book reads as zero pages. So the row would be a real increment that is not real activity, which is a contradiction stored inside one flag. It is already the cause of the delta bug where a hidden row silently absorbs part of a visible delta.

### Decision

**Baselines live on the session, not in the table.** `TrackingSession` gains a baseline progress value, and the session total becomes `baseline + sum of entries`. Catch-up rows stop existing.

This makes the concept describable without exceptions: `progress_updates` **is** Activitat, everything in it is a sitting, and everything else lives on the session. No consumer needs to filter, `countsTowardObjectives` disappears, and the delta bug cannot return because there are no hidden rows.

It also sharpens the single-entry rule. With catch-up rows gone, a session showing exactly one entry has one genuinely logged sitting, rather than a row the app invented on the user's behalf.

And it gives the derived window an honest floor: the first entry of a session measures back to the session start, which is precisely when the baseline was true.

### Partial baselines count too

The current flag is only set for completed sessions. Adding something already in progress — a book at page 180 — writes a counting row, so those 180 pages land in today's objectives as though they were read today. That is the same problem wearing a different coat, and it gets the same answer: 180 is a baseline.

## Presentation

### Grouping is readability only

Because the date carries no claim beyond when the entry was logged, grouping does not have to be correct — it only has to make a long list scannable.

Group by the gaps in the data rather than by media type or a fixed calendar unit. Entries close together cluster; large gaps read as gaps. If the grouping is occasionally odd, nothing breaks, because no meaning rests on it.

An earlier draft of this concept grouped by media type on the theory that each type has a steady cadence. It does not: cadence varies within a single item, so that rule was dropped.

### Per media type

The rules are identical for every type. Only the density of real data differs.

| Type | What a sitting is | Notes |
|---|---|---|
| Book, manga | A reading session | Densest data. The list does most of its work here. |
| Series, anime | Episodes watched in one sitting | Consecutive episodes in one entry read as a run, not as separate rows. |
| Game | A play session | Sparse, and the most likely to use period entries. |
| Movie | One sitting | Usually one entry, so usually hidden. Appears when a film was watched across more than one sitting, which is a fact worth showing. |

Movies are deliberately not special-cased out of the feature. The single-entry rule already hides them in the common case.

### Editing

Editing acts on the entry itself rather than on per-row icon buttons. The current row carries a 32dp edit button and a 32dp delete button side by side, both below the minimum touch target and both adjacent to a destructive action.

The edit surface must offer, in the language of the concept:

- the amount consumed, expressed as an increment in the media type's unit
- the date
- the option to mark the entry as covering a period
- delete

The current dialog labels its field `Progrés acumulat` while the row it was opened from displays a delta. Under increment storage that mismatch disappears on its own.

### Placement

Deferred to the UI overhaul, with one constraint from this concept: Activitat is something the user reads, which argues for an inline or expandable surface on the session card rather than a modal that has to be dismissed. The final choice should be made against real screens.

### Visual language

The feature does not currently match the rest of the app and needs a full pass alongside the overhaul. It should use `OmnilogTheme` colours, the app's own card and typography treatment, and the accent of the session it belongs to, rather than reading as a plain Material list.

## Consistency Requirements

**Deleting a status event must be undoable.** Progress deletions publish a `DeletionRecovery` and show an undo snackbar; status deletions do not, despite removing two rows at once. The more destructive action currently has less protection.

**Delete confirmations must identify what is going.** The progress dialog quotes the entry. The status dialog does not say which pause is being removed.

**The surface must survive a delete.** Deleting an entry currently closes the whole surface, because the open/closed state is keyed on the entry count. Cleaning up three bad entries means reopening three times.

## What This Invalidates

Recorded so the implementation plan has a starting point. None of this is scheduled here.

**Storage.** `ProgressUpdateEntity.progressValue` is cumulative and is read as such by `ObjectiveCalculator`, `TimelineBuilder`, session progress recalculation in `MediaDao.updateProgressUpdateAndRecalculateSession`, deletion recalculation in `OfflineMediaRepository.deleteProgressUpdate`, and the backup JSON format. Moving to increments touches all of them and needs a migration. This is the largest single cost in the concept and should be planned before any UI work.

**Catch-up rows move to the session.** Rows written with `countsTowardObjectives = false` are baselines, not activity, and become a field on `TrackingSession`. The flag is then unused and is removed along with the filtering it forced on `ProgressHistoryAction`, `TimelineBuilder` and `ObjectiveCalculator`. See *Baselines* above.

**Derived deltas.** The delta arithmetic in `ProgressHistoryAction` and the regression handling in `TimelineBuilder` both exist to paper over cumulative storage. Increments remove the need for both, including the timeline's rule that a non-positive delta is data maintenance and gets no row.

**The modal.** `ProgressHistoryModal` and its `OmnilogModal` host are replaced by whatever the overhaul chooses.

**Dead strings.** `progress_history_entry` and `progress_history_more` have no references and should go. The remaining `progress_history_*` strings are renamed to `activity_*` and rewritten in the language of this concept.

**Date formatting.** `dd/MM/yyyy` is hard-coded in `ProgressHistoryAction.formatDate`. It should follow the locale.

## Open Question

`ObjectiveCalculator` attributes progress to objectives by `loggedAt`. A week of gaming logged on Sunday therefore lands entirely in Sunday. Period entries make this visible in the UI without fixing it: the record will say the progress covers a window while objectives still count it on one day.

This is out of scope for the concept and needs its own decision. Spreading a period entry across its window is one option; leaving attribution on the logged date and accepting the skew is another.
