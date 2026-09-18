# Imports Audit

**Date:** 18 September 2026
**Status:** Advisory. Nothing here is fixed just because it is written down.
**Scope:** The user-facing import flow for MyAnimeList (account and XML), IMDb CSV and StoryGraph CSV: entry points, file choice, preview, confirmation, background metadata completion, the progress banner and the Importacions activity sheet. Backup restore is out of scope.

Paths are relative to `app/src/main/java/com/nilpo/contenttracker/`.

---

## 1. The flow today

```text
Entry            Settings › Importacions (IMDb, StoryGraph, MAL XML)
                 Settings › MyAnimeList › Importa des del compte
                 Empty section › "Importa" (Anime → MAL XML only, Books → StoryGraph, Cinema → IMDb)
      │
File             system picker opens straight away, with no word on which file to pick
      │
Preview          AlertDialog per source (4 copy-pasted blocks in ui/ContentTrackerApp.kt)
      │
Confirm          dialog turns into "Important…", then closes
      │
Result           one long snackbar with every counter, zeros included
      │
Background       banner above the navigation bar while metadata is completed
      │
Completion       snackbar "Enriquiment de … completat: …" with a "Revisa" action
      │
Activity sheet   ui/imports/ImportHub.kt: batches, incidents, gaps, reviews, history
```

The core is sound. Imports are additive, duplicates are skipped by stable identity, enrichment is durable, and uncertain matches become reviews instead of guesses. The problems are all in how the flow is presented.

---

## 2. Findings

### 2.1 MAL XML: the file MAL gives you is refused. **Bug.**
MyAnimeList exports `animelist_….xml.gz`. `readProviderImportText` (`core/repository/ProviderImportFile.kt`) only decodes text. The gzip bytes trip the control-character check, and the user reads "No s'ha pogut llegir la codificació del fitxer", which points at the wrong cause. The user has to find a way to decompress on the phone first.

### 2.2 The picker opens cold.
Tapping IMDb, StoryGraph or MAL goes straight to the system file picker. Nothing says where the export comes from, what it is called, or that it must be the unedited original. Several of the read errors ("missing columns", "header only") are really symptoms of picking the wrong file.

### 2.3 Four previews, three designs.
- IMDb and StoryGraph show a sentence plus grouped skipped rows with examples.
- MAL XML and MAL account show a `\n\n`-joined block that always lists "Duplicats ignorats: 0 / Entrades no compatibles: 0 / Entrades invàlides ignorades: 0".
- The MAL account preview asks for the title language; the XML preview does not, although both import anime titles.
- All four are Material alert dialogs: a bold sans title, body text, and two text buttons of equal weight. They share nothing with the rest of the app.

### 2.4 The results read like a log.
The success snackbar lists every counter, zeros included: "12 elements importats. Duplicats ignorats: 0. No compatibles: 0. Files sense títol: 0. …". The completion snackbar does the same with "processats / enriquits / per revisar".

### 2.5 The activity sheet puts decisions last.
The order is batches, then incidents, then gaps ("Totals de seguiment pendents"), then reviews. Reviews are the one thing only the user can do, and they come at the bottom.

Every item is a bordered card with up to three stacked, right-aligned text buttons ("Torna a consultar / Prova una altra coincidència / Dona-ho per bo"). A list of ten incidents is about thirty buttons.

With nothing pending, the sheet says "No hi ha cap decisió pendent." and nothing else. It offers no way to start an import and no reassurance.

### 2.6 Jargon.
"Enriquiment", "enriquits", "processats" and "Totals de seguiment" are internal words. The user thinks in terms of "completing covers, synopses and totals".

### 2.7 Contextual entry points are narrower than they need to be.
An empty Anime section offers only the XML import, even when a MAL account is connected and one tap would import the whole list.

### 2.8 The progress banner.
It is always tinted with the Anime accent, including for IMDb and StoryGraph. It shows the same raw counters as the sheet.

---

## 3. Direction

1. Read `.xml.gz` directly (stdlib `GZIPInputStream`, same size limit on the decompressed text).
2. Put one **import sheet** in front of every source.
   - **Step 1** says where to get the file and has a "Tria el fitxer" button. For MAL with a connected account, the account import is offered here too.
   - **Step 2** is the preview, in the same sheet:
     - a serif lead number;
     - a reassurance line;
     - the skipped rows folded behind one line, zero groups hidden;
     - the title language for any anime import;
     - one primary button ("Importa 42 animes").
3. Make result messages short and zero-free.
4. Rebuild the activity sheet in the editorial style:
   - serif title;
   - reviews first, then incidents, then missing fields, then the running imports;
   - flat rows, each with one visible action and the rest in an overflow menu;
   - an empty state that says everything is up to date.
5. Colour the banner with the source's section accent.
6. Anime section: offer the account import when connected.

Behaviour, safety rules and the enrichment pipeline stay as specified in `docs/features/imports.md`.
