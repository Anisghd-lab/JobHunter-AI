# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 42 files · ~45,393 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 264 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 696 nodes · 1900 edges · 48 communities (11 shown, 37 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 32 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a3525cdc`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOfferDao
- AppDatabase
- ApplicationAutomationManager.kt
- DocxGenerator
- Glass.kt
- GlassSolidButton
- GlassTabBar.kt
- Uri
- CvTailoringEngine
- Motion
- RadarScreen.kt
- GlassStyle
- WebPortalAssistantDialog.kt
- JobOffer
- Palette
- WebViewClient
- PipelineFilter
- .onCreate
- EmailDispatcher

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 61 edges
2. `UserProfile` - 54 edges
3. `CvTailoringEngine` - 36 edges
4. `JobScanner` - 36 edges
5. `2. ✅ État d'Avancement Réel` - 31 edges
6. `SettingsRepository` - 29 edges
7. `JobOfferDao` - 26 edges
8. `ApplicationAutomationManager` - 24 edges
9. `GlassSolidButton()` - 22 edges
10. `JobOfferEntity` - 21 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `CvTailoringEngine`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/ai/CvTailoringEngine.kt
- `2. ✅ État d'Avancement Réel` --references--> `PortalSessionRegistry`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/automation/PortalSessionRegistry.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt
- `2. ✅ État d'Avancement Réel` --references--> `SettingsRepository`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/SettingsRepository.kt
- `2. ✅ État d'Avancement Réel` --references--> `DeadlineParser`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/DeadlineParser.kt

## Import Cycles
- None detected.

## Communities (48 total, 37 thin omitted)

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "ApplicationAutomationManager.kt"
Cohesion: 0.05
Nodes (15): LiveOfferDetails, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, JobPortalAccount, PortalCheckResult (+7 more)

### Community 4 - "DocxGenerator"
Cohesion: 0.08
Nodes (3): DocxGenerator, PdfGenerator, JobHunterTheme()

### Community 7 - "GlassSolidButton"
Cohesion: 0.09
Nodes (23): ApplicationOutcome, glass(), glassCard(), GlassFilterChip(), GlassSoftButton(), GlassSolidButton(), GlassStage(), LiquidGlassBackdrop() (+15 more)

### Community 10 - "CvTailoringEngine"
Cohesion: 0.07
Nodes (22): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, LlmClient, LlmResponse, MldpCoverLetterData (+14 more)

### Community 16 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

### Community 23 - "JobOffer"
Cohesion: 0.10
Nodes (12): JobScanner, ApplicationAutomationManager, AutoApplyBatchReport, FollowUpCycleReport, HtmlSanitizer, IncomingReplyEmail, DocumentFormatRequirement, ANY (+4 more)

### Community 24 - "Palette"
Cohesion: 0.39
Nodes (3): hex(), pair(), Palette

### Community 32 - "PipelineFilter"
Cohesion: 0.33
Nodes (6): PipelineFilter, ALL, AUTO, INTERVIEWS, REPLIES, WEB

### Community 35 - ".onCreate"
Cohesion: 0.07
Nodes (17): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+9 more)

### Community 38 - "EmailDispatcher"
Cohesion: 0.18
Nodes (5): EmailAuthType, OAUTH2_GMAIL, SMTP_DIRECT, EmailConfig, EmailDispatcher

## Knowledge Gaps
- **32 isolated node(s):** `ALL`, `AUTO`, `REPLIES`, `INTERVIEWS`, `WEB` (+27 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 194 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **37 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobOfferDao`, `ApplicationAutomationManager.kt`, `.onCreate`, `GlassSolidButton`, `CvTailoringEngine`, `MainActivity.kt`, `RadarScreen.kt`, `WebPortalAssistantDialog.kt`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **Are the 30 inferred relationships involving `2. ✅ État d'Avancement Réel` (e.g. with `.canSendSilently()` and `SettingsRepository`) actually correct?**
  _`2. ✅ État d'Avancement Réel` has 30 INFERRED edges - model-reasoned connections that need verification._
- **What connects `ALL`, `AUTO`, `REPLIES` to the rest of the system?**
  _32 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1168091168091168 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `CvTailoringEngine` to `AppDatabase`, `ApplicationAutomationManager.kt`, `.onCreate`, `GlassSolidButton`, `Uri`, `CvScreen.kt`, `MainActivity.kt`, `WebPortalAssistantDialog.kt`, `JobOffer`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Should `JobOfferDao` be split into smaller, more focused modules?**
  _Cohesion score 0.13368983957219252 - nodes in this community are weakly interconnected._
- **Why does `SettingsRepository` connect `.onCreate` to `JobHunterForegroundService.kt`, `AgentScreen.kt`, `GlassSolidButton`, `MainActivity.kt`, `WebPortalAssistantDialog.kt`, `JobOffer`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._