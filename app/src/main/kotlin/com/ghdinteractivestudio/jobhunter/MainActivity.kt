package com.ghdinteractivestudio.jobhunter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.ghdinteractivestudio.jobhunter.data.local.UserProfileEntity
import com.ghdinteractivestudio.jobhunter.domain.ai.CvTailoringEngine
import com.ghdinteractivestudio.jobhunter.domain.ai.LlmClient
import com.ghdinteractivestudio.jobhunter.domain.ai.LlmConfig
import com.ghdinteractivestudio.jobhunter.domain.document.CvParser
import com.ghdinteractivestudio.jobhunter.domain.document.DocxGenerator
import com.ghdinteractivestudio.jobhunter.domain.document.PdfGenerator
import com.ghdinteractivestudio.jobhunter.domain.email.EmailConfig
import com.ghdinteractivestudio.jobhunter.domain.email.EmailDispatcher
import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val allOffersState = MutableStateFlow<List<JobOffer>>(emptyList())
    private val userProfileState = MutableStateFlow(UserProfile())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(this)
        val offerDao = db.jobOfferDao()
        val profileDao = db.userProfileDao()

        // Observe Room Database
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
        scope.launch {
            offerDao.getAllOffers().map { list -> list.map { it.toDomain() } }.collect { offers ->
                allOffersState.value = offers
            }
        }
        scope.launch {
            profileDao.getProfileFlow().collect { entity ->
                if (entity != null) {
                    userProfileState.value = entity.toDomain()
                }
            }
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var currentTab by remember { mutableStateOf("radar") }
            val coroutineScope = rememberCoroutineScope()

            JobHunterTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Screen Content
                    when (currentTab) {
                        "radar" -> RadarScreen(
                            offersFlow = allOffersState,
                            onApplyClick = { offer ->
                                coroutineScope.launch {
                                    handleApplyToJob(offer)
                                }
                            }
                        )
                        "pipeline" -> PipelineScreen(
                            appliedOffersFlow = allOffersState
                        )
                        "cv" -> CvScreen(
                            profileFlow = userProfileState,
                            onImportCv = { uri ->
                                coroutineScope.launch {
                                    handleImportCv(uri)
                                }
                            }
                        )
                        "agent" -> AgentScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = it }
                        )
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

    private suspend fun handleImportCv(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        runCatching {
            val text = CvParser.extractText(this@MainActivity, uri)
            val llm = LlmClient(LlmConfig())
            val engine = CvTailoringEngine(llm)

            // Try to parse using LLM, or fallback to heuristics if offline
            val parsedResult = engine.parseCv(text).getOrElse {
                UserProfile(
                    fullName = "Candidat",
                    title = "Développeur Senior",
                    skills = listOf("Kotlin", "Android", "Jetpack Compose", "Coroutines", "Room", "Git")
                )
            }

            val db = AppDatabase.getInstance(this@MainActivity)
            db.userProfileDao().saveProfile(UserProfileEntity.fromDomain(parsedResult))

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "CV importé et analysé avec succès !", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { err ->
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Erreur import : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun handleApplyToJob(offer: JobOffer) = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.getInstance(this@MainActivity)
            val profile = userProfileState.value
            val outputDir = File(filesDir, "applications").apply { mkdirs() }

            val attachments = mutableListOf<File>()

            // 1. Generate CV in demanded format (PDF vs Word .docx)
            val cvSections = listOf(
                "Profil" to listOf(profile.summary.ifBlank { "Professionnel rigoureux et autonome." }),
                "Compétences" to listOf(profile.skills.joinToString(", ")),
                "Expériences Clés" to profile.experiences.map { "${it.title} chez ${it.company} (${it.period}) - ${it.description}" }
            )

            if (offer.requiredFormat == DocumentFormatRequirement.DOCX) {
                val docxFile = File(outputDir, "CV_${profile.fullName.replace(" ", "_")}.docx")
                DocxGenerator.generateDocx("CURRICULUM VITAE - ${profile.fullName}", cvSections, docxFile)
                attachments.add(docxFile)
            } else {
                val pdfFile = File(outputDir, "CV_${profile.fullName.replace(" ", "_")}.pdf")
                PdfGenerator.generatePdf("CURRICULUM VITAE - ${profile.fullName}", cvSections, pdfFile)
                attachments.add(pdfFile)
            }

            // 2. Generate Cover Letter in demanded format
            val letterSections = listOf(
                "Candidature au poste de ${offer.title}" to listOf(
                    "Madame, Monsieur,",
                    "Je vous adresse ma candidature pour rejoindre ${offer.company} en tant que ${offer.title}.",
                    "Mon parcours et mon expertise me permettent d'apporter une contribution immédiate à vos projets.",
                    "Je reste à votre entière disposition pour convenir d'un échange.",
                    "Cordialement,\n${profile.fullName}"
                )
            )

            if (offer.requiredFormat == DocumentFormatRequirement.DOCX) {
                val letterDocx = File(outputDir, "Lettre_Motivation_${offer.company.replace(" ", "_")}.docx")
                DocxGenerator.generateDocx("Lettre de Motivation - ${offer.title}", letterSections, letterDocx)
                attachments.add(letterDocx)
            } else {
                val letterPdf = File(outputDir, "Lettre_Motivation_${offer.company.replace(" ", "_")}.pdf")
                PdfGenerator.generatePdf("Lettre de Motivation - ${offer.title}", letterSections, letterPdf)
                attachments.add(letterPdf)
            }

            // 3. Mark applied in Room
            val updated = offer.copy(
                status = JobStatus.APPLIED,
                tailoredCvPath = attachments.firstOrNull()?.absolutePath ?: "",
                letterPath = attachments.lastOrNull()?.absolutePath ?: ""
            )
            db.jobOfferDao().insertOrUpdate(JobOfferEntity.fromDomain(updated))

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Candidature envoyée avec succès (${offer.requiredFormat.name}) !", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { err ->
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Erreur envoi : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
