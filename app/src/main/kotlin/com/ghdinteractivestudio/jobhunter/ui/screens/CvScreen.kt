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
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import com.ghdinteractivestudio.jobhunter.ui.components.GlassStyle
import com.ghdinteractivestudio.jobhunter.ui.components.glass
import com.ghdinteractivestudio.jobhunter.ui.components.glassCard
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.jobhunter.ui.theme.Type
import com.ghdinteractivestudio.jobhunter.ui.theme.bold
import com.ghdinteractivestudio.jobhunter.ui.theme.monospaced
import com.ghdinteractivestudio.jobhunter.ui.theme.semibold
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CvScreen(
    profileFlow: StateFlow<UserProfile>,
    onImportCv: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by profileFlow.collectAsState()
    val dark = LocalDark.current
    var selectedViewMode by remember { mutableStateOf("analyse") } // "analyse" or "raw_text"

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportCv(it) }
    }

    val hasRealCv = profile.parsedAt > 0L && (profile.rawCvContent.isNotBlank() || profile.fullName.isNotBlank())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // Hero Card (Import & Header)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(20.dp)
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "MON CV (100% RÉEL)",
                            style = Type.mono.bold,
                            color = Palette.emeraldAccent
                        )
                    }

                    if (hasRealCv) {
                        Box(
                            modifier = Modifier
                                .background(Palette.emeraldAccent.copy(alpha = 0.16f), Capsule())
                                .border(0.5.dp, Palette.emeraldAccent.copy(alpha = 0.4f), Capsule())
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Actif & Analysé",
                                style = Type.label.bold,
                                color = Palette.emeraldAccent
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Importez votre document original (PDF ou Word .docx). Toutes les compétences et expériences sont extraites directement sans aucune donnée fictive.",
                    style = Type.bodySmall,
                    color = Palette.secondary
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword"
                            )
                        )
                    },
                    shape = Capsule(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.emeraldAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasRealCv) "Remplacer le CV (Nouveau fichier)" else "Importer mon CV (PDF / Word .docx)",
                        style = Type.body.semibold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!hasRealCv) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Palette.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Aucun CV chargé pour le moment",
                        style = Type.body.bold,
                        color = Palette.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Importez votre fichier pour visualiser l'extraction réelle de votre profil.",
                        style = Type.bodySmall,
                        color = Palette.secondary
                    )
                }
            }
        } else {
            // Document File Details Card
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
                        Column {
                            Text(
                                text = profile.sourceCvFileName.ifBlank { "Document CV" },
                                style = Type.titleMedium.bold,
                                color = Palette.text
                            )
                            Text(
                                text = "Analysé le ${dateFormat.format(Date(profile.parsedAt))}",
                                style = Type.label.monospaced,
                                color = Palette.secondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Palette.electricBlue.copy(alpha = 0.16f), Capsule())
                                .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.4f), Capsule())
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = profile.sourceCvFormat,
                                style = Type.label.bold.monospaced,
                                color = Palette.electricBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(Capsule(), style = GlassStyle.Clear)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selectedViewMode = "analyse" },
                            shape = Capsule(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedViewMode == "analyse") Palette.emeraldAccent else Color.Transparent,
                                contentColor = if (selectedViewMode == "analyse") Color.White else Palette.secondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Compétences & Profil", style = Type.label.bold)
                        }

                        Button(
                            onClick = { selectedViewMode = "raw_text" },
                            shape = Capsule(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedViewMode == "raw_text") Palette.emeraldAccent else Color.Transparent,
                                contentColor = if (selectedViewMode == "raw_text") Color.White else Palette.secondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Texte Intégral du CV", style = Type.label.bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedViewMode == "raw_text") {
                // Raw Extracted CV Document Display
                Text(
                    text = "CONTENU INTÉGRAL EXTRAIT DU DOCUMENT",
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
                            .background(
                                if (dark) Color(0x33000000) else Color(0x10000000),
                                RoundedRectangle(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = profile.rawCvContent.ifBlank { "Aucun contenu textuel brut extrait." },
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
                        Text(
                            text = profile.fullName.ifBlank { "Candidat" },
                            style = Type.titleLarge.bold,
                            color = Palette.text
                        )
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
                                text = listOfNotNull(profile.email.ifBlank { null }, profile.phone.ifBlank { null }).joinToString(" • "),
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
                                if (edu.year.isNotBlank()) {
                                    Text(
                                        text = edu.year,
                                        style = Type.label.monospaced,
                                        color = Palette.secondary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
}
