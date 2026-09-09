# Omnilog Home And Social-Aware Information Architecture

Status: approved product/UI direction, not yet implemented.

This document defines the next Home redesign and the navigation responsibilities around it. It is intentionally social-aware: the Home layout should not need to be rebuilt when friends, friend activity, and shared item context are added.

It supersedes the older Home-specific direction in `docs/omnilog-ui-design-v1.md` where the two disagree. The broader visual principles in that document still apply.

## Product Goal

Home should answer three questions quickly:

1. What am I consuming now?
2. What could I start or resume next?
3. How am I doing?

Home is a daily dashboard, not a miniature copy of every major Omnilog screen and not a social feed.

The intended feel remains cover-led, warm, mobile-first, compact, and personal. Covers provide most of the colour. Panels and borders should support hierarchy rather than turn the page into a stack of boxes.

## Information Architecture

The long-term responsibilities are:

- **Home** — daily personal dashboard.
- **Anime / Llibres / Cinema i TV / Jocs** — owned/tracked library browsing and management.
- **Stats** — detailed personal analytics, reached from Home/Profile rather than bottom navigation.
- **Cronologia** — broader personal and social activity. The existing Timeline is the foundation for this destination.
- **Profile** — identity, library summary, objectives, and social relationships.
- **Item detail** — metadata, personal tracking, and social context for that specific title.

Do not add a dedicated Social bottom-navigation tab for the first social version. Social should enrich existing product surfaces rather than become a parallel product.

The five root navigation items remain Home plus the four media sections.

## Home Structure

The target order is:

1. App header + global content search
2. `Ara mateix`
3. `Per començar`
4. `El teu ritme`
5. `Activitat recent`
6. `Per reprendre`

Do not add `Completats recentment` back as a permanent Home section.

A normal phone's first viewport should contain Search, most or all of `Ara mateix`, and the beginning of `Per començar`. The page may scroll, but it should no longer feel like an endless catalogue.

## 1. Header And Search

Keep the Home header lightweight:

- Omnilog identity / title
- profile entry point
- global content search

Search remains primarily a media/library action. Friend discovery belongs to the Profile/social relationship flow rather than making the Home search ambiguous.

Use vertical space carefully. Avoid decorative hero artwork, greetings, quotes, weather-like context, or other non-functional content above the current-media section.

## 2. Ara mateix

`Ara mateix` is the primary Home surface.

### Layout

Replace poster-only tiles with dedicated horizontal Continue cards.

Suggested card characteristics:

- approximately 280–300 dp wide on a normal phone
- approximately 125–135 dp tall
- horizontal carousel with roughly 1.1–1.3 cards visible
- cover on the left
- title and useful type/context on the right
- current progress and progress bar
- one clear quick-progress action

The next card should visibly peek from the edge so horizontal scrolling is obvious.

### Content

Show only information useful for continuing:

- cover
- title
- media context where useful
- current/total progress when meaningful
- progress bar
- one quick action

Do not repeat an `InProgress` status badge inside a section that already means In Progress. Avoid stacking multiple status/action circles on top of the artwork.

The current hidden-media-type Home preference remains valid and can continue to filter this section.

### Social awareness

Home must stay personal-first after social ships.

A current card may show small contextual social proof when available, for example `2 amics també l'estan veient`, but this is secondary metadata. Do not place friend posts or a social feed inside `Ara mateix`.

## 3. Per començar

Rename the current `Següent a la llista` / planned surface to `Per començar`.

The current Planned collection is not a manually ordered queue; it is a set of Planned items ordered by recent activity. Calling it `Següent` implies queue semantics that do not exist.

### Layout

Do not reuse the same poster component as `Ara mateix`.

Use compact horizontal cards or rows, for example:

- small cover
- title
- creator/collection or media type when useful
- lightweight start action

The section should fit more choices into less height than the current poster carousel.

If manual queue ordering is introduced later, a true `Següent` surface can be reconsidered.

## 4. El teu ritme

Merge the current Home objectives preview and yearly analytics preview into one Home module called `El teu ritme`.

The detailed destinations remain separate; only their Home summaries are merged.

### Top: yearly rhythm

Use one lead metric, currently completion sessions, plus a small recent-month chart and comparison where available.

Example hierarchy:

- `49 sessions`
- `+21 vs. any passat`
- compact monthly activity chart

Tapping this part opens Stats.

### Bottom: objectives

Keep objectives glanceable:

- status summary such as `2 al dia · 1 endarrerit`
- up to four compact goal indicators
- preserve the existing priority rule: objectives needing attention appear first

The full 80 dp dashboard rings are too dominant when combined with analytics. Retain the ring/notch visual language at a smaller size rather than duplicating the Profile goal cards.

Tapping the objectives part opens Profile/Objectives.

This module should remain concise enough that Stats and objectives still have a reason to exist as drill-ins.

## 5. Activitat recent

Reduce the current multi-row Home activity preview to a single latest meaningful entry.

Example:

- small cover/icon
- title
- `+48 pàgines`
- `Avui`
- arrow / `Cronologia` affordance

Tapping the row or header opens the broader chronology.

The goal is presence, not duplication: Home should remind the user that a history exists, while the full destination owns browsing, filtering, and older events.

### Naming

`Activitat` already has a precise title-level meaning in Omnilog: the per-session record of progress/status changes. Keep that concept intact.

Use **Cronologia** for the broader app-level personal/social activity destination. The implementation can retain existing Timeline class/route names during migration; the product label is the important distinction.

## 6. Per reprendre

Rename the Home paused section to `Per reprendre`.

Keep the existing useful ordering principle: longest-stalled titles first, because this surface exists to resurface items the user has stopped noticing.

### Layout

Use a small cover shelf rather than another full-size primary carousel:

- 3–4 covers visible
- title optional below/over the lower edge, depending on readability
- minimal status decoration
- no duplicate Paused badge when the section itself provides that meaning

Hide the section completely when empty.

## Removed From Home

### Completats recentment

Remove the permanent `Completats recentment` carousel.

Completed items remain available through:

- each media library
- Stats
- Cronologia
- Profile summaries where useful

Home can still celebrate completion through a compact metric such as `3 completats aquest mes` inside `El teu ritme`, but it should not spend another full carousel on completed history.

### Multi-row recent activity

Replace the current three-row preview with the one-row chronology teaser described above.

### Separate large objectives and analytics cards

Replace them with the merged `El teu ritme` module.

## Social-Aware Restructuring

The social layer should extend existing destinations as follows.

### Cronologia

Evolve the existing Timeline into the broader activity destination.

Initial social structure:

- `Tu` — the user's own consumption chronology
- `Amics` — activity from friends

Keep personal and friend feeds separated by a clear tab/segment rather than mixing them into one algorithmic Home feed.

The current Timeline derivation remains useful for the `Tu` side. Social activity can come from the future shared/backend model without changing the meaning of the user's local tracking history.

### Profile

Profile becomes the main social relationship hub in addition to its existing identity and goals responsibilities.

Add entry points for:

- friends
- friend requests
- find/invite people

A friend's profile should reuse the visual structure of Profile in read-only form where possible, showing only information that is intentionally shared, such as current titles, favourites, high-level stats, and recent public activity.

Do not re-add `Ara mateix` to the user's own Profile merely because friend profiles may expose a current-media section. The user's own current items already belong on Home.

### Item Detail

Item detail is the best place for title-specific social context.

Add a compact friends surface when social data exists, for example:

- friends who have the title
- their status
- their rating
- recent relevant friend activity

This should be contextual and secondary to the user's own tracking controls.

### Home

Do not add a permanent `Amics` feed section to Home in the first social version.

Allowed social additions are lightweight and contextual, such as:

- `2 amics també l'estan veient`
- small avatar cluster on a current/planned item
- a friend rating count on an item where it materially helps selection

The daily Home hierarchy must still work when the user has zero friends or is offline.

## Current → Target Mapping

| Current Home surface | Target |
| --- | --- |
| Search | Keep, visually integrate with new header |
| `Ara mateix` poster carousel | Dedicated horizontal Continue cards |
| `Següent a la llista` poster carousel | `Per començar`, compact cards/rows |
| Objectives card | Merge into `El teu ritme` |
| Year analytics card | Merge into `El teu ritme` |
| Three-row recent activity | One-row `Activitat recent` teaser → Cronologia |
| `En pausa` poster carousel | `Per reprendre`, smaller cover shelf |
| `Completats recentment` | Remove from Home |
| Full Timeline | Evolve product label/destination to `Cronologia`; later `Tu / Amics` |
| Profile objectives/identity | Keep; add friend relationship entry points |
| Item detail | Add title-specific friend context later |

## Visual Direction

Keep the existing Omnilog visual system and make it warmer through hierarchy rather than decoration.

- warm soft-dark background rather than pitch black
- off-white primary text and warm muted secondary text
- covers provide most of the colour
- section/media accents remain restrained
- more whitespace between conceptual groups
- fewer full-width borders and dividers
- minimal shadows
- no glassmorphism
- no decorative gradients or landscape artwork
- no large generic hero banner
- avoid repeating the same card geometry for every section

Cozy should mean calm, tactile, and personal — not visually busy.

## Implementation Order

Implement the redesign in reversible slices:

1. Introduce the new Home section hierarchy without social dependencies.
2. Build the dedicated `Ara mateix` Continue card.
3. Replace Planned posters with `Per començar` compact cards.
4. Merge analytics + objectives into `El teu ritme`.
5. Reduce Home recent activity to one row and rename the broader destination to `Cronologia` at the UI/product level.
6. Replace `En pausa` with the smaller `Per reprendre` shelf and remove `Completats recentment`.
7. Perform normal-device and 200% font-scale QA.
8. When social infrastructure ships, add `Cronologia: Tu / Amics`, Profile relationship entry points, and item-detail friend context.
9. Add only lightweight social context to Home after the social destinations work independently.

Do not block the Home redesign on the social backend. The layout is deliberately designed so social can be layered on later.

## Acceptance Criteria

The redesign is successful when:

- the first viewport clearly prioritizes Search → `Ara mateix` → `Per començar`
- Home no longer contains repeated full-size poster carousels for every state
- the page is materially shorter than the current dashboard
- current, planned, goals/stats, activity, and paused content each use a layout suited to their purpose
- detailed Stats, goals, chronology, and completed history remain discoverable without being duplicated on Home
- Home remains fully useful with no social account/friends/network connection
- adding friends later does not require a new bottom-navigation destination or a Home information-architecture rewrite
- personal tracking remains authoritative and social data stays contextual rather than taking ownership of local history
