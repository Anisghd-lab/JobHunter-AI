package com.ghdinteractivestudio.jobhunter.ui.screens

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.data.local.FreeLlmCatalog
import com.ghdinteractivestudio.jobhunter.data.local.FreeLlmProvider
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository
import com.ghdinteractivestudio.jobhunter.domain.ai.LlmClient
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationAutomationManager
import com.ghdinteractivestudio.jobhunter.domain.automation.JobPortalAccount
import com.ghdinteractivestudio.jobhunter.domain.automation.PortalSessionRegistry
import com.ghdinteractivestudio.jobhunter.domain.email.EmailDispatcher
import com.ghdinteractivestudio.jobhunter.service.JobHunterForegroundService
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    settingsRepo: SettingsRepository,
    todayAutoCount: Int = 0,
    isRunningAutomation: Boolean = false,
    onRunAutomationNowClick: () -> Unit = {},
    onOpenPortalSetupClick: (JobPortalAccount) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsRepo.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val dark = LocalDark.current

    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }
    var emailTestMsg by remember { mutableStateOf<String?>(null) }
    var isTestingEmail by remember { mutableStateOf(false) }
    var portalTestMsg by remember { mutableStateOf<String?>(null) }
    var testingPortalId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // 1. 24/7 Service Master Switch + Automation Card
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (settings.agentActive247) Palette.emeraldAccent else Palette.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AGENT AUTONOME 24/7",
                                style = Type.body.bold.monospaced,
                                color = Palette.text
                            )
                            Text(
                                text = if (settings.agentActive247) "Veille web Algérie & Monde active" else "En pause",
                                style = Type.label,
                                color = if (settings.agentActive247) Palette.emeraldAccent else Palette.secondary
                            )
                        }
                    }

                    Switch(
                        checked = settings.agentActive247,
                        onCheckedChange = { active ->
                            settingsRepo.update { it.copy(agentActive247 = active) }
                            if (active) JobHunterForegroundService.startService(context)
                            else JobHunterForegroundService.stopService(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.emeraldAccent
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Autonomous CV Matching & Auto-Apply Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Candidature Automatique si Match >= ${settings.autoApplyMinScore}%",
                            style = Type.bodySmall.bold,
                            color = Palette.text
                        )
                        Text(
                            text = "Compare avec le CV, rédige lettre + CV (PDF/DOCX) et envoie/prépare.",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }

                    Switch(
                        checked = settings.autoApplyEnabled,
                        onCheckedChange = { auto ->
                            settingsRepo.update { it.copy(autoApplyEnabled = auto) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.electricBlue
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(45, 50, 60, 70).forEach { threshold ->
                        GlassFilterChip(
                            text = "$threshold%",
                            selected = settings.autoApplyMinScore == threshold,
                            selectedColor = Palette.blueSolidBg,
                            onClick = { settingsRepo.update { it.copy(autoApplyMinScore = threshold) } }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email Direct Uniquement (100% Auto)",
                            style = Type.bodySmall.bold,
                            color = Palette.text
                        )
                        Text(
                            text = "Actif = 100% auto par Email uniquement. Désactivé (recommandé) = envoie auto par Email + prépare les Portails Web (>= seuil) pour l'Assistant Inscription / CAPTCHA.",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }
                    Switch(
                        checked = settings.autoApplyEmailOnly,
                        onCheckedChange = { emailOnly ->
                            settingsRepo.update { it.copy(autoApplyEmailOnly = emailOnly) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.emeraldSolidBg
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 50).forEach { maxPerDay ->
                        GlassFilterChip(
                            text = "Max $maxPerDay/j",
                            selected = settings.autoApplyMaxPerDay == maxPerDay,
                            selectedColor = Palette.emeraldSolidBg,
                            onClick = {
                                val cycleCap = if (maxPerDay >= 25) 12 else 5
                                settingsRepo.update {
                                    it.copy(autoApplyMaxPerDay = maxPerDay, autoApplyMaxPerCycle = cycleCap)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.plateBackground, RoundedRectangle(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Aujourd'hui : $todayAutoCount / ${settings.autoApplyMaxPerDay} candidature(s) auto envoyée(s)",
                            style = Type.label.bold.monospaced,
                            color = Palette.emeraldAccent
                        )
                        if (settings.lastAutomationSummary.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = settings.lastAutomationSummary,
                                style = Type.label,
                                color = Palette.secondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                GlassSolidButton(
                    text = if (isRunningAutomation) "Automatisation & IMAP en cours..." else "Lancer Auto-Candidature & Suivi",
                    icon = Icons.Default.AutoAwesome,
                    containerColor = Palette.emeraldSolidBg,
                    isLoading = isRunningAutomation,
                    onClick = onRunAutomationNowClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                // Humanize Style Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Humanize IA (Écriture 100% Humaine)",
                            style = Type.bodySmall.bold,
                            color = Palette.text
                        )
                        Text(
                            text = "Élimine les tics LLM et clichés robotiques dans toutes vos lettres et emails de relance.",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }
                    Switch(
                        checked = settings.humanizeWritingEnabled,
                        onCheckedChange = { hum ->
                            settingsRepo.update { it.copy(humanizeWritingEnabled = hum) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.emeraldSolidBg
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Original vs Modified CV Policy Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adapter le CV automatiquement par offre",
                            style = Type.bodySmall.bold,
                            color = Palette.text
                        )
                        Text(
                            text = "Désactivé (recommandé) = joint le fichier CV original intact, sauf si vous modifiez votre CV ou si un format différent est exigé.",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }

                    Switch(
                        checked = settings.adaptCvPerOffer,
                        onCheckedChange = { adapt ->
                            settingsRepo.update { it.copy(adaptCvPerOffer = adapt) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.emeraldAccent
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                GlassSoftButton(
                    text = "Exempter batterie (24/7)",
                    icon = Icons.Default.BatteryAlert,
                    accentColor = Palette.electricBlue,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Optimisation batterie déjà désactivée pour JobHunter AI ✓",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. LLM Multi-Providers (100% Free Models ONLY)
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
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Palette.emeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "IA GRATUITE (MODÈLES FREE UNIQUEMENT)",
                            style = Type.mono.bold,
                            color = Palette.emeraldAccent
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Utilise exclusivement les modèles gratuits avec bascule automatique entre fournisseurs en cas de saturation.",
                    style = Type.label,
                    color = Palette.secondary
                )

                Spacer(Modifier.height(10.dp))

                // Provider Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FreeLlmProvider.entries.forEach { provider ->
                        val isSelected = settings.activeProvider == provider
                        val hasKey = settings.apiKeyFor(provider).isNotBlank()
                        GlassFilterChip(
                            text = if (hasKey) "✓ ${provider.displayName}" else provider.displayName,
                            selected = isSelected,
                            onClick = {
                                val defaultModel = FreeLlmCatalog.modelsFor(provider).firstOrNull()?.id ?: ""
                                settingsRepo.update {
                                    it.copy(activeProvider = provider, activeModel = defaultModel)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Modèles Gratuits pour ${settings.activeProvider.displayName} :",
                    style = Type.label.bold,
                    color = Palette.secondary
                )
                Spacer(Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FreeLlmCatalog.modelsFor(settings.activeProvider).forEach { opt ->
                        GlassFilterChip(
                            text = opt.label,
                            selected = settings.activeModel == opt.id,
                            selectedColor = Palette.blueSolidBg,
                            onClick = { settingsRepo.update { it.copy(activeModel = opt.id) } }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Active provider key input
                val currentKey = settings.apiKeyFor(settings.activeProvider)
                if (settings.activeProvider == FreeLlmProvider.OLLAMA_LOCAL) {
                    OutlinedTextField(
                        value = settings.ollamaUrl,
                        onValueChange = { v -> settingsRepo.update { it.copy(ollamaUrl = v) } },
                        label = { Text("URL Ollama Locale") },
                        singleLine = true,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = currentKey,
                        onValueChange = { v ->
                            settingsRepo.update { s ->
                                when (settings.activeProvider) {
                                    FreeLlmProvider.OPENROUTER_FREE -> s.copy(openrouterApiKey = v)
                                    FreeLlmProvider.XKIRO_FREE -> s.copy(xkiroApiKey = v)
                                    FreeLlmProvider.BYTEZ_FREE -> s.copy(bytezApiKey = v)
                                    FreeLlmProvider.GEMINI_FREE -> s.copy(geminiApiKey = v)
                                    FreeLlmProvider.OLLAMA_LOCAL -> s
                                }
                            }
                        },
                        label = {
                            Text(
                                if (currentKey.isNotBlank()) {
                                    "Clé API ${settings.activeProvider.displayName} (Configurée ✓)"
                                } else {
                                    "Coller clé API ${settings.activeProvider.displayName}"
                                }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Live AI Test Button
                GlassSolidButton(
                    text = if (isTestingAi) "Test IA en cours..." else "Tester l'IA Gratuite",
                    icon = Icons.Default.AutoAwesome,
                    containerColor = Palette.emeraldSolidBg,
                    isLoading = isTestingAi,
                    onClick = {
                        isTestingAi = true
                        testResultMsg = null
                        coroutineScope.launch {
                            val res = LlmClient.generateWithFreeFallback(
                                settings = settings,
                                prompt = "Réponds uniquement par : OK + ton nom de modèle."
                            )
                            testResultMsg = res.fold(
                                onSuccess = { "✓ Connecté (${it.providerUsed} · ${it.modelUsed}) : ${it.text.take(80)}" },
                                onFailure = { "✗ Erreur : ${it.message}" }
                            )
                            isTestingAi = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (testResultMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (dark) Color(0x33000000) else Color(0x10000000), RoundedRectangle(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = testResultMsg ?: "",
                            style = Type.label.monospaced,
                            color = if (testResultMsg!!.startsWith("✓")) Palette.emeraldAccent else Palette.danger
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Bimodal Theme Switch (CodeStation Dark / Light)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (settings.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = Palette.text,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "THÈME LIQUID GLASS",
                            style = Type.body.bold.monospaced,
                            color = Palette.text
                        )
                        Text(
                            text = if (settings.isDarkTheme) "Mode Sombre Obsidian OLED" else "Mode Clair Blanc Givré",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }
                }

                Switch(
                    checked = settings.isDarkTheme,
                    onCheckedChange = { darkMode -> settingsRepo.update { it.copy(isDarkTheme = darkMode) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.emeraldSolidBg
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4. Email Configuration (Persisted, Real SMTP / OAuth2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Palette.electricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CONFIGURATION EMAIL D'ENVOI",
                        style = Type.mono.bold,
                        color = Palette.electricBlue
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Detect installed email applications on device (Gmail, Yahoo, Outlook)
                val detectedMailApps = remember(context) {
                    val pm = context.packageManager
                    val candidates = listOf(
                        Triple("Gmail", "com.google.android.gm", "https://myaccount.google.com/apppasswords"),
                        Triple("Yahoo Mail", "com.yahoo.mobile.client.android.mail", "https://login.yahoo.com/myaccount/security"),
                        Triple("Outlook", "com.microsoft.office.outlook", "https://account.live.com/proofs/manage/additional")
                    )
                    candidates.filter { (_, pkg, _) ->
                        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
                    }
                }

                if (detectedMailApps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.plateBackground, RoundedRectangle(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✓ Appli(s) Email détectée(s) : ${detectedMailApps.joinToString { it.first }}",
                            style = Type.label.bold,
                            color = Palette.emeraldAccent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Quick SMTP Presets
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Gmail", "smtp.gmail.com", 465),
                        Triple("Yahoo", "smtp.mail.yahoo.com", 465),
                        Triple("Outlook", "smtp-mail.outlook.com", 587)
                    ).forEach { (label, host, port) ->
                        val isSelected = settings.smtpHost == host
                        GlassFilterChip(
                            text = label,
                            selected = isSelected,
                            selectedColor = Palette.blueSolidBg,
                            onClick = {
                                val matchApp = detectedMailApps.firstOrNull { it.first.contains(label, ignoreCase = true) }
                                settingsRepo.update {
                                    it.copy(
                                        smtpHost = host,
                                        smtpPort = port,
                                        connectedMailAppPackage = matchApp?.second ?: it.connectedMailAppPackage,
                                        connectedMailAppName = matchApp?.first ?: label,
                                        preferInstalledMailApp = matchApp != null,
                                        preferInstalledYahooApp = matchApp?.second?.contains("yahoo") == true,
                                        connectedYahooPackage = if (matchApp?.second?.contains("yahoo") == true) matchApp.second else it.connectedYahooPackage
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = settings.senderEmail,
                    onValueChange = { v ->
                        val lower = v.lowercase()
                        val detectedPreset = when {
                            lower.contains("@gmail.") -> Triple("smtp.gmail.com", 465, "Gmail")
                            lower.contains("@yahoo.") -> Triple("smtp.mail.yahoo.com", 465, "Yahoo Mail")
                            lower.contains("@outlook.") || lower.contains("@hotmail.") -> Triple("smtp-mail.outlook.com", 587, "Outlook")
                            else -> null
                        }
                        val matchingApp = detectedPreset?.let { preset ->
                            detectedMailApps.firstOrNull { it.first.contains(preset.third.take(5), ignoreCase = true) }
                        }
                        settingsRepo.update { s ->
                            s.copy(
                                senderEmail = v,
                                smtpHost = detectedPreset?.first ?: s.smtpHost,
                                smtpPort = detectedPreset?.second ?: s.smtpPort,
                                connectedMailAppPackage = matchingApp?.second ?: s.connectedMailAppPackage,
                                connectedMailAppName = matchingApp?.first ?: s.connectedMailAppName,
                                preferInstalledMailApp = matchingApp != null || s.preferInstalledMailApp
                            )
                        }
                    },
                    label = { Text("Votre adresse email (ex: nom@gmail.com)") },
                    singleLine = true,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = settings.smtpHost,
                    onValueChange = { v -> settingsRepo.update { it.copy(smtpHost = v) } },
                    label = { Text("Serveur SMTP (auto 465/587)") },
                    singleLine = true,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = settings.smtpPassword,
                    onValueChange = { v ->
                        settingsRepo.update { it.copy(smtpPassword = v, smtpAuthPaused = false) }
                    },
                    label = { Text("Mot de passe d'application (16 car. ${settings.getProviderDisplayName()})") },
                    trailingIcon = if (settings.smtpPassword.isNotBlank()) {
                        {
                            IconButton(onClick = {
                                settingsRepo.update { it.copy(smtpPassword = "", smtpAuthPaused = false) }
                                emailTestMsg = "Mot de passe effacé — mode Appli ${settings.effectiveMailAppName()} prêt."
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = Palette.secondary)
                            }
                        }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Total AI Control switches (Auto Send + IMAP Reply & Follow-up)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contrôle IA Total (Envoi + IMAP)",
                            style = Type.bodySmall.bold,
                            color = Palette.text
                        )
                        Text(
                            text = "L'IA envoie directement via SMTP et lit les réponses recruteurs (IMAP 993).",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }
                    Switch(
                        checked = settings.aiAutonomousEmailControl,
                        onCheckedChange = { enabled ->
                            settingsRepo.update { it.copy(aiAutonomousEmailControl = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.emeraldSolidBg
                        )
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Relance Automatique IA (J+${settings.followUpDelayDays})",
                            style = Type.bodySmall.bold,
                            color = Palette.text
                        )
                        Text(
                            text = "Relance polie automatique après ${settings.followUpDelayDays} jours sans réponse.",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }
                    Switch(
                        checked = settings.autoFollowUpEnabled,
                        onCheckedChange = { enabled ->
                            settingsRepo.update { it.copy(autoFollowUpEnabled = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.blueSolidBg
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Live Email Test Button (tests SMTP when App Password entered, or installed Email App binding)
                GlassSolidButton(
                    text = if (isTestingEmail) "Test connexion Email..." else "Tester Connexion Email (${settings.getProviderDisplayName()})",
                    icon = Icons.Default.CheckCircle,
                    containerColor = Palette.blueSolidBg,
                    isLoading = isTestingEmail,
                    onClick = {
                        val hasInstalledApp = detectedMailApps.any { it.second == settings.effectiveMailPackage() } || detectedMailApps.isNotEmpty()
                        if (settings.smtpPassword.isBlank()) {
                            if (hasInstalledApp) {
                                emailTestMsg = "✓ Mode ${settings.effectiveMailAppName()} actif (${settings.senderEmail}). Candidature en 1 clic pré-remplie avec CV & Lettre (ajoutez un Mot de passe d'application 16 car. ${settings.getProviderDisplayName()} pour l'envoi 100% silencieux sans confirmation)."
                            } else {
                                emailTestMsg = "✗ Saisissez un Mot de passe d'application ${settings.getProviderDisplayName()} 16 lettres OU appuyez sur « Lier Appli »."
                            }
                            return@GlassSolidButton
                        }
                        isTestingEmail = true
                        emailTestMsg = null
                        settingsRepo.update { it.copy(smtpAuthPaused = false) }
                        coroutineScope.launch {
                            val disp = EmailDispatcher(ApplicationAutomationManager.buildEmailConfig(settings))
                            val smtpRes = disp.testSmtpConnection()
                            val imapRes = if (smtpRes.isSuccess) {
                                disp.fetchRecentInboxReplies(maxMessages = 3)
                            } else null
                            emailTestMsg = smtpRes.fold(
                                onSuccess = { msg ->
                                    settingsRepo.update { it.copy(smtpAuthPaused = false) }
                                    val imapInfo = imapRes?.fold(
                                        onSuccess = { " · IMAP OK (${it.size} réc.)" },
                                        onFailure = { " · IMAP: ${it.message?.take(35)}" }
                                    ) ?: ""
                                    "$msg$imapInfo"
                                },
                                onFailure = { err ->
                                    val msg = err.message.orEmpty()
                                    if (msg.contains("535") || msg.contains("refusés") || msg.contains("AUTH005")) {
                                        settingsRepo.update { it.copy(smtpAuthPaused = true) }
                                    }
                                    "✗ $msg"
                                }
                            )
                            isTestingEmail = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                val emailAccountPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val pickedName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim().orEmpty()
                    if (pickedName.isNotBlank()) {
                        val matched = detectedMailApps.firstOrNull { pickedName.lowercase().contains(it.first.take(4).lowercase()) }
                            ?: detectedMailApps.firstOrNull()
                        settingsRepo.update {
                            it.copy(
                                senderEmail = pickedName,
                                preferInstalledMailApp = true,
                                connectedMailAppPackage = matched?.second ?: it.connectedMailAppPackage,
                                connectedMailAppName = matched?.first ?: it.connectedMailAppName,
                                preferInstalledYahooApp = matched?.second?.contains("yahoo") == true,
                                connectedYahooPackage = if (matched?.second?.contains("yahoo") == true) matched.second else ""
                            )
                        }
                        emailTestMsg = "✓ Compte '$pickedName' lié avec ${settings.effectiveMailAppName()} !"
                    } else if (detectedMailApps.isNotEmpty()) {
                        val firstApp = detectedMailApps.first()
                        settingsRepo.update {
                            it.copy(
                                preferInstalledMailApp = true,
                                connectedMailAppPackage = firstApp.second,
                                connectedMailAppName = firstApp.first,
                                preferInstalledYahooApp = firstApp.second.contains("yahoo"),
                                connectedYahooPackage = if (firstApp.second.contains("yahoo")) firstApp.second else ""
                            )
                        }
                        emailTestMsg = "✓ Application ${firstApp.first} liée pour les envois directs en 1 clic."
                    }
                }

                // Dual Uniform Buttons: Connect Installed Email App OR Generate App Password (Provider-specific)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLinked = settings.isMailAppPreferred() && settings.effectiveMailPackage().isNotBlank()
                    GlassSoftButton(
                        text = if (isLinked) "✓ ${settings.effectiveMailAppName()} Liée" else "Lier ${settings.effectiveMailAppName()}",
                        icon = Icons.Default.Link,
                        accentColor = Palette.emeraldAccent,
                        height = 40.dp,
                        onClick = {
                            val targetApp = detectedMailApps.firstOrNull { it.first.equals(settings.getProviderDisplayName(), ignoreCase = true) }
                                ?: detectedMailApps.firstOrNull()
                            if (targetApp != null) {
                                settingsRepo.update {
                                    it.copy(
                                        preferInstalledMailApp = true,
                                        connectedMailAppPackage = targetApp.second,
                                        connectedMailAppName = targetApp.first,
                                        preferInstalledYahooApp = targetApp.second.contains("yahoo"),
                                        connectedYahooPackage = targetApp.second
                                    )
                                }
                                emailTestMsg = "✓ Application ${targetApp.first} liée avec succès pour les envois directs en 1 clic !"
                            } else {
                                runCatching {
                                    val pickerIntent = AccountManager.newChooseAccountIntent(
                                        null, null, null, null, null, null, null
                                    )
                                    emailAccountPicker.launch(pickerIntent)
                                }.onFailure {
                                    emailTestMsg = "✓ Envoi direct configuré via le sélecteur d'applications email."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    GlassSoftButton(
                        text = "App Password ↗",
                        icon = Icons.Default.OpenInBrowser,
                        accentColor = Palette.electricBlue,
                        height = 40.dp,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(settings.getAppPasswordHelpUrl()))
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (emailTestMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.plateBackground, RoundedRectangle(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = emailTestMsg ?: "",
                            style = Type.label.monospaced,
                            color = if (emailTestMsg!!.contains("✓")) Palette.emeraldAccent else Palette.danger
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                val isMailLinked = settings.isMailAppPreferred() && settings.effectiveMailPackage().isNotBlank()
                Text(
                    text = when {
                        settings.canSendSilently() -> "✓ Envoi silencieux SMTP & suivi IMAP actifs."
                        settings.smtpAuthPaused -> "⚠️ SMTP mis en pause suite à une erreur 535 (utilisez un App Password 16 car. ${settings.getProviderDisplayName()}, ou envoyez via ${settings.effectiveMailAppName()})."
                        isMailLinked -> "✓ Mode Appli ${settings.effectiveMailAppName()} lié : chaque dossier préparé s'ouvre en 1 clic dans ${settings.effectiveMailAppName()}."
                        else -> "Astuce ${settings.getProviderDisplayName()} : sans Mot de passe d'application (16 car.), appuyez sur « Lier ${settings.effectiveMailAppName()} » pour envoyer via l'application sans erreur 535."
                    },
                    style = Type.label,
                    color = when {
                        settings.canSendSilently() || isMailLinked -> Palette.emeraldAccent
                        settings.smtpAuthPaused -> Palette.attention
                        else -> Palette.secondary
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 5. Web Portals Accounts & Session Connection Tests (Human Login & CAPTCHA Safe)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Palette.emeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "COMPTES PORTAILS WEB (CONNEXION & TEST)",
                        style = Type.mono.bold,
                        color = Palette.emeraldAccent
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Créez votre compte et passez le CAPTCHA dans le navigateur intégré : votre session reste connectée pour postuler sans friction. (Careem/Tamara Greenhouse & LeanTech Ashby ne demandent aucun compte préalable.)",
                    style = Type.label,
                    color = Palette.secondary
                )

                Spacer(Modifier.height(12.dp))

                PortalSessionRegistry.portals.forEach { portal ->
                    val isConnected = settings.isPortalConnected(portal.id)
                    val isTestingThis = testingPortalId == portal.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Palette.plateBackground, RoundedRectangle(10.dp))
                            .border(
                                0.5.dp,
                                if (isConnected) Palette.emeraldAccent.copy(alpha = 0.45f) else Palette.attention.copy(alpha = 0.35f),
                                RoundedRectangle(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${portal.badge} · ${portal.name}",
                                        style = Type.bodySmall.bold,
                                        color = Palette.text
                                    )
                                    Text(
                                        text = portal.description,
                                        style = Type.label,
                                        color = Palette.secondary
                                    )
                                }
                                Text(
                                    text = if (isConnected) "✓ Connecté" else "À connecter",
                                    style = Type.label.bold.monospaced,
                                    color = if (isConnected) Palette.emeraldAccent else Palette.attention
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassSolidButton(
                                    text = if (isConnected) "Ouvrir Session" else "Créer / Connecter",
                                    icon = Icons.Default.OpenInBrowser,
                                    containerColor = Palette.emeraldSolidBg,
                                    height = 38.dp,
                                    onClick = { onOpenPortalSetupClick(portal) },
                                    modifier = Modifier.weight(1.2f)
                                )
                                GlassSoftButton(
                                    text = if (isTestingThis) "Test..." else "Tester",
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = Palette.electricBlue,
                                    isLoading = isTestingThis,
                                    height = 38.dp,
                                    onClick = {
                                        if (testingPortalId != null) return@GlassSoftButton
                                        testingPortalId = portal.id
                                        portalTestMsg = null
                                        coroutineScope.launch {
                                            val res = PortalSessionRegistry.testPortalConnection(portal)
                                            settingsRepo.update { it.withPortalStatus(portal.id, res.connected) }
                                            portalTestMsg = res.message
                                            testingPortalId = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (portalTestMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.plateBackground, RoundedRectangle(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = portalTestMsg ?: "",
                            style = Type.label.monospaced,
                            color = if (portalTestMsg!!.startsWith("✓")) Palette.emeraldAccent else Palette.danger
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(95.dp))
    }
}
