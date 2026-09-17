# Activity Audit

**Date:** 17 September 2026
**Status:** Advisory. Nothing here is fixed just because it is written down.
**Scope:** Per-session Activitat (session card entry, `ActivitySheet`, entry/status editors), the repository mutations behind it, and the library Timeline (main activity screen).

Paths are relative to `app/src/main/java/com/nilpo/contenttracker/`.

---

## 1. Summary

The data model is sound: increments plus a baseline, and an append-only status log with editable dates. Most of the problems come from **three separate filters that each decide what is "meaningful"**, and none of them agree:

| Layer | Rule |
|---|---|
| `meaningfulActivityStatuses` (sheet) | hides every resume without an open pause, every `Planned` transition, repeated pauses |
| `ActivityAction` (entry point) | hides the whole surface below 2 items, and only renders inside the dates row |
| `TimelineBuilder` (main screen) | hides same-day pause/resume pairs, unknown dates; progress rows off by default |

The mutations work against the **full** log. The UI shows a **filtered** log. Your three reported issues all come from that gap, plus a lookup bug in the sheet.

---

## 2. Reported issues: verdicts

### 2.1 "Deleting a state transition doesn't go back to the previous state": CONFIRMED (3 causes)

**A. Hidden later events make a visible transition "not the latest".**
`deleteSessionStatusEvent` only restores status when the event is the last row of the *full* log (`core/repository/OfflineMediaRepository.kt:884`). The sheet hides many rows (`ui/detail/ActivitySheet.kt:421`). So the newest row you *see* is often not the newest row that *exists*, and deleting it changes nothing.

Reproductions:
- Finished → reopen (hidden `Completed→InProgress`). The sheet shows "Finished" on top. Deleting it leaves the session as it is.
- Abandoned → back to In progress (hidden). Same result.
- In progress → Planned from the status menu (hidden). Every visible delete is "historical".
- Pause, Resume, Pause. Delete the Resume: the second Pause now follows a Pause, so `meaningfulActivityStatuses` hides it. The list shows one Pause. Deleting it does not un-pause, because the hidden second pause is the latest.

**B. Status changes are silently rejected, so they never reach the log.**
`updateSessionDetails` returns `null` when the finish date is earlier than the last transition's day (`OfflineMediaRepository.kt:716`). The full editor validates this (`ui/detail/CurrentSessionSection.kt:~495`), but the other two paths only check against `startedAt`:
- `StatusChangeSheet`: `ui/common/QuickProgressSheet.kt:882`
- the quick-complete sheet: `ui/common/QuickProgressSheet.kt:150`

Example: pause today, then "Complete" with yesterday as the finish date. Nothing is saved, and there's no message.

**C. The restored state comes from the neighbouring row, not the event itself.**
`statusBefore` uses the previous event's `status` before the event's own `previousStatus` (`OfflineMediaRepository.kt:888`). They agree only while the log is gap-free. `previousStatus` is the recorded truth and should come first. Legacy rows (null) are the only reason for the fallback.

**Also incoherent:**
- Deleting "Finished" removes the status but keeps the progress folded into the same row. The row turns into a plain entry instead of disappearing, and the confirmation text doesn't warn about it.
- Undoing a start (leaving `Planned`) through delete does not clear the `startedAt` that the start stamped.
- Sessions created already Completed/Dropped have no event. Their "Finished" milestone can't be deleted or re-dated from Activity at all.

### 2.2 "Can't edit the first or last progress correctly": CONFIRMED

**First entry (and last entry on legacy finishes).** A progress entry on the start day is folded into the `Started` milestone. Tapping it sets `editingEntryId`, but the editor looks the id up only among `ActivityRow.Entry` rows (`ui/detail/ActivitySheet.kt:176`). The folded update lives in a `Milestone`, the lookup returns null, and the editor closes before it opens. The same applies to the `Finished` milestone fallback (`:365`).

**Last entry on a normal completion.** It is folded into the `Completed` status row, and tapping that row opens only the status editor (`ActivitySheet.kt:288`, a deliberate change in a678a3f). The folded entry's amount and date can't be edited or deleted from anywhere.

**Related:**
- Moving the folded entry's date off the start/finish day silently unfolds it into a separate row.
- Entry dates have no bounds: an entry can be dated before `startedAt` or after the finish.
- Editing or deleting an entry on a Completed session leaves it Completed below the total, or above the total (no headroom by design). No rule decides what should happen.

### 2.3 "Sometimes it's hidden when it shouldn't be": CONFIRMED (4 causes)

1. **The entry point lives inside the dates row.** On the live card, `ActivityAction` only renders when `startedAt`, `finishedAt` or a recency label exists (`ui/detail/CurrentSessionSection.kt:254`). A session with no dates and only undated entries has no way into its Activity.
2. **The threshold is below 2 items** (`ui/detail/ActivityAction.kt:71`). A single mistaken Pause on a session without a start date hides the only place to delete it. The same goes for a single entry.
3. **The sheet closes itself.** Deleting down to 1 item makes `ActivityAction` return early while the sheet is open (it lives inside it). The "don't close on delete" comment at `:73` no longer holds.
4. **The status filter** (see 2.1A) hides real transitions: start, reopen, un-drop, back to Planned.

Main screen:
- Progress rows are **off by default** for every media type (`TimelineVisibility.historyMediaTypes = emptySet()`), so a fresh install only shows milestones.
- Unknown-date completions and statuses never appear, and the recap doesn't count them.
- Same-day pause/resume pairs are hidden on Timeline but shown in the sheet.

---

## 3. Other findings

### Correctness / coherence

| # | Finding | Where |
|---|---|---|
| C1 | The counter on the Activity link (`updates + visibleEvents`) doesn't match the rows: a folded row counts twice and milestones count zero. | `ActivityAction.kt:77` |
| C2 | Status date bounds in the sheet include hidden events and unknown-date placeholders. Save is disabled with no reason given, and the repository no-ops silently on the same check. | `ActivitySheet.kt:205`, `OfflineMediaRepository.kt:940` |
| C3 | `StatusEventEditor` shows an unknown-date event's placeholder as a real date and can't set it back to unknown (the entry editor can). | `ActivityEntryEditor.kt:461` |
| C4 | Status labels map every non-pause/terminal status to "Resumed", so a start or a return to Planned would read as "Resumed" if unhidden. | `ActivitySheet.kt:728` |
| C5 | Activity and Timeline duplicate the folding logic (start/finish/completion claim) in two implementations that already differ: the sheet folds on unknown-date completion events, Timeline requires known dates. | `ActivitySheet.kt:313`, `TimelineBuilder.kt:103` |
| C6 | Mutations that fail (`return null` or a silent `return`) never reach the UI: status change, status re-date, entry edit with bad input. | repository + `HomeViewModel.kt:626,695,740,826` |
| C7 | Correcting progress downwards in the editor trims the newest entries and then lowers the baseline, with no row and no notice. This matches the spec, but the user can't see history being rewritten. | `OfflineMediaRepository.kt:~2163` |

### Usability (sheet)

- The list is ordered by **write time** while the labels are **dates**. A back-dated entry sits at the top with an older date and a smaller running total. The code notes this as deliberate, but it reads as a bug.
- `Started` is pinned to the bottom and `Finished` to the top regardless of when they were written.
- Three body composables (`Entry`/`Status`/`Milestone`) duplicate the same two-column layout.
- There is no way to add a back-dated entry or a missing transition from Activity; it can only correct.
- Delete sits in the editor behind a confirm dialog, *and* there is an undo snackbar. That's two safety nets. The snackbar can also be hidden behind the modal sheet.
- 460dp max height with the sheet's own gestures swallowed: fine, but long histories have no date grouping or year separation.
- The item row and past-session icon look nothing alike (a quiet text link vs a 32dp tonal disc) for the same surface.

### Usability (Timeline)

- Tapping a row opens the item, not the session's Activity, so there's no path from "that entry is wrong" to fixing it.
- Showing progress is a per-type opt-in hidden in a settings sheet, with no hint on the main screen that rows are hidden.
- Undated events have no presence at all, not even a count.

### Tests

- `ActivityRowsTest` covers row building, but nothing tests "the sheet can open an editor for every clickable row". That test would have caught 2.2.
- `deleteSessionStatusEvent` has no unit test for the hidden-latest-event cases in 2.1A.

---

## 4. Recommended direction (for the fix phase)

1. **One derivation, two scales.** Build a single `core/activity` function that turns a session into display rows: filtering, folding, running totals. The sheet uses it per session; `TimelineBuilder` maps its output per library. This fixes C5 and the three-filter disagreement at the root.
2. **Show every transition that changed the state**, including start, reopen, and back to Planned, with correct labels. Only same-day pause/resume noise stays collapsed, and it collapses identically on both surfaces.
3. **Delete means "revert this transition"** based on its own `previousStatus`. If it isn't the latest, say so in the confirmation: "the session stays X".
4. **A folded row is two targets:** edit the entry *and* the transition from one editor (or a two-section editor). Never a lookup that can miss.
5. **Always offer the Activity entry** once there is ≥1 row (entry, event, or finish), outside the dates row.
6. **Validate in one place** (repository) and return a reason. All three status paths share the same check and show the message.
7. **Completed-vs-progress rule (decided 2026-09-17):** editing or deleting an entry so a Completed session falls below its total reopens the session.
8. Timeline: rows open the item scrolled to that session's Activity, and history visibility gets a visible affordance.

Suggested order: 1 → 3/4/6 (bugs) → 2/5/7 (behaviour) → sheet redesign → Timeline redesign.

**Progress:**
- 1 done (`core/activity/SessionActivity.kt`), which also delivered 2, the Activity-link part of 5, and the first-entry half of 2.2.
- Bug fixes: 2.1A (later rows that change nothing no longer make a delete historical, and are removed with it), 2.1B (status and completion sheets rule out dates the repository refuses; undated transitions no longer block), 2.1C (a transition's own `previousStatus` wins), the folded completion entry is reachable from its transition's editor, the delete confirmation says whether the session changes, 7 (reopen below total, with undo), and 2.3.1 (the Activity link shows on undated sessions). Also: Material date pickers in Activity now convert in UTC.
- Second round: C3 (status changes can be marked undated; an undated ending clears the snapshot finish date), C6 (refused writes surface a snackbar; the status editor names the allowed range; only dated neighbours bound a day), deleting the newest start returns to Planned and clears the start date it stamped.
- Sheet redesign: date order grouped by month, date column instead of the rail, totals on every progress row and in the editor, rating on completions, one editor per row (closes the start-transition gap).
- 2026-09-17: Timeline redesign. Recap card removed; month chapters and diary rows; rows open the session's Activitat; three row tiers (endings, state changes, progress); progress history stays off by default with a visible note and link when hidden; undated rows kept in a collapsed section. Fixed a start shown twice when `startedAt` and the opening transition drifted apart (starting now dates the transition on the start date, and editing either moves both).
