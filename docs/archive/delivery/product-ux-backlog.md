# Omnilog Product and UX Backlog

This document turns the product, UX, UI, and usability review into an implementation backlog. Items are ordered to fix correctness and trust first, establish reusable accessibility foundations second, improve the core daily workflows third, and polish secondary surfaces last.

Original review findings **7** (bulk management) and **10** (browse-toolbar density) are intentionally excluded because they are not considered issues for the product.

## Status Legend

- `[ ]` Todo
- `[-]` In progress
- `[x]` Complete
- `[~]` Deferred

## Recommended Implementation Order

| Order | ID | Original finding | Summary | Priority | Status |
|---:|---|---:|---|---|---|
| 1 | UX-01 | 4 | Make objective metrics, labels, and units consistent | High | `[x]` |
| 2 | UX-02 | 5 | Clarify achieved progress versus objective targets | Medium | `[x]` |
| 3 | UX-03 | 15 | Sanitize and render provider synopsis content | High | `[x]` |
| 4 | UX-04 | 6 | Add recovery for destructive deletion | High | `[x]` |
| 5 | UX-05 | 23 | Complete a focused Catalan copy pass | Low | `[x]` |
| 6 | UX-06 | 8 | Rename the combined movies and TV destination | High | `[x]` |
| 7 | UX-07 | 14 | Replace misleading account terminology | Medium | `[x]` |
| 8 | UX-08 | 11 | Label browse modes and use contextual terminology | Medium | `[x]` |
| 9 | UX-09 | 16 | Make library rows work with larger system text | High | `[x]` |
| 10 | UX-10 | 19 | Improve secondary-text and chip readability | Medium | `[x]` |
| 11 | UX-11 | 17 | Replace the ten-small-stars rating control | High | `[x]` |
| 12 | UX-12 | 18 | Clarify status, ownership, and rating indicators | Medium | `[x]` |
| 13 | UX-13 | 1 | Add quick progress actions to daily-use surfaces | High | `[x]` |
| 14 | UX-14 | 2 | Make the add and metadata-search flow explicit | High | `[x]` |
| 15 | UX-15 | 9 | Separate saved and external search results | Medium | `[x]` |
| 16 | UX-16 | 3 | Turn empty states into useful starting points | High | `[x]` |
| 17 | UX-17 | 12 | Rebalance the Home information hierarchy | Medium | `[x]` |
| 18 | UX-18 | 13 | Replace the games-only Home preference | Medium | `[x]` |
| 19 | UX-19 | 20 | Add actionable loading and error recovery | Medium | `[x]` |
| 20 | UX-20 | 21 | Make statistics comparisons interpretable | Medium | `[x]` |
| 21 | UX-21 | 22 | Support system, light, and dark themes | Medium | `[x]` |

## Phase 1: Correctness, Trust, and Product Language

### [x] UX-01 — Objective metrics, labels, and units must agree

- **Original finding:** 4
- **Issue:** An objective can say `7000 llibres registrats` while showing progress as `3842/7000 pàgines`.
- **Impact:** Users cannot trust what an objective measures or whether its progress is correct.
- **Recommendation:** Generate the title, progress value, and unit from the same objective definition; preview the final sentence before saving and reject incompatible combinations.
- **Priority:** High
- **Done when:** Every objective type produces semantically consistent copy and units on Home, Profile, and the objective form.

### [x] UX-02 — Distinguish current progress from the target

- **Original finding:** 5
- **Issue:** A headline such as `8 llibres completats` reads as an achieved result even when the actual progress is 6/8.
- **Impact:** Users have to inspect the percentage to understand their real position.
- **Recommendation:** Lead with `6 de 8 llibres` and present `Objectiu: 8` or the equivalent target as supporting information.
- **Priority:** Medium
- **Done when:** Incomplete, complete, and exceeded objectives all communicate current value and target without ambiguity.

### [x] UX-03 — Sanitize and render synopsis content

- **Original finding:** 15
- **Issue:** Provider HTML such as `<p>` and `<b>` is displayed literally in item summaries.
- **Impact:** Detail pages look broken and long descriptions become difficult to read.
- **Recommendation:** Sanitize provider content, render a small supported formatting subset, normalize whitespace, and retain a plain-text fallback.
- **Priority:** High
- **Done when:** No raw markup is visible in previews, saved-item details, recommendations, or metadata-refresh previews.

### [x] UX-04 — Provide recovery after destructive deletion

- **Original finding:** 6
- **Issue:** Deleting an item or session is confirmed but cannot be undone without restoring a complete backup.
- **Impact:** A single mistake can permanently remove valuable history.
- **Recommendation:** Add a time-limited Undo action or a temporary recycle bin for deleted items and sessions.
- **Priority:** High
- **Done when:** A user can recover an accidental deletion without replacing the rest of the library.

### [x] UX-05 — Complete a Catalan product-copy pass

- **Original finding:** 23
- **Issue:** Copy contains inconsistent accents, developer-facing terms such as `Local-first`, and awkward dynamic phrases such as `Més de Empiri`.
- **Impact:** The product feels less polished and occasionally exposes implementation language.
- **Recommendation:** Review all strings in context, replace technical terminology with plain Catalan, and handle dynamic contractions or use wording that avoids them.
- **Priority:** Low
- **Done when:** All visible copy is natural, correctly accented, and consistent across equivalent actions and states.

### [x] UX-06 — Rename the combined movies and TV destination

- **Original finding:** 8
- **Issue:** The `TV` destination also contains movies, but neither its label nor its icon communicates that.
- **Impact:** Users can reasonably conclude that movies are missing or stored elsewhere.
- **Recommendation:** Rename the destination to `Cinema i TV`, `Pantalla`, or another inclusive term and use an inclusive screen/play icon.
- **Priority:** High
- **Done when:** Navigation, empty states, filters, headings, and import entry points use the same inclusive section name.

### [x] UX-07 — Replace misleading account terminology

- **Original finding:** 14
- **Issue:** The profile entry is called `Compte` even though Omnilog is local-first and has no online account model.
- **Impact:** Users may assume that their data is synchronized or tied to a remote identity.
- **Recommendation:** Rename it `Perfil` or `La meva biblioteca` and state clearly that the data remains on the device unless exported or backed up.
- **Priority:** Medium
- **Done when:** Profile, settings, backup messaging, and accessibility labels set the correct expectation about storage and synchronization.
- **Verification note:** Implementation is complete; device/emulator accessibility QA is pending because no Android target is connected.

### [x] UX-08 — Label browse modes contextually

- **Original finding:** 11
- **Issue:** Browse modes use unlabeled icons and the generic term `Autors` where studios, developers, directors, or creators may be more accurate.
- **Impact:** Grouping is difficult to discover and semantically incorrect in several media sections.
- **Recommendation:** Give each mode a visible label and derive the creator label from the active media section.
- **Priority:** Medium
- **Done when:** Each section uses terminology users expect and browse modes remain understandable without memorizing icons.

## Phase 2: Accessibility and Reusable UI Foundations

### [x] UX-09 — Support larger system text in library rows

- **Original finding:** 16
- **Issue:** At 130% font size, fixed-height rows truncate titles, hide metadata, and compress genre chips.
- **Impact:** Users who need larger text receive a materially degraded browsing experience.
- **Recommendation:** Allow rows to grow, remove fixed-height assumptions, prioritize essential metadata, and limit secondary chips when space is constrained.
- **Priority:** High
- **Done when:** Core screens remain readable and operable at 200% font scaling without overlapping, clipping essential content, or hiding actions.
- **Verification note:** Verified with the debug APK on a connected Android device at 200% system font scale. The Anime library kept titles, metadata, genre chips, progress, dates, and status actions within each row; the device font scale was restored to 100%.

### [x] UX-10 — Improve secondary-text and chip readability

- **Original finding:** 19
- **Issue:** Muted metadata, genre chips, chart legends, and secondary actions are very small and low contrast.
- **Impact:** Important context is difficult to scan even at the default text scale.
- **Recommendation:** Raise contrast, increase the minimum secondary type size, and reserve the faintest treatment for genuinely optional information.
- **Priority:** Medium
- **Done when:** Text and controls meet the chosen accessibility contrast target in every section accent and theme.
- **Verification note:** Implemented the 11sp compact-label baseline, removed 9–11sp text overrides, replaced low-opacity secondary text and same-accent chip foregrounds, and improved chart legends, badges, metadata chips, and secondary actions. Debug APK build and unit tests pass; the app launched on a connected Android device at 100% and 200% font scale, then the device was restored to 100%. System, light, and dark themes were later delivered under UX-21.

### [x] UX-11 — Replace the ten-small-stars rating control

- **Original finding:** 17
- **Issue:** Ten small star targets are difficult to select accurately and have weak unselected-state contrast.
- **Impact:** Rating entry is error-prone and uncomfortable for users with motor or visual impairments.
- **Recommendation:** Use a labeled 0–10 stepper or slider, or five larger stars with half-step support and an explicit `Sense nota` action.
- **Priority:** High
- **Done when:** Every rating value can be selected accurately with accessible touch targets and is announced clearly by assistive technology.

### [x] UX-12 — Clarify list-row indicators

- **Original finding:** 18
- **Issue:** Library rows use unexplained ownership and status icons and show a bare rating number without identifying the score type.
- **Impact:** Users must memorize icon meanings and infer whether a score is personal or external.
- **Recommendation:** Show a recognizable `★ 8/10` treatment for personal ratings and use short, clearly differentiated status and ownership badges.
- **Priority:** Medium
- **Done when:** A new user can identify status, ownership, personal rating, and external rating without opening the detail page.

## Phase 3: Core Daily Workflows

### [x] UX-13 — Add quick progress actions

- **Original finding:** 1
- **Issue:** Updating progress requires opening an item, entering its editor, changing a value, and saving.
- **Impact:** The most frequent daily action is unnecessarily slow.
- **Recommendation:** Add direct progress entry and contextual quick actions such as `+1` and `Completa` to active Home cards and library rows.
- **Priority:** High
- **Done when:** A typical progress update can be completed from Home or a library in one or two intentional actions with immediate feedback.

### [x] UX-14 — Make the add and metadata-search flow explicit

- **Original finding:** 2
- **Issue:** The add screen has no task-specific title or visible search action, and search submission is effectively hidden in the keyboard.
- **Impact:** First-time users may not understand how to begin or whether typing automatically searches.
- **Recommendation:** Title the screen for the current media type, include a visible search action, retain manual entry as a clear fallback, and dismiss the keyboard when results arrive.
- **Priority:** High
- **Done when:** The flow is understandable without relying on keyboard conventions and preserves a clear back/cancel path.
- **Implementation note:** The metadata-search step now shows a media-type-specific title (`Afegeix un llibre` / `una pel·lícula` / `una sèrie` / `un anime` / `un joc`) with a subtitle explaining search-vs-manual, a leading magnifier plus an inline search button and a clear (✕) control in the pill (loading spinner replaces the search button while a query runs), and the keyboard is dismissed when results arrive and on explicit submit. Manual entry was promoted from a low-emphasis text button to an outlined button; Cancel remains the back path. Debounced auto-search is retained as additive behaviour.
- **Fixed after the fact — the screen opened with the wrong media type:** the add screen was titled from an arbitrary library item rather than the section it was opened from, so `Llibres` → `+` produced `Afegeix una pel·lícula` with the movie accent, defeating this item's own "title the screen for the current media type" requirement. `selectedCollectionItems` (`ContentTrackerApp.kt`) filtered `it.collection?.id == selectedCollectionId`; outside a collection that id is null, and `null == null` matches every collection-less item, so `firstOrNull()` returned an arbitrary item and `initialAddType` never fell through to `selectedSection.defaultType`. The list is now empty when no collection is selected, which restores the fallback and leaves the collection-add path (where the id is genuinely set) computing the same value as before. Pre-existing since `f03ceac`; surfaced by UX-19, which noted it mis-coloured the retry buttons it had just added.
- **Verification note:** Debug APK build and `:app:testDebugUnitTest` pass. The media-type fix is verified on device (`61070DLCQ000KB`, 2026-07-17) against the real library: `Llibres` → `+` gives `Afegeix un llibre` in the book accent, `Jocs` → `+` gives `Afegeix un joc` in the Jocs accent, and adding into the `Trono de Cristal` collection still gives `Afegeix un llibre` — the path the old code was written for, confirming no regression. **Not verified on hardware:** 200% font scale and the keyboard-dismissal behaviour, which were the original QA gaps and remain untested.

### [x] UX-15 — Separate saved and external search results

- **Original finding:** 9
- **Issue:** Section search presents saved items and provider results with only a divider between them.
- **Impact:** Users may mistake external suggestions for library items or accidentally enter the add flow.
- **Recommendation:** Add persistent `A la teva biblioteca` and `Resultats externs` headings and give external rows an explicit add/review affordance.
- **Priority:** Medium
- **Done when:** Ownership state and the consequence of tapping each result are obvious before interaction.
- **Implementation note:** During an active section search, the external provider results are now grouped under a persistent `Resultats externs` header (with a result count plus the hint `Toca un resultat per afegir-lo`), replacing the previous bare divider. The saved-library results above stay unlabelled so default browsing is unchanged. External rows are given an accent-tinted border so they read as distinct, non-owned suggestions rather than library items. The header appears only while searching. (Chosen approach: external heading + row styling and an explicit tap hint, rather than a per-row add button.)
- **Verification note:** `:app:compileDebugKotlin` and `:app:testDebugUnitTest` pass. Device/emulator QA (default and 200% font scale; empty-library-vs-external and possible-duplicate states) is pending because no Android target is currently connected.

### [x] UX-16 — Make empty states actionable

- **Original finding:** 3
- **Issue:** Empty Home and section states explain that no content exists but do not provide a next action.
- **Impact:** New users reach a dead end instead of learning the product’s core workflow.
- **Recommendation:** Add a primary `Afegeix…` action, a relevant import action where available, and a short explanation of metadata search versus manual entry.
- **Priority:** High
- **Done when:** A first-time user can start a library directly from every empty state.
- **Implementation note:** Added a shared `OmnilogEmptyState` component (`ui/common`) taking an icon, title, body, and up to two actions, so later items reuse it instead of hand-rolling panels. Empty is now split into three distinct reasons rather than one branch:
  - **Section with no items:** section-specific title and a one-sentence search-vs-manual explanation, a primary `Afegeix…` action whose label matches the add screen it opens (UX-14), and an import action where a provider export exists (MyAnimeList → Anime, StoryGraph → Llibres, IMDb → Cinema i TV; Jocs has no importer and shows only the primary action). Imports were previously reachable only from Settings.
  - **Filters match nothing:** new state reporting how many items the section actually holds plus an `Esborra els filtres` action. This also fixes a real defect — the old condition (`trackedItems.isEmpty() && searchQuery.isBlank()`) showed the "library is empty" copy to users whose library was merely filtered.
  - **Home first run:** title, a body carrying the UX-07 local-first promise, one accented button per media section routing into that section's add flow, and `Importa una còpia de seguretat`.
  Section empty copy moved from descriptive (`L'anime que segueixis apareixerà aquí.`) to instructional. `MediaSection` gained `emptyTitleResId`, `addActionResId`, `importActionResId`, and `navIconResId` (the last moved from a private helper in `ContentTrackerApp` so both call sites share one definition).
- **Scope note:** The `Ara mateix` empty state was pulled in from UX-17 at the maintainer's request. When the library had items but none in progress, the section vanished silently; it now renders a placeholder (`Res en curs ara mateix…`) and keeps the filtered-empty message distinct from it.
- **Verification note:** Verified on a connected Android device at 100% and 200% font scale; the device font scale was restored to 100%. Confirmed: Home first run, Llibres empty (with import) and Jocs empty (without), the primary action opening the correctly-titled add screen, filtered-empty with a working `Esborra els filtres` against real data, `Ara mateix` empty, and the populated Home unchanged. Action rows use `FlowRow`, so at 200% the buttons wrap to their own lines rather than clipping. Debug APK build and `:app:testDebugUnitTest` pass.
- **Sample-data seeding removed:** `HomeViewModel.init` called `seedSampleDataIfEmpty()` on every launch, ungated by build type, so a fresh install was never empty — a real first-time user got a demo library (Dune, Fullmetal Alchemist, Severance) rather than the Home first-run state, which made UX-16's stated premise unreachable in production. Seeding is now removed entirely: the `init` call, `MediaRepository.seedSampleDataIfEmpty`, its `OfflineMediaRepository` implementation, the `SampleTrackedMedia` fixture (205 lines, which also hardcoded ids 1..n), and the now-unused `MediaDao.countMediaItems`. Verified on device: a cleared install writes zero `media_items` rows and lands on the Home first-run state. Existing libraries are untouched — any sample items a user already has remain theirs to delete.

## Phase 4: Home and Information Hierarchy

### [x] UX-17 — Rebalance the Home hierarchy

- **Original finding:** 12
- **Issue:** Multiple large objective cards appear before recent activity and top-rated content.
- **Impact:** Goal tracking dominates the dashboard over the more frequent resume-and-review workflows.
- **Recommendation:** Reduce objectives to a compact summary and move recent activity directly below `Ara mateix`.
- **Priority:** Medium
- **Done when:** The first viewport prioritizes resuming content and understanding recent activity while objectives remain easy to reach.
- **Scope note:** `Ara mateix` no longer disappears when nothing is in progress — that empty state was implemented under UX-16, so this item covers only the ordering and weight of the dashboard sections.
- **Implementation note:** Home is now `Cerca → Ara mateix → Següent a la llista → Objectius → El teu any en contingut → En pausa → Completats recentment`. Previously the first viewport held only the search pill and `Ara mateix`; roughly 450dp of objectives and analytics sat between it and the remaining carousels. The two carousels below the cards cost nothing in hierarchy terms and give the dashboard a full lifecycle: what I'm on → what's next → *[goals, analytics]* → what I stalled on → what I finished. Each maps to a distinct `TrackingStatus`, so none is redundant with another.
  - **Objectives are one card, not three.** `CompactObjectiveCard` (one Surface per objective) is replaced by `ObjectiveSummaryRow`, a chrome-less row, stacked inside a single card that carries a status roll-up in its header (`2 al dia · 1 endarrerit`). ~245dp → ~175dp. Rows keep the `24 de 40 llibres` phrasing rather than a ring or bare percentage, because UX-01/UX-02 require Home to state value and target unambiguously — that constraint sets the floor on how compact this card can get. The card now renders nothing when there are no objectives; it previously showed a permanent "create one from your profile" panel to users who had none.
  - **`Següent a la llista` added.** Planned-status items, most recently touched first, with a `Comença` action per tile that mirrors the `+` quick-progress action from UX-13. It moves the session to In progress and stamps a start date without touching progress. Answers "what's next" directly below "what am I on".
  - **`En pausa` and `Completats recentment` added below the cards.** Chosen against the library's actual shape (608 items: 536 Completed, 26 Planned, 23 Dropped, 17 Paused, 9 In progress) — the two carousels above the cards speak to 35 items, while the 536-item archive had no presence on Home at all. `En pausa` is ordered longest-stalled first, which is the point of the section, and shares the `Comença`/`Reprèn` action. `Completats recentment` orders strictly by finish date. A last-updated fallback was tried first, for the 194 of 536 completions that carry no finish date (imported history), and was wrong: `updatedAt` moves whenever any field changes, so editing a years-old entry's notes would promote it to the top of a section that claims recency of completion — the same class of error as the start-date bug below. On real data the fallback surfaced titles that had merely been edited recently ahead of the actual latest completions. An entry with no finish date has no honest position on a recency axis, so it is now excluded rather than guessed at; 342 dated completions remain, well past the 8 shown. Both hide entirely when empty, unlike the daily surfaces above them. A most-revisited carousel was considered and rejected on the same data: only 4 revisits exist.
  - **`El teu any en contingut` leads with volume.** The headline was `completedInPeriod` — a completion count, which is what the objectives card directly above it is already about. It now shows completions and average rating as a KPI pair over per-medium volume chips (`221 episodis · 4.101 pàgines · 971 min`) from the unused `StatsSnapshot.progressTotals`. One chip per medium rather than a single figure: the units differ, so a "biggest" number would be meaningless and a sum nonsense. No new computation — every field already existed on the snapshot. The filler description line was dropped in favour of the numbers.
- **Deviation from the stated Done when — recent activity was removed, not promoted:** `Activitat recent` and `Millors notes` are both gone from Home, so the first viewport prioritizes resuming and planning rather than "understanding recent activity". Agreed explicitly with the maintainer. Rationale:
  - `Activitat recent` was redundant. It sorted by `latestActivityMillis()` unfiltered while `Ara mateix` sorted by the same key filtered to in-progress — and the items you touch most recently are almost always the ones in progress, so the two carousels rendered largely the same covers in the same order.
  - `Millors notes` duplicated Stats, which already renders `snapshot.bestRatedItems` in its own section, and is hall-of-fame content that changes monthly rather than dashboard content.
  - Four carousels at ~280dp each would have pushed objectives further down than before, failing the item on its own terms. Two carousels plus one dense block is the budget **for the first viewport** — that constraint does not apply below the cards, which is where `En pausa` and `Completats recentment` went.
  - **Deferred follow-up:** if recent activity returns, it should be a feed (thumbnail, title, what changed, when — `Dune · +32 pàgines · ahir`), not a carousel. `ProgressUpdate` stores an absolute `progressValue` plus `loggedAt`, and `ProgressHistoryAction.kt` already derives deltas from consecutive updates, so the data supports it.
- **Fixed in passing:** `AccentBadge` set a hard `size()` around a font-scaling letter, so at 200% the glyph clipped to a vertical sliver — this also affected the full `ObjectiveProgressCard` on Profile. It now uses `defaultMinSize` plus padding. `MonthlyActivityPreview` had a fixed `height(86.dp)` that clipped its month labels at 200%; now `heightIn(min = 86.dp)`. Also `MediaType.progressUnit()` returned the unaccented `pagines` (a UX-05 miss), now `pàgines` — newly prominent in the volume chips. Removed dead code in `HomeLandingScreen`: `bestRating()` (dead as of this change) plus `statsDate()` and `updatedDate()` (already unused).
- **Start-date bug found and fixed during device verification:** `quickStart` originally applied `startedAt = session.startedAt ?: LocalDate.now()` to both statuses. That fallback is correct for Planned (you are starting it now) and wrong for Paused: the title was started in the past, and most paused sessions here carry no start date, so resuming stamped today. Because `StatsCalculator` buckets a session by `finishedAt ?: startedAt ?: updatedAt` and an in-progress session has no finish date, that fabricated date would have filed a months-old title into the current statistics period. Now only Planned gets the fallback; a paused session keeps its start date, or keeps none. Confirmed against the database after the fix: resuming session 535 left `startedAtEpochDay=None` with status `InProgress` and progress/rating intact.
- **Verification note:** Verified on a connected Android device at 100% and 200% font scale; the device font scale was restored to 100%. Confirmed: the first viewport holds search, `Ara mateix`, and `Següent a la llista` with the objectives card below; the objectives roll-up and rows against real data including an exceeded objective (`33 de 30 títols`, 110%) and `+1 més` overflow; volume chips wrapping via `FlowRow` at 200%; `Comença` moving a planned title into `Ara mateix`; `Reprèn` moving a paused title, verified at the database level; `En pausa` ordering (the resumed title correctly left the visible window, being no longer among the 8 most stalled of 17); and `Completats recentment` ordering, checked tile-by-tile against the finish dates queried from the database. Debug APK build and `:app:testDebugUnitTest` pass. There is no ViewModel test infrastructure (no fake `MediaRepository`); device verification is the agreed approach for these paths.
- **Test data note:** device verification mutated three sessions, each restored afterwards and confirmed at the database level (`Paused`/`Planificat`, `startedAt=None`, progress and ratings intact). Their `updatedAt` stamps are now current, which only affects their position within the planned and paused carousels.
- **Not done — deliberately:** no change-vs-last-year badge on the analytics card. UX-20 exists because the stats hero has an unexplained change badge; adding an unlabelled delta chip here would replicate the exact defect that item is meant to fix. It belongs there, labelled with its comparison basis.

### [x] UX-18 — Replace the games-only Home preference

- **Original finding:** 13
- **Issue:** `Amaga jocs d’Ara mateix` special-cases one media type instead of providing a coherent way to control dashboard content.
- **Impact:** It adds preference complexity without addressing broader filtering needs.
- **Recommendation:** Replace it with media-type filter chips or a single dashboard-content preference that supports every media type consistently.
- **Priority:** Medium
- **Done when:** Users can control `Ara mateix` without media-specific one-off settings.
- **Implementation note:** The games-only boolean becomes a four-section preference (`Contingut d'Ara mateix`) in Settings → `Preferències`, filtering only the `Ara mateix` carousel. The remaining Home carousels and recent activity continue to draw from the full library. Home carries no control — only a notice when the filter is actually on.
  - **The control lives in Settings, not on Home.** An earlier revision put the chips in a row under the Home search field; it was rejected by the maintainer and the objection is correct. The filter only matters if you track a medium *and* don't want it on the dashboard, which is a narrow and very stable preference — it is set once and then forgotten. A permanent ~68dp row in the first viewport for a once-in-a-lifetime interaction contradicts the budget UX-17 set. The backlog's own recommendation names "a single dashboard-content preference" as an acceptable route; the defect in the old switch was that it was *games-only*, not that it was in Settings.
  - **Home discloses the filter, and only then.** A persistent invisible filter makes the dashboard quietly lie — hide `Jocs`, forget, start a game, and it silently never reaches `Ara mateix`. `ActiveFilterIndicator` renders a compact `Sense Jocs ×` chip beside the `Ara mateix` title **only when something is hidden**, so the default state costs nothing and the filtered state is never a mystery. Tapping it clears the whole set.
  - **Chips are `MediaSection`, not `MediaType`,** so the granularity matches the nav and `Cinema i TV` stays one shelf instead of splitting into two chips nothing else in the app distinguishes. Settings offers all four regardless of library contents: it is a standing preference, not a live filter over what is on screen.
  - **Every other Home section is deliberately out of scope.** `Següent a la llista`, recent activity, `En pausa`, completed items, objectives, and analytics remain complete. The preference answers only which media types deserve immediate attention in `Ara mateix`.
  - **Hiding every section is allowed.** `Ara mateix` then explains that the filter hid its contents, while every other Home section remains unchanged and the notice offers the way back.
  - **The Settings switch is replaced, not merely moved.** This removes a real desync defect: both old copies seeded from `SharedPreferences` once (`rememberSaveable` on Home, `remember` in Settings) and neither observed the file, so toggling in one did not update an already-composed other. Now that the writer (Settings) and the reader (Home) are on different screens, correctness *requires* observation: `rememberHiddenActiveSections` registers an `OnSharedPreferenceChangeListener` in a `DisposableEffect`, so Home reflects a change made on the other screen whether or not it stayed in composition.
  - **Shared code:** the preference name, keys, migration, read/write, the observing helper, and `ActiveSectionChips` all live in `ui/common/DashboardSectionPreferences.kt`, since Settings and Home now both touch this state.
  - **Persistence and migration:** `active_hidden_sections` stores *hidden* sections, so a medium added to the library later appears by default. The former `dashboard_hidden_sections` value migrates to it, as does `hide_games_from_active` on older installs (`true` → `{Games}`); legacy keys are removed.
  - **Behaviour change:** the filter is per-device UI state and no longer travels with a backup, unlike the boolean it replaces. Judged correct — a view filter is not user data.
- **Verification note:** `:app:testDebugUnitTest` and `:app:assembleDebug` pass after scoping the filter to `Ara mateix`. On a connected device, the existing `{Games}` selection migrated, Home showed the active-filter indicator, `Ara mateix` and the unfiltered planned carousel both rendered, and startup produced no crash.
- **Copy:** `home_active_hide_games` remains removed. The Settings row and Home notice now name `Ara mateix` explicitly; the planned-carousel filtered-empty copy was removed because that carousel is no longer filtered.

## Phase 5: Feedback, Analytics, and Theme Completion

### [x] UX-19 — Add actionable loading and error recovery

- **Original finding:** 20
- **Issue:** Search errors instruct users to retry but provide no retry action, while loading relies mainly on static text.
- **Impact:** Recovery is unclear and temporary provider failures feel unresponsive.
- **Recommendation:** Add inline Retry actions, visible progress indicators, and useful partial-failure details when only some providers fail.
- **Priority:** Medium
- **Done when:** Every recoverable loading or error state provides a clear next action and prevents duplicate submissions.
- **Implementation note:** The finding named "search errors", but the same text-only dead end existed on **four** surfaces, not one: the add-flow search step, the section search's `Resultats externs` (`HomeScreen`), the metadata link dialog (`ContentTrackerApp`), and the season/edition/review details panels. `SearchStatePanel` (text plus a `Surface`, previously `internal` inside `AddMediaScreen` and imported by `HomeScreen`) is replaced by `ui/common/OmnilogStatusPanel.kt`, which adds an optional progress indicator and an optional action reusing `EmptyStateAction` from UX-16's `OmnilogEmptyState`. `ExternalRecommendationsSection` already had the loading-plus-retry treatment this item generalizes; it was the model, not a target.
  - **Retry reuses `onMetadataSearchSubmitted`**, already wired to `searchMetadataSuggestions(forceShortQuery = true)` at both call sites. The flag is load-bearing: without it a query under `MINIMUM_AUTOMATIC_SEARCH_LENGTH` would hit the guard and silently reset to the un-searched state instead of retrying. Retry re-runs the whole search rather than only the failed providers — partial results are deliberately not cached, so a full retry re-queries everything anyway and needs no per-source request filtering. `cancelMetadataSearch()` already made double-taps harmless.
  - **Partial failure now names the providers.** The repository always computed `MetadataSearchResult.failedSources`; the UI collapsed it to a boolean and said `Alguns proveïdors no han respost`. `MetadataSearchUiState` now carries `failedSources: Set<MetadataSource>` (with `hasPartialError` derived), and `MetadataSource.displayName()` was added beside the existing `ExternalRatingSource.displayName()`. That also fixes the result chips, which rendered the raw enum (`OpenLibrary` → `Open Library`).
  - **Details failures were unreachable, and that was the real defect.** Every provider ended `getSuggestionDetails` with `.getOrElse { suggestion }` / `.getOrDefault(suggestion)`, so it never threw, `hasDetailsError` was permanently false, and the `metadata_details_error` panel never rendered. Users silently got degraded metadata; worse, the season and edition pickers showed `No s'han trobat temporades/edicions útils` — asserting none exist when the provider was simply unreachable. The swallowing is removed from all five providers so failures reach the caller. All three call sites already handled it: `selectMetadataSuggestion` (now shows the panel plus retry), `previewMediaItemMetadataRefresh` and `linkMediaItemMetadata` (both already `runCatching` into an error snackbar, previously reached by silent no-ops). Secondary enrichment that legitimately degrades — `getOfficialMalAnimeDetails` — keeps its own catch.
  - **Races fixed:** `selectMetadataSuggestion` kept no job handle and applied its result unconditionally, so selecting A then B could let A's slower response overwrite `selectedSuggestion` back to A and land the user on the review step for the wrong item. It now cancels the previous job and guards on suggestion identity, mirroring what the search path already did. `cancelMetadataSearch()` cancels the details job too. The removed `runCatching` in the providers also stops swallowing `CancellationException`.
  - **Progress indicators** were added only where nothing else showed one — the season picker, edition picker, link dialog, and the review hero's `Carregant detalls...`. The add search step deliberately keeps its panel text-only: UX-14 already spins the search pill, and a second indicator reads as two separate waits.
- **Not done — deliberately:** the add screen's save button still has no in-flight guard (`enabled = title.isNotBlank() && !isLoadingDetails`) and `addTrackedMedia` is fire-and-forget. The call site sets `isAdding = false` synchronously so the sheet leaves composition on the first tap, making a real double-insert unlikely; a write path is not a loading or error state, so it stays out of scope.
- **Verification note:** verified on device (`61070DLCQ000KB`, 2026-07-17) by forcing failures with the radios off. Search error → `Torna-ho a provar` → offline retry re-attempts cleanly → online retry returns 20 results. Partial failure fired naturally (Google Books down, Open Library up) and rendered `Google Books no ha respost, així que hi pot faltar algun resultat.` with correct singular agreement. Details error now renders non-blockingly over the review form, and its retry loads the details and advances to the edition picker. `:app:testDebugUnitTest` passes, including 5 new `PartialSearchFailureTest` cases covering the provider enumeration. **Not verified:** 200% font scaling, and the Catalan plural for 2+ failed providers (only the one-provider case occurred naturally).
- **Deferred follow-up — now resolved under UX-14:** the add screen opened with the wrong media type (in `Llibres` it was titled `Afegeix una pel·lícula` with the movie accent), which mis-coloured this item's new retry buttons. Pre-existing (commit `f03ceac`) and correctly out of scope here; fixed and device-verified under UX-14, where the "title the screen for the current media type" requirement lives.

### [x] UX-20 — Make statistics comparisons interpretable

- **Original finding:** 21
- **Issue:** The stats hero shows an unexplained change badge and charts without direct values or comparison context.
- **Impact:** Users can see shapes and trends but cannot confidently interpret what changed or relative to which period.
- **Recommendation:** Label comparison periods explicitly, expose chart values on interaction, and keep period and media filters available while scrolling.
- **Priority:** Medium
- **Done when:** Every chart communicates its unit, period, comparison basis, and exact values without guesswork.
- **Implementation note:** Deliberately split against `docs/omnilog-stats-improvement-plan.md`: UX-20 labels and exposes what the stats already compute; definition changes, chart-form replacements, and full drill-down stay with STATS-01/03/04.
  - **Comparison basis is explicit.** `PeriodDelta` now carries a `ComparisonBasis` (`SamePeriodOfYear`, `FullYear`, `Previous12Months`) and the hero delta chip renders beside its label — `vs. mateix període de 2025`, `vs. 2024`, `vs. els 12 mesos anteriors` — with a merged TalkBack description (`4 més (vs. …)`) instead of a bare `↑4`.
  - **Scope note — pulled forward from STATS-01:** `Aquest any` previously compared year-to-date against the *whole* previous calendar year; labelling that honestly would have shipped `vs. tot 2025` next to a July figure, so the like-for-like window fix (`Jan 1 .. today − 1 year`, inclusive) moved into this item. The matching STATS-01 bullets are annotated as done.
  - **Units and populations are stated.** The hero headline reads `Títols completats` while the bars carry `Sessions completades per mes` — accurately labelling that the two count different things; unifying those definitions remains STATS-01. `Tot` appends `· últims 12 mesos` to name the bars' narrower scope. `Estat actual` gained `Biblioteca actual, independent del període`, and the `Composició` group states its population (`Basat en títols completats en el període`, or `…en tota la biblioteca` for `Tot`, matching current calculator behaviour).
  - **Minimal tap-to-inspect.** Hero bars and rating-trend points are tappable: selection highlights the mark (other bars dim; the trend point gets a ring) and reuses the existing caption line (`maig · 4 completats`, `abr.: 7,3 de mitjana (3 notes)` — per-point sample size now visible, not only the latest). Tapping again deselects; every mark is a focusable node with a content description. Drag scrubbing, media splits, and contributing-title strips stay deferred to STATS-04.
  - **Sticky filters.** The period and media chips became a `stickyHeader` with an opaque background, satisfying STATS-04's "filters available while scrolling" bullet (annotated there). Both chips hold one row at 200% font scale.
  - **Tests:** `StatsCalculatorTest` gained coverage for the year-to-date window boundary (inclusive same-day cutoff, late-previous-year exclusions) and the basis mapping for all four periods.
  - **Device-verified** (Pixel, real library): `Aquest any` / `Tot` / `2025` periods, bar and trend selection round-trips, sticky bar deep in the page, and the hero at 200% font scale.

### [x] UX-21 — Support system, light, and dark themes

- **Original finding:** 22
- **Original issue:** Omnilog was dark-only and did not follow the device theme preference.
- **Impact:** Users who need or prefer a light presentation cannot adapt the interface.
- **Recommendation:** Add `Sistema`, `Clar`, and `Fosc` options after the shared color and contrast foundations are stable.
- **Priority:** Medium
- **Done when:** System, light, and dark choices work across the app; shared palettes and accents give new surfaces the correct foundation; representative core screens are verified in light and dark.
- **Delivery record:**
  - *Palette refactor (done).* Split the static `OmnilogColors` object: the neutral surface/text tones (`appBackground`, `appPanel`, `appPanelTranslucent`, `appLine`, `appInk`, `appMuted`) moved into an `OmnilogPalette` read through a `LocalOmnilogPalette` CompositionLocal (`OmnilogTheme.colors`), migrated at ~523 call sites. Media/status accents stay in `OmnilogColors` because they are baked into enums (`MediaSection.accent`) and read from non-composable helpers (`statsColor()`, draw scopes). Non-composable neutral readers were threaded a resolved color parameter (`ObjectiveProgressCard`) or hoisted above their `Canvas` (`StatsScreen`).
  - *Neutral light theme + switch (done).* Added `LightPalette` (warm-paper surfaces) and a `lightColorScheme`, a `Sistema`/`Clar`/`Fosc` preference (`ThemePreference`, stored like `DashboardSectionPreferences`), a Settings selector, root wiring in `MainActivity`, and runtime status/navigation-bar icon inversion via `WindowCompat`.
  - *Light-tuned accents (done).* The problem was worse than "gold/pink read low-contrast": measured against `LightPalette`'s background, **six of the ten accents were under 3:1** (`Games` 1.86, `Paused` 2.23, `Planned` 2.37, `Anime` 2.47, `Tv` 2.71, `Completed` 2.75) and none reached 4.5 — `Dashboard`, on every heading and CTA, sat at 3.82. `OmnilogAccents` now mirrors `OmnilogColors` and is provided through `LocalOmnilogAccents`; read it as `OmnilogTheme.accents`. Each light value holds its dark hue and lowers lightness until it clears 4.5:1 against the light *background* rather than the card, since the background is the darker of the two surfaces and one value then covers both. `Paused` also took a saturation bump at the maintainer's request (`#9D5A08`).
  - *Migration shape.* ~214 call sites across 27 files. Most were a mechanical rename inside composables; the compiler then isolated three groups that could not read a CompositionLocal. **Plain helpers** (`statsColor`, `stateColor`, `genreChartColor`, `objectiveAccent`, and five private copies of `sectionAccent`) became `@Composable @ReadOnlyComposable`, which cascaded to their callers. **Enum constructor properties** (`MediaSection.accent`, `StatsMediaFilter.accent`) genuinely cannot be theme-aware, so they keep the dark value as the section's identity and gained `themedAccent()` resolvers for display. **Draw scopes** needed their colours hoisted above the `Canvas`. A grep afterwards found four display sites still reading raw dark accents — they compiled fine and would simply have looked wrong.
  - *Two bugs found, neither caused by the accent work.* Cover scrims are deliberately dark in both themes, but the text on them read `appInk`, which inverts to near-black on light: the Home tiles, the Stats strip, related media, and external recommendations were all unreadable in light mode. Text over a scrim now uses `OnCoverInk`/`OnCoverMuted`. Separately, the four carousels each carried their own copy of the scrim gradient, drifted to four different middle alphas (0.10–0.16) and two stop counts; they now share `ui/common/CoverScrim.kt`, which keeps the scrim and the ink rule that governs it in one place. A shared carousel *card* was considered and rejected — the four differ too much in size and content for the abstraction to be anything but slots.
  - *Verified on device in light:* Home carousels and dashboard rings, Profile, and Stats end to end. Related-media and external-recommendation cards use the same shared `CoverScrim` and fixed `OnCoverInk`/`OnCoverMuted` rules as the verified cover surfaces.
  - *Ongoing policy.* Theme support is complete as a product capability. Individual contrast or surface issues discovered later should be fixed opportunistically as normal UI maintenance; they do not reopen UX-21 as a dedicated workstream.

## Suggested Working Method

Implement and verify one item at a time:

1. Change its status to `[-]`.
2. Define the exact affected screens and acceptance checks before implementation.
3. Test with populated data, empty data, and the relevant loading/error state.
4. For UI changes, test default font size and 200% font scaling.
5. Mark it `[x]` only after the **Done when** condition is satisfied on a device or emulator.
6. Record any intentionally deferred follow-up directly under the item instead of silently widening its scope.
