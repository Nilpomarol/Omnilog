# Omnilog Stats Improvement Plan

## Purpose

This document turns the stats feature review into an implementation plan. The existing stats MVP is broad and visually consistent, but its next iteration should make the numbers more trustworthy, the hierarchy more selective, and the charts easier and more rewarding to explore.

Stats remain a secondary Home drill-in rather than a primary navigation destination. The goal is not to add more reporting for its own sake; it is to make the existing personal history feel clear, polished, interactive, and memorable without introducing unnecessary complexity.

## Execution Order

Start this plan after the remaining tasks in `docs/omnilog-product-ux-backlog.md` are complete. `UX-20` in that backlog is the minimum comparison-clarity requirement; this plan is the broader follow-up for the whole stats experience.

## What Already Works

- Stats are correctly positioned as a Home preview and drill-in page instead of a sixth bottom-navigation item.
- Period and media filters are easy to find and understand.
- Group headings, panels, empty states, and media-type colors provide a consistent visual language.
- Personal ratings and local tracking history remain more prominent than provider popularity data.
- Best-rated and most-revisited cover strips are visually engaging and link back to useful item details.

These foundations should be preserved. The work below is a refinement, not a redesign from scratch.

## Status Legend

- `[ ]` Todo
- `[-]` In progress
- `[x]` Complete
- `[~]` Deferred

## Recommended Implementation Order

| Order | ID | Summary | Priority | Status |
|---:|---|---|---|---|
| 1 | STATS-01 | Make definitions, periods, and comparisons trustworthy | High | `[x]` |
| 2 | STATS-02 | Create a concise, useful top-level summary | High | `[ ]` |
| 3 | STATS-03 | Replace misleading or unclear chart forms | High | `[ ]` |
| 4 | STATS-04 | Add focused chart inspection and drill-down | Medium | `[ ]` |
| 5 | STATS-05 | Adapt the page to filters, sparse data, and accessibility | Medium | `[ ]` |

## [x] STATS-01 — Make definitions, periods, and comparisons trustworthy

### Problem

- The headline completion value counts distinct titles, while monthly activity counts completed sessions. A revisited title can therefore contribute one to the headline and multiple entries to the chart.
- `This year` compares the current year-to-date with the whole previous calendar year.
- Delta chips do not name the comparison period.
- Content mix uses completed items for dated periods but the entire library for `All time`.
- A best-rated item can qualify through a rating in the selected period while its badge displays a higher rating from outside that period.
- The all-time headline is paired with only the latest 12 monthly bars, without explaining the difference in scope.

### Change

- Name and model distinct concepts explicitly: `unique titles completed` and `completion sessions`. Use one consistently in any single headline/chart relationship.
- Compare year-to-date with the same date range in the previous year. Keep full-year comparisons only for completed historical years. *(Done under UX-20 for the headline KPI delta window.)*
- Label deltas with their basis, such as `vs. mateix període de 2025`. *(Done under UX-20 — `PeriodDelta.basis` + hero label.)*
- Define content mix as content completed in the selected period for every period, including all time. Keep current-library state in the clearly labelled `Estat actual` section.
- Calculate and display best-rated values from the filtered rating set.
- For all time, use yearly aggregation, a horizontally explorable full timeline, or an explicit `Últims 12 mesos` subtitle beneath the all-time total.
- Add focused calculator tests for every definition and comparison boundary.

### Why

Stats lose their value quickly when two nearby numbers appear to disagree. Consistent definitions and visible scope are the most important prerequisites for every later visual or interactive improvement.

### Done when

- Every headline and chart states or clearly implies whether it counts titles, sessions, or units consumed.
- Every comparison uses a like-for-like date window and names its baseline.
- Changing period never silently changes the population definition of a statistic.
- Filtered media badges show values from the active period and media scope.

**Implementation note (2026-07-17):** The calculator, models, labels, filtered badges, and focused boundary tests satisfy the criteria above. The focused stats suite, full debug unit-test suite, and debug APK build pass. Device startup QA found no crash. Visual verification at default and 200% font scale was not completed because the attached physical device was locked behind System UI and no emulator was configured; the item was marked complete at the user's direction with that limitation recorded.

## [ ] STATS-02 — Create a concise, useful top-level summary

### Problem

The page opens with one large completion number and then gives similar visual weight to more than ten modules. Overall average rating, revisits, and other already-calculated summary values are not presented together, so the screen reads as a long report rather than a curated curiosity.

### Change

- Keep monthly activity as the visual hero.
- Add a compact summary row or grid for the selected period: unique titles completed, average personal rating, revisits, and one unit-aware consumption highlight.
- Add one short generated observation derived from existing data, for example `Juny va ser el teu mes amb més activitat` or `Els llibres van ser el tipus més ben valorat`.
- Prioritize the most informative sections first and hide modules that have no meaningful comparison, rather than rendering every possible module at equal weight.
- Keep group headings, but reduce repeated card chrome where adjacent charts already belong to the same conceptual group.

### Why

A strong first viewport gives users the reward of opening Stats immediately. Selective presentation also keeps this secondary feature approachable without removing deeper information.

### Done when

- The first viewport communicates the period's main result, rating, revisits, and one useful observation.
- A user can understand the period without scrolling through the full page.
- Lower sections remain available without competing visually with the summary.

## [ ] STATS-03 — Replace misleading or unclear chart forms

### Problem

- Average-length bars and consumption bubbles compare pages, episodes, minutes, and hours on one visual scale, even though those units are not comparable.
- The genre pie can contain eight slices and treats overlapping genre tags as parts of one whole.
- The rating distribution removes zero-value scores, weakening the fixed 1–10 scale.
- Rating trend lines connect rated months across gaps with no data, and sample size is visible only for the latest point.
- The completion-share pie has no internal label and sits under the misleadingly broad heading `Mitjanes per tipus`.
- Language blocks display stored values rather than the app's friendly language labels.

### Change

- Replace cross-unit bars and bubbles with clearly labelled numeric tiles. Compare consumption only within the same media type or across time for that type.
- Replace the genre pie with ranked bars, or use a top-five-plus-`Altres` view that is explicitly based on genre mentions rather than exclusive title share.
- Preserve all positions from 1 through 10 in the rating distribution.
- Break rating trend lines across empty months and expose the rating count for every populated point.
- Give completion share its own title and retain the pie only when at least two media types have data.
- Reuse the existing language normalization and display-label mapping.

### Why

The current variety is visually appealing, but decorative size differences should not imply invalid comparisons. Clearer forms preserve the visual impact while making the charts easier to interpret accurately.

### Done when

- No shared visual scale compares unlike units.
- Part-to-whole charts are used only for mutually interpretable parts.
- Rating and language charts preserve their expected scales and human-readable labels.
- Empty periods cannot appear as continuous measured trends.

## [ ] STATS-04 — Add focused chart inspection and drill-down

### Problem

The global filters and media covers are interactive, but the charts themselves are static. Users can see shapes without inspecting exact values or discovering which titles produced them.

### Change

- Allow tap or drag inspection on monthly activity and rating trends, showing period, exact value, media split, and rating sample size where relevant.
- Make useful categories selectable: month, rating, genre, creator, language, status, and media type.
- On selection, show a compact associated-title strip or offer a clear route to the corresponding filtered library list.
- Keep interactions optional and discoverable; the default chart must remain understandable without gestures.
- Keep the period and media filters available during long-page exploration, using a sticky treatment only if it does not crowd smaller screens or large text. *(Done under UX-20 — sticky header, verified at 200% font scale.)*

### Why

Inspection and drill-down make the feature feel exploratory without requiring additional statistics. They also solve exact-value clarity more effectively than permanently labelling every mark.

### Done when

- Important chart marks expose exact values through an accessible interaction.
- At least monthly activity, ratings, and content-mix categories can reveal their contributing titles.
- Every interaction has an accessible label and does not depend on color alone.

## [ ] STATS-05 — Adapt to filters, sparse data, and accessibility

### Problem

Selecting one medium can leave one-slice pies and one-bar breakdowns that consume space without adding information. Dense legends and fixed chart dimensions may also become difficult at large font scales, while sparse histories can produce a page dominated by empty modules.

### Change

- When one medium is selected, replace cross-medium composition modules with that medium's useful totals and trends.
- Require enough categories or observations before rendering a comparison chart; otherwise show a concise value or omit the module.
- Consolidate repeated empty states into one helpful explanation near the top when the active filter has very little data.
- Verify chart labels, legends, tooltips, filter controls, and media strips at default and 200% font scale.
- Verify both sparse and dense libraries, all period options, every media filter, missing metadata, and multiple-session items.

### Why

The page should feel intentionally composed for the selected data, not like a fixed dashboard template with most of its cells empty or redundant.

### Done when

- Single-medium mode contains no meaningless one-category comparisons.
- Sparse-data screens remain compact and encouraging.
- Dense data remains legible, and essential content is not clipped at 200% font scale.

## Optional Later Additions

These are not part of the baseline refinement and should be considered only after the five items above are verified:

- **Yearly recap card:** a polished, optionally shareable summary of completed titles, busiest month, favorite genre, average rating, and most-revisited item.
- **Activity calendar:** a tappable annual heatmap, but only when dated completion or progress data is dense enough to produce a meaningful pattern.

Avoid adding streaks, provider statistics, or more distributions merely to increase the number of modules. Any later addition should reveal a genuinely new aspect of the user's history.

## Verification

For each item:

1. Add or update pure Kotlin calculator tests before relying on new UI presentation.
2. Test all time, this year, last 12 months, historical years, and period boundaries.
3. Test all-media and every single-medium filter.
4. Test empty, sparse, dense, and multiple-session libraries.
5. Test default and 200% font scale on a device or emulator.
6. Confirm that chart meaning remains understandable without color or gesture alone.

