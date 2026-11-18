# Graph Report - JobHunterAI  (2026-11-18)

## Corpus Check
- 42 files · ~52,658 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 264 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 715 nodes · 1945 edges · 44 communities (15 shown, 29 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `eaaaaa1a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- EmailDispatcher.kt
- DocxGenerator
- Glass.kt
- AppSettings
- GlassSolidButton
- Uri
- CvTailoringEngine
- GlassTabBar.kt
- Motion
- RadarScreen.kt
- GlassStyle
- JobOffer
- Palette
- WebViewClient
- PipelineFilter
- .onCreate
- ApplicationAutomationManager.kt

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 64 edges
2. `UserProfile` - 59 edges
3. `JobScanner` - 39 edges
4. `CvTailoringEngine` - 36 edges
5. `JobOfferDao` - 28 edges
6. `SettingsRepository` - 28 edges
7. `ApplicationAutomationManager` - 26 edges
8. `AppSettings` - 25 edges
9. `JobOfferEntity` - 21 edges
10. `GlassSolidButton()` - 21 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `JobScanner`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/remote/JobScanner.kt
- `2. ✅ État d'Avancement Réel` --references--> `CvTailoringEngine`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/ai/CvTailoringEngine.kt
- `2. ✅ État d'Avancement Réel` --references--> `ApplicationAutomationManager`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/ApplicationAutomationManager.kt
- `2. ✅ État d'Avancement Réel` --references--> `PortalSessionRegistry`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/PortalSessionRegistry.kt
- `2. ✅ État d'Avancement Réel` --references--> `UserProfile`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/model/UserProfile.kt

## Import Cycles
- None detected.

## Communities (44 total, 29 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.06
Nodes (9): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler, 1. 🎯 Objectifs Principaux & Vision, 2. ✅ État d'Avancement Réel, 3. ⚠️ Dettes Techniques & Points de Vigilance, 4. 📦 Assets & Livrables (+1 more)

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.07
Nodes (11): LiveOfferDetails, LlmClient, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse (+3 more)

### Community 4 - "DocxGenerator"
Cohesion: 0.08
Nodes (3): DocxGenerator, PdfGenerator, JobHunterTheme()

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (7): glass(), glassCard(), glassFieldColors(), GlassStage(), LiquidGlassBackdrop(), pressScale(), rememberPress()

### Community 6 - "AppSettings"
Cohesion: 0.09
Nodes (9): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+1 more)

### Community 7 - "GlassSolidButton"
Cohesion: 0.15
Nodes (14): ApplicationOutcome, GlassFilterChip(), GlassSoftButton(), GlassSolidButton(), CvLanguageSlotCard(), CvScreen(), ApplicationTrackingCard(), MetricCard() (+6 more)

### Community 10 - "CvTailoringEngine"
Cohesion: 0.08
Nodes (24): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, MldpCoverLetterData, IncomingReplyEmail, JobStatus (+16 more)

### Community 11 - "GlassTabBar.kt"
Cohesion: 0.10
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 16 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 23 - "JobOffer"
Cohesion: 0.17
Nodes (7): JobScanner, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer

### Community 25 - "WebViewClient"
Cohesion: 0.13
Nodes (7): copyToClipboard(), DetectedOtpOrLink, exportDocsToDownloads(), fetchLatestOtpOrConfirmationLink(), WebPortalAssistantDialog(), WebChromeClient, WebViewClient

### Community 32 - "PipelineFilter"
Cohesion: 0.33
Nodes (6): PipelineFilter, ALL, AUTO, INTERVIEWS, REPLIES, WEB

### Community 35 - ".onCreate"
Cohesion: 0.12
Nodes (4): SettingsRepository, ApplicationAutomationManager, MainActivity, AgentScreen()

### Community 38 - "ApplicationAutomationManager.kt"
Cohesion: 0.09
Nodes (9): AutoApplyBatchReport, FollowUpCycleReport, DeadlineParser, DeadlineParseResult, EmailAuthType, OAUTH2_GMAIL, SMTP_DIRECT, EmailConfig (+1 more)

## Knowledge Gaps
- **32 isolated node(s):** `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 194 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **29 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserProfile` connect `CvTailoringEngine` to `JobHunterForegroundService.kt`, `AppDatabase`, `EmailDispatcher.kt`, `.onCreate`, `ApplicationAutomationManager.kt`, `GlassSolidButton`, `Uri`, `MainActivity.kt`, `RadarScreen.kt`, `WebPortalAssistantDialog.kt`, `JobOffer`, `WebViewClient`?**
  _High betweenness centrality (0.104) - this node is a cross-community bridge._
- **What connects `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05568627450980392 - nodes in this community are weakly interconnected._
- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `EmailDispatcher.kt`, `.onCreate`, `ApplicationAutomationManager.kt`, `GlassSolidButton`, `CvTailoringEngine`, `MainActivity.kt`, `RadarScreen.kt`, `WebPortalAssistantDialog.kt`, `WebViewClient`?**
  _High betweenness centrality (0.096) - this node is a cross-community bridge._
- **Should `JobOfferDao` be split into smaller, more focused modules?**
  _Cohesion score 0.1253968253968254 - nodes in this community are weakly interconnected._
- **Why does `SettingsRepository` connect `.onCreate` to `JobHunterForegroundService.kt`, `ApplicationAutomationManager.kt`, `AppSettings`, `AgentScreen.kt`, `MainActivity.kt`, `WebPortalAssistantDialog.kt`, `WebViewClient`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.10317460317460317 - nodes in this community are weakly interconnected._