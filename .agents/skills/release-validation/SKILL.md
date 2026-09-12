---
name: release-validation
description: Validate an Omnilog release candidate or release-critical change. Use for signed-release checks, production credential validation, upgrade testing, release QA, or final verification of import/synchronization behavior before distribution.
---

# Release Validation

Use this skill only for release-oriented work, not ordinary feature development.

## Before starting

1. Read `AGENTS.md`.
2. Read `docs/current/development-guide.md`.
3. Read the relevant feature document and any release-specific historical record only when needed.
4. Identify exactly which release-critical flows changed since the last validated build.

## Validate proportionally

Typical release checks include:

- signed release build succeeds and installs;
- application launches with release configuration;
- upgrade from a representative previous production database succeeds;
- provider credentials/redirect configuration work without secrets entering tracked files;
- backup restore remains compatible when persistence changed;
- import/enrichment preserves local history;
- MAL OAuth/token refresh/sync works when relevant;
- offline/reconnect recovery works when background/provider behavior changed;
- changed high-risk UI flows work on a real device or emulator;
- accessibility/large-text checks cover modified release-critical screens.

Do not rerun unrelated expensive manual scenarios when their code paths did not change and existing evidence remains applicable.

## Evidence

Record:

- exact build/commit tested;
- release/debug configuration used;
- automated tasks run;
- device/emulator used for manual QA;
- flows actually exercised;
- known limitations or skipped checks.

Never convert a previous dated successful verification into a claim that the current build has been tested.