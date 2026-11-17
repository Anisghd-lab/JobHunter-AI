package com.ghdinteractivestudio.jobhunter.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationAutomationManager
import com.ghdinteractivestudio.jobhunter.domain.automation.PortalSessionRegistry
import com.ghdinteractivestudio.jobhunter.domain.document.MldpCoverLetterData
import com.ghdinteractivestudio.jobhunter.domain.email.EmailDispatcher
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import com.ghdinteractivestudio.jobhunter.ui.components.GlassFilterChip
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSoftButton
import com.ghdinteractivestudio.jobhunter.ui.components.GlassSolidButton
import com.ghdinteractivestudio.jobhunter.ui.components.LiquidGlassBackdrop
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.jobhunter.ui.theme.Type
import com.ghdinteractivestudio.jobhunter.ui.theme.bold
import com.ghdinteractivestudio.jobhunter.ui.theme.monospaced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

private data class DetectedOtpOrLink(
    val sourceSubject: String = "",
    val code: String = "",
    val verificationUrl: String = "",
    val statusNote: String = ""
)

/**
 * Visible interactive in-app WebView Copilot for external job portals:
 * - Preserves login/session cookies across sessions once YOU log in / create an account & solve any CAPTCHA visibly.
 * - Assists with 1-tap copy, Download-folder export for easy file upload, visible input pre-filling,
 *   and read-only IMAP OTP/confirmation lookup so you never leave the screen while signing up or applying.
 * - Strictly human-in-the-loop: NEVER bypasses CAPTCHAs or submits forms/accounts silently without you.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPortalAssistantDialog(
    offer: JobOffer,
    profile: UserProfile,
    settingsRepo: SettingsRepository,
    onDismiss: () -> Unit,
    onMarkApplied: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepo.settings.collectAsState()
    val isPortalSetupMode = offer.id.startsWith("portal_setup_")
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(offer.url) }
    var isCheckingOtp by remember { mutableStateOf(false) }
    var isTestingPortal by remember { mutableStateOf(false) }
    var otpInfo by remember { mutableStateOf<DetectedOtpOrLink?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        pendingFileCallback?.onReceiveValue(uris)
        pendingFileCallback = null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Copilot Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.plateBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌐 Assistant Portail Web · ${offer.company}",
                                style = Type.bodySmall.bold,
                                color = Palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUrl,
                                style = Type.label.monospaced,
                                color = Palette.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "${offer.title} - ${offer.company}")
                                        putExtra(Intent.EXTRA_TEXT, currentUrl)
                                    }
                                    runCatching { context.startActivity(Intent.createChooser(share, "Partager le lien")) }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Partager", tint = Palette.electricBlue, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))) }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Navigateur", tint = Palette.emeraldAccent, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Palette.text, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Single-row quick tools for human-assisted registration, form fill, file upload & IMAP OTP lookup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassFilterChip(
                            text = "Remplir Formulaire",
                            selected = true,
                            selectedColor = Palette.blueSolidBg,
                            onClick = {
                                val effectiveEmail = profile.email.ifBlank { settingsRepo.settings.value.senderEmail }
                                webViewRef?.evaluateJavascript(buildVisibleFormFillJs(profile, offer, effectiveEmail)) { rawRes ->
                                    val count = rawRes?.toIntOrNull() ?: 0
                                    Toast.makeText(
                                        context,
                                        if (count > 0) "$count champ(s) pré-rempli(s) ✓" else "Aucun champ texte standard détecté sur cette étape.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        GlassFilterChip(
                            text = "Exporter CV/Lettre",
                            selected = false,
                            selectedColor = Palette.emeraldSolidBg,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val exported = exportDocsToDownloads(context, offer)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (exported > 0) "$exported fichier(s) copié(s) dans Téléchargements ✓" else "Préparez d'abord le dossier CV/Lettre.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )

                        GlassFilterChip(
                            text = if (isCheckingOtp) "Lecture Yahoo..." else "Lire Code/Lien IMAP",
                            selected = false,
                            selectedColor = Palette.emeraldSolidBg,
                            onClick = {
                                if (isCheckingOtp) return@GlassFilterChip
                                isCheckingOtp = true
                                scope.launch(Dispatchers.IO) {
                                    val detected = fetchLatestOtpOrConfirmationLink(settingsRepo, profile)
                                    withContext(Dispatchers.Main) {
                                        otpInfo = detected
                                        isCheckingOtp = false
                                    }
                                }
                            }
                        )

                        GlassFilterChip(
                            text = "Copier Lettre",
                            selected = false,
                            selectedColor = Palette.blueSolidBg,
                            onClick = {
                                copyToClipboard(context, "Lettre", offer.generatedLetter)
                                Toast.makeText(context, "Lettre MLDP copiée ✓", Toast.LENGTH_SHORT).show()
                            }
                        )

                        GlassFilterChip(
                            text = "Copier Email",
                            selected = false,
                            selectedColor = Palette.blueSolidBg,
                            onClick = {
                                val mail = settingsRepo.settings.value.senderEmail.ifBlank { profile.email }
                                copyToClipboard(context, "Email", mail)
                                Toast.makeText(context, "$mail copié ✓", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // Portals Bar: direct quick access + live session test for sites requiring accounts
                        PortalSessionRegistry.portals.forEach { portal ->
                            val isPortalConnected = settings.isPortalConnected(portal.id)
                            GlassFilterChip(
                                text = (if (isPortalConnected) "✓ " else "") + portal.name.substringBefore(" "),
                                selected = currentUrl.contains(portal.domainUrl.removePrefix("https://").removePrefix("www."), ignoreCase = true),
                                selectedColor = Palette.emeraldSolidBg,
                                onClick = {
                                    val target = if (isPortalSetupMode) portal.signupUrl else portal.loginUrl
                                    webViewRef?.loadUrl(target)
                                }
                            )
                        }

                        val activePortal = PortalSessionRegistry.portals.firstOrNull {
                            currentUrl.contains(it.domainUrl.removePrefix("https://").removePrefix("www."), ignoreCase = true)
                        }
                        if (activePortal != null) {
                            GlassFilterChip(
                                text = if (isTestingPortal) "Test..." else "Tester ${activePortal.name.substringBefore(" ")}",
                                selected = false,
                                selectedColor = Palette.emeraldSolidBg,
                                onClick = {
                                    if (isTestingPortal) return@GlassFilterChip
                                    isTestingPortal = true
                                    scope.launch {
                                        val res = PortalSessionRegistry.testPortalConnection(activePortal)
                                        settingsRepo.update { it.withPortalStatus(activePortal.id, res.connected) }
                                        otpInfo = DetectedOtpOrLink(
                                            sourceSubject = "Test ${activePortal.name}",
                                            statusNote = res.message
                                        )
                                        isTestingPortal = false
                                    }
                                }
                            )
                        }
                    }

                    // Show detected OTP or Verification URL from read-only IMAP if triggered
                    otpInfo?.let { info ->
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Palette.plateBackground, RoundedRectangle(8.dp))
                                .border(0.5.dp, Palette.emeraldAccent.copy(alpha = 0.5f), RoundedRectangle(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = info.sourceSubject.ifBlank { info.statusNote },
                                        style = Type.label.bold,
                                        color = Palette.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (info.code.isNotBlank()) {
                                        Text(
                                            text = "Code détecté : ${info.code}",
                                            style = Type.bodySmall.bold.monospaced,
                                            color = Palette.emeraldAccent
                                        )
                                    } else if (info.statusNote.isNotBlank() && info.sourceSubject.isNotBlank()) {
                                        Text(
                                            text = info.statusNote,
                                            style = Type.label,
                                            color = Palette.secondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (info.code.isNotBlank()) {
                                        GlassSoftButton(
                                            text = "Copier",
                                            icon = Icons.Default.ContentCopy,
                                            accentColor = Palette.emeraldAccent,
                                            height = 34.dp,
                                            onClick = {
                                                copyToClipboard(context, "OTP", info.code)
                                                Toast.makeText(context, "Code ${info.code} copié ✓", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    if (info.verificationUrl.startsWith("http")) {
                                        GlassSoftButton(
                                            text = "Ouvrir lien",
                                            icon = Icons.Default.Email,
                                            accentColor = Palette.electricBlue,
                                            height = 34.dp,
                                            onClick = { webViewRef?.loadUrl(info.verificationUrl) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Visible Interactive Browser View (persists login cookies across portal visits)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                            }
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                this.getSettings().apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        isLoadingPage = true
                                        if (!url.isNullOrBlank()) currentUrl = url
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoadingPage = false
                                        if (!url.isNullOrBlank()) currentUrl = url
                                        CookieManager.getInstance().flush()
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val nextUrl = request?.url?.toString().orEmpty()
                                        if (nextUrl.startsWith("mailto:", ignoreCase = true)) {
                                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(nextUrl))) }
                                            return true
                                        }
                                        return false
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallback: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        pendingFileCallback?.onReceiveValue(null)
                                        pendingFileCallback = filePathCallback
                                        val chooserIntent = runCatching { fileChooserParams?.createIntent() }.getOrNull()
                                            ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                type = "*/*"
                                            }
                                        runCatching {
                                            fileChooserLauncher.launch(chooserIntent)
                                        }.onFailure {
                                            pendingFileCallback?.onReceiveValue(null)
                                            pendingFileCallback = null
                                        }
                                        return true
                                    }
                                }
                                loadUrl(offer.url)
                                webViewRef = this
                            }
                        }
                    )

                    if (isLoadingPage) {
                        CircularProgressIndicator(
                            color = Palette.emeraldAccent,
                            modifier = Modifier.align(Alignment.Center).size(32.dp)
                        )
                    }
                }

                // Bottom Governance Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.plateBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSoftButton(
                        text = "Retour",
                        icon = Icons.Default.Description,
                        accentColor = Palette.secondary,
                        height = 40.dp,
                        onClick = {
                            val wv = webViewRef
                            if (wv != null && wv.canGoBack()) wv.goBack() else onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassSolidButton(
                        text = if (isPortalSetupMode) "Session Prête ✓" else "Candidature Validée ✓",
                        icon = Icons.Default.CheckCircle,
                        containerColor = Palette.emeraldSolidBg,
                        height = 40.dp,
                        onClick = {
                            if (!isPortalSetupMode) {
                                onMarkApplied()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun exportDocsToDownloads(context: Context, offer: JobOffer): Int {
    val files = listOfNotNull(
        offer.tailoredCvPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() },
        offer.letterPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    )
    var count = 0
    for (src in files) {
        val mime = if (src.name.endsWith(".docx", ignoreCase = true)) {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        } else {
            "application/pdf"
        }
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, src.name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/JobHunterAI")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
                resolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
                true
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "JobHunterAI").apply { mkdirs() }
                src.copyTo(File(dir, src.name), overwrite = true)
                true
            }
        }.getOrDefault(false)
        if (ok) count++
    }
    return count
}

private suspend fun fetchLatestOtpOrConfirmationLink(
    settingsRepo: SettingsRepository,
    profile: UserProfile
): DetectedOtpOrLink {
    val settings = settingsRepo.settings.value
    if (!settings.canSendSilently() || settings.smtpPassword.isBlank()) {
        return DetectedOtpOrLink(statusNote = "Connectez votre Email & App Password dans Agent 24/7 pour lire l'OTP.")
    }
    val dispatcher = EmailDispatcher(ApplicationAutomationManager.buildEmailConfig(settings, profile))
    val res = dispatcher.fetchRecentInboxReplies(maxMessages = 8, lookbackDays = 2)
    val msgs = res.getOrNull().orEmpty()
    if (msgs.isEmpty()) {
        return DetectedOtpOrLink(statusNote = res.exceptionOrNull()?.message?.take(60) ?: "Aucun message récent trouvé.")
    }

    val otpHints = listOf("code", "otp", "verif", "vérif", "confirm", "activ", "pin", "mot de passe", "account", "compte", "emploitic", "tanqeeb", "greenhouse", "ashby")
    val best = msgs.firstOrNull { m ->
        val txt = "${m.subject} ${m.snippet}".lowercase()
        otpHints.any { txt.contains(it) }
    } ?: msgs.first()

    val hay = "${best.subject} ${best.snippet}"
    val codeMatch = Regex("""\b(\d{4,8})\b""").find(hay)?.groupValues?.getOrNull(1).orEmpty()
    val urlMatch = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
        .findAll(best.snippet)
        .map { it.value.trimEnd('.', ',', ')') }
        .firstOrNull { u ->
            val low = u.lowercase()
            low.contains("confirm") || low.contains("verif") || low.contains("activ") || low.contains("token") || low.contains("auth")
        }.orEmpty()

    return DetectedOtpOrLink(
        sourceSubject = "${best.from.substringBefore("<").trim()} — ${best.subject}",
        code = codeMatch,
        verificationUrl = urlMatch,
        statusNote = if (codeMatch.isBlank() && urlMatch.isBlank()) best.snippet.take(70) else ""
    )
}

/**
 * Only populates visible standard text/email/tel/textarea inputs when explicitly triggered by the user.
 * Never clicks buttons, submits forms, or interacts with CAPTCHA / checkbox widgets.
 */
private fun buildVisibleFormFillJs(profile: UserProfile, offer: JobOffer, effectiveEmail: String): String {
    val parts = profile.fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val firstName = parts.dropLast(1).joinToString(" ").ifBlank { parts.firstOrNull().orEmpty() }
    val lastName = parts.lastOrNull().orEmpty()
    val payload = JSONObject().apply {
        put("fullName", profile.fullName)
        put("firstName", firstName)
        put("lastName", lastName)
        put("email", effectiveEmail)
        put("phone", profile.phone)
        put("city", MldpCoverLetterData.extractCityName(profile.address, isFrench = true))
        put("letter", offer.generatedLetter)
    }.toString()

    return """
        (function() {
            var d = $payload;
            var filled = 0;
            function setVal(el, val) {
                if (!val || !el || el.disabled || el.readOnly) return false;
                var proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
                var desc = Object.getOwnPropertyDescriptor(proto, 'value');
                if (desc && desc.set) { desc.set.call(el, val); } else { el.value = val; }
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
            }
            var els = document.querySelectorAll('input, textarea');
            for (var i = 0; i < els.length; i++) {
                var el = els[i];
                var t = (el.type || '').toLowerCase();
                if (t === 'hidden' || t === 'checkbox' || t === 'radio' || t === 'submit' || t === 'file' || t === 'password') continue;
                var key = ((el.name || '') + ' ' + (el.id || '') + ' ' + (el.placeholder || '') + ' ' + (el.getAttribute('aria-label') || '')).toLowerCase();
                if (key.indexOf('captcha') !== -1 || key.indexOf('recaptcha') !== -1 || key.indexOf('turnstile') !== -1) continue;
                var v = '';
                if (el.tagName === 'TEXTAREA' || key.indexOf('letter') !== -1 || key.indexOf('motivation') !== -1 || key.indexOf('cover') !== -1) v = d.letter;
                else if (t === 'email' || key.indexOf('mail') !== -1) v = d.email;
                else if (t === 'tel' || key.indexOf('phone') !== -1 || key.indexOf('tel') !== -1 || key.indexOf('mobile') !== -1) v = d.phone;
                else if (key.indexOf('first') !== -1 || key.indexOf('prenom') !== -1 || key.indexOf('prénom') !== -1) v = d.firstName;
                else if (key.indexOf('last') !== -1 || key.indexOf('nom') !== -1 && key.indexOf('prenom') === -1 && key.indexOf('full') === -1) v = d.lastName;
                else if (key.indexOf('name') !== -1) v = d.fullName;
                else if (key.indexOf('city') !== -1 || key.indexOf('ville') !== -1 || key.indexOf('location') !== -1) v = d.city;
                if (v && (!el.value || el.value.trim() === '')) {
                    if (setVal(el, v)) filled++;
                }
            }
            return filled;
        })();
    """.trimIndent()
}
