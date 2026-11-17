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
 * - Golfe / GCC (UAE, Arabie Saoudite, Qatar, Koweït, Oman, Bahreïn) via Tanqeeb GCC, QatarLiving, Careem, Tamara & LeanTech
 * - International (Worldwide Remote & Présentiel / Visa) via Arbeitnow, Remotive, Jobicy, Himalayas, RemoteOK, WeWorkRemotely
 * - Searches using BOTH uploaded CVs (English + French) for maximum recall
 * - Strict Deadline & Expiry enforcement via DeadlineParser (excludes expired/closed jobs automatically)
 * - All descriptions sanitized via HtmlSanitizer (ZERO unreadable HTML/markup)
 */
class JobScanner {

    companion object {
        private const val TAG = "JobScanner"
        private const val UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        private val GULF_LOC_KEYWORDS = listOf(
            "dubai", "abu dhabi", "sharjah", "ajman", "uae", "united arab emirates", "émirats", "emirates",
            "saudi", "riyadh", "jeddah", "dammam", "khobar", "neom", "ksa", "arabie saoudite",
            "qatar", "doha", "al rayyan", "lusail",
            "kuwait", "koweit", "koweït",
            "oman", "muscat", "mascate",
            "bahrain", "bahrein", "bahreïn", "manama",
            "gcc", "gulf"
        )
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Scans live web boards for Algeria (national), Gulf/GCC (UAE, KSA, Qatar, Kuwait, Oman, Bahrain),
     * and International (worldwide/remote/on-site), leveraging BOTH English & French CV profiles when loaded.
     */
    suspend fun scanOffers(profile: UserProfile? = null): List<JobOffer> = withContext(Dispatchers.IO) {
        val searchTerms = deriveSearchKeywords(profile)
        Log.d(TAG, "Starting real web job scan with dual EN/FR profile terms: $searchTerms")

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

        // 2. GOLFE / GCC (UAE, Arabie Saoudite, Qatar, Koweït, Oman, Bahreïn)
        runCatching {
            allOffers.addAll(fetchTanqeebGccJobs(profile, searchTerms))
        }.onFailure {
            Log.w(TAG, "Tanqeeb GCC error: ${it.message}")
        }

        runCatching {
            allOffers.addAll(fetchQatarLivingJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "QatarLiving error: ${it.message}")
        }

        runCatching {
            allOffers.addAll(fetchGulfGreenhouseBoard("Careem", "careem", searchTerms))
        }.onFailure {
            Log.w(TAG, "Careem Gulf error: ${it.message}")
        }

        runCatching {
            allOffers.addAll(fetchGulfGreenhouseBoard("Tamara", "tamara", searchTerms))
        }.onFailure {
            Log.w(TAG, "Tamara Gulf error: ${it.message}")
        }

        runCatching {
            allOffers.addAll(fetchGulfAshbyBoard("Lean Technologies", "leantech", searchTerms))
        }.onFailure {
            Log.w(TAG, "LeanTech Gulf error: ${it.message}")
        }

        // 3. INTERNATIONAL ON-SITE & REMOTE (Europe / Visa / Global) — Arbeitnow API
        runCatching {
            allOffers.addAll(fetchArbeitnowJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Arbeitnow error: ${it.message}")
        }

        // 4. INTERNATIONAL REMOTE — Remotive API (HR, Admin, Operations, Education, Software, All)
        runCatching {
            allOffers.addAll(fetchRemotiveJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Remotive error: ${it.message}")
        }

        // 5. INTERNATIONAL REMOTE — Jobicy API
        runCatching {
            allOffers.addAll(fetchJobicyJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Jobicy error: ${it.message}")
        }

        // 6. INTERNATIONAL REMOTE — Himalayas API
        runCatching {
            allOffers.addAll(fetchHimalayasJobs(searchTerms))
        }.onFailure {
            Log.w(TAG, "Himalayas error: ${it.message}")
        }

        // 7. INTERNATIONAL REMOTE — RemoteOK API & WeWorkRemotely RSS
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

        // Deduplicate, enforce strict active deadline & age compliance, normalize Gulf scope, and score deterministically
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
                val normalizedScope = if (offer.regionScope == "INTERNATIONAL" && isGulfLocation("${offer.location} ${offer.title}")) {
                    offer.copy(regionScope = "GULF", tags = (listOf("🏜️ Golfe") + offer.tags).distinct())
                } else offer
                scoreWithCvDeterministically(normalizedScope, profile)
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
        if (profile == null || profile.combinedSearchCorpus().isBlank()) {
            return listOf("")
        }
        val rawLower = profile.combinedSearchCorpus().take(4500).lowercase()
        val queries = mutableListOf<String>()

        if (rawLower.contains("human resources") || rawLower.contains("ressources humaines") || rawLower.contains("rh") || rawLower.contains("payroll") || rawLower.contains("paie")) {
            queries.add("ressources humaines")
        }
        if (rawLower.contains("administrat")) {
            queries.add("administratif")
        }
        if (rawLower.contains("french") || rawLower.contains("français") || rawLower.contains("francais") || rawLower.contains("teacher") || rawLower.contains("enseignant")) {
            queries.add("enseignant")
        }
        if (rawLower.contains("develop") || rawLower.contains("développ") || rawLower.contains("kotlin") || rawLower.contains("python") || rawLower.contains("javascript")) {
            queries.add("developpeur")
        }
        if (rawLower.contains("coordinator") || rawLower.contains("coordinateur")) {
            queries.add("coordinateur")
        }

        // Always include the latest general feed from Algeria too
        queries.add("")
        return queries.distinct().take(4)
    }

    private fun deriveGulfQueries(profile: UserProfile?): List<String> {
        if (profile == null || profile.combinedSearchCorpus().isBlank()) return emptyList()
        val rawLower = profile.combinedSearchCorpus().take(4500).lowercase()
        val queries = mutableListOf<String>()
        if (rawLower.contains("human resources") || rawLower.contains("ressources humaines") || rawLower.contains("payroll") || rawLower.contains("paie")) {
            queries.add("human resources")
        }
        if (rawLower.contains("administrat") || rawLower.contains("coordinator") || rawLower.contains("coordinateur")) {
            queries.add("administrator")
        }
        if (rawLower.contains("teacher") || rawLower.contains("enseignant") || rawLower.contains("french") || rawLower.contains("français")) {
            queries.add("teacher")
        }
        if (rawLower.contains("develop") || rawLower.contains("software") || rawLower.contains("python") || rawLower.contains("flutter")) {
            queries.add("developer")
        }
        return queries.distinct().take(2)
    }

    private fun deriveSearchKeywords(profile: UserProfile?): List<String> {
        if (profile == null || profile.combinedSearchCorpus().isBlank()) return emptyList()

        val tokens = mutableListOf<String>()
        tokens.addAll(profile.skills)
        tokens.addAll(profile.softSkills)

        // Extract meaningful role words from title + both EN & FR uploaded CVs
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

        // Extract top domain words from both CV texts for dual FR/EN matching
        listOfNotNull(profile.englishCv?.rawText, profile.frenchCv?.rawText).forEach { raw ->
            Regex("""[a-zA-ZÀ-ÿ]{5,}""").findAll(raw.take(1600))
                .map { it.value.trim() }
                .filter {
                    it.lowercase() in setOf(
                        "payroll", "recruitment", "onboarding", "coordinator", "administrator",
                        "resources", "reporting", "compliance", "french", "english", "teacher",
                        "recrutement", "gestionnaire", "administratif", "ressources", "humaines",
                        "comptable", "logistique", "developer", "flutter", "python", "software"
                    )
                }
                .forEach { tokens.add(it) }
        }

        return tokens.distinctBy { it.lowercase() }.take(32)
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

    /**
     * GULF / GCC SCRAPER #1 : Tanqeeb GCC (UAE, Saudi Arabia, Qatar, Kuwait, Oman, Bahrain)
     * Uses verified data-job-* HTML card metadata + Schema.org JobPosting JSON-LD detail pages.
     */
    private fun fetchTanqeebGccJobs(profile: UserProfile?, keywords: List<String>): List<JobOffer> {
        val countryEndpoints = buildList {
            add(Triple("🇦🇪 Émirats Arabes Unis", "https://uae.tanqeeb.com/jobs/search?order_by=most_recent", "UAE"))
            add(Triple("🇸🇦 Arabie Saoudite", "https://saudi.tanqeeb.com/jobs/search?order_by=most_recent", "Saudi Arabia"))
            add(Triple("🇶🇦 Qatar", "https://qatar.tanqeeb.com/jobs/search?order_by=most_recent", "Qatar"))
            add(Triple("🇰🇼 Koweït", "https://kuwait.tanqeeb.com/jobs/search?order_by=most_recent", "Kuwait"))
            add(Triple("🇴🇲 Oman", "https://oman.tanqeeb.com/jobs/search?order_by=most_recent", "Oman"))
            add(Triple("🇧🇭 Bahreïn", "https://bahrain.tanqeeb.com/jobs/search?order_by=most_recent", "Bahrain"))
            // Targeted keyword queries in UAE & Saudi Arabia matching the candidate's CVs
            deriveGulfQueries(profile).forEach { q ->
                val enc = URLEncoder.encode(q, "UTF-8")
                add(Triple("🇦🇪 Émirats Arabes Unis", "https://uae.tanqeeb.com/jobs/search?keywords=$enc&order_by=most_recent", "UAE"))
                add(Triple("🇸🇦 Arabie Saoudite", "https://saudi.tanqeeb.com/jobs/search?keywords=$enc&order_by=most_recent", "Saudi Arabia"))
            }
        }

        val offers = mutableListOf<JobOffer>()
        val attrPattern = Regex("""(data-job-[a-z-]+)\s*=\s*"([^"]*)"""")

        for ((flagCountry, searchUrl, defaultLoc) in countryEndpoints) {
            runCatching {
                val req = Request.Builder().url(searchUrl).header("User-Agent", UA).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val html = resp.body?.string() ?: return@use
                    // Split HTML into individual job cards containing data-job-share-url
                    val chunks = html.split("""data-job-share-url="""").drop(1)
                    for (chunk in chunks.take(12)) {
                        val shareUrl = chunk.substringBefore("\"").trim()
                        if (!shareUrl.startsWith("http")) continue
                        val cardWindow = """data-job-share-url="$chunk""".take(2200)
                        val attrs = mutableMapOf<String, String>()
                        attrPattern.findAll(cardWindow).forEach { m ->
                            attrs[m.groupValues[1]] = HtmlSanitizer.cleanToReadableText(m.groupValues[2])
                        }
                        if (attrs["data-job-closed"] == "1") continue

                        // Extract exact title either from <h2 class="search-job-title" title="..."> or data-job-name
                        val h2Title = Regex("""class="search-job-title"\s+title="([^"]+)"""")
                            .find(cardWindow)?.groupValues?.getOrNull(1)
                            ?.let { HtmlSanitizer.cleanToReadableText(it) }
                            ?: ""
                        val title = h2Title.ifBlank { attrs["data-job-name"].orEmpty() }.trim()
                        if (title.isBlank()) continue

                        val company = attrs["data-job-company"]?.ifBlank { "Entreprise Golfe" } ?: "Entreprise Golfe"
                        val loc = attrs["data-job-location"]?.ifBlank { defaultLoc } ?: defaultLoc
                        val workplace = attrs["data-job-workplace"]?.ifBlank { "On-site" } ?: "On-site"
                        val jobType = attrs["data-job-type"]?.ifBlank { "Full Time" } ?: "Full Time"
                        val category = attrs["data-job-categories"]?.takeIf { it.isNotBlank() && it != "Other" }.orEmpty()
                        val salary = attrs["data-job-salary"].orEmpty()
                        val rawDate = attrs["data-job-date"].orEmpty()
                        val email = attrs["data-job-contact-email"]?.trim().orEmpty()

                        val isRemote = workplace.contains("remote", ignoreCase = true)
                        val workModeLabel = when {
                            isRemote -> "REMOTE"
                            workplace.contains("hybrid", ignoreCase = true) -> "HYBRID"
                            else -> "ONSITE"
                        }

                        val summaryDesc = buildString {
                            append("$title chez $company — $loc ($workplace, $jobType)")
                            if (category.isNotBlank()) append(" · Domaine : $category")
                            if (salary.isNotBlank()) append(" · Salaire : $salary")
                            if (rawDate.isNotBlank()) append(" · Publié : $rawDate")
                            append(" · Source : Tanqeeb GCC ($flagCountry).")
                        }

                        val tags = buildList {
                            add("🏜️ Golfe")
                            add(flagCountry)
                            add(when (workModeLabel) { "REMOTE" -> "Remote"; "HYBRID" -> "Hybride"; else -> "Présentiel" })
                            if (category.isNotBlank()) add(category)
                        }.distinct()

                        offers.add(
                            JobOffer(
                                id = "tanqeeb_${shareUrl.hashCode()}",
                                title = title,
                                company = company,
                                location = loc,
                                isRemote = isRemote,
                                contractType = jobType,
                                description = summaryDesc,
                                sourcePlatform = "Tanqeeb Golfe",
                                url = shareUrl,
                                detectedAt = System.currentTimeMillis(),
                                requiredFormat = detectFormatFromText(summaryDesc),
                                recruiterEmail = email,
                                tags = tags,
                                regionScope = "GULF",
                                workMode = workModeLabel,
                                applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE",
                                salary = salary
                            ).withDeadlineCheck(rawPublished = rawDate)
                        )
                    }
                }
            }.onFailure {
                Log.w(TAG, "Tanqeeb sub-feed ($searchUrl) failed: ${it.message}")
            }
        }

        // Enrich top prioritized matches with full JSON-LD JobPosting description & deadlines (fast cap: 8 fetches)
        val prioritized = filterAndPrioritize(offers.distinctBy { it.url }, keywords, limit = 36).toMutableList()
        for (i in 0 until minOf(8, prioritized.size)) {
            val base = prioritized[i]
            runCatching {
                val req = Request.Builder().url(base.url).header("User-Agent", UA).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val html = resp.body?.string() ?: return@use
                    val ldObj = extractFirstJobPostingJsonLd(html)
                    if (ldObj != null) {
                        val fullDesc = HtmlSanitizer.cleanToReadableText(ldObj.str("description"))
                        val rawPosted = ldObj.str("datePosted").ifBlank { base.publishedDate }
                        val validThroughMs = DeadlineParser.parseTimestamp(ldObj.str("validThrough"))
                        val ldEmail = base.recruiterEmail.ifBlank { HtmlSanitizer.extractRecruiterEmail(fullDesc) }
                        if (fullDesc.length > base.description.length) {
                            prioritized[i] = base.copy(
                                description = fullDesc,
                                recruiterEmail = ldEmail,
                                applicationMethod = if (ldEmail.isNotBlank()) "EMAIL" else base.applicationMethod,
                                requiredFormat = detectFormatFromText(fullDesc)
                            ).withDeadlineCheck(rawPublished = rawPosted, explicitExpiryMillis = validThroughMs)
                        }
                    }
                }
            }
        }
        return prioritized
    }

    /**
     * GULF / GCC SCRAPER #2 : QatarLiving Jobs (Doha / Al Rayyan / Lusail) via Schema.org ItemList + JobPosting JSON-LD
     */
    private fun fetchQatarLivingJobs(keywords: List<String>): List<JobOffer> {
        val listReq = Request.Builder()
            .url("https://www.qatarliving.com/en/jobs/jobs/list")
            .header("User-Agent", UA)
            .build()

        val candidateUrls = mutableListOf<Pair<String, String>>()
        httpClient.newCall(listReq).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val html = resp.body?.string() ?: return emptyList()
            val itemListMatch = Regex("""<script type="application/ld\+json"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(html)
                .firstOrNull { it.groupValues[1].contains("ItemList") }
            if (itemListMatch != null) {
                val obj = runCatching { json.parseToJsonElement(itemListMatch.groupValues[1]).jsonObject }.getOrNull()
                obj?.get("itemListElement")?.takeIf { it is JsonArray }?.jsonArray?.forEach { item ->
                    val itObj = item.jsonObject
                    val title = HtmlSanitizer.cleanToReadableText(itObj.str("name"))
                    val url = itObj.str("url")
                    if (title.isNotBlank() && url.startsWith("http")) {
                        candidateUrls.add(title to url)
                    }
                }
            }
        }

        val offers = mutableListOf<JobOffer>()
        for ((listTitle, url) in candidateUrls.take(10)) {
            runCatching {
                val detReq = Request.Builder().url(url).header("User-Agent", UA).build()
                httpClient.newCall(detReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val html = resp.body?.string() ?: return@use
                    val jp = extractFirstJobPostingJsonLd(html) ?: return@use
                    val title = HtmlSanitizer.cleanToReadableText(jp.str("title")).ifBlank { listTitle }
                    val cleanDesc = HtmlSanitizer.cleanToReadableText(jp.str("description"))
                    if (title.isBlank() || cleanDesc.length < 25) return@use

                    val company = jp["hiringOrganization"]?.takeIf { it is JsonObject }?.jsonObject?.str("name")
                        ?.ifBlank { "Entreprise (Qatar)" } ?: "Entreprise (Qatar)"
                    val locality = jp["jobLocation"]?.takeIf { it is JsonObject }?.jsonObject
                        ?.get("address")?.takeIf { it is JsonObject }?.jsonObject?.str("addressLocality")
                        ?.ifBlank { "Doha" } ?: "Doha"
                    val empType = jp.str("employmentType").ifBlank { "Full-time" }
                    val datePosted = jp.str("datePosted")
                    val validThroughMs = DeadlineParser.parseTimestamp(jp.str("validThrough"))
                    val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)

                    offers.add(
                        JobOffer(
                            id = "qatarliving_${url.hashCode()}",
                            title = title,
                            company = company,
                            location = "$locality, Qatar",
                            isRemote = false,
                            contractType = empType,
                            description = cleanDesc,
                            sourcePlatform = "QatarLiving Jobs",
                            url = url,
                            detectedAt = System.currentTimeMillis(),
                            requiredFormat = detectFormatFromText(cleanDesc),
                            recruiterEmail = email,
                            tags = listOf("🏜️ Golfe", "🇶🇦 Qatar", "Présentiel", empType).distinct(),
                            regionScope = "GULF",
                            workMode = "ONSITE",
                            applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE"
                        ).withDeadlineCheck(rawPublished = datePosted, explicitExpiryMillis = validThroughMs)
                    )
                }
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 10)
    }

    /**
     * GULF / GCC SCRAPER #3 : Official Gulf Tech/Corporate Greenhouse APIs (Careem, Tamara, etc.)
     */
    private fun fetchGulfGreenhouseBoard(
        companyLabel: String,
        boardSlug: String,
        keywords: List<String>
    ): List<JobOffer> {
        val req = Request.Builder()
            .url("https://boards-api.greenhouse.io/v1/boards/$boardSlug/jobs?content=true")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val jobs = json.parseToJsonElement(body).jsonObject["jobs"]?.jsonArray ?: return emptyList()

            for (el in jobs) {
                val obj = el.jsonObject
                val id = obj.str("id")
                val title = HtmlSanitizer.cleanToReadableText(obj.str("title"))
                val loc = obj["location"]?.takeIf { it is JsonObject }?.jsonObject?.str("name").orEmpty()
                val jobUrl = obj.str("absolute_url")
                val cleanDesc = HtmlSanitizer.cleanToReadableText(obj.str("content"))
                if (title.isBlank() || jobUrl.isBlank() || cleanDesc.length < 25) continue
                if (loc.isNotBlank() && !isGulfLocation(loc) && !loc.contains("Remote", ignoreCase = true)) continue

                val finalLoc = loc.ifBlank { "Dubai, UAE / Riyadh, KSA" }
                val isRemote = finalLoc.contains("remote", ignoreCase = true)
                val updatedAt = obj.str("updated_at").ifBlank { obj.str("first_published") }
                val deadlineMs = DeadlineParser.parseTimestamp(obj.str("application_deadline"))
                val email = HtmlSanitizer.extractRecruiterEmail(cleanDesc)

                offers.add(
                    JobOffer(
                        id = "gh_${boardSlug}_$id",
                        title = title,
                        company = companyLabel,
                        location = finalLoc,
                        isRemote = isRemote,
                        contractType = "Full-time",
                        description = cleanDesc,
                        sourcePlatform = "$companyLabel (Golfe)",
                        url = jobUrl,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(cleanDesc),
                        recruiterEmail = email,
                        tags = listOf("🏜️ Golfe", if (isRemote) "Remote" else "Présentiel", companyLabel).distinct(),
                        regionScope = "GULF",
                        workMode = if (isRemote) "REMOTE" else "ONSITE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE"
                    ).withDeadlineCheck(rawPublished = updatedAt, explicitExpiryMillis = deadlineMs)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 15)
    }

    /**
     * GULF / GCC SCRAPER #4 : Official Gulf Ashby Job Boards (Lean Technologies, etc.)
     */
    private fun fetchGulfAshbyBoard(
        companyLabel: String,
        boardSlug: String,
        keywords: List<String>
    ): List<JobOffer> {
        val req = Request.Builder()
            .url("https://api.ashbyhq.com/posting-api/job-board/$boardSlug")
            .header("User-Agent", UA)
            .build()

        val offers = mutableListOf<JobOffer>()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val jobs = json.parseToJsonElement(body).jsonObject["jobs"]?.jsonArray ?: return emptyList()

            for (el in jobs) {
                val obj = el.jsonObject
                val id = obj.str("id")
                val title = HtmlSanitizer.cleanToReadableText(obj.str("title"))
                val loc = obj.str("location")
                val jobUrl = obj.str("jobUrl")
                val desc = HtmlSanitizer.cleanToReadableText(obj.str("descriptionPlain").ifBlank { obj.str("descriptionHtml") })
                if (title.isBlank() || jobUrl.isBlank() || desc.length < 25) continue
                if (loc.isNotBlank() && !isGulfLocation(loc) && !loc.contains("Remote", ignoreCase = true)) continue

                val isRemote = (obj["isRemote"]?.jsonPrimitive?.booleanOrNull == true) || loc.contains("remote", ignoreCase = true)
                val pub = obj.str("publishedAt")
                val email = HtmlSanitizer.extractRecruiterEmail(desc)

                offers.add(
                    JobOffer(
                        id = "ashby_${boardSlug}_$id",
                        title = title,
                        company = companyLabel,
                        location = loc.ifBlank { "Riyadh / Dubai (Golfe)" },
                        isRemote = isRemote,
                        contractType = obj.str("employmentType").ifBlank { "FullTime" },
                        description = desc,
                        sourcePlatform = "$companyLabel (Golfe)",
                        url = jobUrl,
                        detectedAt = System.currentTimeMillis(),
                        requiredFormat = detectFormatFromText(desc),
                        recruiterEmail = email,
                        tags = listOf("🏜️ Golfe", if (isRemote) "Remote" else "Présentiel", companyLabel).distinct(),
                        regionScope = "GULF",
                        workMode = if (isRemote) "REMOTE" else "ONSITE",
                        applicationMethod = if (email.isNotBlank()) "EMAIL" else "WEBSITE"
                    ).withDeadlineCheck(rawPublished = pub)
                )
            }
        }
        return filterAndPrioritize(offers, keywords, limit = 10)
    }

    private fun extractFirstJobPostingJsonLd(html: String): JsonObject? {
        val matches = Regex("""<script type="application/ld\+json"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html)
        for (m in matches) {
            val raw = m.groupValues[1].trim()
            if (!raw.contains("JobPosting")) continue
            val parsed = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            if (parsed != null && parsed.str("@type") == "JobPosting") return parsed
        }
        return null
    }

    private fun isGulfLocation(text: String): Boolean {
        val lower = text.lowercase()
        return GULF_LOC_KEYWORDS.any { lower.contains(it) }
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
        if (profile == null || profile.combinedSearchCorpus().isBlank()) return offer

        val jobCorpus = "${offer.title} ${offer.description} ${offer.tags.joinToString(" ")}".lowercase()
        val cvCorpus = profile.combinedSearchCorpus().lowercase()

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
