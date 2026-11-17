# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 40 files · ~29,409 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 262 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 578 nodes · 1383 edges · 41 communities (10 shown, 31 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c3ad49cd`
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
- Uri
- .onCreate
- Theme.kt
- Palette.kt
- UserProfile
- JobOffer
- Palette

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 45 edges
2. `UserProfile` - 35 edges
3. `JobScanner` - 27 edges
4. `CvTailoringEngine` - 25 edges
5. `SettingsRepository` - 20 edges
6. `JobHunterForegroundService` - 18 edges
7. `JobOfferDao` - 18 edges
8. `MainActivity` - 16 edges
9. `JobStatus` - 16 edges
10. `JobOfferEntity` - 16 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `MainActivity`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/MainActivity.kt
- `1. 🎯 Objectifs Principaux & Vision` --references--> `MldpCoverLetterData`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/MldpCoverLetter.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobOfferDao`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/JobOfferDao.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobScanner`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/remote/JobScanner.kt
- `2. ✅ État d'Avancement Réel` --references--> `CvTailoringEngine`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/ai/CvTailoringEngine.kt

## Import Cycles
- None detected.

## Communities (41 total, 31 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.05
Nodes (12): JobStatus, ANALYZED, APPLIED, ARCHIVED, DETECTED, INTERVIEW, READY_TO_SEND, REJECTED (+4 more)

### Community 2 - "AppDatabase"
Cohesion: 0.10
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.07
Nodes (8): LiveOfferDetails, LlmClient, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse

### Community 4 - "PdfGenerator.kt"
Cohesion: 0.08
Nodes (9): ApplicationAutomationManager, DocxGenerator, PdfGenerator, EmailAuthType, OAUTH2_GMAIL, SMTP_DIRECT, EmailConfig, EmailDispatcher (+1 more)

### Community 5 - "Glass.kt"
Cohesion: 0.08
Nodes (16): Edge, Bottom, Top, glass(), glassCard(), GlassStage(), GlassStyle, Accent (+8 more)

### Community 7 - "Capsule"
Cohesion: 0.11
Nodes (10): ApplicationOutcome, CvScreen(), ApplicationTrackingCard(), MetricCard(), PipelineScreen(), ApplyAssistantDialog(), JobOfferCard(), RadarScreen() (+2 more)

### Community 8 - "GlassTabBar.kt"
Cohesion: 0.10
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 10 - ".onCreate"
Cohesion: 0.09
Nodes (12): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+4 more)

### Community 23 - "UserProfile"
Cohesion: 0.17
Nodes (10): CvTailoringEngine, TargetedCvContext, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, MldpCoverLetterData, CertificationItem, EducationItem (+2 more)

### Community 24 - "JobOffer"
Cohesion: 0.10
Nodes (14): JobScanner, DeadlineParser, DeadlineParseResult, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF (+6 more)

## Knowledge Gaps
- **27 isolated node(s):** `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 178 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobHunterForegroundService.kt`, `JobOfferDao`, `EmailDispatcher.kt`, `PdfGenerator.kt`, `PipelineScreen.kt`, `Capsule`, `.onCreate`, `MainActivity.kt`, `RadarScreen.kt`, `UserProfile`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **What connects `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.053544494720965306 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `UserProfile` to `AppDatabase`, `EmailDispatcher.kt`, `PdfGenerator.kt`, `Capsule`, `Uri`, `.onCreate`, `CvScreen.kt`, `MainActivity.kt`, `JobOffer`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.10317460317460317 - nodes in this community are weakly interconnected._
- **Why does `2. ✅ État d'Avancement Réel` connect `PdfGenerator.kt` to `JobHunterForegroundService.kt`, `JobOfferDao`, `.onCreate`, `UserProfile`, `JobOffer`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Should `EmailDispatcher.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.0743321718931475 - nodes in this community are weakly interconnected._