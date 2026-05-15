# PROJECT CONTINUATION DOCUMENT
## Session 4 - 15 May 2026

### 1. PROJECT IDENTITY

- **Project Name:** Content Tracker Android App
- **What This Project Is:** A native Android app for personally tracking anime, books, movies/TV, and games, including first-time consumption and rewatches/rereads/replays. It is also a learning project for Kotlin, Jetpack Compose, Room, local-first app architecture, and API-backed metadata search.
- **Primary Objective:** Build a working local-first media tracker where the normal add flow is metadata search first, then review/create a local tracked item and first tracking session, with manual entry retained only as a fallback.
- **Strategic Intent:** Create a practical personal tracking app while learning Android development. Long term, external APIs should make item creation fast and accurate, while local user tracking state remains separate, editable, and authoritative.
- **Hard Constraints:**
  - Android native app using Kotlin, Jetpack Compose, Material 3, Room, and KSP.
  - Preserve the existing layered architecture: Compose UI -> ViewModel -> Repository -> Room DAO/entities -> domain models.
  - UI labels should remain Catalan-facing.
  - Metadata returned from APIs should use practical/common provider language for media data; TMDb currently uses `en-US`.
  - API metadata is the normal source/default for item creation, but it must remain suggested/default metadata, not authoritative tracking state.
  - Local tracking state, sessions, progress, rating, notes, status, platform, and ownership remain user-owned.
  - Rewatch/reread/replay must remain modeled as separate tracking sessions.
  - Do not auto-change status based on progress.
  - Different seasons/entries should remain modeled as separate media items where possible; collections/franchises group related items.
  - Preserve metadata provenance on API-created items via `metadataSource` and `metadataExternalId`.
  - Refresh/refetch metadata should eventually use provenance and show preview/diff before applying; never silently overwrite user tracking state.
  - Do not introduce new frameworks, libraries, or dependencies without asking.
  - Use frequent coherent commits.
  - Run `.\gradlew.bat assembleDebug` after code changes.
  - Provide a short file-by-file explanation and manual test after each implementation.

### 2. WHAT EXISTS RIGHT NOW

- **What is built and working:**
  - Android Compose app builds successfully with `.\gradlew.bat assembleDebug`.
  - Bottom navigation sections: Anime, Llibres, Pellicules, Jocs.
  - Movies section includes both Movie and TV Show media types.
  - Room persistence with destructive migrations currently acceptable because the user allowed breaking/deleting existing local data during model cleanup.
  - Manual item creation still exists as a secondary fallback.
  - Metadata-search-first add flow exists:
    - clean search screen
    - debounced TMDb search while typing
    - suggestions with cover, title, year, and media type
    - selectable suggestions
    - detail/review step with cover, source, total, genres, synopsis, editable title/total/session fields
    - save creates local media item plus first tracking session
  - TMDb metadata search for movies and TV shows works when `TMDB_API_KEY` is configured.
  - TMDb search/detail requests use `language=en-US`.
  - TMDb detail fetch on selection fills:
    - movie runtime as total
    - TV `number_of_episodes` as total
    - cover URL
    - synopsis
    - genres for review display
    - metadata provenance
  - Local media item metadata fields include:
    - cover URL
    - synopsis
    - metadata source
    - metadata external ID
  - Detail screen for tracked media.
  - Item metadata editing and delete-media-item confirmation.
  - Current session editing: progress, status, rating, notes, platform.
  - Past session editing and deletion.
  - Separate session creation for reread/rewatch/replay flows.
  - External tracking references with optional external ID/URL, sync flag, and deletion.
  - Media collections/franchises:
    - create collections
    - assign/remove items from collections
    - edit collection name
    - delete empty collections
    - collection detail view
    - home grouping by collection when sorting by collection
  - Home browsing:
    - local title search
    - status filtering
    - sort by title, collection, progress, rating, recently updated
    - ascending/descending direction
    - smart default direction per sort mode
    - collection grouping only when sorting by collection
  - JSON backup export/import:
    - export collections, media items, tracking sessions, external ratings, and external tracking
    - import preview with counts
    - destructive replace-all import after confirmation
    - success/error feedback
    - backup schema v2 export, with v1 import compatibility

- **What is partially built:**
  - TMDb provider is real but only supports Movie and TV search/details.
  - API-first add flow is structurally much closer to final, but still visually modest.
  - The selected metadata review page shows genres, synopsis, cover, and total, but genres/release year are not persisted as separate local fields.
  - Cover images are loaded directly in Compose using `URL.openStream`; this avoids dependencies but is not a production-grade image pipeline.
  - Metadata refresh/refetch is not implemented yet, but provenance exists to support it.
  - Catalan-facing UI exists, but wording and localization structure need later review.

- **What is broken or blocked:**
  - No known build-blocking issues.
  - Persistent non-blocking git warning: `unable to access 'C:\Users\nilpo/.config/git/ignore': Permission denied`.
  - `gradle.properties` is locally modified and intentionally uncommitted because it likely contains the user's TMDb API key.
  - The old continuation file `AI_Continuation_Document-14May2026-1359.md` remains untracked.
  - No automated tests yet.

- **What has NOT been started yet:**
  - AniList/Jikan anime metadata provider.
  - Books metadata provider.
  - Games metadata provider.
  - Metadata refresh/refetch UI.
  - Metadata diff/preview before applying refreshed values.
  - Persisted genres/release year/author/director/cast metadata fields.
  - Add flow final visual polish.
  - Proper image loading/caching strategy.
  - Cloud sync or remote backup.

### 3. ARCHITECTURE & TECHNICAL MAP

- **Tech stack / tools / platforms:**
  - Kotlin
  - Android SDK 36 (`compileSdk=36`, `targetSdk=36`, `minSdk=26`)
  - Jetpack Compose
  - Material 3
  - Room
  - KSP
  - Gradle Android plugin
  - Built-in Java/Android HTTP stack for TMDb; no new HTTP/image dependencies added.
  - `org.json` for JSON parsing.
  - Git branch: `main`
  - Remote: `origin git@github.com:Nilpomarol/Content-tracking-android-app.git`

- **Key data structures, tables, files, or repos:**
  - Repo path: `C:\Users\nilpo\Documents\Documents\Personal\Content-tracking-android-app`
  - Entry point:
    - `app/src/main/java/com/nilpo/contenttracker/MainActivity.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ContentTrackerApplication.kt`
  - App shell:
    - `app/src/main/java/com/nilpo/contenttracker/ui/ContentTrackerApp.kt`
  - Add flow:
    - `app/src/main/java/com/nilpo/contenttracker/ui/add/AddMediaScreen.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/add/MetadataSearchUiState.kt`
  - Home:
    - `app/src/main/java/com/nilpo/contenttracker/ui/home/HomeViewModel.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/home/HomeScreen.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/home/MediaSection.kt`
  - Detail:
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/DetailScreen.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/CurrentSessionSection.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/ItemDetailsSection.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/ItemDetailsEditor.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/NewSessionSection.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/PastSessionsSection.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ui/detail/ExternalTrackingEditor.kt`
  - Domain models:
    - `core/model/MediaItem.kt`
    - `core/model/TrackedMedia.kt`
    - `core/model/TrackingSession.kt`
    - `core/model/MediaCollection.kt`
    - `core/model/MetadataSuggestion.kt`
    - `core/model/ExternalTracking.kt`
    - `core/model/ExternalRating.kt`
    - `core/model/AddTrackedMediaRequest.kt`
    - `core/model/AddTrackingSessionRequest.kt`
  - Database:
    - `core/database/ContentTrackerDatabase.kt`
    - `core/database/dao/MediaDao.kt`
    - `core/database/entity/*`
    - `core/database/mapper/MediaMappers.kt`
    - `core/database/relation/TrackedMediaRelation.kt`
  - Repositories:
    - `core/repository/MediaRepository.kt`
    - `core/repository/OfflineMediaRepository.kt`
    - `core/repository/MetadataRepository.kt`
    - `core/repository/TmdbMetadataRepository.kt`
  - Strings:
    - `app/src/main/res/values/strings.xml`
  - Local secret configuration:
    - `TMDB_API_KEY` may be configured in user Gradle properties or environment variable.
    - Do not commit the user's API key.

- **How the system works end-to-end:**
  1. `MainActivity` starts Compose and creates `HomeViewModel` with `OfflineMediaRepository` and `TmdbMetadataRepository`.
  2. `ContentTrackerApplication` initializes Room and wires TMDb using `BuildConfig.TMDB_API_KEY`.
  3. `HomeViewModel` observes selected media section, search/filter/sort state, local tracked media, and metadata search state.
  4. The home screen shows tracked local media from Room.
  5. Tapping add opens the metadata-search-first add flow.
  6. As the user types, `AddMediaScreen` triggers debounced metadata search through `HomeViewModel`.
  7. `HomeViewModel` calls `MetadataRepository.searchSuggestions`.
  8. `TmdbMetadataRepository` queries TMDb movie/TV search endpoints and returns `MetadataSuggestion` values.
  9. User taps a suggestion.
  10. `HomeViewModel` calls `MetadataRepository.getSuggestionDetails`.
  11. `TmdbMetadataRepository` fetches TMDb detail endpoint and enriches the suggestion with runtime/episode count, cover, synopsis, genres, and provenance.
  12. Add flow shows a review/create screen; user may edit title/total/session fields.
  13. Saving calls `MediaRepository.addTrackedMedia`.
  14. `OfflineMediaRepository` inserts a `MediaItemEntity` and first `TrackingSessionEntity`.
  15. Room emits updated flows and the UI recomposes.
  16. Backup export serializes all local data; import validates schema and transactionally replaces all local data after confirmation.

- **Naming conventions or standards in use:**
  - Domain request objects use `Add...Request`.
  - Metadata models use `Metadata...` names.
  - Metadata provenance fields are explicit: `metadataSource`, `metadataExternalId`.
  - Repository methods express app use cases, not raw SQL operations.
  - DAO methods operate on entities and database primitives.
  - Compose components are split by screen/section/editor.
  - UI labels are stored in `strings.xml`.
  - Catalan-facing strings are preferred for UI labels; media metadata from TMDb currently uses English.

- **External dependencies:**
  - TMDb API is integrated for movies and TV shows.
  - Requires `TMDB_API_KEY`.
  - No third-party HTTP/image libraries added.
  - Candidate future integrations:
    - Anime: AniList or Jikan
    - Books: Open Library or Google Books
    - Games: RAWG or IGDB
  - External tracking sources are modeled locally: MAL, IMDb, StoryGraph, Goodreads, Letterboxd, TMDb, RAWG, Backloggd, Other.

### 4. RECENT WORK - WHAT JUST HAPPENED (HIGH PRIORITY)

- **What was worked on in this session:**
  - Improved backup/import safety:
    - success/error feedback
    - malformed/unsupported backup handling
    - import preview with counts
  - Strengthened local browsing:
    - progress/rating/recent sorting
    - ascending/descending direction
    - smart default direction per sort mode
    - flat global list for title/progress/rating/recent sorts
    - collection grouping only for collection sort
    - compacted browsing controls slightly
  - Added metadata boundary and then TMDb implementation:
    - `MetadataSuggestion`
    - `MetadataRepository`
    - `TmdbMetadataRepository`
    - `TMDB_API_KEY` BuildConfig wiring
    - INTERNET permission
  - Made metadata provenance explicit:
    - `metadataSource`
    - `metadataExternalId`
  - Reworked add flow around API search:
    - search-first default
    - manual add secondary
    - suggestion selection
    - detail fetch on selection
    - review/create page
  - Changed TMDb metadata language from Catalan (`ca-ES`) to English (`en-US`).

- **What decisions were made and WHY:**
  - **API search is the primary add flow, manual add is fallback.**
    - The user clarified that in 99.9% of cases they expect to search the item via API and add tracking from that.
    - Manual-from-zero should exist but not drive the main UI.
  - **API metadata is normal/default input, but not tracking truth.**
    - Metadata should prefill local fields and usually remain untouched.
    - User tracking state remains local and authoritative.
  - **Metadata provenance is valuable and should be stored.**
    - Needed for refresh/refetch, transparency, source links, conflict handling, and debugging bad metadata.
  - **TMDb was chosen first.**
    - Covers movies and TV.
    - Provides a useful first real API integration.
    - Fits the media-item-per-season/entry model better than reintroducing nested season tracking.
  - **No new dependencies were added.**
    - The app uses built-in HTTP and `org.json` for now to keep dependencies minimal.
    - This is acceptable for early implementation, but not final image/network architecture.
  - **TMDb language is `en-US`.**
    - UI remains Catalan-facing.
    - Media titles/synopses should not be forced into Catalan because common/original media metadata is usually more useful in English.
  - **Search details are fetched on selection, not for every search result.**
    - Search endpoint is lighter.
    - Detail endpoint contains runtime/episode count and richer data.
    - Fetching details only on selection reduces API calls.
  - **Genres are shown but not persisted yet.**
    - There is no genre field in the local schema.
    - Persisting genres/release year/author/director should be a deliberate metadata schema step, not hidden inside UI work.

- **What changed in the system:**
  - `MetadataRepository` now supports search and detail fetch.
  - `TmdbMetadataRepository` queries TMDb search and detail endpoints.
  - `AddMediaScreen` became a step-based flow:
    - Search
    - Review
    - Manual
  - `HomeViewModel` manages metadata search state, selected suggestion, and detail loading.
  - `MediaItemEntity` / `MediaItem` use explicit metadata provenance names.
  - `AddTrackedMediaRequest` carries metadata provenance, cover, and synopsis.
  - `OfflineMediaRepository.addTrackedMedia` stores cover/synopsis/provenance.
  - Backup schema export is v2 and imports old v1 keys for metadata provenance compatibility.
  - Room DB version is now 5.
  - TMDb requests use `en-US`.

- **What was discussed but NOT yet implemented:**
  - Hidden-but-accessible metadata refresh/refetch from detail page.
  - Preview/diff before applying refreshed metadata.
  - Persisted release year, genre, author/director/cast fields.
  - Better session creation UI on the review page.
  - Final visual polish of the add flow.
  - Support for non-TMDb media categories.
  - Better image loading/caching.

- **Open threads or unresolved questions:**
  - Should movie runtime be the right `progressTotal` unit long term, or should movies use a binary watched/not watched model with runtime only as metadata?
  - For TV shows, should TMDb `number_of_episodes` be enough, or should the app model seasons as separate media items from TMDb season details?
  - Should genres/release year be persisted now, or wait until more providers make the metadata schema clearer?
  - Which provider should be next: AniList/Jikan for anime, Google Books/Open Library for books, or RAWG/IGDB for games?
  - Should image loading remain manual for now or introduce an image loading library later?

### 5. WHAT COULD GO WRONG

- **Known bugs or issues:**
  - `gradle.properties` is modified locally and intentionally uncommitted because it likely contains the TMDb API key.
  - `AI_Continuation_Document-14May2026-1359.md` remains untracked.
  - Persistent non-blocking global git ignore warning.
  - Cover image loading is basic and uncached.
  - No automated tests.
  - TMDb failures are mostly silent; errors generally produce empty results or unchanged suggestion details.

- **Edge cases to watch for:**
  - Without `TMDB_API_KEY`, TMDb search returns no suggestions.
  - TMDb search is only useful in Movies section currently because Anime/Books/Games do not have real providers yet.
  - TV details use total number of episodes for the whole show, not a season-specific item.
  - Search while typing may create multiple API calls; debounce exists but no cancellation/result ordering guard beyond Compose coroutine cancellation.
  - TMDb may not return runtime, overview, poster, or episode counts for some items.
  - Save after metadata selection stores cover/synopsis/provenance, but does not store genres/release year separately.
  - Status must never auto-change based on progress.
  - Backup v2 includes metadata provenance field names; v1 import compatibility exists for old `externalId/sourceApi` keys.

- **Technical debt or shortcuts taken:**
  - Built-in HTTP calls and direct image stream loading are pragmatic but not final.
  - Metadata search state lives in `HomeViewModel`; acceptable now, but could be split later if add flow grows.
  - Destructive database migrations are still accepted by user for now.
  - UI is improved structurally but not fully polished.
  - No formal unit tests for metadata mapping, backup import/export, or sorting behavior.

- **Assumptions being made that could be wrong:**
  - TMDb should be the first provider and English metadata is preferred.
  - Movie runtime as `progressTotal` is useful for tracking.
  - TV total episodes at show level is acceptable until season-specific provider flow exists.
  - Persisting genres/release year should wait.
  - Manual add should remain but be visually secondary.
  - No dependency image/network approach is still acceptable for the next few slices.

### 6. HOW TO THINK ABOUT THIS PROJECT

1. **What is the core architectural pattern or design philosophy, and why was it chosen?**
   - This is a local-first layered Android app: Compose UI -> ViewModel -> Repository -> Room DAO/entities -> domain models. External APIs provide metadata suggestions/defaults, while local Room data is the user's durable tracking source of truth. This keeps learning complexity manageable while still allowing the app to grow toward real API-backed workflows.

2. **What is the most common mistake a new person working on this would make?**
   - Treating API data as authoritative tracking state or treating the media item itself as a single completion state. API metadata should prefill item metadata; user sessions carry tracking state. Rewatch/reread/replay remains separate sessions.

3. **What looks like it should be refactored or redesigned but intentionally should NOT be? Why?**
   - Do not reintroduce nested season-progress management. The app deliberately moved to media-item-per-season/entry plus collections/franchises.
   - Do not split into a large Clean Architecture/module setup yet. The current single-module layered structure is intentional for learning and speed.
   - Do not make manual add the primary flow again. The user clarified API search should be the normal add path.
   - Do not add dependencies casually. Network/image libraries may be useful later, but should be chosen deliberately.
   - Do not silently refresh metadata into user fields. Refresh should be hidden/secondary but explicit and previewed.

### 7. DO NOT TOUCH LIST

Explicit rules for the next AI:
- Do NOT refactor stable, working systems without being asked.
- Do NOT redesign architecture unless explicitly instructed.
- Preserve existing naming conventions.
- Maintain previously chosen tradeoffs - they were chosen for reasons documented above.
- Ask before introducing new frameworks, libraries, or dependencies.
- Do NOT commit API keys or local `gradle.properties` secrets.
- Do NOT auto-change tracking status based on progress.
- Do NOT collapse rereads/rewatches/replays into a single item-level state.
- Do NOT treat API metadata as authoritative tracking state.
- Do NOT make manual add the primary flow again.
- Do NOT reintroduce in-item season progress management unless explicitly requested.
- Do NOT implement drag-and-drop collection UI yet unless the user asks; it was intentionally deferred.
- Do NOT remove Catalan UI direction.
- Do NOT force media metadata into Catalan unless the user asks for metadata language settings.
- Do NOT rewrite the database schema unless the requested feature truly requires it; destructive migration is currently allowed but should still be intentional.
- Do NOT revert user or prior-agent changes.
- Do NOT commit the older untracked continuation document unless the user explicitly asks.

### 8. CONFIDENCE & FRESHNESS

- **Project identity:** ✅ HIGH CONFIDENCE - carried forward and reinforced this session.
- **Current built functionality:** ✅ HIGH CONFIDENCE - implemented and verified with `.\gradlew.bat assembleDebug`.
- **Architecture map:** ✅ HIGH CONFIDENCE - directly inspected and modified this session.
- **Recent work:** ✅ HIGH CONFIDENCE - directly implemented and committed this session.
- **TMDb metadata flow:** ✅ HIGH CONFIDENCE - implemented and manually reported working by the user, then language adjusted.
- **Backup/import details:** ✅ HIGH CONFIDENCE - built this session and previously verified by user.
- **Season/collection reasoning:** ✅ HIGH CONFIDENCE - discussed and implemented in prior session; not reworked here.
- **Known issues/edge cases:** ✅ HIGH CONFIDENCE - derived from current implementation and user feedback.
- **Future provider choices:** ⚠️ MEDIUM - candidates discussed, next provider not chosen.
- **Metadata persistence schema:** ⚠️ MEDIUM - provenance/cover/synopsis exist, richer metadata fields are intentionally deferred.
- **Catalan/localization quality:** ⚠️ MEDIUM - UI direction is clear, exact wording should be reviewed later.
- **Image/network architecture:** ❓ LOW - current implementation is pragmatic and should be revisited before production polish.

### 9. CURRENT ROADMAP

Use this roadmap as direction, not as a rigid contract. The user is learning Android development, so implementation should continue in coherent slices with concise explanations and commits.

1. **Stabilize API-First Add Flow**
   - Verify TMDb search, detail review, save, manual fallback on device/emulator.
   - Improve visible error handling for TMDb network/API failures.
   - Consider a small source/provenance display after item creation.
   - Keep UI modest; do not over-polish yet.

2. **Metadata Refresh/Refetch**
   - Add hidden/secondary action in detail when `metadataSource + metadataExternalId` exist.
   - Fetch latest metadata using provenance.
   - Show preview/diff before applying.
   - Never overwrite tracking sessions/status/progress/rating/notes.

3. **Metadata Schema Expansion**
   - Decide whether to persist release year, genres, original title, source URL, author/director/cast.
   - Add fields only when the app actually uses them.
   - Keep metadata separate from tracking state.

4. **Season/TV Handling with TMDb**
   - Decide whether TV show search should create show-level items or season-level items.
   - If season-level is chosen, use TMDb season details and create one media item per season.
   - Preserve collections/franchises as grouping layer.

5. **Next Metadata Provider**
   - Pick next category:
     - Anime: AniList or Jikan
     - Books: Google Books or Open Library
     - Games: RAWG or IGDB
   - Implement behind `MetadataRepository` without disrupting TMDb.

6. **UI Usability and Visual Refinement**
   - Refine add search/review UI.
   - Improve cover presentation, loading, and placeholders.
   - Move large edit forms into sheets/dialogs where appropriate.
   - Review Catalan labels.
   - Add proper multilingual resource organization later.

7. **Technical Hardening**
   - Add repository/unit tests for sorting, backup import/export, and metadata mapping.
   - Consider a proper image loading library only after asking.
   - Improve network error handling and cancellation behavior.
