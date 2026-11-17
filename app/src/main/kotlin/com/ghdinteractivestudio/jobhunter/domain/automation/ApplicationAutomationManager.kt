package com.ghdinteractivestudio.jobhunter.domain.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.data.local.AppSettings
import com.ghdinteractivestudio.jobhunter.data.local.JobOfferEntity
import com.ghdinteractivestudio.jobhunter.data.remote.JobScanner
import com.ghdinteractivestudio.jobhunter.domain.ai.CvTailoringEngine
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.document.DocxGenerator
import com.ghdinteractivestudio.jobhunter.domain.document.PdfGenerator
import com.ghdinteractivestudio.jobhunter.domain.email.EmailAuthType
import com.ghdinteractivestudio.jobhunter.domain.email.EmailConfig
import com.ghdinteractivestudio.jobhunter.domain.email.EmailDispatcher
import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ApplicationOutcome(
    val updatedOffer: JobOffer,
    val sentSilentlyByEmail: Boolean,
    val requiresWebAction: Boolean,
    val requiresEmailAppIntent: Boolean,
    val message: String
)

object ApplicationAutomationManager {

    private const val TAG = "JobAutomation"

    private suspend fun verifyLiveOfferAndArchiveIfExpired(
        context: Context,
        offer: JobOffer,
        scanner: JobScanner
    ): Result<JobOffer> {
        val db = AppDatabase.getInstance(context)
        if (DeadlineParser.isExpiredOrStale(offer)) {
            val archived = offer.copy(status = JobStatus.ARCHIVED, aiVerdict = "EXPIREE")
            db.jobOfferDao().insertOrUpdate(JobOfferEntity.fromDomain(archived))
            return Result.failure(IllegalStateException("Date limite dépassée (${offer.deadlineLabel.ifBlank { "offre expirée" }}) — candidature bloquée."))
        }

        val liveDetails = if (offer.url.startsWith("http")) {
            scanner.fetchOfferLiveDetails(offer.url)
        } else null

        if (liveDetails != null && liveDetails.isExpired) {
            val archived = offer.copy(
                status = JobStatus.ARCHIVED,
                aiVerdict = "EXPIREE",
                deadlineAt = liveDetails.deadlineAt.takeIf { it > 0L } ?: offer.deadlineAt,
                deadlineLabel = liveDetails.deadlineLabel.ifBlank { "Expirée" }
            )
            db.jobOfferDao().insertOrUpdate(JobOfferEntity.fromDomain(archived))
            return Result.failure(IllegalStateException("Offre expirée sur le site (${archived.deadlineLabel}) — archivée."))
        }

        val finalEmail = offer.recruiterEmail.ifBlank { liveDetails?.recruiterEmail.orEmpty() }
        val finalDeadlineAt = if (offer.deadlineAt > 0L) offer.deadlineAt else (liveDetails?.deadlineAt ?: 0L)
        val finalDeadlineLabel = offer.deadlineLabel.ifBlank { liveDetails?.deadlineLabel.orEmpty() }

        return Result.success(
            offer.copy(
                recruiterEmail = finalEmail,
                applicationMethod = if (finalEmail.isNotBlank()) "EMAIL" else "WEBSITE",
                deadlineAt = finalDeadlineAt,
                deadlineLabel = finalDeadlineLabel
            )
        )
    }

    /**
     * Step 1: Compares a real web job offer against the user's real CV using Free LLMs,
     * after verifying strict deadline compliance & recruiter email availability.
     */
    suspend fun evaluateOfferWithAi(
        context: Context,
        offer: JobOffer,
        profile: UserProfile,
        settings: AppSettings,
        scanner: JobScanner
    ): Result<JobOffer> = withContext(Dispatchers.IO) {
        if (profile.rawCvContent.isBlank() || profile.fullName.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Veuillez d'abord importer votre vrai CV dans l'onglet Mon CV.")
            )
        }

        val verifiedOffer = verifyLiveOfferAndArchiveIfExpired(context, offer, scanner)
            .getOrElse { return@withContext Result.failure(it) }

        val engine = CvTailoringEngine(settings)
        val aiResult = engine.analyzeMatch(verifiedOffer, profile)

        val updated = aiResult.fold(
            onSuccess = { analysis ->
                val formatReq = when (analysis.detectedFormatRequirement.uppercase()) {
                    "PDF" -> DocumentFormatRequirement.PDF
                    "DOCX" -> DocumentFormatRequirement.DOCX
                    else -> verifiedOffer.requiredFormat
                }
                verifiedOffer.copy(
                    matchScore = analysis.matchScore.coerceIn(0, 100),
                    matchingHighlights = analysis.matchingPoints.ifEmpty { verifiedOffer.matchingHighlights },
                    missingRequirements = analysis.missingRequirements,
                    requiredFormat = formatReq,
                    aiVerdict = analysis.verdict.ifBlank {
                        if (analysis.matchScore >= 60) "CONVIENT" else if (analysis.matchScore >= 40) "MOYEN" else "NON_ADAPTE"
                    },
                    aiReasoning = analysis.reasoning,
                    status = if (verifiedOffer.status == JobStatus.DETECTED) JobStatus.ANALYZED else verifiedOffer.status
                )
            },
            onFailure = { err ->
                Log.w(TAG, "AI evaluation fallback used: ${err.message}")
                verifiedOffer.copy(
                    status = if (verifiedOffer.status == JobStatus.DETECTED) JobStatus.ANALYZED else verifiedOffer.status
                )
            }
        )

        AppDatabase.getInstance(context).jobOfferDao()
            .insertOrUpdate(JobOfferEntity.fromDomain(updated))

        Result.success(updated)
    }

    /**
     * Step 2: Verifies deadline, builds MLDP Cover Letter & tailored CV in employer's required format
     * (PDF or Word .docx), then dispatches via Email OR prepares for Website portal submission.
     */
    suspend fun prepareAndApply(
        context: Context,
        offer: JobOffer,
        profile: UserProfile,
        settings: AppSettings,
        scanner: JobScanner,
        sendSilentEmailIfConfigured: Boolean = true
    ): Result<ApplicationOutcome> = withContext(Dispatchers.IO) {
        if (profile.rawCvContent.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Aucun CV réel chargé. Importez votre CV PDF ou DOCX avant de postuler.")
            )
        }

        val liveOffer = verifyLiveOfferAndArchiveIfExpired(context, offer, scanner)
            .getOrElse { return@withContext Result.failure(it) }

        val targetEmail = liveOffer.recruiterEmail
        val engine = CvTailoringEngine(settings)

        // Run AI CV tailoring (strictly 100% faithful to real CV)
        val tailored = engine.tailorCvContent(liveOffer, profile).getOrNull()

        // Generate personalized Cover Letter strictly following MLDP 7-part standard
        val mldpLetter = engine.generateMldpCoverLetter(liveOffer, profile)
        val coverLetter = mldpLetter.toPlainText()

        val safeName = profile.fullName.ifBlank { "Candidat" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeCompany = offer.company.ifBlank { "Entreprise" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val uniqueSuffix = offer.id.takeLast(6).replace(Regex("[^a-zA-Z0-9]"), "")

        val outputDir = File(context.filesDir, "applications").apply { mkdirs() }

        // Build real CV sections
        val cvSections = buildList {
            val contactLine = listOfNotNull(
                profile.email.ifBlank { null },
                profile.phone.ifBlank { null },
                profile.address.ifBlank { null }
            ).joinToString(" | ")
            if (contactLine.isNotBlank()) {
                add("Coordonnées" to listOf(contactLine))
            }

            val summaryText = tailored?.executiveSummary?.takeIf { it.isNotBlank() } ?: profile.summary
            if (summaryText.isNotBlank()) {
                add("Profil Professionnel" to listOf(summaryText))
            }

            val skillsList = tailored?.prioritizedSkills?.takeIf { it.isNotEmpty() }
                ?: (profile.skills + profile.softSkills).distinct()
            if (skillsList.isNotEmpty()) {
                add("Compétences Clés" to listOf(skillsList.joinToString(" • ")))
            }

            if (profile.experiences.isNotEmpty()) {
                add(
                    "Expériences Professionnelles" to profile.experiences.map { exp ->
                        val header = listOfNotNull(
                            exp.title.ifBlank { null },
                            exp.company.ifBlank { null },
                            exp.period.ifBlank { null }
                        ).joinToString(" | ")
                        if (exp.description.isNotBlank()) "$header\n${exp.description}" else header
                    }
                )
            }

            if (profile.educations.isNotEmpty()) {
                add(
                    "Formations & Diplômes" to profile.educations.map { edu ->
                        listOfNotNull(
                            edu.degree.ifBlank { null },
                            edu.institution.ifBlank { null },
                            edu.year.ifBlank { null }
                        ).joinToString(" - ")
                    }
                )
            }

            if (profile.certifications.isNotEmpty()) {
                add(
                    "Certifications" to profile.certifications.map {
                        listOfNotNull(it.name, it.issuer.ifBlank { null }, it.year.ifBlank { null }).joinToString(" - ")
                    }
                )
            }

            if (profile.languages.isNotEmpty()) {
                add("Langues" to listOf(profile.languages.joinToString(" | ")))
            }
        }

        val cvHeaderTitle = "${profile.fullName.ifBlank { "CV" }} — ${tailored?.targetTitle?.ifBlank { profile.title } ?: profile.title}"

        // Respect Employer Demanded Format: Word (.docx) OR PDF
        val useDocx = liveOffer.requiredFormat == DocumentFormatRequirement.DOCX

        val cvFile = if (useDocx) {
            val f = File(outputDir, "CV_${safeName}_${uniqueSuffix}.docx")
            DocxGenerator.generateDocx(cvHeaderTitle, cvSections, f)
            f
        } else {
            val f = File(outputDir, "CV_${safeName}_${uniqueSuffix}.pdf")
            PdfGenerator.generatePdf(cvHeaderTitle, cvSections, f)
            f
        }

        // Generate MLDP-compliant Cover Letter file (DOCX vs PDF)
        val letterFile = if (useDocx) {
            val f = File(outputDir, "Lettre_${safeCompany}_${uniqueSuffix}.docx")
            DocxGenerator.generateMldpCoverLetterDocx(mldpLetter, f)
            f
        } else {
            val f = File(outputDir, "Lettre_${safeCompany}_${uniqueSuffix}.pdf")
            PdfGenerator.generateMldpCoverLetterPdf(mldpLetter, f)
            f
        }

        val attachments = listOf(cvFile, letterFile)
        val now = System.currentTimeMillis()
        var sentSilently = false
        var needsWebAction = false
        var needsEmailIntent = false
        var finalStatus = JobStatus.READY_TO_SEND
        var channelLabel = ""
        var resultMessage = ""

        if (targetEmail.isNotBlank()) {
            if (sendSilentEmailIfConfigured && settings.hasEmailCredentials()) {
                val authType = if (settings.gmailOAuthToken.isNotBlank()) {
                    EmailAuthType.OAUTH2_GMAIL
                } else {
                    EmailAuthType.SMTP_DIRECT
                }
                val dispatcher = EmailDispatcher(
                    EmailConfig(
                        authType = authType,
                        senderEmail = settings.senderEmail.ifBlank { profile.email },
                        senderName = profile.fullName,
                        smtpHost = settings.smtpHost,
                        smtpPort = settings.smtpPort,
                        smtpUser = settings.senderEmail.ifBlank { profile.email },
                        smtpPassword = settings.smtpPassword,
                        oauthAccessToken = settings.gmailOAuthToken
                    )
                )
                val emailRes = dispatcher.sendApplicationEmail(
                    recipientEmail = targetEmail,
                    subject = "${mldpLetter.subject} - ${profile.fullName}",
                    bodyText = coverLetter,
                    attachments = attachments
                )
                if (emailRes.isSuccess) {
                    sentSilently = true
                    finalStatus = JobStatus.APPLIED
                    channelLabel = "Email envoyé à $targetEmail"
                    resultMessage = "Candidature MLDP envoyée par email à $targetEmail (${if (useDocx) "DOCX" else "PDF"}) !"
                } else {
                    needsEmailIntent = true
                    channelLabel = "Email prêt ($targetEmail) — Échec SMTP: ${emailRes.exceptionOrNull()?.message?.take(40)}"
                    resultMessage = "Dossier prêt (${if (useDocx) "DOCX" else "PDF"}). Envoi SMTP impossible : ouvrez votre appli Email."
                }
            } else {
                needsEmailIntent = true
                channelLabel = "Email prêt ($targetEmail)"
                resultMessage = "CV & Lettre MLDP générés (${if (useDocx) "DOCX" else "PDF"}) pour $targetEmail !"
            }
        } else {
            needsWebAction = true
            channelLabel = "Portail Web (${liveOffer.sourcePlatform})"
            resultMessage = "CV & Lettre MLDP prêts (${if (useDocx) "DOCX" else "PDF"}). Finalisez sur le site de l'offre !"
        }

        val updated = liveOffer.copy(
            status = finalStatus,
            recruiterEmail = targetEmail,
            applicationMethod = if (targetEmail.isNotBlank()) "EMAIL" else "WEBSITE",
            generatedLetter = coverLetter,
            tailoredCvPath = cvFile.absolutePath,
            letterPath = letterFile.absolutePath,
            appliedAt = if (sentSilently) now else 0L,
            appliedChannel = channelLabel,
            aiVerdict = liveOffer.aiVerdict.ifBlank { "CONVIENT" }
        )

        AppDatabase.getInstance(context).jobOfferDao()
            .insertOrUpdate(JobOfferEntity.fromDomain(updated))

        Result.success(
            ApplicationOutcome(
                updatedOffer = updated,
                sentSilentlyByEmail = sentSilently,
                requiresWebAction = needsWebAction,
                requiresEmailAppIntent = needsEmailIntent,
                message = resultMessage
            )
        )
    }

    /**
     * Launches Android Email App pre-filled with recipient, subject, cover letter and FileProvider attachments.
     */
    fun buildEmailComposeIntent(context: Context, offer: JobOffer): Intent {
        val uris = ArrayList<Uri>()
        listOf(offer.tailoredCvPath, offer.letterPath).forEach { path ->
            if (path.isNotBlank()) {
                val f = File(path)
                if (f.exists()) {
                    uris.add(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            f
                        )
                    )
                }
            }
        }
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            if (offer.recruiterEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(offer.recruiterEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Candidature : ${offer.title}")
            putExtra(Intent.EXTRA_TEXT, offer.generatedLetter)
            if (uris.isNotEmpty()) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    /**
     * Opens or shares a generated PDF / DOCX document using FileProvider.
     */
    fun buildViewOrShareFileIntent(context: Context, filePath: String, share: Boolean = false): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = if (file.name.endsWith(".docx", ignoreCase = true)) {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        } else {
            "application/pdf"
        }
        return if (share) {
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
