# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 42 files · ~46,761 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 264 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 700 nodes · 1922 edges · 59 communities (15 shown, 44 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 34 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `3b18b09f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- JobScanner.kt
- DocxGenerator
- Glass.kt
- EmailDispatcher.kt
- GlassSolidButton
- GlassTabBar.kt
- Uri
- CvTailoringEngine
- DeadlineParser
- Motion
- RadarScreen.kt
- GlassStyle
- LlmClient
- JobOffer
- Palette
- WebViewClient
- PipelineFilter
- .onCreate
- EmailDispatcher
- Type.kt
- LlmProvider
- PortalSessionRegistry

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 62 edges
2. `UserProfile` - 55 edges
3. `JobScanner` - 38 edges
4. `CvTailoringEngine` - 36 edges
5. `2. ✅ État d'Avancement Réel` - 33 edges
6. `SettingsRepository` - 29 edges
7. `JobOfferDao` - 27 edges
8. `ApplicationAutomationManager` - 24 edges
9. `GlassSolidButton()` - 22 edges
10. `JobOfferEntity` - 21 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `PortalSessionRegistry`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/PortalSessionRegistry.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobHunterForegroundService`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/service/JobHunterForegroundService.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobOfferDao`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/JobOfferDao.kt
- `2. ✅ État d'Avancement Réel` --references--> `SettingsRepository`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/SettingsRepository.kt

## Import Cycles
- None detected.

## Communities (59 total, 44 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.07
Nodes (4): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 4 - "DocxGenerator"
Cohesion: 0.08
Nodes (3): DocxGenerator, PdfGenerator, JobHunterTheme()

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (7): glass(), glassCard(), glassFieldColors(), GlassStage(), LiquidGlassBackdrop(), pressScale(), rememberPress()

### Community 7 - "GlassSolidButton"
Cohesion: 0.14
Nodes (14): GlassFilterChip(), GlassSoftButton(), GlassSolidButton(), AgentScreen(), CvLanguageSlotCard(), CvScreen(), ApplicationTrackingCard(), MetricCard() (+6 more)

### Community 10 - "CvTailoringEngine"
Cohesion: 0.08
Nodes (23): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, MldpCoverLetterData, JobStatus, ANALYZED (+15 more)

### Community 11 - "DeadlineParser"
Cohesion: 0.31
Nodes (5): DeadlineParser, 1. 🎯 Objectifs Principaux & Vision, 3. ⚠️ Dettes Techniques & Points de Vigilance, 4. 📦 Assets & Livrables, 🚀 Project State Ledger — JobHunter AI (JobPulse)

### Community 16 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 23 - "JobOffer"
Cohesion: 0.17
Nodes (8): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer, 2. ✅ État d'Avancement Réel

### Community 25 - "WebViewClient"
Cohesion: 0.16
Nodes (7): copyToClipboard(), DetectedOtpOrLink, exportDocsToDownloads(), fetchLatestOtpOrConfirmationLink(), WebPortalAssistantDialog(), WebChromeClient, WebViewClient

### Community 32 - "PipelineFilter"
Cohesion: 0.33
Nodes (6): PipelineFilter, ALL, AUTO, INTERVIEWS, REPLIES, WEB

### Community 35 - ".onCreate"
Cohesion: 0.07
Nodes (16): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+8 more)

### Community 38 - "EmailDispatcher"
Cohesion: 0.18
Nodes (5): EmailAuthType, OAUTH2_GMAIL, SMTP_DIRECT, EmailConfig, EmailDispatcher

### Community 48 - "LlmProvider"
Cohesion: 0.40
Nodes (5): LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI

### Community 56 - "PortalSessionRegistry"
Cohesion: 0.60
Nodes (3): JobPortalAccount, PortalCheckResult, PortalSessionRegistry

## Knowledge Gaps
- **32 isolated node(s):** `GEMINI`, `OLLAMA_CUSTOM`, `OPENAI`, `Accent`, `Card` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 202 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **44 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserProfile` connect `CvTailoringEngine` to `AppDatabase`, `JobScanner.kt`, `.onCreate`, `GlassSolidButton`, `Uri`, `WebPortalAssistantDialog.kt`, `RadarScreen.kt`, `JobOffer`, `WebViewClient`?**
  _High betweenness centrality (0.085) - this node is a cross-community bridge._
- **Are the 32 inferred relationships involving `2. ✅ État d'Avancement Réel` (e.g. with `JobOfferDao` and `.canSendSilently()`) actually correct?**
  _`2. ✅ État d'Avancement Réel` has 32 INFERRED edges - model-reasoned connections that need verification._
- **What connects `GEMINI`, `OLLAMA_CUSTOM`, `OPENAI` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06707317073170732 - nodes in this community are weakly interconnected._
- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `JobScanner.kt`, `.onCreate`, `EmailDispatcher.kt`, `GlassSolidButton`, `CvTailoringEngine`, `WebPortalAssistantDialog.kt`, `RadarScreen.kt`, `WebViewClient`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Should `JobOfferDao` be split into smaller, more focused modules?**
  _Cohesion score 0.13368983957219252 - nodes in this community are weakly interconnected._
- **Why does `2. ✅ État d'Avancement Réel` connect `JobOffer` to `JobHunterForegroundService.kt`, `JobOfferDao`, `.onCreate`, `Glass.kt`, `EmailDispatcher`, `GlassSolidButton`, `CvTailoringEngine`, `DeadlineParser`, `RadarScreen.kt`, `PortalSessionRegistry`?**
  _High betweenness centrality (0.067) - this node is a cross-community bridge._