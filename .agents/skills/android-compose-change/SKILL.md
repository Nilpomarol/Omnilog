---
name: android-compose-change
description: Modify or add Jetpack Compose UI in Omnilog while preserving the app's current architecture, theme, accessibility, and local-first behavior. Use for Compose screens, components, layout, interaction, navigation UI, or state-bound UI changes that are not primarily a redesign exercise.
---

# Android Compose Change

Use this skill for focused Compose implementation work.

## Before editing

1. Read `AGENTS.md`.
2. Read only the relevant current product/design document under `docs/current/` or `docs/features/`.
3. Inspect the affected composable, its nearest reusable siblings, and `ui/theme` before creating new patterns.
4. Trace state ownership before moving state or introducing a new state holder.

## Implementation rules

- Make the smallest coherent change that satisfies the requested behavior.
- Reuse an existing component only when its semantics match; do not force visual reuse merely to reduce file count.
- Prefer Compose/Kotlin/platform capabilities and existing dependencies.
- Keep business and persistence logic out of composables.
- Keep transient UI state local when it is genuinely local; hoist state only when another owner needs it.
- Preserve existing navigation and back behavior unless the task explicitly changes it.
- Use Omnilog theme colors and typography. Do not hard-code arbitrary colors that duplicate theme roles.
- Preserve touch targets, readable contrast, content descriptions where needed, and large-text behavior.
- Do not refactor unrelated screens while touching a shared component.

## Validation

1. Run targeted Kotlin compilation first.
2. Run the nearest relevant unit/UI tests.
3. For a visual change, invoke the `visual-review` workflow before considering the task complete.
4. Escalate to broader tests/build only when the change surface justifies it.

If rendering is unavailable, state that the UI was not visually verified.