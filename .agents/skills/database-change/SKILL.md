---
name: database-change
description: Change Omnilog Room entities, DAO behavior, schema, migrations, persistence mapping, or backup compatibility. Use for any task that can alter stored data shape or migration behavior.
---

# Database Change

Database work must preserve existing user history and upgrade safety.

## Before editing

1. Read `AGENTS.md` and the relevant feature document.
2. Inspect the entity/DAO/mappers involved and the latest migrations under `core/database/migration`.
3. Identify backup/import compatibility implications before changing the schema.
4. Find the closest migration or repository test and follow its established pattern.

## Rules

- Preserve user-entered sessions, progress, ratings, reviews, ownership, collections, ordering, and provider identities unless the requested migration explicitly changes their meaning.
- Make schema meaning explicit. If a field changes semantics materially, prefer a rename/rebuild that makes stale readers fail rather than silently misinterpret data.
- Keep migrations deterministic and safe across real historical data, not only ideal fixtures.
- Do not derive historical facts from mutable timestamps when a trustworthy source is unavailable.
- Keep Room version increments and migration registration synchronized.
- Consider backup serialization/restoration whenever persisted domain shape changes.
- Do not introduce a generic persistence/service layer merely to contain one migration.

## Validation

At minimum:

1. compile affected code;
2. run focused migration/conversion tests;
3. run relevant repository/backup tests;
4. run broader unit tests when shared persistence semantics changed.

For a risky migration, verify against a representative old database or exported fixture when available.

Compilation alone is never sufficient evidence that a Room migration is correct.