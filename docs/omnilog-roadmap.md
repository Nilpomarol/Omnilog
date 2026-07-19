# Omnilog Roadmap

## Product Direction

Omnilog is a native Android/Kotlin personal media tracking app for anime, books, movies/TV, and games. Its purpose is to replace fragmented tracking across external services with a durable local media log where provider data enriches the user's library but does not own it.

The app should remain local-first:

- Room is the source of truth.
- User-entered tracking state is authoritative.
- Imports are additive and skip duplicates.
- Backup restore is the only destructive replace flow.
- Metadata refresh/linking must preserve sessions, progress history, personal ratings, reviews, ownership, and collections unless the user explicitly chooses otherwise.
- Provider-specific import actions stay scoped to their section pages:
  - Anime: MyAnimeList XML.
  - Movies/TV: IMDb CSV.
  - Books: StoryGraph CSV.

Avoid major architecture rewrites unless they are explicitly requested or clearly needed for a focused feature.

## Current Foundation

The app already has:

- core navigation, dashboard, section pages, detail pages, and Room persistence
- sessions, progress updates, personal ratings, collections, ownership, and metadata summaries
- backup export/import/restore
- IMDb CSV import
- StoryGraph CSV import
- MyAnimeList XML import
- metadata linking for Movies/TV through TMDB
- metadata linking for Books through OpenLibrary/Google Books
- metadata linking for Anime through AniList/Jikan/MAL id handling
- metadata refresh confirmation with field-by-field selectable overwrites
- AniList anime linking that preserves MAL ids where available
- optional official MyAnimeList API v2 rating enrichment through `MAL_CLIENT_ID`, with Jikan fallback
- manual external rating management from the detail page overflow menu under `Puntuacions`

The latest known build state from the previous implementation session was healthy: `.\gradlew.bat assembleDebug` passed after heap settings were raised.

## Architecture Map

Core stack:

- Kotlin
- Jetpack Compose
- Room
- Gradle wrapper
- local repository layer

Important files:

- `app/src/main/java/com/nilpo/contenttracker/core/repository/MediaRepository.kt`: repository contract, including metadata refresh preview/apply and manual external rating APIs.
- `app/src/main/java/com/nilpo/contenttracker/core/repository/OfflineMediaRepository.kt`: local implementation for imports, metadata refresh/linking, external ratings, backup serialization.
- `app/src/main/java/com/nilpo/contenttracker/core/database/dao/MediaDao.kt`: Room DAO for media, sessions, progress, collections, ratings, and tracking.
- `app/src/main/java/com/nilpo/contenttracker/ui/ContentTrackerApp.kt`: top-level Compose orchestration, navigation, menus, import dialogs, metadata refresh confirmation.
- `app/src/main/java/com/nilpo/contenttracker/ui/home/HomeViewModel.kt`: UI bridge to repository operations.
- `app/src/main/java/com/nilpo/contenttracker/ui/detail/DetailScreen.kt`: detail screen orchestration.
- `app/src/main/java/com/nilpo/contenttracker/ui/detail/DetailQuickActionsSection.kt`: quick actions and external ratings dialog.
- `app/src/main/java/com/nilpo/contenttracker/core/repository/AniListMetadataRepository.kt`: AniList search/details plus MAL/Jikan enrichment and MAL id preservation support.
- `app/src/main/java/com/nilpo/contenttracker/core/repository/MyAnimeListXmlImport.kt`: MAL XML parser.
- `app/src/main/res/values/strings.xml`: Catalan UI strings.
- `docs/omnilog-ui-design-v1.md`: UI/product design direction.
- `docs/omnilog-stats-system-plan.md`: proposed stats system/page plan.
- `docs/omnilog-product-ux-backlog.md`: current ordered product and UX implementation backlog.
- `docs/omnilog-stats-improvement-plan.md`: post-backlog stats refinement plan.
- `docs/omnilog-content-consumption-timeline-plan.md`: post-stats content consumption timeline plan.

Core tables/entities:

- `media_items`
- `media_credits`
- `tracking_sessions`
- `progress_updates`
- `external_ratings`
- `media_collections`

External metadata/data providers:

- AniList GraphQL
- official MyAnimeList API v2 when `MAL_CLIENT_ID` is configured
- Jikan REST API as MAL fallback
- TMDB and OMDb for Movies/TV
- OpenLibrary and Google Books for Books
- RAWG for Games

## Roadmap

### Current Execution Priority

The active implementation order is:

1. Complete the remaining in-progress and todo items in `docs/omnilog-product-ux-backlog.md`.
2. Next, implement `docs/omnilog-stats-improvement-plan.md` in its documented order.
3. Then, implement `docs/omnilog-content-consumption-timeline-plan.md`.
4. Resume the remaining roadmap work below unless priorities are explicitly changed.

The stats MVP already exists. Its next phase is refinement: trustworthy definitions, a stronger information hierarchy, valid chart comparisons, focused interaction, and visual/accessibility polish. It should not be expanded with optional statistics until the core refinement plan is complete.

### 1. Device And Regression QA

Goal: verify the current user-facing flows before adding larger features.

Tasks:

- Install and run the latest debug APK on a device/emulator.
- Open the detail page overflow menu.
- Verify metadata refresh confirmation can preview, select, and apply overwrites.
- Verify manual external rating management can add, edit, delete, and set primary ratings.
- Confirm provider-specific imports still appear only in their relevant section pages.
- Check that no import or metadata flow overwrites user sessions/progress/rating data unexpectedly.

Exit criteria:

- No known crash in the tested flows.
- Any visual or interaction issues are documented before larger feature work continues.

### 2. Manual External Ratings Polish

Goal: make the existing first-pass external rating feature feel coherent and reliable.

Tasks:

- Improve dialog layout density and labels.
- Show current score formatting clearly in rows.
- Add source-specific default max scores where appropriate.
- Decide whether custom/free-text sources are needed beyond the existing enum.
- Strengthen validation so score/max score combinations are sensible.

Open questions:

- Should custom rating sources be supported?
- Should source presets imply a max score?
- Should provider refresh preserve a user-selected primary external rating by default?

### 3. Primary External Rating Model

Goal: make primary external rating selection unambiguous.

Current issue:

- The UI detects the primary external rating by score/max score. If two sources share the same score and max score, multiple rows can appear effectively primary.

Possible direction:

- Store primary external rating source/id explicitly instead of relying on denormalized score matching.
- Define fallback behavior when the primary rating is deleted.
- Keep denormalized primary fields on `media_items` only as display/cache fields if still useful.

Exit criteria:

- Primary rating identity is deterministic.
- Deleting or editing ratings has predictable fallback behavior.

### 4. Metadata Linking Overwrite Confirmation

Goal: bring metadata linking up to the same safety standard as metadata refresh.

Current issue:

- Metadata refresh has preview/apply with selectable overwrite fields.
- Metadata linking can still overwrite metadata directly.

Tasks:

- Reuse or adapt the metadata refresh preview model for linking.
- Show old/new field values before overwriting existing metadata.
- Apply safe fills directly when there are no overwrites.
- Preserve user sessions, progress, ratings, reviews, ownership, and collections.

Exit criteria:

- Linking and refresh both give the user control over provider overwrites.

### 5. Stats Feature Refinement

Status: the StoryGraph-inspired, Omnilog-native stats MVP is implemented as a Home drill-in. This refinement is the next workstream after the product and UX backlog is complete.

Goal: make the existing stats page trustworthy, selective, interactive, polished, and memorable without turning it into a primary or overly complex app surface.

Reference:

- See `docs/omnilog-stats-improvement-plan.md` for the ordered implementation plan.
- Keep `docs/omnilog-stats-system-plan.md` as the original MVP and data-definition reference.

Required direction:

- Keep Stats as a Home drill-in, not a sixth bottom-navigation item.
- Reconcile title, session, period, and comparison definitions before adding new statistics.
- Build a concise top-level summary from the strongest existing metrics.
- Remove visual comparisons between incompatible units such as pages, episodes, minutes, and hours.
- Make key charts inspectable and connect useful selections to their contributing titles.
- Adapt sections to single-medium filters and sparse data instead of rendering redundant one-category charts.
- Preserve the existing local-first calculator design and avoid a new chart dependency unless interaction requirements clearly justify one.

Implementation order:

- `STATS-01`: trustworthy definitions, periods, and comparisons.
- `STATS-02`: concise top-level summary and hierarchy.
- `STATS-03`: valid and clearer chart forms.
- `STATS-04`: focused inspection and drill-down interactions.
- `STATS-05`: filter-aware, sparse-data, and accessibility polish.

Exit criteria:

- Every number and chart has an interpretable unit, population, period, and comparison basis.
- The first viewport provides a useful summary without requiring a long scroll.
- No chart implies a comparison between incompatible units.
- Key chart values and contributing titles can be inspected accessibly.
- Single-medium, sparse, dense, and 200% font-scale states are verified on a device or emulator.

### 6. Content Consumption Timeline

Status: planned as the next feature workstream after Stats Feature Refinement.

Goal: make the user's local consumption history readable as a chronological feed of progress, starts, revisits, and completions without treating arbitrary item edits as activity.

Reference:

- See `docs/omnilog-content-consumption-timeline-plan.md` for the product, data, UI, and implementation plan.

Required direction:

- Add a compact recent-activity feed to Home after analytics, with a drill-in to the full timeline.
- Keep Timeline as a Home destination rather than adding a sixth bottom-navigation item.
- Derive timeline entries from existing `TrackedMedia`, `TrackingSession`, and `ProgressUpdate` models in pure Kotlin for the first version.
- Calculate progress deltas within each session and handle first, corrected, imported, and unknown-date updates conservatively.
- Merge same-day final progress and completion into one meaningful entry.
- Preserve timeline filters and scroll position when opening an item and returning from detail.
- Avoid a Room migration, new event table, or paging dependency unless measured performance or audit-history requirements justify one.

Implementation order:

- Define and test timeline presentation models and event derivation.
- Build reusable timeline rows, day groups, filters, and screen states.
- Add the full Timeline Home drill-in and detail return behavior.
- Add the compact Home preview after analytics.
- Complete Catalan copy, accessibility, large-history, and 200% font-scale QA.

Exit criteria:

- Entries reflect trustworthy consumption dates rather than generic session update timestamps.
- Progress, start, revisit, and completion events are concise, correctly ordered, and not duplicated.
- Empty, filtered, unknown-date, sparse, and large histories are handled clearly.
- Home gains useful recent context without duplicating its cover carousels or disrupting the resume/plan hierarchy.
- The full timeline opens item detail and returns with its context intact.

### 7. Goals Redesign

Status: delivered. The four surfaces named below — goal form, Profile goals, Home summary, and the
Profile page around them — were redesigned and verified on device.

What shipped, in the order it was built:

- **Goal form as a sentence.** The editor reads `Vull llegir [40] [llibres] durant [el 2026]`, with
  each underlined slot opening a picker. The grammar and formatting live in `ObjectiveSentence.kt`,
  which is plain Kotlin and therefore unit-tested (`ObjectiveSentenceTest`). `episodis` is ambiguous
  between anime and series, so the unit picker splits *format* from *what is counted* rather than
  validating an invalid combination after the fact — an unrepresentable state instead of a rejected
  one. Panel and sentence carry `heightIn(min=)` floors so the sheet does not jump as slots change.
- **Profile goals** keep the card-and-bar form, with an expected-progress tick on the bar.
- **Home summary** is a strip of at most four rings, worst pace first, with a notch marking where
  progress should be today. Rings on Home and bars on Profile deliberately keep the two surfaces
  from being copies of each other: Home is a glance, Profile is where goals are managed.
- **Profile page.** Five stacked cards became a seamless page header plus two panels. See the
  section below.

Deliberately not done: goal detail with contributing titles, and pause/archive states. Both remain
open from the direction list.

#### Profile page

The page was five cards that restated each other: a hero, `La teva biblioteca`, `La teva
col·lecció`, the goals, and `Ara mateix`. `Títols` was by definition the sum of the per-type counts
one card below it, and `Ara mateix` repeated the Home dashboard.

It is now a header and two panels:

- The header is seamless — a full-bleed collage of the reader's own cover art, dimmed and scrimmed,
  with the avatar and name riding up onto it. No card, so the first actual card on screen is data.
- Totals and the per-type breakdown share one panel, because they are the same fact at two
  resolutions.
- `Ara mateix` was removed.

Editing happens in place on the header. Name and bio become fields styled exactly like the text they
replace, marked only by a rule beneath; the avatar takes a badge; the accent swatches sit below; the
top bar swaps to `Cancel·la` / `Desa`. There is no preview to build because the thing being edited is
the thing on screen — the previous form sat below the fold, so tapping edit left an unchanged page.

The profile picture can be any cover in the library. The sheet draws the most recent twelve and
searches the rest; gallery, URL, and remove sit beneath. The URL field and its errors moved here out
of the page body.

**Known inconsistency:** the photo is written as soon as it is chosen, while name, bio, and accent
are drafts committed on `Desa`. `Cancel·la` therefore does not restore a replaced photo.

#### Media-type icons and colours

Films and series had shared `ic_nav_movies_tv` and were told apart by accent colour alone, which
fails wherever they sit side by side. They now have their own glyphs (`ic_media_movie`,
`ic_media_series`) used everywhere a single media type is named — Profile collection, goal rings,
goal cards. `Cinema i TV` is one navigation section covering both, and its tab uses the film mark
alone; a composite of the two glyphs was built, tried on device, and rejected as busy. The old film
strip has no remaining references and was deleted.

Colours are now tokens: `OmnilogColors.Movie` (the section's own teal) and `OmnilogColors.Series`.
The series colour had been a bare `0xFF5E8FC4` in `StatsUiUtils`, byte-identical to
`OmnilogColors.InProgress` — a series bar and an in-progress bar were the same colour by accident.

The navigation bar no longer draws a tinted pill behind the selected tab; the accent on the icon,
plus `appInk` and a heavier weight on the label, carry selection on their own.

#### Fixed along the way

- **A replaced profile photo did not appear.** Every image was written to `filesDir/profile/avatar`,
  so the stored path never changed: Compose saw no state change and Coil served the previous bitmap
  from its path-keyed cache. Images now get a unique filename and stale ones are cleaned up. This
  affected the gallery and URL paths too, not just the new cover picker.
- **The cover grid dragged the sheet.** Overscroll at either end of the grid was handed to the
  bottom sheet and started closing it. A `NestedScrollConnection` now consumes what the grid does
  not use, in `onPostScroll`/`onPostFling` so the grid still scrolls normally.

Still open: a drag starting on a cover may still fire its tap, selecting a cover during a scroll.
Observed once on device and not reproduced deliberately; if confirmed, the fix is a gesture detector
that respects drag slop instead of `Modifier.clickable`.

Goal: make personal goals easier to create, understand, maintain, and act on while keeping them secondary to the resume-and-plan flow on Home.

Required direction:

- Reframe the Profile experience around clear goal definitions: metric, medium, period, target, and current progress.
- Offer sensible presets for common goals while retaining a custom option for users who need a different period or unit.
- Make active, achieved, expired, and paused goals distinct, with explicit actions to edit, pause, archive, or delete them.
- Show current value, target, remaining amount, and over-target progress together; never rely on a percentage alone.
- Make goal progress auditable by showing the titles or progress contributing to the current value where practical.
- Keep the Home presentation as a compact summary and let the full goals view handle management and detail.
- Align goal date and counting rules with Stats so the same completion and progress data does not produce conflicting totals.
- Preserve existing objective data during any migration and avoid introducing a new dependency for the first redesign pass.

Implementation order:

- Define goal states, metrics, periods, and counting rules.
- Prototype the Profile list and guided create/edit flow.
- Add goal detail with progress explanation and contributing titles.
- Refresh the compact Home summary and empty/expired states.
- Add migration, edge-case tests, Catalan copy, accessibility, and 200% font-scale QA.

Exit criteria:

- A user can create a useful goal without understanding the underlying data model.
- Every goal clearly communicates what is counted, for which medium, over what period, and against which target.
- Active and historical goals are easy to distinguish and manage.
- Goal progress matches the corresponding Stats definitions for the same period.
- Home remains compact while Profile provides the complete goals experience.

### 8. Import And Metadata Test Coverage

Goal: reduce regressions in the highest-risk local-first flows.

Tasks:

- Add a MAL XML fixture test using the provided MAL export shape.
- Test duplicate detection after AniList linking with preserved MAL id.
- Test metadata refresh diff generation.
- Test selective metadata refresh apply behavior.
- Add import tests for IMDb CSV and StoryGraph CSV edge cases.

Exit criteria:

- Core import/link/refresh behavior is covered by automated tests.
- Regression risk is lower before future metadata/provider changes.

### 9. MAL API Follow-Up

Goal: make official MAL integration behavior clearer and safer.

Tasks:

- Confirm official MAL API behavior with a configured client id on device.
- Add conservative handling for `429` or other rate-limit responses if needed.
- Decide whether Jikan remains a permanent fallback.
- Confirm whether MAL should be preferred only for anime score/user count or for more metadata fields.

Exit criteria:

- MAL enrichment behavior is documented and predictable.
- The app still works without `MAL_CLIENT_ID`.

### 10. UI System Evolution

Goal: continue the visual direction from `docs/omnilog-ui-design-v1.md` in small, reversible slices.

Likely next slices:

- refine shared preview/detail metadata hero
- improve media list rows
- keep Home dashboard cover-led and scannable
- move edit/create flows toward modal patterns where appropriate
- keep Stats consistent with the dashboard visual language
- defer settings/config until core flows are stable

Design guardrails:

- dark-mode-first
- soft dark base, not pitch black
- section accents as highlights
- cover-led media surfaces
- user tracking state kept visually distinct from provider metadata
- no heavy nested card stacks

### 11. Workspace And Release Hygiene

Goal: keep local setup and repository state safe.

Tasks:

- Avoid committing local API keys/client ids from `gradle.properties`.
- Keep provider credentials in Gradle properties or environment variables only.
- Review unrelated dirty files before commits.
- Keep old session notes out of the project root.
- Prefer focused docs in `docs/`.

## Known Risks

- Manual external rating UI is functional but not polished.
- Metadata linking still needs overwrite confirmation.
- Older AniList-linked anime may lack preserved MAL ids until refreshed/relinked.
- Gradle properties may contain local credentials and should not be committed casually.
- External rating score/max validation is minimal.
- Deleting the primary external rating currently falls back to the first remaining rating.
- Stats that depend on `finishedAt`, `progressTotal`, genres, creators, or language will be incomplete when those fields are missing.
- Progress updates store cumulative values, so progress-over-time stats need careful delta calculation.
- External APIs may rate-limit or omit expected fields.

## Technical Debt

- `ContentTrackerApp.kt` coordinates a lot of navigation, dialogs, imports, metadata linking, and back handling. Refactor only when a focused extraction clearly helps a feature.
- `DetailQuickActionsSection.kt` carries quick actions plus the external ratings dialog.
- Primary external rating is denormalized on `media_items` instead of modeled by id/source.
- Refresh diff display uses formatted string comparisons for many fields.
- Import and metadata flows need automated tests.

## Project Guardrails

- Do not refactor stable systems casually.
- Do not redesign architecture unless explicitly requested.
- Preserve current naming conventions.
- Ask before introducing new frameworks or dependencies.
- Do not make imports destructive.
- Do not show provider-specific imports outside their section pages.
- Do not overwrite user/session/progress/review/ownership/collection data during metadata work.
- Do not remove Jikan fallback unless explicitly approved.
- Do not commit local secrets or unrelated dirty files.
