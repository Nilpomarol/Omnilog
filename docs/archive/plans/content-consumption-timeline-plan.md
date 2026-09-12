# Omnilog Content Consumption Timeline Plan

## Purpose

This document defines the delivered content consumption timeline for Omnilog and records the design decisions behind it.

The timeline should make the user's local history readable as a sequence of meaningful consumption events: progress logged, sessions started, revisits begun, and sessions completed. It should not become a generic audit log of metadata edits or duplicate the cover carousels already present on Home.

Status: delivered after `docs/omnilog-stats-improvement-plan.md`.

## Current App Context

Omnilog already has most of the data needed for a useful first version:

- `MediaItem` provides title, cover, media type, creators, and progress total.
- `TrackingSession` provides status, current progress, personal rating, notes, platform, start and finish dates, revisit number, and last-update time.
- `ProgressUpdate` provides a dated cumulative progress value, creation time, known-date flag, and objective-counting flag.
- `TrackedMedia` joins an item with its sessions and progress updates.
- `ProgressHistoryAction` already orders progress updates within a session and derives deltas from cumulative values.
- Home already presents the current lifecycle through in-progress, planned, paused, and recently completed carousels.
- Stats already uses pure Kotlin calculators over the same domain models.

The product and UX backlog deliberately removed a generic recent-activity carousel because it repeated the same covers as `Ara mateix`. It records the intended follow-up: if activity returns, it should be a compact feed that explains what changed and when.

## Product Definition

The timeline represents dated consumption facts, not every mutation made to a saved item.

The first version should include:

- progress recorded for a session
- a session starting when `startedAt` is known
- a revisit starting, identified by its position in the surviving chronological session order
- a completed session when `finishedAt` is known

It should not create standalone historical events for:

- metadata refreshes or edits
- collection or ownership changes
- rating or note changes
- pause, resume, planned, or dropped status changes

Those values do not currently have trustworthy event timestamps. `TrackingSession.updatedAtEpochMillis` changes when several fields are edited, so it must not be presented as the date when consumption occurred.

## Event Presentation

Each entry should show only information that helps identify and understand the event:

- cover thumbnail
- media title
- media-type accent
- event description
- progress delta and resulting total when trustworthy
- session/revisit number when relevant
- personal rating on a completion when available
- event date

Examples of the intended density:

- `Dune · +32 pàgines · 180 de 412`
- `Severance · 2 episodis · total 7 de 9`
- `Hades · Has començat una nova partida`
- `Fullmetal Alchemist · Completat · 9/10`

Creators, genres, collections, provider scores, and synopsis text should stay out of timeline rows. They are available from the detail page and would make the feed harder to scan.

## Event Derivation Rules

### Progress

`ProgressUpdate.progressValue` is cumulative, not a delta. Updates must be ordered within each session by:

1. `loggedAt`
2. `createdAtEpochMillis`
3. `id`

For updates after the first known baseline, calculate the difference from the previous cumulative value.

- Positive differences can be shown as `+N`.
- Zero and negative differences are maintenance/correction records. They become the baseline for later deltas but do not create timeline entries.
- A first update has no trustworthy baseline. Prefer `Progrés registrat · 180 pàgines` over claiming `+180 pàgines`.

If a correction is the final progress record on the completion date, it supersedes earlier progress rows for that date and supplies the corrected completion total. This keeps corrections available in the detail history without contaminating the consumption timeline.

Progress records created as part of importing or registering an older completed session must not appear as new activity on the import date. Prefer the historical completion event when a real `finishedAt` exists. The existing `countsTowardObjectives` behavior can help suppress current synthetic entries, but it is not a general event-source model and should not be given broader semantics.

### Start And Revisit

Create a start event only when `startedAt` exists. Derive its displayed visit number from the session's current position in `TrackedMedia.orderedSessions`; do not expose the stored `sessionNumber` as a user-facing count. The stored value remains a stable allocation/undo identity and may legitimately contain gaps after deletes. Use the same stable event key before and after deletion-driven Start/Revisit reclassification.

Do not infer a historical start date from `updatedAtEpochMillis`.

### Completion

Create a completion event only when:

- the session status is `Completed`
- `finishedAt` exists

When a final progress update and completion occur for the same title, session, and date, merge them into one completion entry. The merged row may show final progress and personal rating.

When the first progress update lands on the session's start date, merge it into the start or revisit milestone, matching the item's activity sheet. If a session starts and completes on the same day, completion claims the shared update first. Milestone progress remains visible even when standalone progress-history rows are disabled.

A completed session without `finishedAt` has no honest position on a dated timeline. It may be listed in a separate undated group only if that group proves useful in real use.

### Unknown Dates And Ordering

Progress updates with `hasKnownDate == false` stay available in the item's activity sheet so the user can correct or delete them, but do not appear in the library-wide timeline. Without a trustworthy day they have no honest position in a chronology.

Days are newest first. Within one day, order timestamped events by when they were recorded, newest first, matching the item's activity sheet. Starts use their recorded transition timestamp; sessions created directly in progress fall back to their first child activity or session timestamp. Legacy milestones whose sequence cannot be recovered use semantic bookends. Event kind and stable identity are otherwise only deterministic tie-breakers.

## Placement And Navigation

The timeline should remain part of the Home hierarchy:

- Add a compact `Activitat recent` preview after the Home analytics card and before the paused and recently completed carousels.
- Show three to five lightweight entries plus `Veure tota l'activitat`.
- Open a full-screen `Timeline`/`Activitat` destination as a Home drill-in.
- Keep Home selected in the bottom navigation while the full timeline is open.
- Use the dashboard accent in the top bar and standard back navigation.
- Tapping an entry opens the corresponding media detail.
- Back from detail returns to the timeline with filters and scroll position preserved.

Do not add a sixth bottom-navigation item. Timeline is a history/reflection surface, not a media-library root. It should remain separate from Stats: Stats aggregates history, while Timeline shows the underlying sequence of events.

## Full-Screen Structure

Recommended first-screen hierarchy:

1. Page title and short context if needed.
2. Horizontally scrollable media filter: all, anime, books, movies/TV, games.
3. Compact period filter: all time or a year.
4. Newest-first timeline grouped by day, with month separators for older entries.

Avoid adding status, genre, creator, provider, collection, or arbitrary date-range filters in the first version. They do not materially improve the core history-reading task.

## Visual Direction

The feature should reuse Omnilog's existing visual system:

- soft dark app background and panel colors
- dashboard orange for page-level actions
- media-section accents for entry markers
- Lato-based Material typography
- thin `AppLine` dividers and restrained borders
- compact 8–12dp corner radii
- cover-led identification without large poster cards

Timeline rows should sit directly on the page or in a single containing surface, separated by dividers. Avoid a tall card for every event. A small cover, title, action sentence, and secondary progress/date line should provide enough hierarchy.

## States And Behavior

### Loading

Represent initial loading explicitly so the screen does not briefly show an empty state before Room emits. Use a small number of skeleton rows or the shared status-panel treatment.

### Empty

When the library has no timeline activity, use `OmnilogEmptyState` with a route back to current or planned content. Do not imply that the library itself is empty if saved titles exist.

### Filtered Empty

Explain that the selected filters have no activity and provide a visible `Esborra els filtres` action.

### Grouped

Use `Avui`, `Ahir`, and locale-formatted dates for recent day headers. Add month/year separators as the feed extends backward.

### Large Timelines

Use `LazyColumn`, stable event keys, remembered filter/scroll state, and bounded Home preview calculation. The existing all-items flow is sufficient for the initial implementation. Measure it against the real library before adding database complexity.

If event volume later becomes expensive, add a Room projection ordered by event date, suitable date indexes, and keyset pagination. Do not introduce Paging or another dependency before measurement shows it is necessary.

## Technical Design

The first version should use pure Kotlin presentation logic over existing domain models, matching the stats architecture and avoiding a Room schema change.

Suggested package:

- `app/src/main/java/com/nilpo/contenttracker/core/timeline/`

Suggested models:

```kotlin
sealed interface TimelineEntry {
    val stableKey: String
    val mediaItemId: Long
    val date: LocalDate?
}

data class TimelineFilters(
    val mediaTypes: Set<MediaType>,
    val year: Int?,
)

data class TimelineDayGroup(
    val date: LocalDate?,
    val entries: List<TimelineEntry>,
)
```

Suggested responsibilities:

- `TimelineBuilder`: derive, merge, sort, and group entries.
- `TimelineScreen`: render filters, groups, and screen states.
- reusable preview and row composables shared with Home.

Keep timeline derivation out of composables so cumulative-delta, merging, unknown-date, import, and filter behavior can be unit tested.

Navigation will require:

- a Timeline app destination
- a Home preview callback
- a detail return target for Timeline
- restoration of timeline filters and list position after detail navigation

## Implementation Sequence

1. Add timeline presentation models and `TimelineBuilder`.
2. Test progress deltas, first updates, corrections, same-day merges, revisits, imports, and unknown dates.
3. Build reusable timeline row and day-group components.
4. Add the full Timeline screen and media/year filters.
5. Add the compact Home preview after analytics.
6. Add destination, back handling, detail return behavior, and state restoration.
7. Add Catalan strings and content descriptions.
8. Verify empty, filtered, sparse, dense, and 200% font-scale states on a device or emulator.
9. Measure large-history performance before considering new DAO queries or indexes.

## Exit Criteria

- Timeline entries describe actual dated consumption without treating arbitrary edits as activity.
- Progress deltas are correct within each session and first/synthetic records are not overstated.
- Same-day completion and final-progress events do not produce noisy duplicates.
- Unknown and missing dates are handled honestly.
- Home exposes a compact preview without displacing its daily resume/plan hierarchy.
- The full timeline supports media and year filtering and returns correctly from item detail.
- Empty, filtered, large, and 200% font-scale states remain readable and actionable.
- No Room schema change or new dependency is added unless performance evidence requires it.

## Risks And Open Decisions

**Implementation note (2026-07-20):** The pure-Kotlin builder, focused derivation tests, shared Home/full-screen rows, filters, navigation, state restoration, Catalan copy, loading/empty treatments, accessibility semantics, deletion-safe dynamic visit numbering, correction suppression, persistent per-media-type visibility controls, and progress-history editing for every media type are implemented without a schema or dependency change. Editing a cumulative progress value or date atomically recalculates the owning session's current progress. The full debug unit suite and debug APK build pass. A connected device verified the dense Home preview, full timeline, media/year filtering, filtered-empty state, sparse history, detail return with filters/position retained, Home bottom-nav selection, the five-type configuration sheet and its persisted selection, a non-book history editor, and layouts at 100% and 200% font scale; the device settings and test selections were restored afterward. Live empty-library/loading and an on-device unknown-date group were not exercised to avoid replacing the device's real library. Those are residual QA scenarios, not incomplete feature scope.

- Imported sessions may have useful completion dates but synthetic progress-update dates.
- Exact consumption times are unavailable; most historical data is day-level.
- Rating, notes, platform, and status are mutable session snapshots rather than timestamped events.
- A future audit-quality history would require a dedicated activity-event table with event type, source, timestamp, and payload snapshots. That is intentionally outside this feature's initial scope.
- Media-specific revisit wording should remain concise; generic `Nova sessió` is preferable if localized variants become noisy.
