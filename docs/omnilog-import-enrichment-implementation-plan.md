# Omnilog Import Enrichment Implementation Plan

## Purpose

This document defines the implementation plan for making provider imports a complete library-onboarding flow rather than a file parser followed by hundreds of manual metadata links.

The primary experience is a direct MyAnimeList account import through the official MAL API. MAL XML remains a supported fallback. IMDb CSV and StoryGraph CSV continue to provide the other provider entry points, with imported identifiers used to resolve richer metadata in batches.

Status: planned. The current Room database version is 23.

## Outcome

After an import, Omnilog should:

- add only titles that are not already in the local library
- resolve exact provider identities without asking the user to search title by title
- fill missing metadata automatically when the match is trustworthy
- collect uncertain matches and overwrites into one review flow
- show durable progress and allow cancel, resume, and retry
- preserve all local tracking and user-authored data
- avoid repeating titles that a previous enrichment attempt already completed

## Current State

The existing implementation already provides useful foundations:

- additive previews and imports for MAL XML, IMDb CSV, and StoryGraph CSV
- Settings and contextual entry points for the matching media sections
- duplicate detection during import
- stable `malId` persistence and MAL-aware anime duplicate detection
- direct metadata refresh for an item already linked to Jikan, AniList, or another supported provider
- metadata refresh/link previews with field-level selection and local-override protection
- an authenticated MAL connection and a durable WorkManager-backed outbound synchronization queue
- a local-first Room repository in which provider metadata is separate from sessions and progress history

The current limitations are:

- MAL requires an XML file even when the user has connected an account
- import results expose counts but not a durable batch or the inserted media IDs
- imported IMDb and StoryGraph identities cannot currently be resolved by the generic metadata refresh path
- enrichment is launched one title at a time from the detail page
- there is no batch progress, cancellation, retry, or consolidated review
- parser, import, metadata-diff, and selective-apply coverage is incomplete

Two roadmap items are already delivered and should not be reimplemented:

- AniList-linked anime preserves the MAL ID used by duplicate detection
- exact MAL IDs can already seed direct Jikan/MAL detail retrieval once an item enters metadata refresh

## Product Rules

### Local-first safety

- Room remains the source of truth.
- Provider imports are additive. Backup restore remains the only replace-all operation.
- Existing sessions, progress entries, personal ratings, notes/reviews, ownership, collections, and collection order are never changed by enrichment.
- Locally overridden metadata fields remain protected unless the user explicitly selects them in review.
- Re-importing the same source must be idempotent wherever a stable provider identifier is available.
- A provider failure must never roll back successfully imported local tracking data.

### Matching confidence

Only stable external identifiers can produce an automatic exact match:

- MAL anime ID
- IMDb title ID resolved by TMDB
- normalized ISBN-10 or ISBN-13 returned by a book provider

Title, year, author, original title, and media type are ranking signals. They can produce review candidates, but they do not authorize an automatic link.

### Metadata application

For an exact match:

- apply fields only when the local value is empty and the provider value is useful
- apply provider ratings and credits only when their provider-owned slots are empty or can be updated without touching manual entries
- keep populated differences for review
- never auto-select a locally overridden field

For an uncertain match:

- do not link or modify metadata before the user chooses a candidate
- after candidate selection, apply safe empty-field fills and present remaining differences for field-level review

## Target User Experience

### MyAnimeList account import

When `MAL_CLIENT_ID` is configured, the import hub presents `Importa des del compte de MyAnimeList` as the primary anime import action.

1. If needed, the user connects through the existing MAL OAuth flow.
2. Omnilog reads the account list in pages and shows preview counts.
3. The preview distinguishes new titles, existing MAL-ID matches, unsupported rows, and conflicts requiring review.
4. The user confirms the additive import.
5. Omnilog writes the tracking data and begins metadata enrichment.
6. Progress remains visible after the dialog is dismissed.
7. The user can cancel, resume, retry failures, or open the consolidated review list.

MAL XML remains available as `Importa un fitxer XML de MyAnimeList` for disconnected accounts, missing client configuration, offline archives, and manual exports.

### IMDb and StoryGraph imports

The existing file pickers and previews remain. After writing the new titles, both sources enter the same durable enrichment flow used by MAL:

- IMDb IDs are resolved through TMDB.
- StoryGraph ISBN values are resolved through the available book providers.
- StoryGraph UIDs and failed exact lookups fall back to review candidates ranked by title and author.

### Batch status surface

Settings gains a persistent import-enrichment status entry showing:

- source and start time
- processed and total title counts
- automatically enriched count
- review count
- no-match and failure counts
- current state: running, paused, completed, or completed with issues
- actions for review, cancel/pause, resume, and retry failures

The immediate post-import result links to the same surface. Contextual imports and Settings imports do not create separate implementations.

## MAL API Import Design

### Direction and trust

The MAL account import is an explicit inbound operation. It does not turn the existing one-way outbound synchronization into continuous two-way sync.

- The local database remains authoritative after import.
- Later local changes can continue to use the existing outbound MAL synchronization flow.
- MAL-only deletions never delete local titles.
- A later MAL import previews conflicts rather than silently replacing local tracking values.

### Data mapping

Import every field the official API reliably exposes for the list entry:

- MAL anime ID
- title and supported basic node metadata
- list status
- watched episode count
- personal score
- notes/comments when available
- start and finish dates
- rewatch count/state when the current Omnilog session model can represent it honestly

Request useful supported fields with the paginated list call. Fetch per-anime detail only for metadata that is not available in the list response and is needed by enrichment. This avoids one network request per title where possible.

The importer maps API data into the same normalized internal import row used by MAL XML. XML and API parsing should differ only at the source adapter boundary; duplicate detection, persistence, and enrichment must share one implementation.

### Pagination, authentication, and recovery

- Persist the next page/cursor or equivalent progress after each successful page.
- Refresh expired access tokens through the existing token store and MAL client.
- Treat authorization failures as reconnect-required, not as permanent per-title failures.
- Use conservative retry/backoff for transient network errors and rate limits.
- Preserve already written pages and completed enrichment items when work pauses or fails.
- Expose partial completion honestly in the batch status.

### Preventing a feedback loop

`addTrackedMedia` currently calls the MAL outbound queue callback. API and XML imports must be able to suppress that callback while writing inbound MAL state.

Implementation rule:

- add an explicit import write context or repository option that prevents imported MAL rows from being queued for outbound sync
- never use a global mutable flag
- after the import completes, do not bulk-queue those titles automatically
- if the user later enables or explicitly requests outbound synchronization, the normal sync preview/confirmation rules apply

Tests must prove that importing from MAL does not immediately send the same state back to MAL.

### Official API and Jikan roles

- Prefer the official MAL API for account data and fields it supplies reliably.
- Use the stable MAL ID for exact identity throughout the flow.
- Keep Jikan as a fallback for public anime metadata missing from the official response or when no MAL client ID is configured.
- Do not make an official MAL credential mandatory for building or using the app.
- Keep provider failures isolated so Jikan fallback cannot erase successful official data.

## Resolution Architecture

Add a focused `ImportedMetadataResolver` rather than teaching the general search repository to interpret import provenance.

The resolver accepts an imported media item and returns one of:

```text
Exact(provider reference)
Candidates(ranked provider references)
NoMatch
Unavailable(reason)
Failed(retryable, diagnostic)
```

Provider references contain only the stable information required to fetch a fresh suggestion during application or review: provider, external ID, media type, and confidence evidence. Full provider payloads should not be stored in the queue because they become stale and are unnecessarily large.

### MAL resolver

- Read the explicit `malId` first.
- Seed the existing direct Jikan/MAL detail path with that ID.
- Treat a valid response for the same MAL ID as exact.
- Do not fall back to automatic title matching when the ID is missing or invalid; return ranked candidates for review.

### IMDb resolver

- Normalize and validate the imported IMDb title ID.
- Resolve that ID through TMDB's external-ID lookup capability.
- Respect the imported movie/TV type.
- Treat a unique provider result for the same IMDb ID as exact.
- If exact resolution fails, search by title and media type and rank with release year/original title; all such results require review.
- Report TMDB as unavailable when its required credential is absent.

### StoryGraph resolver

- Normalize ISBN input by removing separators and validating ISBN-10/13 shape.
- Search OpenLibrary and Google Books using the ISBN.
- Accept automatically only a candidate whose returned edition ISBN normalizes to the imported ISBN.
- Keep records from different providers separate unless their ISBNs match.
- Treat a non-ISBN StoryGraph UID as import provenance, not as a provider identity.
- Rank title/author candidates for review when exact ISBN resolution is unavailable.
- OpenLibrary remains usable without a configured Google Books key.

## Durable Batch Model

Use Room migration 23 to 24 and add two entities covering the complete import lifecycle, including MAL API preview staging. This avoids a split in which page progress is durable only after the user confirms the import.

### `import_batches`

Suggested fields:

- `id`
- `source` (`MalApi`, `MalXml`, `ImdbCsv`, `StoryGraphCsv`)
- `state` (`Previewing`, `ReadyToImport`, `Importing`, `Enriching`, `Paused`, `Completed`, `CompletedWithIssues`)
- `totalCount`
- `createdAtEpochMillis`
- `updatedAtEpochMillis`
- `completedAtEpochMillis`
- source-level continuation data needed for MAL pagination
- source-level diagnostic or reconnect requirement

### `import_batch_items`

Suggested fields:

- `id`
- `batchId`
- `sourceKey`, stable within the source file/account response
- `sourceExternalId`, such as MAL ID, IMDb ID, or StoryGraph ISBN/UID
- `normalizedPayloadJson`, used only while the previewed row has not yet been persisted locally
- `mediaItemId`, nullable until the user confirms the import
- `state`
- `providerSource`
- `providerExternalId`
- `matchKind`
- `attemptCount`
- `lastError`
- `updatedAtEpochMillis`
- `lastAttemptAtEpochMillis`
- `completedAtEpochMillis`

Use `id` as the primary key and a unique batch/source-key index so a resumed MAL page cannot stage the same row twice. The batch foreign key should cascade on batch deletion. The nullable media-item foreign key should become `NULL` if a locally imported title is later deleted, allowing the batch history to remain intelligible.

During MAL API preview, each completed page stages normalized rows and advances the batch continuation value in the same transaction. Confirmation persists only staged rows classified as new, stores their resulting `mediaItemId`, clears their no-longer-needed normalized payload, and advances them into enrichment. Cancelling before confirmation deletes the staged batch without changing the library.

Item states:

| State | Meaning | Retry behavior |
| --- | --- | --- |
| `Pending` | Not attempted | Process normally |
| `Staged` | Previewed but not yet written locally | Persist only after confirmation |
| `Resolving` | Claimed by a worker | Reset to pending after interrupted work |
| `NeedsReview` | Candidate choice or overwrite decision required | Never retry automatically |
| `Applied` | Safe metadata was applied | Never repeat |
| `NoMatch` | No useful candidate exists | Retry only on explicit request |
| `Unavailable` | Credential/provider unavailable | Retry after configuration changes |
| `Failed` | Resolution or application failed | Retry with bounded backoff or explicitly |
| `Skipped` | User chose not to enrich | Never repeat unless explicitly reopened |

Completed rows remain as the durable record that prevents retries from repeating successful work. Old completed batches can be pruned later through a separate retention policy; pruning is not part of the first implementation.

## Worker and Manager

Add an `ImportEnrichmentManager` and WorkManager worker using the existing MAL sync architecture as a reference.

- enqueue one unique work chain per batch
- require network connectivity
- claim and process a small number of pending items per run
- persist the result after every item
- use structured cancellation so an in-flight request stops safely
- on cancel, mark the batch `Paused`, leave unclaimed items `Pending`, and return an interrupted `Resolving` item to `Pending`
- on resume, return the batch to `Enriching` and process only `Pending` items
- on retry, reset only eligible failed/unavailable/no-match items selected by the user
- never reset `Applied`, `Skipped`, or `NeedsReview`
- expose a `StateFlow` derived from Room for progress UI
- persist fetched covers after successful application through the existing cover repository

Provider-specific throttling belongs in the resolver/provider client. The batch manager should understand retryability and backoff, not provider HTTP details.

## Import Repository Changes

Replace count-only import results with a shared result shape containing:

- imported count
- skipped duplicate count
- unsupported count
- inserted media item IDs
- import batch ID

Keep provider-specific preview types if their copy needs different labels, but share duplicate accounting and persistence orchestration where practical.

Import writes should be transactional per normalized row so a failed row does not corrupt the rest of the batch. The batch item must be linked to the inserted media ID before background enrichment can claim it.

Do not refactor all CSV parsing merely because IMDb and StoryGraph currently contain similar scanners. Parser consolidation is allowed only if the fixture tests demonstrate a concrete correctness benefit.

## Safe Metadata Application and Review

Reuse `MetadataRefreshPreview`, `MetadataRefreshChange`, and selective application as the field-level truth.

For an exact resolver result:

1. Fetch fresh provider details.
2. Build the existing metadata preview.
3. Select only changes where `overwritesExistingValue` is false and `isLocallyOverridden` is false.
4. Apply those safe fields immediately.
5. If populated differences remain, mark the queue item `NeedsReview` with the provider reference.
6. Otherwise mark it `Applied`.

For candidates:

1. Store ranked provider references and mark `NeedsReview`.
2. Let the user choose a candidate or skip.
3. Re-fetch current details and rebuild the preview.
4. Auto-apply safe fills.
5. Show the remaining field differences using a reusable version of the existing confirmation UI.

Application must re-read the current local item before writing. A preview produced before a user edit must not overwrite the newer edit.

The provider identity may change from import provenance (`Imdb`, `StoryGraph`) to the selected metadata provider (`Tmdb`, `OpenLibrary`, `GoogleBooks`). Preserve the original stable import identifier on the batch item until the batch is complete so retries do not lose their lookup key.

## UI Structure

Create a focused `ui/imports` package rather than adding the complete feature to `ContentTrackerApp.kt`.

Suggested components:

- `ImportHubState` and import action coordination
- `ImportBatchViewModel`
- `ImportBatchStatusScreen` or sheet
- `ImportReviewScreen`
- reusable metadata-diff field selector extracted from the current metadata confirmation dialog

`ContentTrackerApp.kt` should retain navigation and activity-result launcher ownership, while parsing previews, batch state, and review state move behind focused components.

All new user-facing copy is Catalan. Progress and error states must remain readable at 200% font scale and must not rely on color alone.

## Implementation Phases

### Phase 1 — Characterization tests and normalized import model

1. Add MAL XML, IMDb CSV, and StoryGraph CSV fixtures under test resources.
2. Add parser and mapping tests for real export shapes and edge cases.
3. Introduce normalized import rows/results shared by MAL API/XML where appropriate.
4. Add a repository write option that suppresses outbound MAL queueing for inbound imports.
5. Add tests for import idempotency and no MAL feedback loop.

Exit: existing import behavior is characterized and MAL API rows can enter the same persistence path as XML rows.

### Phase 2 — MAL API account import

1. Extend the MAL client with paginated list reads and required field selection.
2. Add preview aggregation without mutating Room.
3. Add token refresh, partial failure, reconnect-required, and continuation handling.
4. Add the account import entry point and retain XML fallback.
5. Import through the normalized MAL pipeline with exact `malId` identity.

Exit: a connected user can preview and import a MAL account without producing an XML file.

### Phase 3 — Resolver and durable queue

1. Add resolver outcomes and confidence evidence.
2. Implement MAL exact-ID, IMDb-to-TMDB, and StoryGraph ISBN resolution.
3. Add migration 23 to 24, entities, DAO operations, and state invariants.
4. Return imported IDs and create queue rows during import.
5. Add manager/worker processing with pause, resume, and retry.

Exit: imported titles enrich durably in the background and completed work is not repeated.

### Phase 4 — Safe fills and consolidated review

1. Auto-apply only empty, non-overridden fields for exact matches.
2. Extract the metadata-diff selector for reuse.
3. Build batch status and grouped review surfaces.
4. Support candidate selection, field selection, skip, and retry.
5. Preserve state across navigation and process restart.

Exit: a normal import needs no per-title detail-page linking, while every uncertain or destructive decision remains inspectable.

### Phase 5 — Verification and delivery

1. Add repository/integration coverage for selective application and preservation invariants.
2. Run the full unit suite and debug build.
3. Exercise all three providers on a device with offline, cancellation, token-expiry, and retry scenarios.
4. Verify Settings and contextual entry points.
5. Verify 100% and 200% font scale.
6. Update roadmap status and development documentation.

Exit: automated and device checks satisfy the acceptance criteria below.

## Test Matrix

### MAL API

- single and multiple pages
- empty library
- token expiry followed by successful refresh
- reconnect-required authorization failure
- transient failure after completed pages and successful resume
- repeated account import with the same MAL IDs
- existing local anime with the same MAL ID
- conflicting local session values remain unchanged
- unsupported/missing optional dates and scores
- XML fallback produces equivalent normalized rows
- inbound import does not enqueue outbound MAL synchronization

### MAL XML

- representative exported XML fixture
- XML declaration and Unicode titles
- zero/missing MAL ID
- all supported statuses
- zero/unknown dates and episode totals
- comments, tags, scores, and watched episodes
- duplicate rows and an existing MAL-ID match
- external entity/doctype hardening remains effective

### IMDb CSV

- BOM and header variations already accepted by the parser
- quoted commas, escaped quotes, CRLF, and multiline cells
- movies, TV series, mini-series, episodes, and unsupported types
- missing year/runtime/rating
- exact IMDb ID duplicate
- exact TMDB external-ID result
- no exact result with ranked title/year review candidates
- absent TMDB credential

### StoryGraph CSV

- quoted commas, escaped quotes, CRLF, and multiline reviews
- every supported reading status
- owned and borrowed entries
- rating conversion and multiple date formats
- ISBN-10, ISBN-13, separators, invalid ISBN, and StoryGraph-only UID
- exact ISBN provider result
- providers returning different nonmatching editions
- title/author candidates require review
- OpenLibrary operation without Google Books credentials

### Metadata application

- diff generation for empty fills, provider changes, and local overrides
- applying only selected fields
- re-reading after a concurrent local edit
- provider ratings never delete manual ratings or their primary selection
- credits change only when selected
- sessions, progress entries, status history, personal rating, review, ownership, collection, and ordering remain byte-for-byte equivalent
- an applied queue item is not reprocessed by resume or retry

### Queue and UI state

- state transitions and invalid transition rejection
- cancellation between items
- worker interruption while an item is resolving
- retryable and permanent failure behavior
- process recreation with running, paused, and review batches
- correct progress counts when titles are skipped or deleted
- accessible labels and large-text wrapping

## Verification Strategy

Keep matching, transition rules, normalization, and metadata selection as pure Kotlin wherever possible so they run in the existing unit suite.

Add focused Room integration coverage for the parts a pure test cannot prove:

- migration 23 to 24
- queue claiming/state persistence
- selective metadata writes
- preservation of related tracking rows

Use standard Android/Room test dependencies only if required for those integration checks; no production framework is needed.

Provider clients must support injected fakes or transports for pagination, token, rate-limit, and partial-failure tests. Automated tests must not call live provider APIs.

## Acceptance Criteria

- A connected MAL user can import their account without downloading an XML file.
- MAL XML remains a functional fallback and shares the same persistence/enrichment rules.
- Exact MAL, IMDb, and ISBN identities receive available metadata without manual title-by-title linking.
- Title/year/author heuristics never auto-link an uncertain record.
- Empty metadata fills automatically; populated differences and local overrides are reviewed explicitly.
- Import progress survives navigation and process restart.
- Cancelled or failed batches can resume without reprocessing applied titles.
- Missing credentials and offline providers produce actionable states without losing imported tracking data.
- MAL imports do not create an immediate outbound-sync feedback loop.
- Sessions, progress history, personal ratings, reviews, ownership, collections, and collection order survive enrichment unchanged.
- Settings and contextual section entry points still launch only their intended importer.
- The debug build, full unit suite, focused Room tests, and device QA pass.

## Delivery Boundaries

Included:

- direct MAL account import
- MAL XML fallback
- post-import enrichment for MAL, IMDb, and StoryGraph
- durable batch state, progress, cancellation, resume, retry, and review
- targeted parser/import/metadata tests

Not included:

- continuous bidirectional MAL synchronization
- importing games from a new provider
- automatically merging non-ISBN book editions
- deleting local titles because they disappeared from a provider account
- a generic provider-job framework beyond import enrichment
- redesigning the whole Settings screen or add flow
- historical import batch retention controls in the first release

## Likely Code Areas

- `core/mal/MalApiClient.kt`, `MalTokenStore.kt`, and `MalSyncManager.kt`
- `core/repository/MyAnimeListXmlImport.kt`
- `core/repository/ImdbCsvImport.kt`
- `core/repository/StoryGraphCsvImport.kt`
- `core/repository/MetadataRepository.kt` and provider repositories
- `core/repository/MediaRepository.kt`
- `core/repository/OfflineMediaRepository.kt`
- `core/database/ContentTrackerDatabase.kt`, entities, DAO, and migration 23 to 24
- new `core/imports` manager/resolver/worker code
- new `ui/imports` state and screens
- `ui/ContentTrackerApp.kt` for launchers and navigation wiring
- `ui/settings/SettingsScreen.kt` for import and batch status entry points
- `res/values/strings.xml`
- `src/test` fixtures and focused tests, plus targeted Room integration tests if introduced

## Delivery Order

Deliver and review each phase separately. Phase 1 must land before the MAL API or worker implementation so import semantics and the outbound-sync boundary are protected by tests. Phase 2 makes the highest-value import seamless. Phase 3 establishes durable execution for every provider. Phase 4 adds the user decisions that cannot safely be automated. Phase 5 closes the work with regression evidence and documentation.
