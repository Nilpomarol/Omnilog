# Omnilog UI Design Direction

Status: Current  
Last reviewed: 2026-09-12

This document defines the global visual language. Feature-specific UI may refine it. For Home, [`home.md`](home.md) wins wherever the two differ.

## Product Feel

Omnilog should feel like a personal media library and tracker: editorial, warm, restrained, mobile-first, and practical for daily use.

It should not feel like:

- default Material UI;
- a generic web dashboard;
- a stack of identical cards;
- an AI-generated collection of chips, gradients, glass panels, and hero banners.

## Core Principles

### Cover-led

Covers/posters carry most of the visual weight on Home, browse lists, previews, and detail pages.

Use containers where they clarify grouping, but do not wrap every concept in the same panel treatment.

### Separate metadata from personal tracking

Provider metadata and user-authored tracking are different semantic layers.

- metadata: title, creators, release data, provider ratings, synopsis, edition information;
- personal tracking: status, progress, sessions, ratings, notes, ownership, collections.

Do not visually blur those concepts.

### Semantic composition over uniform geometry

Consistency does not require every section to use the same card.

Use layouts that match the meaning of the content:

- continuation → progress-focused card;
- compact queue/planned list → dense row/card;
- analytics → typography/chart-led module;
- activity → chronology row;
- paused/resume → cover shelf;
- detail → cover-led editorial composition.

### Warm neutral base

The existing Omnilog theme is the baseline:

- soft charcoal / paper-like neutral backgrounds;
- theme-aware media accents;
- thin, restrained separators and borders;
- soft contrast rather than pitch black + pure white.

Do not introduce arbitrary colors outside the semantic palette without a product reason.

### Typography

Lato remains the primary functional family for body, controls, metadata, and compact headings.

Libre Baskerville may be used selectively for editorial identity, such as:

- Omnilog branding;
- major page display titles;
- selected media hero titles where it improves hierarchy.

Do not convert every heading to serif.

### Density

The interface should be spacious enough to scan but compact enough for real mobile use.

Avoid oversized decorative spacing that pushes useful content below the fold.

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

Media lists should remain cover-left / information-right when appropriate.

Prioritize:

- title;
- creator/context;
- user rating/status/progress when meaningful.

Do not expose irrelevant progress for media/state combinations where it adds noise.

Editing actions should remain discoverable; avoid relying exclusively on hidden gestures.

## Item Detail

The hero is the page title area; avoid adding a redundant screen title above it.

Detail should distinguish:

1. media identity / metadata;
2. current personal tracking state;
3. sessions/history/activity;
4. secondary provider/external information.

Preview and saved detail should share metadata presentation where semantics match.

## Forms And Dialogs

Prefer focused modal flows for short configuration tasks.

Avoid forms that expose every optional field at once. Show common fields first and secondary details progressively.

Destructive actions require clear confirmation or recoverability.

## Accessibility

UI changes must preserve:

- readable contrast;
- touch-target size;
- large-text behavior;
- clear labels;
- meaningful content descriptions where needed.

Do not encode meaning only through color.

## Visual Verification

A UI change is not complete solely because it compiles.

When possible, inspect rendered output for:

- normal populated state;
- empty/error state when affected;
- long title/metadata when relevant;
- enlarged system text for sensitive layouts.

Use screenshot regression only for high-value visual contracts rather than every component.
