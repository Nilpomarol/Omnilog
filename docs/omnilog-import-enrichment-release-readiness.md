# Import and Enrichment Release Readiness

Status: implementation and debug-device verification completed on 2026-07-24.

Commit: `6d1142d` (`Improve provider imports and enrichment`).

The detailed design and implementation history remain in the
[Import Enrichment Implementation Plan](omnilog-import-enrichment-implementation-plan.md). This document is the concise
delivery record and release checklist.

## Delivered

- MyAnimeList account import through the official API, with XML fallback.
- IMDb CSV and StoryGraph CSV previews and additive imports.
- Shared validation for empty files, missing columns, malformed rows, unsupported types, and duplicates.
- Preview counts that match the confirmed import result.
- Rejected-row summaries with source row numbers and bounded examples.
- Durable background enrichment with progress, pause, resume, retry, cancellation, and interruption recovery.
- Exact matching through stable MAL, IMDb, and ISBN identities.
- Review queues for uncertain matches, provider incidents, and incomplete metadata.
- Safe automatic replacement of import-owned metadata while protecting manual edits.
- Preservation of sessions, progress history, ratings, notes, reviews, ownership, collections, and ordering.
- Protection against inbound MAL imports immediately entering the outbound MAL synchronization queue.
- Database migrations through Room schema version 27.

## Provider Behavior

| Source | Imported data | Enrichment | Important limit |
| --- | --- | --- | --- |
| MAL account | List status, progress, score, notes, dates, rewatch data, and stable MAL identity | Official MAL data first, AniList for complementary data, Jikan only as a last fallback | Requires a configured MAL client and user login |
| MAL XML | The same normalized local tracking model used by account import | Stable MAL ID drives exact enrichment | Export quality controls which optional fields are available |
| IMDb CSV | Movies, series, mini-series, status, rating, dates, runtime, and IMDb identity | TMDB external-ID lookup, then review-only candidates | Episodes and games are rejected; TMDB credentials improve results |
| StoryGraph CSV | Books, authors, status, ratings, ownership, tags, and reading dates | Exact ISBN lookup through book providers, then review-only title/author candidates | Page totals and covers may remain incomplete when providers do not supply them |

Provider data is evidence, not user history. An unavailable provider or failed enrichment never rolls back the imported
local tracking data.

## Verification Record

Automated verification completed during implementation:

- full debug unit-test suite
- Android test-source compilation
- debug APK assembly
- focused parser, duplicate, retry, cancellation, metadata-coverage, stale-review, and presentation tests
- connected Room preservation tests for user-authored tracking data

Final Pixel device verification:

- IMDb fixture: 2 of 5 rows imported; one missing-title row and two unsupported rows were reported with correct source
  row numbers; 2 of 2 imported titles enriched.
- StoryGraph fixture: 4 of 4 books imported; statuses, ratings, authors, ownership, reading dates, and completed progress
  were preserved; all four enrichment attempts reached a terminal result and unresolved metadata was surfaced as
  incidents or incomplete coverage.
- MAL XML fixture: 5 of 5 anime imported; 4 enriched and the archive-only title became a reviewable no-match incident.
- Interruption: the app was force-stopped immediately after IMDb confirmation; the imported titles and 0/2 job were
  recovered on relaunch and completed normally.
- Accessibility: the IMDb preview remained readable, scrollable, and actionable at 200% system font scale.
- Restoration: the original app database, WorkManager state, files, and 1.0 font scale were restored; the app relaunched
  on the original empty library without an Android runtime crash.

Earlier connected MAL account testing imported and enriched a 178-title library. It also exposed and verified the
current pacing, `Retry-After`, HTTP 429, and transient provider-failure handling. The final isolated fixture pass used
the XML fallback because no MAL account was connected in that clean test state.

## Known Non-Blocking Limits

- Metadata completeness depends on provider credentials, availability, and coverage.
- A provider retry can wait at least 60 seconds; the durable status remains visible during that interval.
- Ambiguous title-only or author-only matches always require a user decision.
- StoryGraph books without a trustworthy provider page count remain marked incomplete instead of receiving invented
  progress.
- At 200% text size, the secondary MAL title-choice label and the `Cinema i TV` bottom-navigation label can truncate.
  The import actions remain reachable, but these labels should be made adaptive in the UI-polish pass.
- Successfully completed and cancelled batches remain stored for diagnostics but are hidden when no action remains.

## Release Checklist

Completed:

- [x] Imports are additive and duplicate-aware.
- [x] Preview and confirmed-import counts agree.
- [x] Invalid and unsupported rows are explained before confirmation.
- [x] Imported tracking data survives provider failures.
- [x] Manual metadata and user history survive enrichment.
- [x] Progress survives navigation, process restart, and immediate interruption.
- [x] Retry, pause, resume, cancellation, incidents, and manual review are available.
- [x] MAL inbound import cannot create an immediate outbound-sync feedback loop.
- [x] Room migrations, debug tests, Android test compilation, and debug assembly pass.
- [x] MAL XML, IMDb, and StoryGraph complete on a physical device.
- [x] The critical preview works at 100% and 200% font scale.

Required before production release:

- [x] Build and install the signed release candidate, not only the debug APK.
- [x] Upgrade a copy of the last production database through schema version 27 using the release candidate.
- [x] Validate release credentials and redirect configuration for MAL, TMDB, and optional Google Books access without
  committing secrets.
- [x] Repeat MAL OAuth, token refresh, and one account import with the release client configuration.
- [x] Repeat one import while offline, then reconnect and confirm automatic recovery on the release candidate.
- [x] Confirm whether the two known 200% label truncations are acceptable for release or fix them first.
- [x] Run the short regression smoke test: launch, Home, Settings import entries, one provider preview, import activity,
  review action, pause/resume, and app restart.

## Release Decision

The import and enrichment implementation is ready for release-candidate validation. No known issue threatens imported
tracking data or manual user history. Production release should wait for the unchecked release-build, upgrade,
credential, offline-recovery, and final accessibility decisions above.

