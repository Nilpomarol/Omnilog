# Omnilog Documentation

Status: Current documentation index  
Last reviewed: 2026-09-12

This directory is organized by **authority**, not by when a document was written.

## Reading Order

For normal implementation work, use this order:

1. `../AGENTS.md` — repository-wide rules that apply to almost every task.
2. `current/` — current cross-cutting product, engineering, and design direction.
3. `features/` — current rules for a specific feature.
4. Source code and tests — authoritative for implementation details not specified by the docs above.
5. `audits/` — advisory reports; useful for improvement work but not product specifications.
6. `archive/` — historical context only. Do not use archived plans as current requirements.

If a current document and archived material disagree, the current document wins unless the task is explicitly investigating history.

## Current

- [`current/roadmap.md`](current/roadmap.md) — active product/engineering priorities and delivered foundation.
- [`current/design.md`](current/design.md) — current global UI/design principles.
- [`current/home.md`](current/home.md) — approved Home information architecture and social-aware direction.
- [`current/development-guide.md`](current/development-guide.md) — setup, build/test, credentials, and practical validation.

## Feature Specifications

- [`features/activity.md`](features/activity.md) — per-session activity semantics.
- [`features/timeline.md`](features/timeline.md) — library-wide chronology semantics.
- [`features/stats.md`](features/stats.md) — Stats definitions and constraints.
- [`features/imports.md`](features/imports.md) — provider import/enrichment behavior and safety rules.
- [`features/add-and-book-metadata.md`](features/add-and-book-metadata.md) — add flow and remaining book-edition work.

## Audits

- [`audits/ai-assisted-repository-audit.md`](audits/ai-assisted-repository-audit.md) — AI-assisted development audit and recommendations.
- [`audits/imports-audit.md`](audits/imports-audit.md) — import flow usability audit (September 2026).

## Archive

`archive/` contains superseded design documents, completed implementation plans, delivery records, and old roadmap snapshots.

Archive material is intentionally retained for reasoning/history, but agents should not read it by default.

## Document Maintenance

A current document should state its status and last-reviewed date near the top.

When a plan is fully delivered or superseded:

- extract any still-valid rules into `current/` or `features/`;
- move the detailed plan/delivery record into `archive/` if its reasoning is still useful;
- delete it instead when it contains no durable information beyond Git history;
- update this index and `AGENTS.md` if the authoritative map changes.

Do not keep two active documents that claim authority over the same behavior without an explicit precedence rule.
