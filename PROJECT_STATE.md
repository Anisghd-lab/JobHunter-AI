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

## 1. 🎯 Objectifs & Réalisations
- [x] **Agent Autonome 24/7 sur Smartphone** : Implémenté via `JobHunterForegroundService` avec notification d'activité permanente et réveil périodique natif Android via `AlarmManager` (`JobHunterScheduler`).
- [x] **Moteur Documentaire Bivalent (PDF & Word .docx)** :
  - Ingestion sémantique du CV source (formats `.pdf` et `.docx`).
  - Détection automatique dans le texte de l'offre du format requis par l'employeur.
  - Génération vectorielle PDF native via `android.graphics.pdf.PdfDocument`.
  - Génération OpenXML Word `.docx` standardisé sans dépendances externes via `DocxGenerator`.
  - Tailoring de CV éthique sans invention d'expériences (`agency-resume-tailor`).
  - Rédaction de lettres de motivation percutantes selon les règles du poste (`CvTailoringEngine`).
- [x] **Double Envoi d'Email** :
  - Support OAuth2 Gmail REST API (`POST /gmail/v1/users/me/messages/send`).
  - Client direct SMTP sur socket TLS chiffré (ports 465 / 587) avec encodage multipart et pièces jointes automatiques.
- [x] **Tableau de Suivi & Pipeline** :
  - Écran Pipeline avec métriques en temps réel (Envoyées, Entretiens, À Valider).
  - Persistance Room Database chiffrable (`JobOfferEntity`, `UserProfileEntity`).
- [x] **UI/UX Liquid Glass CodeStation Bimodale** :
  - Thème Dark Mode Obsidian Glass OLED (`#0B0D12`).
  - Thème Light Mode Blanc Givré (`#F8F9FA`).
  - Barre de navigation flottante `GlassTabBar` identique (capsule dépolie avec pilule animée `animateDpAsState`).
- [x] **Publication & Release GitHub** : Dépôt public créé, code source poussé, tag `v1.0.0` et GitHub Release avec APK attaché publiée.
- [x] **Résolution du Bug Critique d'Ingestion CV** :
  - Intégration de `com.tom_roush:pdfbox-android:2.0.27.0` avec initialisation `PDFBoxResourceLoader.init(context)`.
  - Décompression intégrale des flux PDF compressés `FlateDecode` et des tables de polices.
  - Décompression native des archives DOCX via `ZipInputStream` et parsing XML pur via `XmlPullParser` sur `word/document.xml` sans aucune dépendance lourde.
  - Détection automatique et fiable des types MIME et extensions.
  - Validation sur terminal physique réel de l'extraction intégrale du CV source et de l'affichage du texte brut lisible.

---

## 2. 📦 Assets & Livrables
- **GitHub Repository** : [https://github.com/Anisghd-lab/JobHunter-AI](https://github.com/Anisghd-lab/JobHunter-AI)
- **GitHub Release v1.0.0** : [https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0](https://github.com/Anisghd-lab/JobHunter-AI/releases/tag/v1.0.0)
- **Fichier APK Local** : [JobHunter-AI-v1.0.0.apk](file:///root/JobHunter-AI-v1.0.0.apk) (18 Mo)
