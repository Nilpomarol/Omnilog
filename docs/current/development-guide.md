# Omnilog Development Guide

Status: Current  
Last reviewed: 2026-09-12

Repository-wide behavioral and architectural guardrails live in [`../../AGENTS.md`](../../AGENTS.md). This guide only covers practical setup and validation.

## Build

From the repository root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain -q
```

On Unix-like systems use `./gradlew`.

If Android Studio's bundled JBR is needed on Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## Targeted Validation

Prefer the smallest useful check first:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain -q
.\gradlew.bat :app:testDebugUnitTest --tests "<fully.qualified.TestName>" --console=plain -q
.\gradlew.bat :app:testDebugUnitTest --console=plain -q
.\gradlew.bat :app:assembleDebug --console=plain -q
```

If a task fails, rerun that task with normal output. Add `--stacktrace` only when the failure needs it.

Run broader checks when changing shared persistence, imports, synchronization, navigation, or other high-risk behavior.

Room/entity changes require relevant migration/schema verification; compilation alone is not enough.

## Visual Validation

A Compose change is not considered visually verified because it compiles.

When tooling permits:

1. render the affected preview or run the affected screen/state;
2. inspect the rendered output;
3. check long content / empty content where relevant;
4. check enlarged system text for layouts likely to be sensitive;
5. run screenshot validation when a reference test exists.

If rendering is unavailable, report that limitation explicitly.

## Credentials

Provider credentials are optional for building, but missing keys may limit metadata behavior.

Supported keys include:

- `TMDB_API_KEY`
- `GOOGLE_BOOKS_API_KEY`
- `RAWG_API_KEY`
- `IGDB_CLIENT_ID`
- `IGDB_CLIENT_SECRET`
- `OMDB_API_KEY`
- `MAL_CLIENT_ID`

Use environment variables, user-level `~/.gradle/gradle.properties`, or the ignored root `gradle.properties` copied from `gradle.properties.example`.

Never commit real credentials.

For MAL account synchronization, the OAuth redirect is:

```text
omnilog://mal-oauth
```

Direct IGDB client-secret use is suitable only for local/development builds; a distributed Android client cannot keep a client secret private.

## High-Value Test Areas

Search for the closest existing tests before creating a new pattern. Existing coverage is especially important around:

- import parsing and duplicate detection;
- metadata refresh/linking and preservation;
- backup/restore;
- Room migrations;
- Activity and Timeline semantics;
- Stats calculations;
- ratings;
- navigation/detail behavior.

## Manual Regression Checks

For larger user-facing changes, verify the affected subset of:

- app launch;
- Home load and root navigation;
- item detail and back behavior;
- current-session edits;
- provider import entry points;
- metadata refresh/link preview and apply/cancel;
- user-data preservation after provider operations;
- large-text behavior for changed UI.

Do not run the entire checklist mechanically for a tiny isolated change.

## Documentation

Start from [`../README.md`](../README.md) and read only the current/feature document relevant to the task.

Historical plans under `../archive/` are non-authoritative unless the task explicitly needs implementation history.
