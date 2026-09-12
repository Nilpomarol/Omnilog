# Omnilog Home And Social-Aware Information Architecture

Status: Current approved direction; implementation still pending  
Last reviewed: 2026-09-12

This document is authoritative for Home and supersedes older Home-specific guidance.

## Goal

Home should answer quickly:

1. What am I consuming now?
2. What could I start or resume next?
3. How am I doing?

Home is a daily personal dashboard, not a miniature copy of every Omnilog screen and not a social feed.

## Long-Term Surface Responsibilities

- **Home** — daily personal dashboard.
- **Anime / Books / Cinema & TV / Games** — tracked-library browsing and management.
- **Stats** — detailed personal analytics reached from Home/Profile.
- **Timeline / Cronologia** — broader personal activity and the natural base for future social activity.
- **Profile** — identity, library summary, objectives, and future social relationships.
- **Item detail** — metadata, personal tracking, and title-specific social context.

Do not add a dedicated Social root tab for the first social version.

## Target Home Order

1. Header + global media/library search
2. `Ara mateix`
3. `Per començar`
4. `El teu ritme`
5. `Activitat recent`
6. `Per reprendre`

Do not keep `Completats recentment` as a permanent Home section.

A normal first viewport should contain Search, most or all of `Ara mateix`, and the beginning of `Per començar`.

## Header And Search

Keep the header lightweight:

- Omnilog identity;
- profile entry point;
- global media/library search.

Friend discovery belongs to Profile/social relationships rather than making Home search ambiguous.

Avoid decorative greetings, quotes, hero art, weather-style context, or other content above the primary daily-use section.

## Ara mateix

This is the primary Home module.

Use dedicated horizontal continuation cards rather than generic poster tiles.

Suggested characteristics:

- about 280–300 dp wide;
- about 125–135 dp tall;
- roughly 1.1–1.3 cards visible;
- cover on the left;
- title/context on the right;
- progress and progress bar;
- one clear quick-progress action.

The next card should peek enough to signal horizontal scrolling.

Do not repeat an `In progress` badge inside a section that already means exactly that.

The existing hidden-media-type preference may continue to filter this section.

Future social context may appear only as secondary metadata, e.g. a small “2 friends are also watching” line. Do not put a social feed here.

## Per començar

Rename the planned-items surface to `Per començar`.

The current collection is not a manually ordered queue, so avoid names that imply strict next-up ordering.

Do not reuse the `Ara mateix` continuation card.

Use a denser treatment such as:

- small cover;
- title;
- creator/collection/media type when useful;
- lightweight start action.

The section should show more choices in less vertical space than the current poster carousel.

If explicit manual queue ordering is introduced later, a true `Següent` concept can be reconsidered.

## El teu ritme

Merge the current objectives preview and yearly/analytics preview into one Home summary module.

Keep their detailed destinations separate; only their Home-level summary is unified.

This module should be typography/data-led rather than another poster/card carousel.

Focus on a small amount of useful information, for example:

- one active objective/progress summary;
- one concise recent/yearly insight;
- entry points to full objectives and Stats.

## Activitat recent

Use one compact chronology row/preview rather than a large activity module.

Its purpose is to make the Timeline discoverable and show that something changed recently, not to recreate Timeline on Home.

Future social activity should primarily live in Timeline/Cronologia; Home may surface a highly contextual social hint only when useful.

## Per reprendre

Use a compact cover shelf for paused/stalled items.

Its role is resurfacing forgotten media, not showing full tracking detail.

Order should continue to favor the most stale/forgotten items when that is the existing semantic rule.

## Explicit Removals / Non-Goals

Do not add to Home merely because the data exists:

- recently completed carousel;
- generic top-rated carousel;
- large social feed;
- decorative recommendation hero;
- duplicated Stats modules;
- duplicated Profile/objective modules;
- repeated card geometry for all sections.

## Social-Aware Constraints

Social features should enrich existing product surfaces instead of requiring a separate parallel navigation model.

Likely homes:

- friend relationships / discovery → Profile;
- broad friend activity → Timeline/Cronologia;
- reactions/comments/shared context for one title → item detail;
- small contextual social proof → selected Home/item surfaces.

The personal tracking experience remains primary.

## Implementation Notes

Implement this redesign incrementally rather than rewriting Home in one large pass.

A sensible order is:

1. `Ara mateix` continuation cards;
2. `Per començar` compact treatment;
3. `El teu ritme` merge;
4. compact activity preview;
5. `Per reprendre` cover shelf;
6. remove the old completed section and obsolete Home-only components.

Each slice should preserve existing navigation and user-data behavior and should be visually inspected before being considered complete.
