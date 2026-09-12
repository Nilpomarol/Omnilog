# AI-Assisted Repository Audit & Optimization

**Repository:** `Nilpomarol/Content-tracking-android-app`  
**Project:** Omnilog  
**Audit date:** 12 September 2026  
**Status:** Audit report / advisory. Recommendations are not implemented merely by being documented here.  
**Focus:** Claude Code, Codex, Cursor, and similar agentic development workflows.

---

## 1. Executive Summary

Omnilog is already in considerably better shape for agent-assisted development than a typical organically grown AI-coded project. The architecture has real domain boundaries, provider implementations are separated, local-first invariants are explicit, the testing surface is non-trivial, and the UI already has an intentional visual identity.

Its biggest problem is **not low code quality or excessive AI-generated abstraction**.

Its biggest problem is **context topology**.

A fresh agent currently encounters:

- no concise cross-agent instruction entry point,
- a flat documentation directory where current specifications, completed plans, historical delivery records, and superseded decisions coexist,
- a roughly 153 KB application orchestration file,
- a roughly 136 KB offline repository implementation,
- a roughly 51 KB `HomeViewModel` that is effectively an application-wide ViewModel,
- a Home implementation that currently contradicts the latest approved Home design document,
- and no systematic rendered-UI feedback loop.

The consequence is that agents can understand the project, but often by reading substantially more than they should.

The highest-value changes are therefore:

1. introduce a **small `AGENTS.md` as the authoritative entry point**;
2. reorganize documentation into **current / feature / archive** knowledge;
3. split the three main context magnets by cohesive responsibility, without doing an architectural rewrite;
4. establish **Compose preview + screenshot/render review as part of UI completion**;
5. use **Android CLI and official Android agent skills immediately**;
6. benchmark **one**, not several, repository-intelligence systems;
7. use a lightweight simplicity skill or test Ponytail rather than introducing more architecture.

I would **not** introduce Hilt, a new clean-architecture layer, multi-module decomposition, Sourcegraph, several overlapping MCP servers, or a large generic agent framework.

---

## 2. Repository Baseline

The project is a native Kotlin Android application using Jetpack Compose, Room, Navigation 3, WorkManager, Coil, KSP and Gradle.

The root build currently uses:

- Android Gradle Plugin 9.2.1
- Kotlin 2.2.10

The app module currently includes, among other dependencies:

- Compose UI 1.11.2
- Material 3 1.4.0
- Navigation 3 1.1.4
- Room 2.8.4
- WorkManager 2.11.2
- Coil 3.2.0

Compose preview/tooling dependencies are already present.

The main architectural areas are already recognizable:

```text
core/
    backup/
    cover/
    database/
    imports/
    mal/
    model/
    refresh/
    repository/
    stats/
    timeline/

ui/
    add/
    common/
    detail/
    home/
    imports/
    navigation/
    profile/
    settings/
    stats/
    theme/
    timeline/
```

This feature/domain-oriented structure is a strength. The problem is concentrated in a few files rather than systemic disorder.

The README also defines several important invariants correctly:

- Room is authoritative.
- Provider data enriches rather than owns the user's tracking data.
- Imports are additive.
- Metadata work must preserve user history.
- Secrets must remain untracked.

---

## 3. Scores

| Dimension | Score | Assessment |
|---|---:|---|
| **Agent readiness** | **5.5 / 10** | Good README, domain organization and invariants, but no `AGENTS.md`/`CLAUDE.md`, ambiguous documentation status and several huge orchestration surfaces. |
| **Context/token efficiency** | **4.5 / 10** | Feature folders and focused tests help, but several routine tasks lead into 50–150 KB files and multiple overlapping docs. |
| **Resistance to generic AI UI** | **6.5 / 10** | Strong written design principles, deliberate palette and fonts, but the newest design direction is not reflected in Home yet and there is no systematic rendered visual review. |

With the changes in this audit, the first two scores should be able to move into roughly the **8/10 range without fundamentally changing the product architecture**.

---

# 4. Important Findings

## Finding: No authoritative cross-agent entry point

### Problem

There is no `AGENTS.md` or equivalent concise repository-level source of truth for a fresh coding agent.

The repository instead relies on the README, development guide, roadmap, feature plans and Claude-specific local permissions.

That means a fresh agent has to infer which rules are fundamental and which are contextual.

### Evidence

The root contains the app, docs, tools and `.claude/`, but no `AGENTS.md`, `CLAUDE.md`, `.codex/` or `.cursor/` entry point was found during the audit.

Relevant files include:

- `README.md`
- `docs/development-guide.md`
- `docs/omnilog-roadmap.md`
- `.claude/settings.local.json`

The development guide contains valuable architectural rules, build instructions and important-file mappings, but much of it duplicates the README.

Meanwhile `.claude/settings.local.json` contains local Windows paths, a project path, Claude temporary scratchpad paths and machine-specific command permissions.

### Impact

- repeated architecture rediscovery;
- agents load README + development guide + roadmap before simple work;
- behavior differs across Claude/Codex/Cursor;
- local Claude configuration leaks environment-specific noise into repository context.

### Recommendation

Create a short root `AGENTS.md` containing only stable, nearly universal rules.

Add a very small `CLAUDE.md` only if Claude-specific behavior is genuinely required; otherwise it should simply redirect to the canonical project instructions rather than repeat them.

Untrack:

```text
.claude/settings.local.json
```

and add it to `.gitignore`.

If shared Claude permissions are useful, put only portable settings in:

```text
.claude/settings.json
```

Do **not** move all documentation into `AGENTS.md`.

### Primary benefit

**MULTIPLE**

### Priority

**Critical**

---

## Finding: Documentation has a status problem, not merely a size problem

### Problem

The `docs/` directory mixes:

- authoritative current design,
- proposed future design,
- completed implementation plans,
- delivery records,
- release QA records,
- original system plans,
- superseded UI direction.

The agent therefore has to reason about document chronology before reasoning about the task.

### Evidence

Examples from the flat `docs/` directory include:

- `omnilog-product-ux-backlog.md` — roughly 46.6 KB
- `omnilog-import-enrichment-implementation-plan.md` — roughly 37.7 KB
- `omnilog-roadmap.md` — roughly 26.5 KB
- `omnilog-stats-improvement-plan.md` — roughly 18.2 KB
- `omnilog-activity-concept.md` — roughly 17.5 KB
- `omnilog-activity-implementation-plan.md` — roughly 14.9 KB
- `omnilog-home-social-aware-redesign.md` — roughly 12.9 KB

The product/UX backlog is mostly a record of completed `[x]` items, including detailed verification and historical debugging information.

The roadmap still contains a statement that the build was healthy **as of 22 July 2026**. That is useful history, but it should not be interpreted as a current build verification.

Most importantly, `docs/omnilog-home-social-aware-redesign.md` explicitly says that it supersedes the older Home-specific guidance in `docs/omnilog-ui-design-v1.md` wherever they disagree.

### Impact

A UI agent might read:

```text
ui-design-v1
+
product backlog
+
roadmap
+
Home redesign
```

just to answer:

> How should the Planned section look?

That is exactly the kind of unnecessary reasoning that progressive disclosure should eliminate.

### Recommendation

Reorganize by authority rather than chronology.

Every retained current document should begin with metadata such as:

```text
Status: Current
Authoritative for: Home information architecture
Supersedes: <document/section>
Last verified: 2026-09-xx
```

Completed implementation plans should move into `docs/archive/`.

Delivery history remains valuable; it simply must stop presenting itself beside active specifications.

### Primary benefit

**TOKENS**

### Priority

**High**

---

## Finding: `ContentTrackerApp.kt` is an application orchestration sink

### Problem

The top-level Compose file has accumulated navigation, state collection, import workflows, backup workflows, MAL flows, metadata linking, confirmation states, top bars and numerous dialogs.

It now costs too much context for changes that should only concern navigation or application shell behavior.

### Evidence

`app/src/main/java/com/nilpo/contenttracker/ui/ContentTrackerApp.kt` is approximately **153 KB**.

At the beginning of `ContentTrackerApp`, it collects:

- main UI state,
- timeline state,
- contributor state,
- metadata state,
- recommendation state,
- MAL state,
- import-enrichment state,
- metadata-refresh state.

It then locally owns state for backup restore, several provider imports, MAL confirmation, metadata linking, metadata change confirmation, bulk actions, duplicate handling and detail/header actions.

### Impact

A task such as:

> adjust back navigation from detail

can drag the agent through unrelated import and metadata flows.

Conversely:

> improve MAL import confirmation

requires opening a file containing nearly the entire app shell.

This increases:

- files/tokens read,
- accidental collateral edits,
- merge conflicts,
- incentive for the agent to “clean up” unrelated code.

### Recommendation

Split **by existing responsibility**, not by arbitrary line count.

`ContentTrackerApp.kt` should primarily retain:

- root Scaffold,
- root navigation/back stack,
- route selection,
- truly global UI hosts.

Move cohesive flows into existing feature packages, preferably as composable hosts/state holders rather than immediately inventing new manager classes.

For example:

```text
ui/navigation/
    AppNavHost.kt
    AppScaffold.kt

ui/imports/
    ImportFlowHost.kt
    ImportDialogs.kt

ui/metadata/
    MetadataLinkHost.kt
    MetadataRefreshHost.kt
```

Only introduce a dedicated coordinator/ViewModel if the extracted state genuinely needs one.

Do not create a generic “application orchestration framework”.

### Primary benefit

**MULTIPLE**

### Priority

**High**

---

## Finding: `HomeViewModel` is not really a Home ViewModel anymore

### Problem

The class has become an application-level façade.

### Evidence

`app/src/main/java/com/nilpo/contenttracker/ui/home/HomeViewModel.kt` is approximately **51 KB**.

It directly coordinates:

- media repository operations,
- metadata search,
- recommendation search,
- cover management,
- MAL OAuth and synchronization,
- import enrichment,
- metadata refresh,
- backup import/export,
- IMDb imports,
- StoryGraph imports,
- MAL XML/account imports,
- timeline derivation,
- contributor-directory derivation,
- Home filters and sorting.

### Impact

An agent modifying one Home filter sees unrelated synchronization and import logic.

The name also gives a fresh agent the wrong architectural model: it looks like a screen ViewModel but is effectively central application state.

### Recommendation

Refactor incrementally.

First remove operations that already map to clearly separate product surfaces:

- import/backup state → import/settings state holder;
- MAL account synchronization → settings/sync state holder;
- metadata linking/refresh lifecycle → metadata/detail state holder where useful.

Keep Home/library filtering and Home-derived state in `HomeViewModel`.

Do **not** split it into six ViewModels merely because six categories can be named.

The boundary should follow what changes together.

### Primary benefit

**TOKENS**

### Priority

**High**

---

## Finding: `OfflineMediaRepository` has too many cohesive responsibilities in one implementation

### Problem

`OfflineMediaRepository` has become the implementation location for much more than normal local media persistence.

### Evidence

`app/src/main/java/com/nilpo/contenttracker/core/repository/OfflineMediaRepository.kt` is approximately **136 KB**.

Its opening sections already contain:

- tracked-media observation,
- objective CRUD,
- complete JSON backup export,
- backup preview,
- destructive restore,
- IMDb planning/import,
- imported-session persistence.

`docs/development-guide.md` additionally identifies the same implementation as owning imports, metadata refresh/linking and backup serialization.

By contrast, provider-specific metadata repositories are already reasonably well isolated.

### Impact

A change to backup serialization and a change to session mutation both tend to load the same enormous implementation file.

That is poor context locality even if the runtime architecture is valid.

### Recommendation

Keep the public repository behavior stable initially.

Extract implementation details, for example:

```text
core/repository/
    OfflineMediaRepository.kt

core/backup/
    BackupCodec.kt
    BackupRestorer.kt

core/imports/
    ProviderImportPlanner.kt
    ProviderImportPersistence.kt

core/metadata/
    MetadataRefreshApplier.kt
```

The important point is that an agent modifying backup code should no longer need the implementation of every session operation.

Do **not** immediately split `MediaRepository` into ten interfaces or introduce a service layer everywhere.

### Primary benefit

**MULTIPLE**

### Priority

**High**

---

## Finding: Application composition and migration history are mixed

### Problem

`ContentTrackerApplication.kt` acts correctly as a simple dependency-composition root, but it also contains historical Room migrations.

### Evidence

`app/src/main/java/com/nilpo/contenttracker/ContentTrackerApplication.kt` constructs:

- Room,
- Coil,
- `OfflineMediaRepository`,
- provider repositories,
- recommendation repositories,
- MAL manager,
- import-enrichment manager,
- metadata-refresh manager.

It also contains inline migration implementations while newer migrations are imported from `core/database/migration`.

### Impact

Agents changing startup dependency wiring also encounter schema-history implementation.

This is unnecessary context coupling.

### Recommendation

Move all migration declarations into:

```text
core/database/migration/
```

and have `ContentTrackerApplication` only register them.

Importantly, **keep the manual dependency wiring**.

Do not add Hilt/Dagger here merely to make the dependency graph more fashionable. The current manual graph is easy for a human and an agent to follow.

### Primary benefit

**MAINTAINABILITY**

### Priority

**Medium**

---

## Finding: Approved Home design and current Home implementation disagree

### Problem

This is currently the most important UI problem because it also damages agent reliability.

An agent can faithfully follow the newest design document and still find existing implementation patterns telling it to do the opposite.

### Evidence

The approved `docs/omnilog-home-social-aware-redesign.md` says:

- `Ara mateix` → dedicated horizontal Continue cards;
- `Per començar` → **do not reuse the same poster component**;
- objectives + yearly analytics → merge into `El teu ritme`;
- recent activity → one compact entry;
- paused → smaller cover shelf;
- `Completats recentment` → remove from Home;
- avoid repeating identical card geometry.

Current `HomeLandingScreen.kt` still renders a structure equivalent to:

```text
HomeActiveCarousel
HomeCarousel(planned)
DashboardObjectivesPreview
DashboardAnalyticsPreview
TimelineRecentActivity
HomeCarousel(paused)
HomeCarousel(completed)
```

### Impact

- visual convergence persists;
- agents see reusable `HomeCarousel` and naturally reuse it again;
- documentation and implementation give conflicting priors;
- future social additions are more likely to inherit the wrong Home structure.

### Recommendation

Treat `docs/omnilog-home-social-aware-redesign.md` as the active specification and implement it in small slices as it already recommends.

After implementation, move the superseded Home sections of older UI documentation to history or make the precedence extremely obvious.

This does **not** require a general UI rewrite.

### Primary benefit

**UI QUALITY**

### Priority

**Critical**

---

## Finding: The visual grammar is coherent but too container-led

### Problem

The design is not generic Material out of the box, but much of its hierarchy is still expressed using the same family of surfaces.

### Evidence

A representative Home module uses a pattern equivalent to:

```text
Surface
+ fillMaxWidth
+ RoundedCornerShape(8.dp)
+ appPanel
+ 1dp appLine border
+ bold/extra-bold heading
+ muted secondary text
```

The search field is another bordered panel/pill, and the search overlay is another bordered `Surface`.

At the same time, `docs/omnilog-ui-design-v1.md` explicitly states that the application should not become a stack of boxed panels.

### Impact

Semantically different things visually converge:

```text
statistics ≈ objectives ≈ search ≈ general content modules
```

The problem is not that `Surface` or cards exist. It is that containers often become the primary expression of hierarchy.

### Recommendation

Use semantic composition, not more card variants.

The intended Home grammar should be:

```text
currently consuming
→ continuation card

planned
→ compact editorial row/card

rhythm / objectives / stats
→ typography + compact chart-led module

activity
→ chronology row

paused
→ cover shelf

detail metadata
→ cover-led editorial composition
```

Avoid solving this by creating `CardStyle1` through `CardStyle8`.

### Primary benefit

**UI QUALITY**

### Priority

**High**

---

## Finding: Omnilog has a display font but barely gets identity from it

### Problem

A distinctive display font is packaged but the theme assigns the body family to every Material typography role.

### Evidence

`app/src/main/java/com/nilpo/contenttracker/ui/theme/Theme.kt` defines:

- `DisplayFontFamily` → Libre Baskerville;
- `BodyFontFamily` → Lato.

But `OmnilogTypography` uses `BodyFontFamily` for display, headline, title, body and label roles.

The same file otherwise has a strong custom visual foundation: warm light/dark palettes, theme-adjusted accent colors and comments documenting contrast reasoning.

### Impact

Product identity is forced to come primarily from:

- color,
- covers,
- containers,
- font weight.

That increases the risk that agents reach for additional panels, chips and decorative geometry when trying to make a screen feel designed.

### Recommendation

Use Libre Baskerville selectively, for example:

- Omnilog brand/title;
- major page display headings where appropriate;
- media hero titles or occasional editorial feature titles.

Keep Lato for:

- controls,
- metadata,
- body copy,
- dense section headings,
- lists.

Do not simply change all headings to serif.

A screenshot comparison should decide the final roles.

### Primary benefit

**UI QUALITY**

### Priority

**Medium**

---

## Finding: Design tokens are strong for color but weak for spatial semantics

### Problem

Colors are centralized semantically, while spacing, shapes and layout rhythm are mostly local numeric decisions.

### Evidence

`Theme.kt` gives the application semantic neutral palettes and section/status accents.

Representative Home code directly contains values such as:

- 14 dp spacing/padding,
- 10 dp,
- 8 dp,
- 7 dp,
- 5 dp,
- 8 dp corners,
- 999 dp pill corners.

### Impact

Agents can preserve colors easily but independently reinvent density and shape patterns.

### Recommendation

Add only a **small** spatial vocabulary, for example:

```text
screen horizontal inset
section gap
content gap
compact gap

cover small / medium radius
panel radius
control radius
```

Do not create a giant design-token framework.

And do not require every component to use the same radius merely because a token exists.

### Primary benefit

**UI QUALITY**

### Priority

**Medium**

---

## Finding: UI completion is build-oriented rather than render-oriented

### Problem

The repository has good support for compiling and installing the app, but no repository-level visual validation workflow.

### Evidence

Compose tooling dependencies exist in `app/build.gradle.kts`.

The Claude local permissions already permit operations such as:

- targeted Kotlin compilation,
- debug assembly,
- unit tests,
- install debug,
- `adb shell`,
- `adb exec-out`.

During the audit, repository search found no `@Preview` usage and no Figma integration, and no screenshot-testing dependency/configuration was found.

### Impact

An AI agent can declare:

> build passes

while producing a visually weak or structurally wrong screen.

For an application whose design documents explicitly care about information hierarchy, visual convergence and typography, compilation is insufficient.

### Recommendation

Make rendered validation mandatory for visual changes.

Start with deterministic previews for a few high-value states, not every composable.

Then add screenshot comparison for those states.

A proposed workflow appears later in this document.

### Primary benefit

**MULTIPLE**

### Priority

**High**

---

## Finding: Build/test commands already support a more token-efficient ladder

### Problem

Documentation primarily advertises broad build/test commands even though Claude permissions show that targeted commands are already used in practice.

### Evidence

README and development guide document commands such as:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

`.claude/settings.local.json` already permits targeted variants such as:

```text
:app:compileDebugKotlin --console=plain
:app:compileDebugKotlin -q
:app:testDebugUnitTest -q
```

### Impact

The workflow can waste both execution time and model context by emitting broad Gradle output before it is necessary.

### Recommendation

Encode this escalation ladder in a testing skill:

```text
1. :app:compileDebugKotlin --console=plain -q

2. focused unit test:
   :app:testDebugUnitTest --tests "<test>" --console=plain -q

3. :app:testDebugUnitTest --console=plain -q

4. :app:assembleDebug --console=plain -q

5. if failure:
   rerun only the failed task without -q

6. only then:
   --stacktrace / detailed diagnostics
```

Database work should run the relevant migration/instrumentation test before the whole suite.

UI work should render before spending time on broad regression execution.

Correctness still wins over token savings.

### Primary benefit

**TOKENS**

### Priority

**Medium**

---

## Finding: Anti-overengineering protection is useful, but Omnilog is not currently abstraction-bloated

### Problem

The repository should be protected from future agent overengineering, but that does not justify a large refactor or aggressive simplification tool by itself.

### Evidence

There are managers such as:

- `ImportEnrichmentManager`,
- `MetadataRefreshManager`,
- `MalSyncManager`.

But they correspond to persistent/background workflows built on WorkManager rather than a generic pattern of pointless manager classes.

Likewise provider repositories are separated by real external APIs rather than artificial layering.

### Impact

An overly aggressive anti-complexity agent could make the architecture worse by collapsing useful boundaries.

### Recommendation

Use a simplicity gate before adding new architecture:

```text
1. Is the new thing necessary?
2. Does an equivalent already exist?
3. Can Kotlin/Android/Compose provide it?
4. Can an existing dependency provide it?
5. Can an existing Omnilog component be adapted?
6. Can the change remain local?
7. Only then introduce a new abstraction/dependency.
```

This belongs in a task skill.

Ponytail is worth benchmarking, but it should not be made mandatory globally without evidence from this repository.

### Primary benefit

**MAINTAINABILITY**

### Priority

**Medium**

---

# 5. Comments Audit

The code comments are generally **better than average for an AI-developed repository**.

Several comments should explicitly be protected.

Examples in `HomeLandingScreen.kt` explain:

- why Settings owns the `Ara mateix` filter;
- why paused titles are ordered longest-stalled first;
- why missing completion dates should not fall back to `updatedAt`.

Repository comments also explain behavioral invariants around deleting current versus historical sessions and the semantics of deleting a status transition.

These are exactly the comments an agent benefits from.

The expensive historical material is much more concentrated in planning/delivery documents than inline code.

**Recommendation:** archive historical documents; do not start deleting explanatory code comments in a token-reduction exercise.

---

# 6. Proposed Agent Instruction Architecture

The desired hierarchy should be:

```text
AGENTS.md
    ↓
nearly universal repository rules

task skills
    ↓
procedure for this kind of change

docs/current + docs/features
    ↓
domain/product knowledge required by task

docs/archive
    ↓
history, only when explicitly investigating history
```

## `AGENTS.md`

Keep it approximately 100–150 lines, not a second development manual.

It should contain:

- Omnilog in one paragraph;
- Kotlin/Compose/Room stack;
- local-first invariants;
- user data preservation rules;
- secrets rules;
- architecture map;
- source-of-truth documentation map;
- manual dependency wiring rule;
- minimal-change rule;
- build/test escalation ladder;
- UI visual-verification requirement;
- rule that archive docs are non-authoritative;
- rule not to touch unrelated dirty files;
- rule to ask before introducing major frameworks/dependencies.

It should **not** contain:

- complete feature specs;
- provider APIs;
- migration procedures;
- full design system;
- old implementation history;
- release logs.

---

# 7. Recommended Project Skills

| Skill | Trigger | Contains | Replaces/references | Context avoided |
|---|---|---|---|---|
| **android-compose-change** | Any Compose screen/component modification | Compose conventions, state boundaries, theme usage, accessibility, compile command | `Theme.kt`, current design doc | Agent does not need full dev guide + unrelated UI docs |
| **ui-design** | New screen, redesign, visual restructuring | Omnilog visual grammar, semantic-layout rules, typography, anti-card-convergence checklist | `docs/current/design.md`, feature UI spec | Prevents loading all historical design/backlog material |
| **visual-review** | Any visual UI change before completion | Preview/render/install/capture/compare procedure | Android CLI + screenshot tooling | Stops broad manual/emulator exploration |
| **database-change** | Entity, DAO, schema or persistence change | migration rules, schema export, migration tests, backup compatibility | database docs and migrations | Avoids reading import/UI documentation |
| **provider-import** | Metadata providers, CSV/XML imports, enrichment | additive semantics, duplicate rules, preservation invariants, targeted tests | consolidated imports feature doc | Avoids loading historical implementation plans |
| **testing** | Validation after implementation | targeted command ladder, quiet output, escalation rules, screenshot tests | development-guide testing sections | Avoids rediscovering commands |
| **release-validation** | Release candidate / import release QA | signing/checklist/device flow | release-readiness doc | Release history stays out of ordinary development |

A permanent generic `architecture-review` skill is not necessary. Large architectural investigations are better performed by a temporary subagent or repository-intelligence tool.

Likewise, a separate `migration` skill would overlap too heavily with `database-change`.

---

# 8. Subagent Strategy

Subagents are useful here primarily as **context isolation**, not as token-saving magic.

## Good use: architecture scout

For questions such as:

> Where does metadata refresh cross database/UI boundaries?

Return only:

```text
relevant file:symbol
dependency path
important invariant
suggested edit boundary
```

Do not dump source.

## Good use: visual reviewer

After implementation, provide the rendered screenshot and requirements to a separate reviewer that has **not** seen the implementation rationale. It can detect hierarchy problems without being anchored to the code.

## Good use: migration reviewer

For a schema change, isolate the migration/schema/backup compatibility analysis from the main implementation conversation.

## Bad use

Do not spawn a research subagent for:

- changing one string,
- adjusting one Compose padding,
- adding one isolated test,
- a local bug with a clear stack trace.

Subagents can increase total token usage even when they keep the main context cleaner.

---

# 9. Recommended UI Feedback Loop

For Omnilog, make this the standard UI procedure:

```text
read requirement
        ↓
read only current relevant design spec
        ↓
inspect sibling implementation + Theme.kt
        ↓
choose semantic composition
        ↓
implement
        ↓
targeted Kotlin compile
        ↓
render Compose preview
        ↓
capture screenshot + semantics
        ↓
visually review
        ↓
iterate if needed
        ↓
screenshot validation
        ↓
targeted tests
        ↓
full regression only when appropriate
```

## Practical implementation

Google's current Android CLI is particularly well matched to this repository.

It can expose Android Studio-backed operations including symbol navigation and Compose preview rendering. Current tooling includes preview rendering and can output the rendered image plus semantics; it also supports device screen capture and layout inspection.

The CLI also supports differential layout inspection, so an agent can request changed UI information rather than repeatedly ingesting a complete hierarchy.

Start by making previews for high-value states such as:

```text
Home:
- populated normal state
- empty state
- long title
- 200% text-oriented fixture

Detail:
- in progress
- completed
- long metadata

Important dialogs:
- import preview
- metadata overwrite
```

Use small deterministic fake fixtures; do not create a giant preview-data architecture.

---

# 10. Screenshot Testing

The official Jetpack Compose Preview Screenshot Testing tooling is a good technical fit for Omnilog.

Omnilog already meets the modern AGP/Kotlin requirements needed by current Android tooling.

The principal caveat is maturity: screenshot tooling may still evolve quickly, so adoption should begin narrowly.

### Recommendation

Adopt it first for:

- Home continuation/planned/rhythm modules;
- item-detail hero;
- one or two critical dialogs.

Do not snapshot every component.

Screenshots should protect **visual contracts**, not freeze every pixel of the app.

If the official solution proves too limited or unstable, benchmark Roborazzi rather than immediately running both systems permanently.

---

# 11. Current Visual Grammar

The current grammar can roughly be expressed as:

```text
warm dark/light base
+
warm panel surface
+
thin line border
+
moderate rounded rectangle
+
heavy Lato heading
+
muted Lato secondary text
+
media/status accent
+
cover artwork
```

It is coherent and already more distinctive than default Material.

Its weak point is that **container geometry does too much of the semantic work**.

The approved Home redesign is directionally correct because it introduces semantic diversity without abandoning consistency.

The correct distinction is:

```text
consistency
≠
every concept uses the same component geometry
```

---

# 12. Typography Direction

A restrained role assignment would be:

| Role | Suggested family |
|---|---|
| Omnilog brand | Libre Baskerville |
| Major editorial/page display | Libre Baskerville selectively |
| Media hero title | Libre Baskerville where it visually works |
| Section heading | Lato Semibold/Bold |
| Body | Lato Regular |
| Metadata | Lato Regular |
| Buttons / compact controls | Lato Semibold |
| Labels | Lato Regular/Semibold |

The important change is not “use more serif”.

It is giving the product a typographic hierarchy that does not require another panel or badge to create personality.

---

# 13. Documentation Target Structure

A useful target would be:

```text
repo/
├── AGENTS.md
├── CLAUDE.md                  # tiny, only if needed
│
├── .agents/
│   └── skills/
│       ├── android-compose-change/
│       │   └── SKILL.md
│       ├── ui-design/
│       │   └── SKILL.md
│       ├── visual-review/
│       │   └── SKILL.md
│       ├── database-change/
│       │   └── SKILL.md
│       ├── provider-import/
│       │   └── SKILL.md
│       ├── testing/
│       │   └── SKILL.md
│       └── release-validation/
│           └── SKILL.md
│
├── docs/
│   ├── current/
│   │   ├── architecture.md
│   │   ├── product.md
│   │   ├── design.md
│   │   └── home.md
│   │
│   ├── features/
│   │   ├── activity.md
│   │   ├── stats.md
│   │   ├── timeline.md
│   │   └── imports.md
│   │
│   ├── release/
│   │   └── import-enrichment-readiness.md
│   │
│   └── archive/
│       ├── plans/
│       └── delivery/
│
├── app/
└── tools/
```

The exact folder used for portable skills may need an adapter for each harness. Keep **one canonical copy** of each skill; do not maintain independently divergent Claude/Codex copies.

---

# 14. What Should Move Where

## Always-loaded

`AGENTS.md`:

- architecture in roughly 15 lines;
- local-first invariants;
- preservation rules;
- build/test ladder;
- source-of-truth hierarchy;
- minimal-change rule;
- UI visual-review rule.

## On-demand skill

Procedural knowledge such as:

- how to change Room;
- how to visually validate Compose;
- how provider imports work;
- how to validate a release.

## Current docs

Product/domain truth:

- current Home IA;
- current design language;
- social architecture;
- stats semantics;
- activity semantics.

## Archive

- completed UX backlog;
- delivered implementation plans;
- superseded Stats refinement;
- old implementation sequences;
- old QA/delivery narratives.

Nothing needs to be deleted.

---

# 15. MCP Strategy

Omnilog does **not** currently need a broad MCP stack.

MCP should only enter where it provides a capability that local agent tools cannot supply economically.

## What does not need MCP

### Repository files

Claude/Codex/Cursor can already inspect them.

### Git

Native shell/Git is sufficient.

### Room

No database MCP is justified.

### Gradle

Native commands are better.

### Android emulator

Android CLI/ADB are a more direct fit.

### Project instructions

Docs/skills solve this better than a server.

## Where MCP could help

### Figma

Only if Figma becomes a real design source of truth.

### Large semantic repository intelligence

Possibly, but only after native Android CLI symbol operations prove insufficient.

---

# 16. External Tool Research

## Tool Recommendation Table

| Tool | Problem solved | Fit for Omnilog | Expected benefit | Cost / overhead | Verdict |
|---|---|---|---|---|---|
| **Android CLI** | Android semantic navigation, build/run, layout inspection, screenshot/preview rendering | Excellent | Less shell spelunking, rendered feedback, semantic UI inspection | New CLI workflow, but low conceptual overhead | **Strongly recommended** |
| **Official Android agent skills** | Reusable Android procedures | Excellent if installed selectively | Framework-correct workflows without custom prompt repetition | Loading too many skills would add noise | **Strongly recommended selectively** |
| **Compose Preview Screenshot Testing** | Visual regression from Compose previews | Excellent technically | Prevents compile-only UI completion | Tooling still evolving; reference image maintenance | **Strongly recommended as a pilot** |
| **Graphify** | Persistent structural code graph | Good; Kotlin supported | Cross-file blast-radius queries, less grep | Index/tool overhead; Kotlin edge cases | **Worth testing** |
| **JetBrains Context** | Semantic repository retrieval/index | Good, particularly Android Studio users | Potentially fewer exploration turns | Cloud semantic index, subscription/internet, vendor dependency | **Worth testing** |
| **Serena** | Symbol-level retrieval/editing via language servers | Conceptually excellent | Small symbol context instead of whole files | Kotlin language-server support is less mature | **Worth testing only as fallback** |
| **Ponytail** | Scope/minimal-change/anti-overengineering guardrail | Moderate-good | Smaller diffs and fewer speculative abstractions | Hook/plugin overhead; can be over-restrictive | **Worth testing** |
| **Roborazzi** | Mature Compose/JVM screenshot regression | Good | Strong screenshot reports/UI-tree support | Robolectric + plugin/dependency overhead | **Worth testing if official screenshots fall short** |
| **Figma MCP** | Gives agents authoritative design data | Conditional | Better major redesign/design-system fidelity | MCP context, Figma workflow, possible plan requirements | **Optional / later** |
| **JetBrains IDE MCP** | IDE semantic tools to agents | Moderate | IDE code analysis | Overlaps Android CLI; schema/tool context | **Not useful yet** |
| **Maestro** | Device E2E flows + screenshot assertions | Moderate | Repeatable end-to-end UX flows | Additional YAML/runtime/tool layer | **Not useful yet** |
| **Sourcegraph MCP** | Cross-repository semantic search/analysis | Poor for current scale | Strong at multi-repo enterprise scale | Enterprise infrastructure/subscription, remote platform | **Not useful yet** |
| Generic “Clean Android” agent skills | Generates architecture/scaffolding | Poor | Fast scaffolding | High risk of Hilt/use-case/multi-module overengineering | **Avoid** |

---

# 17. Android CLI

This is the clearest external-tool recommendation.

Google now provides Android CLI and agent-oriented Android tooling capable of running applications, inspecting UI, capturing screens and accessing Android Studio-backed semantic tooling.

For Omnilog specifically it addresses several problems simultaneously:

```text
huge Kotlin files
→ symbol/find-usage operations

Compose UI
→ render previews

visual validation
→ screenshot capture

agent context
→ layout diff rather than full hierarchy
```

This is preferable to introducing an Android-specific MCP server unless that MCP exposes something materially better.

### Verdict

**Install now.**

Reference: https://developer.android.com/tools/agents/android-cli

---

# 18. Graphify

Graphify is a particularly interesting candidate because Omnilog's issue is not “can't search text”; it is reconstructing cross-file structure repeatedly.

Graphify builds a local structural graph and supports Kotlin/KTS among its languages.

That could help questions such as:

> What touches metadata refresh?

> Which paths reach `OfflineMediaRepository`?

> What is the blast radius of moving this state?

However, Kotlin support should be tested on this repository before treating it as authoritative. Tool/vendor token-efficiency claims should not be treated as proof that it saves tokens on Omnilog.

### Verdict

**Worth testing**, not automatic adoption.

References:

- https://github.com/Graphify-Labs/graphify
- https://graphify.com/

---

# 19. JetBrains Context

JetBrains Context is a strong conceptual fit because Omnilog is Kotlin/Android and likely developed in Android Studio.

It uses an incremental semantic index and supports Kotlin and common coding-agent workflows.

Its potential advantage is fewer repository-exploration turns and stronger semantic retrieval than plain grep.

Its downsides are cloud/service dependency, possible subscription requirements, and vendor coupling. Any vendor benchmark should be treated as a vendor benchmark until reproduced on Omnilog.

### Verdict

**A/B test against Graphify.**

Do not permanently use both unless they prove distinct enough to justify the overlap.

Reference: https://www.jetbrains.com/context/

---

# 20. Serena

Serena's philosophy matches this audit closely: retrieve symbols, declarations and references rather than entire files.

This is especially attractive around files like `ContentTrackerApp.kt`.

The concern is Kotlin stack maturity. If its Kotlin language-server path produces incomplete or inaccurate symbol relationships, it may be worse than targeted native tooling.

### Verdict

**Worth testing only if Android CLI + Graphify/JetBrains Context do not solve retrieval adequately.**

Do not install Serena alongside two other repository-index systems permanently.

References:

- https://oraios.github.io/serena/01-about/035_tools.html
- https://github.com/oraios/serena

---

# 21. Ponytail

Ponytail targets a real agent problem:

> agents often solve the requested task plus several imagined future tasks.

Its rule set emphasizes YAGNI, using stdlib/platform capabilities first, reusing existing dependencies and minimizing the diff.

Omnilog, however, does not currently exhibit catastrophic over-abstraction.

Its large files are actually evidence of **under-separated responsibilities**, not excessive layers.

### Verdict

Test Ponytail in a lighter/review configuration on representative tasks.

Compare it against the repository-local seven-question simplicity skill.

If the custom skill produces comparable diffs, prefer the custom skill because it has:

- zero external dependency;
- project-specific constraints;
- almost no tool/context overhead.

Reference: https://github.com/DietrichGebert/ponytail

---

# 22. Figma MCP

Figma's MCP integration can expose design context to supported coding agents and can be useful around design/code handoff.

The missing condition in Omnilog is simple:

**there is currently no Figma source of truth in the repository workflow.**

Installing Figma MCP cannot improve fidelity to a design that does not exist there.

### Recommendation

Use it when:

- redesigning Home materially;
- establishing reusable screen compositions;
- formalizing a design system;
- working from an approved Figma reference.

Do not require Figma for:

- a copy change;
- padding adjustment;
- minor state addition;
- straightforward bug fix.

### Verdict

**Optional / later.**

References:

- https://developers.figma.com/docs/figma-mcp-server/
- https://github.com/figma/code-connect

---

# 23. Visual QA Alternatives

## Official screenshot testing vs Roborazzi

Roborazzi is mature and provides record/compare/verify workflows for Android and Compose, including useful reports.

But Omnilog should first try Google's official Compose screenshot solution.

Recommended order:

```text
try official screenshot testing first
        ↓
if unstable or missing useful functionality
        ↓
benchmark Roborazzi
```

Do not install both permanently unless they serve clearly different layers.

Reference: https://github.com/takahirom/roborazzi

## Android tooling vs Maestro

Maestro remains a capable E2E product and supports screenshots/assertions.

For an Android-only project, however, another test language is not yet justified if Android-native tooling can cover the necessary flows.

Reference: https://docs.maestro.dev/

---

# 24. Tool Overlap

There are two major overlap clusters.

## Repository intelligence

```text
Android CLI semantic tools
Graphify
JetBrains Context
Serena
Sourcegraph
```

Do not run all five.

Use:

```text
Android CLI
    ↓
baseline / local semantic navigation

+

one persistent repo intelligence winner
    ↓
only if benchmark proves benefit
```

Recommended evaluation order:

```text
Graphify ↔ JetBrains Context
```

If both have Kotlin/navigation deficiencies, then test Serena.

Sourcegraph is unnecessary at current project scale.

## Visual verification

```text
Android CLI screenshots/previews
official Compose screenshot tests
Roborazzi
Maestro
Figma MCP
```

These are not all substitutes, but installing all of them would create needless process.

Recommended baseline:

```text
Android CLI
+
official Compose screenshot tests
```

Then add another only after an observed gap.

---

# 25. Repository-Intelligence A/B Test

Do not judge token tools based on one impressive demo.

Run the same tasks in separate clean sessions.

Representative tasks:

1. Change only the Planned section on Home to a compact layout.
2. Add one provider metadata field and display it on detail.
3. Make a Room schema change and migration.
4. Fix one import edge case.
5. Trace a detail → collection navigation/back-stack defect.

Test:

```text
A. Native agent + Android CLI
B. Native + Graphify
C. Native + JetBrains Context
```

Optionally test Serena only after this phase.

Record:

| Metric | Why |
|---|---|
| Input/output tokens, where exposed | Direct context cost |
| Peak context usage | Whether tool prevents context accumulation |
| Agent turns | Exploration efficiency |
| Number of repository files opened | Context locality |
| Wall time | Real developer efficiency |
| Files modified | Scope discipline |
| Diff size | Overengineering proxy |
| Tests passing | Correctness |
| Human review score | Architectural/UI quality |
| Task success | Ultimate criterion |

A repository-intelligence tool should demonstrate a repeatable material improvement, not a small token win that adds setup and failure modes.

A reasonable project-level threshold is roughly a **15–20% improvement in exploration effort/context on harder tasks without harming quality** before accepting permanent complexity. This is a project decision, not an industry benchmark.

---

# 26. Build vs Install Decisions

| Missing capability | Best solution | Why |
|---|---|---|
| Stable repository rules | **Documentation (`AGENTS.md`)** | No runtime/tool capability is missing |
| Task procedures | **Custom project skills** | Procedures depend on Omnilog invariants |
| Android semantic inspection | **Android CLI** | Official and framework-aware |
| Visual feedback | **Android CLI + screenshot tests** | Native to actual stack |
| Design intent | **Current design docs** | Already exists; reorganize it |
| High-fidelity design source | **Figma MCP later** | Only useful once Figma is authoritative |
| Cross-file structural understanding | **Benchmark external intelligence tool** | Potential value due to 50–150 KB hotspots |
| Simplicity guardrail | **Custom skill first; Ponytail test** | Current code does not justify compulsory tool |
| Room operations | **DB skill + existing tests** | MCP adds little |
| Provider API understanding | **Feature docs + code** | Existing boundaries already strong |
| Generic architecture generation | **Nothing** | Would likely decrease quality |

---

# 27. Recommended Minimal Tool Stack

## Install now

### Android CLI

Highest-confidence external addition.

Use for:

- declarations/usages;
- Compose previews;
- screenshots;
- UI semantics/layout;
- run/install workflows.

### Official Android agent skills — selectively

Install only useful skills such as Android CLI/testing setup, rather than an entire catalog of loosely relevant skills.

### Compose Preview Screenshot Testing

Pilot it on a small set of visually important components/screens.

### Repository-local Omnilog skills

These should be versioned with the repository because they encode **Omnilog's architecture**, not a generic Android architecture.

## Test

### Graphify vs JetBrains Context

Benchmark them as alternatives.

### Ponytail

Test against a repository-local simplicity skill.

### Roborazzi

Only if official screenshot tooling is not sufficient.

### Serena

Only if retrieval remains a pain after the first benchmark.

## Do not install yet

### Sourcegraph MCP

Too much system for a single repository of this scale.

### Figma MCP

Wait until Figma is part of the actual design workflow.

### Maestro

Android-native tooling plus existing tests should be tried first.

### JetBrains IDE MCP

Large overlap with Android CLI at present.

### Generic Android Clean Architecture skills

High risk of encouraging:

```text
UseCase
Repository
Interactor
Manager
Factory
DI module
multi-module split
```

for problems that currently have much simpler solutions.

---

# 28. Practices Already Good and Worth Protecting

## Local-first ownership is unusually clear

Room being authoritative and external providers being enrichment rather than ownership is correctly documented in both README and development guidance.

Future agents should **not weaken this invariant** for convenience.

## Provider boundaries are sensible

AniList, TMDB, RAWG, Google Books, OpenLibrary, Steam and recommendation implementations are separated instead of being one generic “API service”.

Do not merge them into a mega-provider abstraction in the name of reducing files.

## Manual dependency composition is appropriate

`ContentTrackerApplication` makes dependencies explicit without a DI framework.

Do not introduce Hilt solely because an agent-generated Android template normally would.

## Domain comments preserve important reasoning

Several comments explain **why behavior must remain a certain way** rather than narrating implementation history.

These are high-value agent context.

## Tests are organized around real behavior

The project has unit-test areas under both core and UI, with feature-specific test directories and migration/instrumentation testing present.

That gives agents a safer refactoring environment.

## The visual palette already has product identity

The warm charcoal/paper palette, per-medium accents, state colors and explicit contrast reasoning are good foundations.

Do not replace these with generic Material dynamic color unless the product direction changes intentionally.

## The design documentation already recognizes AI-UI convergence

The newest Home document explicitly rejects repeated carousels and repeated card geometry and assigns different visual forms to different semantics.

The correct action is to make agents **follow this better**, not invent another design philosophy.

---

# 29. Suggested Order of Implementation

## Phase 1 — almost zero architectural risk

1. Add `AGENTS.md`.
2. Untrack/ignore `.claude/settings.local.json`.
3. Add document status metadata.
4. Move historical docs into `archive/`.
5. Introduce the small project skills.
6. Document targeted build/test escalation.

## Phase 2 — UI-agent feedback

7. Install Android CLI.
8. Introduce deterministic Compose previews.
9. Add screenshot testing to 3–5 high-value UI states.
10. Add the visual-review completion rule.

## Phase 3 — improve context locality

11. Extract navigation/import/metadata hosts from `ContentTrackerApp.kt`.
12. Reduce application-wide responsibilities in `HomeViewModel`.
13. Extract backup/import/metadata concerns from `OfflineMediaRepository`.
14. Move inline Room migrations out of `ContentTrackerApplication`.

Each change should preserve externally visible behavior.

## Phase 4 — UI alignment

15. Implement the already-approved Home redesign.
16. Activate Libre Baskerville selectively.
17. Add a minimal spacing/shape vocabulary.
18. Visually validate before considering redesign complete.

## Phase 5 — external intelligence experiments

19. Baseline representative tasks with native tooling.
20. A/B Graphify vs JetBrains Context.
21. Keep at most one if the numbers justify it.
22. Test Ponytail separately for scope discipline.

---

# 30. Final Recommended Configuration

The target development loop should ultimately be much simpler than the amount of tooling investigated in this audit:

```text
                 AGENTS.md
                     │
          essential project truth
                     │
            ┌────────┴────────┐
            │                 │
       task skill       current feature doc
            │                 │
            └────────┬────────┘
                     │
             relevant code only
                     │
         Android CLI semantics
                     │
                implementation
                     │
       compile + focused tests
                     │
          rendered visual review
                     │
              screenshot check
                     │
                final diff
```

The installed tooling can remain approximately:

```text
Claude Code / Codex / Cursor
        +
Android CLI
        +
official Compose screenshot testing
        +
project-specific skills
        +
ONE repository-intelligence tool
  only if benchmark proves worthwhile
```

No general-purpose MCP collection is needed.

---

# 31. Bottom Line

The repository's architecture should **not** be replaced.

The project does not need:

- a Clean Architecture migration;
- Hilt;
- more generic services/managers;
- a multi-module rewrite;
- three code-index MCPs;
- a wholesale redesign.

The highest leverage comes from making the architecture that already exists **cheaper for an agent to discover**.

The two most important structural changes are:

> **Progressive disclosure for documentation/instructions**

and

> **better context locality around the few oversized orchestration files.**

For UI work, the most important change is:

> **an agent may not treat a Compose task as complete until it has inspected rendered output.**

For external tooling, the recommended approach is deliberately conservative:

> **Install Android CLI and screenshot testing now. Benchmark Graphify against JetBrains Context. Test Ponytail rather than assuming it helps. Leave Figma MCP, Sourcegraph, Maestro and other overlapping systems out until a concrete missing capability appears.**

That should reduce agent exploration, improve visual results, and constrain AI-generated complexity **without fighting Omnilog's current architecture, product decisions or working codebase**.

---

# 32. External References

- Android CLI: https://developer.android.com/tools/agents/android-cli
- Android agent/Compose testing documentation: https://developer.android.com/develop/ui/compose/testing
- Compose preview screenshot testing: https://developer.android.com/studio/preview/compose-screenshot-testing
- Graphify: https://github.com/Graphify-Labs/graphify
- Graphify website: https://graphify.com/
- JetBrains Context: https://www.jetbrains.com/context/
- Serena: https://github.com/oraios/serena
- Serena tools documentation: https://oraios.github.io/serena/01-about/035_tools.html
- Ponytail: https://github.com/DietrichGebert/ponytail
- Roborazzi: https://github.com/takahirom/roborazzi
- Figma MCP: https://developers.figma.com/docs/figma-mcp-server/
- Figma Code Connect: https://github.com/figma/code-connect
- Maestro: https://docs.maestro.dev/
