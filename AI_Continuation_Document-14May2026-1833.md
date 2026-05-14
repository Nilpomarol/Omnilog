# PROJECT CONTINUATION DOCUMENT
## Session 3 - 14 May 2026

### 1. PROJECT IDENTITY

- **Project Name:** Content Tracker Android App
- **What This Project Is:** A native Android app for personally tracking anime, books, movies/TV, and games, including first-time consumption and rewatches/rereads/replays. It is also a learning project for Kotlin, Jetpack Compose, Room, and layered Android architecture.
- **Primary Objective:** Build a working local-first media tracker where the user can manually add media items, track separate sessions, group related media into collections/franchises, and keep durable local data with backup/export/import.
- **Strategic Intent:** Create a practical personal tracking app while learning Android development. Long term, the app should support external metadata/API lookup, but user-entered tracking state remains the source of truth.
- **Hard Constraints:**
  - Android native app using Kotlin, Jetpack Compose, Material 3, Room, and KSP.
  - Preserve the existing layered architecture: Compose UI -> ViewModel -> Repository -> Room DAO/entities -> domain models.
  - UI text should remain Catalan-facing.
  - Rewatch/reread/replay must remain modeled as separate tracking sessions.
  - Do not auto-change status based on progress.
  - API metadata, when added later, must be suggested/default metadata, not authoritative tracking state.
  - Different seasons/entries should be represented as separate media items where possible; collections/franchises group related items.
  - Do not introduce new frameworks, libraries, or dependencies without asking.
  - Use frequent coherent commits.
  - Run `.\gradlew.bat assembleDebug` after code changes.
  - Provide a short file-by-file explanation and manual test after each implementation.

### 2. WHAT EXISTS RIGHT NOW

- **What is built and working:**
  - Android Compose app builds successfully with `.\gradlew.bat assembleDebug`.
  - Bottom navigation sections: Anime, Llibres, Pellicules, Jocs.
  - Movies section includes both Movie and TV Show media types.
  - Manual media item creation with title, type, status, progress total, ownership, platform, and platform type.
  - Local Room persistence with destructive migration currently acceptable because the user allowed deleting existing data during the model cleanup.
  - Detail screen for tracked media.
  - Item metadata editing and delete-media-item confirmation.
  - Current session editing: progress, total, status, rating, notes.
  - Past session editing and deletion.
  - Separate session creation for reread/rewatch/replay flows.
  - External tracking references with optional external ID/URL, sync flag, and deletion.
  - External ratings are still represented in the data model.
  - Media collections/franchises:
    - create collections
    - assign/remove items from collections
    - edit collection name/sort order
    - delete empty collections
    - collection detail view
    - home grouping by collection
  - Home browsing controls:
    - local title search
    - status filtering
    - sort controls
  - JSON backup export/import:
    - export collections, media items, tracking sessions, external ratings, and external tracking
    - import replaces all local app data after confirmation
    - schema version 1

- **What is partially built:**
  - Collection/franchise support is functional but UI polish is intentionally deferred.
  - Backup/import is functional but has no automated tests yet.
  - Catalan-facing UI exists, but wording and proper localization structure need later review.
  - UI is usable but not visually polished.

- **What is broken or blocked:**
  - No known build-blocking issues.
  - Persistent non-blocking git warning: `unable to access 'C:\Users\nilpo/.config/git/ignore': Permission denied`.
  - No automated test suite currently verifies repository import/export behavior.

- **What has NOT been started yet:**
  - External API metadata/search/import layer.
  - API metadata preview for untracked items.
  - Dragging cards/items together to create or add to a collection.
  - Full UI polish pass with sheets/dialogs/cards/navigation refinement.
  - Proper multilingual resource organization.
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
  - Git branch: `main`
  - Remote: `origin git@github.com:Nilpomarol/Content-tracking-android-app.git`

- **Key data structures, tables, files, or repos:**
  - Repo path: `C:\Users\nilpo\Documents\Documents\Personal\Content-tracking-android-app`
  - Entry point:
    - `app/src/main/java/com/nilpo/contenttracker/MainActivity.kt`
    - `app/src/main/java/com/nilpo/contenttracker/ContentTrackerApplication.kt`
  - App shell:
    - `app/src/main/java/com/nilpo/contenttracker/ui/ContentTrackerApp.kt`
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
  - Collections:
    - collection domain/entity support in `core/model`, `core/database/entity`, DAO, mapper, repository, and UI detail/home flows.
  - Domain models:
    - `core/model/MediaItem.kt`
    - `core/model/TrackedMedia.kt`
    - `core/model/TrackingSession.kt`
    - `core/model/MediaCollection.kt`
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
  - Repository:
    - `core/repository/MediaRepository.kt`
    - `core/repository/OfflineMediaRepository.kt`
  - Strings:
    - `app/src/main/res/values/strings.xml`

- **How the system works end-to-end:**
  1. `MainActivity` starts Compose and obtains `HomeViewModel`.
  2. `ContentTrackerApplication` initializes Room and `OfflineMediaRepository`.
  3. `HomeViewModel` observes selected media section, search/filter/sort state, selected item/collection state, and data from the repository.
  4. `OfflineMediaRepository` observes Room data through `MediaDao`, maps database entities into domain models, and returns app-facing flows.
  5. Compose screens render `HomeUiState`.
  6. User actions call ViewModel methods.
  7. ViewModel launches coroutines and calls repository methods.
  8. Repository validates/clamps relevant data and calls DAO.
  9. Room emits updated flows and the UI recomposes.
  10. Backup export serializes all relevant tables to JSON; backup import parses schema version 1 and transactionally replaces all local data.

- **Naming conventions or standards in use:**
  - Domain request objects use `Add...Request`.
  - Repository methods express app use cases, not raw SQL operations.
  - DAO methods operate on entities and database primitives.
  - Compose components are split by screen/section/editor.
  - UI labels are stored in `strings.xml`.
  - Catalan-facing strings are preferred even when accents are omitted to match current file style.

- **External dependencies:**
  - No external metadata APIs integrated yet.
  - Candidate future integrations discussed:
    - Books: Open Library or Google Books
    - Anime: AniList or Jikan
    - Movies/TV: TMDb
  - External tracking sources are modeled locally: MAL, IMDb, StoryGraph, Goodreads, Letterboxd, TMDb, RAWG, Backloggd, Other.

### 4. RECENT WORK - WHAT JUST HAPPENED (HIGH PRIORITY)

- **What was worked on in this session:**
  - Revisited the season model after the user questioned variable session lengths and season totals.
  - Checked the API-facing concept for TV/anime seasons and decided not to keep complex in-item season progress management.
  - Simplified the model: media items represent individual trackable entries/seasons/books/games/movies, while collections/franchises group related entries.
  - Removed season-progress complexity instead of keeping dead code, because the user explicitly allowed breaking/deleting existing data.
  - Added media collections and collection detail/management.
  - Added home browsing controls.
  - Added media item deletion.
  - Added JSON backup export/import with replace-all import confirmation.
  - Built successfully after each implementation slice.
  - Committed the backup work as `f2d4b0e Add JSON backup import export`.

- **What decisions were made and WHY:**
  - **Use one media item per season/entry where possible.**
    - This matches how services like MyAnimeList generally treat separate anime seasons as separate entries.
    - It also fits TV APIs conceptually, where seasons are distinct data under a show but often have separate IDs/metadata and episode counts.
    - It avoids needing session-specific season totals, aggregate progress distribution, and special logic for multi-season sessions.
  - **Use collections/franchises as the grouping layer for everything.**
    - This generalizes beyond anime/TV: book series, movie franchises, game series, and anime seasons can all be grouped.
    - It keeps tracking state simple while still supporting navigation around related media.
  - **Delete old season-management code instead of preserving compatibility.**
    - The user explicitly allowed breaking existing data and cleaning the model from the start.
    - This avoided dead code and migration complexity.
  - **Keep sessions as the tracking unit.**
    - Rewatch/reread/replay flows still need distinct sessions.
    - Progress/status/rating/notes live at the session level, not as a single immutable item completion state.
  - **Backup import replaces all local data.**
    - This is simple and appropriate for a local-first early app.
    - Import shows a confirmation because it overwrites current local data.
  - **No new dependencies for JSON.**
    - Used platform `org.json` to avoid adding libraries.

- **What changed in the system:**
  - Removed old `SeasonProgress` model/table/flows and related UI.
  - Added collection entities/domain/mapping/repository/UI.
  - Added `collectionId` on media items and collection grouping in the home screen.
  - Added collection detail route/selected collection state.
  - Added collection creation/edit/delete and item assignment/removal.
  - Added search/filter/sort controls on the home screen.
  - Added media item deletion with confirmation.
  - Added DAO methods to dump and transactionally replace all backup-supported data.
  - Added repository backup methods:
    - `exportBackupJson(): String`
    - `importBackupJson(json: String)`
  - Added Android document picker integration:
    - `CreateDocument("application/json")` for export
    - `OpenDocument()` for import
  - Added import confirmation dialog and Catalan-facing strings.

- **What was discussed but NOT yet implemented:**
  - Dragging cards/items together to add/create collections. User chose to defer this to UI polish.
  - External API metadata lookup/search/import.
  - More polished collection/franchise UI.
  - Automated tests for backup/import and repository behavior.
  - Full visual design pass.

- **Open threads or unresolved questions:**
  - Which metadata API should be integrated first?
  - Should collection membership eventually support ordering beyond the current sort order/name basics?
  - Should import/export support user-visible validation errors instead of failing silently or only through coroutine exceptions?
  - Should backup schema versioning get formal migration logic before more tables are added?

### 5. WHAT COULD GO WRONG

- **Known bugs or issues:**
  - Persistent non-blocking git warning about inaccessible global ignore file.
  - Backup import currently replaces local data as designed; this is destructive if the user confirms the wrong file.
  - Backup/import has no automated tests yet.
  - UI text is Catalan-facing but may need linguistic correction.

- **Edge cases to watch for:**
  - Importing malformed JSON or a future unsupported schema version should be handled more visibly later.
  - Backup import order matters because foreign keys exist between collections, items, sessions, ratings, and external tracking.
  - Collection delete should remain restricted to empty collections unless a deliberate cascade UX is added.
  - Media item deletion should not be exposed as a casual one-tap action; keep confirmation.
  - Status must not auto-change when progress reaches total.
  - External API totals must not overwrite user tracking state later.

- **Technical debt or shortcuts taken:**
  - Destructive database migration was accepted during simplification; this should not be assumed acceptable forever.
  - JSON backup code is hand-written with `org.json` helpers and no tests.
  - UI is functionally organized but not polished.
  - No formal test coverage yet; verification has been Gradle builds and manual test plans.

- **Assumptions being made that could be wrong:**
  - A single media item per season/entry will remain simpler than nested seasons for this user.
  - Collections/franchises are flexible enough for all media types.
  - Replace-all backup import is acceptable at this stage.
  - API metadata can be layered later without changing the tracking core.

### 6. HOW TO THINK ABOUT THIS PROJECT

1. **What is the core architectural pattern or design philosophy, and why was it chosen?**
   - This is a local-first layered Android app: Compose UI -> ViewModel -> Repository -> Room DAO/entities -> domain models. The design keeps user tracking data durable and editable, keeps database details out of UI code, and stays simple enough for a learning project.

2. **What is the most common mistake a new person working on this would make?**
   - Reintroducing nested season-progress complexity or treating API metadata as the app's tracking truth. The current model intentionally makes each trackable season/entry its own media item and uses collections/franchises for grouping.

3. **What looks like it should be refactored or redesigned but intentionally should NOT be? Why?**
   - Do not split the app into a larger Clean Architecture/module setup yet. The current single-module layered structure is intentional for learning and speed.
   - Do not re-add season-specific tables unless the user explicitly asks. That complexity was deliberately removed.
   - Do not make drag-and-drop collection management now. It was explicitly deferred to later UI polish.
   - Do not add API dependencies yet. Metadata/search should be designed as suggested metadata after the local model remains stable.

### 7. DO NOT TOUCH LIST

Explicit rules for the next AI:
- Do NOT refactor stable, working systems without being asked.
- Do NOT redesign architecture unless explicitly instructed.
- Preserve existing naming conventions.
- Maintain previously chosen tradeoffs - they were chosen for reasons documented above.
- Ask before introducing new frameworks, libraries, or dependencies.
- Do NOT auto-change tracking status based on progress.
- Do NOT collapse rereads/rewatches/replays into a single item-level state.
- Do NOT treat API metadata as authoritative tracking state.
- Do NOT reintroduce in-item season progress management unless explicitly requested.
- Do NOT implement drag-and-drop collection UI yet unless the user asks; it was intentionally deferred.
- Do NOT remove Catalan UI direction.
- Do NOT rewrite the database schema unless the requested feature truly requires it.
- Do NOT revert user or prior-agent changes.
- Do NOT commit the older untracked continuation document unless the user explicitly asks.

### 8. CONFIDENCE & FRESHNESS

- **Project identity:** HIGH CONFIDENCE - carried forward from prior continuation and reinforced this session.
- **Current built functionality:** HIGH CONFIDENCE - implemented and verified with `.\gradlew.bat assembleDebug`.
- **Architecture map:** HIGH CONFIDENCE - files were inspected and modified this session.
- **Recent work:** HIGH CONFIDENCE - directly implemented and committed this session.
- **Backup/export/import details:** HIGH CONFIDENCE - built this session and compiled successfully.
- **Season/collection reasoning:** HIGH CONFIDENCE - directly discussed with the user and implemented.
- **Known issues/edge cases:** HIGH CONFIDENCE - derived from implementation decisions and current verification.
- **Future API provider choices:** MEDIUM - candidates discussed conceptually, no final provider decision.
- **Catalan/localization quality:** MEDIUM - UI direction is clear, exact wording should be reviewed later.
- **Manual runtime behavior of document picker/import:** MEDIUM - compiled successfully; should be manually tested on device/emulator.

### 9. CURRENT ROADMAP

Use this roadmap as direction, not as a rigid contract. The user is learning Android development, so implementation should continue in coherent slices with concise explanations and commits.

1. **Manual Test Backup/Import**
   - Export a backup JSON from the app.
   - Modify/delete local data.
   - Import the backup and confirm collections, media items, sessions, external ratings, and external tracking restore correctly.
   - Check cancel behavior on the import confirmation dialog.

2. **Improve Backup/Import UX**
   - Show success/failure feedback.
   - Add visible handling for malformed JSON and unsupported schema versions.
   - Consider a small import summary before replacement.

3. **Strengthen Local Browsing**
   - Review search/filter/sort behavior across all sections.
   - Add any missing sort/filter options that matter in daily use.
   - Keep it local-first and simple.

4. **Collection UX Polish**
   - Improve collection detail layout and item assignment flow.
   - Later: add drag cards/items together to create or add to a collection.
   - Do this during a UI polish pass, not as a data-model change.

5. **Metadata/Search Layer**
   - Choose first API integration.
   - Store API metadata separately from user tracking data.
   - Use API values as defaults/suggestions when creating items.
   - Do not let API values overwrite user-edited tracking fields without explicit user action.

6. **UI Usability and Visual Refinement**
   - Move large edit forms into sheets/dialogs where appropriate.
   - Improve spacing, grouping, empty states, and action placement.
   - Review Catalan labels.
   - Add proper multilingual resource organization later.
