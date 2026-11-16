package com.ghdinteractivestudio.jobhunter.domain.document

import androidx.core.text.HtmlCompat

/**
 * Sanitizes HTML, double-encoded XML/RSS markup, CSS noise, UTF-8 mojibake,
 * and malformed entities so the Radar NEVER displays incomprehensible characters or markup.
 */
object HtmlSanitizer {

    fun cleanToReadableText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""

        var text = raw
            // Strip script, style, img, svg completely before HtmlCompat (prevents U+FFFC object boxes)
            .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?is)<(img|svg|iframe)[^>]*>"), " ")
            // Replace common HTML entities if double-encoded
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")

        // Second pass for entity-encoded script/style/img
        text = text
            .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?is)<(img|svg|iframe)[^>]*>"), " ")

        // Convert structured tags into clean line breaks and bullets
        text = text
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|tr|h[1-6]|section)>"), "\n")
            .replace(Regex("(?i)<li[^>]*>"), "\n• ")
            .replace(Regex("(?i)</li>"), "")

        // Decode HTML via Android HtmlCompat
        val decoded = runCatching {
            HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        }.getOrElse {
            text.replace(Regex("<[^>]+>"), " ")
        }

        // Repair common double-encoded UTF-8 mojibake (e.g. from RemoteOK feeds)
        val mojibakeFixed = decoded
            .replace("â€™", "'")
            .replace("â€œ", "\"")
            .replace("â€\u009d", "\"")
            .replace("â€“", "-")
            .replace("â€”", "-")
            .replace("â€¢", "•")
            .replace(Regex("ðŸ[\\u0080-\\u00FF]{1,3}"), "")
            .replace(Regex("Ã[\\u0080-\\u009F]"), "")

        // Remove residual tags, U+FFFC object replacement boxes, U+FFFD replacement chars
        return mojibakeFixed
            .replace(Regex("<[^>]+>"), " ")
            .replace("\uFFFC", " ")
            .replace("\uFFFD", "")
            .replace(Regex("[ \\t\\xA0]+"), " ")
            .replace(Regex("\\n[ \\t]+"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    fun extractRecruiterEmail(text: String): String {
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""")
        return emailRegex.findAll(text)
            .map { it.value }
            .firstOrNull { email ->
                val lower = email.lowercase()
                !lower.contains("example.com") &&
                    !lower.contains("noreply") &&
                    !lower.contains("no-reply") &&
                    !lower.contains("sentry") &&
                    !lower.endsWith(".png") &&
                    !lower.endsWith(".jpg")
            } ?: ""
    }
}
