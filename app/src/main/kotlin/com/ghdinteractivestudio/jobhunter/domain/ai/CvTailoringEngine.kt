package com.ghdinteractivestudio.jobhunter.domain.ai

import com.ghdinteractivestudio.jobhunter.data.local.AppSettings
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
     * Generates a realistic, convincing Cover Letter strictly grounded in real CV experiences
     * and written in the exact language of the job offer (French or English).
     */
    suspend fun generateCoverLetter(offer: JobOffer, profile: UserProfile): Result<String> {
        val systemInstruction = "Tu es un expert en recrutement et rédaction de lettres de motivation. Tu rédiges uniquement à partir des faits réels du CV."

        val prompt = """
Rédige une lettre de motivation complète, authentique et convaincante pour ce poste précis.
RÈGLES STRICTES :
1. Langue : Rédige EN FRANÇAIS si l'offre est en français, ou EN ANGLAIS si l'offre est en anglais.
2. Appuie-toi EXCLUSIVEMENT sur les expériences réelles, formations, compétences et langues du candidat ci-dessous. Zéro invention !
3. Structure complète : Coordonnées en-tête, Objet précis, Formule d'appel, Accroche liée à l'entreprise, Preuve par les réalisations réelles, Valeur ajoutée, Appel à l'entretien, Formule de politesse & Signature.
4. Format : Texte clair et propre prêt à être envoyé par email ou inséré en PDF/Word (pas de balises markdown ** ou # inutiles).

Candidat Réel :
Nom : ${profile.fullName}
Lieu : ${profile.address}
Email : ${profile.email} | Tél : ${profile.phone}
Titre : ${profile.title}
Compétences : ${(profile.skills + profile.softSkills).distinct().joinToString(", ")}
Langues : ${profile.languages.joinToString(", ")}
Expériences : ${profile.experiences.joinToString("; ") { "${it.title} chez ${it.company} (${it.period}): ${it.description.take(250)}" }}
Diplômes : ${profile.educations.joinToString("; ") { "${it.degree} (${it.institution}, ${it.year})" }}

Offre d'emploi ciblée :
Poste : ${offer.title}
Entreprise : ${offer.company}
Lieu : ${offer.location}
Description :
${offer.description.take(2000)}

Renvoie uniquement le texte final de la lettre.
        """.trimIndent()

        return LlmClient.generateWithFreeFallback(settings, prompt, systemInstruction).map { resp ->
            resp.text.replace(Regex("(?s)<think>.*?</think>"), "").trim()
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
