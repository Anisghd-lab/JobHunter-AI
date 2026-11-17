package com.ghdinteractivestudio.jobhunter.domain.ai

import com.ghdinteractivestudio.jobhunter.data.local.AppSettings
import com.ghdinteractivestudio.jobhunter.domain.document.MldpCoverLetterData
import com.ghdinteractivestudio.jobhunter.domain.email.IncomingReplyEmail
import com.ghdinteractivestudio.jobhunter.domain.model.CertificationItem
import com.ghdinteractivestudio.jobhunter.domain.model.EducationItem
import com.ghdinteractivestudio.jobhunter.domain.model.ExperienceItem
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

@Serializable
data class MldpLetterAiPayload(
    val isFrench: Boolean = true,
    val subject: String = "",
    val salutation: String = "Madame, Monsieur,",
    val paragraphPresentAndRequest: String = "",
    val paragraphExpectationsAndCompany: String = "",
    val paragraphSelfValorization: String = "",
    val paragraphInterviewAndCvInvite: String = "",
    val closingFormula: String = ""
)

@Serializable
data class MatchAnalysisResponse(
    val matchScore: Int = 50,
    val verdict: String = "MOYEN", // "CONVIENT", "MOYEN", "NON_ADAPTE"
    val reasoning: String = "",
    val matchingPoints: List<String> = emptyList(),
    val missingRequirements: List<String> = emptyList(),
    val detectedFormatRequirement: String = "ANY" // "PDF", "DOCX", "ANY"
)

@Serializable
data class TailoredCvResponse(
    val targetTitle: String = "",
    val executiveSummary: String = "",
    val prioritizedSkills: List<String> = emptyList(),
    val tailoredHighlights: List<String> = emptyList()
)

class CvTailoringEngine(private val settings: AppSettings = AppSettings()) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Parses clean extracted CV text using Free LLM intelligence & delicacy.
     * ZERO fabrication rule strictly enforced.
     */
    suspend fun parseCvWithIntelligence(rawText: String): Result<UserProfile> {
        val systemInstruction = "Tu es un analyseur de CV expert et rigoureux. Tu extrais 100% fidèlement les informations réelles sans JAMAIS rien inventer."

        val prompt = """
Analyse le texte complet de ce CV avec intelligence et délicatesse et extrait TOUTES les informations réelles au format JSON strict.
RÈGLES ABSOLUES :
1. N'invente, ne complète ou ne suppose AUCUNE information absente du CV. Laisse "" ou [] si absent.
2. Sépare bien les compétences techniques/métier ("skills") des qualités humaines/transversales ("softSkills").
3. Extrais toutes les expériences professionnelles avec poste, entreprise, dates, lieu et missions réelles.
4. Extrais les diplômes ("educations") ET les formations/certifications ("certifications").
5. Conserve les langues avec leur niveau exact indiqué dans le CV (ex: "French: Fluent (C1)").

Format JSON strict attendu :
{
  "fullName": "Nom complet exact",
  "email": "Email exact",
  "phone": "Téléphone exact",
  "address": "Ville / Pays exacts",
  "title": "Titre professionnel principal",
  "summary": "Résumé professionnel réel du CV",
  "skills": ["Compétence 1", "Compétence 2"],
  "softSkills": ["Qualité 1", "Qualité 2"],
  "experiences": [
    {
      "title": "Intitulé exact du poste",
      "company": "Entreprise / Projet exact",
      "period": "Période exacte",
      "location": "Lieu si mentionné",
      "description": "Missions et réalisations réelles",
      "keySkills": ["Compétence utilisée"]
    }
  ],
  "educations": [
    {
      "degree": "Diplôme exact",
      "institution": "Établissement",
      "year": "Année",
      "description": "Précisions si existantes"
    }
  ],
  "certifications": [
    {
      "name": "Nom certification ou formation continue",
      "issuer": "Organisme",
      "year": "Année"
    }
  ],
  "languages": ["Langue et niveau"]
}

Texte intégral du CV à analyser :
$rawText

Réponds UNIQUEMENT par l'objet JSON pur sans texte autour.
        """.trimIndent()

        return LlmClient.generateWithFreeFallback(settings, prompt, systemInstruction).mapCatching { resp ->
            val cleanJson = extractJsonObject(resp.text)
            json.decodeFromString<UserProfile>(cleanJson)
        }
    }

    /**
     * Backward-compatible alias used if invoked directly
     */
    suspend fun parseCv(rawText: String): Result<UserProfile> = parseCvWithIntelligence(rawText)

    /**
     * Evaluates job offer against the candidate's REAL CV using Free LLMs.
     */
    suspend fun analyzeMatch(offer: JobOffer, profile: UserProfile): Result<MatchAnalysisResponse> {
        val systemInstruction = "Tu es un évaluateur RH honnête et précis. Tu compares un vrai CV avec une vraie offre d'emploi."

        val prompt = """
Compare honnêtement ce profil réel avec cette offre d'emploi web :

1. Calcule un "matchScore" réaliste entre 0 et 100.
2. Détermine le "verdict" :
   - "CONVIENT" si score >= 60 (le candidat a les compétences clés ou transférables solides)
   - "MOYEN" si score entre 40 et 59
   - "NON_ADAPTE" si score < 40
3. Vérifie le format exigé dans l'offre ("PDF", "DOCX", ou "ANY" si non précisé).

Profil Réel du Candidat :
Nom : ${profile.fullName}
Localisation : ${profile.address}
Titre : ${profile.title}
Résumé : ${profile.summary}
Compétences : ${(profile.skills + profile.softSkills).distinct().joinToString(", ")}
Langues : ${profile.languages.joinToString(", ")}
Expériences : ${profile.experiences.joinToString(" | ") { "${it.title} (${it.company}, ${it.period}): ${it.description.take(200)}" }}
Formations : ${profile.educations.joinToString(" | ") { "${it.degree} - ${it.institution} (${it.year})" }}

Offre d'Emploi Web :
Titre : ${offer.title}
Entreprise : ${offer.company}
Lieu : ${offer.location} (Remote: ${offer.isRemote}, Mode: ${offer.workMode})
Description :
${offer.description.take(2200)}

Réponds UNIQUEMENT en JSON strict :
{
  "matchScore": 78,
  "verdict": "CONVIENT",
  "reasoning": "Explication claire en 2 phrases de l'adéquation ou des écarts",
  "matchingPoints": ["Point fort réel 1", "Point fort réel 2"],
  "missingRequirements": ["Critère manquant éventuel"],
  "detectedFormatRequirement": "ANY"
}
        """.trimIndent()

        return LlmClient.generateWithFreeFallback(settings, prompt, systemInstruction).mapCatching { resp ->
            val cleanJson = extractJsonObject(resp.text)
            json.decodeFromString<MatchAnalysisResponse>(cleanJson)
        }
    }

    /**
     * Tailors real CV presentation ethcially for the specific offer.
     */
    suspend fun tailorCvContent(offer: JobOffer, profile: UserProfile): Result<TailoredCvResponse> {
        val systemInstruction = "Tu adaptes la mise en valeur d'un CV sans JAMAIS inventer d'expérience ou de diplôme."

        val prompt = """
Adapte l'accroche et la priorisation des compétences du CV réel pour cette offre précise.
INTERDICTION ABSOLUE D'INVENTER des postes, entreprises ou diplômes.

Candidat : ${profile.fullName} | ${profile.title}
Compétences réelles : ${(profile.skills + profile.softSkills).distinct().joinToString(", ")}
Expériences réelles : ${profile.experiences.joinToString(" | ") { "${it.title} chez ${it.company} (${it.period}): ${it.description.take(250)}" }}

Offre ciblée : ${offer.title} chez ${offer.company} (${offer.location})
Description : ${offer.description.take(1800)}

Réponds UNIQUEMENT en JSON strict :
{
  "targetTitle": "Titre du candidat adapté au poste ciblé",
  "executiveSummary": "Résumé professionnel authentique ciblant le poste",
  "prioritizedSkills": ["Compétence réelle prioritaire 1", "Compétence 2"],
  "tailoredHighlights": ["Point fort réel reformulé pour le recruteur 1", "Point fort 2"]
}
        """.trimIndent()

        return LlmClient.generateWithFreeFallback(settings, prompt, systemInstruction).mapCatching { resp ->
            val cleanJson = extractJsonObject(resp.text)
            json.decodeFromString<TailoredCvResponse>(cleanJson)
        }
    }

    /**
     * Generates a smart, targeted Cover Letter strictly following MLDP 7-part spatial structure:
     * enforces 100% single-language output (strictly French OR strictly English matching the offer),
     * anchors facts in the same-language CV slot when uploaded, and verifies zero language mixing.
     */
    suspend fun generateMldpCoverLetter(offer: JobOffer, profile: UserProfile): MldpCoverLetterData {
        val migratedProfile = profile.migrateLegacySlotIfNeeded()
        val offerLangFrench = looksLikeFrenchOffer(offer)
        val targeted = selectTargetedProfile(offer, migratedProfile)

        val sameLangCvExcerpt = (if (offerLangFrench) {
            migratedProfile.frenchCv?.rawText
        } else {
            migratedProfile.englishCv?.rawText
        }).orEmpty().take(1800)

        val skillsPrompt = targeted.skills
            .map { localizeCvTerm(it, offerLangFrench) }
            .joinToString(", ")
            .ifBlank { if (offerLangFrench) "Gestion administrative, coordination, organisation" else "Administration, coordination, operational organization" }

        val expPrompt = targeted.experiences.joinToString("; ") {
            val localizedTitle = localizeCvTerm(it.title, offerLangFrench)
            "$localizedTitle — ${it.company} (${it.period}): ${it.description.take(200)}"
        }.ifBlank {
            if (offerLangFrench) "Expérience confirmée en coordination et gestion opérationnelle"
            else "Proven experience in operational coordination and administration"
        }

        val certPrompt = targeted.certifications.joinToString("; ") {
            "${localizeCvTerm(it.name, offerLangFrench)}${if (it.issuer.isNotBlank()) " (${it.issuer})" else ""}"
        }
        val eduPrompt = targeted.education?.let {
            "${localizeCvTerm(it.degree, offerLangFrench)}${if (it.institution.isNotBlank()) " (${it.institution})" else ""}"
        } ?: ""

        val (systemInstruction, prompt) = if (offerLangFrench) {
            val sys = "Tu rédiges des lettres de motivation exécutives ultra-ciblées selon la structure MLDP, STRICTEMENT en FRANÇAIS uniquement. Aucun mot ni phrase d'anglais n'est toléré dans la réponse."
            val prm = """
Rédige les 4 paragraphes d'une lettre de motivation ciblée selon la méthodologie MLDP au format JSON strict.

RÈGLES ABSOLUES :
1. LANGUE UNIQUE : Rédige 100 % en FRANÇAIS PROFESSIONNEL soutenu. Traduis impérativement en français tout intitulé ou compétence du profil formulé en anglais. INTERDICTION STRICTE de laisser la moindre phrase ou expression en anglais.
2. NE RÉCITE JAMAIS TOUT LE CV : cite UNIQUEMENT les compétences, l'expérience et la formation ciblées ci-dessous qui répondent directement au poste "${offer.title}".
3. Pas d'ouverture robotique « Je suis [Nom] » (le nom figure déjà en en-tête et en signature).
4. Concision & impact (2 à 4 phrases fluides par paragraphe, ~40 à 60 mots, format 1 page A4) :
   - "paragraphPresentAndRequest" : Accroche directe reliant le profil ciblé à la candidature pour "${offer.title}" chez ${offer.company}.
   - "paragraphExpectationsAndCompany" : Intérêt concret pour ${offer.company} / les enjeux du poste et la valeur ajoutée apportée.
   - "paragraphSelfValorization" : Mise en valeur UNIQUEMENT des compétences visées par l'offre ($skillsPrompt) appuyée par l'expérience pertinente${if (certPrompt.isNotBlank()) " et la certification utile" else ""}.
   - "paragraphInterviewAndCvInvite" : Demande d'entretien concise et invitation naturelle à consulter le CV en français ci-joint.
5. "closingFormula" : Formule de politesse classique en français.

Éléments réels du candidat pré-filtrés pour cette offre :
- Profil ciblé : ${localizeCvTerm(targeted.title, true)}
- Compétences retenues : $skillsPrompt
- Expérience(s) retenue(s) : $expPrompt
${if (certPrompt.isNotBlank()) "- Certification(s) pertinente(s) : $certPrompt\n" else ""}${if (eduPrompt.isNotBlank() && targeted.educationDirectFit) "- Formation directe : $eduPrompt\n" else ""}${if (targeted.languages.isNotEmpty()) "- Langues pertinentes : ${targeted.languages.joinToString(", ")}\n" else ""}${if (sameLangCvExcerpt.isNotBlank()) "\nExtrait du CV français du candidat (source factuelle prioritaire) :\n$sameLangCvExcerpt\n" else ""}
Offre ciblée :
Poste : ${offer.title}
Entreprise : ${offer.company}
Lieu : ${offer.location}
Description :
${offer.description.take(1800)}

Réponds UNIQUEMENT par cet objet JSON pur (sans markdown) :
{
  "isFrench": true,
  "subject": "Objet : Candidature au poste de ${offer.title}",
  "salutation": "Madame, Monsieur,",
  "paragraphPresentAndRequest": "...",
  "paragraphExpectationsAndCompany": "...",
  "paragraphSelfValorization": "...",
  "paragraphInterviewAndCvInvite": "...",
  "closingFormula": "Je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations distinguées."
}
            """.trimIndent()
            sys to prm
        } else {
            val sys = "You write concise executive MLDP cover letters STRICTLY in natural professional ENGLISH ONLY. Never write a single French sentence, header, or closing phrase."
            val prm = """
Write the 4 body paragraphs of a tailored cover letter in strict JSON following the 7-part business letter structure.

STRICT NON-NEGOTIABLE RULES:
1. SINGLE LANGUAGE ONLY: Write 100% in natural professional ENGLISH ONLY. If any detail from the candidate context below appears in French, translate it smoothly into natural English. NEVER leave any French sentence, connector, or greeting anywhere in the JSON.
2. NEVER DUMP THE WHOLE CV: cite ONLY the pre-selected relevant skills, experience, and credentials below that directly answer "${offer.title}". Zero fabricated facts.
3. Avoid robotic openings like "My name is..." (your name is already in the header and signature).
4. Crisp executive pace (2 to 4 fluent sentences per paragraph, ~40 to 60 words each, fitting cleanly on 1 A4 page):
   - "paragraphPresentAndRequest": Direct opening connecting the targeted background to your application for "${offer.title}" at ${offer.company}.
   - "paragraphExpectationsAndCompany": Genuine alignment with ${offer.company}, its goals, and the key responsibilities of the role.
   - "paragraphSelfValorization": Concrete evidence highlighting ONLY the targeted competencies ($skillsPrompt) backed by relevant experience${if (certPrompt.isNotBlank()) " and relevant certification" else ""}.
   - "paragraphInterviewAndCvInvite": Confident request for an interview and invitation to review the attached English CV.
5. "closingFormula": Standard English business closing (e.g., "Thank you in advance for your time and consideration. I look forward to hearing from you. Yours sincerely,").

Pre-filtered real candidate context for THIS role only:
- Targeted profile: ${localizeCvTerm(targeted.title, false)}
- Selected relevant skills: $skillsPrompt
- Relevant experience: $expPrompt
${if (certPrompt.isNotBlank()) "- Relevant certification(s): $certPrompt\n" else ""}${if (eduPrompt.isNotBlank() && targeted.educationDirectFit) "- Direct education fit: $eduPrompt\n" else ""}${if (targeted.languages.isNotEmpty()) "- Relevant languages: ${targeted.languages.joinToString(", ")}\n" else ""}${if (sameLangCvExcerpt.isNotBlank()) "\nCandidate's English CV excerpt (primary factual reference):\n$sameLangCvExcerpt\n" else ""}
Target job posting:
Role: ${offer.title}
Company: ${offer.company}
Location: ${offer.location}
Description:
${offer.description.take(1800)}

Reply ONLY with valid pure JSON (no markdown code fences):
{
  "isFrench": false,
  "subject": "Subject: Application for ${offer.title}",
  "salutation": "Dear Hiring Manager,",
  "paragraphPresentAndRequest": "...",
  "paragraphExpectationsAndCompany": "...",
  "paragraphSelfValorization": "...",
  "paragraphInterviewAndCvInvite": "...",
  "closingFormula": "Thank you in advance for your time and consideration. I look forward to hearing from you. Yours sincerely,"
}
            """.trimIndent()
            sys to prm
        }

        val aiPayload = LlmClient.generateWithFreeFallback(settings, prompt, systemInstruction)
            .mapCatching { resp ->
                val cleanJson = extractJsonObject(resp.text)
                json.decodeFromString<MldpLetterAiPayload>(cleanJson)
            }.getOrNull()

        val deterministic = buildDeterministicMldpLetter(offer, migratedProfile, targeted, offerLangFrench)

        if (aiPayload == null || aiPayload.paragraphPresentAndRequest.isBlank()) {
            return deterministic
        }

        val isFr = offerLangFrench
        val rawAiParagraphs = listOf(
            aiPayload.paragraphPresentAndRequest,
            aiPayload.paragraphExpectationsAndCompany,
            aiPayload.paragraphSelfValorization,
            aiPayload.paragraphInterviewAndCvInvite
        )
        // Strictly reject/replace any single paragraph that leaked into the wrong language
        val validatedParagraphs = rawAiParagraphs.mapIndexed { idx, para ->
            val clean = para.trim()
            if (clean.length >= 25 && isStrictlyTargetLanguage(clean, isFr)) {
                clean
            } else {
                deterministic.bodyParagraphs.getOrElse(idx) { "" }
            }
        }.filter { it.isNotBlank() }

        val validSubject = aiPayload.subject.trim()
            .takeIf { it.isNotBlank() && isStrictlyTargetLanguage(it, isFr) }
            ?: deterministic.subject
        val validSalutation = aiPayload.salutation.trim()
            .takeIf { it.isNotBlank() && isStrictlyTargetLanguage(it, isFr) }
            ?: deterministic.salutation
        val validClosing = aiPayload.closingFormula.trim()
            .takeIf { it.isNotBlank() && isStrictlyTargetLanguage(it, isFr) }
            ?: deterministic.closingFormula

        return MldpCoverLetterData(
            senderName = deterministic.senderName,
            senderAddress = deterministic.senderAddress,
            senderPhone = deterministic.senderPhone,
            senderEmail = deterministic.senderEmail,
            recipientCompany = deterministic.recipientCompany,
            recipientAttention = deterministic.recipientAttention,
            recipientLocation = deterministic.recipientLocation,
            placeAndDate = deterministic.placeAndDate,
            subject = validSubject,
            salutation = validSalutation,
            bodyParagraphs = validatedParagraphs.ifEmpty { deterministic.bodyParagraphs },
            closingFormula = validClosing,
            signatureName = deterministic.signatureName,
            isFrench = isFr
        )
    }

    suspend fun generateCoverLetter(offer: JobOffer, profile: UserProfile): Result<String> =
        Result.success(generateMldpCoverLetter(offer, profile).toPlainText())

    private fun buildDeterministicMldpLetter(
        offer: JobOffer,
        profile: UserProfile,
        targeted: TargetedCvContext,
        isFrench: Boolean
    ): MldpCoverLetterData {
        val topExp = targeted.experiences.firstOrNull()
        val localizedTitle = localizeCvTerm(targeted.title.ifBlank { offer.title }, isFrench)
        val localizedExpTitle = topExp?.let { localizeCvTerm(it.title, isFrench) }.orEmpty()
        val skillsText = targeted.skills.take(4).joinToString(", ") { localizeCvTerm(it, isFrench) }
        val topCert = targeted.certifications.firstOrNull()?.let { localizeCvTerm(it.name, isFrench) }
        val edu = targeted.education?.let { localizeCvTerm(it.degree, isFrench) }

        val paragraphs = if (isFrench) {
            listOf(
                buildString {
                    when {
                        targeted.educationDirectFit && !edu.isNullOrBlank() ->
                            append("Titulaire d'un $edu")
                        topExp != null ->
                            append("Fort(e) d'une expérience confirmée en tant que $localizedExpTitle chez ${topExp.company}")
                        else ->
                            append("Professionnel(le) spécialisé(e) en $localizedTitle")
                    }
                    append(", je souhaite vous soumettre ma candidature pour le poste de ${offer.title} au sein de ${offer.company}.")
                },
                "Votre entreprise reconnue dans son secteur constitue un environnement stimulant où je pourrais m'investir pleinement sur les missions clés du poste, tout en contribuant avec rigueur à l'organisation et aux objectifs de vos équipes.",
                buildString {
                    if (topExp != null && skillsText.isNotBlank()) {
                        append("Mon parcours en qualité de $localizedExpTitle chez ${topExp.company} m'a permis de développer une solide maîtrise en $skillsText")
                    } else if (skillsText.isNotBlank()) {
                        append("Mes réalisations m'ont permis de consolider des compétences ciblées en $skillsText")
                    }
                    if (!topCert.isNullOrBlank()) {
                        append(", complétée par ma certification en $topCert")
                    }
                    append(".")
                    if (targeted.languages.isNotEmpty()) {
                        append(" La maîtrise des langues (${targeted.languages.joinToString(", ")}) viendra également appuyer mes échanges au quotidien.")
                    }
                }.trim(),
                "Je serais ravi(e) de vous rencontrer lors d'un entretien pour échanger sur ma contribution au sein de ${offer.company}. Vous trouverez ci-joint mon CV en français détaillant l'ensemble de mon parcours."
            )
        } else {
            listOf(
                buildString {
                    when {
                        targeted.educationDirectFit && !edu.isNullOrBlank() ->
                            append("Holding a $edu")
                        topExp != null ->
                            append("Drawing on my hands-on experience as $localizedExpTitle at ${topExp.company}")
                        else ->
                            append("As an experienced $localizedTitle")
                    }
                    append(", I am eager to apply for the ${offer.title} position at ${offer.company}.")
                },
                "${offer.company} stands out as an organization where I can immediately contribute to your team's objectives while bringing structure, reliability, and operational focus to the role.",
                buildString {
                    if (topExp != null && skillsText.isNotBlank()) {
                        append("In my role as $localizedExpTitle at ${topExp.company}, I developed proven expertise in $skillsText")
                    } else if (skillsText.isNotBlank()) {
                        append("Through my background, I have built strong targeted capabilities in $skillsText")
                    }
                    if (!topCert.isNullOrBlank()) {
                        append(", reinforced by my $topCert certification")
                    }
                    append(".")
                    if (targeted.languages.isNotEmpty()) {
                        append(" My language proficiency (${targeted.languages.joinToString(", ")}) further supports smooth cross-team collaboration.")
                    }
                }.trim(),
                "I would welcome the opportunity to discuss how my background aligns with ${offer.company}'s needs in an interview. Please find my attached English CV for a complete overview of my track record."
            )
        }

        return MldpCoverLetterData(
            senderName = profile.fullName.ifBlank { "Candidat" },
            senderAddress = profile.address,
            senderPhone = profile.phone,
            senderEmail = profile.email,
            recipientCompany = offer.company.ifBlank { if (isFrench) "Service Recrutement" else "Recruitment Team" },
            recipientAttention = if (isFrench) "À l'attention du Service Recrutement" else "Attn: Recruitment Team",
            recipientLocation = offer.location,
            placeAndDate = MldpCoverLetterData.formatCurrentPlaceAndDate(profile.address, isFrench = isFrench),
            subject = if (isFrench) "Objet : Candidature au poste de ${offer.title}" else "Subject: Application for ${offer.title}",
            salutation = if (isFrench) "Madame, Monsieur," else "Dear Hiring Manager,",
            bodyParagraphs = paragraphs.filter { it.isNotBlank() },
            closingFormula = if (isFrench) {
                "Je vous remercie par avance de l'attention que vous voudriez bien porter à ma candidature et me tiens à votre entière disposition pour convenir d'un rendez-vous. Dans cette attente, je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations les plus distinguées."
            } else {
                "Thank you in advance for your time and consideration of my application. I look forward to discussing my candidacy in an interview. Yours sincerely,"
            },
            signatureName = profile.fullName.ifBlank { "Candidat" },
            isFrench = isFrench
        )
    }

    private fun isStrictlyTargetLanguage(text: String, targetIsFrench: Boolean): Boolean {
        val padded = " " + text.lowercase().replace(Regex("[^a-zà-ÿ0-9']"), " ").replace(Regex("\\s+"), " ") + " "
        val frMarkers = listOf(
            " je ", " j'", " vous ", " mon ", " ma ", " mes ", " votre ", " vos ", " notre ",
            " au sein ", " candidature ", " poste ", " entretien ", " salutations ", " madame ",
            " monsieur ", " veuillez ", " compétence", " expérience", " parcours ", " pour le ", " de la ", " dans le "
        )
        val enMarkers = listOf(
            " i ", " i'm ", " i am ", " i have ", " i would ", " my ", " your ", " our ",
            " hiring manager", " dear ", " sincerely", " thank you ", " looking forward",
            " please find", " attached cv", " in my role ", " drawing on ", " stands out as",
            " through my ", " would welcome ", " for the ", " at your convenience"
        )
        val frHits = frMarkers.count { padded.contains(it) }
        val enHits = enMarkers.count { padded.contains(it) }
        return if (targetIsFrench) {
            // Reject any paragraph containing clear English sentences/structures
            enHits == 0
        } else {
            // Reject any paragraph containing French connectors/sentences
            frHits == 0
        }
    }

    private fun localizeCvTerm(raw: String, toFrench: Boolean): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return trimmed
        val pairs = listOf(
            "Administrative Manager" to "Responsable Administratif",
            "Human Resources & Administrative Coordinator" to "Coordinateur Ressources Humaines & Administratif",
            "Human Resources Administration" to "Administration des Ressources Humaines",
            "Human Resources Management" to "Gestion des Ressources Humaines",
            "Human Resources" to "Ressources Humaines",
            "Compensation and Payroll Management" to "Gestion de la Paie et Rémunération",
            "Payroll Support" to "Gestion de la Paie",
            "Payroll Management" to "Gestion de la Paie",
            "HR Compliance" to "Conformité RH",
            "Administrative Coordination" to "Coordination Administrative",
            "Office Administration" to "Gestion Administrative",
            "Staffing & Recruitment" to "Recrutement & Intégration",
            "Recruitment" to "Recrutement",
            "Onboarding" to "Intégration des collaborateurs",
            "Attendance Monitoring" to "Suivi des pointages et absences",
            "French Teacher" to "Enseignant de Français",
            "Project Coordinator" to "Coordinateur de Projet"
        )
        var result = trimmed
        for ((en, fr) in pairs) {
            result = if (toFrench) {
                result.replace(en, fr, ignoreCase = true)
            } else {
                result.replace(fr, en, ignoreCase = true)
            }
        }
        return result
    }

    private data class TargetedCvContext(
        val title: String,
        val skills: List<String>,
        val experiences: List<ExperienceItem>,
        val certifications: List<CertificationItem>,
        val education: EducationItem?,
        val educationDirectFit: Boolean,
        val languages: List<String>
    )

    private fun selectTargetedProfile(offer: JobOffer, profile: UserProfile): TargetedCvContext {
        val titleTokens = expandedTokens(offer.title)
        val bodyTokens = expandedTokens("${offer.title} ${offer.description} ${offer.tags.joinToString(" ")}")

        // 1. Pick only the most relevant title segment
        val titleSegments = profile.title.split("|", "/", "•").map { it.trim() }.filter { it.isNotBlank() }
        val bestTitle = titleSegments.maxByOrNull { scoreMatch(it, titleTokens, bodyTokens) }
            ?.takeIf { titleSegments.size <= 1 || scoreMatch(it, titleTokens, bodyTokens) > 0 }
            ?: titleSegments.firstOrNull()
            ?: profile.title

        // 2. Score & filter experiences (omit off-domain roles like Teaching for an HR job)
        val scoredExps = profile.experiences.map { exp ->
            val expText = "${exp.title} ${exp.description} ${exp.keySkills.joinToString(" ")}"
            exp to scoreMatch(expText, titleTokens, bodyTokens)
        }.sortedByDescending { it.second }

        val topExpScore = scoredExps.firstOrNull()?.second ?: 0
        val relevantExps = when {
            topExpScore >= 6 -> scoredExps.filter { it.second >= 4 || it.second >= topExpScore / 2 }.take(2).map { it.first }
            topExpScore > 0 -> scoredExps.filter { it.second > 0 }.take(2).map { it.first }
            else -> profile.experiences.take(1)
        }

        // 3. Score & filter skills strictly (suppress irrelevant dev/CAD/teaching tools unless offer matches)
        val allowTech = bodyTokens.any { it in TECH_MARKERS }
        val allowDesign = bodyTokens.any { it in DESIGN_MARKERS }
        val allowTeaching = bodyTokens.any { it in TEACHING_MARKERS }

        val candidateSkills = (profile.skills + profile.softSkills)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeToken(it) }
            // Avoid redundant short tokens like "Python" when "Python & Dart" already exists, preferring clean atomic names
            .filterNot { skill ->
                val norm = normalizeToken(skill)
                ((!allowTech && isInMarkerGroup(norm, TECH_MARKERS)) ||
                    (!allowDesign && isInMarkerGroup(norm, DESIGN_MARKERS)) ||
                    (!allowTeaching && isInMarkerGroup(norm, TEACHING_MARKERS)))
            }

        val scoredSkills = candidateSkills.map { skill ->
            skill to scoreMatch(skill, titleTokens, bodyTokens)
        }.sortedByDescending { it.second }

        val matchedSkills = scoredSkills.filter { it.second > 0 }.map { it.first }
        val fallbackExpSkills = relevantExps.flatMap { it.keySkills }
            .filterNot { skill ->
                val norm = normalizeToken(skill)
                (!allowTech && isInMarkerGroup(norm, TECH_MARKERS)) ||
                    (!allowDesign && isInMarkerGroup(norm, DESIGN_MARKERS)) ||
                    (!allowTeaching && isInMarkerGroup(norm, TEACHING_MARKERS))
            }

        val finalSkills = deduplicateSubsumedSkills(
            if (matchedSkills.size >= 3) matchedSkills.take(5)
            else (matchedSkills + fallbackExpSkills + scoredSkills.map { it.first }).distinctBy { normalizeToken(it) }.take(5)
        )

        // 4. Score certifications directly against the offer
        val relevantCerts = profile.certifications
            .map { it to scoreMatch("${it.name} ${it.issuer}", titleTokens, bodyTokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(2)
            .map { it.first }

        // 5. Education: drop high school ("Baccalauréat") when higher degree exists, flag direct fit cleanly
        val nonHighSchool = profile.educations.filterNot { normalizeToken(it.degree).contains("baccalaureat") }
        val eduPool = nonHighSchool.ifEmpty { profile.educations }
        val scoredEdus = eduPool.map { it to scoreMatch("${it.degree} ${it.description}", titleTokens, bodyTokens) }
            .sortedByDescending { it.second }
        val bestEdu = scoredEdus.firstOrNull()
        val eduDirectFit = (bestEdu?.second ?: 0) >= 2

        // 6. Include languages ONLY if mentioned in the offer / remote / international scope
        val mentionsLanguage = bodyTokens.any { it in LANGUAGE_MARKERS } ||
            offer.regionScope.equals("INTERNATIONAL", ignoreCase = true) ||
            offer.workMode.equals("REMOTE", ignoreCase = true)

        return TargetedCvContext(
            title = bestTitle,
            skills = finalSkills,
            experiences = relevantExps,
            certifications = relevantCerts,
            education = bestEdu?.first,
            educationDirectFit = eduDirectFit,
            languages = if (mentionsLanguage) profile.languages else emptyList()
        )
    }

    private fun deduplicateSubsumedSkills(skills: List<String>): List<String> {
        val cleaned = mutableListOf<String>()
        for (skill in skills) {
            val norm = normalizeToken(skill)
            val alreadyCovered = cleaned.any { existing ->
                val exNorm = normalizeToken(existing)
                exNorm == norm || (exNorm.contains("/") && exNorm.split("/").any { normalizeToken(it) == norm }) ||
                    (norm.contains("/") && norm.split("/").any { normalizeToken(it) == exNorm })
            }
            if (!alreadyCovered) cleaned.add(skill)
        }
        return cleaned
    }

    private fun scoreMatch(text: String, titleTokens: Set<String>, bodyTokens: Set<String>): Int {
        val itemTokens = expandedTokens(text)
        if (itemTokens.isEmpty()) return 0
        var score = 0
        for (tok in itemTokens) {
            if (tok in titleTokens) score += 4
            else if (tok in bodyTokens) score += 2
        }
        return score
    }

    private fun isInMarkerGroup(normText: String, markers: Set<String>): Boolean {
        val parts = normText.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        return parts.any { it in markers }
    }

    private fun expandedTokens(text: String): Set<String> {
        val base = normalizeToken(text)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .toMutableSet()
        for (tok in base.toList()) {
            SYNONYM_MAP[tok]?.let { base.addAll(it) }
        }
        return base
    }

    private fun normalizeToken(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    companion object {
        private val STOP_WORDS = setOf(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "en", "au", "aux", "pour",
            "par", "avec", "sur", "dans", "son", "ses", "nous", "vous", "poste", "offre", "chez",
            "the", "and", "for", "with", "from", "into", "your", "our", "are", "you", "job"
        )

        private val TECH_MARKERS = setOf(
            "python", "dart", "flutter", "html", "css", "javascript", "js", "react", "kotlin",
            "java", "sql", "php", "node", "nodejs", "laravel", "dev", "developpeur", "developer",
            "fullstack", "backend", "frontend", "mobile", "software", "programmation", "api"
        )

        private val DESIGN_MARKERS = setOf(
            "autocad", "fusion", "360", "cad", "cao", "dao", "photoshop", "adobe",
            "illustrator", "figma", "graphisme", "infographie"
        )

        private val TEACHING_MARKERS = setOf(
            "enseignement", "enseignant", "enseignante", "professeur", "educatrice", "educateur",
            "teacher", "teaching", "classroom", "lesson", "pedagogie", "didactique", "didactics",
            "eleves", "students", "student", "ecole", "school", "lycee", "formation"
        )

        private val LANGUAGE_MARKERS = setOf(
            "francais", "french", "anglais", "english", "arabe", "arabic", "bilingue",
            "bilingual", "langue", "langues", "language", "languages", "trilingue"
        )

        private val SYNONYM_MAP: Map<String, Set<String>> = buildMap {
            val clusters = listOf(
                setOf("rh", "hr", "ressources", "humaines", "human", "resources"),
                setOf("paie", "payroll", "salaire", "salaires", "remuneration", "compensation", "cnas", "cacobatph"),
                setOf("recrutement", "recruitment", "talent", "acquisition", "embauche"),
                setOf("onboarding", "integration", "accueil"),
                setOf("attendance", "pointage", "presence", "presences", "conge", "conges", "leave"),
                setOf("administratif", "administrative", "administration", "gestionnaire", "gestion", "personnel"),
                setOf("coordination", "coordinateur", "coordinator", "scheduling", "planning", "planification"),
                setOf("compliance", "conformite", "reglementation", "juridique", "travail"),
                setOf("reporting", "rapport", "rapports", "analyse", "analysis", "data", "donnees", "excel"),
                setOf("erp", "odoo", "sap", "sage", "sirh", "hris"),
                setOf("records", "dossiers", "archives", "archivage", "document", "documents"),
                setOf("enseignant", "teacher", "teaching", "enseignement", "educatrice", "educateur", "professeur"),
                setOf("francais", "french"),
                setOf("anglais", "english"),
                setOf("comptable", "comptabilite", "accounting", "finance"),
                setOf("logistique", "logistics", "achats", "procurement", "supply")
            )
            clusters.forEach { cluster ->
                cluster.forEach { term -> put(term, cluster) }
            }
        }
    }

    fun isFrenchOffer(offer: JobOffer): Boolean = looksLikeFrenchOffer(offer)

    /**
     * Generates a polite AI follow-up email referencing the submitted application & position.
     */
    suspend fun generateFollowUpEmail(offer: JobOffer, profile: UserProfile): Pair<String, String> {
        val isFr = looksLikeFrenchOffer(offer)
        val targeted = selectTargetedProfile(offer, profile)
        val defaultSubject = if (isFr) {
            "Relance : Candidature au poste de ${offer.title} - ${profile.fullName}"
        } else {
            "Follow-up: Application for ${offer.title} - ${profile.fullName}"
        }

        if (settings.hasAnyAiKey()) {
            val prompt = """
Rédige un court e-mail de relance courtois et professionnel (80 à 120 mots max) en ${if (isFr) "FRANÇAIS" else "ANGLAIS"}.
Objet suggéré : $defaultSubject
Candidat : ${profile.fullName}
Entreprise : ${offer.company}
Poste : ${offer.title}
Atouts principaux ciblés : ${targeted.skills.take(3).joinToString(", ")}
Expérience clé : ${targeted.experiences.firstOrNull()?.let { "${it.title} (${it.company})" } ?: targeted.title}
Réponds UNIQUEMENT par le corps du message (sans objet ni balises markdown).
            """.trimIndent()
            val aiBody = LlmClient.generateWithFreeFallback(
                settings,
                prompt,
                "Tu rédiges des emails de relance professionnels concis et respectueux."
            ).getOrNull()?.text?.trim()
            if (!aiBody.isNullOrBlank() && aiBody.length >= 60) {
                return defaultSubject to aiBody
            }
        }

        val fallbackBody = if (isFr) {
            """
Madame, Monsieur,

Je me permets de revenir vers vous concernant ma candidature au poste de ${offer.title} au sein de ${offer.company}.

Toujours vivement intéressé(e) par cette opportunité et convaincu(e) que mon expérience en ${targeted.skills.take(3).joinToString(", ").ifBlank { targeted.title }} répond aux besoins de votre équipe, je reste à votre entière disposition pour convenir d'un échange téléphonique ou d'un entretien à votre convenance.

Je vous remercie par avance de votre retour et vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations distinguées.

${profile.fullName}
${listOfNotNull(profile.phone.ifBlank { null }, profile.email.ifBlank { null }).joinToString(" | ")}
            """.trim()
        } else {
            """
Dear Hiring Team,

I am writing to kindly follow up on my application for the ${offer.title} role at ${offer.company}.

I remain strongly interested in contributing to your team, bringing hands-on expertise in ${targeted.skills.take(3).joinToString(", ").ifBlank { targeted.title }}. I would welcome the opportunity to discuss how my background aligns with your current goals.

Thank you for your time and consideration. I look forward to hearing from you.

Best regards,
${profile.fullName}
${listOfNotNull(profile.phone.ifBlank { null }, profile.email.ifBlank { null }).joinToString(" | ")}
            """.trim()
        }
        return defaultSubject to fallbackBody
    }

    /**
     * Classifies a genuine recruiter email reply scanned from IMAP into INTERVIEW, REJECTED, or APPLIED (with summary).
     */
    suspend fun classifyRecruiterReply(offer: JobOffer, reply: IncomingReplyEmail): Pair<JobStatus, String> {
        val corpus = "${reply.subject} ${reply.snippet}".lowercase()
        if (settings.hasAnyAiKey()) {
            val prompt = """
Classe cette réponse réelle de recruteur pour l'offre "${offer.title}" chez "${offer.company}".
Sujet email : ${reply.subject}
Extrait : ${reply.snippet}

Réponds en une ligne exacte : VERDICT | Résumé court en français (max 15 mots)
Où VERDICT est STRICTEMENT un de : INTERVIEW, REJECTED, ACK
            """.trimIndent()
            val aiLine = LlmClient.generateWithFreeFallback(
                settings,
                prompt,
                "Tu classes précisément des réponses email de recrutement."
            ).getOrNull()?.text?.trim()
            if (!aiLine.isNullOrBlank()) {
                val upper = aiLine.uppercase()
                val summary = aiLine.substringAfter("|", reply.subject).trim().take(90)
                return when {
                    upper.startsWith("INTERVIEW") -> JobStatus.INTERVIEW to summary
                    upper.startsWith("REJECTED") -> JobStatus.REJECTED to summary
                    upper.startsWith("ACK") -> JobStatus.APPLIED to summary
                    else -> classifyReplyKeywords(corpus, reply.subject)
                }
            }
        }
        return classifyReplyKeywords(corpus, reply.subject)
    }

    private fun classifyReplyKeywords(corpus: String, fallbackSubject: String): Pair<JobStatus, String> {
        val interviewHints = listOf("entretien", "interview", "call", "meeting", "créneau", "rdv", "rendez-vous", "shortlist", "schedule", "zoom", "teams", "disponibilit")
        val rejectHints = listOf("unfortunately", "not selected", "malheureusement", "pas retenu", "ne donnerons pas suite", "autre candidat", "other candidates", "regret to inform", "clôturé")
        return when {
            interviewHints.any { corpus.contains(it) } -> JobStatus.INTERVIEW to "Invitation entretien détectée (${fallbackSubject.take(45)})"
            rejectHints.any { corpus.contains(it) } -> JobStatus.REJECTED to "Réponse négative (${fallbackSubject.take(45)})"
            else -> JobStatus.APPLIED to "Réponse reçue : ${fallbackSubject.take(55)}"
        }
    }

    private fun looksLikeFrenchOffer(offer: JobOffer): Boolean {
        val sample = " ${offer.title} ${offer.description.take(900)} ".lowercase()
        val frHints = listOf(
            "responsable", "gestionnaire", "administratif", "administrative", "cadre ",
            "assistant", "assistante", "poste ", "nous recherchons", "missions ", "profil ",
            "algérie", "alger", "candidature", "expérience", "entreprise", "cdd", "cdi",
            "recrutement", "ressources humaines", "h/f", "f/h", "paie", "comptable",
            "maîtrise", "vous êtes", "votre rôle", "tâches", "diplôme", "niveau "
        )
        val enHints = listOf(
            "requirements", "responsibilities", "qualifications", "we are looking for",
            "full-time", "apply now", "experience with", "experience in", "salary",
            "benefits", "about the role", "what you will", "what you'll", "who we are",
            "bachelor", "years of experience", "united arab emirates", "saudi arabia",
            "qatar", "dubai", "riyadh", "doha", "remote ", "human resources", "partner"
        )
        val frScore = frHints.count { sample.contains(it) }
        val enScore = enHints.count { sample.contains(it) }
        return when {
            frScore == 0 && enScore == 0 -> offer.regionScope == "ALGERIA"
            frScore == enScore -> offer.regionScope == "ALGERIA"
            else -> frScore > enScore
        }
    }

    private fun extractJsonObject(raw: String): String {
        var text = raw.replace(Regex("(?s)<think>.*?</think>"), "").trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        text = text.trim()

        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start != -1 && end > start) {
            return text.substring(start, end + 1)
        }
        return text
    }
}
