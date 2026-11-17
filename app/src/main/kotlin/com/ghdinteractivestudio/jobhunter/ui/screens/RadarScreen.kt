package com.ghdinteractivestudio.jobhunter.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationAutomationManager
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationOutcome
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import com.ghdinteractivestudio.jobhunter.ui.components.GlassFilterChip
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSoftButton
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSolidButton
import com.ghdinteractivestudio.jobhunter.ui.components.GlassStyle
import com.ghdinteractivestudio.jobhunter.ui.components.glass
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadarScreen(
    offersFlow: StateFlow<List<JobOffer>>,
    isScanning: Boolean,
    busyOfferId: String?,
    activeOutcome: ApplicationOutcome?,
    onDismissOutcome: () -> Unit,
    onMarkAsApplied: (JobOffer) -> Unit,
    onRefreshScanClick: () -> Unit,
    onAnalyzeOfferClick: (JobOffer) -> Unit,
    onApplyClick: (JobOffer) -> Unit,
    onRegenerateOfferClick: (JobOffer) -> Unit = {},
    onToggleCvMode: (JobOffer, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val rawOffers by offersFlow.collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ALGERIA, INTERNATIONAL, REMOTE, ONSITE
    var searchQuery by remember { mutableStateOf("") }

    // Enforce strict deadline compliance: exclude expired / archived offers from Radar
    val offers = remember(rawOffers) {
        rawOffers.filter { it.status != JobStatus.ARCHIVED && !DeadlineParser.isExpiredOrStale(it) }
    }

    val filteredOffers = remember(offers, selectedFilter, searchQuery) {
        offers.filter { offer ->
            val matchesFilter = when (selectedFilter) {
                "ALGERIA" -> offer.regionScope == "ALGERIA"
                "GULF" -> offer.regionScope == "GULF"
                "INTERNATIONAL" -> offer.regionScope == "INTERNATIONAL"
                "REMOTE" -> offer.isRemote || offer.workMode == "REMOTE"
                "ONSITE" -> !offer.isRemote || offer.workMode == "ONSITE" || offer.workMode == "HYBRID"
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                offer.title.contains(searchQuery, ignoreCase = true) ||
                offer.company.contains(searchQuery, ignoreCase = true) ||
                offer.location.contains(searchQuery, ignoreCase = true) ||
                offer.tags.any { it.contains(searchQuery, ignoreCase = true) }

            matchesFilter && matchesQuery
        }
    }

    val dzCount = offers.count { it.regionScope == "ALGERIA" }
    val gulfCount = offers.count { it.regionScope == "GULF" }
    val intlCount = offers.count { it.regionScope == "INTERNATIONAL" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header Card
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Palette.emeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "RADAR WEB 100% RÉEL",
                            style = Type.mono.bold,
                            color = Palette.emeraldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    GlassSolidButton(
                        text = if (isScanning) "Scan..." else "Scanner",
                        icon = Icons.Default.Refresh,
                        containerColor = Palette.blueSolidBg,
                        isLoading = isScanning,
                        height = 36.dp,
                        onClick = onRefreshScanClick
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${offers.size} offres actives · 🇩🇿 Algérie: $dzCount · 🏜️ Golfe: $gulfCount · 🌍 Monde: $intlCount",
                    style = Type.label,
                    color = Palette.secondary
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtrer poste, compétence, Golfe, wilaya...", style = Type.label) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Palette.secondary, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = Capsule(),
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Filter Chips Bar (Tout, Algérie, Golfe GCC, Monde, Présentiel, Remote)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "ALL" to "Tout (${offers.size})",
                "ALGERIA" to "🇩🇿 Algérie ($dzCount)",
                "GULF" to "🏜️ Golfe ($gulfCount)",
                "INTERNATIONAL" to "🌍 Monde ($intlCount)",
                "ONSITE" to "🏢 Présentiel",
                "REMOTE" to "🏠 Remote"
            ).forEach { (key, label) ->
                GlassFilterChip(
                    text = label,
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredOffers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Palette.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isScanning) "Collecte des offres réelles sur le web..." else "Aucune offre trouvée pour ce filtre.",
                        style = Type.body.bold,
                        color = Palette.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Appuyez sur « Scanner » pour récolter en direct les offres Emploitic Algérie et internationales.",
                        style = Type.label,
                        color = Palette.secondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredOffers, key = { it.id }) { offer ->
                    JobOfferCard(
                        offer = offer,
                        isBusy = busyOfferId == offer.id,
                        onOpenUrlClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url)))
                            }
                        },
                        onAnalyzeClick = { onAnalyzeOfferClick(offer) },
                        onApplyClick = { onApplyClick(offer) }
                    )
                }
                item {
                    Spacer(Modifier.height(95.dp))
                }
            }
        }
    }

    // Dialog after clicking Postuler -> Handles Email App OR Website Portal with ready PDF/DOCX + Cover Letter
    if (activeOutcome != null) {
        ApplyAssistantDialog(
            outcome = activeOutcome,
            isRegenerating = busyOfferId == activeOutcome.updatedOffer.id,
            onRegenerate = { onRegenerateOfferClick(activeOutcome.updatedOffer) },
            onSwitchCvMode = { useTailored -> onToggleCvMode(activeOutcome.updatedOffer, useTailored) },
            onDismiss = onDismissOutcome,
            onMarkApplied = { onMarkAsApplied(activeOutcome.updatedOffer) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobOfferCard(
    offer: JobOffer,
    isBusy: Boolean,
    onOpenUrlClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    val dark = LocalDark.current
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Column {
            // Top badges row: Platform + WorkMode + Required Format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .glass(Capsule(), style = GlassStyle.Clear)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = offer.sourcePlatform,
                            style = Type.label.monospaced,
                            color = Palette.electricBlue
                        )
                    }

                    val regionColor = when (offer.regionScope) {
                        "ALGERIA" -> Palette.emeraldAccent
                        "GULF" -> Palette.cyanNeon
                        else -> Palette.attention
                    }
                    Box(
                        modifier = Modifier
                            .background(regionColor.copy(alpha = 0.14f), Capsule())
                            .border(0.5.dp, regionColor.copy(alpha = 0.4f), Capsule())
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (offer.regionScope) {
                                "ALGERIA" -> if (offer.isRemote) "🇩🇿 DZ • Remote" else "🇩🇿 DZ • Présentiel"
                                "GULF" -> if (offer.isRemote) "🏜️ Golfe • Remote" else "🏜️ Golfe • Présentiel"
                                else -> if (offer.isRemote) "🌍 Remote" else "🌍 Présentiel"
                            },
                            style = Type.label.bold,
                            color = regionColor
                        )
                    }
                }

                // Required Format Chip
                val formatLabel = when (offer.requiredFormat) {
                    DocumentFormatRequirement.PDF -> "PDF requis"
                    DocumentFormatRequirement.DOCX -> "Word .docx"
                    DocumentFormatRequirement.ANY -> "PDF / DOCX"
                }
                val formatColor = if (offer.requiredFormat == DocumentFormatRequirement.DOCX) Palette.electricBlue else Palette.emeraldAccent

                Box(
                    modifier = Modifier
                        .background(formatColor.copy(alpha = 0.15f), Capsule())
                        .border(0.5.dp, formatColor.copy(alpha = 0.4f), Capsule())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatLabel,
                        style = Type.label.bold,
                        color = formatColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title & Company & Location
            Text(
                text = offer.title,
                style = Type.titleMedium.bold,
                color = Palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = Palette.secondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = offer.company,
                    style = Type.bodySmall.semibold,
                    color = Palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Palette.tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = buildString {
                        append(offer.location)
                        if (offer.publishedDate.isNotBlank()) append(" • ${offer.publishedDate}")
                        if (offer.salary.isNotBlank()) append(" • ${offer.salary}")
                    },
                    style = Type.label,
                    color = Palette.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (offer.deadlineAt > 0L) {
                Spacer(Modifier.height(5.dp))
                val remLabel = DeadlineParser.formatDaysRemaining(offer.deadlineAt)
                val urgent = (offer.deadlineAt - System.currentTimeMillis()) <= 7L * 86_400_000L
                val dlColor = if (urgent) Palette.attention else Palette.emeraldAccent
                Row(
                    modifier = Modifier
                        .background(dlColor.copy(alpha = 0.14f), Capsule())
                        .border(0.5.dp, dlColor.copy(alpha = 0.4f), Capsule())
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = dlColor, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = remLabel, style = Type.label.bold, color = dlColor)
                }
            }

            // Real Web Tags Flow
            if (offer.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    offer.tags.take(6).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (dark) Color(0x22FFFFFF) else Color(0x14000000),
                                    Capsule()
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "#$tag", style = Type.label, color = Palette.secondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Clean Sanitized Description Plate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (dark) Color(0x26000000) else Color(0x10000000),
                        RoundedRectangle(10.dp)
                    )
                    .clickable { expanded = !expanded }
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = offer.description,
                        style = Type.bodySmall,
                        color = Palette.text,
                        maxLines = if (expanded) 40 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Réduire ▲" else "Lire l'offre complète ▼",
                        style = Type.label.bold,
                        color = Palette.electricBlue
                    )
                }
            }

            // AI Verdict / Matching Highlights Box (if analyzed against real CV)
            if (offer.matchScore > 0 || offer.aiReasoning.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                val verdictColor = when {
                    offer.matchScore >= 60 -> Palette.emeraldAccent
                    offer.matchScore >= 40 -> Palette.attention
                    else -> Color(0xFFF87171)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(verdictColor.copy(alpha = 0.12f), RoundedRectangle(10.dp))
                        .border(0.5.dp, verdictColor.copy(alpha = 0.35f), RoundedRectangle(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Compatibilité CV : ${offer.matchScore}% ${if (offer.aiVerdict.isNotBlank()) "(${offer.aiVerdict})" else ""}",
                                style = Type.label.bold.monospaced,
                                color = verdictColor
                            )
                            Text(
                                text = if (offer.recruiterEmail.isNotBlank()) "✉ ${offer.recruiterEmail}" else "🌐 Site Web",
                                style = Type.label.monospaced,
                                color = Palette.secondary
                            )
                        }
                        if (offer.matchingHighlights.isNotEmpty()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Points forts : ${offer.matchingHighlights.joinToString(", ")}",
                                style = Type.label,
                                color = Palette.text
                            )
                        }
                        if (offer.aiReasoning.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = offer.aiReasoning,
                                style = Type.label,
                                color = Palette.secondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Clickable Original Web Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenUrlClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = Palette.electricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = offer.url,
                    style = Type.label.monospaced,
                    color = Palette.electricBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            // Uniform Action Buttons Row (equal size, single-line, zero overflow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassSoftButton(
                    text = "Analyser IA",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = Palette.electricBlue,
                    enabled = !isBusy,
                    onClick = onAnalyzeClick,
                    modifier = Modifier.weight(1f)
                )

                GlassSolidButton(
                    text = if (offer.recruiterEmail.isNotBlank()) "Postuler Auto" else "Postuler",
                    icon = Icons.AutoMirrored.Filled.Send,
                    containerColor = Palette.emeraldSolidBg,
                    enabled = !isBusy,
                    isLoading = isBusy,
                    onClick = onApplyClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ApplyAssistantDialog(
    outcome: ApplicationOutcome,
    isRegenerating: Boolean = false,
    onRegenerate: () -> Unit = {},
    onSwitchCvMode: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onMarkApplied: () -> Unit
) {
    val context = LocalContext.current
    val offer = outcome.updatedOffer
    val dark = LocalDark.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (dark) Color(0xFF131823) else Color(0xFFF3F7FC),
        title = {
            Text(
                text = if (outcome.sentSilentlyByEmail) "Candidature Envoyée !" else "Dossier Prêt (${offer.requiredFormat.name})",
                style = Type.titleMedium.bold,
                color = Palette.emeraldAccent
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "${offer.title} — ${offer.company}",
                    style = Type.bodySmall.bold,
                    color = Palette.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = outcome.message,
                    style = Type.label,
                    color = Palette.secondary
                )

                Spacer(Modifier.height(10.dp))

                // Uniform File Buttons Row
                val usingOriginal = outcome.usesOriginalCv || offer.tailoredCvPath.contains("_Original_")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (offer.tailoredCvPath.isNotBlank()) {
                        GlassSoftButton(
                            text = if (usingOriginal) "CV Original" else "CV Modifié",
                            icon = Icons.Default.Description,
                            accentColor = Palette.electricBlue,
                            height = 40.dp,
                            onClick = {
                                val intent = ApplicationAutomationManager.buildViewOrShareFileIntent(context, offer.tailoredCvPath, share = false)
                                if (intent != null) runCatching { context.startActivity(Intent.createChooser(intent, "Ouvrir CV")) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (offer.letterPath.isNotBlank()) {
                        GlassSoftButton(
                            text = "Lettre ${File(offer.letterPath).extension.uppercase()}",
                            icon = Icons.Default.Description,
                            accentColor = Palette.emeraldAccent,
                            height = 40.dp,
                            onClick = {
                                val intent = ApplicationAutomationManager.buildViewOrShareFileIntent(context, offer.letterPath, share = false)
                                if (intent != null) runCatching { context.startActivity(Intent.createChooser(intent, "Ouvrir Lettre")) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!outcome.sentSilentlyByEmail) {
                    TextButton(
                        onClick = { onSwitchCvMode(usingOriginal) },
                        enabled = !isRegenerating
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (usingOriginal) "Adapter le CV (joindre version modifiée)" else "Joindre le CV Original (si sauvegardé)",
                            style = Type.label.bold,
                            color = Palette.electricBlue
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lettre Ciblée :", style = Type.label.bold, color = Palette.text)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!outcome.sentSilentlyByEmail) {
                            TextButton(
                                onClick = onRegenerate,
                                enabled = !isRegenerating
                            ) {
                                if (isRegenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(3.dp))
                                Text("Régénérer", style = Type.label.bold)
                            }
                        }
                        TextButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Lettre", offer.generatedLetter))
                                Toast.makeText(context, "Lettre copiée !", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copier", style = Type.label.bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (dark) Color(0x33000000) else Color(0x10000000), RoundedRectangle(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = offer.generatedLetter,
                        style = Type.label,
                        color = Palette.text,
                        maxLines = 12,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (offer.recruiterEmail.isNotBlank() && !outcome.sentSilentlyByEmail) {
                    val repo = remember { com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository.getInstance(context) }
                    val appSettings by repo.settings.collectAsState()
                    val yahooPkg = if (appSettings.preferInstalledYahooApp) appSettings.connectedYahooPackage else ""
                    GlassSolidButton(
                        text = if (yahooPkg.isNotBlank()) "Envoyer via Yahoo Mail" else "Envoyer Email (${offer.recruiterEmail})",
                        icon = Icons.Default.Email,
                        containerColor = Palette.blueSolidBg,
                        onClick = {
                            val directIntent = ApplicationAutomationManager.buildEmailComposeIntent(
                                context = context,
                                offer = offer,
                                preferredPackage = yahooPkg
                            )
                            val openedDirect = if (yahooPkg.isNotBlank()) {
                                runCatching { context.startActivity(directIntent) }.isSuccess
                            } else false
                            if (!openedDirect) {
                                val fallbackIntent = ApplicationAutomationManager.buildEmailComposeIntent(context, offer)
                                runCatching { context.startActivity(Intent.createChooser(fallbackIntent, "Envoyer par Email")) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (offer.url.startsWith("http")) {
                    GlassSolidButton(
                        text = "Ouvrir l'offre Web ↗",
                        icon = Icons.Default.OpenInBrowser,
                        containerColor = Palette.emeraldSolidBg,
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Lettre", offer.generatedLetter))
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url))) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!outcome.sentSilentlyByEmail) {
                GlassSolidButton(
                    text = "Postulé ✓",
                    icon = Icons.Default.CheckCircle,
                    containerColor = Palette.emeraldSolidBg,
                    height = 38.dp,
                    onClick = onMarkApplied
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", style = Type.label.bold, color = Palette.secondary)
            }
        }
    )
}
