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
- **Respect strict des dates limites (`DeadlineParser`)** : filtrage systématique des offres expirées ou obsolètes (> 45 jours), vérification temps réel HTTP + JSON-LD `validThrough` avant analyse/postulation, archivage automatique et blocage de l'envoi.
- **Moteur IA 100 % Gratuit Multi-Fournisseurs** avec bascule automatique (OpenRouter Free, xKIRO Free Tier, Bytez Free, Gemini AI Studio Free, Ollama Local).
- **Lettres de motivation conformes au standard MLDP (`MldpCoverLetterData`)** : structure officielle en 7 parties et méthode narrative en 6 étapes (`http://www.missionlocaledp.org/emploi/lettre-de-motivation`), générées fidèlement en texte, PDF vectoriel et Word `.docx`.

---

## 2. ✅ État d'Avancement Réel
- [x] **Audit Technique & Fiabilité Android** :
  - Observateurs `CoroutineScope` liés au `lifecycleScope` dans `MainActivity` (zéro fuite mémoire).
  - Demande explicite `POST_NOTIFICATIONS` sur Android 13+, cooldown de scan (3–10 min), respect du toggle `agentActive247` au boot (`BootReceiver`) et dans `JobHunterApp`.
  - Sauvegarde locale du CV source dans `filesDir/source_cv/` préservée même lors d'une ré-analyse IA (`sourceCvPath`).
  - Tokens `surfaceVariant`, `onSurfaceVariant`, `outline`, `error`, `onError` ajoutés à `Theme.kt`.
  - Pagination & retours à la ligne corrigés dans `PdfGenerator` (titres longs et `\r?\n`) et `DocxGenerator` (paragraphes distincts `<w:p>` + `xml:space="preserve"`).
  - Migration Room explicite `MIGRATION_2_3` (`publishedAt`, `deadlineAt`, `deadlineLabel`) sans perte du profil CV ni des réglages.
- [x] **Respect des Dates Limites (`DeadlineParser`, `JobScanner`, `JobOfferDao`)** :
  - Parsing multi-format : Unix (`Himalayas`, `Arbeitnow`), ISO-8601 (`Emploitic DZ`, `RemoteOK`, `Remotive`), RFC-2822 (`Jobicy`, `WeWorkRemotely`), JSON-LD `validThrough`, marqueurs `"offre expirée"` et phrases FR/EN (`avant le`, `date limite`, `deadline`, `valable X jours`).
  - Purge SQL `purgeExpiredDetectedOffers(...)` + vérification live HTTP 404/410 dans `ApplicationAutomationManager.verifyLiveOfferAndArchiveIfExpired(...)`.
  - Badges d'échéance (`DeadlineParser.formatDaysRemaining(...)`) affichés dans `RadarScreen` et `PipelineScreen` avec verrouillage d'envoi sur offres expirées.
- [x] **Lettre de Motivation Standard MLDP (`MldpCoverLetter`, `CvTailoringEngine`, `PdfGenerator`, `DocxGenerator`)** :
  - Modèle structuré en 7 parties : (1) Coordonnées expéditeur haut-gauche, (2) Destinataire droite, (3) Ville & date à droite, (4) `Objet :` gras à gauche + formule d'appel, (5) 4–5 paragraphes avec alinéa (présentation/demande, valorisation entreprise/attentes, valorisation candidat/preuves CV, sollicitation d'entretien + CV ci-joint), (6) Formule de politesse, (7) Signature bas-droite.
  - Rendu PDF vectoriel deux colonnes + alinéas (`generateMldpCoverLetterPdf`) et Word `.docx` (`generateMldpCoverLetterDocx`).
- [x] **Agent Autonome 24/7, Radar Multi-Sources & Envoi Email** :
  - `JobHunterForegroundService` (`DATA_SYNC`), réveil `AlarmManager` sécurisé Android 14+, SMTP STARTTLS/SSL + Gmail OAuth2.

---

## 3. ⚠️ Dettes Techniques & Points de Vigilance
- Envoi automatique silencieux par SMTP nécessite que l'utilisateur renseigne son App Password Yahoo/Gmail dans `Agent 24/7` (sans cela, l'app ouvre l'appli Email pré-remplie avec CV + lettre attachés).
- Clés API personnelles provisionnées localement dans le stockage privé du smartphone via ADB pour ne jamais exposer de secret dans le dépôt GitHub public.
- Tests unitaires JVM locaux à enrichir progressivement pour couvrir `DeadlineParser` et `MldpCoverLetterData`.

---

## 4. 📋 Ce qu'il reste à faire
- [ ] Vérifier compilation `./gradlew assembleDebug` + publication APK sur GitHub Release `v1.0.0` + déploiement ADB sur smartphone dès reconnexion Wireless Debugging.

---

## 5. 📦 Assets & Livrables
- **GitHub Repository** : [https://github.com/Anisghd-lab/JobHunter-AI](https://github.com/Anisghd-lab/JobHunter-AI)
- **GitHub Release v1.0.0** : [https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0](https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0)
- **Fichier APK Local** : [JobHunter-AI-v1.0.0.apk](file:///root/JobHunter-AI-v1.0.0.apk)
