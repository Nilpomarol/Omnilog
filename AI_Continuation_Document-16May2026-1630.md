# PROJECT CONTINUATION DOCUMENT
## Session 7 — 16 May 2026

### 1. PROJECT IDENTITY

- **Project Name:** Omnilog
- **What This Project Is:** Native Android personal media tracking app for books, anime, TV, movies, and games. It helps a user search metadata, create tracked items, and maintain authoritative personal tracking sessions.
- **Primary Objective:** Build a usable Android app where API metadata helps create media items, while user-owned tracking state remains explicit, editable, and reliable.
- **Strategic Intent:** Create a long-lived personal media log that can support rich search, collections/franchises, progress history, rewatch/reread/replay sessions, and eventually polished navigation/home summaries.
- **Hard Constraints:**
  - Preserve existing architecture and naming conventions.
  - Kotlin + Jetpack Compose + Material 3 + Room/KSP.
  - UI labels are Catalan-facing.
  - App name is Omnilog.
  - TMDb metadata currently uses English (`en-US`); do not force provider metadata into Catalan unless asked.
  - API metadata is suggested/default metadata, not authoritative tracking state.
  - User tracking state remains separate and authoritative.
  - Rewatch/reread/replay are separate tracking sessions.
  - Do not auto-change status based on progress.
  - Different seasons/entries should remain separate media items where possible; collections/franchises group related items.
  - Preserve `metadataSource` and `metadataExternalId`.
  - Do not reintroduce in-item season progress management unless explicitly requested.
  - Do not make manual add the primary flow again; it is a fallback.
  - Do not commit API keys or local secrets such as `gradle.properties`.
  - Dragging cards/items together to create or add to a collection was intentionally deferred.
  - During UI redesign iterations, do not commit until explicitly approved.

### 2. WHAT EXISTS RIGHT NOW

- **What is built and working:**
  - Native Android app with Room persistence, repositories, ViewModels, and Compose screens.
  - Main sections for anime, books, TV/movies, games, collections, item detail, add/search flows, backups, and tracking sessions.
  - Metadata search via AniList, TMDb, RAWG, Google Books, and now Open Library fallback/merge for books.
  - Detail header supports optional back and overflow actions from the shared top bar.
  - Current session card has visual state summaries for planned, in progress, paused, completed, and dropped.
  - Session edit flow uses a full-screen modal editor with draft fields and explicit Save/Cancel.
  - `.\gradlew.bat assembleDebug` passes after the latest changes.
- **What is partially built:**
  - UI redesign is mid-iteration. Current session card was improved but not visually approved as final.
  - Home redesign is still a placeholder/early slice.
  - Header polish and overflow menu style improved, but may need more review on device.
  - Book search quality improved, but provider limitations remain.
- **What is broken or blocked:**
  - No known compile blocker. `assembleDebug` passed.
  - Some strings/files show encoding artifacts from earlier sessions; avoid broad rewrites unless asked.
  - `gradle.properties` is dirty locally and must not be committed.
- **What has NOT been started yet:**
  - Drag-to-collection UI.
  - Final home dashboard.
  - Full navigation redesign/navbar icon polish.
  - Broader visual QA on multiple physical/emulator screen sizes.

### 3. ARCHITECTURE & TECHNICAL MAP

- **Tech stack / tools / platforms:**
  - Android native, Kotlin, Jetpack Compose, Material 3.
  - Room database with KSP.
  - Gradle build via `.\gradlew.bat assembleDebug`.
  - Git remote: `git@github.com:Nilpomarol/Content-tracking-android-app.git`.
- **Key data structures, tables, files, or repos:**
  - `core/model/MediaItem.kt`, `TrackedMedia.kt`, `TrackingSession.kt`, `TrackingStatus.kt`, `MediaType.kt`.
  - Room DAO/entity/mapper files under `core/database`.
  - `MediaRepository.kt` and `OfflineMediaRepository.kt` own tracking persistence.
  - Metadata repositories under `core/repository`: AniList, TMDb, RAWG, Google Books, Open Library, Composite.
  - UI entry/navigation in `ui/ContentTrackerApp.kt`.
  - Detail UI in `ui/detail/*`, especially `CurrentSessionSection.kt`.
  - Theme colors in `ui/theme/Theme.kt` through `OmnilogColors` and `MaterialTheme.colorScheme`.
- **How the system works end-to-end:**
  1. User searches metadata from the add flow.
  2. Provider repositories return `MetadataSuggestion` values.
  3. `CompositeMetadataRepository` chooses/merges candidate metadata where applicable.
  4. User reviews suggested metadata and creates a `MediaItem` plus initial `TrackingSession`.
  5. Room stores item/session/tracking data.
  6. `HomeViewModel` exposes observed media state to Compose screens.
  7. Detail screen displays provider metadata, item details, current session, history, and external tracking.
  8. Session edits update tracking state through repository/DAO methods, not provider metadata.
- **Naming conventions or standards in use:**
  - Core models use English enum/type names.
  - UI text resources are Catalan-facing.
  - App branding is Omnilog.
  - Metadata provenance uses `metadataSource` and `metadataExternalId`.
- **External dependencies:**
  - AniList GraphQL/API for anime.
  - TMDb for TV/movie metadata, currently `en-US`.
  - RAWG for games.
  - Google Books and Open Library for books.
  - Material icons core dependency was added for Compose icons.

### 4. RECENT WORK — WHAT JUST HAPPENED (HIGH PRIORITY)

- **What was worked on in this session:**
  - Improved book metadata reliability by adding Open Library and better Google Books search behavior.
  - Normalized/displayed book genres where possible.
  - Added display-only title cleanup to remove parenthetical segments such as years/saga hints.
  - Moved detail back/menu actions into the shared Omnilog header and styled the overflow menu.
  - Added Material icons core dependency.
  - Reworked `CurrentSessionSection.kt` into a more visual session state card and full-screen session editor.
  - Added combined session-detail update path for status, progress, rating, notes, start date, and end date.
  - Added date pickers to session edit form.
  - Added save/cancel behavior; edit form no longer auto-updates while typing/selecting.
  - Status `Completat` in edit form fills progress to max.
  - Rating selector highlights all values up to the selected rating.
  - Current session card now:
    - Uses distinct status colors from `OmnilogColors`.
    - Shows dropped with a close icon, not completed checkmark.
    - Shows rating whenever available.
    - Keeps dropped progress partial/faded instead of reusing completed full bar.
    - Shows progress with media-aware units: `episodis`, `pagines`, `minuts`, `hores`.
    - Shows rating highlight as label-left (`Nota`) and score-right (`X/10`) with stars below.
    - Shows dates more prominently: start/end on one line, updated below.
- **What decisions were made and WHY:**
  - Open Library was added as a fallback/merge source because Google Books had inconsistent missing data and weak search results.
  - API metadata remains only default/suggested metadata; user tracking edits remain authoritative.
  - Session edit was converted to draft state with Save/Cancel because the user explicitly did not want auto-updates.
  - Selecting `Completat` fills progress to max as a form convenience, but progress still does not automatically change status.
  - Dropped no longer reuses completed summary because that incorrectly made its progress bar full.
  - Status colors must come from `Theme.kt` (`OmnilogColors`) to keep the palette centralized.
  - Progress units are derived from `MediaType` because `progressTotal` alone cannot indicate whether the number is episodes/pages/minutes/hours.
- **What changed in the system:**
  - New repository files:
    - `OpenLibraryMetadataRepository.kt`
    - `BookGenreNormalizer.kt`
  - Modified repository/metadata composition behavior for books.
  - Modified DAO/repository/ViewModel paths to save session details atomically.
  - Modified detail/header/current session UI.
  - Added string resources for session editor, dates, rating, and progress units.
  - Added `androidx.compose.material:material-icons-core:1.7.8`.
- **What was discussed but NOT yet implemented:**
  - Font change: user asked if app would benefit from a less generic font; no font change was implemented.
  - Further navbar icon usage/polish.
  - Later UI slices 4 and 5 from the prior plan were deferred.
  - Dragging items/cards into collections remains deferred.
- **Open threads or unresolved questions:**
  - The current session card needs user visual review after these iterations.
  - Whether the date/progress/rating hierarchy is final is not confirmed.
  - Whether to apply progress units to home cards/history summaries is undecided; this session only updated current session detail display/editor hint.

### 5. WHAT COULD GO WRONG

- **Known bugs or issues:**
  - No known compile errors; latest `assembleDebug` passed.
  - Existing local `gradle.properties` is modified and must remain uncommitted.
  - Existing untracked older continuation docs are present; do not assume they should be committed.
- **Edge cases to watch for:**
  - Long date labels may crowd on narrow screens now that start/end share one line.
  - Movie progress is treated as minutes and game progress as hours; this matches current provider totals but may not fit every item.
  - Books use `pagines` without accent to match the current strings file style/encoding.
  - Date picker stores `YYYY-MM-DD`; invalid typed dates are ignored on save via parse-or-null.
  - Completed edit sets progress to max when selected; ensure future changes do not accidentally auto-change status from progress.
- **Technical debt or shortcuts taken:**
  - `CurrentSessionSection.kt` is large and contains the full editor; user suggested maybe moving edit page to a separate file earlier, but it was not split yet.
  - Some non-ASCII comments/strings show encoding artifacts due to prior file encoding issues.
  - Status colors are centralized, but the theme may still need design review.
- **Assumptions being made that could be wrong:**
  - Anime/TV progress means episodes.
  - Book progress means pages.
  - Movie progress means minutes.
  - Game progress means hours/playtime.
  - Current Material icons core set is sufficient; avoid switching to extended without asking.

### 6. HOW TO THINK ABOUT THIS PROJECT

1. **Core architectural pattern/design philosophy:** Keep a conservative layered Android architecture: Compose UI calls ViewModel/repository methods, repositories own persistence/provider logic, Room stores authoritative local state. This keeps metadata suggestions separate from user tracking truth.
2. **Most common mistake a new person would make:** Treat provider metadata as authoritative and overwrite user tracking state, or auto-change status based on progress. Do not do that.
3. **What looks like it should be refactored but intentionally should NOT be:** The provider/search system and tracking-session model may look duplicative, but the separation is intentional. Metadata helps create items; user sessions represent personal tracking history. Do not collapse sessions into item-level progress or reintroduce in-item season progress.

### 7. DO NOT TOUCH LIST

- Do NOT refactor stable, working systems without being asked.
- Do NOT redesign architecture unless explicitly instructed.
- Preserve existing naming conventions.
- Maintain previously chosen tradeoffs documented above.
- Ask before introducing new frameworks, libraries, or dependencies.
- Do NOT commit `gradle.properties`, API keys, local paths, or secret changes.
- Do NOT make manual add the primary flow again.
- Do NOT auto-change status based on progress.
- Do NOT merge rewatch/reread/replay into the same tracking session.
- Do NOT remove metadata provenance fields.
- Do NOT force TMDb/provider metadata into Catalan unless asked.
- Do NOT reintroduce season progress inside a media item unless explicitly requested.
- Do NOT commit UI redesign iterations until the user approves.

### 8. CONFIDENCE & FRESHNESS

- **Project identity:** ✅ HIGH CONFIDENCE — verified through continuation docs and current repo.
- **Current built state:** ✅ HIGH CONFIDENCE — inspected and built this session.
- **Architecture map:** ✅ HIGH CONFIDENCE — based on current files and modified code paths.
- **Recent work:** ✅ HIGH CONFIDENCE — implemented and verified this session.
- **Known risks:** ⚠️ MEDIUM — compile verified; visual/device behavior still needs manual review.
- **Future/deferred items:** ⚠️ MEDIUM — carried from user discussion and prior docs.
- **Provider/API behavior details:** ⚠️ MEDIUM — code changed locally; live API quality remains external and variable.

---

## RESUME PROMPT

```text
You are resuming work on the Omnilog Android App.

Before doing anything else, read the attached file in full:

AI_Continuation_Document-16May2026-1630.md

After reading it:

1. Check for a USER DIRECTIVE below.
2. Summarize your understanding of the current project state in 3–5 sentences.
3. Confirm the next action you will take.
4. Ask clarification questions ONLY if something blocks execution.
5. Then begin working.

Important operating rules:
- Preserve the existing architecture and naming conventions.
- Do not refactor stable systems unless explicitly asked.
- Do not introduce new frameworks, libraries, or dependencies without asking.
- Keep UI labels Catalan-facing.
- App name is Omnilog.
- TMDb metadata currently uses English (`en-US`); do not force provider metadata into Catalan unless asked.
- API metadata is the normal/default source for creating items, but it remains suggested/default metadata, not authoritative tracking state.
- User tracking state remains separate and authoritative.
- Rewatch/reread/replay must remain modeled as separate tracking sessions.
- Do not auto-change status based on progress.
- Different seasons/entries should remain modeled as separate media items where possible; collections/franchises group related items.
- Preserve metadata provenance via `metadataSource` and `metadataExternalId`.
- Do not reintroduce in-item season progress management unless explicitly requested.
- Do not make manual add the primary flow again; it is a fallback.
- Do not commit API keys or local secret changes such as `gradle.properties`.
- Dragging cards/items together to create or add to a collection was intentionally deferred to later UI polish.
- During UI redesign iterations, do not commit until explicitly approved.
- Use frequent coherent commits once approved.
- Run `.\gradlew.bat assembleDebug` after code changes.
- Provide a short file-by-file explanation and manual test after each implementation.

Current workspace path:
C:\Users\nilpo\Documents\Documents\Personal\Content-tracking-android-app

---

USER DIRECTIVE:
```
