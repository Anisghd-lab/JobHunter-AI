package com.ghdinteractivestudio.jobhunter.domain.email

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class EmailAuthType {
    OAUTH2_GMAIL,
    SMTP_DIRECT
}

data class EmailConfig(
    val authType: EmailAuthType = EmailAuthType.SMTP_DIRECT,
    val senderEmail: String = "",
    val senderName: String = "",
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 465,
    val smtpUser: String = "",
    val smtpPassword: String = "",
    val oauthAccessToken: String = ""
) {
    fun normalizedPassword(): String =
        if (smtpHost.contains("yahoo", ignoreCase = true) || smtpHost.contains("gmail", ignoreCase = true)) {
            smtpPassword.replace(" ", "").trim()
        } else {
            smtpPassword.trim()
        }

    fun inferImapHost(): String = when {
        smtpHost.contains("yahoo", ignoreCase = true) || senderEmail.contains("@yahoo.", ignoreCase = true) -> "imap.mail.yahoo.com"
        smtpHost.contains("gmail", ignoreCase = true) || senderEmail.contains("@gmail.", ignoreCase = true) -> "imap.gmail.com"
        smtpHost.contains("outlook", ignoreCase = true) || smtpHost.contains("office365", ignoreCase = true) ||
            senderEmail.contains("@outlook.", ignoreCase = true) || senderEmail.contains("@hotmail.", ignoreCase = true) -> "outlook.office365.com"
        smtpHost.startsWith("smtp.", ignoreCase = true) -> "imap." + smtpHost.substringAfter("smtp.")
        else -> smtpHost
    }
}

data class IncomingReplyEmail(
    val from: String,
    val subject: String,
    val date: String,
    val snippet: String
)

class EmailDispatcher(private val config: EmailConfig) {

    private val httpClient = OkHttpClient()

    suspend fun sendApplicationEmail(
        recipientEmail: String,
        subject: String,
        bodyText: String,
        attachments: List<File>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            when (config.authType) {
                EmailAuthType.SMTP_DIRECT -> sendViaSmtpWithFallback(recipientEmail, subject, bodyText, attachments)
                EmailAuthType.OAUTH2_GMAIL -> sendViaGmailOAuth2(recipientEmail, subject, bodyText, attachments)
            }
            true
        }
    }

    /**
     * Tests live SMTP authentication without sending any message, trying primary port then fallback port.
     */
    suspend fun testSmtpConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (config.authType == EmailAuthType.OAUTH2_GMAIL) {
                if (config.oauthAccessToken.isBlank()) error("Token OAuth2 Gmail manquant.")
                return@runCatching "Gmail OAuth2 configuré ✓"
            }
            if (config.senderEmail.isBlank() || config.normalizedPassword().isBlank()) {
                error("Email ou mot de passe d'application manquant.")
            }
            val usedPort = executeSmtpAuth(config.smtpPort) { _, _ -> /* Auth-only test */ }
            "Connexion SMTP & Authentification réussies (${config.smtpHost}:$usedPort) ✓"
        }
    }

    /**
     * Read-only IMAP check (EXAMINE INBOX + BODY.PEEK) over TLS port 993 using the user's same email + App Password.
     * Never alters Read/Unread flags in the user's mailbox.
     */
    suspend fun fetchRecentInboxReplies(maxMessages: Int = 20): Result<List<IncomingReplyEmail>> = withContext(Dispatchers.IO) {
        runCatching {
            val password = config.normalizedPassword()
            val user = config.smtpUser.ifBlank { config.senderEmail }.trim()
            if (user.isBlank() || password.isBlank()) {
                error("Identifiants email requis pour la vérification IMAP.")
            }

            val imapHost = config.inferImapHost()
            val socket = SSLSocketFactory.getDefault().createSocket(imapHost, 993) as SSLSocket
            socket.soTimeout = 20000
            socket.startHandshake()

            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))

            fun readTagged(tag: String): List<String> {
                val lines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    lines.add(line)
                    if (line.startsWith("$tag OK") || line.startsWith("$tag NO") || line.startsWith("$tag BAD")) {
                        if (!line.startsWith("$tag OK")) error("IMAP error: $line")
                        break
                    }
                }
                return lines
            }

            val greeting = reader.readLine() ?: ""
            if (!greeting.startsWith("* OK")) error("IMAP connection error: $greeting")

            val escapedUser = user.replace("\"", "\\\"")
            val escapedPass = password.replace("\"", "\\\"")
            writer.write("a1 LOGIN \"$escapedUser\" \"$escapedPass\"\r\n")
            writer.flush()
            readTagged("a1")

            writer.write("a2 EXAMINE INBOX\r\n")
            writer.flush()
            val examineLines = readTagged("a2")
            val existsCount = examineLines
                .firstOrNull { it.contains(" EXISTS", ignoreCase = true) }
                ?.let { Regex("""\*\s+(\d+)\s+EXISTS""").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
                ?: 0

            val results = mutableListOf<IncomingReplyEmail>()
            if (existsCount > 0) {
                val startSeq = maxOf(1, existsCount - maxMessages + 1)
                writer.write("a3 FETCH $startSeq:$existsCount (BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE)] BODY.PEEK[TEXT]<0.1000>)\r\n")
                writer.flush()
                val fetchLines = readTagged("a3")

                // Parse streamed FETCH blocks cleanly
                var currentFrom = ""
                var currentSubject = ""
                var currentDate = ""
                val bodyBuf = StringBuilder()

                fun flushCurrent() {
                    if (currentFrom.isNotBlank() || currentSubject.isNotBlank()) {
                        results.add(
                            IncomingReplyEmail(
                                from = currentFrom.trim(),
                                subject = currentSubject.trim(),
                                date = currentDate.trim(),
                                snippet = bodyBuf.toString()
                                    .replace(Regex("<[^>]+>"), " ")
                                    .replace(Regex("\\s+"), " ")
                                    .trim()
                                    .take(600)
                            )
                        )
                    }
                    currentFrom = ""
                    currentSubject = ""
                    currentDate = ""
                    bodyBuf.setLength(0)
                }

                for (line in fetchLines) {
                    when {
                        line.startsWith("* ") && line.contains(" FETCH ") -> flushCurrent()
                        line.startsWith("From:", ignoreCase = true) -> currentFrom = line.substringAfter(":").trim()
                        line.startsWith("Subject:", ignoreCase = true) -> currentSubject = decodeMimeHeader(line.substringAfter(":").trim())
                        line.startsWith("Date:", ignoreCase = true) -> currentDate = line.substringAfter(":").trim()
                        line.startsWith("a3 OK") -> flushCurrent()
                        !line.startsWith(")") && !line.startsWith("BODY[") && line.isNotBlank() -> {
                            if (bodyBuf.length < 1200) bodyBuf.append(line).append(' ')
                        }
                    }
                }
                flushCurrent()
            }

            runCatching {
                writer.write("a4 LOGOUT\r\n")
                writer.flush()
            }
            runCatching { socket.close() }
            results.reversed()
        }
    }

    private fun decodeMimeHeader(raw: String): String {
        val m = Regex("""=\?UTF-8\?B\?([^?]+)\?=""", RegexOption.IGNORE_CASE).find(raw)
        if (m != null) {
            return runCatching {
                String(Base64.decode(m.groupValues[1], Base64.DEFAULT), Charsets.UTF_8)
            }.getOrDefault(raw)
        }
        return raw
    }

    private fun readSmtpResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            sb.append(line).append("\n")
            if (line.length < 4 || line[3] != '-') {
                break
            }
        }
        return sb.toString().trim()
    }

    private fun sendViaSmtpWithFallback(
        recipient: String,
        subject: String,
        bodyText: String,
        attachments: List<File>
    ) {
        executeSmtpAuth(config.smtpPort) { writer, readReply ->
            fun sendCmd(cmd: String, expectedCode: String? = null): String {
                writer.write(cmd + "\r\n")
                writer.flush()
                val response = readReply()
                if (expectedCode != null && !response.startsWith(expectedCode)) {
                    error("SMTP Error sending '${cmd.take(20)}': $response")
                }
                return response
            }

            sendCmd("MAIL FROM:<${config.senderEmail}>", "250")
            sendCmd("RCPT TO:<$recipient>", "250")
            sendCmd("DATA", "354")

            val boundary = "==_JobHunterBoundary_${System.currentTimeMillis()}_=="

            writer.write("From: ${config.senderName.ifBlank { config.senderEmail }} <${config.senderEmail}>\r\n")
            writer.write("To: <$recipient>\r\n")
            writer.write("Subject: =?UTF-8?B?${Base64.encodeToString(subject.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}?=\r\n")
            writer.write("MIME-Version: 1.0\r\n")
            writer.write("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")

            writer.write("--$boundary\r\n")
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n")
            writer.write("Content-Transfer-Encoding: 8bit\r\n\r\n")
            writer.write(bodyText.replace("\n.", "\n..") + "\r\n\r\n")

            for (file in attachments) {
                if (file.exists()) {
                    val mimeType = if (file.name.endsWith(".docx", ignoreCase = true)) {
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    } else {
                        "application/pdf"
                    }
                    val encodedBytes = Base64.encodeToString(file.readBytes(), Base64.CRLF)

                    writer.write("--$boundary\r\n")
                    writer.write("Content-Type: $mimeType; name=\"${file.name}\"\r\n")
                    writer.write("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
                    writer.write("Content-Transfer-Encoding: base64\r\n\r\n")
                    writer.write(encodedBytes + "\r\n\r\n")
                }
            }

            writer.write("--$boundary--\r\n")
            sendCmd(".", "250")
        }
    }

    /**
     * Authenticates via primary SMTP port, automatically retrying alternate TLS/STARTTLS port (465 <-> 587)
     * if connection handshake fails, before invoking the SMTP session block.
     */
    private fun executeSmtpAuth(
        preferredPort: Int,
        sessionBlock: (writer: BufferedWriter, readReply: () -> String) -> Unit
    ): Int {
        val alternatePort = if (preferredPort == 465) 587 else 465
        var lastError: Throwable? = null

        for (port in listOf(preferredPort, alternatePort).distinct()) {
            var socket: Socket? = null
            try {
                socket = if (port == 465) {
                    SSLSocketFactory.getDefault().createSocket(config.smtpHost, port)
                } else {
                    Socket(config.smtpHost, port)
                }
                socket.soTimeout = 20000

                var reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                var writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

                fun sendCmd(cmd: String, expectedCode: String? = null): String {
                    writer.write(cmd + "\r\n")
                    writer.flush()
                    val response = readSmtpResponse(reader)
                    if (expectedCode != null && !response.startsWith(expectedCode)) {
                        error("SMTP Error (${config.smtpHost}:$port): $response")
                    }
                    return response
                }

                val banner = readSmtpResponse(reader)
                if (!banner.startsWith("220")) {
                    error("SMTP Connection error ($port): $banner")
                }

                var ehloResp = sendCmd("EHLO localhost", "250")

                if (port == 587) {
                    sendCmd("STARTTLS", "220")
                    val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    val tlsSocket = sslFactory.createSocket(socket, config.smtpHost, port, true) as SSLSocket
                    tlsSocket.startHandshake()
                    socket = tlsSocket
                    reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                    ehloResp = sendCmd("EHLO localhost", "250")
                }

                val user = config.smtpUser.ifBlank { config.senderEmail }.trim()
                val pass = config.normalizedPassword()

                // Prefer AUTH PLAIN when advertised (Yahoo Mail Hermes prefers AUTH PLAIN), with automatic AUTH LOGIN fallback
                var authenticated = false
                if (ehloResp.contains("PLAIN", ignoreCase = true)) {
                    val plainToken = Base64.encodeToString("\u0000$user\u0000$pass".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val plainReply = sendCmd("AUTH PLAIN $plainToken")
                    if (plainReply.startsWith("235")) {
                        authenticated = true
                    } else if (!plainReply.startsWith("535")) {
                        // Continue to AUTH LOGIN fallback on non-fatal auth mechanism mismatch
                    }
                }
                if (!authenticated) {
                    sendCmd("AUTH LOGIN", "334")
                    sendCmd(Base64.encodeToString(user.toByteArray(Charsets.UTF_8), Base64.NO_WRAP), "334")
                    sendCmd(Base64.encodeToString(pass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP), "235")
                }

                sessionBlock(writer) { readSmtpResponse(reader) }

                runCatching { sendCmd("QUIT") }
                runCatching { socket.close() }
                return port
            } catch (e: Throwable) {
                runCatching { socket?.close() }
                lastError = e
                // Surface exact 535 reply & guidance without pointless port retries
                if (e.message?.contains("535") == true || e.message?.contains("Authentication") == true) {
                    val detail = e.message?.substringAfter("): ")?.trim().orEmpty()
                    throw IllegalStateException(
                        "Identifiants refusés ($detail). Yahoo/Gmail exige un « Mot de passe d'application » 16 car. (pas le mot de passe du compte), sinon liez l'appli Yahoo Mail ci-dessous.",
                        e
                    )
                }
            }
        }
        throw lastError ?: IllegalStateException("Connexion SMTP impossible sur ${config.smtpHost}")
    }

    private fun sendViaGmailOAuth2(
        recipient: String,
        subject: String,
        bodyText: String,
        attachments: List<File>
    ) {
        val boundary = "==_GmailBoundary_${System.currentTimeMillis()}_=="
        val rawMessage = StringBuilder()

        rawMessage.append("From: ${config.senderName.ifBlank { config.senderEmail }} <${config.senderEmail}>\r\n")
        rawMessage.append("To: <$recipient>\r\n")
        rawMessage.append("Subject: =?UTF-8?B?${Base64.encodeToString(subject.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}?=\r\n")
        rawMessage.append("MIME-Version: 1.0\r\n")
        rawMessage.append("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")

        rawMessage.append("--$boundary\r\n")
        rawMessage.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
        rawMessage.append(bodyText).append("\r\n\r\n")

        for (file in attachments) {
            if (file.exists()) {
                val mimeType = if (file.name.endsWith(".docx", ignoreCase = true)) {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                } else "application/pdf"

                val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                rawMessage.append("--$boundary\r\n")
                rawMessage.append("Content-Type: $mimeType; name=\"${file.name}\"\r\n")
                rawMessage.append("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
                rawMessage.append("Content-Transfer-Encoding: base64\r\n\r\n")
                rawMessage.append(encoded).append("\r\n\r\n")
            }
        }
        rawMessage.append("--$boundary--\r\n")

        val base64Url = Base64.encodeToString(
            rawMessage.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )

        val jsonPayload = """{"raw": "$base64Url"}"""
        val request = Request.Builder()
            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
            .addHeader("Authorization", "Bearer ${config.oauthAccessToken}")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                error("Gmail API Error ${response.code}: $err")
            }
        }
    }
}
