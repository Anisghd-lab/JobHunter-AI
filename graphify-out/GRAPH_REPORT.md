# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 40 files · ~34,849 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 262 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 619 nodes · 1610 edges · 40 communities (15 shown, 25 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 21 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6c191247`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- ApplicationAutomationManager.kt
- DocxGenerator
- Glass.kt
- PipelineScreen.kt
- Capsule
- GlassTabBar.kt
- Uri
- CvTailoringEngine
- RadarScreen.kt
- Motion
- AgentScreen.kt
- Palette.kt
- JobOffer
- DeadlineParser
- GlassStyle
- .onCreate

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 53 edges
2. `UserProfile` - 48 edges
3. `JobScanner` - 34 edges
4. `CvTailoringEngine` - 31 edges
5. `SettingsRepository` - 21 edges
6. `2. ✅ État d'Avancement Réel` - 20 edges
7. `JobStatus` - 19 edges
8. `GlassSoftButton()` - 19 edges
9. `GlassSolidButton()` - 19 edges
10. `JobHunterForegroundService` - 18 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `EmailDispatcher`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/email/EmailDispatcher.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobHunterForegroundService`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/service/JobHunterForegroundService.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt
- `2. ✅ État d'Avancement Réel` --references--> `CvDocumentSlot`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/model/UserProfile.kt
- `2. ✅ État d'Avancement Réel` --references--> `UserProfile`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/model/UserProfile.kt

## Import Cycles
- None detected.

## Communities (40 total, 25 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.07
Nodes (4): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "ApplicationAutomationManager.kt"
Cohesion: 0.06
Nodes (11): LiveOfferDetails, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, EmailAuthType, OAUTH2_GMAIL (+3 more)

### Community 4 - "DocxGenerator"
Cohesion: 0.08
Nodes (3): DocxGenerator, PdfGenerator, JobHunterTheme()

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (9): glass(), glassCard(), GlassFilterChip(), GlassSoftButton(), GlassSolidButton(), GlassStage(), LiquidGlassBackdrop(), pressScale() (+1 more)

### Community 6 - "PipelineScreen.kt"
Cohesion: 0.11
Nodes (3): ApplicationTrackingCard(), MetricCard(), PipelineScreen()

### Community 7 - "Capsule"
Cohesion: 0.20
Nodes (5): CvLanguageSlotCard(), CvScreen(), JobOfferCard(), Capsule(), RoundedRectangle()

### Community 8 - "GlassTabBar.kt"
Cohesion: 0.11
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 10 - "CvTailoringEngine"
Cohesion: 0.09
Nodes (16): CvTailoringEngine, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, LlmClient, LlmResponse, MldpCoverLetterData, IncomingReplyEmail (+8 more)

### Community 12 - "RadarScreen.kt"
Cohesion: 0.17
Nodes (3): ApplicationOutcome, ApplyAssistantDialog(), RadarScreen()

### Community 16 - "Palette.kt"
Cohesion: 0.39
Nodes (3): hex(), pair(), Palette

### Community 23 - "JobOffer"
Cohesion: 0.19
Nodes (8): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer, 2. ✅ État d'Avancement Réel

### Community 24 - "DeadlineParser"
Cohesion: 0.15
Nodes (6): DeadlineParser, DeadlineParseResult, 1. 🎯 Objectifs Principaux & Vision, 3. ⚠️ Dettes Techniques & Points de Vigilance, 4. 📦 Assets & Livrables, 🚀 Project State Ledger — JobHunter AI (JobPulse)

### Community 32 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 35 - ".onCreate"
Cohesion: 0.07
Nodes (21): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+13 more)

## Knowledge Gaps
- **27 isolated node(s):** `OAUTH2_GMAIL`, `SMTP_DIRECT`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 178 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `ApplicationAutomationManager.kt`, `.onCreate`, `PipelineScreen.kt`, `Capsule`, `CvTailoringEngine`, `CvScreen.kt`, `RadarScreen.kt`, `DeadlineParser`?**
  _High betweenness centrality (0.096) - this node is a cross-community bridge._
- **What connects `OAUTH2_GMAIL`, `SMTP_DIRECT`, `BYTEZ_FREE` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06659619450317125 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `.onCreate` to `AppDatabase`, `ApplicationAutomationManager.kt`, `Capsule`, `Uri`, `CvTailoringEngine`, `CvScreen.kt`, `JobOffer`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.10317460317460317 - nodes in this community are weakly interconnected._
- **Why does `JobOfferEntity` connect `JobOfferDao` to `JobHunterForegroundService.kt`, `AppDatabase`, `ApplicationAutomationManager.kt`, `.onCreate`, `CvScreen.kt`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Should `ApplicationAutomationManager.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06352941176470588 - nodes in this community are weakly interconnected._