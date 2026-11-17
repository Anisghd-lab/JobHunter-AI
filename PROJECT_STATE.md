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
- [x] **Module Email Autonome & Liaison Appli Yahoo (`EmailDispatcher`, `SettingsRepository`, `ApplicationAutomationManager`, `JobHunterForegroundService`, `PipelineScreen`, `AgentScreen`)** :
  - Support `AUTH PLAIN` + `AUTH LOGIN` sur SMTP `465`/`587`, arrêt immédiat sur `535` / `#AUTH005` ou socket fermé (suppression de la cascade `AUTH LOGIN` / bascule `:587` qui provoquait les verrouillages `#AUTH005`), et affichage direct du diagnostic serveur Yahoo exact.
  - Protection anti-blocage `smtpAuthPaused` (`canSendSilently()`): suspension automatique des tentatives IMAP/relance en arrière-plan dès qu'une erreur `535`/`#AUTH005` survient, réinitialisation automatique à la modification du mot de passe ou test réussi, et bouton `✕` pour vider instantanément l'App Password.
  - Liaison directe avec l'application **Yahoo Mail installée** (`<queries>`, `AccountManager` picker, validation directe de la liaison dans *Tester Connexion Email* lorsque l'App Password est vide, envoi pré-rempli direct via `com.yahoo.mobile.client.android.mail` + lien direct générateur App Password Yahoo `login.yahoo.com/myaccount/security`).
  - Lecteur **IMAP TLS `993`** (`EXAMINE INBOX`) + classification IA (`classifyRecruiterReply`) + génération & envoi automatique de relance IA (`generateFollowUpEmail`, `Relancer IA`, `Suivi Réponses & Relances IA`).
  - Migration Room explicite `MIGRATION_3_4` (version 4) et `MIGRATION_4_5` (version 5 : `autoApplied`, `lastAutoAttemptAt`, `lastReplyFrom`, `lastReplySubject`, `lastReplyAt`, `lastReplyMessageId` + assainissement SQL des faux emails `accommodations`/`privacy`).
  - **Lettres MLDP Monolingues & Lieu Épuré (`CvTailoringEngine`, `MldpCoverLetter`)** : extraction automatique de la ville seule (`extractCityName` -> `Oum El Bouaghi` sans rue/code postal en haut à droite), séparation stricte des prompts 100 % FR vs 100 % EN ancrés sur le slot CV correspondant (`frenchCv`/`englishCv`), garde anti-mélange par paragraphe (`isStrictlyTargetLanguage`) + pièces jointes (`Lettre_Motivation_*.pdf` + CV FR vs `Cover_Letter_*.pdf` + CV EN).
- [x] **Moteur d'Auto-Candidature Quotas & Suivi Boîte Temps Réel (`ApplicationAutomationManager`, `EmailDispatcher`, `HtmlSanitizer`, `JobHunterForegroundService`, `PipelineScreen`, `AgentScreen`)** :
  - **Humanize IA (`CvTailoringEngine`)** : directive de style humain naturel sur toutes les lettres MLDP et relances e-mail + post-traitement `humanizeBusinessText` (zéro tirets cadratins, zéro cliché robotique IA).
  - **Enrichissement & Filtrage Email (`HtmlSanitizer`, `JobScanner`, `runAutonomousAutoApplyBatch`)** : liste noire stricte des adresses non-recrutement, garde anti-décalage métier dans `scoreWithCvDeterministically` (plafonnement des titres hors-domaine), scan Emploitic 3 pages & Tanqeeb fiches live, découverte live jusqu'à `50` pages/cycle sur top `150` candidates, déduplication inter-plateformes, seuil configurable abaissé à **`50%`** (`50 / 60 / 70 / 80%`), espacement naturel dicté par le temps de génération des lettres et quotas jusqu'à **`50` envois/jour** (`12` max/cycle).
  - **Scraper Web Direct Recruteurs & Entreprises Officielles (`JobScanner`, `HtmlSanitizer`, `JobOfferDao`, `RadarScreen`)** :
    - **Scraper LinkedIn Guest Sans Barrière d'Authentification (`fetchLinkedInJobs`)** : Exploitation de l'API publique invité `seeMoreJobPostings/search` pour Algérie (`location=Algeria`), Pays du Golfe (UAE, Arabie Saoudite, Qatar) et Monde entier, suivie de l'extraction des fiches descriptives complètes `jobPosting/$jobId` pour récupérer les emails directs des recruteurs et liens employeurs.
    - **Priorité 1 (Emails Recruteurs Directs)** : Scraper multi-flux WeWorkRemotely (Support, Management, Tech, All Other - 40+ emails directs), Scraper multi-pays Tanqeeb (`algeria.tanqeeb.com`, UAE, KSA, Koweït, Qatar avec requêtes ciblées `send cv`, `hr email`, `recrutement cv`, `careers email`, extraction directe des cartes HTML), API multi-catégories Jobicy (Support, Admin, Management, Marketing, Operations, Finance - 18+ emails), flux Remotive & RemoteOK, et scan approfondi Emploitic DZ (3 pages par requête).
    - **Décodeur Anti-Obscurcissement Email (`HtmlSanitizer`)** : Déchiffrement direct des adresses Cloudflare `data-cfemail` (XOR hexadécimal), unmasking `[at]`, `(at)`, `[chez]`, `[dot]`, extraction `mailto:`, et extension complète des tokens de recrutement FR/EN (`recrutement`, `drh`, `rh`, `candidature`, `carrieres`, `embauche`, `recruitment`, `hiring`, `careers`).
    - **Crawler de Sites Officiels d'Entreprises (`crawlCompanyCareerWebsite`)** : Pour chaque offre comportant un lien ou nom de domaine d'entreprise, exploration automatique des pages `/contact`, `/careers`, `/recrutement`, `/jobs`, `/carrieres` pour extraire l'adresse e-mail directe du recruteur.
    - **Priorité 2 (Portails Web Tiers & Sauvegardes)** : QatarLiving, tableaux ATS Greenhouse/Ashby (Careem, Tamara, LeanTech), Arbeitnow et Himalayas relégués en rang secondaire comme dossiers web assistés.
    - **Tri Déterministe du Radar & Filtre Dédié** : Les offres disposant d'un e-mail recruteur vérifié remontent automatiquement au sommet du Radar (`CASE WHEN recruiterEmail LIKE '%@%' THEN 1 ELSE 0 END DESC`). Ajout du filtre capsule `"📧 Email Direct"` et compteur live dans l'en-tête du Radar.
  - **Moteur 24/7 Non-Stop Haute Fréquence (`JobHunterForegroundService`)** :
    - Intervalle du cycle d'arrière-plan accéléré de 30 minutes à **8 minutes** (`delay(8 * 60 * 1000L)`), permettant jusqu'à **180 cycles quotidiens** ininterrompus de scan, génération de lettres humanisées, envoi e-mail (quota 50/jour) et écoute IMAP des réponses d'entretiens.
  - **Assistant Portails Web & Tests de Session (`PortalSessionRegistry`, `WebPortalAssistantDialog`, `AgentScreen`)** : navigateur intégré persistant (`CookieManager`) + barre de comptes portails (`Emploitic`, `Tanqeeb`, `QatarLiving`, `LinkedIn`) avec bouton **Créer / Connecter**, test HTTP/cookie/JWT réel en direct (incluant le cookie de session `li_at` pour LinkedIn et `qat`/`qat_refresh` pour QatarLiving), remplissage des champs visibles en 1 clic, export `CV + Lettre` dans Téléchargements, et lecteur d'OTP/lien de vérification IMAP en lecture seule tout en conservant la validation humaine des CAPTCHAs.
  - **Tableau `Suivi` strict (`PipelineScreen`)** : exclut les offres hors-profil rejetées en arrière-plan (`ANALYZED`) pour n'afficher que les candidatures réelles (`APPLIED`, `INTERVIEW`, `REJECTED`, `READY_TO_SEND`).
  - **Lecteur IMAP Avancé (`EmailDispatcher`)** : `SEARCH SINCE` 14j, décodage RFC 2047 (`Base64` + `Quoted-Printable`, UTF-8/ISO-8859-1, en-têtes repliés), décodage corps Quoted-Printable, `EXAMINE INBOX` + `BODY.PEEK` 100% lecture seule.
  - **Tableau `Suivi` & Gouvernance (`PipelineScreen`, `AgentScreen`)** : 4 compteurs temps réel (`Envoyées`, `Entretiens`, `Réponses`, `Web`), filtres capsules (`Toutes`, `Auto`, `Réponses`, `Entretiens`, `Web`), badge `AUTO`, carte détaillée de réponse boîte de réception (`De`, `Objet`, date, verdict IA), notifications Android dédiées sur envoi auto / entretien, bouton **"Lancer Auto-Candidature & Suivi"**, et bouton **"Remettre à 0"** sans perte des CVs ni des identifiants.

---

## 3. ⚠️ Dettes Techniques & Points de Vigilance
- L'envoi silencieux en arrière-plan et le suivi IMAP autonome exigent un **Mot de passe d'application** valide (16 caractères) renseigné dans **Agent 24/7 → Configuration Email** (car Android interdit aux applications tierces de lire le jeton OAuth privé de l'application Yahoo Mail). Sans App Password (champ vidé via `✕`) + **Connecter Appli Yahoo** activé, l'envoi bascule sans erreur `535` sur l'application Yahoo Mail pré-remplie en 1 clic.
- En cas de verrouillage temporaire Yahoo `#AUTH005`, attendre ~15 minutes avant de retester ou régénérer un nouveau Mot de passe d'application.
- Clés API personnelles provisionnées localement dans le stockage privé du smartphone pour ne jamais exposer de secret dans le dépôt GitHub public.
- Tests unitaires JVM locaux (`app/src/test`) absents ; validation assurée par compilation complète et exécution directe sur appareil physique.

---

## 4. 📦 Assets & Livrables
- **GitHub Repository** : [https://github.com/Anisghd-lab/JobHunter-AI](https://github.com/Anisghd-lab/JobHunter-AI)
- **GitHub Release v1.0.0** : [https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0](https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0)
- **Fichier APK Local** : [JobHunter-AI-v1.0.0.apk](file:///root/JobHunter-AI-v1.0.0.apk)
