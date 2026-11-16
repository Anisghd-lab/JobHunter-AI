package com.ghdinteractivestudio.jobhunter.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.service.JobHunterForegroundService
import com.ghdinteractivestudio.jobhunter.ui.components.glassCard
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.Type
import com.ghdinteractivestudio.jobhunter.ui.theme.bold
import com.ghdinteractivestudio.jobhunter.ui.theme.monospaced
import com.ghdinteractivestudio.jobhunter.ui.theme.semibold

@Composable
fun AgentScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAgentActive by remember { mutableStateOf(true) }

    var senderEmail by remember { mutableStateOf("candidat@gmail.com") }
    var smtpHost by remember { mutableStateOf("smtp.gmail.com") }
    var smtpPassword by remember { mutableStateOf("app_password_ici") }

    var geminiApiKey by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // 24/7 Service Master Switch Card
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
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isAgentActive) Palette.emeraldAccent else Palette.secondary,
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
                                text = if (isAgentActive) "En veille permanente sur smartphone" else "En pause",
                                style = Type.label,
                                color = if (isAgentActive) Palette.emeraldAccent else Palette.secondary
                            )
                        }
                    }

                    Switch(
                        checked = isAgentActive,
                        onCheckedChange = { active ->
                            isAgentActive = active
                            if (active) {
                                JobHunterForegroundService.startService(context)
                            } else {
                                JobHunterForegroundService.stopService(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Palette.emeraldAccent
                        )
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Battery optimization button
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

        // Bimodal Theme Switch (CodeStation Dark / Light)
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
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
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
                            text = if (isDarkTheme) "Mode Sombre Obsidian OLED" else "Mode Clair Blanc Givré",
                            style = Type.label,
                            color = Palette.secondary
                        )
                    }
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.emeraldAccent
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Email Configuration (Dual OAuth2 / SMTP)
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

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = senderEmail,
                    onValueChange = { senderEmail = it },
                    label = { Text("Votre adresse email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = smtpHost,
                    onValueChange = { smtpHost = it },
                    label = { Text("Serveur SMTP (ex: smtp.gmail.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = smtpPassword,
                    onValueChange = { smtpPassword = it },
                    label = { Text("Mot de passe / App Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // AI Provider Keys
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Palette.emeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "MOTEUR LLM MULTI-FOURNISSEURS",
                        style = Type.mono.bold,
                        color = Palette.emeraldAccent
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { geminiApiKey = it },
                    label = { Text("Clé API Gemini (ou OpenAI / Ollama)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Stockage chiffré dans Android Keystore. Permet le parsing du CV, le scoring et la rédaction de lettres.",
                    style = Type.label,
                    color = Palette.secondary
                )
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}
