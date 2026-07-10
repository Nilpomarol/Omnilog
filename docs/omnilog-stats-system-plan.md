# Omnilog Stats System Plan

## Purpose

This document captures the proposed direction for a StoryGraph-inspired stats system in Omnilog. It is a planning document, not an implementation record.

The goal is to make the user's local media history understandable at a glance: what they read, watched, played, rated, completed, revisited, and tended to choose over time.

The stats system must remain local-first. Room data and user-entered tracking state are authoritative; provider metadata can enrich statistics, but should not define the user's history.

## Current App Context

Omnilog already has enough data for a useful first stats page:

- `MediaItem` stores media type, release year, language, genres, creators, external rating, ranking/popularity metadata, ownership, and progress total.
- `TrackingSession` stores status, progress, rating, notes, platform, start date, finish date, update time, and progress updates.
- `ProgressUpdate` stores dated cumulative progress values.
- `TrackedMedia` joins the item, sessions, credits, collections, external ratings, and external tracking.
- Home already shows a small KPI strip with total titles, in-progress titles, average rating, and completed titles this year.

The current navigation model has `Home` plus selected media section pages. The bottom navigation already has five items: Home, Anime, Books, TV, and Games. Adding a sixth stats tab would crowd mobile navigation.

## Product Direction

Stats should be accessible from Home as a drill-in page. Home can show a compact `Estadistiques` module or CTA near the existing KPI strip. Tapping it opens the full stats screen.

This keeps stats visible without making it compete with the core daily navigation. If stats becomes a primary daily surface later, it can be promoted after real use.

The visual direction should be StoryGraph-inspired but Omnilog-native:

- dark-mode-first
- compact and scannable
- section accents used carefully
- simple chart modules built from Compose surfaces and bars
- no new chart dependency for the first version
- user history and personal ratings emphasized over provider popularity

## First Stats Page Shape

The first full stats screen should include:

- overview KPIs
- period filter: all time, this year, last 12 months
- media filter: all, anime, books, TV/movies, games
- completed activity by month
- rating distribution
- status breakdown
- progress totals by media type
- top genres
- top creators
- language breakdown
- best-rated items
- most recently active or most revisited items

Useful first KPIs:

- total tracked titles
- completed titles in selected period
- active titles now
- planned titles
- average personal rating
- total pages read
- total episodes watched
- total minutes watched
- total hours played
- revisits/replays

## Data Definitions

The stats page should use explicit definitions so numbers stay trustworthy.

### Completion

A title/session counts as completed in a period when:

- the session status is `Completed`
- `finishedAt` exists
- `finishedAt` falls inside the selected period

If `finishedAt` is missing, the session can count in all-time completed status summaries, but should not count in dated period charts unless a fallback is intentionally added later.

### Activity

Recent activity can use `TrackingSession.updatedAtEpochMillis`.

Monthly activity charts should prefer dated completion via `finishedAt` for completed counts. Progress activity can use `ProgressUpdate.loggedAt`.

### Personal Rating

Personal rating stats should use `TrackingSession.rating`, not provider ratings.

If an item has multiple sessions, initial MVP behavior can use all rated sessions for rating distribution and average. A later version can offer a mode for latest rating, best rating, or first completion rating.

### Progress Totals

Progress totals need media-specific interpretation:

- Books: progress unit is pages.
- Anime: progress unit is episodes.
- TV: progress unit is episodes.
- Movies: progress unit is minutes.
- Games: progress unit is hours.

For completed sessions, if `progressTotal` is present and greater than zero, use `progressTotal` as the completed amount. Otherwise use `progressCurrent`.

For progress charts based on `ProgressUpdate`, updates are cumulative values, not deltas. Calculate deltas between ordered progress updates within the same session. Clamp negative deltas to zero unless a future edit-history model can distinguish corrections from regressions.

### Genres

Top genres should come from `MediaItem.genres`.

For items with multiple genres, the first version can count each listed genre once per item/session. Weighted genre scoring can come later.

### Creators

Top creators should come from `MediaItem.creators`.

Interpretation differs by media type:

- Books: authors.
- Movies/TV: directors, studios, or primary provider creators depending on imported metadata.
- Games: developers/publishers depending on provider metadata.
- Anime: studios/creators depending on provider metadata.

Because creator semantics vary by provider, labels should stay generic at first: `Creadors principals`.

### Language

Language breakdown should use `MediaItem.language` when present. Unknown languages should be grouped under a clear `Sense idioma` or `Desconegut` label.

### External Ratings

Provider ratings can appear as supporting comparisons later, but MVP stats should not mix external ratings with personal ratings. Personal stats should remain user-owned.

## Technical Design

Start with pure Kotlin calculation over existing in-memory domain models. Do not add Room queries or schema changes for the first version.

Suggested package:

- `app/src/main/java/com/nilpo/contenttracker/core/stats/`

Suggested files:

- `StatsPeriod.kt`
- `StatsFilters.kt`
- `StatsModels.kt`
- `StatsCalculator.kt`

Suggested model shape:

```kotlin
data class StatsFilters(
    val period: StatsPeriod,
    val mediaTypes: Set<MediaType>,
)

data class StatsSnapshot(
    val filters: StatsFilters,
    val overview: List<StatsKpi>,
    val completedByMonth: List<StatsBucket>,
    val ratingDistribution: List<StatsBucket>,
    val statusBreakdown: List<StatsBucket>,
    val progressTotals: List<StatsKpi>,
    val topGenres: List<RankedStat>,
    val topCreators: List<RankedStat>,
    val languageBreakdown: List<StatsBucket>,
    val bestRatedItems: List<TrackedMedia>,
    val mostRevisitedItems: List<TrackedMedia>,
)
```

Keep the calculator independent from Compose and Android APIs where possible. This makes unit tests straightforward.

## UI Design

Suggested UI package:

- `app/src/main/java/com/nilpo/contenttracker/ui/stats/`

Suggested composables:

- `StatsScreen`
- `StatsFilterBar`
- `StatsKpiGrid`
- `StatsHorizontalBarChart`
- `StatsMonthlyBarChart`
- `StatsRankedList`
- `StatsMediaStrip`

The first UI should reuse the existing Omnilog design language:

- `OmnilogColors.AppBackground`
- `OmnilogColors.AppPanel`
- `OmnilogColors.AppLine`
- `OmnilogColors.AppInk`
- section accents from `MediaSection`
- 8dp rounded modules
- compact headings
- no heavy nested cards

The first charts should be custom Compose layouts using rows, boxes, proportional widths, and labels. Avoid a chart library until the requirements exceed simple bars and distributions.

## Navigation

Recommended first implementation:

- add `Stats` to `AppDestination`
- keep bottom navigation unchanged
- add a Home stats CTA/module
- tapping it sets `selectedDestination = AppDestination.Stats`
- back from Stats returns Home
- top bar accent remains dashboard accent

Do not add provider-specific import actions on Stats. Import actions should remain scoped to their section pages.

## Strings

Use Catalan labels consistent with the current app. Candidate labels:

- `Estadistiques`
- `Tot el temps`
- `Aquest any`
- `Ultims 12 mesos`
- `Tots`
- `Completats`
- `En curs`
- `Planificats`
- `Nota mitjana`
- `Activitat mensual`
- `Distribucio de notes`
- `Estats`
- `Progres total`
- `Generes principals`
- `Creadors principals`
- `Idiomes`
- `Mes valorats`
- `Mes revisitats`

The project currently has some mojibake in older docs and strings. When editing source files, preserve existing file encoding behavior and keep UI strings in the existing Android string resource style.

## MVP Implementation Sequence

1. Create the stats domain models and calculator.
2. Add focused tests for completion counts, rating distribution, progress totals, and progress-update delta handling.
3. Add `StatsScreen` with static sections fed by a `StatsSnapshot`.
4. Add the Home CTA and `AppDestination.Stats` navigation.
5. Add Catalan strings.
6. Build with `assembleDebug`.
7. Run emulator/device visual QA if available.

## Later Enhancements

Later versions can add:

- custom date ranges
- yearly comparison
- streaks
- weekday/month heatmaps
- owned vs not owned stats
- imported-source breakdown
- provider metadata freshness stats
- external-rating comparison
- creator role-specific stats once metadata roles are richer
- exportable yearly recap
- shareable visual summary

## Risks And Open Questions

- Some stats will be incomplete when `finishedAt`, `progressTotal`, genres, creators, or language are missing.
- Movies need reliable duration in `progressTotal` to produce useful minutes-watched totals.
- Creator fields are not role-specific enough for precise StoryGraph-like breakdowns across all media types.
- Multiple sessions per item need a deliberate rating policy.
- Progress updates represent cumulative values, so progress-over-time requires careful delta calculation.
- If stats later need fast large-library performance, Room aggregate queries may be useful. Start with pure calculation first.

