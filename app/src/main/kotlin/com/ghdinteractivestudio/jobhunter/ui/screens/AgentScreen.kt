package com.ghdinteractivestudio.jobhunter.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.ghdinteractivestudio.jobhunter.service.JobHunterForegroundService
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    settingsRepo: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsRepo.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val dark = LocalDark.current

    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 70, 80).forEach { threshold ->
                        val active = settings.autoApplyMinScore == threshold
                        Box(
                            modifier = Modifier
                                .background(if (active) Palette.electricBlue else Color.Transparent, Capsule())
                                .border(0.75.dp, if (active) Palette.electricBlue else Palette.tertiary.copy(alpha = 0.4f), Capsule())
                                .clickable { settingsRepo.update { it.copy(autoApplyMinScore = threshold) } }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Seuil $threshold%",
                                style = Type.label.bold,
                                color = if (active) Color.White else Palette.secondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    },
                    shape = Capsule(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.electricBlue.copy(alpha = 0.15f),
                        contentColor = Palette.electricBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Exempter l'app d'optimisation batterie (24/7)", style = Type.bodySmall.semibold)
                }
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
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Palette.emeraldAccent else Color.Transparent,
                                    Capsule()
                                )
                                .glass(Capsule(), style = GlassStyle.Clear)
                                .border(
                                    0.75.dp,
                                    if (isSelected) Palette.emeraldAccent else if (hasKey) Palette.emeraldAccent.copy(alpha = 0.5f) else Palette.tertiary.copy(alpha = 0.35f),
                                    Capsule()
                                )
                                .clickable {
                                    val defaultModel = FreeLlmCatalog.modelsFor(provider).firstOrNull()?.id ?: ""
                                    settingsRepo.update {
                                        it.copy(activeProvider = provider, activeModel = defaultModel)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (hasKey) "✓ ${provider.displayName}" else provider.displayName,
                                style = Type.label.bold,
                                color = if (isSelected) Color.White else Palette.text
                            )
                        }
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
                        val selected = settings.activeModel == opt.id
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) Palette.electricBlue.copy(alpha = 0.25f) else Color.Transparent,
                                    Capsule()
                                )
                                .border(
                                    0.75.dp,
                                    if (selected) Palette.electricBlue else Palette.tertiary.copy(alpha = 0.3f),
                                    Capsule()
                                )
                                .clickable { settingsRepo.update { it.copy(activeModel = opt.id) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = opt.label,
                                style = Type.label.bold,
                                color = if (selected) Palette.electricBlue else Palette.secondary
                            )
                        }
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Live AI Test Button
                Button(
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
                    enabled = !isTestingAi,
                    shape = Capsule(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.emeraldAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingAi) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Test du modèle gratuit...", style = Type.label.bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tester la connexion IA Gratuite", style = Type.label.bold)
                    }
                }

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
                            color = if (testResultMsg!!.startsWith("✓")) Palette.emeraldAccent else Color(0xFFF87171)
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
                        checkedTrackColor = Palette.emeraldAccent
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

                // Quick SMTP Presets
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Yahoo", "smtp.mail.yahoo.com", 465),
                        Triple("Gmail", "smtp.gmail.com", 465),
                        Triple("Outlook", "smtp-mail.outlook.com", 587)
                    ).forEach { (label, host, port) ->
                        val active = settings.smtpHost == host
                        Box(
                            modifier = Modifier
                                .background(if (active) Palette.electricBlue.copy(alpha = 0.2f) else Color.Transparent, Capsule())
                                .border(0.75.dp, if (active) Palette.electricBlue else Palette.tertiary.copy(alpha = 0.35f), Capsule())
                                .clickable { settingsRepo.update { it.copy(smtpHost = host, smtpPort = port) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = label, style = Type.label.bold, color = if (active) Palette.electricBlue else Palette.secondary)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = settings.senderEmail,
                    onValueChange = { v -> settingsRepo.update { it.copy(senderEmail = v) } },
                    label = { Text("Votre adresse email (ex: nom@yahoo.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = settings.smtpHost,
                    onValueChange = { v -> settingsRepo.update { it.copy(smtpHost = v) } },
                    label = { Text("Serveur SMTP (port ${settings.smtpPort})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = settings.smtpPassword,
                    onValueChange = { v -> settingsRepo.update { it.copy(smtpPassword = v) } },
                    label = { Text("Mot de passe d'application (App Password)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (settings.hasEmailCredentials()) {
                        "✓ Envoi automatique activé en arrière-plan."
                    } else {
                        "Optionnel : sans mot de passe d'application, l'app prépare votre Email avec pièces jointes et ouvre Yahoo Mail / Gmail prêt à envoyer."
                    },
                    style = Type.label,
                    color = if (settings.hasEmailCredentials()) Palette.emeraldAccent else Palette.secondary
                )
            }
        }

        Spacer(Modifier.height(95.dp))
    }
}
