# Omnilog

Omnilog is a native Android personal media tracker for anime, books, movies/TV, and games. It is local-first: Room is the source of truth, while external providers enrich local items without owning the user's tracking history.

## What It Tracks

- media items and collections
- sessions and progress/activity updates
- personal ratings and notes
- ownership
- provider metadata and external ratings
- imports from MyAnimeList, IMDb, and StoryGraph
- backup export/import/restore

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- WorkManager
- Navigation 3
- Gradle wrapper
- Android min SDK 26, target SDK 36, compile SDK 36

## Build

From the repository root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain -q
```

Run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain -q
```

Use the equivalent `./gradlew` commands on Unix-like systems.

If Android Studio's bundled JBR is needed on Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## Provider Credentials

Provider keys are optional. The app builds without them, but metadata enrichment may be limited.

Supported keys include:

- `TMDB_API_KEY`
- `GOOGLE_BOOKS_API_KEY`
- `RAWG_API_KEY`
- `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET`
- `OMDB_API_KEY`
- `MAL_CLIENT_ID`

Do not commit real keys to this repository. Use environment variables, user-level Gradle properties, or the ignored root `gradle.properties` copied from `gradle.properties.example`.

For MAL account synchronization, register:

```text
omnilog://mal-oauth
```

as the OAuth redirect URI.

Direct IGDB client-secret use is appropriate only for local/development builds; a distributed Android application cannot keep a client secret private.

## Documentation

Start with the [documentation index](docs/README.md).

Most development tasks should only need:

- [Agent instructions](AGENTS.md)
- [Current roadmap](docs/current/roadmap.md)
- [Current design direction](docs/current/design.md)
- [Development guide](docs/current/development-guide.md)
- one relevant document under [`docs/features/`](docs/features/)

Historical implementation plans and delivery records live under `docs/archive/` and are intentionally non-authoritative.

## Project Guardrails

- Keep imports additive; backup restore is the normal destructive replace-all flow.
- Preserve sessions, progress/activity history, ratings, reviews/notes, ownership, and collections during metadata operations.
- Keep provider imports discoverable in Settings; contextual entry points should launch only the matching provider flow.
- Ask before introducing major new frameworks or dependencies.
- Keep secrets and local-machine configuration out of tracked files.
- Prefer focused changes over broad architecture rewrites.
