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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CvScreen(
    profileFlow: StateFlow<UserProfile>,
    onImportCv: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by profileFlow.collectAsState()
    val dark = LocalDark.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportCv(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // Import Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Palette.emeraldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CV SOURCE (BASE DE RÉFÉRENCE)",
                        style = Type.mono.bold,
                        color = Palette.emeraldAccent
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Importez votre CV au format PDF ou Word (.docx). L'IA extrait vos compétences réelles pour servir de socle déterministe à toutes les candidatures.",
                    style = Type.bodySmall,
                    color = Palette.secondary
                )

                Spacer(Modifier.height(16.dp))

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
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Importer CV (PDF / Word .docx)",
                        style = Type.body.semibold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Profile Overview
        if (profile.fullName.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = profile.fullName,
                        style = Type.titleLarge.bold,
                        color = Palette.text
                    )
                    Text(
                        text = profile.title,
                        style = Type.body.semibold,
                        color = Palette.emeraldAccent
                    )
                    Text(
                        text = "${profile.email} • ${profile.phone}",
                        style = Type.label.monospaced,
                        color = Palette.secondary
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "COMPÉTENCES CLÉS DÉTECTÉES",
                        style = Type.label.bold,
                        color = Palette.tertiary
                    )

                    Spacer(Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (skill in profile.skills) {
                            Box(
                                modifier = Modifier
                                    .background(Palette.emeraldAccent.copy(alpha = 0.12f), Capsule())
                                    .border(0.5.dp, Palette.emeraldAccent.copy(alpha = 0.3f), Capsule())
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = skill,
                                    style = Type.label.semibold,
                                    color = Palette.emeraldAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Experiences
        if (profile.experiences.isNotEmpty()) {
            Text(
                text = "EXPÉRIENCES ANALYSÉES",
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
                        Text(
                            text = "${exp.company} • ${exp.period}",
                            style = Type.label.monospaced,
                            color = Palette.secondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = exp.description,
                            style = Type.bodySmall,
                            color = Palette.secondary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}
