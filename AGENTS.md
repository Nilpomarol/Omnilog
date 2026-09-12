# AGENTS.md

This file is the default entry point for coding agents working in Omnilog.
Keep it small and stable. Feature-specific behavior belongs in the relevant document or code, not here.

## Project

Omnilog is a native Android personal media tracker for anime, books, movies/TV, and games.
It is local-first: Room is the source of truth, while external providers enrich local items without owning the user's tracking history.

Primary stack:

- Kotlin
- Jetpack Compose
- Room
- WorkManager
- Navigation 3
- Gradle
- Coil

Minimum Android SDK 26; target/compile SDK 36.

## Non-Negotiable Data Rules

- Room remains authoritative for user tracking data.
- Provider metadata may enrich local items but must not own or replace user history.
- Imports are additive and should skip duplicates unless the user explicitly chooses otherwise.
- Backup restore is the only normal destructive replace flow.
- Metadata refresh/linking must preserve sessions, progress history, personal ratings, reviews, ownership, and collections unless the user explicitly requests a destructive action.
- MAL synchronization must not delete MAL-only titles.
- Never commit API keys, client secrets, keystores, APKs/AABs, or local machine configuration.

When in doubt, preserve user-entered data.

## Architecture Map

Important production areas:

- `app/src/main/java/com/nilpo/contenttracker/core/model` — domain models.
- `app/src/main/java/com/nilpo/contenttracker/core/database` — Room entities, DAO, relations, mappers, migrations.
- `app/src/main/java/com/nilpo/contenttracker/core/repository/MediaRepository.kt` — main repository contract.
- `app/src/main/java/com/nilpo/contenttracker/core/repository/OfflineMediaRepository.kt` — local persistence plus several import/backup/metadata operations.
- `app/src/main/java/com/nilpo/contenttracker/core/repository/*MetadataRepository.kt` — external metadata providers.
- `app/src/main/java/com/nilpo/contenttracker/core/imports` — import-related code.
- `app/src/main/java/com/nilpo/contenttracker/core/mal` — MyAnimeList synchronization.
- `app/src/main/java/com/nilpo/contenttracker/core/refresh` — background metadata refresh.
- `app/src/main/java/com/nilpo/contenttracker/core/stats` — statistics logic.
- `app/src/main/java/com/nilpo/contenttracker/core/timeline` — activity/timeline logic.
- `app/src/main/java/com/nilpo/contenttracker/ui/ContentTrackerApp.kt` — current top-level app/navigation/orchestration entry point.
- `app/src/main/java/com/nilpo/contenttracker/ui/home` — Home, library sections, filtering/sorting/grouping.
- `app/src/main/java/com/nilpo/contenttracker/ui/detail` — media detail and session editing.
- `app/src/main/java/com/nilpo/contenttracker/ui/add` — search/add flow.
- `app/src/main/java/com/nilpo/contenttracker/ui/imports` — import UI.
- `app/src/main/java/com/nilpo/contenttracker/ui/settings` — settings and import hub.
- `app/src/main/java/com/nilpo/contenttracker/ui/stats` — Stats UI.
- `app/src/main/java/com/nilpo/contenttracker/ui/timeline` — Timeline/activity UI.
- `app/src/main/java/com/nilpo/contenttracker/ui/common` — shared UI helpers.
- `app/src/main/java/com/nilpo/contenttracker/ui/theme` — Omnilog theme, colors, typography.

Do not infer architecture only from file names. Read the relevant implementation before changing behavior.

## Documentation Authority

Read only the documentation needed for the task.

- `README.md` — project overview, credentials, high-level guardrails.
- `docs/development-guide.md` — practical implementation, testing, and Git guidance.
- `docs/omnilog-roadmap.md` — standing product direction; dated build/status notes are historical, not proof of current state.
- `docs/omnilog-ui-design-v1.md` — general UI/design direction.
- `docs/omnilog-home-social-aware-redesign.md` — authoritative Home direction where it conflicts with older Home guidance.
- `docs/omnilog-stats-system-plan.md` and `docs/omnilog-stats-improvement-plan.md` — Stats model and delivered refinement history.
- `docs/omnilog-content-consumption-timeline-plan.md` — Timeline behavior/design.
- `docs/omnilog-import-enrichment-implementation-plan.md` — import/enrichment implementation context.
- `docs/omnilog-import-enrichment-release-readiness.md` — release validation for import/enrichment work.
- `docs/ai-assisted-repository-audit.md` — advisory report for improving agent workflows; it is not a product specification.

If two documents conflict, prefer the document that explicitly supersedes the other or the more narrowly scoped current specification.
Do not treat old delivery notes or completed checklists as current requirements without verifying code and newer docs.

## Change Discipline

- Make the smallest coherent change that fully solves the requested problem.
- Preserve established architecture and behavior unless the task explicitly requires changing them.
- Prefer existing project patterns, Kotlin/Android/Compose capabilities, and current dependencies before introducing new abstractions or libraries.
- Do not introduce Hilt/Dagger, a new architecture layer, multi-module decomposition, or another major framework unless explicitly requested or clearly justified and approved.
- Do not create an abstraction for a single trivial use case merely because it may be useful later.
- Do not refactor unrelated code while implementing a focused task.
- Treat unrelated dirty files as user-owned and leave them untouched.
- Preserve useful comments that explain domain invariants or non-obvious behavior.
- Add comments for `why`, invariants, or constraints; avoid comments that merely narrate obvious code.
- Ask before introducing a major new dependency or framework.

Before adding a new abstraction, dependency, or helper, check in this order:

1. Is it necessary for the requested behavior?
2. Does an equivalent already exist in the repository?
3. Can Kotlin, Android, or Compose provide it directly?
4. Can an existing dependency provide it?
5. Can an existing Omnilog component be adapted cleanly?
6. Can the solution remain local and simple?
7. Only then add a new abstraction or dependency.

## UI Rules

Omnilog should feel editorial, warm, restrained, and cover-led rather than like default Material UI.

- Use the existing theme and semantic colors instead of inventing arbitrary colors.
- Avoid generic AI-style UI: repeated identical cards, excessive chips, decorative gradients, glassmorphism, unnecessary hero sections, and containers around every piece of information.
- Different semantic sections may use different compositions; consistency does not mean identical geometry everywhere.
- Reuse existing components when their semantics match. Do not force reuse when it creates the wrong hierarchy.
- Keep user tracking information visually distinct from provider metadata.
- Preserve accessibility, touch targets, readable contrast, and large-text behavior.

For Home specifically, follow `docs/omnilog-home-social-aware-redesign.md` when it conflicts with older Home implementation or design guidance.

A visual UI change is not complete merely because it compiles.
When the environment supports it, render or run the affected state and inspect the actual output before considering the task finished.
If visual verification is unavailable, state that explicitly rather than claiming the UI was visually validated.

## Build And Test Strategy

Prefer targeted, quiet validation first, then expand only when risk requires it.

Typical progression:

```text
1. Compile Kotlin for the app module.
2. Run the most relevant focused unit test(s).
3. Run the app unit-test suite when appropriate.
4. Assemble debug when broader validation is justified.
5. If a task fails, rerun that task with normal output.
6. Use stack traces or broader diagnostics only when needed.
```

Representative Windows commands:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain -q
.\gradlew.bat :app:testDebugUnitTest --tests "<fully.qualified.TestName>" --console=plain -q
.\gradlew.bat :app:testDebugUnitTest --console=plain -q
.\gradlew.bat :app:assembleDebug --console=plain -q
```

On Unix-like environments use the equivalent `./gradlew` commands.

Run broader checks when touching shared architecture, persistence, imports, migrations, synchronization, navigation, or release-critical behavior.

When changing Room schema/entities/DAO behavior, verify the relevant migration/schema behavior rather than relying only on compilation.

High-value tests exist around imports, duplicate detection, metadata refresh/linking, ratings, stats, backup/restore, navigation, and detail behavior. Search for the closest existing test before creating a new pattern.

## Git And Secrets

- Review the final diff before staging or committing.
- Do not include unrelated formatting or cleanup.
- Do not commit real provider credentials or local Gradle/IDE configuration.
- Root `gradle.properties` is local-only; shared safe defaults belong in `gradle.properties.example`.
- If a secret was ever pushed, removing it from the current tree is insufficient; rotate it.

## Definition Of Done

Before reporting completion:

- requested behavior is implemented;
- user-data invariants still hold;
- the diff is scoped to the task;
- relevant targeted tests/build checks pass, or failures are reported accurately;
- UI changes have been visually inspected when tooling permits;
- documentation is updated only when behavior, architecture, or an authoritative workflow actually changed;
- no unrelated files or secrets were introduced.
