# PROJECT CONTINUATION DOCUMENT
## Session 13 — 23 May 2026

### 1. PROJECT IDENTITY

- **Project Name:** Omnilog
- **What This Project Is:** Native Android/Kotlin personal media tracking app for anime, books, movies/TV, and games. It is for user-owned local tracking of progress, sessions, ratings, collections, metadata, and imports.
- **Primary Objective:** Maintain a usable local-first Android app that lets the user add, organize, import, link, refresh, rate, and review tracked media without losing manual/user-entered data.
- **Strategic Intent:** Replace fragmented tracking across external services with a durable personal media log where external providers enrich data but do not own it.
- **Hard Constraints:**
  - Android app using Kotlin, Jetpack Compose, Room, Gradle wrapper.
  - Local-first repository architecture; Room is the source of truth.
  - No major architecture rewrites unless explicitly requested.
  - Imports must be additive and skip duplicates; backup import remains the only destructive replace flow.
  - Metadata linking/refresh must preserve user sessions, progress history, personal ratings, reviews, ownership, and collections unless explicitly requested.
  - Provider-specific import actions stay scoped to their section pages:
    - Anime: MyAnimeList XML.
    - Movies/TV: IMDb CSV.
    - Books: StoryGraph CSV.
  - Ask before introducing new frameworks or dependencies.

### 2. WHAT EXISTS RIGHT NOW

- **What is built and working:**
  - Core navigation, dashboard, section pages, detail pages, Room persistence, sessions, progress updates, personal ratings, collections, ownership, metadata summaries, backup/export/restore.
  - External imports for IMDb CSV, StoryGraph CSV, and MyAnimeList XML.
  - Metadata linking for Movies/TV through TMDB, Books through OpenLibrary/Google Books, Anime through AniList/Jikan/MAL id handling.
  - Metadata refresh confirmation flow: if provider data would overwrite existing metadata fields, a modal shows field-by-field old/new values with checkboxes so the user can choose what to overwrite.
  - MAL XML import now tolerates Android XML parser feature differences and accepts the provided MAL XML export.
  - AniList anime linking now preserves MAL ids where available, so future MAL imports and MAL rating enrichment still identify the same anime.
  - Optional official MyAnimeList API v2 rating enrichment is wired through `MAL_CLIENT_ID`; if configured, MAL score and scoring-user count are preferred for anime ratings, with Jikan fallback.
  - Manual external rating management exists on the detail page overflow menu under `Puntuacions`: add, edit, delete, and set main external rating.
  - Gradle heap settings were raised to reduce `Java heap space` failures during Android Studio/Gradle runs.
- **What is partially built:**
  - Manual external rating management is a first implementation, not a polished UX. It is functional but visually/basic-flow polish is still needed.
  - Metadata refresh preview/apply is implemented for refresh, not for metadata linking. Linking can still overwrite metadata directly.
  - MAL official API use is optional and only active when `MAL_CLIENT_ID` is configured. Jikan remains fallback.
  - Import tests/fixtures are still not automated.
- **What is broken or blocked:**
  - No known compile blocker. Latest verified command: `.\gradlew.bat assembleDebug` passed after heap changes.
  - No emulator/device visual QA was run after the manual external ratings dialog.
  - If older AniList-linked anime already lost their MAL id before the preservation fix, the app can only regain it if AniList returns `idMal` again during refresh/link.
- **What has NOT been started yet:**
  - Automated tests for import/link/refresh/external-rating flows.
  - Visual QA for metadata refresh modal and external rating management modal.
  - Polished UX for manual external ratings.
  - Official MAL API rate-limit/backoff UX beyond safe fallback behavior.

### 3. ARCHITECTURE & TECHNICAL MAP

- **Tech stack / tools / platforms:**
  - Kotlin Android app.
  - Jetpack Compose UI.
  - Room database.
  - Gradle wrapper.
  - Local-first repository layer.
  - External metadata APIs: TMDB/OMDb, AniList, MAL/Jikan, OpenLibrary, Google Books, RAWG.
  - GitHub remote: `github.com:Nilpomarol/Content-tracking-android-app.git`.
- **Key data structures, tables, files, or repos:**
  - `MediaRepository.kt`: repository contract; now includes metadata refresh preview/apply and manual external rating APIs.
  - `OfflineMediaRepository.kt`: local implementation for imports, metadata refresh/linking, external ratings, backup serialization.
  - `MediaDao.kt`: Room DAO; now includes external rating CRUD, primary external rating update, and per-item credit query.
  - `ContentTrackerApp.kt`: top-level Compose orchestration, menus, import dialogs, metadata refresh confirmation modal.
  - `HomeViewModel.kt`: UI bridge for repository operations.
  - `DetailScreen.kt`: detail screen orchestration; now opens external ratings manager.
  - `DetailQuickActionsSection.kt`: contains `ExternalRatingsDialog` and external tracking dialog.
  - `AniListMetadataRepository.kt`: AniList search/details plus MAL/Jikan enrichment and MAL id preservation support.
  - `MyAnimeListXmlImport.kt`: MAL XML parser.
  - `strings.xml`: Catalan UI strings.
  - Tables/entities: `media_items`, `media_credits`, `tracking_sessions`, `progress_updates`, `external_ratings`, `external_tracking`, `media_collections`.
- **How the system works end-to-end:**
  1. User navigates by section: Anime, Books, Movies/TV, Games.
  2. User can manually add media, import from provider exports, or search/link metadata.
  3. UI calls `HomeViewModel`, which delegates to `MediaRepository`.
  4. `OfflineMediaRepository` mutates Room tables and emits tracked media through DAO flows.
  5. Metadata providers return `MetadataSuggestion` objects used for add/link/refresh.
  6. Refresh now first builds `MetadataRefreshPreview`, compares provider values to current Room values, and either applies safe fills directly or prompts for selected overwrites.
  7. External ratings are stored in `external_ratings`; the primary displayed external rating is copied onto `media_items.externalRatingScore`, `externalRatingMax`, and `externalRatingVoteCount`.
  8. Backup/export serializes local data; backup import remains the destructive restore path.
- **Naming conventions or standards in use:**
  - Domain enums use PascalCase values: `MediaType.Book`, `TrackingStatus.Completed`, `MetadataSource.Jikan`.
  - UI strings are in `app/src/main/res/values/strings.xml`, mostly Catalan.
  - Provider source enums: `MetadataSource.*`, `ExternalRatingSource.*`, `ExternalTrackingSource.*`.
  - Compose screens/components live under `ui/home`, `ui/detail`, `ui/add`, `ui/common`.
- **External dependencies:**
  - AniList GraphQL for anime search/details.
  - Official MyAnimeList API v2 for anime rating/user count when `MAL_CLIENT_ID` is configured.
  - Jikan REST API as MAL fallback.
  - TMDB and OMDb for Movies/TV.
  - OpenLibrary and Google Books for books.
  - RAWG for games.

### 4. RECENT WORK — WHAT JUST HAPPENED (HIGH PRIORITY)

- **What was worked on in this session:**
  - Fixed MyAnimeList XML import failure for the provided export.
  - Added metadata refresh overwrite confirmation with field-by-field selectable changes.
  - Preserved MAL ids when linking anime to AniList.
  - Added optional official MAL API v2 enrichment for anime rating and scoring-user count.
  - Added manual external rating management with add/edit/delete/set-primary behavior.
  - Increased Gradle/Kotlin heap settings to address `Java heap space` failures.
  - Generated this continuation document.
- **What decisions were made and WHY:**
  - Android XML parser hardening features are now applied only if supported. Reason: some Android XML factories can throw on unsupported features before parsing a valid MAL export.
  - Metadata refresh is split into preview/apply. Reason: provider refresh can overwrite meaningful manual/imported metadata, so the user needs visibility and control.
  - Refresh prompts only when existing values would be overwritten. Reason: filling empty fields should stay low-friction.
  - Anime can use AniList as the primary metadata link while preserving MAL id in metadata JSON. Reason: AniList has rich metadata/search, but user wants MAL rating/user count and MAL import duplicate detection.
  - Official MAL API is optional and falls back to Jikan. Reason: MAL v2 requires a client id and published limit behavior is less straightforward; the app should still work without credentials.
  - Manual external ratings use existing `external_ratings` plus copied primary fields on `media_items`. Reason: this matches the current display model without a schema migration.
  - Manual external rating UI is intentionally accepted as first-pass functionality, not polished. Reason: user asked for capability first; UX can be iterated after real use.
  - Gradle workers were capped and heap raised. Reason: reduce memory pressure during Android Studio/Gradle builds.
- **What changed in the system:**
  - `MyAnimeListXmlImport.kt` now tolerates unsupported XML parser features.
  - `MediaRepository.kt` now defines `MetadataRefreshPreview`, `MetadataRefreshChange`, `MetadataRefreshField`, and external rating CRUD APIs.
  - `OfflineMediaRepository.kt` now builds refresh previews, applies selected refresh fields, preserves MAL ids for AniList-linked anime, updates MAL duplicate detection, and manages manual external ratings.
  - `MediaDao.kt` now supports per-item credits, external rating CRUD, and primary external rating updates.
  - `ContentTrackerApp.kt` now routes refresh through preview/confirmation and exposes `Puntuacions` in the detail overflow menu.
  - `HomeViewModel.kt` now exposes refresh preview/apply and external rating APIs.
  - `DetailScreen.kt` now opens `ExternalRatingsDialog`.
  - `DetailQuickActionsSection.kt` now contains external rating add/edit/delete/set-primary UI.
  - `AniListMetadataRepository.kt` now accepts `malClientId`, tries MAL v2 first for score/scoring users, preserves MAL id in AniList metadata, and falls back to Jikan.
  - `ContentTrackerApplication.kt` passes `BuildConfig.MAL_CLIENT_ID`.
  - `app/build.gradle.kts` exposes `MAL_CLIENT_ID` from Gradle property/environment variable.
  - `gradle.properties` heap settings were increased locally.
  - `strings.xml` gained refresh confirmation and external rating management strings.
- **What was discussed but NOT yet implemented:**
  - Polished manual external rating UX.
  - Metadata overwrite confirmation for metadata linking as well as refresh.
  - Automated tests for the MAL XML fixture and refresh diff behavior.
  - Device/emulator visual QA.
- **Open threads or unresolved questions:**
  - Whether official MAL API should fully replace Jikan later or remain optional fallback-based.
  - Whether manual external ratings should support custom/free-text sources beyond the existing enum.
  - Whether setting primary external rating should be source-aware instead of matching only score/max score.
  - Whether metadata refresh should preserve a user-selected primary external rating when provider refresh also supplies ratings.

### 5. WHAT COULD GO WRONG

- **Known bugs or issues:**
  - Manual external rating UI is not polished; it is a first implementation.
  - Manual external rating primary detection matches by score/max score, not by a stored primary rating id. If two sources share the same score/max, the UI may mark both as effectively primary.
  - Metadata linking still overwrites metadata without the new refresh confirmation.
  - Older AniList-linked anime may lack preserved MAL ids until refreshed/relinked.
  - Gradle properties currently include local API/client ids in the working tree; be careful not to commit secrets unintentionally.
- **Edge cases to watch for:**
  - MAL official API may return 401/403/429 or omit `mean`; code falls back to Jikan where possible.
  - Jikan can rate-limit or fail upstream.
  - External rating score/max validation is minimal; score can exceed max.
  - Deleting the primary external rating falls back to the first remaining rating, which may not be the user’s desired fallback.
  - Refreshing provider metadata may overwrite external ratings unless the user deselects those fields in the confirmation modal.
- **Technical debt or shortcuts taken:**
  - Manual external rating management is implemented in `DetailQuickActionsSection.kt`, which is already carrying quick actions and external tracking modal logic.
  - Primary external rating is still denormalized on `media_items` rather than modeled by id/source.
  - Refresh diff display uses formatted string comparisons for many fields; it is pragmatic but not a full structured diff engine.
  - No tests were added.
- **Assumptions being made that could be wrong:**
  - User wants MAL to be preferred only for anime rating/user count, not necessarily for all anime metadata.
  - Jikan should remain fallback even with official MAL API configured.
  - Manual rating source list is sufficient as an enum.
  - Raising Gradle heap/capping workers is acceptable for this local project.

### 6. HOW TO THINK ABOUT THIS PROJECT

1. **Core architectural pattern/design philosophy:** Local-first repository architecture. Room is the source of truth; providers enrich local items but should not own or erase user data. This was chosen because Omnilog is a personal media log, not a provider mirror.
2. **Most common mistake a new person would make:** Treat provider metadata refresh/linking as a full replacement of the local item. That risks overwriting sessions, progress, user ratings, collections, ownership, reviews, and manual edits.
3. **What looks refactorable but intentionally should NOT be:** `ContentTrackerApp.kt` and detail orchestration are large and tempting to split, but they coordinate navigation, dialogs, imports, metadata linking, and back handling. Refactor only if explicitly requested or if a focused extraction is required for a specific feature.

### 7. DO NOT TOUCH LIST

- Do NOT refactor stable, working systems without being asked.
- Do NOT redesign architecture unless explicitly instructed.
- Preserve existing naming conventions.
- Maintain previously chosen tradeoffs — they were chosen for reasons documented above.
- Ask before introducing new frameworks, libraries, or dependencies.
- Do NOT make imports destructive; only backup restore may replace data.
- Do NOT show provider-specific import actions outside their section pages.
- Do NOT overwrite user/session/progress/review/ownership/collection data during metadata work.
- Do NOT casually remove Jikan fallback until the user explicitly approves.
- Do NOT commit local API keys/client ids or unrelated dirty files.
- Do NOT polish/refactor manual external ratings unless requested; acknowledge it is first-pass functionality.

### 8. CONFIDENCE & FRESHNESS

- **Project identity:** ✅ HIGH CONFIDENCE — carried forward and consistent with current code.
- **Current built state:** ✅ HIGH CONFIDENCE — `.\gradlew.bat assembleDebug` passed after recent changes.
- **Architecture map:** ✅ HIGH CONFIDENCE — files were inspected/modified this session.
- **Recent work:** ✅ HIGH CONFIDENCE — implemented and build-verified this session.
- **Known issues/risks:** ⚠️ MEDIUM — based on code review and no emulator visual QA.
- **External API behavior:** ⚠️ MEDIUM — official MAL API shape verified conceptually; runtime behavior depends on configured client id and API responses.
- **Roadmap:** ⚠️ MEDIUM — based on user direction, not a formal backlog.

###9. Roadmap

1. **Run app/device QA**
   - Install/run latest debug APK.
   - Open detail page overflow menu.
   - Verify metadata refresh confirmation modal.
   - Verify manual external rating modal can add, edit, delete, and set primary rating.

2. **Polish manual external ratings**
   - Improve layout density and labels.
   - Consider showing current score formatting in rows.
   - Consider source-specific defaults for max score.
   - Consider supporting custom rating sources if enum list is too limiting.

3. **Harden primary external rating model**
   - Consider storing primary external rating source/id instead of matching by score/max.
   - Decide desired fallback behavior when primary rating is deleted.

4. **Extend overwrite confirmation**
   - Apply the preview/selective overwrite model to metadata linking, not only refresh.

5. **Import/metadata tests**
   - Add MAL XML fixture test using `animelist_1779485507_-_7420659.xml`.
   - Add tests for duplicate detection after AniList linking with preserved MAL id.
   - Add tests for refresh diff generation and selective apply.

6. **MAL API follow-up**
   - Confirm official MAL API behavior with a configured client id on device.
   - Add conservative handling for `429` if needed.
   - Decide whether Jikan remains permanent fallback.

7. **Workspace cleanup**
   - Review unrelated dirty files and old continuation docs.
   - Avoid committing local credentials from `gradle.properties`.
