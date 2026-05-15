# Omnilog UI Design Direction - Iteration 1

This document captures the first agreed direction for the Omnilog redesign. It is intentionally a first iteration: the design should evolve after implementation, visual review, and real use.

## Product Feel

Omnilog should feel like a personal media library and tracker, somewhere between Goodreads and StoryGraph:

- visually appealing, clean, modern, and mobile-first
- simple enough for fast daily use
- rich enough that stats, metadata, and tracking state are easy to discover
- not web-page-like or overly "HTML-y"
- light on animation, using motion only where it improves understanding

The app should support a database-like browsing experience for media metadata and a personal tracking experience for user state. Metadata and user tracking state are both important, but they occupy different parts of the page.

## Visual Principles

1. **Cover-led and visual**
   - Covers/posters should carry the main visual weight on Home, item lists, item previews, and detail pages.
   - Cards are useful for individual rows, repeated items, and modal surfaces, but the whole app should not become a stack of heavy boxed panels.

2. **Metadata and tracking have separate focus**
   - Important provider metadata belongs prominently in the item hero area.
   - User tracking data belongs below the hero, with direct controls for current progress, rating, status, sessions, and notes.
   - API metadata remains metadata; user tracking state remains authoritative.

3. **Section accents, shared app base**
   - Anime, books, TV, and games each get a distinct accent color.
   - Accents should be soft and non-aggressive, not bright red/green/neon.
   - State colors should be separate from media-section colors where possible.
   - Colors should be centralized as design tokens so they are easy to change later.

4. **Spacious but not sparse**
   - The app should show useful information at a glance without feeling dense or overwhelming.
   - The feel should be editorial enough to be pleasant, but still fast and utilitarian.

## Theme Direction

Focus on dark mode first.

- Use a soft dark base, not pitch black.
- Avoid harsh contrast.
- Keep neutral surfaces consistent across sections.
- Use section colors as highlights: selected nav item, key buttons, progress accents, small visual markers.
- Light mode can come later after dark mode has a stable design language.

## App Identity

The app name is **Omnilog**.

Header direction:

- `Omni` can take the current section accent color.
- `log` can remain neutral.
- The name should generally remain visible in the header.

## Navigation

Bottom navigation has five items:

- `Inici`
- `Anime`
- `Llibres`
- `TV`
- `Jocs`

`Inici` is the landing page.

Each media section item should include a label and icon. The selected item should be highlighted with the relevant section accent.

Android back behavior should remain predictable:

- detail -> item list
- item list -> home/previous root when appropriate
- modal -> dismiss modal

## Page Model

### Inici

The Home page should eventually include:

- Omnilog header and profile/settings entry point
- global search later, not required in the first redesign slice
- high-level stats:
  - total items
  - items this year
  - minutes watched for anime/TV/movies
  - hours played for games
  - other useful summary stats as the model matures
- current reading/watching/playing items
- top-ranked items
- latest reviews or recent activity

Current and top sections should be cover-centric and swipeable. They should show relevant information at a glance:

- title
- author/director/studio/developer where relevant
- user score where relevant
- progress where relevant

Do not show irrelevant progress, for example movie progress in top-rated sections.

### Media List Pages

Each section page should have:

- header/search
- filters/sort controls
- saved item list

List rows should be cover-left and info-right, using a restrained card design. User-side info should be prominent:

- title
- author/director/studio/developer
- user score
- status
- progress where relevant

Tap opens item detail.

Editing should be available without forcing navigation away from the list. Preferred direction:

- visible lightweight edit affordance or contextual control for common edits
- long press/swipe can be added later for secondary actions
- avoid making long press/swipe the only way to discover actions

### Item Preview And Item Detail

Preview and detail should feel like the same item page in different states:

- Preview: item is not saved yet; history/current user sessions are absent.
- Detail: item is saved; sessions, history, tracking, edits, and delete are available.

Use shared item metadata components between:

- API review/preview
- saved item detail
- future metadata refresh preview/diff

The detail page should not need a separate title above the content. The hero acts as the title area.

Hero content:

- prominent cover/poster
- title
- original title/year when useful
- author/director/studio/developer
- provider rating and metadata
- relevant provider collection/franchise info
- Genre tags (below cover just above the use tracking section and scrolable if needed)

Below hero:

- user tracking/session area
- additional metadata/credits that did not fit in the hero
- history/past sessions

There should probably be no explicit label for the current session section, or it should be very light. If a label is needed later, candidates include `Seguiment actual`, `Ara mateix`, or `Progres actual`.

Actions:

- item edits live under a top-right vertical `...` menu
- each session has its own edit affordance or long-press affordance
- add/edit session should be a floating bottom-right action, similar in spirit to MAL
- avoid a dedicated "actions" section inside the page

### Stats Page

Stats can be introduced after core browsing/detail UI is stable. StoryGraph is a good reference for stats direction and visualizations.

Direction:

- accessible from Home, exact nav placement later
- general stats at top
- per-media sections
- top genres
- top authors/directors/studios/developers
- visualizations/charts once the data model and UX need are clearer

Do not build charts before the dashboard/list/detail redesign unless needed.

### Config Page

Can be deferred.

Likely entry point:

- top-right profile/settings icon

Possible future settings:

- theme mode
- section colors
- default view
- backup/import/export

## Forms And Data Entry

Data entry should be easy, seamless, and visually simple.

- Prefer modals for add/edit forms.
- Forms should generally not live directly on the page.
- Modals should be centered when appropriate.
- If a form is too long for a centered modal, consider a larger modal or full-screen modal treatment.
- Prefer dropdowns/segmented controls/toggles over text inputs where the value set is known.
- Keep manual add as fallback, not the primary flow.
- Keep labels clear and natural in Catalan.

## Interaction Direction

Keep interactions minimal and purposeful.

Possible later interactions:

- swipeable cover carousels
- long press or swipe for secondary row actions
- pull/swipe refresh for metadata refetch if it feels natural

Do not rely only on hidden gestures for important actions.

## Catalan Tone

Use warm, natural Catalan wording, without over-optimizing wording in early redesign work. Labels can be manually refined later.

## Implementation Sequence

Work in small, reversible slices:

1. Establish design tokens:
   - app name/header treatment
   - soft dark palette
   - media section accents
   - state colors
   - reusable surfaces/buttons/list row styles
2. Redesign shared preview/detail metadata hero.
3. Redesign media list rows.
4. Add Home as the landing dashboard.
5. Move edit/create flows toward modal patterns.
6. Add stats page after the core UI language is stable.
7. Add settings/config later if needed.

The first implementation pass should prioritize design foundations and the shared preview/detail page, because those choices will carry through the rest of the app.
