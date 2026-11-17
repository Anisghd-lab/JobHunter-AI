package com.ghdinteractivestudio.jobhunter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.model.CvDocumentSlot
import com.ghdinteractivestudio.jobhunter.domain.model.CvLanguage
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import com.ghdinteractivestudio.jobhunter.ui.components.GlassFilterChip
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSoftButton
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSolidButton
import com.ghdinteractivestudio.jobhunter.ui.components.glassCard
import com.ghdinteractivestudio.jobhunter.ui.components.glassFieldColors
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.jobhunter.ui.theme.Type
import com.ghdinteractivestudio.jobhunter.ui.theme.bold
import com.ghdinteractivestudio.jobhunter.ui.theme.monospaced
import com.ghdinteractivestudio.jobhunter.ui.theme.semibold
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CvScreen(
    profileFlow: StateFlow<UserProfile>,
    isAnalyzingCv: Boolean,
    onImportCvForLang: (Uri, CvLanguage) -> Unit,
    onDeleteCvForLang: (CvLanguage) -> Unit,
    onViewCvForLang: (CvLanguage) -> Unit,
    onReanalyzeWithAi: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rawProfile by profileFlow.collectAsState()
    val profile = remember(rawProfile) { rawProfile.migrateLegacySlotIfNeeded() }
    val dark = LocalDark.current
    var selectedViewMode by remember { mutableStateOf("analyse") } // "analyse", "raw_en", "raw_fr"
    var showEditDialog by remember { mutableStateOf(false) }
    var pendingPickLang by remember { mutableStateOf(CvLanguage.EN) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportCvForLang(it, pendingPickLang) }
    }

    fun launchPicker(lang: CvLanguage) {
        pendingPickLang = lang
        filePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword"
            )
        )
    }

    val hasRealCv = profile.parsedAt > 0L && (profile.combinedSearchCorpus().isNotBlank() || profile.fullName.isNotBlank())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // Hero Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Palette.emeraldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "MES CV (ANGLAIS & FRANÇAIS)",
                            style = Type.mono.bold,
                            color = Palette.emeraldAccent
                        )
                    }

                    if (hasRealCv) {
                        val activeSlots = listOfNotNull(profile.englishCv, profile.frenchCv).size
                        Box(
                            modifier = Modifier
                                .background(Palette.emeraldAccent.copy(alpha = 0.14f), Capsule())
                                .border(0.75.dp, Palette.emeraldAccent.copy(alpha = 0.45f), Capsule())
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$activeSlots/2 Actif(s)",
                                style = Type.label.bold,
                                color = Palette.emeraldAccent
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Importez vos CV originaux en Anglais et/ou Français. L'application combine les deux pour maximiser le Radar (Algérie, Golfe & Monde) et joint automatiquement le CV dans la langue de l'offre.",
                    style = Type.bodySmall,
                    color = Palette.secondary
                )

                if (hasRealCv) {
                    Spacer(Modifier.height(12.dp))
                    GlassSoftButton(
                        text = if (isAnalyzingCv) "Analyse IA en cours..." else "Ré-analyser avec l'IA Gratuite",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = Palette.electricBlue,
                        enabled = !isAnalyzingCv,
                        isLoading = isAnalyzingCv,
                        onClick = onReanalyzeWithAi,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dual CV Cards: English + French with uniform Import / View / Delete actions
        CvLanguageSlotCard(
            title = "🇬🇧 CV ANGLAIS (Golfe / International / Tech)",
            slot = profile.englishCv,
            isModifiedGlobal = profile.isModifiedCv,
            dateFormat = dateFormat,
            isAnalyzing = isAnalyzingCv,
            onImportClick = { launchPicker(CvLanguage.EN) },
            onViewClick = {
                val path = profile.englishCv?.filePath.orEmpty()
                if (path.isNotBlank() && File(path).exists()) {
                    onViewCvForLang(CvLanguage.EN)
                } else {
                    selectedViewMode = "raw_en"
                }
            },
            onDeleteClick = {
                if (selectedViewMode == "raw_en") selectedViewMode = "analyse"
                onDeleteCvForLang(CvLanguage.EN)
            }
        )

        Spacer(Modifier.height(10.dp))

        CvLanguageSlotCard(
            title = "🇫🇷 CV FRANÇAIS (Algérie / Europe / Admin)",
            slot = profile.frenchCv,
            isModifiedGlobal = profile.isModifiedCv,
            dateFormat = dateFormat,
            isAnalyzing = isAnalyzingCv,
            onImportClick = { launchPicker(CvLanguage.FR) },
            onViewClick = {
                val path = profile.frenchCv?.filePath.orEmpty()
                if (path.isNotBlank() && File(path).exists()) {
                    onViewCvForLang(CvLanguage.FR)
                } else {
                    selectedViewMode = "raw_fr"
                }
            },
            onDeleteClick = {
                if (selectedViewMode == "raw_fr") selectedViewMode = "analyse"
                onDeleteCvForLang(CvLanguage.FR)
            }
        )

        Spacer(Modifier.height(14.dp))

        if (!hasRealCv) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Palette.secondary,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Aucun CV importé pour le moment",
                        style = Type.body.bold,
                        color = Palette.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Importez votre CV Anglais et/ou Français ci-dessus.",
                        style = Type.bodySmall,
                        color = Palette.secondary
                    )
                }
            }
        } else {
            // Mode Switcher Bar (Analyse vs Texte EN vs Texte FR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassFilterChip(
                    text = "Profil Analysé",
                    selected = selectedViewMode == "analyse",
                    onClick = { selectedViewMode = "analyse" },
                    modifier = Modifier.weight(1f)
                )
                if (profile.englishCv != null) {
                    GlassFilterChip(
                        text = "Texte EN",
                        selected = selectedViewMode == "raw_en",
                        onClick = { selectedViewMode = "raw_en" },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (profile.frenchCv != null) {
                    GlassFilterChip(
                        text = "Texte FR",
                        selected = selectedViewMode == "raw_fr",
                        onClick = { selectedViewMode = "raw_fr" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (profile.isModifiedCv) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ Profil modifié — la version modifiée sera jointe.",
                        style = Type.label.bold,
                        color = Palette.electricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            onSaveProfile(
                                profile.copy(
                                    isModifiedCv = false,
                                    englishCv = profile.englishCv?.copy(isModified = false),
                                    frenchCv = profile.frenchCv?.copy(isModified = false)
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Original", style = Type.label.bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (selectedViewMode == "raw_en" || selectedViewMode == "raw_fr") {
                val chosenRaw = if (selectedViewMode == "raw_en") {
                    profile.englishCv?.rawText ?: profile.rawCvContent
                } else {
                    profile.frenchCv?.rawText ?: profile.rawCvContent
                }
                Text(
                    text = if (selectedViewMode == "raw_en") "TEXTE EXTRAIT — CV ANGLAIS" else "TEXTE EXTRAIT — CV FRANÇAIS",
                    style = Type.mono.bold,
                    color = Palette.secondary
                )
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.plateBackground, RoundedRectangle(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = chosenRaw.ifBlank { "Aucun contenu textuel extrait." },
                            style = Type.mono,
                            color = Palette.text
                        )
                    }
                }
            } else {
                // Structured Analysis Display
                // 1. Contact & Identity
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.fullName.ifBlank { "Candidat" },
                                style = Type.titleLarge.bold,
                                color = Palette.text,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Modifier", style = Type.label.bold)
                            }
                        }
                        if (profile.title.isNotBlank()) {
                            Text(
                                text = profile.title,
                                style = Type.body.semibold,
                                color = Palette.emeraldAccent
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ContactMail, contentDescription = null, tint = Palette.secondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = listOfNotNull(
                                    profile.email.ifBlank { null },
                                    profile.phone.ifBlank { null },
                                    profile.address.ifBlank { null }
                                ).joinToString(" • "),
                                style = Type.label.monospaced,
                                color = Palette.secondary
                            )
                        }

                        if (profile.summary.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (dark) Color(0x26000000) else Color(0x10000000),
                                        RoundedRectangle(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = profile.summary,
                                    style = Type.bodySmall,
                                    color = Palette.text
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 2. Real Skills Extracted
                if (profile.skills.isNotEmpty()) {
                    Text(
                        text = "COMPÉTENCES CLÉS DÉTECTÉES (${profile.skills.size})",
                        style = Type.mono.bold,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(16.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (skill in profile.skills) {
                                Box(
                                    modifier = Modifier
                                        .background(Palette.emeraldAccent.copy(alpha = 0.15f), Capsule())
                                        .border(0.5.dp, Palette.emeraldAccent.copy(alpha = 0.35f), Capsule())
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = skill,
                                        style = Type.label.bold,
                                        color = Palette.emeraldAccent
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // 3. Real Experiences
                if (profile.experiences.isNotEmpty()) {
                    Text(
                        text = "EXPÉRIENCES PROFESSIONNELLES (${profile.experiences.size})",
                        style = Type.mono.bold,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(8.dp))

                    for (exp in profile.experiences) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = null,
                                        tint = Palette.electricBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = exp.title,
                                        style = Type.body.bold,
                                        color = Palette.text
                                    )
                                }
                                if (exp.period.isNotBlank() || exp.company.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(exp.company.ifBlank { null }, exp.period.ifBlank { null }).joinToString(" • "),
                                        style = Type.label.monospaced,
                                        color = Palette.secondary
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = exp.description,
                                    style = Type.bodySmall,
                                    color = Palette.text
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // 4. Formations & Diplômes
                if (profile.educations.isNotEmpty()) {
                    Text(
                        text = "FORMATIONS & DIPLÔMES (${profile.educations.size})",
                        style = Type.mono.bold,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(8.dp))

                    for (edu in profile.educations) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Palette.attention,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = edu.degree,
                                        style = Type.body.bold,
                                        color = Palette.text
                                    )
                                }
                                val eduSub = listOfNotNull(edu.institution.ifBlank { null }, edu.year.ifBlank { null }).joinToString(" • ")
                                if (eduSub.isNotBlank()) {
                                    Text(
                                        text = eduSub,
                                        style = Type.label.monospaced,
                                        color = Palette.secondary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // 4b. Soft Skills
                if (profile.softSkills.isNotEmpty()) {
                    Text(
                        text = "ATOUTS & COMPÉTENCES TRANSVERSALES (${profile.softSkills.size})",
                        style = Type.mono.bold,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(14.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (soft in profile.softSkills) {
                                Box(
                                    modifier = Modifier
                                        .background(Palette.electricBlue.copy(alpha = 0.14f), Capsule())
                                        .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.35f), Capsule())
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(text = soft, style = Type.label.semibold, color = Palette.electricBlue)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // 4c. Certifications
                if (profile.certifications.isNotEmpty()) {
                    Text(
                        text = "CERTIFICATIONS & DÉVELOPPEMENT (${profile.certifications.size})",
                        style = Type.mono.bold,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    for (cert in profile.certifications) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(text = cert.name, style = Type.bodySmall.bold, color = Palette.text)
                                val sub = listOfNotNull(cert.issuer.ifBlank { null }, cert.year.ifBlank { null }).joinToString(" • ")
                                if (sub.isNotBlank()) {
                                    Text(text = sub, style = Type.label.monospaced, color = Palette.secondary)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 5. Langues
                if (profile.languages.isNotEmpty()) {
                    Text(
                        text = "LANGUES MAÎTRISÉES",
                        style = Type.mono.bold,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(14.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (lang in profile.languages) {
                                Box(
                                    modifier = Modifier
                                        .background(Palette.electricBlue.copy(alpha = 0.12f), Capsule())
                                        .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.3f), Capsule())
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = lang,
                                        style = Type.label.semibold,
                                        color = Palette.electricBlue
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(Modifier.height(90.dp))
    }

    if (showEditDialog) {
        var editedTitle by remember(profile) { mutableStateOf(profile.title) }
        var editedSummary by remember(profile) { mutableStateOf(profile.summary) }
        var editedSkills by remember(profile) { mutableStateOf(profile.skills.joinToString(", ")) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = if (dark) Color(0xFF131823) else Color(0xFFF3F7FC),
            title = {
                Text(
                    text = "Modifier mon CV",
                    style = Type.titleMedium.bold,
                    color = Palette.text
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Si vous enregistrez une modification ici, l'application joindra cette version modifiée au lieu du fichier PDF/DOCX d'origine.",
                        style = Type.label,
                        color = Palette.secondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Titre professionnel") },
                        singleLine = true,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedSummary,
                        onValueChange = { editedSummary = it },
                        label = { Text("Résumé / Accroche") },
                        minLines = 3,
                        maxLines = 5,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedSkills,
                        onValueChange = { editedSkills = it },
                        label = { Text("Compétences (séparées par virgule)") },
                        minLines = 2,
                        maxLines = 4,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GlassSolidButton(
                    text = "Enregistrer",
                    containerColor = Palette.emeraldSolidBg,
                    height = 38.dp,
                    onClick = {
                        val updatedSkills = editedSkills.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onSaveProfile(
                            profile.copy(
                                title = editedTitle.trim(),
                                summary = editedSummary.trim(),
                                skills = updatedSkills,
                                isModifiedCv = true,
                                parsedAt = System.currentTimeMillis()
                            )
                        )
                        showEditDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler", style = Type.label.bold, color = Palette.secondary)
                }
            }
        )
    }
}

@Composable
private fun CvLanguageSlotCard(
    title: String,
    slot: CvDocumentSlot?,
    isModifiedGlobal: Boolean,
    dateFormat: SimpleDateFormat,
    isAnalyzing: Boolean,
    onImportClick: () -> Unit,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val hasOriginalFile = slot != null && slot.filePath.isNotBlank() && File(slot.filePath).exists()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = Type.bodySmall.bold,
                    color = Palette.text,
                    modifier = Modifier.weight(1f)
                )
                if (slot != null) {
                    Box(
                        modifier = Modifier
                            .background(Palette.electricBlueSoft, Capsule())
                            .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.45f), Capsule())
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = slot.format.ifBlank { "PDF" },
                            style = Type.label.bold.monospaced,
                            color = Palette.electricBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (slot != null) {
                Text(
                    text = slot.fileName.ifBlank { "Document importé" },
                    style = Type.label.bold,
                    color = Palette.text
                )
                Text(
                    text = when {
                        slot.isModified || isModifiedGlobal -> "✏️ Version modifiée utilisée · Importé le ${dateFormat.format(Date(slot.parsedAt))}"
                        hasOriginalFile -> "✓ Original conservé intact · ${dateFormat.format(Date(slot.parsedAt))}"
                        else -> "⚠️ Ré-importez pour joindre le fichier original intact"
                    },
                    style = Type.label,
                    color = when {
                        slot.isModified || isModifiedGlobal -> Palette.electricBlue
                        hasOriginalFile -> Palette.emeraldAccent
                        else -> Palette.attention
                    }
                )
            } else {
                Text(
                    text = "Aucun fichier uploadé pour cette langue.",
                    style = Type.label,
                    color = Palette.secondary
                )
            }

            Spacer(Modifier.height(10.dp))

            // Uniform 3-button action bar (equal weight & fixed height 38dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassSolidButton(
                    text = if (slot != null) "Remplacer" else "Importer",
                    icon = Icons.Default.FolderOpen,
                    containerColor = Palette.emeraldSolidBg,
                    enabled = !isAnalyzing,
                    height = 38.dp,
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f)
                )

                GlassSoftButton(
                    text = "Voir",
                    icon = Icons.Default.Visibility,
                    accentColor = Palette.electricBlue,
                    enabled = slot != null && !isAnalyzing,
                    height = 38.dp,
                    onClick = onViewClick,
                    modifier = Modifier.weight(1f)
                )

                GlassSoftButton(
                    text = "Suppr.",
                    icon = Icons.Default.DeleteOutline,
                    accentColor = Palette.danger,
                    enabled = slot != null && !isAnalyzing,
                    height = 38.dp,
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
