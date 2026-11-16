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
                EmailAuthType.SMTP_DIRECT -> sendViaSmtpSocket(recipientEmail, subject, bodyText, attachments)
                EmailAuthType.OAUTH2_GMAIL -> sendViaGmailOAuth2(recipientEmail, subject, bodyText, attachments)
            }
            true
        }
    }

    private fun sendViaSmtpSocket(
        recipient: String,
        subject: String,
        bodyText: String,
        attachments: List<File>
    ) {
        val socket: Socket = if (config.smtpPort == 465) {
            SSLSocketFactory.getDefault().createSocket(config.smtpHost, config.smtpPort)
        } else {
            Socket(config.smtpHost, config.smtpPort)
        }

        socket.soTimeout = 15000
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

        fun sendCmd(cmd: String, expectedCode: String? = null): String {
            writer.write(cmd + "\r\n")
            writer.flush()
            val response = reader.readLine() ?: ""
            if (expectedCode != null && !response.startsWith(expectedCode)) {
                error("SMTP Error sending '$cmd': $response")
            }
            return response
        }

        // Read server banner
        reader.readLine()

        sendCmd("EHLO localhost", "250")
        sendCmd("AUTH LOGIN", "334")
        sendCmd(Base64.encodeToString(config.smtpUser.toByteArray(), Base64.NO_WRAP), "334")
        sendCmd(Base64.encodeToString(config.smtpPassword.toByteArray(), Base64.NO_WRAP), "235")

        sendCmd("MAIL FROM:<${config.senderEmail}>", "250")
        sendCmd("RCPT TO:<$recipient>", "250")
        sendCmd("DATA", "354")

        val boundary = "==_JobHunterBoundary_${System.currentTimeMillis()}_=="

        writer.write("From: ${config.senderName} <${config.senderEmail}>\r\n")
        writer.write("To: <$recipient>\r\n")
        writer.write("Subject: =?UTF-8?B?${Base64.encodeToString(subject.toByteArray(), Base64.NO_WRAP)}?=\r\n")
        writer.write("MIME-Version: 1.0\r\n")
        writer.write("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")

        // Body text part
        writer.write("--$boundary\r\n")
        writer.write("Content-Type: text/plain; charset=UTF-8\r\n")
        writer.write("Content-Transfer-Encoding: 8bit\r\n\r\n")
        writer.write(bodyText + "\r\n\r\n")

        // Attachments (.pdf, .docx)
        for (file in attachments) {
            if (file.exists()) {
                val mimeType = if (file.name.endsWith(".docx", ignoreCase = true)) {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                } else {
                    "application/pdf"
                }
                val encodedBytes = Base64.encodeToString(file.readBytes(), Base64.DEFAULT)

                writer.write("--$boundary\r\n")
                writer.write("Content-Type: $mimeType; name=\"${file.name}\"\r\n")
                writer.write("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
                writer.write("Content-Transfer-Encoding: base64\r\n\r\n")
                writer.write(encodedBytes + "\r\n\r\n")
            }
        }

        writer.write("--$boundary--\r\n")
        sendCmd(".", "250")
        sendCmd("QUIT")

        socket.close()
    }

    private fun sendViaGmailOAuth2(
        recipient: String,
        subject: String,
        bodyText: String,
        attachments: List<File>
    ) {
        val boundary = "==_GmailBoundary_${System.currentTimeMillis()}_=="
        val rawMessage = StringBuilder()

        rawMessage.append("From: ${config.senderName} <${config.senderEmail}>\r\n")
        rawMessage.append("To: <$recipient>\r\n")
        rawMessage.append("Subject: =?UTF-8?B?${Base64.encodeToString(subject.toByteArray(), Base64.NO_WRAP)}?=\r\n")
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
