# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 42 files · ~47,801 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 264 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 703 nodes · 1942 edges · 51 communities (13 shown, 38 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 35 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `15946857`
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
- GlassSolidButton
- Uri
- CvTailoringEngine
- Theme.kt
- GlassTabBar
- RadarScreen.kt
- GlassStyle
- JobOffer
- Palette
- WebViewClient
- PipelineFilter
- .onCreate
- ApplicationAutomationManager.kt

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 63 edges
2. `UserProfile` - 56 edges
3. `JobScanner` - 38 edges
4. `CvTailoringEngine` - 36 edges
5. `2. ✅ État d'Avancement Réel` - 34 edges
6. `SettingsRepository` - 29 edges
7. `JobOfferDao` - 27 edges
8. `ApplicationAutomationManager` - 26 edges
9. `GlassSolidButton()` - 22 edges
10. `JobOfferEntity` - 21 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `MainActivity`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/MainActivity.kt
- `2. ✅ État d'Avancement Réel` --references--> `SettingsRepository`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/SettingsRepository.kt
- `2. ✅ État d'Avancement Réel` --references--> `ApplicationAutomationManager`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/ApplicationAutomationManager.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobHunterForegroundService`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/service/JobHunterForegroundService.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt

## Import Cycles
- None detected.

## Communities (51 total, 38 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.06
Nodes (5): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler, AgentScreen()

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.07
Nodes (11): LiveOfferDetails, LlmClient, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse (+3 more)

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (8): glass(), glassCard(), glassFieldColors(), GlassFilterChip(), GlassStage(), LiquidGlassBackdrop(), pressScale(), rememberPress()

### Community 7 - "GlassSolidButton"
Cohesion: 0.18
Nodes (9): GlassSoftButton(), GlassSolidButton(), ApplicationTrackingCard(), MetricCard(), PipelineScreen(), JobOfferCard(), RadarScreen(), Capsule() (+1 more)

### Community 10 - "CvTailoringEngine"
Cohesion: 0.11
Nodes (15): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, MldpCoverLetterData, CertificationItem, CvDocumentSlot (+7 more)

### Community 13 - "GlassTabBar"
Cohesion: 0.12
Nodes (3): GlassTabBar(), JobTabItem, Motion

### Community 16 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 23 - "JobOffer"
Cohesion: 0.18
Nodes (8): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer, 2. ✅ État d'Avancement Réel

### Community 25 - "WebViewClient"
Cohesion: 0.15
Nodes (7): copyToClipboard(), DetectedOtpOrLink, exportDocsToDownloads(), fetchLatestOtpOrConfirmationLink(), WebPortalAssistantDialog(), WebChromeClient, WebViewClient

### Community 32 - "PipelineFilter"
Cohesion: 0.33
Nodes (6): PipelineFilter, ALL, AUTO, INTERVIEWS, REPLIES, WEB

### Community 35 - ".onCreate"
Cohesion: 0.08
Nodes (14): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+6 more)

### Community 38 - "ApplicationAutomationManager.kt"
Cohesion: 0.06
Nodes (22): AutoApplyBatchReport, FollowUpCycleReport, DeadlineParser, DeadlineParseResult, EmailAuthType, OAUTH2_GMAIL, SMTP_DIRECT, EmailConfig (+14 more)

## Knowledge Gaps
- **32 isolated node(s):** `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 198 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserProfile` connect `CvTailoringEngine` to `AppDatabase`, `EmailDispatcher.kt`, `.onCreate`, `ApplicationAutomationManager.kt`, `CvScreen.kt`, `Uri`, `MainActivity.kt`, `RadarScreen.kt`, `JobOffer`, `WebViewClient`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Are the 33 inferred relationships involving `2. ✅ État d'Avancement Réel` (e.g. with `JobOfferDao` and `.canSendSilently()`) actually correct?**
  _`2. ✅ État d'Avancement Réel` has 33 INFERRED edges - model-reasoned connections that need verification._
- **What connects `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06294326241134751 - nodes in this community are weakly interconnected._
- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `EmailDispatcher.kt`, `.onCreate`, `ApplicationAutomationManager.kt`, `GlassSolidButton`, `CvTailoringEngine`, `MainActivity.kt`, `RadarScreen.kt`, `WebViewClient`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Should `JobOfferDao` be split into smaller, more focused modules?**
  _Cohesion score 0.13368983957219252 - nodes in this community are weakly interconnected._
- **Why does `2. ✅ État d'Avancement Réel` connect `JobOffer` to `JobHunterForegroundService.kt`, `JobOfferDao`, `EmailDispatcher.kt`, `.onCreate`, `Glass.kt`, `ApplicationAutomationManager.kt`, `GlassSolidButton`, `CvTailoringEngine`, `RadarScreen.kt`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._