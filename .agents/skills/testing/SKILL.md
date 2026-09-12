---
name: testing
description: Validate Omnilog changes efficiently with the smallest relevant Gradle/test scope first. Use when deciding what tests/build commands to run, adding focused tests, or investigating a failing validation step.
---

# Testing

Prefer targeted validation and concise output. Expand only when the risk surface justifies it.

## Default ladder

On Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain -q
.\gradlew.bat :app:testDebugUnitTest --tests "<fully.qualified.TestName>" --console=plain -q
.\gradlew.bat :app:testDebugUnitTest --console=plain -q
.\gradlew.bat :app:assembleDebug --console=plain -q
```

Use `./gradlew` on Unix-like systems.

Do not run every step mechanically. Pick the smallest set that proves the change.

If a quiet task fails:

1. rerun only that failing task without `-q`;
2. inspect the first actionable failure;
3. use `--stacktrace` only if the failure remains unclear.

## Test selection

Search for existing tests closest to the behavior before creating a new test style.

High-value areas include:

- imports and duplicate detection;
- metadata preservation/linking;
- migrations and repository persistence;
- backup/restore;
- Activity/Timeline derivation;
- Stats calculation;
- ratings;
- navigation/detail behavior.

Prefer behavior-oriented tests over implementation-detail assertions.

## UI

A passing unit test/build does not validate visual quality. For visible Compose changes, use the `visual-review` skill in addition to code tests.

## Reporting

Report exactly what was run and whether it passed. Never imply a broader test suite passed if only a focused test was executed.