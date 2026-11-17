# 🚀 Project State Ledger — JobHunter AI (JobPulse)

> **Repository** : `https://github.com/Anisghd-lab/JobHunter-AI`  
> **Package** : `com.ghdinteractivestudio.jobhunter`  
> **Release Officielle** : `https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0`  
> **APK Direct** : `JobHunter-AI-v1.0.0.apk`  
> **Orchestrateur** : `agency-agents-orchestrator`  
> **Lead Mobile & UI** : `agency-mobile-app-builder` & `liquid-glass-design`  
> **NLP & Documents** : `agency-resume-tailor` & `agency-document-generator`  
> **Email & Détection** : `agency-email-intelligence-engineer`  

---

## 1. 🎯 Objectifs Principaux & Vision
Application Android native (Kotlin + Jetpack Compose Liquid Glass) servant d'agent de recherche d'emploi autonome 24/7 :
- Couverture **Algérie nationale (Présentiel / Hybride)**, **Pays du Golfe / GCC (Émirats, Arabie Saoudite, Qatar, Koweït, Oman, Bahreïn)** et **International Worldwide (Remote & Présentiel / Visa)**.
- **Double CV Anglais & Français (`englishCv` + `frenchCv`)** : maximisation de la recherche sur le Radar et sélection automatique du CV original correspondant à la langue de l'offre, avec visualisation et suppression directe depuis l'interface.
- **0 donnée fictive** : uniquement des offres web réelles vérifiables (avec liens cliquables et tags) et un profil 100 % issu des CV importés.
- **Respect strict des dates limites (`DeadlineParser`)** : filtrage systématique des offres expirées ou obsolètes (> 45 jours), vérification temps réel HTTP + JSON-LD `validThrough`.
- **Lettres de motivation conformes au standard MLDP (`MldpCoverLetterData`)** : date & lieu tout en haut à droite, filtrage sémantique ciblé sur le poste (`selectTargetedProfile`), rendus texte, PDF vectoriel et Word `.docx`.
- **Contrôle IA Total Email & Suivi** : envoi SMTP résilient multi-ports (`465 SSL <-> 587 STARTTLS` + test live), lecture IMAP TLS `993` en lecture seule pour détecter les réponses (Entretien / Refus) et relance automatique IA `J+5`.

---

## 2. ✅ État d'Avancement Réel
- [x] **Sources Golfe / GCC (`JobScanner`, `RadarScreen`, `DeadlineParser`)** :
  - `fetchTanqeebGccJobs` (UAE, Arabie Saoudite, Qatar, Koweït, Oman, Bahreïn + requêtes ciblées EN), `fetchQatarLivingJobs` (JSON-LD `JobPosting`), `fetchGulfGreenhouseBoard` (`Careem`, `Tamara`) et `fetchGulfAshbyBoard` (`Lean Technologies`).
  - Support des dates relatives (`1 hour ago`, `2 days ago`, `Today`) dans `DeadlineParser` + filtre dédié **🏜️ Golfe** dans `RadarScreen`.
- [x] **Double Emplacement CV Anglais & Français (`UserProfile`, `CvScreen`, `MainActivity`, `ApplicationAutomationManager`)** :
  - Gestion de deux slots persistés (`CvDocumentSlot` EN & FR) avec sauvegarde binaire dans `files/source_cv/`.
  - Boutons uniformes **Importer / Remplacer**, **Voir** (ouverture directe PDF/DOCX + lecture texte EN/FR) et **Suppr.** depuis `Mon CV`.
  - Recherche combinée (`combinedSearchCorpus()`) et sélection automatique de la langue du CV selon l'annonce tout en respectant **CV Original par défaut** (sauf modification manuelle/IA ou exigence `.docx`).
- [x] **Design System Liquid Glass Bimodal & UI/UX (`Palette`, `Theme`, `Glass`, `GlassTabBar`)** :
  - Fond atmosphérique translucide `LiquidGlassBackdrop` identique en qualité entre Dark Obsidian OLED et Light Mode Crystal Givré.
  - Résolution définitive du contraste (suppression des fonds blancs/émeraude illisibles avec texte blanc grâce aux tokens `emeraldSolidBg`, `blueSolidBg`, `GlassSolidButton`, `GlassSoftButton`, `GlassFilterChip` et `glassFieldColors()`).
  - Uniformisation de la hauteur (`42dp` / `38dp`), des espacements et troncature sécurisée (`maxLines = 1`) sur tous les boutons sans débordement.
- [x] **Module Email Autonome & Liaison Appli Yahoo (`EmailDispatcher`, `ApplicationAutomationManager`, `JobHunterForegroundService`, `PipelineScreen`, `AgentScreen`)** :
  - Support `AUTH PLAIN` + `AUTH LOGIN` sur SMTP `465`/`587`, normalisation automatique des App Passwords (espaces Yahoo/Gmail), bascule auto de port, et diagnostic clair en cas de `535`.
  - Liaison directe avec l'application **Yahoo Mail installée** (`<queries>`, `AccountManager` picker, envoi pré-rempli direct sans sélecteur via `com.yahoo.mobile.client.android.mail` + lien direct générateur App Password Yahoo `login.yahoo.com/myaccount/security`).
  - Lecteur **IMAP TLS `993`** (`EXAMINE INBOX`) + classification IA (`classifyRecruiterReply`) + génération & envoi automatique de relance IA (`generateFollowUpEmail`, `Relancer IA`, `Suivi Réponses & Relances IA`).
  - Migration Room explicite `MIGRATION_3_4` (version 4 : `followUpCount`, `lastFollowUpAt`, `lastReplySummary`).

---

## 3. ⚠️ Dettes Techniques & Points de Vigilance
- L'envoi silencieux et le suivi IMAP autonome nécessitent un **Mot de passe d'application** Yahoo/Gmail renseigné dans **Agent 24/7 → Configuration Email** (testable en 1 clic via *Tester Connexion Email*).
- Clés API personnelles provisionnées localement dans le stockage privé du smartphone pour ne jamais exposer de secret dans le dépôt GitHub public.
- Tests unitaires JVM locaux (`app/src/test`) absents ; validation assurée par compilation complète et exécution directe sur appareil physique.

---

## 4. 📦 Assets & Livrables
- **GitHub Repository** : [https://github.com/Anisghd-lab/JobHunter-AI](https://github.com/Anisghd-lab/JobHunter-AI)
- **GitHub Release v1.0.0** : [https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0](https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0)
- **Fichier APK Local** : [JobHunter-AI-v1.0.0.apk](file:///root/JobHunter-AI-v1.0.0.apk)
