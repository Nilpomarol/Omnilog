---
name: visual-review
description: Visually validate an Omnilog Compose change after implementation. Use whenever a task changes layout, typography, spacing, colors, component composition, empty/loading/error states, or other visible UI behavior.
---

# Visual Review

A successful compile is not visual validation.

## Workflow

1. Identify the smallest representative state affected by the change.
2. Prefer an existing deterministic `@Preview`; add a focused preview only when it provides lasting value.
3. Render the preview with Android tooling when available; otherwise run the affected screen on emulator/device.
4. Inspect both pixels and semantics/layout information when available.
5. Compare against the relevant requirement and Omnilog design rules, not merely against the previous implementation.
6. Fix visible issues before declaring completion.

## Check

- information hierarchy is obvious;
- first viewport contains the intended priority content;
- no accidental repeated-card/card-inside-card pattern;
- text does not clip or become ambiguous;
- long titles and missing metadata remain usable;
- touch targets and actions remain discoverable;
- empty/loading/error states still fit the composition;
- cover/artwork proportions are stable;
- spacing and alignment look intentional;
- media/status accents are used semantically;
- user tracking and provider metadata are visually distinguishable.

For layouts sensitive to text size, also inspect enlarged system text.

## Screenshot regression

If a reference screenshot test exists, run it after the visual iteration. Update reference images only when the visual change is intentional and reviewed; never update snapshots merely to make a failing test green.

## Reporting

State what was rendered/inspected. If no rendering path was available, explicitly say that visual verification was not performed.