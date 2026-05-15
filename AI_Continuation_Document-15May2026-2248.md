# PROJECT CONTINUATION DOCUMENT
## Session 6 — 15 May 2026

### 1. PROJECT IDENTITY

- **Project Name:** Omnilog, formerly Content Tracker Android App.
- **What This Project Is:** A native Android personal media tracker for anime, books, movies/TV, and games, including first-time consumption and rewatches/rereads/replays. It is also a learning project for Kotlin, Jetpack Compose, Room, local-first architecture, and API-backed metadata flows.
- **Primary Objective:** Build a working local-first media tracker where API metadata search is the normal add flow, manual add remains a fallback, and user tracking state remains editable and authoritative.
- **Strategic Intent:** Create a practical personal tracking/library app with Goodreads/StoryGraph-style browsing and rich item pages, while keeping architecture simple enough for learning and iteration.
- **Hard Constraints:**
  - Android native: Kotlin, Jetpack Compose, Material 3, Room, KSP.
  - Preserve layered architecture: Compose UI -> ViewModel -> Repository -> Room DAO/entities -> domain models.
  - UI labels remain Catalan-facing.
  - App name is now **Omnilog**.
  - API metadata is default/suggested item metadata, not authoritative tracking state.
  - User tracking state is separate and authoritative: sessions, progress, status, rating, notes, platform, ownership.
  - Rewatch/reread/replay remain separate tracking sessions.
  - Do not auto-change status based on progress.
  - Different seasons/entries should remain separate media items where possible; collections/franchises group related items.
  - Preserve metadata provenance via `metadataSource` and `metadataExternalId`.
  - Do not reintroduce in-item season progress management unless explicitly requested.
  - Manual add is a fallback, not the primary flow.
  - Do not introduce new dependencies without asking.
  - Do not commit API keys or local secret changes such as `gradle.properties`.
  - Run `.\gradlew.bat assembleDebug` after code changes.
  - For UI redesign iterations, do not commit until the user explicitly approves moving on or asks to commit.

### 2. WHAT EXISTS RIGHT NOW

- **What is built and working:**
  - Android Compose app builds successfully with `.\gradlew.bat assembleDebug`.
  - Bottom navigation now has five destinations: `Inici`, `Anime`, `Llibres`, `TV`, `Jocs`.
  - Omnilog top header exists; `Omni` changes accent color by current destination/section.
  - Soft dark theme and centralized `OmnilogColors` exist.
  - Section accents exist for Anime, Books, TV, Games.
  - Existing section pages still work for Anime/Books/TV/Games.
  - API-backed metadata search exists for all media sections:
    - TMDb for movies/TV.
    - AniList for anime.
    - Google Books for books.
    - RAWG for games.
  - API search -> review -> save creates local `MediaItem` plus first `TrackingSession`.
  - Manual add still exists as fallback.
  - Rich item metadata is persisted:
    - original title, release year, genres, creators, source URL, provider collection title.
    - external rating score/max/vote count.
    - popularity/ranking summary fields.
    - provider JSON snapshots for rating distribution, popularity, and rankings.
  - Structured credits are persisted via `MediaCredit` / `MediaCreditEntity`:
    - authors, directors, creators, studios, developers, cast, voice actors.
  - Detail page uses split metadata components:
    - `MediaMetadataHero`
    - `MediaMetadataHeroGenres`
    - `MediaMetadataSecondary`
    - `MediaMetadataSummary` still combines all for preview/review.
  - Detail ordering is now: old page title/buttons, hero, genre row, current session, item details/secondary metadata, new session section, history.
  - Genres are a one-line horizontally scrollable row.
  - `Resum` in secondary metadata collapses if long, with right-aligned `Veure mes` / `Mostra menys`.
  - JSON backup export/import is schema v3 and includes media credits; older v1/v2 import compatibility remains.
  - Room version is 6 with destructive migration policy still accepted for now.

- **What is partially built:**
  - UI redesign is in progress, not complete.
  - `Inici` exists only as a placeholder landing page, not the planned dashboard.
  - Detail page hero is redesigned, but page-level structure is not finished:
    - title still appears above hero.
    - back/delete actions still sit above hero.
    - item edit/delete is not under a `...` menu.
    - add session is still an in-page section, not a floating action.
    - current session still uses old section style.
  - Media list rows are not redesigned yet.
  - Forms are still mostly inline/full page, not modal-centered.
  - Rich metadata is displayed, but not all provider JSON snapshots have UI visualizations.
  - `sourceUrl` exists but is not yet surfaced as a link/action.
  - Catalan wording is functional but not formally polished.

- **What is broken or blocked:**
  - No known build-blocking issues.
  - Persistent non-blocking git warning: `unable to access 'C:\Users\nilpo/.config/git/ignore': Permission denied`.
  - `gradle.properties` is locally modified and intentionally uncommitted because it contains API keys.
  - Older continuation files remain untracked unless explicitly committed.
  - No automated tests yet.

- **What has NOT been started yet:**
  - Full Home dashboard with stats/current/top/recent sections.
  - Full media list redesign.
  - Modal-based edit/add forms.
  - Metadata refresh/refetch preview/diff flow.
  - Proper image caching/loading library.
  - Stats page.
  - Config/settings page.
  - Cloud sync or remote backup.
  - Automated tests.

### 3. ARCHITECTURE & TECHNICAL MAP

- **Tech stack / tools / platforms:**
  - Kotlin, Android SDK 36 (`compileSdk=36`, `targetSdk=36`, `minSdk=26`).
  - Jetpack Compose, Material 3.
  - Room + KSP.
  - Gradle Android plugin 9.2.1, Kotlin 2.2.10.
  - Built-in `HttpURLConnection` + `org.json`; no OkHttp/Retrofit/Coil.
  - Git branch: `main`.
  - Remote: `git@github.com:Nilpomarol/Content-tracking-android-app.git`.

- **Key data structures, tables, files, or repos:**
  - Repo: `C:\Users\nilpo\Documents\Documents\Personal\Content-tracking-android-app`
  - Entry/application:
    - `MainActivity.kt`
    - `ContentTrackerApplication.kt`
  - App shell:
    - `ui/ContentTrackerApp.kt`
  - Theme/design:
    - `ui/theme/Theme.kt`
    - `ui/home/MediaSection.kt`
    - `docs/omnilog-ui-design-v1.md`
  - Shared metadata UI:
    - `ui/common/MediaMetadataSummary.kt`
  - Add flow:
    - `ui/add/AddMediaScreen.kt`
    - `ui/add/MetadataSearchUiState.kt`
  - Home/list:
    - `ui/home/HomeViewModel.kt`
    - `ui/home/HomeScreen.kt`
    - `ui/home/MediaCard.kt`
    - `ui/home/SectionHeader.kt`
  - Detail:
    - `ui/detail/DetailScreen.kt`
    - `ui/detail/ItemDetailsSection.kt`
    - `ui/detail/CurrentSessionSection.kt`
    - `ui/detail/NewSessionSection.kt`
    - `ui/detail/PastSessionsSection.kt`
  - Domain:
    - `MediaItem.kt`
    - `MediaCredit.kt`
    - `TrackedMedia.kt`
    - `TrackingSession.kt`
    - `MetadataSuggestion.kt`
    - `AddTrackedMediaRequest.kt`
  - Database:
    - `ContentTrackerDatabase.kt` version 6.
    - `MediaItemEntity.kt`
    - `MediaCreditEntity.kt`
    - `TrackingSessionEntity.kt`
    - `MediaDao.kt`
    - `MediaMappers.kt`
  - Repositories:
    - `OfflineMediaRepository.kt`
    - `MetadataRepository.kt`
    - `CompositeMetadataRepository.kt`
    - `TmdbMetadataRepository.kt`
    - `AniListMetadataRepository.kt`
    - `GoogleBooksMetadataRepository.kt`
    - `RawgMetadataRepository.kt`
  - Strings:
    - `app/src/main/res/values/strings.xml`

- **How the system works end-to-end:**
  1. `MainActivity` starts Compose with `ContentTrackerTheme` and `ContentTrackerApp`.
  2. `ContentTrackerApplication` initializes Room and wires repositories.
  3. `HomeViewModel` observes current media section, local Room data, search/filter/sort state, and metadata search state.
  4. `ContentTrackerApp` displays Omnilog shell, top header, bottom nav, add flow, list pages, collection detail, item detail, or Home placeholder.
  5. User taps add in a media section.
  6. Add screen performs debounced metadata search through `CompositeMetadataRepository`.
  7. Composite repository routes to TMDb/AniList/Google Books/RAWG based on selected media types.
  8. User selects a suggestion.
  9. ViewModel calls `getSuggestionDetails`; TMDb and RAWG enrich with detail calls; AniList/Google Books mostly return search data.
  10. Review uses shared `MediaMetadataSummary`, then tracking setup fields.
  11. Save calls `OfflineMediaRepository.addTrackedMedia`.
  12. Repository inserts `MediaItemEntity`, related `MediaCreditEntity` rows, and first `TrackingSessionEntity`.
  13. Room emits updates; UI recomposes.
  14. Detail uses saved `MediaItem` + `MediaCredit` data to render `MediaMetadataHero`, genre row, current session, and secondary metadata.
  15. Backup export serializes v3 data including media credits; import can read v1/v2/v3.

- **Naming conventions or standards in use:**
  - Domain request objects: `Add...Request`.
  - Metadata models: `Metadata...`.
  - Metadata provenance fields: `metadataSource`, `metadataExternalId`.
  - Compose components split by screen/section/editor/shared common components.
  - Repository methods express app use cases.
  - DAO methods operate on entities and database primitives.
  - UI strings are in `strings.xml`; Catalan-facing labels preferred.

- **External dependencies:**
  - TMDb: movies/TV; `TMDB_API_KEY`.
  - AniList: anime; no API key; GraphQL POST.
  - Google Books: books; `GOOGLE_BOOKS_API_KEY`.
  - RAWG: games; `RAWG_API_KEY`.
  - External tracking sources modeled locally include MAL, IMDb, StoryGraph, Goodreads, Letterboxd, TMDb, RAWG, Backloggd, Other.

### 4. RECENT WORK — WHAT JUST HAPPENED (HIGH PRIORITY)

- **What was worked on in this session:**
  - Added display of provider `creators` in metadata review.
  - Implemented RAWG detail fetch so game developers populate `creators`.
  - Expanded item metadata persistence:
    - `MediaItem` gained rich metadata fields.
    - Room `MediaItemEntity` gained matching columns.
    - New `MediaCredit` domain model and `MediaCreditEntity` table.
    - Backup schema moved to v3 with `mediaCredits`.
  - Provider mappings were expanded:
    - TMDb: popularity, directors/creators, cast.
    - AniList: popularity, rankings, score distribution, studios, Japanese voice actors.
    - Google Books: authors as credits.
    - RAWG: developers, rating distribution, popularity snapshot.
  - Added shared metadata presentation:
    - `MediaMetadataUi`.
    - `MediaMetadataSummary`.
    - conversion helpers from `MetadataSuggestion` and `MediaItem`.
  - Documented first Omnilog UI direction in `docs/omnilog-ui-design-v1.md`.
  - Established Omnilog visual foundation:
    - soft dark palette.
    - section accent colors.
    - app rename to Omnilog.
    - `Inici` bottom nav destination.
    - placeholder Home landing.
    - top Omnilog header.
  - Redesigned shared metadata hero:
    - larger rounded cover.
    - title/original title/creator block.
    - compact pills for source+rating, ranking, users, total.
    - media-specific total units.
    - source/rating combined like `AniList 8.1`.
    - `Popularitat` changed to `Usuaris` in pill context.
  - Split metadata components:
    - `MediaMetadataHero`
    - `MediaMetadataHeroGenres`
    - `MediaMetadataSecondary`
    - `MediaMetadataSummary`
  - Moved detail ordering toward target design:
    - hero.
    - single-line scrollable genres.
    - current session.
    - item details / secondary metadata.
    - history.
  - Added collapsible summary:
    - `Resum` max 5 lines when long.
    - right-aligned `Veure mes` / `Mostra menys`.

- **What decisions were made and WHY:**
  - **Do the redesign in slices.** User expects multiple iterations and wants commits only after approval for a slice.
  - **Detail page before media list.** The detail page still has structural mismatch with target design and is the better visual reference before redesigning rows.
  - **Metadata is not quiet.** User clarified provider metadata should have prominent hero placement, while user tracking has its own section below.
  - **Keep preview/detail visually aligned.** Preview should feel like an item page before saving, similar to Goodreads/IMDb/StoryGraph.
  - **Split hero and secondary metadata.** Needed so detail can insert current session between hero/genres and lower metadata while preview can still show everything continuously.
  - **Genres belong to hero.** User clarified genres should sit below cover/title/pills and above current session.
  - **Secondary metadata needs labels/hierarchy.** Summary, cast, studios/authors/developers need clear section labels.
  - **No commit during iteration.** Commit only when user approves. This was explicitly requested and followed.

- **What changed in the system:**
  - Commits added this session, most recent first:
    - `6e50f7c Split metadata hero and details`
    - `bf118bd Refine shared metadata hero`
    - `3f85389 Redesign shared metadata hero`
    - `a9fbf64 Establish Omnilog design foundation`
    - `1179b9c Document Omnilog UI design direction`
    - `c9c19d2 Share metadata summary between preview and detail`
    - `b98dc49 Persist richer item metadata`
    - `7f59a7b Fetch RAWG game developers`
    - `5d3d124 Show creators in metadata review`
  - `MediaMetadataSummary.kt` is now central to the redesign; treat it carefully.
  - `DetailScreen.kt` now imports and uses `MediaMetadataHero` and `MediaMetadataHeroGenres`.
  - `ItemDetailsSection.kt` now uses `MediaMetadataSecondary`.
  - `strings.xml` includes new Omnilog, nav, metadata, total unit, summary, and show-more strings.
  - `docs/omnilog-ui-design-v1.md` was edited by user and committed during this session.

- **What was discussed but NOT yet implemented:**
  - Finish detail page structure:
    - remove standalone title above hero.
    - replace top action buttons with back + vertical `...` menu.
    - item edit/delete under `...`.
    - add/edit session as floating bottom-right action, MAL-style.
    - current session redesign with no heavy section title.
  - Redesign current session/user tracking panel.
  - Redesign media list rows.
  - Build real Home dashboard.
  - Move forms into centered modals or suitable mobile modal patterns.
  - Metadata refresh/refetch preview/diff.
  - Stats page.
  - Config page.

- **Open threads or unresolved questions:**
  - Exact detail page structure still needs iteration.
  - Whether current session needs a label at all remains open; user prefers probably no label.
  - Floating action behavior for add/edit session needs design and implementation.
  - Whether centered modals will work for longer mobile forms remains to be validated.
  - Section/page title style is explicitly outside the just-finished metadata split slice.
  - Home dashboard content and global search remain later work.

### 5. WHAT COULD GO WRONG

- **Known bugs or issues:**
  - No known compile failures.
  - `gradle.properties` is locally modified and must not be committed.
  - Persistent git ignore permission warning.
  - Cover images still use `URL.openStream` in Compose coroutine; no caching, placeholders only.
  - No automated tests.
  - Room destructive migration means local data can be wiped between schema changes.

- **Edge cases to watch for:**
  - Long titles/original titles may still crowd hero on small screens.
  - Genre row is horizontally scrollable; ensure it is discoverable enough.
  - Summary collapse uses character length threshold and max lines; may need tuning.
  - Provider popularity fields are not semantically uniform:
    - AniList popularity ~= users.
    - RAWG popularity snapshot uses `added`.
    - TMDb popularity is a provider score, not user count.
  - Google Books ratings are sparse.
  - RAWG `playtime` is average hours, not a fixed completion total.
  - Movie runtime is stored as progress total in minutes; long-term UX may treat movies differently.
  - TV totals are show-level episodes, not season-specific.
  - AniList voice actor fetch is limited and Japanese-only.

- **Technical debt or shortcuts taken:**
  - No image loading library.
  - No network retry/throttling.
  - No tests for metadata mappings or backup v3.
  - JSON strings are used for provider-specific snapshots.
  - UI components are evolving quickly; some older sections still look inconsistent.
  - `ContentTrackerApp` still manually manages navigation state; no navigation framework.

- **Assumptions being made that could be wrong:**
  - `MediaCreditRole.Studio` is acceptable as anime creator metadata.
  - Generic `creators: List<String>` plus structured `MediaCredit` is enough before typed credit UX matures.
  - Dark-first design will remain preferred.
  - Five-item bottom nav with `TV` is acceptable for movies+TV, though user may rename later.
  - Stats should wait until core detail/list/dashboard UI is stable.

### 6. HOW TO THINK ABOUT THIS PROJECT

1. **What is the core architectural pattern or design philosophy, and why was it chosen?**
   - Local-first layered Android app: Compose UI -> ViewModel -> Repository -> Room DAO/entities -> domain models. External APIs provide metadata suggestions/defaults; local Room data is the durable source of truth for user tracking. This keeps learning complexity manageable while supporting real API-backed workflows.

2. **What is the most common mistake a new person working on this would make?**
   - Treating API metadata as authoritative tracking state, or collapsing rereads/rewatches/replays into one item-level status. Metadata can be prominent and rich, but sessions/status/progress/rating/notes remain user-owned tracking state.

3. **What looks like it should be refactored or redesigned but intentionally should NOT be? Why?**
   - Do not reintroduce nested season progress. The app intentionally models seasons/entries as separate media items and collections/franchises as grouping.
   - Do not add a large navigation or Clean Architecture framework. The single-module layered setup is intentional for learning and speed.
   - Do not make manual add primary. API search-first is the chosen flow.
   - Do not add dependencies casually. Image/network libraries may come later but require user approval.
   - Do not silently refresh metadata into user fields. Refresh must be explicit and previewed.

### 7. DO NOT TOUCH LIST

Explicit rules for the next AI:
- Do NOT refactor stable, working systems without being asked.
- Do NOT redesign architecture unless explicitly instructed.
- Preserve existing naming conventions.
- Maintain previously chosen tradeoffs — they were chosen for reasons documented above.
- Ask before introducing new frameworks, libraries, or dependencies.
- Do NOT commit `gradle.properties`, API keys, local secret changes, or unrelated local config.
- Do NOT auto-change tracking status based on progress.
- Do NOT collapse rereads/rewatches/replays into a single item-level state.
- Do NOT treat API metadata as authoritative tracking state.
- Do NOT make manual add the primary flow again.
- Do NOT reintroduce in-item season progress management unless explicitly requested.
- Do NOT implement drag-and-drop collection UI unless asked; it was intentionally deferred.
- Do NOT force provider metadata into Catalan; UI labels stay Catalan-facing.
- Do NOT commit older continuation documents unless user explicitly asks.
- During UI redesign iterations, do NOT commit until the user approves the iteration or says to move to the next slice.
- Always run `.\gradlew.bat assembleDebug` after code changes.

### 8. CONFIDENCE & FRESHNESS

- **Project identity:** ✅ HIGH CONFIDENCE — updated this session; app renamed to Omnilog.
- **Hard constraints:** ✅ HIGH CONFIDENCE — repeatedly reinforced by user and current implementation.
- **Current build state:** ✅ HIGH CONFIDENCE — `.\gradlew.bat assembleDebug` passed after latest code changes.
- **Metadata provider integrations:** ✅ HIGH CONFIDENCE — implemented and modified this session.
- **Rich metadata persistence:** ✅ HIGH CONFIDENCE — implemented this session, built successfully.
- **Backup v3 details:** ✅ HIGH CONFIDENCE — implemented this session, but not automated-test verified.
- **UI design direction:** ✅ HIGH CONFIDENCE — documented and user-adjusted this session.
- **Shared metadata hero/detail split:** ✅ HIGH CONFIDENCE — implemented and iterated with user feedback this session.
- **Detail page remaining work:** ✅ HIGH CONFIDENCE — known from active discussion, not implemented yet.
- **Home dashboard plan:** ⚠️ MEDIUM — documented direction, not implemented.
- **Stats page:** ⚠️ MEDIUM — direction discussed; implementation not started.
- **Catalan label quality:** ⚠️ MEDIUM — functional labels exist, exact wording may change.
- **Image/network architecture:** ❓ LOW — current approach is pragmatic and should be revisited later.
- **Automated test coverage:** ✅ HIGH CONFIDENCE — none exists.

