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

    private val BLOCKED_EMAIL_TOKENS = listOf(
        "example.com", "noreply", "no-reply", "donotreply", "do-not-reply",
        "sentry", "accommodation", "accessibility", "reasonable-",
        "privacy", "gdpr", "rgpd", "dpo@", "legal@", "compliance@",
        "abuse@", "security@", "postmaster@", "mailer-daemon", "webmaster@",
        "unsubscribe", "bounce", "notification@", "newsletter", "support@",
        "marketing@", "press@", "media@", "w3.org", "schema.org", "remotive.",
        "jobicy.", "himalayas.", "weworkremotely.", "arbeitnow.", "emploitic.",
        "tanqeeb.", "qatarliving.", "greenhouse.io", "ashbyhq.com", "lever.co",
        "workday.com", "cloudflare.com"
    )

    private val HIRING_EMAIL_TOKENS = listOf(
        "recrutement", "recrute", "recruteur", "recrut", "candidat", "candidature",
        "candidatures", "drh", "rh", "embauche", "emploi", "emplois", "carriere", "carrieres",
        "postuler", "recruit", "recruiting", "recruitment", "hiring", "hire",
        "talent", "talents", "career", "careers", "jobs", "job", "apply", "hr",
        "people", "staffing"
    )

    fun decodeCloudflareEmail(hex: String): String {
        if (hex.length < 4 || hex.length % 2 != 0) return ""
        return runCatching {
            val r = hex.substring(0, 2).toInt(16)
            val sb = StringBuilder()
            for (i in 2 until hex.length step 2) {
                val c = hex.substring(i, i + 2).toInt(16) xor r
                sb.append(c.toChar())
            }
            sb.toString().trim()
        }.getOrDefault("")
    }

    fun unmaskObfuscatedEmails(raw: String): String {
        return raw
            .replace("&#64;", "@")
            .replace("&commat;", "@")
            .replace(Regex("""(?i)\s*\[at\]\s*"""), "@")
            .replace(Regex("""(?i)\s*\(at\)\s*"""), "@")
            .replace(Regex("""(?i)\s*\[chez\]\s*"""), "@")
            .replace(Regex("""(?i)\s*\[dot\]\s*"""), ".")
            .replace(Regex("""(?i)\s*\(dot\)\s*"""), ".")
    }

    fun isValidRecruiterEmail(email: String): Boolean {
        val clean = email.trim().lowercase()
        if (clean.length < 6 || !clean.contains("@")) return false
        val domain = clean.substringAfter("@")
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) return false
        if (clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") ||
            clean.endsWith(".gif") || clean.endsWith(".svg") || clean.endsWith(".webp") ||
            clean.endsWith(".css") || clean.endsWith(".js")
        ) return false
        return BLOCKED_EMAIL_TOKENS.none { clean.contains(it) }
    }

    fun extractRecruiterEmail(text: String): String {
        val candidates = mutableListOf<String>()

        // 1. Cloudflare obfuscated emails: data-cfemail="6a090b..."
        val cfPattern = Regex("""data-cfemail="([0-9a-fA-F]{4,})"""")
        cfPattern.findAll(text).forEach { m ->
            val decoded = decodeCloudflareEmail(m.groupValues[1])
            if (isValidRecruiterEmail(decoded)) candidates.add(decoded)
        }

        // 2. Mailto URLs
        val mailtoPattern = Regex("""mailto:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6})""", RegexOption.IGNORE_CASE)
        mailtoPattern.findAll(text).forEach { m ->
            val e = m.groupValues[1].trim()
            if (isValidRecruiterEmail(e)) candidates.add(e)
        }

        // 3. Unmasked body text
        val unmasked = unmaskObfuscatedEmails(text)
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""")
        emailRegex.findAll(unmasked).forEach { m ->
            val e = m.value.trim()
            if (isValidRecruiterEmail(e)) candidates.add(e)
        }

        return candidates
            .filter { isValidRecruiterEmail(it) }
            .distinctBy { it.lowercase() }
            .maxByOrNull { email ->
                val lower = email.lowercase()
                when {
                    HIRING_EMAIL_TOKENS.any { lower.contains(it) } -> 100
                    lower.startsWith("info@") || lower.startsWith("contact@") || lower.startsWith("admin@") -> 50
                    else -> 30
                }
            } ?: ""
    }
}
