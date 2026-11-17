package com.ghdinteractivestudio.jobhunter.data.remote

import android.util.Log
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.document.HtmlSanitizer
import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

data class LiveOfferDetails(
    val recruiterEmail: String = "",
    val isExpired: Boolean = false,
    val deadlineAt: Long = 0L,
    val deadlineLabel: String = ""
)

/**
 * 100% REAL Multi-Source Live Job Scanner :
 * - National Algérie (Présentiel / Hybride / Remote DZ) via Emploitic DZ
 * - International (Worldwide Remote & Présentiel / Visa) via Arbeitnow, Remotive, Jobicy, Himalayas, RemoteOK, WeWorkRemotely
 * - Strict Deadline & Expiry enforcement via DeadlineParser (excludes expired/closed jobs automatically)
 * - All descriptions sanitized via HtmlSanitizer (ZERO unreadable HTML/markup)
 */
class JobScanner {

    companion object {
        private const val TAG = "JobScanner"
        private const val UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Scans live web boards for both Algeria (national/on-site) and International (worldwide/remote/on-site),
     * tailored to the user's real CV profile when loaded.
     */
    suspend fun scanOffers(profile: UserProfile? = null): List<JobOffer> = withContext(Dispatchers.IO) {
        val searchTerms = deriveSearchKeywords(profile)
        Log.d(TAG, "Starting real web job scan with profile terms: $searchTerms")

        val allOffers = mutableListOf<JobOffer>()

        // 1. NATIONAL ALGÉRIE — Emploitic DZ (Présentiel, Hybride, CDI/CDD en Algérie)
        val dzQueries = deriveAlgeriaQueries(profile)
        for (q in dzQueries) {
            runCatching {
                allOffers.addAll(fetchEmploiticDz(q))
            }.onFailure {
                Log.w(TAG, "Emploitic DZ ($q) error: ${it.message}")
            }
        }

        // 2. INTERNATIONAL ON-SITE & REMOTE (Europe / Visa / Global) — Arbeitnow API
        runCatching {
            allOffers.addAll(fetchArbeitnowJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Arbeitnow error: ${it.message}")
        }

        // 3. INTERNATIONAL REMOTE — Remotive API (HR, Admin, Operations, Education, Software, All)
        runCatching {
            allOffers.addAll(fetchRemotiveJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Remotive error: ${it.message}")
        }

        // 4. INTERNATIONAL REMOTE — Jobicy API
        runCatching {
            allOffers.addAll(fetchJobicyJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Jobicy error: ${it.message}")
        }

        // 5. INTERNATIONAL REMOTE — Himalayas API
        runCatching {
            allOffers.addAll(fetchHimalayasJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Himalayas error: ${it.message}")
        }

        // 6. INTERNATIONAL REMOTE — RemoteOK API & WeWorkRemotely RSS
        runCatching {
            allOffers.addAll(fetchRemoteOkJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "RemoteOK error: ${it.message}")
        }

        runCatching {
            allOffers.addAll(fetchWeWorkRemotelyRss(searchTerms))
        }.onFailure {
            Log.w(TAG, "WWR error: ${it.message}")
        }

        // Deduplicate, enforce strict active deadline & age compliance, and score deterministically
        val now = System.currentTimeMillis()
        val distinct = allOffers
            .filter {
                it.title.isNotBlank() &&
                    it.url.startsWith("http") &&
                    it.description.length >= 25 &&
                    !DeadlineParser.isExpiredOrStale(it, nowMillis = now)
            }
            .distinctBy { it.url.ifBlank { it.id } }
            .map { offer ->
                scoreWithCvDeterministically(offer, profile)
            }
            .sortedWith(compareByDescending<JobOffer> { it.matchScore }.thenByDescending { it.publishedAt }.thenByDescending { it.detectedAt })

        Log.d(TAG, "Scan complete. Total active non-expired real offers collected: ${distinct.size}")
        distinct
    }

    /**
     * Fetches the live offer detail page to verify it is NOT expired (404/410, validThrough, or "Offre expirée"),
     * parse any detail-only deadline, and extract any recruiter contact email.
     */
    suspend fun fetchOfferLiveDetails(url: String): LiveOfferDetails = withContext(Dispatchers.IO) {
        if (!url.startsWith("http")) return@withContext LiveOfferDetails()
        runCatching {
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 404 || resp.code == 410) {
                    return@use LiveOfferDetails(isExpired = true, deadlineLabel = "Expirée (HTTP ${resp.code})")
                }
                if (!resp.isSuccessful) return@use LiveOfferDetails()

                val html = resp.body?.string() ?: return@use LiveOfferDetails()

                // Check JSON-LD "validThrough"
                val validThroughRaw = Regex(""""validThrough"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)
                val validThroughMs = DeadlineParser.parseTimestamp(validThroughRaw)

                val cleanText = HtmlSanitizer.cleanToReadableText(html)
                val dl = DeadlineParser.extractDeadline(
                    cleanText = cleanText,
                    explicitExpiryMillis = validThroughMs
                )

                val mailto = Regex("""mailto:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6})""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)
                val email = mailto?.takeIf { it.isNotBlank() } ?: HtmlSanitizer.extractRecruiterEmail(cleanText)

                LiveOfferDetails(
                    recruiterEmail = email,
                    isExpired = dl.isExpired,
                    deadlineAt = dl.deadlineAt,
                    deadlineLabel = dl.deadlineLabel
                )
            }
        }.getOrDefault(LiveOfferDetails())
    }

    suspend fun fetchRecruiterEmailFromUrl(url: String): String =
        fetchOfferLiveDetails(url).recruiterEmail

    private fun deriveAlgeriaQueries(profile: UserProfile?): List<String> {
        if (profile == null || profile.rawCvContent.isBlank()) {
            return listOf("")
        }
        val rawLower = (profile.title + " " + profile.skills.joinToString(" ") + " " + profile.rawCvContent.take(1500)).lowercase()
        val queries = mutableListOf<String>()

        if (rawLower.contains("human resources") || rawLower.contains("ressources humaines") || rawLower.contains("rh") || rawLower.contains("payroll")) {
            queries.add("ressources humaines")
        }
        if (rawLower.contains("administrat")) {
            queries.add("administratif")
        }
        if (rawLower.contains("french") || rawLower.contains("français") || rawLower.contains("francais") || rawLower.contains("teacher") || rawLower.contains("enseignant")) {
            queries.add("enseignant")
        }
        if (rawLower.contains("develop") || rawLower.contains("kotlin") || rawLower.contains("python") || rawLower.contains("javascript")) {
            queries.add("developpeur")
        }
        if (rawLower.contains("coordinator") || rawLower.contains("coordinateur")) {
            queries.add("coordinateur")
        }

        // Always include the latest general feed from Algeria too
        queries.add("")
        return queries.distinct().take(4)
    }

    private fun deriveSearchKeywords(profile: UserProfile?): List<String> {
        if (profile == null || profile.rawCvContent.isBlank()) return emptyList()

        val tokens = mutableListOf<String>()
        tokens.addAll(profile.skills)
        tokens.addAll(profile.softSkills)

        // Extract meaningful role words from title
        profile.title.split("|", "/", ",", "-", " ").forEach { part ->
            val clean = part.trim()
            if (clean.length >= 3 && clean.lowercase() !in setOf("and", "the", "for", "with", "des", "les")) {
                tokens.add(clean)
            }
        }

        profile.experiences.forEach { exp ->
            exp.title.split(" ", "/", "-").forEach { part ->
                val clean = part.trim()
                if (clean.length >= 4) tokens.add(clean)
            }
        }

        return tokens.distinctBy { it.lowercase() }.take(25)
    }

    private fun fetchEmploiticDz(query: String): List<JobOffer> {
        val url = if (query.isBlank()) {
            "https://emploitic.com/offres-d-emploi"
        } else {
            "https://emploitic.com/offres-d-emploi?q=${URLEncoder.encode(query, "UTF-8")}"
        }

        val request = Request.Builder().url(url).header("User-Agent", UA).build()
        val offers = mutableListOf<JobOffer>()

        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val html = resp.body?.string() ?: return emptyList()

            // Extract exact job detail hrefs indexed by alias suffix
            val hrefRegex = Regex("""href="([^"]*/offres-d-emploi/[^"]+)"""")
            val hrefList = hrefRegex.findAll(html).map { it.groupValues[1] }.toList()

            val nextDataMatch = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                .find(html) ?: return emptyList()

            val root = json.parseToJsonElement(nextDataMatch.groupValues[1]).jsonObject
            val dataList = root["props"]?.jsonObject
                ?.get("pageProps")?.jsonObject
                ?.get("searchResult")?.jsonObject
                ?.get("data")?.jsonArray ?: return emptyList()

            for (el in dataList) {
                val obj = el.jsonObject
                val id = obj.str("id")
                val rawTitle = obj.str("title")
                val alias = obj.str("alias")
                if (rawTitle.isBlank() || alias.isBlank()) continue

                val title = HtmlSanitizer.cleanToReadableText(rawTitle)
                val cleanDesc = HtmlSanitizer.cleanToReadableText(obj.str("description"))
                if (cleanDesc.isBlank()) continue

                val companyName = obj["company"]?.takeIf { it !is JsonNull }?.jsonObject?.str("name")
                    ?.ifBlank { "Entreprise Confidentielle (Algérie)" }
                    ?: "Entreprise Confidentielle (Algérie)"

                val locations = obj["location"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonObject.str("label").takeIf(String::isNotBlank) }
                    ?: emptyList()
                val locationText = locations.firstOrNull() ?: "Algérie"

                val contracts = obj["contractType"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonObject.str("label").takeIf(String::isNotBlank) }
                    ?: emptyList()
                val contractStr = contracts.joinToString(" / ").ifBlank { "Contrat Algérie" }

                val rawWorkMode = obj.str("workMode").lowercase()
                val isRemote = rawWorkMode.contains("remote") || rawWorkMode.contains("télétravail")
                val workModeLabel = when {
                    isRemote -> "REMOTE"
                    rawWorkMode.contains("hybrid") -> "HYBRID"
                    else -> "ONSITE"
                }

                val professions = obj["profession"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonObject.str("label").takeIf(String::isNotBlank) }
                    ?: emptyList()
                val apiTags = obj["tags"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
                    ?: emptyList()

                val matchedHref = hrefList.firstOrNull { it.endsWith(alias) }
                val jobUrl = when {
                    matchedHref != null && matchedHref.startsWith("http") -> matchedHref
                    matchedHref != null -> "https://emploitic.com$matchedHref"
                    else -> "https://emploitic.com/offres-d-emploi?q=${URLEncoder.encode(title, "UTF-8")}"
                }

                val recruiterEmail = HtmlSanitizer.extractRecruiterEmail(cleanDesc)
                val rawPubAt = obj.str("publishedAt")
                val publishedAt = rawPubAt.take(10)

                val tags = buildList {
                    add("🇩🇿 Algérie")
                    add(when (workModeLabel) { "REMOTE" -> "Remote"; "HYBRID" -> "Hybride"; else -> "Présentiel" })
                    addAll(contracts.take(2))
                    addAll(professions.take(2))
                    addAll(apiTags.take(3))
                }.distinct()

                offers.add(
                    JobOffer(
                        id = "emploitic_$id",
                        title = title,
                        company = companyName,
                        location = locationText,
                        isRemote = isRemote,
                        contractType = contractStr,
                        description = cleanDesc,
                        sourcePlatform = "Emploitic Algérie",
                        url = jobUrl,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = recruiterEmail,
                        tags = tags,
                        regionScope = "ALGERIA",
                        workMode = workModeLabel,
                        applicationMethod = if (recruiterEmail.isNotBlank()) "EMAIL" else "WEBSITE",
                        publishedDate = publishedAt
                    ).withDeadlineCheck(rawPublished = rawPubAt)
                )
            }
        }
        return offers
    }

    private fun fetchArbeitnowJobs(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://www.arbeitnow.com/api/job-board-api?page=1")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: return emptyList()

            for (el in data.take(40)) {
                val obj = el.jsonObject
                val slug = obj.str("slug")
                val title = HtmlSanitizer.cleanToReadableText(obj.str("title"))
                val company = HtmlSanitizer.cleanToReadableText(obj.str("company_name")).ifBlank { "Company" }
                val cleanDesc = HtmlSanitizer.cleanToReadableText(obj.str("description"))
                val url = obj.str("url")
                val location = obj.str("location").ifBlank { "Europe / International" }
                val isRemote = obj["remote"]?.jsonPrimitive?.booleanOrNull ?: false

                val apiTags = obj["tags"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() } ?: emptyList()
                val jobTypes = obj["job_types"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() } ?: emptyList()

                if (title.isBlank() || cleanDesc.length < 30 || url.isBlank()) continue

                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)
                val tags = buildList {
                    add("🌍 International")
                    add(if (isRemote) "Remote" else "Présentiel")
                    addAll(jobTypes.take(2))
                    addAll(apiTags.take(3))
                }.distinct()

                val rawCreated = obj.str("created_at")
                offers.add(
                    JobOffer(
                        id = "arbeitnow_${slug.ifBlank { UUID.randomUUID().toString() }}",
                        title = title,
                        company = company,
                        location = location,
                        isRemote = isRemote,
                        contractType = jobTypes.firstOrNull() ?: "Full-time / Visa",
                        description = cleanDesc,
                        sourcePlatform = "Arbeitnow",
                        url = url,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = tags,
                        regionScope = "INTERNATIONAL",
                        workMode = if (isRemote) "REMOTE" else "ONSITE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE"
                    ).withDeadlineCheck(rawPublished = rawCreated)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 25)
    }

    private fun fetchRemotiveJobs(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://remotive.com/api/remote-jobs?limit=60")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val jobs = json.parseToJsonElement(body).jsonObject["jobs"]?.jsonArray ?: return emptyList()

            for (el in jobs) {
                val obj = el.jsonObject
                val id = obj.str("id")
                val title = HtmlSanitizer.cleanToReadableText(obj.str("title"))
                val company = HtmlSanitizer.cleanToReadableText(obj.str("company_name"))
                val cleanDesc = HtmlSanitizer.cleanToReadableText(obj.str("description"))
                val url = obj.str("url")
                val category = obj.str("category")
                val location = obj.str("candidate_required_location").ifBlank { "Worldwide Remote" }
                val salary = obj.str("salary")
                val rawPubDate = obj.str("publication_date")
                val pubDate = rawPubDate.take(10)
                val jobType = obj.str("job_type").replace("_", " ").ifBlank { "Full-time" }
                val apiTags = obj["tags"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() } ?: emptyList()

                if (title.isBlank() || cleanDesc.length < 30 || url.isBlank()) continue

                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)
                val tags = buildList {
                    add("🌍 Remote")
                    if (category.isNotBlank()) add(category)
                    add(jobType)
                    addAll(apiTags.take(3))
                }.distinct()

                offers.add(
                    JobOffer(
                        id = "remotive_$id",
                        title = title,
                        company = company,
                        location = location,
                        isRemote = true,
                        contractType = jobType,
                        description = cleanDesc,
                        sourcePlatform = "Remotive",
                        url = url,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = tags,
                        regionScope = "INTERNATIONAL",
                        workMode = "REMOTE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE",
                        salary = salary,
                        publishedDate = pubDate
                    ).withDeadlineCheck(rawPublished = rawPubDate)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 25)
    }

    private fun fetchJobicyJobs(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://jobicy.com/api/v2/remote-jobs?count=40")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val jobs = json.parseToJsonElement(body).jsonObject["jobs"]?.jsonArray ?: return emptyList()

            for (el in jobs) {
                val obj = el.jsonObject
                val id = obj.str("id")
                val title = HtmlSanitizer.cleanToReadableText(obj.str("jobTitle"))
                val company = HtmlSanitizer.cleanToReadableText(obj.str("companyName"))
                val rawDesc = obj.str("jobDescription").ifBlank { obj.str("jobExcerpt") }
                val cleanDesc = HtmlSanitizer.cleanToReadableText(rawDesc)
                val url = obj.str("url")
                val geo = obj.str("jobGeo").ifBlank { "Worldwide" }
                val rawPubDate = obj.str("pubDate")
                val pubDate = rawPubDate.take(10)
                val industries = obj["jobIndustry"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val jobTypes = obj["jobType"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                if (title.isBlank() || cleanDesc.length < 25 || url.isBlank()) continue

                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)
                val tags = buildList {
                    add("🌍 Remote")
                    addAll(jobTypes.take(2))
                    addAll(industries.take(2))
                }.distinct()

                offers.add(
                    JobOffer(
                        id = "jobicy_$id",
                        title = title,
                        company = company,
                        location = geo,
                        isRemote = true,
                        contractType = jobTypes.firstOrNull() ?: "Full-Time",
                        description = cleanDesc,
                        sourcePlatform = "Jobicy",
                        url = url,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = tags,
                        regionScope = "INTERNATIONAL",
                        workMode = "REMOTE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE",
                        publishedDate = pubDate
                    ).withDeadlineCheck(rawPublished = rawPubDate)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 20)
    }

    private fun fetchHimalayasJobs(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://himalayas.app/jobs/api?limit=35")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val jobs = json.parseToJsonElement(body).jsonObject["jobs"]?.jsonArray ?: return emptyList()

            for (el in jobs) {
                val obj = el.jsonObject
                val title = HtmlSanitizer.cleanToReadableText(obj.str("title"))
                val company = HtmlSanitizer.cleanToReadableText(obj.str("companyName"))
                val rawDesc = obj.str("description").ifBlank { obj.str("excerpt") }
                val cleanDesc = HtmlSanitizer.cleanToReadableText(rawDesc)
                val url = obj.str("applicationLink").ifBlank { obj.str("guid") }
                val empType = obj.str("employmentType").ifBlank { "Full Time" }
                val locations = obj["locationRestrictions"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val cats = obj["parentCategories"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val seniorities = obj["seniority"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                if (title.isBlank() || cleanDesc.length < 25 || url.isBlank()) continue

                val locationStr = if (locations.isEmpty()) "Worldwide Remote" else locations.take(3).joinToString(", ")
                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)
                val tags = buildList {
                    add("🌍 International")
                    add("Remote")
                    add(empType)
                    addAll(cats.take(2))
                    addAll(seniorities.take(1))
                }.distinct()

                val rawPubDate = obj.str("pubDate")
                val expiryMs = DeadlineParser.parseTimestamp(obj.str("expiryDate"))

                offers.add(
                    JobOffer(
                        id = "himalayas_${url.hashCode()}",
                        title = title,
                        company = company,
                        location = locationStr,
                        isRemote = true,
                        contractType = empType,
                        description = cleanDesc,
                        sourcePlatform = "Himalayas",
                        url = url,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = tags,
                        regionScope = "INTERNATIONAL",
                        workMode = "REMOTE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE"
                    ).withDeadlineCheck(rawPublished = rawPubDate, explicitExpiryMillis = expiryMs)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 20)
    }

    private fun fetchRemoteOkJobs(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://remoteok.com/api")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val array = json.parseToJsonElement(body).jsonArray

            for (element in array.drop(1).take(40)) {
                val obj = element.jsonObject
                val position = HtmlSanitizer.cleanToReadableText(obj.str("position"))
                val company = HtmlSanitizer.cleanToReadableText(obj.str("company")).ifBlank { "Entreprise" }
                val cleanDesc = HtmlSanitizer.cleanToReadableText(obj.str("description"))
                val url = obj.str("url")
                val location = obj.str("location").ifBlank { "Remote Global" }
                val apiTags = obj["tags"]?.takeIf { it is JsonArray }?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() } ?: emptyList()

                if (position.isBlank() || cleanDesc.length < 30 || url.isBlank()) continue

                val id = obj.str("id").ifBlank { UUID.randomUUID().toString() }
                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)
                val rawDate = obj.str("date").ifBlank { obj.str("epoch") }

                offers.add(
                    JobOffer(
                        id = "remoteok_$id",
                        title = position,
                        company = company,
                        location = location,
                        isRemote = true,
                        contractType = "Remote Contract",
                        description = cleanDesc,
                        sourcePlatform = "RemoteOK",
                        url = url,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = (listOf("🌍 Remote") + apiTags.take(4)).distinct(),
                        regionScope = "INTERNATIONAL",
                        workMode = "REMOTE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE",
                        publishedDate = obj.str("date").take(10)
                    ).withDeadlineCheck(rawPublished = rawDate)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 15)
    }

    private fun fetchWeWorkRemotelyRss(keywords: List<String>): List<JobOffer> {
        val request = Request.Builder()
            .url("https://weworkremotely.com/remote-jobs.rss")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val xml = response.body?.string() ?: return emptyList()

            val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            val titleRegex = Regex("<title><!\\[CDATA\\[(.*?)\\]\\]></title>|<title>(.*?)</title>")
            val descRegex = Regex("<description><!\\[CDATA\\[(.*?)\\]\\]></description>|<description>(.*?)</description>", RegexOption.DOT_MATCHES_ALL)
            val linkRegex = Regex("<link>(.*?)</link>")
            val catRegex = Regex("<category>(.*?)</category>")
            val regionRegex = Regex("<region>(.*?)</region>")
            val pubRegex = Regex("<pubDate>(.*?)</pubDate>")

            for (itemMatch in itemRegex.findAll(xml).take(35)) {
                val itemContent = itemMatch.groupValues[1]
                val rawTitle = titleRegex.find(itemContent)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } } ?: continue
                val rawDesc = descRegex.find(itemContent)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } } ?: ""
                val link = linkRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
                val category = catRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
                val region = regionRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: "Anywhere"
                val rawPubDate = pubRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""

                val parts = HtmlSanitizer.cleanToReadableText(rawTitle).split(":", limit = 2)
                val company = if (parts.size > 1) parts[0].trim() else "Remote Company"
                val position = if (parts.size > 1) parts[1].trim() else rawTitle.trim()

                val cleanDesc = HtmlSanitizer.cleanToReadableText(rawDesc)
                if (position.isBlank() || cleanDesc.length < 25 || link.isBlank()) continue

                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)

                offers.add(
                    JobOffer(
                        id = "wwr_${link.hashCode()}",
                        title = position,
                        company = company,
                        location = region,
                        isRemote = true,
                        contractType = "Full-time / Remote",
                        description = cleanDesc,
                        sourcePlatform = "WeWorkRemotely",
                        url = link,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = listOfNotNull("🌍 Remote", category.ifBlank { null }).distinct(),
                        regionScope = "INTERNATIONAL",
                        workMode = "REMOTE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE"
                    ).withDeadlineCheck(rawPublished = rawPubDate)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 15)
    }

    private fun filterAndPrioritize(
        offers: List<JobOffer>,
        keywords: List<String>,
        limit: Int
    ): List<JobOffer> {
        if (keywords.isEmpty()) return offers.take(limit)
        val scored = offers.map { offer ->
            val corpus = "${offer.title} ${offer.description} ${offer.tags.joinToString(" ")}".lowercase()
            val hits = keywords.count { kw -> corpus.contains(kw.lowercase()) }
            offer to hits
        }
        // Keep matching offers first; if few match strictly, still return latest genuine live jobs
        val matched = scored.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        return if (matched.size >= 5) matched.take(limit) else (matched + scored.filter { it.second == 0 }.map { it.first }).take(limit)
    }

    /**
     * Computes deterministic honest match pre-score against the real CV profile,
     * including cross-language FR/EN equivalences for Algeria + Worldwide matching.
     * NEVER invents matches; only lists competencies actually present in both CV and offer.
     */
    private fun scoreWithCvDeterministically(offer: JobOffer, profile: UserProfile?): JobOffer {
        if (profile == null || profile.rawCvContent.isBlank()) return offer

        val jobCorpus = "${offer.title} ${offer.description} ${offer.tags.joinToString(" ")}".lowercase()
        val cvCorpus = "${profile.title} ${profile.summary} ${profile.skills.joinToString(" ")} ${profile.softSkills.joinToString(" ")}".lowercase()

        val realSkills = (profile.skills + profile.softSkills).distinct()
        val matchedSkills = mutableListOf<String>()

        for (sk in realSkills) {
            if (sk.length >= 2 && jobCorpus.contains(sk.lowercase())) {
                matchedSkills.add(sk)
            }
        }

        // Cross-language EN <-> FR equivalents strictly triggered only when BOTH the CV & the job offer share the concept
        val bilingualPairs = listOf(
            listOf("human resources", "ressources humaines", "gestionnaire rh", "responsable rh", "rrh", "drh") to "Ressources Humaines",
            listOf("administrat") to "Administration",
            listOf("recruitment", "recrutement", "talent acquisition") to "Recrutement",
            listOf("payroll", "paie", "salaire") to "Paie & Rémunération",
            listOf("onboarding", "intégration") to "Onboarding / Intégration",
            listOf("training", "formation", "didactique") to "Formation",
            listOf("coordinator", "coordinateur", "coordination") to "Coordination",
            listOf("teacher", "enseignant", "professeur") to "Enseignement",
            listOf("french", "français", "francais") to "Français",
            listOf("office", "excel", "erp", "bureautique") to "Bureautique / ERP"
        )
        for ((terms, label) in bilingualPairs) {
            val cvHas = terms.any { cvCorpus.contains(it) }
            val jobHas = terms.any { jobCorpus.contains(it) }
            if (cvHas && jobHas && label !in matchedSkills) {
                matchedSkills.add(label)
            }
        }

        // Check domain/title word overlap
        val titleWords = profile.title.split("|", "/", ",", "-", " ")
            .map { it.trim().lowercase() }
            .filter { it.length >= 4 && it !in setOf("with", "and", "pour", "avec") }
            .distinct()
        val titleMatches = titleWords.filter { jobCorpus.contains(it) }

        val combinedMatches = (matchedSkills + titleMatches.map { it.replaceFirstChar(Char::uppercase) }).distinct()
        if (combinedMatches.isEmpty()) {
            return offer.copy(
                matchScore = if (offer.matchScore > 0) offer.matchScore else 15,
                aiVerdict = if (offer.aiVerdict.isBlank()) "NON_ADAPTE" else offer.aiVerdict
            )
        }

        val bonusTitleBoost = if (bilingualPairs.any { (terms, _) ->
                terms.any { profile.title.lowercase().contains(it) } && terms.any { offer.title.lowercase().contains(it) }
            }) 18f else 0f

        val skillRatio = if (realSkills.isNotEmpty()) (matchedSkills.size.toFloat() / 6f).coerceAtMost(1f) * 58f else 22f
        val titleBonus = (titleMatches.size.coerceAtMost(3) * 10f) + bonusTitleBoost
        val computedScore = (skillRatio + titleBonus + 18f).toInt().coerceIn(25, 96)

        val verdict = when {
            computedScore >= 60 -> "CONVIENT"
            computedScore >= 40 -> "MOYEN"
            else -> "NON_ADAPTE"
        }

        return offer.copy(
            matchScore = maxOf(offer.matchScore, computedScore),
            matchingHighlights = if (offer.matchingHighlights.isEmpty()) combinedMatches.take(6) else offer.matchingHighlights,
            aiVerdict = if (offer.aiVerdict.isBlank()) verdict else offer.aiVerdict
        )
    }

    private fun detectFormatFromText(text: String): DocumentFormatRequirement {
        val lower = text.lowercase()
        return when {
            lower.contains("format word") || lower.contains(".docx") || lower.contains("format doc") -> DocumentFormatRequirement.DOCX
            lower.contains("format pdf") || lower.contains("au format pdf") || lower.contains("pdf format") -> DocumentFormatRequirement.PDF
            else -> DocumentFormatRequirement.ANY
        }
    }

    private fun JobOffer.withDeadlineCheck(
        rawPublished: String = "",
        explicitExpiryMillis: Long = 0L
    ): JobOffer {
        val pubMs = DeadlineParser.parseTimestamp(rawPublished)
        val dl = DeadlineParser.extractDeadline(
            cleanText = description,
            publishedAtMillis = pubMs,
            explicitExpiryMillis = explicitExpiryMillis
        )
        val finalPubDate = publishedDate.ifBlank {
            if (pubMs > 0L) {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date(pubMs))
            } else ""
        }
        return copy(
            publishedAt = pubMs,
            publishedDate = finalPubDate,
            deadlineAt = dl.deadlineAt,
            deadlineLabel = dl.deadlineLabel
        )
    }

    private fun JsonObject.str(key: String): String {
        val el: JsonElement = this[key] ?: return ""
        if (el is JsonNull) return ""
        return runCatching { el.jsonPrimitive.contentOrNull ?: "" }.getOrDefault("")
    }
}
