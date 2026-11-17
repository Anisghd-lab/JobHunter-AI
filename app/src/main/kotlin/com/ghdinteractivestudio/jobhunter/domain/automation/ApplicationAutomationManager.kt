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
    val message: String,
    val usesOriginalCv: Boolean = false
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
     * Step 2: Verifies deadline, builds MLDP Cover Letter & selects language-matched CV:
     * picks FR CV for French offers & EN CV for English/Gulf/Worldwide offers,
     * attaching the ORIGINAL file by default unless modified by the user/AI
     * or an incompatible format (e.g. DOCX required vs PDF original) demands regeneration.
     */
    suspend fun prepareAndApply(
        context: Context,
        offer: JobOffer,
        profile: UserProfile,
        settings: AppSettings,
        scanner: JobScanner,
        sendSilentEmailIfConfigured: Boolean = true,
        forceTailoredCv: Boolean? = null
    ): Result<ApplicationOutcome> = withContext(Dispatchers.IO) {
        val migratedProfile = profile.migrateLegacySlotIfNeeded()
        if (migratedProfile.combinedSearchCorpus().isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Aucun CV réel chargé. Importez votre CV PDF ou DOCX avant de postuler.")
            )
        }

        val liveOffer = verifyLiveOfferAndArchiveIfExpired(context, offer, scanner)
            .getOrElse { return@withContext Result.failure(it) }

        val targetEmail = liveOffer.recruiterEmail
        val engine = CvTailoringEngine(settings)
        val offerIsFrench = engine.isFrenchOffer(liveOffer)
        val chosenSlot = if (offerIsFrench) {
            migratedProfile.frenchCv ?: migratedProfile.englishCv
        } else {
            migratedProfile.englishCv ?: migratedProfile.frenchCv
        }
        val langTag = when {
            offerIsFrench && migratedProfile.frenchCv != null -> "FR"
            !offerIsFrench && migratedProfile.englishCv != null -> "EN"
            migratedProfile.frenchCv != null -> "FR"
            else -> "EN"
        }

        // Generate personalized Cover Letter strictly following MLDP 7-part standard & job targeting
        val mldpLetter = engine.generateMldpCoverLetter(liveOffer, migratedProfile)
        val coverLetter = mldpLetter.toPlainText()

        val safeName = migratedProfile.fullName.ifBlank { "Candidat" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeCompany = offer.company.ifBlank { "Entreprise" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val uniqueSuffix = offer.id.takeLast(6).replace(Regex("[^a-zA-Z0-9]"), "")

        val outputDir = File(context.filesDir, "applications").apply { mkdirs() }

        // Check whether to attach the Original uploaded EN/FR CV vs generating a Modified/Tailored CV
        val useDocx = liveOffer.requiredFormat == DocumentFormatRequirement.DOCX
        val candidatePath = chosenSlot?.filePath?.ifBlank { migratedProfile.sourceCvPath } ?: migratedProfile.sourceCvPath
        val originalFile = candidatePath.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?.takeIf { it.exists() && it.length() > 0L }
        val originalExt = originalFile?.extension?.lowercase().orEmpty()
        val originalFormatMatches = originalFile != null && (
            liveOffer.requiredFormat == DocumentFormatRequirement.ANY ||
                (liveOffer.requiredFormat == DocumentFormatRequirement.PDF && originalExt == "pdf") ||
                (liveOffer.requiredFormat == DocumentFormatRequirement.DOCX && (originalExt == "docx" || originalExt == "doc"))
            )
        val slotModified = (chosenSlot?.isModified == true) || migratedProfile.isModifiedCv
        val modificationRequested = forceTailoredCv ?: (slotModified || settings.adaptCvPerOffer)
        val attachOriginalCv = originalFormatMatches && !modificationRequested

        val cvFile: File = if (attachOriginalCv) {
            File(outputDir, "CV_${safeName}_${langTag}_Original_${uniqueSuffix}.${originalExt}").also { dest ->
                originalFile!!.copyTo(dest, overwrite = true)
            }
        } else {
            val tailored = engine.tailorCvContent(liveOffer, migratedProfile).getOrNull()
            val cvSections = buildCvSections(migratedProfile, tailored)
            val cvHeaderTitle = "${migratedProfile.fullName.ifBlank { "CV" }} — ${tailored?.targetTitle?.ifBlank { migratedProfile.title } ?: migratedProfile.title}"

            if (useDocx) {
                File(outputDir, "CV_${safeName}_${langTag}_${uniqueSuffix}.docx").also { f ->
                    DocxGenerator.generateDocx(cvHeaderTitle, cvSections, f)
                }
            } else {
                File(outputDir, "CV_${safeName}_${langTag}_${uniqueSuffix}.pdf").also { f ->
                    PdfGenerator.generatePdf(cvHeaderTitle, cvSections, f)
                }
            }
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

        val cvKindLabel = when {
            attachOriginalCv -> "CV original $langTag (${originalExt.uppercase()})"
            originalFile == null -> "CV $langTag généré (ré-importez dans Mon CV pour joindre l'original)"
            else -> "CV $langTag modifié (${cvFile.extension.uppercase()})"
        }

        val attachments = listOf(cvFile, letterFile)
        val now = System.currentTimeMillis()
        var sentSilently = false
        var needsWebAction = false
        var needsEmailIntent = false
        var finalStatus = JobStatus.READY_TO_SEND
        var channelLabel = ""
        var resultMessage = ""

        val allowAutoSend = sendSilentEmailIfConfigured && settings.canSendSilently()
        if (targetEmail.isNotBlank()) {
            if (allowAutoSend) {
                val dispatcher = EmailDispatcher(buildEmailConfig(settings, migratedProfile))
                val emailRes = dispatcher.sendApplicationEmail(
                    recipientEmail = targetEmail,
                    subject = "${mldpLetter.subject} - ${migratedProfile.fullName}",
                    bodyText = coverLetter,
                    attachments = attachments
                )
                if (emailRes.isSuccess) {
                    sentSilently = true
                    finalStatus = JobStatus.APPLIED
                    channelLabel = "Email auto envoyé à $targetEmail"
                    resultMessage = "Candidature envoyée à $targetEmail ($cvKindLabel + Lettre MLDP) !"
                } else {
                    val errMsg = emailRes.exceptionOrNull()?.message.orEmpty()
                    if (errMsg.contains("535") || errMsg.contains("refusés") || errMsg.contains("AUTH005")) {
                        com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository.getInstance(context)
                            .update { it.copy(smtpAuthPaused = true) }
                    }
                    needsEmailIntent = true
                    channelLabel = if (settings.preferInstalledYahooApp) {
                        "Prêt via Appli Yahoo Mail ($targetEmail)"
                    } else {
                        "Email prêt ($targetEmail) — Erreur SMTP"
                    }
                    resultMessage = if (settings.preferInstalledYahooApp) {
                        "Dossier prêt ($cvKindLabel + Lettre MLDP) — touchez « Envoyer via Yahoo Mail » !"
                    } else {
                        "Échec SMTP (${errMsg.take(55)}) — ouvrez l'appli Email."
                    }
                }
            } else {
                needsEmailIntent = true
                channelLabel = if (settings.preferInstalledYahooApp) {
                    "Prêt via Appli Yahoo Mail ($targetEmail)"
                } else {
                    "Email prêt ($targetEmail)"
                }
                resultMessage = if (settings.preferInstalledYahooApp) {
                    "$cvKindLabel & Lettre MLDP prêts — touchez « Envoyer via Yahoo Mail » !"
                } else {
                    "$cvKindLabel & Lettre MLDP prêts pour $targetEmail !"
                }
            }
        } else {
            needsWebAction = true
            channelLabel = "Portail Web (${liveOffer.sourcePlatform})"
            resultMessage = "$cvKindLabel & Lettre MLDP prêts. Finalisez sur le site de l'offre !"
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
                message = resultMessage,
                usesOriginalCv = attachOriginalCv
            )
        )
    }

    /**
     * Sends an immediate AI-crafted follow-up email for an already APPLIED job offer.
     */
    suspend fun sendFollowUpNow(
        context: Context,
        offer: JobOffer,
        profile: UserProfile,
        settings: AppSettings
    ): Result<JobOffer> = withContext(Dispatchers.IO) {
        runCatching {
            if (!settings.hasEmailCredentials()) {
                error("Configurez d'abord votre email & App Password dans Agent 24/7.")
            }
            if (offer.recruiterEmail.isBlank()) {
                error("Aucun e-mail recruteur enregistré pour cette candidature.")
            }
            if (DeadlineParser.isExpiredOrStale(offer)) {
                error("Offre expirée — relance annulée.")
            }

            val engine = CvTailoringEngine(settings)
            val (subject, body) = engine.generateFollowUpEmail(offer, profile)
            val dispatcher = EmailDispatcher(buildEmailConfig(settings, profile))
            val attachments = listOfNotNull(
                offer.tailoredCvPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
            )
            dispatcher.sendApplicationEmail(offer.recruiterEmail, subject, body, attachments).getOrThrow()

            val updated = offer.copy(
                followUpCount = offer.followUpCount + 1,
                lastFollowUpAt = System.currentTimeMillis(),
                lastReplySummary = "Relance IA envoyée à ${offer.recruiterEmail}"
            )
            AppDatabase.getInstance(context).jobOfferDao().insertOrUpdate(JobOfferEntity.fromDomain(updated))
            updated
        }
    }

    /**
     * Autonomous AI Email Tracking & Relance Cycle:
     * 1) Scans IMAP INBOX (read-only) for genuine recruiter replies matching APPLIED offers and auto-classifies
     *    INTERVIEW / REJECTED statuses.
     * 2) Automatically dispatches a polite J+N AI follow-up when autoFollowUpEnabled is active.
     */
    suspend fun runAutonomousEmailFollowUpCycle(
        context: Context,
        profile: UserProfile,
        settings: AppSettings
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        if (!settings.canSendSilently() || settings.smtpPassword.isBlank()) {
            val msg = if (settings.smtpAuthPaused) {
                "SMTP en pause (erreur 535/AUTH005) — vérifiez App Password ou utilisez Appli Yahoo Mail."
            } else {
                "App Password SMTP requis pour la lecture IMAP & relance silencieuse."
            }
            return@withContext 0 to msg
        }
        val dao = AppDatabase.getInstance(context).jobOfferDao()
        val applied = dao.getOffersByStatusOnce(JobStatus.APPLIED.name).map { it.toDomain() }
        if (applied.isEmpty()) return@withContext 0 to "Aucune candidature envoyée à suivre."

        val engine = CvTailoringEngine(settings)
        val dispatcher = EmailDispatcher(buildEmailConfig(settings, profile))
        var updates = 0
        val highlights = mutableListOf<String>()

        // 1. Check IMAP replies (read-only)
        val replies = dispatcher.fetchRecentInboxReplies(maxMessages = 25).getOrNull().orEmpty()
        if (replies.isNotEmpty()) {
            for (offer in applied) {
                val recDomain = offer.recruiterEmail.substringAfter("@", "").lowercase()
                val companyKey = offer.company.lowercase().trim()
                val matchedReply = replies.firstOrNull { rep ->
                    val hay = "${rep.from} ${rep.subject} ${rep.snippet}".lowercase()
                    (offer.recruiterEmail.isNotBlank() && hay.contains(offer.recruiterEmail.lowercase())) ||
                        (recDomain.length >= 4 && !recDomain.contains("gmail") && !recDomain.contains("yahoo") && hay.contains(recDomain)) ||
                        (companyKey.length >= 4 && hay.contains(companyKey))
                }
                if (matchedReply != null) {
                    val (newStatus, summary) = engine.classifyRecruiterReply(offer, matchedReply)
                    if (newStatus != offer.status || summary != offer.lastReplySummary) {
                        val updated = offer.copy(status = newStatus, lastReplySummary = summary)
                        dao.insertOrUpdate(JobOfferEntity.fromDomain(updated))
                        updates++
                        highlights.add("${offer.company}: $summary")
                    }
                }
            }
        }

        // 2. Auto Follow-up (J+N) on unanswered APPLIED offers
        if (settings.autoFollowUpEnabled) {
            val now = System.currentTimeMillis()
            val delayMs = settings.followUpDelayDays.coerceIn(2, 14) * 86_400_000L
            for (offer in applied.take(3)) {
                val current = dao.getOfferById(offer.id)?.toDomain() ?: continue
                if (current.status != JobStatus.APPLIED || current.followUpCount > 0) continue
                if (current.recruiterEmail.isBlank() || current.appliedAt == 0L) continue
                if (now - current.appliedAt < delayMs) continue
                if (DeadlineParser.isExpiredOrStale(current, now)) continue

                sendFollowUpNow(context, current, profile, settings).onSuccess {
                    updates++
                    highlights.add("Relance IA envoyée à ${current.company}")
                }
            }
        }

        updates to (highlights.joinToString(" · ").ifBlank { "Boîte vérifiée — aucune nouvelle réponse." })
    }

    fun buildEmailConfig(settings: AppSettings, profile: UserProfile = UserProfile()): EmailConfig {
        val authType = if (settings.gmailOAuthToken.isNotBlank()) {
            EmailAuthType.OAUTH2_GMAIL
        } else {
            EmailAuthType.SMTP_DIRECT
        }
        return EmailConfig(
            authType = authType,
            senderEmail = settings.senderEmail.ifBlank { profile.email },
            senderName = profile.fullName,
            smtpHost = settings.smtpHost,
            smtpPort = settings.smtpPort,
            smtpUser = settings.senderEmail.ifBlank { profile.email },
            smtpPassword = settings.smtpPassword,
            oauthAccessToken = settings.gmailOAuthToken
        )
    }

    private fun buildCvSections(
        profile: UserProfile,
        tailored: com.ghdinteractivestudio.jobhunter.domain.ai.TailoredCvResponse?
    ): List<Pair<String, List<String>>> = buildList {
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

    /**
     * Launches installed Yahoo Mail app (when connected/available) or chooser pre-filled with
     * recipient, subject, cover letter and FileProvider attachments.
     */
    fun buildEmailComposeIntent(
        context: Context,
        offer: JobOffer,
        preferredPackage: String = ""
    ): Intent {
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
            type = "message/rfc822"
            if (offer.recruiterEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(offer.recruiterEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Candidature : ${offer.title}")
            putExtra(Intent.EXTRA_TEXT, offer.generatedLetter)
            if (uris.isNotEmpty()) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (preferredPackage.isNotBlank()) {
                val installed = runCatching {
                    context.packageManager.getPackageInfo(preferredPackage, 0)
                }.isSuccess
                if (installed) {
                    setPackage(preferredPackage)
                }
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
