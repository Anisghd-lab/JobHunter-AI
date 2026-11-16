# Graph Report - JobHunterAI  (2026-11-16)

## Corpus Check
- 35 files · ~13,053 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 199 file(s) not represented in the graph (top: .xml 191, .properties 2, (none) 1)
- WARNING: 199 of 249 files seen (80%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 433 nodes · 914 edges · 23 communities (8 shown, 15 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a9902212`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JobHunterForegroundService.kt
- JobOffer
- UserProfile
- EmailDispatcher.kt
- MainActivity.kt
- Glass.kt
- Capsule
- GlassTabBar.kt
- CvParser.kt
- .onCreate
- Theme.kt
- Motion
- GlassStyle
- Palette.kt
- Palette

## God Nodes (most connected - your core abstractions)
1. `JobOffer` - 23 edges
2. `UserProfile` - 20 edges
3. `JobHunterForegroundService` - 18 edges
4. `JobOfferEntity` - 15 edges
5. `JobOfferDao` - 14 edges
6. `Capsule()` - 13 edges
7. `AppDatabase` - 12 edges
8. `CvParser` - 12 edges
9. `JobStatus` - 12 edges
10. `UserProfileEntity` - 11 edges

## Surprising Connections (you probably didn't know these)
- `1. 🎯 Objectifs & Réalisations` --references--> `JobOfferEntity`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/JobOfferEntity.kt
- `1. 🎯 Objectifs & Réalisations` --references--> `DocxGenerator`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/domain/document/DocxGenerator.kt
- `1. 🎯 Objectifs & Réalisations` --references--> `JobHunterForegroundService`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/service/JobHunterForegroundService.kt
- `1. 🎯 Objectifs & Réalisations` --references--> `JobHunterScheduler`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/service/JobHunterScheduler.kt
- `1. 🎯 Objectifs & Réalisations` --references--> `UserProfileEntity`  [INFERRED]
  PROJECT_STATE.md → app/src/main/kotlin/com/ghdinteractivestudio/jobhunter/data/local/UserProfileEntity.kt

## Import Cycles
- None detected.

## Communities (23 total, 15 thin omitted)

### Community 0 - "JobHunterForegroundService.kt"
Cohesion: 0.06
Nodes (5): JobHunterApp, BootReceiver, JobHunterForegroundService, JobHunterScheduler, AgentScreen()

### Community 1 - "JobOffer"
Cohesion: 0.08
Nodes (16): JobOfferDao, JobOfferEntity, JobScanner, DocumentFormatRequirement, ANY, DOCX, PDF, JobOffer (+8 more)

### Community 2 - "UserProfile"
Cohesion: 0.09
Nodes (12): UserProfileDao, UserProfileEntity, CvTailoringEngine, MatchAnalysisResponse, TailoredCvResponse, CertificationItem, EducationItem, ExperienceItem (+4 more)

### Community 3 - "EmailDispatcher.kt"
Cohesion: 0.07
Nodes (11): LlmClient, LlmConfig, LlmProvider, GEMINI, OLLAMA_CUSTOM, OPENAI, EmailAuthType, OAUTH2_GMAIL (+3 more)

### Community 5 - "Glass.kt"
Cohesion: 0.11
Nodes (9): Edge, Bottom, Top, glass(), glassCard(), GlassStage(), pressScale(), rememberPress() (+1 more)

### Community 7 - "Capsule"
Cohesion: 0.12
Nodes (8): CvScreen(), ApplicationTrackingCard(), MetricCard(), PipelineScreen(), JobOfferCard(), RadarScreen(), Capsule(), RoundedRectangle()

### Community 8 - "GlassTabBar.kt"
Cohesion: 0.12
Nodes (3): GlassTabBar(), JobTabItem, Type

### Community 15 - "GlassStyle"
Cohesion: 0.33
Nodes (6): GlassStyle, Accent, Card, Clear, Menu, Regular

## Knowledge Gaps
- **22 isolated node(s):** `GEMINI`, `OPENAI`, `OLLAMA_CUSTOM`, `OAUTH2_GMAIL`, `SMTP_DIRECT` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 137 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JobOfferEntity` connect `JobOffer` to `JobHunterForegroundService.kt`, `.onCreate`, `UserProfile`, `MainActivity.kt`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **What connects `GEMINI`, `OPENAI`, `OLLAMA_CUSTOM` to the rest of the system?**
  _22 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JobHunterForegroundService.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.060408163265306125 - nodes in this community are weakly interconnected._
- **Why does `UserProfile` connect `UserProfile` to `MainActivity.kt`, `Capsule`, `CvParser.kt`, `.onCreate`, `CvScreen.kt`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Should `JobOffer` be split into smaller, more focused modules?**
  _Cohesion score 0.08405797101449275 - nodes in this community are weakly interconnected._
- **Why does `JobHunterForegroundService` connect `JobHunterForegroundService.kt` to `JobOffer`, `UserProfile`, `AgentScreen.kt`?**
  _High betweenness centrality (0.073) - this node is a cross-community bridge._
- **Should `UserProfile` be split into smaller, more focused modules?**
  _Cohesion score 0.08943089430894309 - nodes in this community are weakly interconnected._