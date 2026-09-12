# Omnilog Current Roadmap

Status: Current  
Last reviewed: 2026-09-12

## Product Direction

Omnilog is a native Android personal media tracker for anime, books, movies/TV, and games.

The product remains local-first:

- Room is the source of truth.
- User-entered tracking state is authoritative.
- Provider data enriches but does not own user history.
- Imports are additive and duplicate-aware.
- Backup restore is the normal destructive replace-all flow.
- Metadata work must preserve sessions, progress, ratings, reviews/notes, ownership, and collections unless the user explicitly chooses otherwise.

Avoid broad architecture rewrites unless a focused problem clearly requires them.

## Delivered Foundation

The application already includes:

- Home, media-section browsing, detail pages, and local Room persistence;
- sessions, incremental progress/activity entries, status-event history, personal ratings, collections, and ownership;
- backup export/import/restore;
- IMDb CSV, StoryGraph CSV, MAL XML, and MAL account imports;
- durable import enrichment with retry/pause/resume/cancel/recovery;
- metadata search/link/refresh across supported providers;
- selective metadata overwrite previews with local-override protection;
- explicit primary external-rating identity;
- status-first add flow and book edition selection;
- Activity and library-wide Timeline;
- refined Stats and yearly analysis;
- system/light/dark themes with Omnilog-specific palettes;
- undo/recovery for destructive user actions where implemented.

Treat old dated build-verification notes in archive documents as history, not current evidence. Always validate the current checkout for the task at hand.

## Active Product / Engineering Priorities

### 1. Implement the approved Home redesign

The current implementation still reflects the older Home structure.

Follow [`home.md`](home.md):

- dedicated `Ara mateix` continuation cards;
- compact `Per començar` treatment;
- merge objective/analytics previews into `El teu ritme`;
- compact `Activitat recent`;
- small `Per reprendre` shelf;
- remove permanent `Completats recentment` from Home.

This is the primary active UI direction.

### 2. Improve agent/developer feedback loops

Incrementally add:

- deterministic Compose previews for high-value UI states;
- rendered visual inspection for UI changes;
- screenshot regression testing for selected important screens/components;
- Android CLI / official Android agent tooling where useful.

These changes should improve verification without changing product architecture.

### 3. Improve context locality in oversized orchestration files

Refactor incrementally, preserving behavior:

- reduce unrelated responsibilities in `ui/ContentTrackerApp.kt`;
- move non-Home application operations out of `ui/home/HomeViewModel.kt` when a clear product boundary exists;
- extract cohesive backup/import/metadata implementation concerns from `OfflineMediaRepository.kt` without fragmenting the repository contract unnecessarily;
- keep manual dependency wiring unless a concrete need justifies something else.

Do not turn this into a Clean Architecture or multi-module rewrite.

### 4. Finish remaining add/book-edition gaps

See [`../features/add-and-book-metadata.md`](../features/add-and-book-metadata.md).

The edition-aware search/selection path exists, but full first-class persisted/displayed edition metadata still has gaps. Finish only the remaining pieces verified against the current model/code.

### 5. Provider/regression maintenance

Address MAL or other external-provider issues when real behavior exposes them. Provider changes should remain defensive, preserve local data, and come with focused characterization/regression tests where practical.

## Later / Optional

These are not current blockers:

- deeper Stats drill-downs;
- richer audit-quality history views;
- expanded social functionality beyond the approved social-aware IA foundation;
- more advanced goal-management states;
- broader UI polish after Home alignment;
- additional external tooling only when benchmarks show a real gain.

## Architecture Direction

Preserve the current feature/domain organization and manual composition root.

The main architectural improvement target is **context locality**, not abstraction count.

Good refactors make one feature easier to understand without requiring agents/humans to load unrelated flows. Avoid creating generic coordinators, services, use cases, or interfaces merely to shrink files.

## Validation Direction

Use the targeted validation ladder in [`development-guide.md`](development-guide.md).

For UI work, rendered output is part of completion when tooling permits.

For persistence/import/provider work, correctness and user-data preservation take priority over minimal test execution.
