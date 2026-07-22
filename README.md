# Omnilog

Omnilog is a native Android personal media tracker for anime, books, movies/TV, and games. It is local-first: Room is the source of truth, while external providers enrich local items without owning the user's tracking history.

## What It Tracks

- media items and collections
- sessions and progress updates
- personal ratings and notes
- ownership
- provider metadata and external ratings
- imports from MyAnimeList XML, IMDb CSV, and StoryGraph CSV
- backup export/import/restore

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- Gradle wrapper
- Android min SDK 26, target SDK 36, compile SDK 36

## Build

From the repository root:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

If Android Studio's bundled JBR is needed on Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

## Provider Credentials

Provider keys are optional. The app builds without them, but metadata enrichment may be limited.

Supported keys:

- `TMDB_API_KEY`
- `GOOGLE_BOOKS_API_KEY`
- `RAWG_API_KEY`
- `OMDB_API_KEY`
- `MAL_CLIENT_ID`

Do not commit real keys to this repository. Use environment variables or user-level Gradle properties instead.

Environment variable example:

```powershell
$env:TMDB_API_KEY='your-key'
.\gradlew.bat assembleDebug
```

User-level Gradle property example:

```properties
# C:\Users\<you>\.gradle\gradle.properties
TMDB_API_KEY=your-key
GOOGLE_BOOKS_API_KEY=your-key
RAWG_API_KEY=your-key
OMDB_API_KEY=your-key
MAL_CLIENT_ID=your-client-id
```

For MAL account synchronization, register `omnilog://mal-oauth` as the application's OAuth redirect
URI in MyAnimeList. Omnilog uses the authorization-code flow with PKCE, stores tokens behind Android
Keystore encryption, and only sends data from Omnilog to MAL after the first bulk sync is confirmed.
MAL-only titles are never deleted.

The tracked root `gradle.properties` intentionally contains blank credential placeholders only.

## Documentation

- [Roadmap](docs/omnilog-roadmap.md)
- [UI Design Direction](docs/omnilog-ui-design-v1.md)
- [Stats System Plan](docs/omnilog-stats-system-plan.md)
- [Development Guide](docs/development-guide.md)

## Project Guardrails

- Keep imports additive; only backup restore may replace data.
- Preserve sessions, progress history, ratings, reviews, ownership, and collections during metadata work.
- Keep provider-specific imports scoped to their section pages.
- Ask before introducing major new frameworks or dependencies.
- Keep secrets out of tracked files.
