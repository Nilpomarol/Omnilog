# Provider Imports And Enrichment

Status: Current feature specification  
Last reviewed: 2026-09-12

This document defines the current provider-import/enrichment behavior. Detailed implementation history and release verification are archived.

## Supported Sources

- MyAnimeList account import through the official API;
- MyAnimeList XML fallback;
- IMDb CSV;
- StoryGraph CSV.

Games currently have no provider import.

## Safety Rules

- Imports are additive.
- Existing local tracking data is never replaced by enrichment.
- Backup restore is the normal destructive replace-all flow.
- Duplicate detection should use stable provider identity where possible.
- Provider failures must not roll back already-imported local tracking data.
- Re-importing the same source should be idempotent when stable identifiers are available.
- Manual/local metadata overrides remain protected unless the user explicitly approves replacement.

Preserve:

- sessions;
- progress/activity;
- personal ratings;
- notes/reviews;
- ownership;
- collections and collection order.

## Matching Confidence

Automatic exact matching requires stable identity evidence, such as:

- MAL anime ID;
- IMDb ID resolved through the movie/TV provider path;
- normalized ISBN-10/ISBN-13 for books.

Title/year/author/original-title similarity can rank review candidates, but should not authorize an automatic destructive link by itself.

## Enrichment

For trustworthy matches:

- fill useful missing metadata automatically;
- update provider-owned data where safe;
- preserve manual/user-owned metadata;
- surface conflicting or uncertain values for review.

Imported local tracking state exists independently of whether enrichment succeeds.

## Durable Batch Behavior

Import enrichment can be long-running and should remain durable across normal process interruptions.

The current implementation supports progress plus recovery-oriented actions such as pause/resume/retry/cancel where applicable.

Do not replace this with an in-memory-only foreground loop.

## MAL

- MAL account sync/import uses authenticated provider identity.
- XML remains a fallback/import path.
- Inbound MAL import must not immediately create an outbound-sync feedback loop.
- MAL-only remote titles must not be deleted by local sync behavior.

## IMDb

- Imported IMDb IDs are the stable source identity.
- Unsupported source rows should be rejected/explained rather than coerced into incorrect media types.
- Ambiguous provider candidates require review.

## StoryGraph / Books

- ISBN is the strongest exact edition identity where available.
- Provider enrichment may combine complementary book metadata when the edition identity is trustworthy.
- Missing page count/cover data should remain missing rather than being invented.

## Errors And Review

Before confirmation, import previews should explain invalid/unsupported rows with useful bounded examples where practical.

Uncertain matches, provider incidents, and incomplete metadata should remain reviewable rather than being silently guessed.

## UI Entry Points

The Settings import hub remains the discoverable central entry point.

Matching media sections may also expose contextual provider-import actions, but each contextual action should launch only the provider flow relevant to that section.

Every provider import goes through one import sheet (`ui/imports/ImportSheet.kt`):

1. a guide saying where the export lives and how to get it, with the file picker (and, for MAL with a connected account, the account import);
2. a preview with the count of new titles, the skipped rows folded by reason, the anime title language where it applies, and one confirm button.

Read and validation errors are shown in the sheet, next to the button that picks another file. MAL's `.xml.gz` export is read as-is.

## Implementation References

- import parsers in `core/repository` / `core/imports`;
- `core/imports/ImportEnrichmentManager.kt` and worker flow;
- `ui/imports`;
- MAL integration under `core/mal`;
- provider characterization/import tests;
- connected preservation tests under `androidTest`.

See `docs/archive/implementation/import-enrichment-plan.md` and `docs/archive/delivery/import-enrichment-release-readiness.md` only when implementation/release history is specifically needed.
