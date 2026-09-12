# Add Flow And Book Metadata

Status: Current feature specification / remaining work  
Last reviewed: 2026-09-12

This document keeps only the current add-flow rules and the verified remaining book-edition gaps. The old phased implementation plan is archived.

## Add Flow

The add experience is status-first.

A user can add an item directly as:

- Planned;
- In progress;
- Completed;
- Paused;
- Dropped.

Collection and ownership remain visible regardless of status.

Status-specific tracking fields should appear only when relevant. Optional low-frequency fields remain behind a secondary `More details`-style path rather than making the primary add sheet dense.

The primary action should describe user intent (`Add as planned`, etc.), not internal session implementation.

Duplicate detection happens before creating a new tracked item.

## Search

Local results remain immediate and visually distinct from provider/API results.

Remote search should avoid stale-result races and unnecessary provider detail fetches.

Book ISBN search should prioritize exact edition identity over fuzzy title matching.

## Book Edition Selection

Books are edition-aware during search/add.

The current provider model can expose edition candidates with data such as:

- external/provider edition ID;
- title;
- release year;
- language;
- page count;
- cover;
- ISBN;
- format;
- publisher.

OpenLibrary and Google Books may provide complementary edition data when there is trustworthy identity evidence.

Do not automatically merge unrelated editions based only on similar title/author/year.

## Current Remaining Gap

Edition-aware provider selection exists, but `MediaItem` itself still does not persist a full first-class `BookEditionMetadata` object/identity.

Therefore the remaining work is to decide and implement the smallest durable persistence/display model needed for selected edition information that should survive after add/refresh/import.

Before implementing, verify which fields are already represented indirectly through existing media metadata and import-provider references so data is not duplicated unnecessarily.

Likely durable edition concepts to evaluate include:

- selected edition provider/external ID;
- normalized ISBN;
- format;
- publisher;
- edition/work relationship if it materially improves future matching;
- edition-specific release/language/page/cover data where existing media fields are insufficient to preserve identity.

Do not introduce a separate complex work/edition subsystem unless the product behavior requires it.

## Metadata Ownership

Manual/local metadata overrides are protected during refresh/linking.

Metadata linking and refresh use preview/selective-apply behavior when an existing local value would be replaced.

This is already delivered; do not reimplement the old plan's “linking still needs confirmation” item.

## Detail Presentation

When durable edition identity is implemented, the detail page should expose useful edition information compactly, for example language/format/page count/ISBN, without turning the hero into a bibliographic database dump.

## Validation

High-value tests for future book-edition work include:

- exact ISBN identity;
- non-ISBN similar editions remain distinct;
- selected provider edition survives persistence/restore if persistence is added;
- local metadata overrides survive refresh;
- duplicate detection remains correct;
- backup compatibility for any new persisted fields;
- Room migration if schema changes.

## Implementation References

- `core/model/MetadataSuggestion.kt` / `BookEditionMetadata`
- `core/repository/OpenLibraryMetadataRepository.kt`
- `core/repository/GoogleBooksMetadataRepository.kt`
- `core/repository/CompositeMetadataRepository.kt`
- `ui/add/AddMediaScreen.kt`
- import book resolution under `core/imports`

Historical phase details are in `docs/archive/implementation/add-and-book-metadata-remake-plan.md`.
