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
- Couverture **Algérie nationale (Présentiel / Hybride)** + **International Worldwide (Remote & Présentiel / Visa)**.
- **0 donnée fictive** : uniquement des offres web réelles vérifiables (avec liens cliquables et tags) et un profil 100 % issu du CV importé.
- **0 texte illisible** : assainissement HTML/RSS multi-passe (`HtmlSanitizer`) + extraction PDF vectorielle native (`PDFBox Android`) et DOCX (`ZipInputStream` + `XmlPullParser`).
- **Moteur IA 100 % Gratuit Multi-Fournisseurs** avec bascule automatique (OpenRouter Free, xKIRO Free Tier, Bytez Free, Gemini AI Studio Free, Ollama Local).
- **Automatisation complète** : Comparaison intelligente offre/CV -> Génération lettre de motivation adaptée (FR/EN) + CV ciblé au format exigé (PDF ou `.docx`) -> Envoi direct Email (SMTP / OAuth2 Gmail / App Intent) ou préparation guidée sur portail Web.

---

## 2. ✅ État d'Avancement Réel
- [x] **Agent Autonome 24/7** : `JobHunterForegroundService` (`DATA_SYNC`), réveil `AlarmManager` sécurisé Android 14+ et redémarrage automatique `BootReceiver`.
- [x] **Moteur Documentaire Bivalent (PDF & Word .docx)** :
  - Extraction PDF via `com.tom-roush:pdfbox-android:2.0.27.0` (`FlateDecode` et polices décodés).
  - Extraction DOCX via `ZipInputStream` + `XmlPullParser` (`<w:t>`).
  - Génération PDF vectorielle native (`PdfGenerator`) et Word `.docx` (`DocxGenerator`).
  - Partage et ouverture directe des pièces jointes via `androidx.core.content.FileProvider`.
- [x] **IA Gratuite Multi-Fournisseurs & Parsing Intelligent (`LlmClient`, `SettingsRepository`, `CvTailoringEngine`)** :
  - Catalogue strictement limité aux modèles gratuits (`openrouter/free`, `nvidia/nemotron-3.5-lightning:free`, `google/gemma-4-31b-it:free`, `mistralai/ministral-14b`, `cohere/command-a`, `mistralai/mistral-large-2512`, etc.).
  - Fallback automatique multi-fournisseurs sans fuite de secrets dans le dépôt Git.
  - Analyse IA fine du CV (`parseCvWithIntelligence`), comparaison offre/CV (`analyzeMatch`), adaptation éthique (`tailorCvContent`) et rédaction de lettre (`generateCoverLetter`).
- [x] **Radar Web Multi-Sources 100 % Réel (`JobScanner`, `HtmlSanitizer`, `RadarScreen`)** :
  - **Algérie** : Emploitic DZ (`__NEXT_DATA__` + URL canoniques vérifiées, Wilaya, CDI/CDD, Présentiel/Hybride).
  - **International** : Arbeitnow (Présentiel/Visa/Remote Europe), Remotive, Jobicy, Himalayas, RemoteOK, WeWorkRemotely.
  - Filtres instantanés Tout / 🇩🇿 Algérie / 🌍 Monde / 🏢 Présentiel / 🏠 Remote + recherche par mots-clés.
- [x] **Automatisation & Suivi Réel (`ApplicationAutomationManager`, `PipelineScreen`, `EmailDispatcher`)** :
  - Support SMTP multi-lignes + STARTTLS port 587 / SSL port 465 (Yahoo, Gmail, Outlook) et Gmail REST OAuth2.
  - Suivi strictement filtré sur les candidatures réelles (`ANALYZED`, `READY_TO_SEND`, `APPLIED`, `INTERVIEW`, `REJECTED`) avec liens cliquables, tags et pièces jointes ouvrables.
- [x] **UI/UX Liquid Glass CodeStation Bimodale** : Thème Dark Obsidian OLED (`#0B0D12`) & Light Mode Givré (`#F8F9FA`) avec barre flottante `GlassTabBar`.

---

## 3. ⚠️ Dettes Techniques & Points de Vigilance
- Envoi automatique silencieux par SMTP nécessite que l'utilisateur renseigne son App Password Yahoo/Gmail dans `Agent 24/7` (sans cela, l'app ouvre l'appli Email pré-remplie avec CV + lettre attachés).
- Clés API personnelles provisionnées localement dans le stockage privé du smartphone via ADB pour ne jamais exposer de secret dans le dépôt GitHub public.

---

## 4. 📦 Assets & Livrables
- **GitHub Repository** : [https://github.com/Anisghd-lab/JobHunter-AI](https://github.com/Anisghd-lab/JobHunter-AI)
- **GitHub Release v1.0.0** : [https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0](https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0)
- **Fichier APK Local** : [JobHunter-AI-v1.0.0.apk](file:///root/JobHunter-AI-v1.0.0.apk)
