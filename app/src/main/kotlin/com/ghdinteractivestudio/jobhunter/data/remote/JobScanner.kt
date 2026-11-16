package com.ghdinteractivestudio.jobhunter.data.remote

import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 100% REAL Live Job Scanner :
 * Fetches strictly real live offers from public job APIs (RemoteOK, WeWorkRemotely RSS).
 * NEVER produces fictitious or hardcoded fake jobs.
 */
class JobScanner {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Scans real live web sources for actual job opportunities matching user competencies.
     */
    suspend fun scanOffers(keywords: List<String>): List<JobOffer> = withContext(Dispatchers.IO) {
        val results = mutableListOf<JobOffer>()

        // 1. Fetch real live jobs from RemoteOK API
        runCatching {
            results.addAll(fetchRemoteOkJobs(keywords))
        }

        // 2. Fetch real live jobs from WeWorkRemotely RSS feed
        runCatching {
            results.addAll(fetchWeWorkRemotelyRss(keywords))
        }

        results
    }

    private fun fetchRemoteOkJobs(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://remoteok.com/api")
            .header("User-Agent", "JobHunterAI-Bot/1.0 (Android; 100% Real Job Radar)")
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val array = json.parseToJsonElement(body).jsonArray

            for (element in array.drop(1)) { // First item is legal metadata
                val obj = element.jsonObject
                val position = obj["position"]?.jsonPrimitive?.content ?: continue
                val company = obj["company"]?.jsonPrimitive?.content ?: "Entreprise"
                val description = obj["description"]?.jsonPrimitive?.content ?: ""
                val url = obj["url"]?.jsonPrimitive?.content ?: ""
                val location = obj["location"]?.jsonPrimitive?.content ?: "Remote Global"

                // Match keywords if user has skills, otherwise load all real open positions
                val matchesKeyword = keywords.isEmpty() || keywords.any { kw ->
                    position.contains(kw, ignoreCase = true) || description.contains(kw, ignoreCase = true)
                }

                if (matchesKeyword) {
                    val id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                    val formatReq = detectFormatFromText(description)

                    offers.add(
                        JobOffer(
                            id = "remoteok_$id",
                            title = position,
                            company = company,
                            location = location,
                            isRemote = true,
                            contractType = "Contrat / CDI",
                            description = stripHtml(description),
                            sourcePlatform = "RemoteOK",
                            url = url,
                            detectedAt = System.currentTimeMillis(),
                            requiredFormat = formatReq,
                            recruiterEmail = extractEmail(description)
                        )
                    )
                }
            }
        }
        return offers
    }

    private fun fetchWeWorkRemotelyRss(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://weworkremotely.com/categories/remote-programming-jobs.rss")
            .header("User-Agent", "JobHunterAI-Bot/1.0 (Android)")
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val xml = response.body?.string() ?: return emptyList()

            val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            val titleRegex = Regex("<title><!\\[CDATA\\[(.*?)\\]\\]></title>|<title>(.*?)</title>")
            val descRegex = Regex("<description><!\\[CDATA\\[(.*?)\\]\\]></description>|<description>(.*?)</description>", RegexOption.DOT_MATCHES_ALL)
            val linkRegex = Regex("<link>(.*?)</link>")

            for (itemMatch in itemRegex.findAll(xml)) {
                val itemContent = itemMatch.groupValues[1]
                val rawTitle = titleRegex.find(itemContent)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } } ?: continue
                val rawDesc = descRegex.find(itemContent)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } } ?: ""
                val link = linkRegex.find(itemContent)?.groupValues?.get(1) ?: ""

                // Format of WWR title is usually "Company: Position"
                val parts = rawTitle.split(":", limit = 2)
                val company = if (parts.size > 1) parts[0].trim() else "Tech Company"
                val position = if (parts.size > 1) parts[1].trim() else rawTitle.trim()

                val cleanDesc = stripHtml(rawDesc)
                val formatReq = detectFormatFromText(cleanDesc)

                val matches = keywords.isEmpty() || keywords.any { kw ->
                    position.contains(kw, ignoreCase = true) || cleanDesc.contains(kw, ignoreCase = true)
                }

                if (matches) {
                    offers.add(
                        JobOffer(
                            id = "wwr_${link.hashCode()}",
                            title = position,
                            company = company,
                            location = "Remote",
                            isRemote = true,
                            contractType = "Full-time / Contract",
                            description = cleanDesc,
                            sourcePlatform = "WeWorkRemotely",
                            url = link,
                            detectedAt = System.currentTimeMillis(),
                            requiredFormat = formatReq,
                            recruiterEmail = extractEmail(cleanDesc)
                        )
                    )
                }
            }
        }
        return offers
    }

    private fun detectFormatFromText(text: String): DocumentFormatRequirement {
        val lower = text.lowercase()
        return when {
            lower.contains("format word") || lower.contains(".docx") || lower.contains("format doc") -> DocumentFormatRequirement.DOCX
            lower.contains("format pdf") || lower.contains("au format pdf") -> DocumentFormatRequirement.PDF
            else -> DocumentFormatRequirement.ANY
        }
    }

    private fun extractEmail(text: String): String {
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""")
        return emailRegex.find(text)?.value ?: ""
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").trim()
}
