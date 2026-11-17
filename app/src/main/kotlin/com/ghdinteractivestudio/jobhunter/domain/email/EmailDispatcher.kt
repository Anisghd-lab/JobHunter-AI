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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
    val snippet: String,
    val messageId: String = ""
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
     * Uses SEARCH SINCE + RFC 2047 (Base64 & Quoted-Printable) decoding and NEVER alters Read/Unread flags.
     */
    suspend fun fetchRecentInboxReplies(
        maxMessages: Int = 40,
        lookbackDays: Int = 14
    ): Result<List<IncomingReplyEmail>> = withContext(Dispatchers.IO) {
        runCatching {
            val password = config.normalizedPassword()
            val user = config.smtpUser.ifBlank { config.senderEmail }.trim()
            if (user.isBlank() || password.isBlank()) {
                error("Identifiants email requis pour la vérification IMAP.")
            }

            val imapHost = config.inferImapHost()
            val socket = SSLSocketFactory.getDefault().createSocket(imapHost, 993) as SSLSocket
            socket.soTimeout = 20000
            try {
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
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -lookbackDays) }
                    val sinceDate = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(cal.time)
                    val searchIds = runCatching {
                        writer.write("a2b SEARCH SINCE $sinceDate\r\n")
                        writer.flush()
                        val searchLines = readTagged("a2b")
                        searchLines
                            .filter { it.startsWith("* SEARCH", ignoreCase = true) }
                            .flatMap { it.removePrefix("* SEARCH").trim().split(Regex("\\s+")) }
                            .mapNotNull { it.toIntOrNull() }
                    }.getOrDefault(emptyList())

                    val fetchTarget = if (searchIds.isNotEmpty()) {
                        searchIds.takeLast(maxMessages).joinToString(",")
                    } else {
                        val startSeq = maxOf(1, existsCount - maxMessages + 1)
                        "$startSeq:$existsCount"
                    }

                    writer.write(
                        "a3 FETCH $fetchTarget (BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE MESSAGE-ID)] BODY.PEEK[TEXT]<0.1600>)\r\n"
                    )
                    writer.flush()
                    val fetchLines = readTagged("a3")

                    var currentFrom = ""
                    var currentSubject = ""
                    var currentDate = ""
                    var currentMessageId = ""
                    var activeHeader = ""
                    var inHeaderSection = false
                    val bodyBuf = StringBuilder()

                    fun flushCurrent() {
                        if (currentFrom.isNotBlank() || currentSubject.isNotBlank()) {
                            val cleanSnippet = decodeBodySnippet(bodyBuf.toString())
                            val cleanSubject = decodeMimeHeader(currentSubject.trim())
                            val cleanFrom = decodeMimeHeader(currentFrom.trim())
                            val msgId = currentMessageId.trim().ifBlank {
                                "fallback_${cleanFrom}|${cleanSubject}|${currentDate.trim()}".hashCode().toString()
                            }
                            results.add(
                                IncomingReplyEmail(
                                    from = cleanFrom,
                                    subject = cleanSubject,
                                    date = currentDate.trim(),
                                    snippet = cleanSnippet,
                                    messageId = msgId
                                )
                            )
                        }
                        currentFrom = ""
                        currentSubject = ""
                        currentDate = ""
                        currentMessageId = ""
                        activeHeader = ""
                        inHeaderSection = false
                        bodyBuf.setLength(0)
                    }

                    for (line in fetchLines) {
                        when {
                            line.startsWith("* ") && line.contains(" FETCH ") -> {
                                flushCurrent()
                                inHeaderSection = true
                            }
                            line.startsWith("a3 OK") -> flushCurrent()
                            line.trimStart().startsWith("BODY[HEADER", ignoreCase = true) -> inHeaderSection = true
                            line.trimStart().startsWith("BODY[TEXT", ignoreCase = true) ||
                                (inHeaderSection && line.isBlank()) -> {
                                inHeaderSection = false
                                activeHeader = ""
                            }
                            inHeaderSection && (line.startsWith(" ") || line.startsWith("\t")) && activeHeader.isNotBlank() -> {
                                when (activeHeader) {
                                    "FROM" -> currentFrom += " " + line.trim()
                                    "SUBJECT" -> currentSubject += " " + line.trim()
                                    "DATE" -> currentDate += " " + line.trim()
                                    "MSGID" -> currentMessageId += line.trim()
                                }
                            }
                            inHeaderSection && line.startsWith("From:", ignoreCase = true) -> {
                                activeHeader = "FROM"
                                currentFrom = line.substringAfter(":").trim()
                            }
                            inHeaderSection && line.startsWith("Subject:", ignoreCase = true) -> {
                                activeHeader = "SUBJECT"
                                currentSubject = line.substringAfter(":").trim()
                            }
                            inHeaderSection && line.startsWith("Date:", ignoreCase = true) -> {
                                activeHeader = "DATE"
                                currentDate = line.substringAfter(":").trim()
                            }
                            inHeaderSection && line.startsWith("Message-ID:", ignoreCase = true) -> {
                                activeHeader = "MSGID"
                                currentMessageId = line.substringAfter(":").trim()
                            }
                            inHeaderSection && line.contains(":") -> activeHeader = ""
                            !line.trimStart().startsWith(")") && !line.trimStart().startsWith("BODY[") && line.isNotBlank() -> {
                                if (bodyBuf.length < 2000) bodyBuf.append(line).append('\n')
                            }
                        }
                    }
                    flushCurrent()
                }

                runCatching {
                    writer.write("a4 LOGOUT\r\n")
                    writer.flush()
                }
                results.reversed()
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    private fun decodeMimeHeader(raw: String): String {
        if (!raw.contains("=?")) return raw
        val normalized = raw.replace(Regex("""\?=\s+=\?"""), "?==?")
        val tokenRegex = Regex("""=\?([^?]+)\?([BQ])\?([^?]+)\?=""", RegexOption.IGNORE_CASE)
        return tokenRegex.replace(normalized) { match ->
            val charsetName = match.groupValues[1].trim()
            val encoding = match.groupValues[2].uppercase()
            val payload = match.groupValues[3]
            val charset = runCatching { Charset.forName(charsetName) }.getOrDefault(Charsets.UTF_8)
            runCatching {
                when (encoding) {
                    "B" -> String(Base64.decode(payload, Base64.DEFAULT), charset)
                    "Q" -> decodeQuotedPrintable(payload.replace('_', ' '), charset)
                    else -> match.value
                }
            }.getOrDefault(match.value)
        }.trim()
    }

    private fun decodeQuotedPrintable(input: String, charset: Charset = Charsets.UTF_8): String {
        val cleaned = input.replace(Regex("""=\r?\n"""), "")
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < cleaned.length) {
            val ch = cleaned[i]
            if (ch == '=' && i + 2 < cleaned.length) {
                val hex = cleaned.substring(i + 1, i + 3)
                val parsed = hex.toIntOrNull(16)
                if (parsed != null) {
                    out.write(parsed)
                    i += 3
                    continue
                }
            }
            out.write(ch.code and 0xFF)
            i++
        }
        return String(out.toByteArray(), charset)
    }

    private fun decodeBodySnippet(rawBody: String): String {
        val qpDecoded = if (rawBody.contains("=C3=") || rawBody.contains("=20") || rawBody.contains("=\n")) {
            decodeQuotedPrintable(rawBody, Charsets.UTF_8)
        } else {
            rawBody
        }
        return qpDecoded
            .replace(Regex("(?im)^--[a-zA-Z0-9._=-]+(--)?\\s*$"), " ")
            .replace(Regex("(?im)^(Content-Type|Content-Transfer-Encoding|Content-Disposition|MIME-Version):.*$"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(700)
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

                var authenticated = false
                if (ehloResp.contains("PLAIN", ignoreCase = true)) {
                    val plainToken = Base64.encodeToString("\u0000$user\u0000$pass".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val plainReply = sendCmd("AUTH PLAIN $plainToken")
                    when {
                        plainReply.startsWith("235") -> authenticated = true
                        plainReply.startsWith("535") || plainReply.startsWith("534") -> {
                            error("SMTP Error (${config.smtpHost}:$port): $plainReply")
                        }
                        plainReply.isBlank() -> {
                            error("SMTP Error (${config.smtpHost}:$port): Connexion fermée après AUTH PLAIN")
                        }
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
                val msg = e.message.orEmpty()
                if (msg.contains("535") || msg.contains("534") || msg.contains("AUTH005") || msg.contains("Authentication")) {
                    val detail = msg.substringAfter("): ").trim().ifBlank { msg }
                    val friendly = if (msg.contains("AUTH005", ignoreCase = true)) {
                        "Yahoo bloque temporairement SMTP ($detail — trop de tentatives). Attendez 15 min ou utilisez l'envoi via l'Appli Yahoo Mail ci-dessous !"
                    } else {
                        "Identifiants refusés ($detail). Yahoo/Gmail exige un Mot de passe d'application à 16 lettres (pas le mot de passe normal), ou utilisez l'Appli Yahoo Mail !"
                    }
                    throw IllegalStateException(friendly, e)
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
