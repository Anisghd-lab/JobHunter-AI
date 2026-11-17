package com.ghdinteractivestudio.jobhunter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationAutomationManager
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSoftButton
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSolidButton
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PipelineScreen(
    allOffersFlow: StateFlow<List<JobOffer>>,
    isCheckingFollowUp: Boolean = false,
    onRunAiFollowUpClick: () -> Unit = {},
    onSendFollowUpClick: (JobOffer) -> Unit = {},
    onUpdateStatus: (JobOffer, JobStatus) -> Unit,
    onPrepareOrDispatchClick: (JobOffer) -> Unit,
    onRemoveFromPipeline: (JobOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    val allOffers by allOffersFlow.collectAsState()

    // Strictly track REAL analyzed, ready, applied, interview or rejected candidatures (NEVER raw radar scan or archived noise)
    val trackedApps = allOffers.filter { it.status != JobStatus.DETECTED && it.status != JobStatus.ARCHIVED }

    val totalApplied = trackedApps.count { it.status == JobStatus.APPLIED }
    val totalInterview = trackedApps.count { it.status == JobStatus.INTERVIEW }
    val totalPending = trackedApps.count { it.status == JobStatus.READY_TO_SEND || it.status == JobStatus.ANALYZED }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                title = "À Finaliser",
                count = totalPending,
                color = Palette.attention,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        GlassSoftButton(
            text = if (isCheckingFollowUp) "Analyse boîte & relances IA..." else "Suivi Réponses & Relances IA",
            icon = Icons.Default.Sync,
            accentColor = Palette.emeraldAccent,
            isLoading = isCheckingFollowUp,
            height = 40.dp,
            onClick = onRunAiFollowUpClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "SUIVI RÉEL DES CANDIDATURES (${trackedApps.size})",
            style = Type.mono.bold,
            color = Palette.secondary
        )

        Spacer(Modifier.height(10.dp))

        if (trackedApps.isEmpty()) {
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
                        text = "Aucune candidature active pour l'instant.",
                        style = Type.bodySmall.bold,
                        color = Palette.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Comparez une offre dans le Radar ou lancez « Postuler » pour voir vos dossiers suivis ici avec liens, CV, lettres et statuts réels.",
                        style = Type.label,
                        color = Palette.secondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trackedApps, key = { it.id }) { app ->
                    ApplicationTrackingCard(
                        app = app,
                        onUpdateStatus = { newStatus -> onUpdateStatus(app, newStatus) },
                        onDispatchClick = { onPrepareOrDispatchClick(app) },
                        onFollowUpClick = { onSendFollowUpClick(app) },
                        onRemoveClick = { onRemoveFromPipeline(app) }
                    )
                }
                item {
                    Spacer(Modifier.height(95.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplicationTrackingCard(
    app: JobOffer,
    onUpdateStatus: (JobStatus) -> Unit,
    onDispatchClick: () -> Unit,
    onFollowUpClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val context = LocalContext.current
    val dark = LocalDark.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val statusColor = when (app.status) {
        JobStatus.INTERVIEW -> Palette.emeraldAccent
        JobStatus.APPLIED -> Palette.electricBlue
        JobStatus.READY_TO_SEND -> Palette.attention
        JobStatus.ANALYZED -> Color(0xFF38BDF8)
        JobStatus.REJECTED -> Color(0xFFF87171)
        else -> Palette.secondary
    }

    val statusLabel = when (app.status) {
        JobStatus.ANALYZED -> "ANALYSÉE (${app.matchScore}%)"
        JobStatus.READY_TO_SEND -> "PRÊTE À ENVOYER"
        JobStatus.APPLIED -> "POSTULÉE ✓"
        JobStatus.INTERVIEW -> "ENTRETIEN 🎉"
        JobStatus.REJECTED -> "REFUSÉE"
        else -> app.status.name
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.company,
                        style = Type.titleMedium.bold,
                        color = Palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.title,
                        style = Type.bodySmall.semibold,
                        color = Palette.emeraldAccent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.16f), Capsule())
                            .border(0.5.dp, statusColor.copy(alpha = 0.4f), Capsule())
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = Type.label.bold.monospaced,
                            color = statusColor
                        )
                    }

                    IconButton(onClick = onRemoveClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Retirer du suivi",
                            tint = Palette.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "${app.location} • ${app.sourcePlatform}${if (app.appliedChannel.isNotBlank()) " • ${app.appliedChannel}" else ""}",
                style = Type.label,
                color = Palette.secondary
            )

            if (app.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    app.tags.take(4).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(if (dark) Color(0x22FFFFFF) else Color(0x12000000), Capsule())
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(text = "#$tag", style = Type.label, color = Palette.tertiary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Clickable Web URL
            if (app.url.startsWith("http")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url))) }
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Palette.electricBlue, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = app.url,
                        style = Type.label.monospaced,
                        color = Palette.electricBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Attachment & Timestamp Info Plate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (dark) Color(0x26000000) else Color(0x10000000),
                        RoundedRectangle(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = Palette.emeraldAccent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Dossier (${app.requiredFormat.name})",
                                style = Type.label.bold,
                                color = Palette.text
                            )
                        }

                        val ts = if (app.appliedAt > 0L) app.appliedAt else app.detectedAt
                        Text(
                            text = dateFormat.format(Date(ts)),
                            style = Type.label.monospaced,
                            color = Palette.tertiary
                        )
                    }

                    if (app.tailoredCvPath.isNotBlank() || app.letterPath.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (app.tailoredCvPath.isNotBlank() && File(app.tailoredCvPath).exists()) {
                                Row(
                                    modifier = Modifier
                                        .background(Palette.electricBlue.copy(alpha = 0.16f), Capsule())
                                        .clickable {
                                            ApplicationAutomationManager.buildViewOrShareFileIntent(context, app.tailoredCvPath)?.let {
                                                runCatching { context.startActivity(Intent.createChooser(it, "Ouvrir CV")) }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Palette.electricBlue, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (app.tailoredCvPath.contains("_Original_")) "CV Original" else "CV Modifié (${File(app.tailoredCvPath).extension.uppercase()})",
                                        style = Type.label.bold,
                                        color = Palette.electricBlue
                                    )
                                }
                            }
                            if (app.letterPath.isNotBlank() && File(app.letterPath).exists()) {
                                Row(
                                    modifier = Modifier
                                        .background(Palette.emeraldAccent.copy(alpha = 0.16f), Capsule())
                                        .clickable {
                                            ApplicationAutomationManager.buildViewOrShareFileIntent(context, app.letterPath)?.let {
                                                runCatching { context.startActivity(Intent.createChooser(it, "Ouvrir Lettre")) }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Palette.emeraldAccent, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Lettre ${File(app.letterPath).extension.uppercase()}", style = Type.label.bold, color = Palette.emeraldAccent)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Pipeline Reply & Deadline Summary + Uniform Action Buttons
            if (app.lastReplySummary.isNotBlank() || app.followUpCount > 0) {
                val followSuffix = if (app.followUpCount > 0) " (${app.followUpCount} relance(s))" else ""
                Text(
                    text = "🤖 Suivi IA$followSuffix : ${app.lastReplySummary.ifBlank { "En attente de retour" }}",
                    style = Type.label.bold,
                    color = Palette.electricBlue
                )
                Spacer(Modifier.height(6.dp))
            }

            val isExpired = DeadlineParser.isExpiredOrStale(app)
            if (app.deadlineAt > 0L) {
                Text(
                    text = if (isExpired) "⛔ Date limite dépassée (${app.deadlineLabel})" else "⏰ ${DeadlineParser.formatDaysRemaining(app.deadlineAt)}",
                    style = Type.label.bold,
                    color = if (isExpired) Palette.danger else Palette.attention
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (app.status == JobStatus.ANALYZED || app.status == JobStatus.READY_TO_SEND) {
                    GlassSolidButton(
                        text = when {
                            isExpired -> "Expirée"
                            app.status == JobStatus.ANALYZED -> "Générer"
                            else -> "Envoyer"
                        },
                        icon = Icons.AutoMirrored.Filled.Send,
                        containerColor = Palette.emeraldSolidBg,
                        enabled = !isExpired,
                        height = 40.dp,
                        onClick = onDispatchClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (app.status != JobStatus.APPLIED && app.status != JobStatus.INTERVIEW) {
                    GlassSoftButton(
                        text = "Postulé ✓",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Palette.electricBlue,
                        height = 40.dp,
                        onClick = { onUpdateStatus(JobStatus.APPLIED) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (app.status == JobStatus.APPLIED) {
                    if (app.recruiterEmail.isNotBlank() && !isExpired) {
                        GlassSoftButton(
                            text = "Relancer IA",
                            icon = Icons.AutoMirrored.Filled.Send,
                            accentColor = Palette.electricBlue,
                            height = 40.dp,
                            onClick = onFollowUpClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GlassSoftButton(
                        text = "Entretien",
                        icon = Icons.Default.Event,
                        accentColor = Palette.emeraldAccent,
                        height = 40.dp,
                        onClick = { onUpdateStatus(JobStatus.INTERVIEW) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
