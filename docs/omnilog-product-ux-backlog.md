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
| 7 | UX-07 | 14 | Replace misleading account terminology | Medium | `[-]` |
| 8 | UX-08 | 11 | Label browse modes and use contextual terminology | Medium | `[x]` |
| 9 | UX-09 | 16 | Make library rows work with larger system text | High | `[x]` |
| 10 | UX-10 | 19 | Improve secondary-text and chip readability | Medium | `[x]` |
| 11 | UX-11 | 17 | Replace the ten-small-stars rating control | High | `[x]` |
| 12 | UX-12 | 18 | Clarify status, ownership, and rating indicators | Medium | `[x]` |
| 13 | UX-13 | 1 | Add quick progress actions to daily-use surfaces | High | `[x]` |
| 14 | UX-14 | 2 | Make the add and metadata-search flow explicit | High | `[-]` |
| 15 | UX-15 | 9 | Separate saved and external search results | Medium | `[ ]` |
| 16 | UX-16 | 3 | Turn empty states into useful starting points | High | `[ ]` |
| 17 | UX-17 | 12 | Rebalance the Home information hierarchy | Medium | `[ ]` |
| 18 | UX-18 | 13 | Replace the games-only Home preference | Medium | `[ ]` |
| 19 | UX-19 | 20 | Add actionable loading and error recovery | Medium | `[ ]` |
| 20 | UX-20 | 21 | Make statistics comparisons interpretable | Medium | `[ ]` |
| 21 | UX-21 | 22 | Support system, light, and dark themes | Medium | `[ ]` |

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
- **Verification note:** Implemented the 11sp compact-label baseline, removed 9–11sp text overrides, replaced low-opacity secondary text and same-accent chip foregrounds, and improved chart legends, badges, metadata chips, and secondary actions. Debug APK build and unit tests pass; the app launched on a connected Android device at 100% and 200% font scale, then the device was restored to 100%. Light theme remains scoped to UX-21 because the app currently has a dark-only theme.

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

### [-] UX-14 — Make the add and metadata-search flow explicit

- **Original finding:** 2
- **Issue:** The add screen has no task-specific title or visible search action, and search submission is effectively hidden in the keyboard.
- **Impact:** First-time users may not understand how to begin or whether typing automatically searches.
- **Recommendation:** Title the screen for the current media type, include a visible search action, retain manual entry as a clear fallback, and dismiss the keyboard when results arrive.
- **Priority:** High
- **Done when:** The flow is understandable without relying on keyboard conventions and preserves a clear back/cancel path.
- **Implementation note:** The metadata-search step now shows a media-type-specific title (`Afegeix un llibre` / `una pel·lícula` / `una sèrie` / `un anime` / `un joc`) with a subtitle explaining search-vs-manual, a leading magnifier plus an inline search button and a clear (✕) control in the pill (loading spinner replaces the search button while a query runs), and the keyboard is dismissed when results arrive and on explicit submit. Manual entry was promoted from a low-emphasis text button to an outlined button; Cancel remains the back path. Debounced auto-search is retained as additive behaviour.
- **Verification note:** Debug APK build and `:app:testDebugUnitTest` pass. Device/emulator QA (default and 200% font scale, keyboard-dismissal behaviour) is pending because no Android target is currently connected.

### [ ] UX-15 — Separate saved and external search results

- **Original finding:** 9
- **Issue:** Section search presents saved items and provider results with only a divider between them.
- **Impact:** Users may mistake external suggestions for library items or accidentally enter the add flow.
- **Recommendation:** Add persistent `A la teva biblioteca` and `Resultats externs` headings and give external rows an explicit add/review affordance.
- **Priority:** Medium
- **Done when:** Ownership state and the consequence of tapping each result are obvious before interaction.

### [ ] UX-16 — Make empty states actionable

- **Original finding:** 3
- **Issue:** Empty Home and section states explain that no content exists but do not provide a next action.
- **Impact:** New users reach a dead end instead of learning the product’s core workflow.
- **Recommendation:** Add a primary `Afegeix…` action, a relevant import action where available, and a short explanation of metadata search versus manual entry.
- **Priority:** High
- **Done when:** A first-time user can start a library directly from every empty state.

## Phase 4: Home and Information Hierarchy

### [ ] UX-17 — Rebalance the Home hierarchy

- **Original finding:** 12
- **Issue:** Multiple large objective cards appear before recent activity and top-rated content.
- **Impact:** Goal tracking dominates the dashboard over the more frequent resume-and-review workflows.
- **Recommendation:** Reduce objectives to a compact summary and move recent activity directly below `Ara mateix`.
- **Priority:** Medium
- **Done when:** The first viewport prioritizes resuming content and understanding recent activity while objectives remain easy to reach.

### [ ] UX-18 — Replace the games-only Home preference

- **Original finding:** 13
- **Issue:** `Amaga jocs d’Ara mateix` special-cases one media type instead of providing a coherent way to control dashboard content.
- **Impact:** It adds preference complexity without addressing broader filtering needs.
- **Recommendation:** Replace it with media-type filter chips or a single dashboard-content preference that supports every media type consistently.
- **Priority:** Medium
- **Done when:** Users can control `Ara mateix` without media-specific one-off settings.

## Phase 5: Feedback, Analytics, and Theme Completion

### [ ] UX-19 — Add actionable loading and error recovery

- **Original finding:** 20
- **Issue:** Search errors instruct users to retry but provide no retry action, while loading relies mainly on static text.
- **Impact:** Recovery is unclear and temporary provider failures feel unresponsive.
- **Recommendation:** Add inline Retry actions, visible progress indicators, and useful partial-failure details when only some providers fail.
- **Priority:** Medium
- **Done when:** Every recoverable loading or error state provides a clear next action and prevents duplicate submissions.

### [ ] UX-20 — Make statistics comparisons interpretable

- **Original finding:** 21
- **Issue:** The stats hero shows an unexplained change badge and charts without direct values or comparison context.
- **Impact:** Users can see shapes and trends but cannot confidently interpret what changed or relative to which period.
- **Recommendation:** Label comparison periods explicitly, expose chart values on interaction, and keep period and media filters available while scrolling.
- **Priority:** Medium
- **Done when:** Every chart communicates its unit, period, comparison basis, and exact values without guesswork.

### [ ] UX-21 — Support system, light, and dark themes

- **Original finding:** 22
- **Issue:** Omnilog is dark-only and does not follow the device theme preference.
- **Impact:** Users who need or prefer a light presentation cannot adapt the interface.
- **Recommendation:** Add `Sistema`, `Clar`, and `Fosc` options after the shared color and contrast foundations are stable.
- **Priority:** Medium
- **Done when:** All core screens, dialogs, charts, states, and section accents have verified light and dark treatments.

## Suggested Working Method

Implement and verify one item at a time:

1. Change its status to `[-]`.
2. Define the exact affected screens and acceptance checks before implementation.
3. Test with populated data, empty data, and the relevant loading/error state.
4. For UI changes, test default font size and 200% font scaling.
5. Mark it `[x]` only after the **Done when** condition is satisfied on a device or emulator.
6. Record any intentionally deferred follow-up directly under the item instead of silently widening its scope.
