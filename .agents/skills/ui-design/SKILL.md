---
name: ui-design
description: Design or substantially redesign Omnilog UI while preserving its editorial, warm, cover-led visual language and avoiding generic AI-generated interface patterns. Use for new screens, major layout changes, hierarchy changes, Home redesign work, visual-system decisions, or requests framed as redesign/polish rather than a small Compose edit.
---

# UI Design

Use this skill when the task is primarily about visual hierarchy or screen composition.

## Load only what applies

- Always read `docs/current/design.md`.
- For Home, also read `docs/current/home.md`.
- For a feature-specific screen, read the corresponding file under `docs/features/`.
- Inspect the current implementation and `ui/theme`; do not design from documentation alone.

## Omnilog design rules

- Editorial, warm, restrained, mobile-first, and cover-led.
- Covers/artwork provide much of the color and identity.
- Use containers to support hierarchy, not to box every piece of information.
- Avoid generic AI UI patterns: repeated identical cards, excessive chips, gratuitous gradients, glassmorphism, decorative hero sections, oversized empty spacing, and redundant status badges.
- Different semantic sections should be allowed different compositions.
- Keep provider metadata visually distinct from user tracking state.
- Prefer typography, spacing, imagery, and information hierarchy before adding decorative chrome.
- Keep animations light and purposeful.

## Before implementation

Define the intended hierarchy in plain language first:

- primary user question/action;
- first viewport content;
- section order;
- what can be removed or merged;
- component form for each semantic section;
- key empty/loading/error states.

Do not add a new reusable component family until at least two real usages justify it.

## Typography and tokens

- Use existing semantic colors.
- Prefer Lato for dense UI, controls, metadata, body, and most section headings.
- Use Libre Baskerville selectively for brand/editorial emphasis where it improves the screen.
- Reuse existing spacing/radius patterns where appropriate, but do not force identical geometry across unlike content.

## Completion

After implementation, use the `visual-review` skill. A design task is incomplete until the rendered result has been inspected when tooling permits.