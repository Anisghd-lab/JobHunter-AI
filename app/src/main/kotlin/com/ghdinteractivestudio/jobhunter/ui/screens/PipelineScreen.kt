package com.ghdinteractivestudio.jobhunter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
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

@Composable
fun PipelineScreen(
    appliedOffersFlow: StateFlow<List<JobOffer>>,
    modifier: Modifier = Modifier
) {
    val applications by appliedOffersFlow.collectAsState()
    val dark = LocalDark.current

    val totalApplied = applications.count { it.status == JobStatus.APPLIED }
    val totalInterview = applications.count { it.status == JobStatus.INTERVIEW }
    val totalReady = applications.count { it.status == JobStatus.READY_TO_SEND }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Metrics Banner (CodeStation Inspired)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Envoyées",
                count = totalApplied,
                color = Palette.electricBlue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Entretiens",
                count = totalInterview,
                color = Palette.emeraldAccent,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "À Valider",
                count = totalReady,
                color = Palette.attention,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "TABLEAU DE SUIVI DES CANDIDATURES",
            style = Type.mono.bold,
            color = Palette.secondary
        )

        Spacer(Modifier.height(10.dp))

        if (applications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Palette.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Aucune candidature envoyée pour l'instant.",
                        style = Type.bodySmall,
                        color = Palette.secondary
                    )
                    Text(
                        text = "L'agent autonome 24/7 postule dès qu'une opportunité correspond.",
                        style = Type.label,
                        color = Palette.tertiary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(applications, key = { it.id }) { app ->
                    ApplicationTrackingCard(app = app)
                }
                item {
                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCard()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = Type.titleLarge.bold.monospaced,
                color = color
            )
            Text(
                text = title,
                style = Type.label.semibold,
                color = Palette.secondary
            )
        }
    }
}

@Composable
fun ApplicationTrackingCard(app: JobOffer) {
    val dark = LocalDark.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val statusColor = when (app.status) {
        JobStatus.INTERVIEW -> Palette.emeraldAccent
        JobStatus.APPLIED -> Palette.electricBlue
        JobStatus.READY_TO_SEND -> Palette.attention
        else -> Palette.secondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.company,
                    style = Type.titleMedium.bold,
                    color = Palette.text
                )

                // Status chip
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.16f), Capsule())
                        .border(0.5.dp, statusColor.copy(alpha = 0.4f), Capsule())
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = app.status.name,
                        style = Type.label.bold.monospaced,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = app.title,
                style = Type.bodySmall,
                color = Palette.secondary
            )

            Spacer(Modifier.height(8.dp))

            // Attachment & Dispatch Info Plate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (dark) Color(0x26000000) else Color(0x10000000),
                        RoundedRectangle(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = Palette.emeraldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Pièces jointes : CV & Lettre (${app.requiredFormat.name})",
                            style = Type.label,
                            color = Palette.text
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(app.detectedAt)),
                        style = Type.label.monospaced,
                        color = Palette.tertiary
                    )
                }
            }
        }
    }
}
