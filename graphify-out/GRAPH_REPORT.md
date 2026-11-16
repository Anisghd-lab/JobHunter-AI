# Graph Report - JobHunterAI  (2026-11-16)

## Corpus Check
- 38 files · ~22,905 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 200 file(s) not represented in the graph (top: .xml 192, .properties 2, (none) 1)
- WARNING: 200 of 253 files seen (79%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 526 nodes · 1231 edges · 35 communities (12 shown, 23 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 16 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1a39972f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- EmailDispatcher.kt
- PdfGenerator.kt
- Glass.kt
- Capsule
- GlassTabBar.kt
- CvParser.kt
- .onCreate
- Theme.kt
- Motion
- GlassStyle
- Palette.kt
- RadarScreen.kt
- 2. ✅ État d'Avancement Réel
- JobOffer

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 38 edges
2. `UserProfile` - 32 edges
3. `JobScanner` - 24 edges
4. `SettingsRepository` - 18 edges
5. `JobHunterForegroundService` - 18 edges
6. `JobOfferDao` - 16 edges
7. `JobOfferEntity` - 16 edges
8. `2. ✅ État d'Avancement Réel` - 16 edges
9. `MainActivity` - 15 edges
10. `AppSettings` - 15 edges

## Surprising Connections (you probably didn't know these)
- `1. 🎯 Objectifs Principaux & Vision` --references--> `HtmlSanitizer`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/HtmlSanitizer.kt
- `2. ✅ État d'Avancement Réel` --references--> `SettingsRepository`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/SettingsRepository.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobScanner`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/remote/JobScanner.kt
- `2. ✅ État d'Avancement Réel` --references--> `ApplicationAutomationManager`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/ApplicationAutomationManager.kt
- `2. ✅ État d'Avancement Réel` --references--> `HtmlSanitizer`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/HtmlSanitizer.kt

## Import Cycles
- None detected.

## Communities (35 total, 23 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.07
Nodes (4): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler

### Community 2 - "AppDatabase"
Cohesion: 0.11
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.05
Nodes (19): LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse, EmailAuthType, OAUTH2_GMAIL (+11 more)

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (9): Edge, Bottom, Top, glass(), glassCard(), GlassStage(), pressScale(), rememberPress() (+1 more)

### Community 7 - "Capsule"
Cohesion: 0.13
Nodes (8): ApplicationOutcome, AgentScreen(), CvScreen(), ApplyAssistantDialog(), JobOfferCard(), RadarScreen(), Capsule(), RoundedRectangle()

### Community 8 - "GlassTabBar.kt"
Cohesion: 0.10
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 9 - "CvParser.kt"
Cohesion: 0.16
Nodes (6): CvParser, ExtractedCvData, CertificationItem, EducationItem, ExperienceItem, UserProfile

### Community 10 - ".onCreate"
Cohesion: 0.17
Nodes (3): SettingsRepository, ApplicationAutomationManager, MainActivity

### Community 13 - "Theme.kt"
Cohesion: 0.11
Nodes (4): ApplicationTrackingCard(), MetricCard(), PipelineScreen(), JobHunterTheme()

### Community 15 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 23 - "2. ✅ État d'Avancement Réel"
Cohesion: 0.08
Nodes (18): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+10 more)

### Community 24 - "JobOffer"
Cohesion: 0.20
Nodes (7): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer

## Knowledge Gaps
- **27 isolated node(s):** `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 161 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `EmailDispatcher.kt`, `PipelineScreen.kt`, `Capsule`, `.onCreate`, `AgentScreen.kt`, `Theme.kt`, `RadarScreen.kt`, `2. ✅ État d'Avancement Réel`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **What connects `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `CvParser.kt` to `AppDatabase`, `EmailDispatcher.kt`, `Capsule`, `.onCreate`, `CvScreen.kt`, `AgentScreen.kt`, `2. ✅ État d'Avancement Réel`, `JobOffer`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.10826210826210826 - nodes in this community are weakly interconnected._
- **Why does `JobOfferEntity` connect `JobOfferDao` to `JobHunterForegroundService.kt`, `AppDatabase`, `EmailDispatcher.kt`, `.onCreate`, `AgentScreen.kt`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Should `EmailDispatcher.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05370101596516691 - nodes in this community are weakly interconnected._