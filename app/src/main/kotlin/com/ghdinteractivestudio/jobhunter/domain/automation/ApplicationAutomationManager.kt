package com.ghdinteractivestudio.jobhunter.domain.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.data.local.AppSettings
import com.ghdinteractivestudio.jobhunter.data.local.JobOfferDao
import com.ghdinteractivestudio.jobhunter.data.local.JobOfferEntity
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository
import com.ghdinteractivestudio.jobhunter.data.remote.JobScanner
import com.ghdinteractivestudio.jobhunter.domain.ai.CvTailoringEngine
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.document.DocxGenerator
import com.ghdinteractivestudio.jobhunter.domain.document.HtmlSanitizer
import com.ghdinteractivestudio.jobhunter.domain.document.PdfGenerator
import com.ghdinteractivestudio.jobhunter.domain.email.EmailAuthType
import com.ghdinteractivestudio.jobhunter.domain.email.EmailConfig
import com.ghdinteractivestudio.jobhunter.domain.email.EmailDispatcher
import com.ghdinteractivestudio.jobhunter.domain.email.IncomingReplyEmail
import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

data class ApplicationOutcome(
    val updatedOffer: JobOffer,
    val sentSilentlyByEmail: Boolean,
    val requiresWebAction: Boolean,
    val requiresEmailAppIntent: Boolean,
    val message: String,
    val usesOriginalCv: Boolean = false
)

data class AutoApplyBatchReport(
    val sentByEmailCount: Int = 0,
    val webPreparedCount: Int = 0,
    val enrichedEmailsFound: Int = 0,
    val dailyCountTotal: Int = 0,
    val dailyQuota: Int = 10,
    val skippedQuotaOrPaused: Boolean = false,
    val summary: String = ""
)

data class FollowUpCycleReport(
    val updatedCount: Int = 0,
    val replyUpdatesCount: Int = 0,
    val interviewAlerts: List<String> = emptyList(),
    val followUpsSentCount: Int = 0,
    val summary: String = ""
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
        forceTailoredCv: Boolean? = null,
        isAutonomous: Boolean = false
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

        // Generate MLDP-compliant Cover Letter file (DOCX vs PDF) matching offer language
        val letterBase = if (offerIsFrench) {
            "Lettre_Motivation_${safeCompany}_${uniqueSuffix}"
        } else {
            "Cover_Letter_${safeCompany}_${uniqueSuffix}"
        }
        val letterFile = if (useDocx) {
            val f = File(outputDir, "$letterBase.docx")
            DocxGenerator.generateMldpCoverLetterDocx(mldpLetter, f)
            f
        } else {
            val f = File(outputDir, "$letterBase.pdf")
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
                val cleanSubject = mldpLetter.subject
                    .removePrefix("Objet :")
                    .removePrefix("Objet:")
                    .removePrefix("Subject:")
                    .trim()
                val emailRes = dispatcher.sendApplicationEmail(
                    recipientEmail = targetEmail,
                    subject = "$cleanSubject - ${migratedProfile.fullName}",
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
            aiVerdict = liveOffer.aiVerdict.ifBlank { "CONVIENT" },
            autoApplied = liveOffer.autoApplied || (isAutonomous && sentSilently),
            lastAutoAttemptAt = if (isAutonomous) now else liveOffer.lastAutoAttemptAt
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
     * Upserts fresh web scan results without erasing already-discovered valid recruiter emails,
     * timestamps, or AI analysis on surviving DETECTED offers.
     */
    suspend fun preserveEnrichmentsAndReplaceDetected(
        context: Context,
        scannedOffers: List<JobOffer>
    ) = withContext(Dispatchers.IO) {
        if (scannedOffers.isEmpty()) return@withContext
        val dao = AppDatabase.getInstance(context).jobOfferDao()
        val existingMap = dao.getDetectedOffersOnce().associateBy { it.id }

        val merged = scannedOffers.map { offer ->
            val cleanNewEmail = offer.recruiterEmail.takeIf { HtmlSanitizer.isValidRecruiterEmail(it) }.orEmpty()
            val prev = existingMap[offer.id]
            val prevValidEmail = prev?.recruiterEmail?.takeIf { HtmlSanitizer.isValidRecruiterEmail(it) }.orEmpty()
            val finalEmail = cleanNewEmail.ifBlank { prevValidEmail }

            offer.copy(
                recruiterEmail = finalEmail,
                applicationMethod = if (finalEmail.isNotBlank()) "EMAIL" else offer.applicationMethod,
                lastAutoAttemptAt = prev?.lastAutoAttemptAt ?: 0L,
                matchScore = if (!prev?.aiReasoning.isNullOrBlank()) maxOf(offer.matchScore, prev!!.matchScore) else offer.matchScore,
                deadlineAt = if (offer.deadlineAt > 0L) offer.deadlineAt else (prev?.deadlineAt ?: 0L),
                deadlineLabel = offer.deadlineLabel.ifBlank { prev?.deadlineLabel.orEmpty() }
            )
        }

        dao.clearDetectedOffers()
        dao.insertAll(merged.map { JobOfferEntity.fromDomain(it) })
        dao.sanitizeInvalidRecruiterEmails()
    }

    /**
     * Resets all tracking counters (APPLIED, READY_TO_SEND, ANALYZED, INTERVIEW, REJECTED) back to DETECTED (0)
     * so the user can restart autonomous tracking cleanly without losing CVs, AI keys, or email settings.
     */
    suspend fun resetPipelineToZero(
        context: Context,
        settingsRepo: SettingsRepository
    ): Int = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).jobOfferDao()
        dao.sanitizeInvalidRecruiterEmails()
        val resetCount = dao.resetAllPipelineCounters()
        settingsRepo.update {
            it.copy(
                lastAutoApplyRunAt = 0L,
                lastInboxCheckAt = 0L,
                lastAutomationSummary = "Compteurs remis à 0 ($resetCount dossier(s) réinitialisé(s)) — prêt pour l'automatisation."
            )
        }
        resetCount
    }

    /**
     * Executes an intelligent, quota-guarded autonomous application batch:
     * 1) Reads top-scoring DETECTED/ANALYZED offers from Room sorted by matchScore DESC (not arbitrary source order).
     * 2) Scrapes live offer URLs for up to 10 top un-attempted candidates to discover hidden recruiter emails.
     * 3) Evaluates with AI if not yet reasoned, enforces deduplication across boards, respects daily/cycle Yahoo quotas,
     *    spaces sends by 8s, and records APPLIED (autoApplied=true) for real-time tracking in Suivi.
     */
    suspend fun runAutonomousAutoApplyBatch(
        context: Context,
        profile: UserProfile,
        settingsRepo: SettingsRepository,
        scanner: JobScanner,
        forceRun: Boolean = false
    ): AutoApplyBatchReport = withContext(Dispatchers.IO) {
        val migratedProfile = profile.migrateLegacySlotIfNeeded()
        val now = System.currentTimeMillis()
        val initialSettings = settingsRepo.settings.value

        if (migratedProfile.combinedSearchCorpus().isBlank() || migratedProfile.fullName.isBlank()) {
            val msg = "Importez d'abord votre CV (EN ou FR) dans Mon CV."
            settingsRepo.update { it.copy(lastAutoApplyRunAt = now, lastAutomationSummary = msg) }
            return@withContext AutoApplyBatchReport(dailyQuota = initialSettings.autoApplyMaxPerDay, summary = msg)
        }

        if (!forceRun && !initialSettings.autoApplyEnabled) {
            return@withContext AutoApplyBatchReport(
                dailyQuota = initialSettings.autoApplyMaxPerDay,
                summary = "Auto-candidature en pause."
            )
        }

        val dao = AppDatabase.getInstance(context).jobOfferDao()
        dao.sanitizeInvalidRecruiterEmails()

        val sentToday = dao.countAutoAppliedSince(startOfTodayMillis())
        if (initialSettings.autoApplyEmailOnly && !initialSettings.canSendSilently()) {
            val msg = if (initialSettings.smtpAuthPaused) {
                "SMTP en pause (erreur 535/AUTH005) — vérifiez App Password dans Agent 24/7."
            } else {
                "Configurez et testez votre Email + App Password pour l'auto-envoi silencieux."
            }
            settingsRepo.update { it.copy(lastAutoApplyRunAt = now, lastAutomationSummary = msg) }
            return@withContext AutoApplyBatchReport(
                dailyCountTotal = sentToday,
                dailyQuota = initialSettings.autoApplyMaxPerDay,
                skippedQuotaOrPaused = true,
                summary = msg
            )
        }

        if (sentToday >= initialSettings.autoApplyMaxPerDay && initialSettings.autoApplyEmailOnly) {
            val msg = "Quota sécurité atteint ($sentToday/${initialSettings.autoApplyMaxPerDay} aujourd'hui)."
            settingsRepo.update { it.copy(lastAutoApplyRunAt = now, lastAutomationSummary = msg) }
            return@withContext AutoApplyBatchReport(
                dailyCountTotal = sentToday,
                dailyQuota = initialSettings.autoApplyMaxPerDay,
                skippedQuotaOrPaused = true,
                summary = msg
            )
        }

        // Build normalized deduplication sets from already-applied / interview / rejected rows
        val alreadyApplied = dao.getAppliedSignatures().map { it.toDomain() }
        val appliedEmails = alreadyApplied
            .mapNotNull { it.recruiterEmail.trim().lowercase().takeIf(String::isNotBlank) }
            .toMutableSet()
        val appliedSignatures = alreadyApplied
            .map { normalizeSignatureKey(it.company, it.title) }
            .toMutableSet()

        // Re-score non-AI cached rows with latest domain guard so out-of-profile tech/medical titles drop out immediately
        val minEligibilityScore = (initialSettings.autoApplyMinScore - 15).coerceAtLeast(45)
        val rawCandidates = dao.getTopAutoApplyCandidates(limit = 120)
            .map { entity ->
                val domain = entity.toDomain()
                if (domain.aiReasoning.isBlank()) scanner.rescoreWithProfile(domain, migratedProfile) else domain
            }
            .filter { !DeadlineParser.isExpiredOrStale(it, now) && it.matchScore >= minEligibilityScore }
            .sortedWith(
                compareByDescending<JobOffer> { HtmlSanitizer.isValidRecruiterEmail(it.recruiterEmail) }
                    .thenByDescending { it.matchScore }
            )

        var enrichedCount = 0
        var sentCount = 0
        var webPreparedCount = 0
        var liveLookups = 0

        for (candidate in rawCandidates) {
            val currentSettings = settingsRepo.settings.value
            if (currentSettings.autoApplyEmailOnly && !currentSettings.canSendSilently()) break
            if (sentCount >= currentSettings.autoApplyMaxPerCycle && currentSettings.autoApplyEmailOnly) break
            if ((sentToday + sentCount) >= currentSettings.autoApplyMaxPerDay && currentSettings.autoApplyEmailOnly) break

            val sigKey = normalizeSignatureKey(candidate.company, candidate.title)
            val cleanCandEmail = candidate.recruiterEmail.trim().lowercase()
            if (sigKey in appliedSignatures || (cleanCandEmail.isNotBlank() && cleanCandEmail in appliedEmails)) {
                continue
            }

            // Skip recently attempted web-only offers (within 2h) so cycles progress deeper through unvisited ads
            val recentlyAttempted = candidate.lastAutoAttemptAt > 0L && (now - candidate.lastAutoAttemptAt) < 2 * 3_600_000L
            if (recentlyAttempted && cleanCandEmail.isBlank()) {
                continue
            }

            // 1. Live URL check to discover recruiter email or confirm not expired when missing email
            var enrichedOffer = candidate
            if (!HtmlSanitizer.isValidRecruiterEmail(enrichedOffer.recruiterEmail) &&
                enrichedOffer.url.startsWith("http") &&
                liveLookups < 35
            ) {
                liveLookups++
                val details = scanner.fetchOfferLiveDetails(enrichedOffer.url)
                if (details.isExpired) {
                    dao.insertOrUpdate(
                        JobOfferEntity.fromDomain(
                            enrichedOffer.copy(
                                status = JobStatus.ARCHIVED,
                                aiVerdict = "EXPIREE",
                                deadlineAt = details.deadlineAt.takeIf { it > 0L } ?: enrichedOffer.deadlineAt,
                                deadlineLabel = details.deadlineLabel.ifBlank { "Expirée" }
                            )
                        )
                    )
                    continue
                }
                val liveEmail = details.recruiterEmail.takeIf { HtmlSanitizer.isValidRecruiterEmail(it) }.orEmpty()
                if (liveEmail.isNotBlank()) enrichedCount++
                enrichedOffer = enrichedOffer.copy(
                    recruiterEmail = liveEmail,
                    applicationMethod = if (liveEmail.isNotBlank()) "EMAIL" else "WEBSITE",
                    deadlineAt = if (enrichedOffer.deadlineAt > 0L) enrichedOffer.deadlineAt else details.deadlineAt,
                    deadlineLabel = enrichedOffer.deadlineLabel.ifBlank { details.deadlineLabel },
                    lastAutoAttemptAt = now
                )
                dao.insertOrUpdate(JobOfferEntity.fromDomain(enrichedOffer))
            }

            val hasEmail = HtmlSanitizer.isValidRecruiterEmail(enrichedOffer.recruiterEmail)
            if (!hasEmail && currentSettings.autoApplyEmailOnly) {
                continue
            }

            // 2. AI evaluation if candidate score meets threshold or needs reasoning confirmation
            var evaluated = enrichedOffer
            if (evaluated.status == JobStatus.DETECTED &&
                currentSettings.hasAnyAiKey() &&
                evaluated.aiReasoning.isBlank()
            ) {
                evaluated = evaluateOfferWithAi(
                    context = context,
                    offer = evaluated,
                    profile = migratedProfile,
                    settings = currentSettings,
                    scanner = scanner
                ).getOrNull() ?: continue
            }

            if (evaluated.matchScore < currentSettings.autoApplyMinScore ||
                DeadlineParser.isExpiredOrStale(evaluated, now) ||
                evaluated.aiVerdict == "NON_ADAPTE"
            ) {
                continue
            }

            val latestSig = normalizeSignatureKey(evaluated.company, evaluated.title)
            val latestEmail = evaluated.recruiterEmail.trim().lowercase()
            if (latestSig in appliedSignatures || (latestEmail.isNotBlank() && latestEmail in appliedEmails)) {
                continue
            }

            // 3. Apply autonomously (Email silent dispatch or optional Web portal dossier prep)
            val liveSettings = settingsRepo.settings.value
            if (hasEmail) {
                if (!liveSettings.canSendSilently()) break
                if (sentCount >= liveSettings.autoApplyMaxPerCycle ||
                    (sentToday + sentCount) >= liveSettings.autoApplyMaxPerDay
                ) break

                if (sentCount > 0) delay(300L)

                val outcome = prepareAndApply(
                    context = context,
                    offer = evaluated,
                    profile = migratedProfile,
                    settings = liveSettings,
                    scanner = scanner,
                    sendSilentEmailIfConfigured = true,
                    isAutonomous = true
                ).getOrNull()

                if (outcome?.sentSilentlyByEmail == true) {
                    sentCount++
                    appliedSignatures.add(latestSig)
                    if (latestEmail.isNotBlank()) appliedEmails.add(latestEmail)
                }

                // Abort cycle cleanly if an SMTP auth error tripped smtpAuthPaused
                if (settingsRepo.settings.value.smtpAuthPaused) break
            } else if (!liveSettings.autoApplyEmailOnly && webPreparedCount < 5) {
                val outcome = prepareAndApply(
                    context = context,
                    offer = evaluated,
                    profile = migratedProfile,
                    settings = liveSettings,
                    scanner = scanner,
                    sendSilentEmailIfConfigured = false,
                    isAutonomous = true
                ).getOrNull()
                if (outcome != null && !outcome.sentSilentlyByEmail) {
                    webPreparedCount++
                    appliedSignatures.add(latestSig)
                }
            }
        }

        val finalToday = sentToday + sentCount
        val summary = buildString {
            append("$sentCount candidature(s) envoyée(s) auto")
            if (enrichedCount > 0) append(" · $enrichedCount email(s) découvert(s)")
            if (webPreparedCount > 0) append(" · $webPreparedCount dossier(s) Web")
            append(" · Total jour : $finalToday/${initialSettings.autoApplyMaxPerDay}")
            if (settingsRepo.settings.value.smtpAuthPaused) append(" · SMTP en pause (535)")
        }
        settingsRepo.update {
            it.copy(
                lastAutoApplyRunAt = now,
                lastAutomationSummary = summary
            )
        }

        AutoApplyBatchReport(
            sentByEmailCount = sentCount,
            webPreparedCount = webPreparedCount,
            enrichedEmailsFound = enrichedCount,
            dailyCountTotal = finalToday,
            dailyQuota = initialSettings.autoApplyMaxPerDay,
            skippedQuotaOrPaused = settingsRepo.settings.value.smtpAuthPaused,
            summary = summary
        )
    }

    /**
     * Autonomous AI Email Tracking & Relance Cycle:
     * 1) Scans IMAP INBOX (read-only EXAMINE + BODY.PEEK, 14-day SEARCH SINCE) and matches recruiter replies
     *    using multi-signal correlation across all tracked APPLIED/INTERVIEW/READY_TO_SEND dossiers.
     * 2) Classifies INTERVIEW / REJECTED / ACK with AI and saves exact From/Subject/Timestamp/Message-ID.
     * 3) Dispatches polite J+N AI follow-up when autoFollowUpEnabled is active (within maxFollowUpsPerOffer).
     */
    suspend fun runAutonomousEmailFollowUpCycle(
        context: Context,
        profile: UserProfile,
        settingsRepo: SettingsRepository
    ): FollowUpCycleReport = withContext(Dispatchers.IO) {
        val settings = settingsRepo.settings.value
        if (!settings.canSendSilently() || settings.smtpPassword.isBlank()) {
            val msg = if (settings.smtpAuthPaused) {
                "SMTP/IMAP en pause (erreur 535/AUTH005) — vérifiez App Password ou utilisez Appli Yahoo Mail."
            } else {
                "App Password SMTP requis pour la lecture IMAP & relance silencieuse."
            }
            return@withContext FollowUpCycleReport(summary = msg)
        }

        val dao = AppDatabase.getInstance(context).jobOfferDao()
        val trackedOffers = dao.getTrackedFollowUpCandidates().map { it.toDomain() }
        val now = System.currentTimeMillis()
        settingsRepo.update { it.copy(lastInboxCheckAt = now) }

        if (trackedOffers.isEmpty()) {
            return@withContext FollowUpCycleReport(summary = "Aucune candidature active à suivre dans la boîte.")
        }

        val engine = CvTailoringEngine(settings)
        val dispatcher = EmailDispatcher(buildEmailConfig(settings, profile))
        var updates = 0
        var replyUpdates = 0
        var followUpsSent = 0
        val highlights = mutableListOf<String>()
        val interviewAlerts = mutableListOf<String>()

        // 1. Scan IMAP INBOX (read-only)
        val imapResult = dispatcher.fetchRecentInboxReplies(maxMessages = 40, lookbackDays = 14)
        imapResult.onFailure { err ->
            val msg = err.message.orEmpty()
            if (msg.contains("AUTHENTICATIONFAILED", ignoreCase = true) ||
                msg.contains("535") ||
                msg.contains("AUTH005")
            ) {
                settingsRepo.update { it.copy(smtpAuthPaused = true) }
            }
            return@withContext FollowUpCycleReport(summary = "Erreur IMAP : ${msg.take(75)}")
        }

        val replies = imapResult.getOrNull().orEmpty()
        val senderSelf = settings.senderEmail.ifBlank { profile.email }.trim().lowercase()

        if (replies.isNotEmpty()) {
            // Process oldest-to-newest so the most recent reply defines final summary
            for (rep in replies.asReversed()) {
                val fromLower = rep.from.lowercase()
                val subjLower = rep.subject.lowercase()
                if (senderSelf.isNotBlank() && fromLower.contains(senderSelf) && !subjLower.startsWith("re:")) {
                    continue
                }

                // Correlate against highest-scoring tracked offer (>= 55 threshold)
                var bestOffer: JobOffer? = null
                var bestScore = 0
                for (offer in trackedOffers) {
                    val s = scoreReplyMatch(offer, rep)
                    if (s > bestScore) {
                        bestScore = s
                        bestOffer = offer
                    }
                }

                if (bestOffer != null && bestScore >= 55) {
                    val latestState = dao.getOfferById(bestOffer.id)?.toDomain() ?: bestOffer
                    if (rep.messageId.isNotBlank() && latestState.lastReplyMessageId == rep.messageId) {
                        continue
                    }

                    val (classifiedStatus, aiSummary) = engine.classifyRecruiterReply(latestState, rep)
                    val resolvedStatus = when {
                        latestState.status == JobStatus.INTERVIEW && classifiedStatus == JobStatus.APPLIED -> JobStatus.INTERVIEW
                        latestState.status == JobStatus.READY_TO_SEND && classifiedStatus == JobStatus.APPLIED -> JobStatus.APPLIED
                        else -> classifiedStatus
                    }

                    val replyTimestamp = parseEmailDateOrNow(rep.date, now)
                    val updated = latestState.copy(
                        status = resolvedStatus,
                        appliedAt = if (latestState.appliedAt == 0L && resolvedStatus != JobStatus.READY_TO_SEND) replyTimestamp else latestState.appliedAt,
                        lastReplySummary = aiSummary,
                        lastReplyFrom = rep.from.take(100),
                        lastReplySubject = rep.subject.take(120),
                        lastReplyAt = replyTimestamp,
                        lastReplyMessageId = rep.messageId
                    )
                    dao.insertOrUpdate(JobOfferEntity.fromDomain(updated))
                    updates++
                    replyUpdates++
                    highlights.add("${latestState.company}: $aiSummary")
                    if (resolvedStatus == JobStatus.INTERVIEW) {
                        interviewAlerts.add("${latestState.company} (${latestState.title})")
                    }
                }
            }
        }

        // 2. Auto Follow-up (J+N) on unanswered APPLIED offers
        if (settings.autoFollowUpEnabled && !settingsRepo.settings.value.smtpAuthPaused) {
            val applied = dao.getOffersByStatusOnce(JobStatus.APPLIED.name).map { it.toDomain() }
            val delayMs = settings.followUpDelayDays.coerceIn(2, 14) * 86_400_000L
            for (offer in applied.take(3)) {
                if (followUpsSent >= 2) break
                val current = dao.getOfferById(offer.id)?.toDomain() ?: continue
                if (current.status != JobStatus.APPLIED) continue
                if (current.followUpCount >= settings.maxFollowUpsPerOffer) continue
                if (current.lastReplyMessageId.isNotBlank()) continue
                if (current.recruiterEmail.isBlank() || current.appliedAt == 0L) continue
                if (now - current.appliedAt < delayMs) continue
                if (DeadlineParser.isExpiredOrStale(current, now)) continue

                if (followUpsSent > 0) delay(6_000L)
                sendFollowUpNow(context, current, profile, settings).onSuccess {
                    updates++
                    followUpsSent++
                    highlights.add("Relance IA envoyée à ${current.company}")
                }
            }
        }

        val summaryText = highlights.joinToString(" · ").ifBlank {
            "Boîte vérifiée (${replies.size} msg récents) — aucune nouvelle réponse ciblée."
        }
        FollowUpCycleReport(
            updatedCount = updates,
            replyUpdatesCount = replyUpdates,
            interviewAlerts = interviewAlerts,
            followUpsSentCount = followUpsSent,
            summary = summaryText
        )
    }

    private fun scoreReplyMatch(offer: JobOffer, reply: IncomingReplyEmail): Int {
        val hayFrom = reply.from.lowercase()
        val haySubj = reply.subject.lowercase()
        val hayBody = reply.snippet.lowercase()
        val hayAll = "$hayFrom $haySubj $hayBody"

        var score = 0
        val recEmail = offer.recruiterEmail.trim().lowercase()
        if (recEmail.isNotBlank() && hayFrom.contains(recEmail)) {
            score += 100
        } else if (recEmail.isNotBlank() && hayAll.contains(recEmail)) {
            score += 80
        }

        val recDomain = recEmail.substringAfter("@", "")
        val genericDomains = setOf("gmail.com", "yahoo.com", "yahoo.fr", "outlook.com", "hotmail.com", "live.com", "icloud.com", "proton.me", "aplitrak.com")
        if (recDomain.length >= 4 && recDomain !in genericDomains && (hayFrom.contains(recDomain) || haySubj.contains(recDomain))) {
            score += 70
        }

        val companyTokens = offer.company.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 4 && it !in setOf("sarl", "eurl", "group", "groupe", "company", "entrepris", "remote", "corp", "inc") }
        val companyNameClean = offer.company.lowercase().trim()
        if (companyNameClean.length >= 4 && (hayFrom.contains(companyNameClean) || haySubj.contains(companyNameClean))) {
            score += 55
        } else if (companyTokens.isNotEmpty() && companyTokens.all { hayFrom.contains(it) || haySubj.contains(it) }) {
            score += 50
        } else if (companyNameClean.length >= 4 && hayBody.contains(companyNameClean)) {
            score += 35
        }

        val titleWords = offer.title.lowercase()
            .replace(Regex("[^a-zà-ÿ0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 4 && it !in setOf("pour", "avec", "dans", "remote", "senior", "junior", "poste") }
            .distinct()
        if (titleWords.size >= 2) {
            val hits = titleWords.count { haySubj.contains(it) || hayBody.contains(it) }
            if (hits >= 2) score += 45
        }

        if (score >= 35 && listOf("re:", "candidature", "application", "entretien", "interview", "recrutement", "recruitment", "shortlist").any { haySubj.contains(it) }) {
            score += 15
        }
        return score
    }

    private fun parseEmailDateOrNow(rawDate: String, fallbackNow: Long): Long {
        if (rawDate.isBlank()) return fallbackNow
        val parsed = DeadlineParser.parseTimestamp(rawDate)
        if (parsed > 0L) return parsed
        val patterns = listOf("EEE, d MMM yyyy HH:mm:ss Z", "d MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss z")
        for (pat in patterns) {
            runCatching {
                val fmt = java.text.SimpleDateFormat(pat, java.util.Locale.US)
                val ts = fmt.parse(rawDate.trim())?.time ?: 0L
                if (ts > 0L) return ts
            }
        }
        return fallbackNow
    }

    fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun normalizeSignatureKey(company: String, title: String): String {
        val c = company.lowercase().replace(Regex("[^a-z0-9]"), "")
        val t = title.lowercase().replace(Regex("[^a-z0-9]"), "").take(32)
        return "$c|$t"
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
            val isFr = CvTailoringEngine().isFrenchOffer(offer)
            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isFr) "Candidature : ${offer.title}" else "Application: ${offer.title}"
            )
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
