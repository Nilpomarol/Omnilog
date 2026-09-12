---
name: provider-import
description: Modify Omnilog provider imports, metadata enrichment, duplicate detection, metadata linking/refresh, MAL synchronization boundaries, or import review flows. Use for MAL, IMDb, StoryGraph, metadata providers, enrichment jobs, or preservation-sensitive provider work.
---

# Provider Import And Enrichment

Provider data enriches the local library; it never owns user history.

## Before editing

1. Read `AGENTS.md` and `docs/features/imports.md`.
2. For book/add-flow work also read `docs/features/add-and-book-metadata.md`.
3. Inspect the specific provider/import path and the repository persistence path before changing behavior.
4. Find the closest characterization/preservation test.

## Non-negotiable behavior

- Imports are additive and duplicate-aware.
- Backup restore is the normal replace-all exception.
- Provider failure must never roll back successfully imported local tracking data.
- Preserve sessions, progress, ratings, notes/reviews, ownership, collections, ordering, and manual metadata overrides.
- Stable external identifiers can authorize exact matches; title/year/author similarities are ranking/review signals, not automatic identity.
- Re-import should be idempotent when a stable source identity exists.
- Imported MAL data must not immediately create an outbound-sync feedback loop.
- Ambiguous matches belong in review rather than silent linking.

## Scope discipline

Do not generalize all providers into one abstraction unless the providers genuinely share behavior. Preserve provider-specific boundaries where API semantics differ.

Do not add a new dependency for parsing/networking when the existing stack already handles the required format safely.

## Validation

Run the nearest tests for:

- parser behavior;
- duplicate detection;
- exact identity resolution;
- preservation of local/user-authored fields;
- retry/cancel/recovery when WorkManager flow changes;
- metadata preview/selective apply when linking or refresh changes.

For user-facing import changes, also verify the relevant preview/review UI and large-text behavior when tooling permits.