package com.ghdinteractivestudio.jobhunter.domain.ai

import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.ExperienceItem
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MatchAnalysisResponse(
    val matchScore: Int,
    val matchingPoints: List<String>,
    val missingRequirements: List<String>,
    val detectedFormatRequirement: String = "ANY" // "PDF", "DOCX", "ANY"
)

@Serializable
data class TailoredCvResponse(
    val targetTitle: String,
    val executiveSummary: String,
    val prioritizedSkills: List<String>,
    val tailoredHighlights: List<String>
)

class CvTailoringEngine(private val llmClient: LlmClient) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses raw CV into structured UserProfile
     */
    suspend fun parseCv(rawText: String): Result<UserProfile> {
        val prompt = """
Tu es un expert recruteur et analyste de CV professionnel (agency-resume-tailor).
Analyse le texte brut du CV suivant et extrait les informations au format JSON strict.
Ne falsifie aucune information.

Champs attendus en JSON :
{
  "fullName": "Nom Prénom",
  "email": "email",
  "phone": "téléphone",
  "title": "Titre professionnel principal",
  "summary": "Résumé de profil",
  "skills": ["Compétence 1", "Compétence 2"],
  "experiences": [
    {
      "title": "Poste",
      "company": "Entreprise",
      "period": "Dates / Durée",
      "description": "Responsabilités et réalisations",
      "keySkills": ["Outil 1", "Méthode 2"]
    }
  ],
  "educations": [
    {
      "degree": "Diplôme",
      "institution": "École / Université",
      "year": "Année"
    }
  ],
  "languages": ["Français (Natif)", "Anglais (Courant)"]
}

CV Brut :
$rawText

Réponds UNIQUEMENT par le JSON pur sans texte explicatif ni balises additionnelles.
        """.trimIndent()

        return llmClient.generateText(prompt).mapCatching { responseText ->
            val cleanJson = cleanMarkdownJson(responseText)
            json.decodeFromString<UserProfile>(cleanJson)
        }
    }

    /**
     * Analyzes job offer vs user profile, computes score and detects required file format (PDF / DOCX)
     */
    suspend fun analyzeMatch(offer: JobOffer, profile: UserProfile): Result<MatchAnalysisResponse> {
        val prompt = """
Tu es un évaluateur de candidatures et ATS (agency-resume-tailor).
Compare le profil du candidat avec l'offre d'emploi.

RÈGLE D'OR :
Vérifie dans la description de l'offre si l'employeur exige un format de fichier spécifique pour le CV ou la lettre :
- "PDF" si l'offre stipule explicitement format PDF.
- "DOCX" si l'offre stipule explicitement format Word / doc / docx.
- "ANY" si aucune mention particulière n'est précisée.

Profil Candidat :
Titre : ${profile.title}
Compétences : ${profile.skills.joinToString(", ")}
Expériences : ${profile.experiences.joinToString("; ") { "${it.title} chez ${it.company}: ${it.description}" }}

Offre d'Emploi :
Poste : ${offer.title} - ${offer.company}
Lieu : ${offer.location} (Remote: ${offer.isRemote})
Description : ${offer.description}

Réponds en JSON strict :
{
  "matchScore": 85,
  "matchingPoints": ["Point fort 1", "Point fort 2"],
  "missingRequirements": ["Éventuelle compétence manquante ou à approfondir"],
  "detectedFormatRequirement": "PDF" // ou "DOCX" ou "ANY"
}
        """.trimIndent()

        return llmClient.generateText(prompt).mapCatching { responseText ->
            val cleanJson = cleanMarkdownJson(responseText)
            json.decodeFromString<MatchAnalysisResponse>(cleanJson)
        }
    }

    /**
     * Tailors CV content ethically (no fake experience, only optimal ATS phrasing & prioritization)
     */
    suspend fun tailorCvContent(offer: JobOffer, profile: UserProfile): Result<TailoredCvResponse> {
        val prompt = """
Tu es un spécialiste de l'adaptation de CV (agency-resume-tailor).
Adapte la présentation du profil pour correspondre parfaitement au vocabulaire de l'offre.
INTERDICTION FORMELLE : N'invente AUCUNE expérience, entreprise, diplôme ou chiffre faux. Met en valeur les points réels du candidat.

Profil :
Nom : ${profile.fullName}
Titre actuel : ${profile.title}
Compétences : ${profile.skills.joinToString(", ")}
Expériences réelles : ${profile.experiences.joinToString(" | ") { "${it.title} chez ${it.company}: ${it.description}" }}

Poste ciblé :
${offer.title} chez ${offer.company}
${offer.description}

Réponds en JSON strict :
{
  "targetTitle": "Titre professionnel aligné sur l'offre",
  "executiveSummary": "Accroche percutante mettant en valeur la trajectoire du candidat en regard du besoin du recruteur",
  "prioritizedSkills": ["Compétences prioritaires alignées"],
  "tailoredHighlights": ["Réalisation concrète 1 reformulée", "Réalisation concrète 2"]
}
        """.trimIndent()

        return llmClient.generateText(prompt).mapCatching { responseText ->
            val cleanJson = cleanMarkdownJson(responseText)
            json.decodeFromString<TailoredCvResponse>(cleanJson)
        }
    }

    /**
     * Generates a tailored cover letter following the rules of the art
     */
    suspend fun generateCoverLetter(offer: JobOffer, profile: UserProfile): Result<String> {
        val prompt = """
Rédige une lettre de motivation professionnelle complète et percutante pour la candidature suivante.

Règles de rédaction (agency-deal-strategist & agency-resume-tailor) :
1. En-tête professionnel (coordonnées candidat, date, coordonnées entreprise).
2. Objet clair et référencé.
3. Formule d'appel formelle (Madame, Monsieur, / Dear Hiring Manager).
4. Paragraphe 1 : L'accroche et compréhension du défi de l'entreprise.
5. Paragraphe 2 : La preuve par l'exemple (expériences et réalisations concrètes du candidat qui répondent au besoin).
6. Paragraphe 3 : La collaboration future et proposition de valeur mutuelle.
7. Appel à l'action pour fixer un entretien.
8. Formule de politesse professionnelle et signature.
Langue : Utilise le français si l'offre est en français, ou l'anglais si l'offre est en anglais.

Candidat :
${profile.fullName} | ${profile.email} | ${profile.phone}
Compétences : ${profile.skills.joinToString(", ")}
Expériences : ${profile.experiences.joinToString("; ") { "${it.title} chez ${it.company}" }}

Offre :
Poste : ${offer.title}
Entreprise : ${offer.company}
Description :
${offer.description}

Renvoie directement le texte de la lettre prêt à être envoyé ou imprimé.
        """.trimIndent()

        return llmClient.generateText(prompt)
    }

    private fun cleanMarkdownJson(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.substring(7)
        } else if (text.startsWith("```")) {
            text = text.substring(3)
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3)
        }
        return text.trim()
    }
}
