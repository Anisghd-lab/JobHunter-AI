# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 40 files · ~35,828 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 262 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 623 nodes · 1623 edges · 41 communities (10 shown, 31 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 23 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `967d0f6e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- ApplicationAutomationManager.kt
- DocxGenerator
- Glass.kt
- GlassSoftButton
- GlassTabBar.kt
- Uri
- CvTailoringEngine
- Motion
- Type.kt
- JobOffer
- DeadlineParser
- GlassStyle
- .onCreate

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 53 edges
2. `UserProfile` - 48 edges
3. `JobScanner` - 34 edges
4. `CvTailoringEngine` - 31 edges
5. `SettingsRepository` - 22 edges
6. `2. ✅ État d'Avancement Réel` - 22 edges
7. `AppSettings` - 19 edges
8. `JobStatus` - 19 edges
9. `GlassSoftButton()` - 19 edges
10. `GlassSolidButton()` - 19 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `SettingsRepository`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/SettingsRepository.kt
- `2. ✅ État d'Avancement Réel` --references--> `ApplicationAutomationManager`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/ApplicationAutomationManager.kt
- `2. ✅ État d'Avancement Réel` --references--> `EmailDispatcher`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/email/EmailDispatcher.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobScanner`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/remote/JobScanner.kt

## Import Cycles
- None detected.

## Communities (41 total, 31 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.07
Nodes (4): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "ApplicationAutomationManager.kt"
Cohesion: 0.06
Nodes (13): LiveOfferDetails, LlmClient, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse (+5 more)

### Community 4 - "DocxGenerator"
Cohesion: 0.08
Nodes (3): DocxGenerator, PdfGenerator, JobHunterTheme()

### Community 7 - "GlassSoftButton"
Cohesion: 0.10
Nodes (22): ApplicationOutcome, glass(), glassCard(), glassFieldColors(), GlassFilterChip(), GlassSoftButton(), GlassSolidButton(), GlassStage() (+14 more)

### Community 10 - "CvTailoringEngine"
Cohesion: 0.09
Nodes (20): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, MldpCoverLetterData, IncomingReplyEmail, JobStatus (+12 more)

### Community 23 - "JobOffer"
Cohesion: 0.19
Nodes (7): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer

### Community 24 - "DeadlineParser"
Cohesion: 0.10
Nodes (9): DeadlineParser, DeadlineParseResult, hex(), pair(), Palette, 1. 🎯 Objectifs Principaux & Vision, 3. ⚠️ Dettes Techniques & Points de Vigilance, 4. 📦 Assets & Livrables (+1 more)

### Community 32 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 35 - ".onCreate"
Cohesion: 0.08
Nodes (15): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+7 more)

## Knowledge Gaps
- **27 isolated node(s):** `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 179 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `ApplicationAutomationManager.kt`, `.onCreate`, `GlassSoftButton`, `CvTailoringEngine`, `CvScreen.kt`, `RadarScreen.kt`, `DeadlineParser`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **What connects `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06866002214839424 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `CvTailoringEngine` to `AppDatabase`, `ApplicationAutomationManager.kt`, `.onCreate`, `CvScreen.kt`, `GlassSoftButton`, `Uri`, `CvScreen.kt`, `JobOffer`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.10317460317460317 - nodes in this community are weakly interconnected._
- **Why does `JobOfferEntity` connect `JobOfferDao` to `CvScreen.kt`, `JobHunterForegroundService.kt`, `AppDatabase`, `.onCreate`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Should `ApplicationAutomationManager.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.056866303690260134 - nodes in this community are weakly interconnected._