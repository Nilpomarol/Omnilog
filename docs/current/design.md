# Omnilog UI Design Direction

Status: Current  
Last reviewed: 2026-09-13

This document defines the global visual language. Feature-specific UI may refine it. For Home, [`home.md`](home.md) wins wherever the two differ.

## Product Feel

Omnilog should feel like a **personal collection of stories**: warm, tactile, calm, cover-led, restrained, mobile-first, and practical for daily use.

The product may borrow from editorial design, but **personal comes before editorial**. It should feel closer to opening your own media shelf, journal, or collection than opening a magazine, streaming service, media database, or analytics dashboard.

The target feeling is:

- warm rather than clinical;
- personal rather than institutional;
- collected rather than decorated;
- tactile rather than glossy;
- refined without feeling luxurious or formal;
- modern without losing character.

It should not feel like:

- default Material UI;
- a generic web dashboard;
- a streaming-service catalogue;
- a luxury/editorial magazine layout;
- a stack of identical cards;
- an AI-generated collection of chips, gradients, glass panels, hero banners, and decorative filler.

## Aesthetic Identity — Warm Personal Library

### Personal before editorial

Editorial influence should mainly appear through hierarchy, typography, spacing, and composition. It must not make the app feel formal, fashion-oriented, or publication-like.

If a choice has to be made between a cleaner editorial composition and a warmer, more personal one, prefer the personal one as long as usability remains strong.

### Artwork provides the personality

Covers, posters, and user-selected imagery should provide most of the screen's strong colour and visual character.

The surrounding UI should stay quiet enough that the library feels like it belongs to the user rather than to Omnilog's chrome.

Avoid decorating empty space with generic illustrations, quotes, scenery, gradients, or motifs that are unrelated to the user's actual collection.

### Warm tactile surfaces

Surfaces should feel closer to paper, cards, shelves, or a personal journal than to glass or floating SaaS panels.

Prefer:

- warm ivory / cream / paper-like backgrounds in light mode;
- soft charcoal rather than pitch black in dark mode;
- subtle tonal separation between background and surfaces;
- restrained borders;
- very soft shadows when depth is genuinely useful;
- moderate corner radii rather than exaggerated pill geometry everywhere.

Avoid:

- pure white as the dominant light surface;
- pure black as the dominant dark surface;
- glassmorphism;
- glossy gradients;
- strong drop shadows;
- excessive floating cards.

### Colour character

The semantic Omnilog palette remains authoritative, but its visual character should stay muted and natural.

Preferred supporting character:

- muted forest / olive greens;
- ochre and warm yellow;
- terracotta / clay tones;
- dusty blue where appropriate;
- warm greys and browns.

These are supporting tones, not a licence to introduce arbitrary colour. Media artwork should still provide most of the saturated colour on screen.

Avoid highly saturated accents unless the colour has an existing semantic role.

## Core Principles

### Cover-led

Covers/posters carry most of the visual weight on Home, browse lists, previews, and detail pages.

Use artwork generously and preserve recognisable aspect ratios. Avoid unnecessary overlays that compete with the cover itself.

Use containers where they clarify grouping, but do not wrap every concept in the same panel treatment.

### Separate metadata from personal tracking

Provider metadata and user-authored tracking are different semantic layers.

- metadata: title, creators, release data, provider ratings, synopsis, edition information;
- personal tracking: status, progress, sessions, ratings, notes, ownership, collections.

Do not visually blur those concepts.

Personal information should usually carry more emotional weight than provider metadata. The app exists to represent the user's relationship with the media, not merely to reproduce a provider database.

### Semantic composition over uniform geometry

Consistency does not require every section to use the same card.

Use layouts that match the meaning of the content:

- continuation → progress-focused card;
- compact queue/planned list → dense row/card;
- analytics → typography/chart-led module;
- activity → chronology row;
- paused/resume → cover shelf;
- detail → cover-led personal composition;
- large library → compact cover-left list or intentionally cover-dominant browse treatment.

Whitespace, typography, dividers, shelves, rows, and cards are all valid tools. Prefer the simplest structure that makes the content hierarchy obvious.

### Warm neutral base

The existing Omnilog theme is the baseline:

- paper-like warm neutral backgrounds in light mode;
- soft charcoal neutral backgrounds in dark mode;
- theme-aware media accents;
- thin, restrained separators and borders;
- soft contrast rather than pitch black + pure white.

Do not introduce arbitrary colors outside the semantic palette without a product reason.

### Typography

Lato remains the primary functional family for body, controls, metadata, and compact headings.

Libre Baskerville may be used selectively for identity and warmth, such as:

- Omnilog branding;
- major page display titles;
- selected media titles or section headings where it improves character and hierarchy.

Serif type should add personality, not dominate the interface. Functional information should remain highly legible and compact.

Do not convert every heading to serif.

### Density

The interface should be spacious enough to scan but compact enough for real mobile use.

Warmth must not come from oversized spacing. Prefer tighter, well-composed layouts with tactile surfaces and good typography over decorative breathing room.

Avoid oversized decorative spacing that pushes useful content below the fold.

### One strong action before many equal actions

Important surfaces should normally have one visually dominant action. Secondary actions should recede into icons, contextual controls, overflow menus, or lower-emphasis buttons.

Do not turn every available action into an equally prominent button or chip.

## Navigation

Root navigation remains:

- Home
- Anime
- Books
- Cinema/TV
- Games

Do not add a dedicated Social root tab for the initial social direction.

Selected navigation should primarily use accent/color + label weight, not redundant pills/containers.

Android back behavior should remain predictable:

- modal → dismiss;
- detail → previous list/surface;
- nested screen → previous destination;
- root destinations should not create surprising loops.

## Browse / Lists

Browse pages should feel like **looking through your own collection**, not querying a database table.

Media lists should remain cover-left / information-right when appropriate.

Prioritize:

- cover/poster;
- title;
- creator/context;
- personal rating/status/progress when meaningful.

Secondary information such as genres, provider scores, ownership, collection position, and dates should only appear when they materially improve scanning or decision-making. Do not reproduce every field available in the model simply because it exists.

Do not expose irrelevant progress for media/state combinations where it adds noise.

Editing actions should remain discoverable; avoid relying exclusively on hidden gestures.

For dense libraries, prefer compact rows and separators over large repeated cards. A list may use little or no card chrome if spacing and dividers already provide enough structure.

## Item Detail

The hero is the page title area; avoid adding a redundant screen title above it.

Detail should distinguish:

1. media identity / metadata;
2. current personal tracking state;
3. sessions/history/activity;
4. secondary provider/external information.

The page should feel like the user's page for that title, not a metadata profile with personal controls attached afterward.

Prefer artwork, typography, and whitespace over a stack of boxed sections. Use disclosure rows or compact secondary sections for lower-frequency information.

Preview and saved detail should share metadata presentation where semantics match.

## Activity / Chronology

Activity should read as a **personal media diary**, not an audit log.

Use a calm chronological rhythm with covers, dates, and meaningful milestones. Starts, completions, ratings, and major session events may carry more visual weight; routine progress entries should be lighter.

Avoid excessive technical timeline decoration or large cards for every entry. The chronology should feel like a story of what the user experienced over time.

## Stats

Stats should remain personal and reflective rather than dashboard-like.

Prefer:

- a small number of meaningful lead metrics;
- restrained charts;
- warm surfaces;
- direct relationship to the user's media history.

Avoid KPI-wall layouts, excessive chart cards, and business-dashboard visual language.

## Forms And Dialogs

Prefer focused modal flows for short configuration tasks.

Avoid forms that expose every optional field at once. Show common fields first and secondary details progressively.

Destructive actions require clear confirmation or recoverability.

## Motion

Motion should feel gentle, physical, and purposeful.

Appropriate uses include:

- progress changes;
- expanding/collapsing information;
- adding or removing items;
- moving into media detail;
- shelf/carousel interactions.

Avoid flashy transitions, large bouncing effects, or gamified animation that competes with the media itself.

## Accessibility

UI changes must preserve:

- readable contrast;
- touch-target size;
- large-text behavior;
- clear labels;
- meaningful content descriptions where needed.

Do not encode meaning only through color.

Warmth and subtle contrast must never reduce readability.

## Visual Rules

When making visual decisions, prefer these rules in order:

1. Personal before editorial.
2. Artwork before decoration.
3. Warm before sterile.
4. Whitespace before unnecessary containers.
5. Typography before borders.
6. One strong action before many equal actions.
7. Different content deserves different layouts.
8. Personal history before generic analytics.
9. Real user content before decorative filler.
10. Compact mobile usability before presentation-only aesthetics.

## Visual Verification

A UI change is not complete solely because it compiles.

When possible, inspect rendered output for:

- normal populated state;
- empty/error state when affected;
- long title/metadata when relevant;
- enlarged system text for sensitive layouts;
- whether artwork still carries the intended visual emphasis;
- whether the screen feels personal rather than like generic Material or dashboard UI.

Use screenshot regression only for high-value visual contracts rather than every component.

## Target Feeling

Opening Omnilog should feel closer to opening **your own shelf, collection, or media journal** than opening a streaming service, media database, or analytics product.

The interface should be modern enough for fast everyday use, but warm and distinctive enough that the collection feels like it belongs to the user.
