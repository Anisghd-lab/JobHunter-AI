package com.ghdinteractivestudio.jobhunter.domain.document

import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DeadlineParseResult(
    val deadlineAt: Long = 0L, // Epoch millis (end of deadline day, or 0L if none detected)
    val deadlineLabel: String = "",
    val isExpired: Boolean = false
)

object DeadlineParser {

    private const val ONE_DAY_MS = 86_400_000L
    const val DEFAULT_MAX_OFFER_AGE_DAYS = 45L

    private val frMonths = mapOf(
        "janvier" to 0, "janv" to 0,
        "février" to 1, "fevrier" to 1, "févr" to 1, "fevr" to 1,
        "mars" to 2,
        "avril" to 3, "avr" to 3,
        "mai" to 4,
        "juin" to 5,
        "juillet" to 6, "juil" to 6,
        "août" to 7, "aout" to 7,
        "septembre" to 8, "sept" to 8, "sep" to 8,
        "octobre" to 9, "oct" to 9,
        "novembre" to 10, "nov" to 10,
        "décembre" to 11, "decembre" to 11, "déc" to 11, "dec" to 11
    )

    private val enMonths = mapOf(
        "january" to 0, "jan" to 0,
        "february" to 1, "feb" to 1,
        "march" to 2, "mar" to 2,
        "april" to 3, "apr" to 3,
        "may" to 4,
        "june" to 5, "jun" to 5,
        "july" to 6, "jul" to 6,
        "august" to 7, "aug" to 7,
        "september" to 8, "sept" to 8, "sep" to 8,
        "october" to 9, "oct" to 9,
        "november" to 10, "nov" to 10,
        "december" to 11, "dec" to 11
    )

    /**
     * Parses API date strings (ISO-8601, yyyy-MM-dd, RFC-2822) into epoch millis.
     */
    fun parseTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val trimmed = raw.trim()

        // Numeric unix timestamp check (seconds vs millis)
        if (trimmed.matches(Regex("^\\d{10,13}$"))) {
            val v = trimmed.toLongOrNull() ?: return 0L
            return if (v < 10_000_000_000L) v * 1000L else v
        }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to Locale.US,
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to Locale.US,
            "yyyy-MM-dd'T'HH:mm:ssXXX" to Locale.US,
            "yyyy-MM-dd'T'HH:mm:ss" to Locale.US,
            "EEE, dd MMM yyyy HH:mm:ss Z" to Locale.US,
            "EEE, dd MMM yyyy HH:mm:ss z" to Locale.US,
            "yyyy-MM-dd" to Locale.US
        )

        for ((pat, loc) in patterns) {
            runCatching {
                val sdf = SimpleDateFormat(pat, loc).apply {
                    if (pat.endsWith("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(trimmed)
                if (date != null) return date.time
            }
        }
        return 0L
    }

    /**
     * Detects explicit application deadlines from API expiry timestamps, JSON-LD validThrough,
     * or French/English deadline phrases inside job posting text.
     */
    fun extractDeadline(
        cleanText: String,
        publishedAtMillis: Long = 0L,
        explicitExpiryMillis: Long = 0L,
        nowMillis: Long = System.currentTimeMillis()
    ): DeadlineParseResult {
        val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // 0. Check explicit API expiry timestamp first (e.g. Himalayas expiryDate or validThrough)
        if (explicitExpiryMillis > 0L) {
            val expired = explicitExpiryMillis < nowMillis
            return DeadlineParseResult(
                deadlineAt = explicitExpiryMillis,
                deadlineLabel = displaySdf.format(Date(explicitExpiryMillis)),
                isExpired = expired
            )
        }

        val lower = cleanText.lowercase()

        // Quick literal expired markers published on web listings
        if (lower.contains("offre expirée") ||
            lower.contains("offre clôturée") ||
            lower.contains("candidatures closes") ||
            lower.contains("no longer accepting applications") ||
            lower.contains("this job has expired")
        ) {
            return DeadlineParseResult(
                deadlineAt = nowMillis - ONE_DAY_MS,
                deadlineLabel = "Expirée",
                isExpired = true
            )
        }

        // 1. Numeric date paired with explicit deadline phrase:
        // e.g. "Date limite : 30/11/2026", "avant le 15-11-2026", "deadline: 2026-11-30"
        val numericPattern = Regex(
            """(?:date\s+limite(?:\s+de\s+(?:dépôt|candidature|réponse))?|date\s+butoir|date\s+de\s+clôture|clôture\s+des\s+candidatures|au\s+plus\s+tard\s+le|avant\s+le|valable\s+jusqu['’]au|jusqu['’]au|application\s+deadline|closing\s+date|apply\s+(?:before|by)|valid\s+(?:until|through)|expires?\s+(?:on)?|deadline)\s*:?\s*(?:le\s+)?(\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4})""",
            RegexOption.IGNORE_CASE
        )
        numericPattern.find(cleanText)?.let { m ->
            val dateStr = m.groupValues[1]
            val parsed = parseNumericDateEndOfDay(dateStr)
            if (parsed > 0L) {
                return DeadlineParseResult(
                    deadlineAt = parsed,
                    deadlineLabel = displaySdf.format(Date(parsed)),
                    isExpired = parsed < nowMillis
                )
            }
        }

        // 2. Natural French month phrase: "avant le 25 novembre 2026", "date limite : 12 décembre 2026"
        val frTextPattern = Regex(
            """(?:date\s+limite(?:\s+de\s+(?:dépôt|candidature))?|date\s+butoir|clôture|au\s+plus\s+tard\s+le|avant\s+le|jusqu['’]au)\s*:?\s*(?:le\s+)?(\d{1,2})\s+([a-zA-Zéèêûùôîàç]+)\s+(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        frTextPattern.find(cleanText)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            val monthName = m.groupValues[2].lowercase()
            val year = m.groupValues[3].toIntOrNull()
            val monthIdx = frMonths[monthName]
            if (day != null && year != null && monthIdx != null) {
                val ts = toEndOfDayMillis(year, monthIdx, day)
                return DeadlineParseResult(
                    deadlineAt = ts,
                    deadlineLabel = displaySdf.format(Date(ts)),
                    isExpired = ts < nowMillis
                )
            }
        }

        // 3. Natural English month phrase: "Deadline: November 25, 2026" or "before 25 Nov 2026"
        val enTextPattern1 = Regex(
            """(?:application\s+deadline|closing\s+date|apply\s+(?:before|by)|valid\s+(?:until|through)|expires?\s+(?:on)?|deadline)\s*:?\s*([a-zA-Z]+)\s+(\d{1,2}),?\s+(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        enTextPattern1.find(cleanText)?.let { m ->
            val monthIdx = enMonths[m.groupValues[1].lowercase()]
            val day = m.groupValues[2].toIntOrNull()
            val year = m.groupValues[3].toIntOrNull()
            if (monthIdx != null && day != null && year != null) {
                val ts = toEndOfDayMillis(year, monthIdx, day)
                return DeadlineParseResult(
                    deadlineAt = ts,
                    deadlineLabel = displaySdf.format(Date(ts)),
                    isExpired = ts < nowMillis
                )
            }
        }

        val enTextPattern2 = Regex(
            """(?:application\s+deadline|closing\s+date|apply\s+(?:before|by)|valid\s+(?:until|through)|expires?\s+(?:on)?|deadline)\s*:?\s*(\d{1,2})\s+([a-zA-Z]+)\s+(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        enTextPattern2.find(cleanText)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            val monthIdx = enMonths[m.groupValues[2].lowercase()]
            val year = m.groupValues[3].toIntOrNull()
            if (day != null && monthIdx != null && year != null) {
                val ts = toEndOfDayMillis(year, monthIdx, day)
                return DeadlineParseResult(
                    deadlineAt = ts,
                    deadlineLabel = displaySdf.format(Date(ts)),
                    isExpired = ts < nowMillis
                )
            }
        }

        // 4. Relative duration from publication date: "valable 15 jours", "open for 10 days"
        if (publishedAtMillis > 0L) {
            val relPattern = Regex(
                """(?:offre\s+valable|délai\s+de\s+candidature|candidater\s+sous|open\s+for|valid\s+for)\s*:?\s*(\d{1,2})\s+(?:jours?|days?)""",
                RegexOption.IGNORE_CASE
            )
            relPattern.find(cleanText)?.let { m ->
                val days = m.groupValues[1].toLongOrNull()
                if (days != null && days in 1..90) {
                    val ts = publishedAtMillis + (days * ONE_DAY_MS)
                    return DeadlineParseResult(
                        deadlineAt = ts,
                        deadlineLabel = displaySdf.format(Date(ts)),
                        isExpired = ts < nowMillis
                    )
                }
            }
        }

        return DeadlineParseResult()
    }

    /**
     * Returns true if offer has passed an explicit deadline OR is staler than maxAgeDays without active extension.
     */
    fun isExpiredOrStale(
        offer: JobOffer,
        nowMillis: Long = System.currentTimeMillis(),
        maxAgeDays: Long = DEFAULT_MAX_OFFER_AGE_DAYS
    ): Boolean {
        if (offer.deadlineAt > 0L && offer.deadlineAt < nowMillis) {
            return true
        }
        if (offer.publishedAt > 0L && (nowMillis - offer.publishedAt) > (maxAgeDays * ONE_DAY_MS)) {
            return true
        }
        return false
    }

    fun formatDaysRemaining(deadlineAt: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (deadlineAt <= 0L) return ""
        val diff = deadlineAt - nowMillis
        if (diff < 0L) return "Expirée"
        val days = (diff / ONE_DAY_MS).toInt()
        return when {
            days == 0 -> "Expire aujourd'hui !"
            days == 1 -> "Expire demain"
            days <= 7 -> "Dans $days jours"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                "Limite : ${sdf.format(Date(deadlineAt))}"
            }
        }
    }

    private fun parseNumericDateEndOfDay(dateStr: String): Long {
        val parts = dateStr.split('-', '/', '.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return 0L
        val (year, month, day) = if (parts[0] > 1900) {
            Triple(parts[0], parts[1], parts[2])
        } else {
            val y = if (parts[2] < 100) 2000 + parts[2] else parts[2]
            Triple(y, parts[1], parts[0])
        }
        if (month !in 1..12 || day !in 1..31 || year !in 2020..2035) return 0L
        return toEndOfDayMillis(year, month - 1, day)
    }

    private fun toEndOfDayMillis(year: Int, monthZeroBased: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
