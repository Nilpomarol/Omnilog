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

### 5. Stats System MVP

Goal: add a StoryGraph-inspired, Omnilog-native stats page.

Reference:

- See `docs/omnilog-stats-system-plan.md`.

Planned direction:

- Stats should first be a Home drill-in, not a sixth bottom navigation item.
- The first implementation should calculate stats in pure Kotlin from existing `TrackedMedia` domain data.
- Avoid a Room schema change for the MVP.
- Avoid a chart dependency for the MVP; use simple Compose-built charts.

MVP tasks:

- Add stats domain models and a pure Kotlin calculator.
- Add tests for completion counts, rating distribution, progress totals, and progress-update delta handling.
- Add a Home stats CTA/module.
- Add a full stats screen as a Home drill-in.
- Add period filtering: all time, this year, last 12 months.
- Add media filtering: all, anime, books, TV/movies, games.
- Add overview KPIs, monthly completion/activity, rating distribution, status breakdown, progress totals, top genres, top creators, language breakdown, best-rated items, and most-revisited items.

Open questions:

- How should multiple sessions per item affect rating averages?
- How should incomplete metadata be surfaced?
- How should progress corrections be handled in progress-over-time charts?

### 6. Import And Metadata Test Coverage

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

### 7. MAL API Follow-Up

Goal: make official MAL integration behavior clearer and safer.

Tasks:

- Confirm official MAL API behavior with a configured client id on device.
- Add conservative handling for `429` or other rate-limit responses if needed.
- Decide whether Jikan remains a permanent fallback.
- Confirm whether MAL should be preferred only for anime score/user count or for more metadata fields.

Exit criteria:

- MAL enrichment behavior is documented and predictable.
- The app still works without `MAL_CLIENT_ID`.

### 8. UI System Evolution

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

### 9. Workspace And Release Hygiene

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
