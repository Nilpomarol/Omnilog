# Omnilog Activity Concept

## Purpose

This document defines what the per-session progress history is for, and records the decisions behind it. It replaced the feature previously shipped as `ProgressHistoryAction` and named `Historial de progrés`.

It is a concept document, not an implementation plan. It fixes the rules; `docs/omnilog-activity-implementation-plan.md` records how they were satisfied.

Status: delivered. The rules below describe the feature as it stands, and the decisions they overturned are kept rather than deleted — several were reversed once, and the reasoning matters more than the conclusion.

## The Problem It Replaced

The old history was a log of edits to the database rather than a log of consumption. It presented rows of `progress_updates` in the order they were written, labelled with the arithmetic difference between neighbouring cumulative values.

That produced entries like `-3 pàgines · total 180`. Nothing of the kind happened to the user. A correction was being reported as an event.

The same confusion showed up elsewhere in it:

- Rows were displayed by insertion instant but deltas were computed by logged date, so back-dating a row made the arithmetic disagree with the visible sequence.
- The delta baseline was taken from all rows including import snapshots, while rendering filtered those out, so a hidden row silently absorbed part of a visible delta.
- The surface was reached through a button that read as a repair tool, so it was only opened when something was wrong.
- It was offered even when a single entry existed, where there is no sequence to read.

These were symptoms of one thing: the feature had no product definition, so its presentation defaulted to its storage shape.

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

**Status changes belong in the same list.** Pauses, resumes, completions and abandonments are part of the session's story and are read in sequence with progress. They are a different kind of row and must look like one.

**A status change is dated by the user, not by the clock.** The instant a row was written fixes its place in the sequence; the day it happened is the user's to state and can be corrected. Pause a book on Friday, remember on Sunday, and the log should not insist on Sunday. This is the same split `progress_updates` already makes between `loggedAtEpochDay` and `createdAtEpochMillis`.

**Deleting a transition undoes only that transition.** It never happened, so the session falls back to the state before it: delete a resume and the session is paused again, which is what it was. An earlier version of this concept removed a pause and its resume together, on the grounds that half a pair described nothing real. That was wrong — a lone pause describes something perfectly real, namely a session that is still paused. Only the newest transition moves the session's own status; removing an older one edits the record without touching the present.

**A single row is not activity.** With one row there is no sequence, so the surface is not offered. Milestones count towards this: a session with one entry but a start and a finish does have a sequence to read — began, advanced, ended — which is what the rule is protecting. It applies to every media type without exception.

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

## The Status Log

The log records every transition, but Activitat does not show every row. Two filters decide what is worth reading, both in `meaningfulActivityStatuses`.

**A transition that changes nothing is not shown.** A second consecutive pause says nothing the first did not. A resume with no pause before it is not a resume at all — it is a session starting, which the start milestone already says.

**Terminal transitions stay.** Completing and abandoning are the end of the story and belong in it, unlike starting, which the milestone covers.

### Milestones

A session's `startedAt` and `finishedAt` appear as rows, so the list does not begin mid-story with a run of entries that have no beginning and no end.

They are **read-only**. Both are edited through the session editor, and offering them here as well would be two places to change one fact. This is the distinction the earlier draft of this concept missed when it excluded them entirely: showing is not editing.

A finish milestone is **suppressed when a matching terminal transition exists**, because the two would say the same thing twice. Sessions that predate the log have no such transition and rely on the milestone; sessions that have one rely on the transition, which carries a date the user can correct.

### Two orderings

**The list** is ordered by the instant each row was written. Entries and status changes can both be re-dated, and a correction should not make rows jump around the list it was made in.

**Running totals** accumulate in date order instead. A total answers "where had I got to by then", which is a claim about the order things were consumed. It also has to agree with `TimelineBuilder`, which totals the same entries the same way.

The cost is that a back-dated entry can show a total out of step with its position — re-date the newest entry to the earliest day and its total becomes the smallest while it stays at the top. That is the honest consequence of the row sitting where it was recorded and the total counting where it belongs.

Milestones have no recorded instant, so they take the ends of the list outright, which is also where they belong.

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

A row is acted on by tapping the row. The surface it replaced put a 32dp edit button and a 32dp delete button on every line — two targets below the minimum touch size, one destructive, a thumb's width apart.

An entry's editor offers the amount as an increment in the media type's unit, the date, the period switch, and delete. Delete sits under the save button in the error colour: reachable, but never where the thumb lands by default. A status row's editor offers its date and delete; milestones open nothing.

**Entries are not capped by the media's total.** A provider's page count is metadata and can be corrected below what the user has actually recorded, so validating an entry against it would make real history unsaveable. The only bound is that an amount must be positive — an entry worth nothing is a deletion.

**A status change cannot be dated outside its neighbours.** The editor bounds the picker by the previous and next transition, because a log whose rows disagree with their own order describes nothing.

### Placement

A bottom sheet, opened from an `Activitat (n)` trigger on the session card. Activitat is read, and a sheet is put down rather than closed with a button. It also lets a long history scroll without the session card growing to hold it.

**Only the top dismisses it.** A sheet drags on whatever its content leaves unconsumed, which would make a downward swipe anywhere in the list a dismissal — including at the top of a long history, mid-read. The list keeps every vertical gesture, leaving the drag handle and the title as the only places that close it.

### Visual language

A miniature of the Timeline: entries hang off a rail with beads, a status change taking the larger bead in its own accent and an entry a small one in the line's tone. Activitat is the same content at one title's scale, so it should not need a second reading vocabulary.

## Durability

**Deleting is undoable.** Both entry and status deletions publish a `DeletionRecovery` and show an undo snackbar. Restoring a status deletion puts back the row and the session status it moved, and refuses if either has changed underneath — a half-applied undo would leave a sequence that never happened.

**Delete confirmations name what is going.**

**The surface survives a delete.** Its open state is not keyed on the rows it displays, so cleaning up three bad entries does not mean reopening three times.

**The status log is backed up.** It was not, and `replaceAllData` deletes sessions, so restoring a backup silently destroyed every pause and resume in the library. Backups carry it from schema 9.

## What It Cost

The work touched storage, every reader of it, the backup format and the whole surface, across four database versions. `docs/omnilog-activity-implementation-plan.md` records it in detail — the migration and its lossy steps, the readers that assumed cumulative values, and the two bugs the conversion uncovered along the way.

Two are worth naming here because they were the point rather than the by-product.

**`countsTowardObjectives` was hiding a real bug.** The flag was set whenever a row was written after its session was already complete, which caught both "added a book I finished years ago" and "logged today's reading a day late" — and silently dropped the second from goals. Two books worth 1033 pages had gone uncounted for months. The flag is gone; baselines and dates carry what it was standing in for.

**The status log was never backed up.** Restoring a backup silently destroyed every pause and resume in the library. Nothing on screen said so.

## Open Question

`ObjectiveCalculator` attributes progress to objectives by `loggedAt`. A week of gaming logged on Sunday therefore lands entirely in Sunday. Period entries make this visible in the UI without fixing it: the record will say the progress covers a window while objectives still count it on one day.

This is out of scope for the concept and needs its own decision. Spreading a period entry across its window is one option; leaving attribution on the logged date and accepting the skew is another.
