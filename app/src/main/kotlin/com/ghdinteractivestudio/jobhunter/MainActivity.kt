package com.ghdinteractivestudio.jobhunter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.data.local.JobOfferEntity
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository
import com.ghdinteractivestudio.jobhunter.data.local.UserProfileEntity
import com.ghdinteractivestudio.jobhunter.data.remote.JobScanner
import com.ghdinteractivestudio.jobhunter.domain.ai.CvTailoringEngine
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationAutomationManager
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationOutcome
import com.ghdinteractivestudio.jobhunter.domain.document.CvParser
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import com.ghdinteractivestudio.jobhunter.ui.components.GlassTabBar
import com.ghdinteractivestudio.jobhunter.ui.screens.AgentScreen
import com.ghdinteractivestudio.jobhunter.ui.screens.CvScreen
import com.ghdinteractivestudio.jobhunter.ui.screens.PipelineScreen
import com.ghdinteractivestudio.jobhunter.ui.screens.RadarScreen
import com.ghdinteractivestudio.jobhunter.ui.theme.JobHunterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val allOffersState = MutableStateFlow<List<JobOffer>>(emptyList())
    private val userProfileState = MutableStateFlow(UserProfile())
    private val isScanningState = MutableStateFlow(false)
    private val isAnalyzingCvState = MutableStateFlow(false)
    private val busyOfferIdState = MutableStateFlow<String?>(null)
    private val activeOutcomeState = MutableStateFlow<ApplicationOutcome?>(null)

    private val scanner = JobScanner()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+ so 24/7 Agent status notifications appear properly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val db = AppDatabase.getInstance(this)
        val offerDao = db.jobOfferDao()
        val profileDao = db.userProfileDao()
        val settingsRepo = SettingsRepository.getInstance(this)

        handleIntent(intent)

        // Observe Room Database safely tied to Activity lifecycle (zero leaks)
        lifecycleScope.launch {
            offerDao.getAllOffers().map { list -> list.map { it.toDomain() } }.collect { offers ->
                allOffersState.value = offers
            }
        }
        lifecycleScope.launch {
            profileDao.getProfileFlow().collect { entity ->
                if (entity != null) {
                    val domainProfile = entity.toDomain()
                    userProfileState.value = domainProfile
                    autoConfigureEmailPresetIfBlank(domainProfile, settingsRepo)
                }
            }
        }

        // Purge expired offers immediately and perform initial scan only if DB is empty or stale (> 10 min)
        lifecycleScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val minPub = now - (DeadlineParser.DEFAULT_MAX_OFFER_AGE_DAYS * 86_400_000L)
            offerDao.purgeExpiredDetectedOffers(now, minPub)

            val currentOffers = offerDao.getAllOffers().firstOrNull() ?: emptyList()
            val lastTs = settingsRepo.settings.value.lastScanTimestamp
            if (currentOffers.isEmpty() || (now - lastTs) > 10 * 60_000L) {
                val existingProfile = profileDao.getProfile()?.toDomain()
                handleRefreshScan(existingProfile, settingsRepo, showToast = false)
            }
        }

        setContent {
            val settings by settingsRepo.settings.collectAsState()
            var currentTab by remember { mutableStateOf("radar") }
            val coroutineScope = rememberCoroutineScope()

            val isScanning by isScanningState.collectAsState()
            val isAnalyzingCv by isAnalyzingCvState.collectAsState()
            val busyOfferId by busyOfferIdState.collectAsState()
            val activeOutcome by activeOutcomeState.collectAsState()

            JobHunterTheme(darkTheme = settings.isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    when (currentTab) {
                        "radar" -> RadarScreen(
                            offersFlow = allOffersState,
                            isScanning = isScanning,
                            busyOfferId = busyOfferId,
                            activeOutcome = activeOutcome,
                            onDismissOutcome = { activeOutcomeState.value = null },
                            onMarkAsApplied = { appliedOffer ->
                                coroutineScope.launch {
                                    markOfferStatus(
                                        appliedOffer.copy(appliedAt = System.currentTimeMillis()),
                                        JobStatus.APPLIED
                                    )
                                    activeOutcomeState.value = null
                                    Toast.makeText(this@MainActivity, "Candidature enregistrée dans Suivi ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRefreshScanClick = {
                                coroutineScope.launch {
                                    handleRefreshScan(userProfileState.value, settingsRepo, showToast = true)
                                }
                            },
                            onAnalyzeOfferClick = { offer ->
                                coroutineScope.launch {
                                    handleAnalyzeSingleOffer(offer, settingsRepo)
                                }
                            },
                            onApplyClick = { offer ->
                                coroutineScope.launch {
                                    handleApplyToJob(offer, settingsRepo)
                                }
                            },
                            onRegenerateOfferClick = { offer ->
                                coroutineScope.launch {
                                    handleApplyToJob(offer, settingsRepo, sendSilentEmail = false)
                                }
                            },
                            onToggleCvMode = { offer, useTailored ->
                                coroutineScope.launch {
                                    handleApplyToJob(
                                        offer = offer,
                                        settingsRepo = settingsRepo,
                                        sendSilentEmail = false,
                                        forceTailoredCv = useTailored
                                    )
                                }
                            }
                        )

                        "pipeline" -> PipelineScreen(
                            allOffersFlow = allOffersState,
                            onUpdateStatus = { offer, newStatus ->
                                coroutineScope.launch {
                                    markOfferStatus(offer, newStatus)
                                }
                            },
                            onPrepareOrDispatchClick = { offer ->
                                coroutineScope.launch {
                                    if (offer.tailoredCvPath.isNotBlank() && offer.generatedLetter.isNotBlank()) {
                                        activeOutcomeState.value = ApplicationOutcome(
                                            updatedOffer = offer,
                                            sentSilentlyByEmail = false,
                                            requiresWebAction = offer.recruiterEmail.isBlank(),
                                            requiresEmailAppIntent = offer.recruiterEmail.isNotBlank(),
                                            message = if (offer.recruiterEmail.isNotBlank()) {
                                                "Email et pièces jointes prêts pour ${offer.recruiterEmail}"
                                            } else {
                                                "CV et Lettre prêts à déposer sur ${offer.sourcePlatform}"
                                            },
                                            usesOriginalCv = offer.tailoredCvPath.contains("_Original_")
                                        )
                                        currentTab = "radar"
                                    } else {
                                        handleApplyToJob(offer, settingsRepo)
                                    }
                                }
                            },
                            onRemoveFromPipeline = { offer ->
                                coroutineScope.launch {
                                    markOfferStatus(offer, JobStatus.DETECTED)
                                }
                            }
                        )

                        "cv" -> CvScreen(
                            profileFlow = userProfileState,
                            isAnalyzingCv = isAnalyzingCv,
                            onImportCv = { uri ->
                                coroutineScope.launch {
                                    handleImportCv(uri, settingsRepo)
                                }
                            },
                            onReanalyzeWithAi = {
                                coroutineScope.launch {
                                    handleReanalyzeCvWithAi(settingsRepo)
                                }
                            },
                            onSaveProfile = { editedProfile ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    AppDatabase.getInstance(this@MainActivity)
                                        .userProfileDao()
                                        .saveProfile(UserProfileEntity.fromDomain(editedProfile))
                                    userProfileState.value = editedProfile
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            if (editedProfile.isModifiedCv) "CV modifié enregistré !" else "CV original rétabli ✓",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )

                        "agent" -> AgentScreen(settingsRepo = settingsRepo)
                    }

                    // Identical CodeStation Floating Liquid Glass Navigation Bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    ) {
                        GlassTabBar(
                            selectedRoute = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val settingsRepo = SettingsRepository.getInstance(this)
        val testPath = intent?.getStringExtra("import_path")
        if (!testPath.isNullOrBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val file = File(testPath)
                if (file.exists()) {
                    handleImportCv(Uri.fromFile(file), settingsRepo)
                }
            }
        } else if (intent?.getBooleanExtra("reanalyze_cv", false) == true) {
            lifecycleScope.launch(Dispatchers.IO) {
                val existing = AppDatabase.getInstance(this@MainActivity).userProfileDao().getProfile()?.toDomain()
                if (existing != null && existing.rawCvContent.isNotBlank()) {
                    userProfileState.value = existing
                    handleReanalyzeCvWithAi(settingsRepo)
                    handleRefreshScan(userProfileState.value, settingsRepo, showToast = false)
                }
            }
        }
    }

    private fun autoConfigureEmailPresetIfBlank(profile: UserProfile, settingsRepo: SettingsRepository) {
        val current = settingsRepo.settings.value
        if (current.senderEmail.isBlank() && profile.email.isNotBlank()) {
            val lower = profile.email.lowercase()
            val (host, port) = when {
                lower.contains("@yahoo.") -> "smtp.mail.yahoo.com" to 465
                lower.contains("@outlook.") || lower.contains("@hotmail.") || lower.contains("@live.") -> "smtp-mail.outlook.com" to 587
                else -> "smtp.gmail.com" to 465
            }
            settingsRepo.update {
                it.copy(senderEmail = profile.email, smtpHost = host, smtpPort = port)
            }
        }
    }

    private suspend fun handleRefreshScan(
        profile: UserProfile?,
        settingsRepo: SettingsRepository,
        showToast: Boolean = true
    ) = withContext(Dispatchers.IO) {
        if (isScanningState.value) return@withContext
        isScanningState.value = true
        runCatching {
            val db = AppDatabase.getInstance(this@MainActivity)
            val now = System.currentTimeMillis()
            val minPub = now - (DeadlineParser.DEFAULT_MAX_OFFER_AGE_DAYS * 86_400_000L)
            db.jobOfferDao().purgeExpiredDetectedOffers(now, minPub)

            val offers = scanner.scanOffers(profile)
            if (offers.isNotEmpty()) {
                db.jobOfferDao().clearDetectedOffers()
                db.jobOfferDao().insertAll(offers.map { JobOfferEntity.fromDomain(it) })
                settingsRepo.update { it.copy(lastScanTimestamp = System.currentTimeMillis()) }
            }
            if (showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "${offers.size} offres actives valides (Algérie & Monde)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.onFailure { err ->
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Erreur scan web : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
        isScanningState.value = false
    }

    private suspend fun handleImportCv(uri: Uri, settingsRepo: SettingsRepository) = withContext(Dispatchers.IO) {
        isAnalyzingCvState.value = true
        runCatching {
            val extractedData = CvParser.parseCvFile(this@MainActivity, uri)

            // Save persistent backup copy of original user CV file cleanly inside app storage
            val backupFile = runCatching {
                val cvDir = File(filesDir, "source_cv").apply { mkdirs() }
                val dest = File(cvDir, extractedData.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))
                if (uri.scheme == "file" && uri.path != null) {
                    File(uri.path!!).copyTo(dest, overwrite = true)
                } else {
                    contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                dest.absolutePath
            }.getOrDefault("")

            val settings = settingsRepo.settings.value
            val engine = CvTailoringEngine(settings)

            // Parse with Free LLM Intelligence & Delicacy when available, else clean deterministic fallback
            val aiParsed = if (extractedData.rawText.isNotBlank() && settings.hasAnyAiKey()) {
                engine.parseCvWithIntelligence(extractedData.rawText).getOrNull()
            } else null

            val baseProfile = aiParsed ?: extractedData.deterministicProfile

            // Always ensure key deterministic fields are preserved and zero hallucination overrides
            val det = extractedData.deterministicProfile
            val finalProfile = baseProfile.copy(
                fullName = baseProfile.fullName.ifBlank { det.fullName },
                email = baseProfile.email.ifBlank { det.email },
                phone = baseProfile.phone.ifBlank { det.phone },
                title = baseProfile.title.ifBlank { det.title },
                skills = baseProfile.skills.ifEmpty { det.skills },
                experiences = baseProfile.experiences.ifEmpty { det.experiences },
                educations = baseProfile.educations.ifEmpty { det.educations },
                languages = baseProfile.languages.ifEmpty { det.languages },
                sourceCvFileName = extractedData.fileName,
                sourceCvFormat = extractedData.format,
                sourceCvPath = backupFile,
                rawCvContent = extractedData.rawText,
                isModifiedCv = false,
                parsedAt = System.currentTimeMillis()
            )

            val db = AppDatabase.getInstance(this@MainActivity)
            db.userProfileDao().saveProfile(UserProfileEntity.fromDomain(finalProfile))
            userProfileState.value = finalProfile

            val usedAi = aiParsed != null
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    if (usedAi) "CV analysé avec l'IA Gratuite !" else "CV réel '${extractedData.fileName}' importé !",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Automatically trigger a targeted job radar scan matching the freshly imported CV
            handleRefreshScan(finalProfile, settingsRepo, showToast = false)
        }.onFailure { err ->
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Erreur analyse CV : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
        isAnalyzingCvState.value = false
    }

    private suspend fun handleReanalyzeCvWithAi(settingsRepo: SettingsRepository) = withContext(Dispatchers.IO) {
        val current = userProfileState.value
        if (current.rawCvContent.isBlank()) return@withContext
        isAnalyzingCvState.value = true
        runCatching {
            val settings = settingsRepo.settings.value
            val engine = CvTailoringEngine(settings)
            val parsed = engine.parseCvWithIntelligence(current.rawCvContent).getOrThrow()

            val updated = parsed.copy(
                fullName = parsed.fullName.ifBlank { current.fullName },
                email = parsed.email.ifBlank { current.email },
                phone = parsed.phone.ifBlank { current.phone },
                sourceCvFileName = current.sourceCvFileName,
                sourceCvFormat = current.sourceCvFormat,
                sourceCvPath = current.sourceCvPath,
                rawCvContent = current.rawCvContent,
                isModifiedCv = current.isModifiedCv,
                parsedAt = System.currentTimeMillis()
            )
            AppDatabase.getInstance(this@MainActivity).userProfileDao()
                .saveProfile(UserProfileEntity.fromDomain(updated))
            userProfileState.value = updated

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Analyse IA du CV mise à jour !", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { err ->
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Erreur IA : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
        isAnalyzingCvState.value = false
    }

    private suspend fun handleAnalyzeSingleOffer(offer: JobOffer, settingsRepo: SettingsRepository) = withContext(Dispatchers.IO) {
        busyOfferIdState.value = offer.id
        val res = ApplicationAutomationManager.evaluateOfferWithAi(
            context = this@MainActivity,
            offer = offer,
            profile = userProfileState.value,
            settings = settingsRepo.settings.value,
            scanner = scanner
        )
        withContext(Dispatchers.Main) {
            res.onSuccess { eval ->
                Toast.makeText(
                    this@MainActivity,
                    "Match ${eval.matchScore}% (${eval.aiVerdict})",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, err.message ?: "Erreur comparaison IA", Toast.LENGTH_LONG).show()
            }
        }
        busyOfferIdState.value = null
    }

    private suspend fun handleApplyToJob(
        offer: JobOffer,
        settingsRepo: SettingsRepository,
        sendSilentEmail: Boolean = true,
        forceTailoredCv: Boolean? = null
    ) = withContext(Dispatchers.IO) {
        busyOfferIdState.value = offer.id
        val res = ApplicationAutomationManager.prepareAndApply(
            context = this@MainActivity,
            offer = offer,
            profile = userProfileState.value,
            settings = settingsRepo.settings.value,
            scanner = scanner,
            sendSilentEmailIfConfigured = sendSilentEmail,
            forceTailoredCv = forceTailoredCv
        )
        withContext(Dispatchers.Main) {
            res.onSuccess { outcome ->
                Toast.makeText(this@MainActivity, outcome.message, Toast.LENGTH_SHORT).show()
                if (!outcome.sentSilentlyByEmail) {
                    activeOutcomeState.value = outcome
                }
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, "Erreur : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
        busyOfferIdState.value = null
    }

    private suspend fun markOfferStatus(offer: JobOffer, status: JobStatus) = withContext(Dispatchers.IO) {
        val updated = offer.copy(
            status = status,
            appliedAt = if (status == JobStatus.APPLIED && offer.appliedAt == 0L) System.currentTimeMillis() else offer.appliedAt
        )
        AppDatabase.getInstance(this@MainActivity).jobOfferDao()
            .insertOrUpdate(JobOfferEntity.fromDomain(updated))
    }
}
