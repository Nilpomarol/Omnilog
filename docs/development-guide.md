# Omnilog Development Guide

This guide captures the practical rules for continuing implementation safely.

## Build And Test Commands

Build debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Use Android Studio's bundled JBR on Windows if needed:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

## Credentials

Provider credentials are optional and should not be committed.

Supported keys:

- `TMDB_API_KEY`
- `GOOGLE_BOOKS_API_KEY`
- `RAWG_API_KEY`
- `OMDB_API_KEY`
- `MAL_CLIENT_ID`

Preferred locations:

- environment variables
- user-level Gradle properties in `~/.gradle/gradle.properties`

Do not put real provider keys in tracked files. If a key was committed or pushed, rotate it with the provider.

## Local-First Rules

- Room is the source of truth.
- Provider metadata enriches local items but must not own user history.
- Imports must be additive and skip duplicates.
- Backup restore is the only destructive replace flow.
- Metadata refresh/linking must preserve user sessions, progress history, personal ratings, reviews, ownership, and collections unless the user explicitly chooses otherwise.
- Provider-specific imports stay scoped to their section pages:
  - Anime: MyAnimeList XML.
  - Movies/TV: IMDb CSV.
  - Books: StoryGraph CSV.

## Important Code Areas

- `core/model`: domain models such as `MediaItem`, `TrackedMedia`, `TrackingSession`, and `ProgressUpdate`.
- `core/database`: Room entities, relations, DAO, and mappers.
- `core/repository/MediaRepository.kt`: repository contract.
- `core/repository/OfflineMediaRepository.kt`: local persistence, imports, metadata refresh/linking, backup serialization.
- `core/repository/*MetadataRepository.kt`: provider integrations.
- `ui/ContentTrackerApp.kt`: top-level navigation, dialogs, import launchers, metadata refresh confirmation.
- `ui/home`: dashboard, section pages, list state, grouping/sorting/filtering.
- `ui/detail`: detail page, session editing, and external rating UI.
- `ui/add`: metadata search and add flow.
- `ui/common`: shared UI helpers and formatters.

## Implementation Priorities

Use [Roadmap](omnilog-roadmap.md) as the standing plan.

Current recommended order:

1. Device and regression QA.
2. Manual external ratings polish.
3. Primary external rating model hardening.
4. Metadata linking overwrite confirmation.
5. Stats system MVP.
6. Import and metadata test coverage.
7. MAL API follow-up.

## Stats Implementation Notes

Use [Stats System Plan](omnilog-stats-system-plan.md) for details.

For the MVP:

- calculate stats in pure Kotlin from existing `TrackedMedia` data
- avoid a Room schema change
- avoid a chart dependency
- add focused unit tests for the calculator
- expose stats as a Home drill-in before considering a bottom navigation item

## Testing Guidance

Add tests when touching:

- import parsers
- duplicate detection
- metadata refresh/linking
- external rating primary behavior
- stats calculations
- backup serialization/restore behavior

High-value test cases:

- MAL XML import fixture parses on Android-compatible XML settings.
- AniList-linked anime keeps MAL id for duplicate detection.
- Metadata refresh preview identifies overwrite vs fill-only changes.
- Selective metadata refresh only applies selected fields.
- Progress-update stats calculate deltas from cumulative values.

## Manual QA Checklist

Before considering a larger feature done:

- app launches
- Home loads
- section navigation works
- detail page opens and back navigation is predictable
- add/import flows still work for relevant sections
- metadata refresh confirmation can apply and cancel
- current session edits save correctly
- external rating dialog can add, edit, delete, and set primary
- user-entered sessions/progress/ratings are preserved after metadata operations

## Git Hygiene

- Keep root session notes out of the repository.
- Keep durable docs in `docs/`.
- Do not commit real API keys, client ids, keystores, APKs, AABs, or local IDE files.
- Review `git diff` before staging.
- Treat unrelated dirty files as user-owned unless explicitly asked to change them.
- If secrets were ever pushed, removing them from the current tree is not enough; rotate them.
