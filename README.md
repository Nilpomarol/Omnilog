# Omnilog

[![CI](https://github.com/Nilpomarol/Omnilog/actions/workflows/ci.yml/badge.svg)](https://github.com/Nilpomarol/Omnilog/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A native Android tracker for everything you watch, read and play: anime, books, movies and TV, and games, in one place.

It is **local-first**. The on-device database is the source of truth for your history, and external providers (AniList, TMDB, Open Library, Steam…) only enrich items with covers, synopses and ratings. They never own or overwrite what you logged.

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

The UI is currently in Catalan.

## Features

- **Four media types, one model.** Anime, books, movies/TV and games share sessions, progress, ratings, notes, ownership and collections.
- **Progress and activity.** Log episodes, pages, minutes or hours per session. Every update is kept, so re-watches and re-reads keep their own history.
- **Timeline and calendar.** A library-wide chronology derived from that history, not stored separately.
- **Stats and goals.** Yearly comparisons, rating distributions and personal goals with pace tracking ("2 behind schedule · 101 days left").
- **Metadata from nine providers.** Search, link and refresh from AniList, MyAnimeList, TMDB, OMDb, Open Library, Google Books, RAWG, IGDB and Steam. Refreshes show a preview and never overwrite local edits.
- **Imports.** MyAnimeList (XML export or account), IMDb CSV and StoryGraph CSV. Imports are additive and duplicate-aware, and enrichment runs as a durable background job that can be paused, resumed, cancelled and recovered after a crash.
- **MyAnimeList sync.** Push progress back to MAL through an offline-tolerant queue. It never deletes MAL-only titles.
- **Backup and restore.** Full export/import, plus automatic backups.
- **Themes.** System, light and dark, each with its own palette.

## Engineering notes

- **Room schema v37** with hand-written migrations, every schema version exported under [`app/schemas`](app/schemas) and covered by instrumented migration tests for recent versions.
- **Data-safety invariants** are written down and tested: metadata operations must preserve sessions, ratings, notes, ownership and collections ([`AGENTS.md`](AGENTS.md), [`HistoryInvariantTest`](app/src/test/java/com/nilpo/contenttracker/core/repository/HistoryInvariantTest.kt)).
- **Pure logic is separated from Android**, so stats, timeline, objectives, import parsing and MAL sync payloads are plain JVM unit tests. The suite has 70+ test classes against roughly 57k lines of Kotlin.
- **Import fixtures are synthetic.** The test exports under `app/src/test/resources` are made-up data, not a real account.
- Design and behaviour are documented per feature in [`docs/`](docs/README.md).

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · Navigation 3 · Coil · kotlinx.serialization · Gradle (Kotlin DSL) · min SDK 26, target/compile SDK 36

## Getting started

Requirements: JDK 21 (Android Studio's bundled JBR works) and the Android SDK.

```bash
git clone https://github.com/Nilpomarol/Omnilog.git
cd Omnilog
cp gradle.properties.example gradle.properties   # required: enables AndroidX
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows use `.\gradlew.bat` and `copy gradle.properties.example gradle.properties`. Android Studio needs the SDK path in `local.properties` (it writes this for you).

The debug build installs as `com.nilpo.contenttracker.debug`, so it never collides with a release-signed install.

### Provider keys (optional)

The app builds and runs without any keys. Search and enrichment are limited until you add them. Set them in the ignored root `gradle.properties`, in `~/.gradle/gradle.properties`, or as environment variables:

`TMDB_API_KEY` · `GOOGLE_BOOKS_API_KEY` · `RAWG_API_KEY` · `OMDB_API_KEY` · `IGDB_CLIENT_ID` + `IGDB_CLIENT_SECRET` · `MAL_CLIENT_ID`

Keys are compiled into `BuildConfig`, so anyone who can install your APK can extract them. Use restricted or low-value keys, and don't distribute a build containing the IGDB client secret.

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

Metadata, ratings and cover art come from AniList, MyAnimeList, TMDB, OMDb, Open Library, Google Books, RAWG, IGDB and Steam, and belong to their respective owners. This product uses the TMDB API but is not endorsed or certified by TMDB.

## License

[MIT](LICENSE)
