# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 40 files · ~40,834 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 262 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 654 nodes · 1726 edges · 45 communities (14 shown, 31 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 29 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `aa204edc`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- EmailDispatcher.kt
- DocxGenerator
- Glass.kt
- CvScreen.kt
- Capsule
- GlassTabBar.kt
- Uri
- CvTailoringEngine
- PipelineScreen.kt
- Motion
- EmailDispatcher
- JobOffer
- Palette
- PipelineFilter
- GlassStyle
- .onCreate

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 55 edges
2. `UserProfile` - 49 edges
3. `CvTailoringEngine` - 35 edges
4. `JobScanner` - 35 edges
5. `2. ✅ État d'Avancement Réel` - 28 edges
6. `JobOfferDao` - 26 edges
7. `SettingsRepository` - 26 edges
8. `ApplicationAutomationManager` - 23 edges
9. `JobOfferEntity` - 21 edges
10. `GlassSolidButton()` - 20 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `ApplicationAutomationManager`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/ApplicationAutomationManager.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt
- `2. ✅ État d'Avancement Réel` --references--> `SettingsRepository`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/SettingsRepository.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobScanner`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/remote/JobScanner.kt
- `2. ✅ État d'Avancement Réel` --references--> `DeadlineParser`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/DeadlineParser.kt

## Import Cycles
- None detected.

## Communities (45 total, 31 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.06
Nodes (5): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler, AgentScreen()

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.05
Nodes (14): LiveOfferDetails, LlmClient, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse (+6 more)

### Community 4 - "DocxGenerator"
Cohesion: 0.08
Nodes (3): DocxGenerator, PdfGenerator, JobHunterTheme()

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (10): glass(), glassCard(), glassFieldColors(), GlassFilterChip(), GlassSoftButton(), GlassSolidButton(), GlassStage(), LiquidGlassBackdrop() (+2 more)

### Community 7 - "Capsule"
Cohesion: 0.19
Nodes (7): ApplicationOutcome, ApplicationTrackingCard(), ApplyAssistantDialog(), JobOfferCard(), RadarScreen(), Capsule(), RoundedRectangle()

### Community 8 - "GlassTabBar.kt"
Cohesion: 0.10
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 10 - "CvTailoringEngine"
Cohesion: 0.08
Nodes (24): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, MldpCoverLetterData, IncomingReplyEmail, JobStatus (+16 more)

### Community 16 - "EmailDispatcher"
Cohesion: 0.14
Nodes (7): AutoApplyBatchReport, FollowUpCycleReport, EmailAuthType, OAUTH2_GMAIL, SMTP_DIRECT, EmailConfig, EmailDispatcher

### Community 23 - "JobOffer"
Cohesion: 0.19
Nodes (7): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer

### Community 24 - "Palette"
Cohesion: 0.39
Nodes (3): hex(), pair(), Palette

### Community 32 - "PipelineFilter"
Cohesion: 0.33
Nodes (6): PipelineFilter, ALL, AUTO, INTERVIEWS, REPLIES, WEB

### Community 34 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 35 - ".onCreate"
Cohesion: 0.08
Nodes (12): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+4 more)

## Knowledge Gaps
- **32 isolated node(s):** `GEMINI`, `OLLAMA_CUSTOM`, `OPENAI`, `ANALYZED`, `APPLIED` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 186 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `EmailDispatcher.kt`, `.onCreate`, `Capsule`, `CvTailoringEngine`, `AgentScreen.kt`, `PipelineScreen.kt`, `RadarScreen.kt`, `EmailDispatcher`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._
- **Are the 27 inferred relationships involving `2. ✅ État d'Avancement Réel` (e.g. with `.canSendSilently()` and `SettingsRepository`) actually correct?**
  _`2. ✅ État d'Avancement Réel` has 27 INFERRED edges - model-reasoned connections that need verification._
- **What connects `GEMINI`, `OLLAMA_CUSTOM`, `OPENAI` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.061979648473635525 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `CvTailoringEngine` to `AppDatabase`, `EmailDispatcher.kt`, `.onCreate`, `CvScreen.kt`, `Uri`, `AgentScreen.kt`, `EmailDispatcher`, `JobOffer`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Should `JobOfferDao` be split into smaller, more focused modules?**
  _Cohesion score 0.13368983957219252 - nodes in this community are weakly interconnected._
- **Why does `SettingsRepository` connect `.onCreate` to `EmailDispatcher`, `JobHunterForegroundService.kt`, `CvTailoringEngine`, `AgentScreen.kt`?**
  _High betweenness centrality (0.051) - this node is a cross-community bridge._