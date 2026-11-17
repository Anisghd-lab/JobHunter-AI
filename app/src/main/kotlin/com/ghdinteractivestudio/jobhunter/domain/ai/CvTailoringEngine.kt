package com.ghdinteractivestudio.jobhunter.domain.ai

import com.ghdinteractivestudio.jobhunter.data.local.AppSettings
import com.ghdinteractivestudio.jobhunter.domain.document.MldpCoverLetterData
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
     * Generates a structured Cover Letter adhering strictly to the official MLDP standard:
     * 1. Sender coordinates (Top-Left)
     * 2. Recipient coordinates (Middle-Right)
     * 3. Place & Date (Right)
     * 4. Clear Subject & Formal Salutation
     * 5. 4-5 MLDP narrative paragraphs (Present yourself & request, Company interest & role alignment,
     *    Self-valorization with real skills/experiences/diplomas, Interview request & CV invitation)
     * 6. MLDP Formal Closing Formula
     * 7. Signature (Bottom-Right)
     */
    suspend fun generateMldpCoverLetter(offer: JobOffer, profile: UserProfile): MldpCoverLetterData {
        val offerLangFrench = looksLikeFrenchOffer(offer)
        val systemInstruction = "Tu rédiges des lettres de motivation selon la méthodologie officielle MLDP (Se présenter, Formuler la demande & valoriser l'entreprise, Exprimer ses attentes, Se valoriser par des preuves réelles du CV, Solliciter un entretien et inviter à consulter le CV ci-joint, Formule de politesse)."

        val prompt = """
Rédige les sections d'une lettre de motivation authentique en suivant SCRUPULEUSEMENT le modèle MLDP (http://www.missionlocaledp.org/emploi/lettre-de-motivation) au format JSON strict.

RÈGLES OBLIGATOIRES :
1. Langue : ${if (offerLangFrench) "FRANÇAIS" else "ANGLAIS (si l'offre est en anglais, traduis la structure MLDP en anglais professionnel soutenu)"}.
2. ZÉRO INVENTION : Utilise uniquement les vrais diplômes, expériences, entreprises, compétences et langues du candidat ci-dessous.
3. Les 4 paragraphes du corps DOIVENT suivre cet enchaînement exact MLDP :
   - "paragraphPresentAndRequest" : (1. Se présenter & Formuler sa requête) Titre/formation réelle du candidat + candidature claire pour le poste précis de ${offer.title}.
   - "paragraphExpectationsAndCompany" : (2. Valoriser l'entreprise & Exprimer ses souhaits) Pourquoi choisir ${offer.company} (secteur/missions de l'annonce) et quelles responsabilités le candidat souhaite y exercer.
   - "paragraphSelfValorization" : (3. Se valoriser) Preuves concrètes tirées des expériences, certifications, compétences clés et langues du CV réel.
   - "paragraphInterviewAndCvInvite" : (4. Solliciter un entretien & suggérer la lecture du CV ci-joint) Rappel explicite invitant le recruteur à découvrir le CV ci-joint qui détaille le parcours et demande de rencontre/entretien.
4. "closingFormula" : Formule de politesse complète MLDP (remerciement pour l'attention portée à la candidature + salutations distinguées).

Candidat Réel :
Nom : ${profile.fullName}
Lieu : ${profile.address}
Titre : ${profile.title}
Compétences : ${(profile.skills + profile.softSkills).distinct().joinToString(", ")}
Langues : ${profile.languages.joinToString(", ")}
Expériences : ${profile.experiences.joinToString("; ") { "${it.title} chez ${it.company} (${it.period}): ${it.description.take(250)}" }}
Diplômes : ${profile.educations.joinToString("; ") { "${it.degree} (${it.institution}, ${it.year})" }}
Certifications : ${profile.certifications.joinToString("; ") { "${it.name} (${it.issuer}, ${it.year})" }}

Offre d'emploi ciblée :
Poste : ${offer.title}
Entreprise : ${offer.company}
Lieu : ${offer.location}
Description :
${offer.description.take(1800)}

Réponds UNIQUEMENT par cet objet JSON pur (sans markdown) :
{
  "isFrench": $offerLangFrench,
  "subject": "${if (offerLangFrench) "Objet : Candidature au poste de ${offer.title}" else "Subject: Application for ${offer.title}"}",
  "salutation": "${if (offerLangFrench) "Madame, Monsieur," else "Dear Hiring Manager,"}",
  "paragraphPresentAndRequest": "...",
  "paragraphExpectationsAndCompany": "...",
  "paragraphSelfValorization": "...",
  "paragraphInterviewAndCvInvite": "...",
  "closingFormula": "..."
}
        """.trimIndent()

        val aiPayload = LlmClient.generateWithFreeFallback(settings, prompt, systemInstruction)
            .mapCatching { resp ->
                val cleanJson = extractJsonObject(resp.text)
                json.decodeFromString<MldpLetterAiPayload>(cleanJson)
            }.getOrNull()

        return if (aiPayload != null && aiPayload.paragraphPresentAndRequest.isNotBlank()) {
            val isFr = aiPayload.isFrench
            val paragraphs = listOf(
                aiPayload.paragraphPresentAndRequest,
                aiPayload.paragraphExpectationsAndCompany,
                aiPayload.paragraphSelfValorization,
                aiPayload.paragraphInterviewAndCvInvite
            ).filter { it.isNotBlank() }

            MldpCoverLetterData(
                senderName = profile.fullName.ifBlank { "Candidat" },
                senderAddress = profile.address,
                senderPhone = profile.phone,
                senderEmail = profile.email,
                recipientCompany = offer.company.ifBlank { if (isFr) "Direction des Ressources Humaines" else "Hiring Team" },
                recipientAttention = if (isFr) "À l'attention du Service Recrutement" else "Attn: Recruitment Team",
                recipientLocation = offer.location,
                placeAndDate = MldpCoverLetterData.formatCurrentPlaceAndDate(profile.address, isFr),
                subject = aiPayload.subject.ifBlank {
                    if (isFr) "Objet : Candidature au poste de ${offer.title}" else "Subject: Application for ${offer.title}"
                },
                salutation = aiPayload.salutation.ifBlank {
                    if (isFr) "Madame, Monsieur," else "Dear Hiring Manager,"
                },
                bodyParagraphs = paragraphs,
                closingFormula = aiPayload.closingFormula.ifBlank {
                    if (isFr) {
                        "Je vous remercie par avance de l'attention que vous voudriez bien porter à ma candidature et reste à votre disposition pour convenir d'un entretien. Dans cette attente, je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations distinguées."
                    } else {
                        "Thank you in advance for your consideration of my application. I remain at your disposal to arrange an interview and look forward to hearing from you. Sincerely,"
                    }
                },
                signatureName = profile.fullName.ifBlank { "Candidat" }
            )
        } else {
            buildDeterministicMldpLetter(offer, profile, offerLangFrench)
        }
    }

    suspend fun generateCoverLetter(offer: JobOffer, profile: UserProfile): Result<String> =
        Result.success(generateMldpCoverLetter(offer, profile).toPlainText())

    private fun buildDeterministicMldpLetter(
        offer: JobOffer,
        profile: UserProfile,
        isFrench: Boolean
    ): MldpCoverLetterData {
        val eduText = profile.educations.firstOrNull()?.let { "${it.degree}${if (it.institution.isNotBlank()) " (${it.institution})" else ""}" } ?: ""
        val expText = profile.experiences.firstOrNull()?.let { "${it.title}${if (it.company.isNotBlank()) " au sein de ${it.company}" else ""}" } ?: ""
        val expTextEn = profile.experiences.firstOrNull()?.let { "${it.title}${if (it.company.isNotBlank()) " at ${it.company}" else ""}" } ?: ""
        val skillsText = (profile.skills + profile.softSkills).distinct().take(6).joinToString(", ")

        val paragraphs = if (isFrench) {
            listOf(
                buildString {
                    if (eduText.isNotBlank()) append("Titulaire d'un $eduText")
                    else append("Professionnel(le) spécialisé(e) en ${profile.title.ifBlank { offer.title }}")
                    append(", je souhaite vous proposer ma candidature pour le poste de ${offer.title} au sein de ${offer.company}.")
                },
                "Votre entreprise reconnue dans son secteur représente pour moi un environnement privilégié où je pourrais exercer pleinement mes missions de coordination et de gestion, tout en contribuant activement au développement opérationnel de vos équipes.",
                buildString {
                    if (expText.isNotBlank()) {
                        append("J'ai pu acquérir, au fil de mon expérience notamment en qualité de $expText, une solide maîtrise en $skillsText. ")
                    } else if (skillsText.isNotBlank()) {
                        append("Mon parcours m'a permis de développer des compétences solides en $skillsText. ")
                    }
                    if (profile.languages.isNotEmpty()) {
                        append("De plus, ma maîtrise des langues (${profile.languages.joinToString(", ")}) constitue un atout concret que je souhaite mettre à la disposition de ${offer.company}.")
                    }
                }.trim(),
                "C'est pourquoi je souhaiterais vivement vous rencontrer lors d'un entretien afin de vous exposer plus en détail mes motivations. Je vous invite dès à présent à découvrir ci-joint mon CV, qui retrace mon parcours académique et mes expériences professionnelles."
            )
        } else {
            listOf(
                buildString {
                    if (eduText.isNotBlank()) append("Holding a $eduText")
                    else append("As an experienced ${profile.title.ifBlank { offer.title }}")
                    append(", I am eager to apply for the ${offer.title} position at ${offer.company}.")
                },
                "${offer.company} stands out as an organization where I can contribute directly to operational excellence while aligning my responsibilities with your team's strategic goals.",
                buildString {
                    if (expTextEn.isNotBlank()) {
                        append("Through my role as $expTextEn, I have developed proven expertise in $skillsText. ")
                    } else if (skillsText.isNotBlank()) {
                        append("Throughout my background, I have built strong capabilities in $skillsText. ")
                    }
                    if (profile.languages.isNotEmpty()) {
                        append("In addition, my language proficiency (${profile.languages.joinToString(", ")}) is an asset I look forward to bringing to your team.")
                    }
                }.trim(),
                "I would welcome the opportunity to meet for an interview to discuss my motivation in further detail. Please find my attached CV, which outlines my educational background and professional achievements."
            )
        }

        return MldpCoverLetterData(
            senderName = profile.fullName.ifBlank { "Candidat" },
            senderAddress = profile.address,
            senderPhone = profile.phone,
            senderEmail = profile.email,
            recipientCompany = offer.company.ifBlank { if (isFrench) "Service Recrutement" else "Hiring Team" },
            recipientAttention = if (isFrench) "À l'attention du Service Recrutement" else "Attn: Recruitment Team",
            recipientLocation = offer.location,
            placeAndDate = MldpCoverLetterData.formatCurrentPlaceAndDate(profile.address, isFrench = isFrench),
            subject = if (isFrench) "Objet : Candidature au poste de ${offer.title}" else "Subject: Application for ${offer.title}",
            salutation = if (isFrench) "Madame, Monsieur," else "Dear Hiring Manager,",
            bodyParagraphs = paragraphs.filter { it.isNotBlank() },
            closingFormula = if (isFrench) {
                "Je vous remercie par avance de l'attention que vous voudriez bien porter à ma candidature et me tiens à votre entière disposition pour convenir d'un rendez-vous. Dans cette attente, je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations les plus distinguées."
            } else {
                "Thank you in advance for your time and consideration of my application. I remain at your disposal for an interview and look forward to hearing from you."
            },
            signatureName = profile.fullName.ifBlank { "Candidat" }
        )
    }

    private fun looksLikeFrenchOffer(offer: JobOffer): Boolean {
        val sample = "${offer.title} ${offer.description.take(400)}".lowercase()
        val frHints = listOf("responsable", "gestionnaire", "administratif", "cadre ", "assistant", "poste ", "nous recherchons", "missions ", "profil ", "algérie", "candidature", "expérience")
        return frHints.count { sample.contains(it) } >= 2 || offer.regionScope == "ALGERIA"
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
