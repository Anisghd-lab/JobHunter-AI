# 🚀 Project State Ledger — JobHunter AI (JobPulse)

> **Repository** : `Anisghd-lab/JobHunter-AI`  
> **Package** : `com.ghdinteractivestudio.jobhunter`  
> **Orchestrateur** : `agency-agents-orchestrator`  
> **Lead Mobile & UI** : `agency-mobile-app-builder` & `liquid-glass-design`  
> **NLP & Documents** : `agency-resume-tailor` & `agency-document-generator`  
> **Email & Détection** : `agency-email-intelligence-engineer`  

---

## 1. 🎯 Objectifs Principaux & Vision
- **Agent Autonome 24/7 sur Smartphone** : Veille continue multi-plateformes (France & International, Remote & Présentiel, CDI, CDD, Freelance, Missions).
- **Moteur Documentaire Bivalent (PDF & Word .docx)** :
  - Parsing et ingestion intelligente du CV source (PDF et .docx).
  - Détection automatique du format requis par l'employeur (PDF vs Word .docx).
  - Tailoring éthique du CV (mise en valeur des compétences réelles sans falsification).
  - Rédaction de lettres de motivation personnalisées percutantes selon les règles de l'art.
- **Double Envoi d'Email** : Intégration OAuth2 (Gmail API / Outlook) + SMTP direct configurable avec pièces jointes (.pdf/.docx).
- **Tableau de Suivi & Pipeline** : Suivi des statuts (Détectée, Analysée, Prête, Envoyée, Entretien, Archivée) et scanner d'inbox pour détecter les réponses recruteurs.
- **Design System CodeStation Bimodal** : Thème Liquid Glass avec switch instantané Dark Mode (Obsidian OLED `#0B0D12`) et Light Mode (Blanc givré `#F8F9FA`), et réplication pixel-perfect de la barre de navigation `GlassTabBar`.

---

## 2. ⚡ État d'Avancement Réel
- [x] Spécifications et cadrage technique validés avec l'utilisateur via `ask_question`.
- [x] Sélection et activation des 8 agents experts Agency.
- [x] Artefact Maître d'Architecture généré (`MASTER_PLAN_JOBHUNTER_AI.md`).
- [x] Création et liaison du dépôt GitHub officiel : `https://github.com/Anisghd-lab/JobHunter-AI`.
- [x] Initialisation du socle Gradle 8.x + Kotlin 2.0.20 + Jetpack Compose.
- [ ] Implémentation du Design System bimodal CodeStation (`Palette.kt`, `Glass.kt`, `Chrome.kt`, `Type.kt`, `Motion.kt`).
- [ ] Implémentation de la barre de navigation flottante `GlassTabBar`.
- [ ] Implémentation du Moteur Documentaire Bivalent (PDF & OpenXML Word `.docx`).
- [ ] Implémentation du Service d'Arrière-Plan 24/7 (`JobHunterForegroundService` + `WorkManager`).
- [ ] Implémentation du Moteur LLM Multi-Fournisseurs (Gemini, GPT, Claude, Ollama).
- [ ] Implémentation du Client Email Dual (OAuth2 + JavaMail/Jakarta Mail SMTP).
- [ ] Implémentation de la Base de Données Room et des Écrans (Radar, Pipeline, Mon CV, Agent 24/7).
- [ ] Compilation de l'APK et publication en GitHub Release.

---

## 3. 🔍 Lacunes, Dettes Techniques & Points de Vigilance
- **Gestion agressive de la batterie Android** : Obligation d'afficher le dialogue système `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` pour garantir l'exécution 24/7 ininterrompue.
- **Quota & Économie de Tokens** : Maintien du filtre heuristique de niveau 1 (0 token) pour ne solliciter les LLMs que pour les offres réellement pertinentes.
- **Compatibilité OpenXML Word (.docx)** : Génération d'une structure ZIP/XML standardisée sans dépendances lourdes pour préserver la légèreté de l'APK.

---

## 4. 📋 Prochaines Étapes (Backlog Priorisé)
1. Configuration des fichiers Gradle racine et module `app`.
2. Port du Design System CodeStation (`Palette`, `Glass`, `GlassTabBar`, `Motion`, `Shape`, `Type`).
3. Création des modules de domaine, données (Room), services d'arrière-plan 24/7 et parser/générateur PDF/DOCX.
4. Création des écrans Compose : Dashboard/Radar, Pipeline de Suivi, CV Profiler & Settings.
5. Compilation et test du projet.
6. Push sur GitHub et création de la GitHub Release avec l'APK attaché.
