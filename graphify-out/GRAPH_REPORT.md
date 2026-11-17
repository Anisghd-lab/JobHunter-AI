# Graph Report - JobHunterAI  (2026-11-17)

## Corpus Check
- 40 files · ~27,159 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 197 file(s) not represented in the graph (top: .xml 189, .properties 2, (none) 1)
- WARNING: 197 of 262 files seen (75%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 562 nodes · 1344 edges · 34 communities (12 shown, 22 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 17 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e82c380b`
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
- Palette.kt
- CvTailoringEngine
- JobOffer

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 44 edges
2. `UserProfile` - 33 edges
3. `JobScanner` - 27 edges
4. `SettingsRepository` - 20 edges
5. `JobOfferDao` - 18 edges
6. `CvTailoringEngine` - 18 edges
7. `JobHunterForegroundService` - 18 edges
8. `MainActivity` - 16 edges
9. `JobOfferEntity` - 16 edges
10. `DeadlineParser` - 16 edges

## Surprising Connections (you probably didn't know these)
- `2. ✅ État d'Avancement Réel` --references--> `JobHunterApp`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/JobHunterApp.kt
- `2. ✅ État d'Avancement Réel` --references--> `MainActivity`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/MainActivity.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobOfferDao`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/JobOfferDao.kt
- `2. ✅ État d'Avancement Réel` --references--> `JobScanner`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/remote/JobScanner.kt
- `2. ✅ État d'Avancement Réel` --references--> `CvTailoringEngine`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/ai/CvTailoringEngine.kt

## Import Cycles
- None detected.

## Communities (34 total, 22 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.05
Nodes (12): JobStatus, ANALYZED, APPLIED, ARCHIVED, DETECTED, INTERVIEW, READY_TO_SEND, REJECTED (+4 more)

### Community 2 - "AppDatabase"
Cohesion: 0.11
Nodes (3): AppDatabase, UserProfileDao, UserProfileEntity

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.07
Nodes (11): LiveOfferDetails, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, LlmResponse, EmailAuthType, OAUTH2_GMAIL (+3 more)

### Community 4 - "PdfGenerator.kt"
Cohesion: 0.14
Nodes (3): DocxGenerator, PdfGenerator, 2. ✅ État d'Avancement Réel

### Community 5 - "Glass.kt"
Cohesion: 0.07
Nodes (16): Edge, Bottom, Top, glass(), glassCard(), GlassStage(), GlassStyle, Accent (+8 more)

### Community 7 - "Capsule"
Cohesion: 0.10
Nodes (9): AgentScreen(), CvScreen(), ApplicationTrackingCard(), MetricCard(), PipelineScreen(), JobOfferCard(), RadarScreen(), Capsule() (+1 more)

### Community 8 - "GlassTabBar.kt"
Cohesion: 0.11
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 9 - "CvParser.kt"
Cohesion: 0.16
Nodes (6): CvParser, ExtractedCvData, CertificationItem, EducationItem, ExperienceItem, UserProfile

### Community 10 - ".onCreate"
Cohesion: 0.09
Nodes (14): AppSettings, FreeLlmCatalog, FreeLlmProvider, BYTEZ_FREE, GEMINI_FREE, OLLAMA_LOCAL, OPENROUTER_FREE, XKIRO_FREE (+6 more)

### Community 16 - "Palette.kt"
Cohesion: 0.39
Nodes (3): hex(), pair(), Palette

### Community 23 - "CvTailoringEngine"
Cohesion: 0.15
Nodes (7): CvTailoringEngine, MatchAnalysisResponse, MldpLetterAiPayload, TailoredCvResponse, LlmClient, LlmConfig, MldpCoverLetterData

### Community 24 - "JobOffer"
Cohesion: 0.10
Nodes (14): JobScanner, DeadlineParser, DeadlineParseResult, HtmlSanitizer, DocumentFormatRequirement, ANY, DOCX, PDF (+6 more)

## Knowledge Gaps
- **27 isolated node(s):** `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE`, `GEMINI_FREE`, `OLLAMA_LOCAL` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 170 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **22 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOffer` connect `JobOffer` to `JobHunterForegroundService.kt`, `JobOfferDao`, `EmailDispatcher.kt`, `PipelineScreen.kt`, `Capsule`, `.onCreate`, `MainActivity.kt`, `RadarScreen.kt`, `CvTailoringEngine`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **What connects `OPENROUTER_FREE`, `XKIRO_FREE`, `BYTEZ_FREE` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.053544494720965306 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `CvParser.kt` to `AppDatabase`, `EmailDispatcher.kt`, `Capsule`, `.onCreate`, `CvScreen.kt`, `MainActivity.kt`, `CvTailoringEngine`, `JobOffer`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.10826210826210826 - nodes in this community are weakly interconnected._
- **Why does `DeadlineParser` connect `JobOffer` to `JobHunterForegroundService.kt`, `EmailDispatcher.kt`, `PdfGenerator.kt`, `PipelineScreen.kt`, `Capsule`, `MainActivity.kt`, `RadarScreen.kt`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Should `EmailDispatcher.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06565656565656566 - nodes in this community are weakly interconnected._