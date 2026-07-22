# Add Flow And Book Metadata Remake Plan

## Goal

Make adding content fast for every tracking status, while making book metadata edition-aware so users do not routinely need to correct covers, language, or page count by hand.

This is a focused flow and metadata change. It preserves the current local-first repository, Room database, existing sections, and the clear visual distinction between library and API search results.

## Decisions Already Made

- Library and API search-result sections remain visually distinct; they are not part of this remake.
- A user can add an item as `Planned`, `In progress`, `Completed`, `Paused`, or `Dropped` directly from the add flow.
- `Collection` and `Ownership` are visible for every status, not hidden under advanced fields.
- Books are edition-aware. Page count, language, cover, and publication data must be tied to the chosen edition.
- Different provider records are never automatically merged unless their ISBNs match.
- Notes, platform, and other low-frequency data remain under `More details`.

## Target Add Experience

### 1. Find the item

The user searches from the relevant media section.

- Existing library matches continue to open the saved item.
- Selecting a movie, anime, game, or a specific TV season opens the add sheet.
- Selecting a book work opens an edition picker before the add sheet.
- An ISBN query is recognised as a book-edition lookup and is ranked above title matching.

### 2. Choose a book edition when needed

The book edition picker presents a recommended edition followed by `More editions`.

Each edition row shows:

- cover
- title and author
- language
- format, when known
- publisher and publication year, when known
- page count
- ISBN, when available
- metadata provider

Ordering is: exact ISBN, exact title and author, preferred language, completeness of edition information, then provider relevance. The picker must not claim that one edition is universally correct.

### 3. Choose a status and enter only relevant tracking data

The primary add sheet always contains a status selector, collection selector, and one-tap ownership checkbox. The visible tracking fields change with the selected status.

| Status | Status-specific fields | Always visible |
| --- | --- | --- |
| Planned | None | Status, collection, ownership |
| In progress | Current progress, started date | Status, collection, ownership |
| Completed | Final progress, started date, finished date, rating | Status, collection, ownership |
| Paused | Current progress, started date, finished date, rating | Status, collection, ownership |
| Dropped | Current progress, started date, finished date, rating | Status, collection, ownership |

Dates and rating for paused/dropped items are deliberately blank by default. A collection is preselected when the user begins from a collection page. Ownership defaults to `None`.

The primary action is status-aware: `Add as planned`, `Add as in progress`, and so on. It replaces the misleading `Create session` label.

### 4. Save immediately

Saving creates the item and its initial tracking state without requiring every optional field. The current persistence model can continue to create its initial tracking session internally; the UI wording should describe the user intent as adding an item with a status.

The add sheet includes `More details` for platform, notes, and other optional values. Manual add follows the same status-first sheet, starting with type and title.

### 5. Preserve corrections

After saving, the item detail page shows the selected edition, for example: `English paperback · 412 pages · ISBN …`.

If the user changes a metadata field manually, that field is marked as a local override. Future refreshes can fill missing data, but must not silently replace an overridden value.

## Implementation Phases

### Phase 1 — Search correctness and latency

1. Replace the independent search launches in `HomeViewModel` with a cancellable/latest-only search pipeline.
   - Cancel or ignore earlier requests when the query changes.
   - Bind the loading/result state to the query that produced it.
2. Keep local filtering immediate; debounce remote metadata lookup at about 300 ms for queries of three or more characters.
   - Permit explicit search for short titles such as `It`.
3. Add a small in-memory, time-limited cache for recent metadata queries.
4. Stop fetching TV season summaries for every search result.
   - Fetch season data only after the user chooses a TV series.
5. Surface partial provider failures without discarding successful results from another provider.

**Acceptance:** old results can never replace a newer query, and selecting TV search no longer triggers a details request for every result.

### Phase 2 — Status-first add sheet

1. Replace the current review/manual-screen layout with one shared add configuration component.
2. Implement the status selector and the exact conditional fields in the table above.
3. Keep collection and ownership visible regardless of selected status.
   - Reuse the current collection matching and collection-page prefill behaviour.
4. Move platform, notes, and remaining optional fields into `More details`.
5. Rename the primary action based on the chosen status.
6. Keep duplicate detection before opening the add sheet.

**Acceptance:** a planned item requires only selecting the catalog result and pressing `Add as planned`; each other status exposes precisely its agreed tracking fields.

### Phase 3 — Book work and edition model

1. Add domain models that distinguish a book work from a book edition.
   - Edition identity includes provider ID, work ID when available, ISBN-10/ISBN-13, language, format, publisher, publication year, and page count.
2. Extend Room with a focused migration to persist the selected edition identity on a media item.
3. Change provider integration:
   - OpenLibrary identifies works and supplies edition candidates.
   - Google Books supplies volume/edition candidates.
   - ISBN matches may enrich one candidate; title/author/year similarities are only grouping hints.
4. Replace automatic non-ISBN book merging with an edition-picker result set.
5. Add user preferred-language ordering, initially as a local default with a safe fallback to all languages.

**Acceptance:** two editions with different language, cover, or page count are kept separate; the selected edition is identifiable after saving and during refresh.

### Phase 4 — Metadata ownership and refresh safety

1. Add a small `media_metadata_overrides` table keyed by media item and metadata field.
2. Mark a field overridden only when the user changes it in manual metadata editing.
3. Update refresh and link previews to label fields as:
   - missing and safe to fill
   - provider-managed and changed
   - locally overridden and protected
4. Default refresh selection to safe fills; require an explicit choice to replace a local override.

**Acceptance:** manually chosen pages, language, cover, title, or publication year survive refreshes unless the user explicitly elects to replace them.

### Phase 5 — Verification and polish

1. Add unit tests for latest-only search state, ISBN matching, and no non-ISBN edition merging.
2. Add repository tests for edition persistence, migration, and protected metadata overrides.
3. Add focused UI/state tests for every status field combination and collection preselection.
4. Perform device QA for:
   - each add status
   - adding from a collection
   - title, author, and ISBN book searches
   - alternate-language/alternate-page-count editions
   - metadata refresh after a manual correction
5. Run the debug build and unit-test suite before each phase is considered complete.

## Delivery Order

Implement and review each phase as a separate commit. Phase 1 is independently valuable and low risk. Phase 2 delivers the new add experience without a database migration. Phases 3 and 4 are the necessary correctness work for books and should not be shortcut by further heuristic merging.

## Non-Goals For This Remake

- Redesigning the existing library/API search-result distinction.
- Replacing Room or the local-first architecture.
- Introducing a new UI framework or third-party metadata SDK.
- Automatically deciding which book edition the user owns when the data is ambiguous.
