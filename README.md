# Omnilog

[![CI](https://github.com/Nilpomarol/Omnilog/actions/workflows/ci.yml/badge.svg)](https://github.com/Nilpomarol/Omnilog/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A native Android tracker for everything you watch, read and play: anime, books, movies/TV and games, in one place.

Omnilog is **local-first**. The on-device database is the source of truth for your history, while external providers such as AniList, TMDB, Open Library and Steam only enrich items with covers, synopses and ratings. They never own or overwrite what you logged.

> **Status:** Functional Android app with tracking, imports, MyAnimeList sync, statistics, goals, backup and restore implemented. The UI is currently in Catalan.

## Why it stands out

- **Four media types, one data model** for sessions, progress, ratings, notes, ownership and collections.
- **Nine metadata providers** integrated for search and enrichment without giving up local ownership of user data.
- **Durable import and sync workflows** that are duplicate-aware, offline-tolerant and recoverable after interruption.
- **Room schema v37 with hand-written migrations**, migration tests and explicit data-safety invariants.
- **70+ test classes** across roughly **57k lines of Kotlin**, with pure application logic kept testable outside Android.

<table>
  <tr>
    <td><img src="docs/screenshots/home-scroll.gif" width="240" alt="Scrolling through Home"></td>
    <td><img src="docs/screenshots/library-detail.gif" width="240" alt="Browsing the anime library and opening an item"></td>
    <td><img src="docs/screenshots/timeline-stats.gif" width="240" alt="Stats and timeline"></td>
  </tr>
</table>

<table>
  <tr>
    <td><img src="docs/screenshots/home.png" width="240" alt="Home: continue watching, up next, and yearly pace rings"></td>
    <td><img src="docs/screenshots/library.png" width="240" alt="Anime library with status filters and progress"></td>
    <td><img src="docs/screenshots/detail.png" width="240" alt="Item detail with progress, personal rating and external ratings"></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/timeline.png" width="240" alt="Library-wide timeline of started and completed items"></td>
    <td><img src="docs/screenshots/stats.png" width="240" alt="Yearly statistics with completions per month and rating distribution"></td>
    <td><img src="docs/screenshots/profile.png" width="240" alt="Profile with totals per media type"></td>
  </tr>
</table>

## Features

- **Progress and activity.** Log episodes, pages, minutes or hours per session. Every update is kept, so re-watches and re-reads preserve their own history.
- **Timeline and calendar.** A library-wide chronology derived from activity history rather than stored separately.
- **Stats and goals.** Yearly comparisons, rating distributions and personal goals with pace tracking such as `2 behind schedule · 101 days left`.
- **Metadata enrichment.** Search, link and refresh from AniList, MyAnimeList, TMDB, OMDb, Open Library, Google Books, RAWG, IGDB and Steam. Refreshes show a preview and never overwrite local edits.
- **Imports.** MyAnimeList XML or account data, IMDb CSV and StoryGraph CSV. Imports are additive and duplicate-aware; enrichment runs as a durable background job that can be paused, resumed, cancelled and recovered after a crash.
- **MyAnimeList sync.** Push progress back to MAL through an offline-tolerant queue without deleting MAL-only titles.
- **Backup and restore.** Full export/import plus automatic backups.
- **Themes.** System, light and dark themes, each with its own palette.

## Engineering

- **Versioned Room schema.** Schema v37 uses hand-written migrations, with every schema version exported under [`app/schemas`](app/schemas) and recent migrations covered by instrumented tests.
- **Data-safety invariants.** Metadata operations must preserve sessions, ratings, notes, ownership and collections. These rules are documented and tested in [`AGENTS.md`](AGENTS.md) and [`HistoryInvariantTest`](app/src/test/java/com/nilpo/contenttracker/core/repository/HistoryInvariantTest.kt).
- **Android-independent application logic.** Stats, timeline, objectives, import parsing and MAL sync payloads are plain JVM-testable logic rather than being tied to UI or Android framework code.
- **Synthetic fixtures.** Test exports under `app/src/test/resources` contain made-up data rather than data from a real account.
- **Feature-level documentation.** Design and behaviour are documented under [`docs/`](docs/README.md).

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · Navigation 3 · Coil · kotlinx.serialization · Gradle (Kotlin DSL)

**Android:** min SDK 26 · target/compile SDK 36

## Getting started

Requirements: JDK 21 (Android Studio's bundled JBR works) and the Android SDK.

```bash
git clone https://github.com/Nilpomarol/Omnilog.git
cd Omnilog
cp gradle.properties.example gradle.properties   # required: enables AndroidX
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows use `.\gradlew.bat` and `copy gradle.properties.example gradle.properties`. Android Studio needs the SDK path in `local.properties` and normally writes it automatically.

The debug build installs as `com.nilpo.contenttracker.debug`, so it does not collide with a release-signed install.

### Provider keys (optional)

The app builds and runs without provider keys, but search and enrichment are limited until they are configured. Add them to the ignored root `gradle.properties`, `~/.gradle/gradle.properties`, or environment variables:

`TMDB_API_KEY` · `GOOGLE_BOOKS_API_KEY` · `RAWG_API_KEY` · `OMDB_API_KEY` · `IGDB_CLIENT_ID` + `IGDB_CLIENT_SECRET` · `MAL_CLIENT_ID`

Keys are compiled into `BuildConfig`, so anyone who can install the APK can extract them. Use restricted or low-value keys, and do not distribute a build containing the IGDB client secret.

For MAL account sync, register `omnilog://mal-oauth` as the OAuth redirect URI.

## Project layout

```text
app/src/main/java/com/nilpo/contenttracker/
  core/   database, repository, imports, mal, stats, timeline, objectives, backup, refresh
  ui/     home, detail, add, timeline, stats, profile, settings, imports, theme
```

## Documentation

Start with the [documentation index](docs/README.md): [roadmap](docs/current/roadmap.md), [design direction](docs/current/design.md), [development guide](docs/current/development-guide.md), and one spec per feature under [`docs/features/`](docs/features/).

## Data providers

Metadata, ratings and cover art come from AniList, MyAnimeList, TMDB, OMDb, Open Library, Google Books, RAWG, IGDB and Steam and belong to their respective owners. This product uses the TMDB API but is not endorsed or certified by TMDB.

## License

[MIT](LICENSE)
