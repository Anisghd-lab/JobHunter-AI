package com.ghdinteractivestudio.jobhunter.domain.automation

import android.util.Base64
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class JobPortalAccount(
    val id: String,
    val name: String,
    val badge: String,
    val description: String,
    val domainUrl: String,
    val loginUrl: String,
    val signupUrl: String,
    val checkUrl: String,
    val authCookieHints: List<String>
)

data class PortalCheckResult(
    val portalId: String,
    val connected: Boolean,
    val message: String
)

object PortalSessionRegistry {

    val portals: List<JobPortalAccount> = listOf(
        // --- FRANCE (Ferroviaire & Travaux Publics) ---
        JobPortalAccount(
            id = "sncf",
            name = "SNCF Emploi & Réseau",
            badge = "🇫🇷 France Rail",
            description = "Voies ferrées, caténaires, signalisation, métré & régénération réseau LGV",
            domainUrl = "https://www.emploi.sncf.com",
            loginUrl = "https://www.emploi.sncf.com",
            signupUrl = "https://www.emploi.sncf.com",
            checkUrl = "https://www.emploi.sncf.com",
            authCookieHints = listOf("sncf", "session", "auth", "token", "remember")
        ),
        JobPortalAccount(
            id = "colasrail",
            name = "Colas Rail (Bouygues)",
            badge = "🇫🇷 France Rail / TP",
            description = "Infrastructures ferroviaires, voies, électrification & études de prix",
            domainUrl = "https://www.colasrail.com",
            loginUrl = "https://www.colasrail.com/nos-offres/",
            signupUrl = "https://www.colasrail.com/nos-offres/",
            checkUrl = "https://www.colasrail.com/nos-offres/",
            authCookieHints = listOf("colas", "session", "auth", "phpsessid")
        ),
        JobPortalAccount(
            id = "systra",
            name = "SYSTRA Carrières",
            badge = "🇫🇷 France Ingénierie",
            description = "Leader ingénierie ferroviaire, LGV, métros, métré & chiffrage budgétaire",
            domainUrl = "https://jobs.systra.com",
            loginUrl = "https://jobs.systra.com",
            signupUrl = "https://jobs.systra.com",
            checkUrl = "https://jobs.systra.com",
            authCookieHints = listOf("systra", "session", "token", "sap-usercontext")
        ),
        JobPortalAccount(
            id = "etf_vinci",
            name = "ETF (VINCI Construction)",
            badge = "🇫🇷 France Rail",
            description = "Travaux ferroviaires, ballast, caténaires, métré d'exécution & chiffrage",
            domainUrl = "https://www.etf.fr",
            loginUrl = "https://www.etf.fr/nous-rejoindre/",
            signupUrl = "https://www.etf.fr/nous-rejoindre/",
            checkUrl = "https://www.etf.fr/nous-rejoindre/",
            authCookieHints = listOf("etf", "vinci", "session", "auth")
        ),
        JobPortalAccount(
            id = "eiffage_rail",
            name = "Eiffage Rail & TP",
            badge = "🇫🇷 France TP / Rail",
            description = "Grands projets ferroviaires, génie civil, études de prix et métré",
            domainUrl = "https://jobs.eiffage.com",
            loginUrl = "https://jobs.eiffage.com",
            signupUrl = "https://jobs.eiffage.com",
            checkUrl = "https://jobs.eiffage.com",
            authCookieHints = listOf("eiffage", "session", "auth", "token")
        ),
        JobPortalAccount(
            id = "tso_nge",
            name = "TSO (NGE Ferroviaire)",
            badge = "🇫🇷 France Rail",
            description = "Construction et renouvellement de voies ferrées, métré travaux et devis",
            domainUrl = "https://www.tso.fr",
            loginUrl = "https://www.tso.fr",
            signupUrl = "https://www.tso.fr",
            checkUrl = "https://www.tso.fr",
            authCookieHints = listOf("tso", "nge", "session", "auth")
        ),

        // --- CANADA (Chemin de Fer & Grands Travaux) ---
        JobPortalAccount(
            id = "cn_rail",
            name = "CN (Canadian National)",
            badge = "🇨🇦 Canada Rail",
            description = "Réseau ferroviaire transcanadien, entretien des voies & estimation de coûts",
            domainUrl = "https://www.cn.ca",
            loginUrl = "https://www.cn.ca/en/careers/",
            signupUrl = "https://www.cn.ca/en/careers/",
            checkUrl = "https://www.cn.ca/en/careers/",
            authCookieHints = listOf("cn", "career", "session", "auth", "token")
        ),
        JobPortalAccount(
            id = "cpkc",
            name = "CPKC (Canadian Pacific)",
            badge = "🇨🇦 Canada Rail",
            description = "Infrastructures ferroviaires, métré, estimation de contrats & ponts",
            domainUrl = "https://www.cpkcr.com",
            loginUrl = "https://www.cpkcr.com/en/careers",
            signupUrl = "https://www.cpkcr.com/en/careers",
            checkUrl = "https://www.cpkcr.com/en/careers",
            authCookieHints = listOf("cpkc", "session", "auth", "token")
        ),
        JobPortalAccount(
            id = "via_rail",
            name = "VIA Rail Canada",
            badge = "🇨🇦 Canada Rail",
            description = "Infrastructures voyageurs, ouvrages d'art ferroviaires & suivi des budgets",
            domainUrl = "https://corpo.viarail.ca",
            loginUrl = "https://corpo.viarail.ca/fr/carrieres",
            signupUrl = "https://corpo.viarail.ca/fr/carrieres",
            checkUrl = "https://corpo.viarail.ca/fr/carrieres",
            authCookieHints = listOf("viarail", "session", "auth", "token")
        ),
        JobPortalAccount(
            id = "atkinsrealis",
            name = "AtkinsRéalis (SNC-Lavalin)",
            badge = "🇨🇦 Canada Ingénierie",
            description = "Grands projets ferroviaires, voies, génie civil & estimation principale",
            domainUrl = "https://careers.atkinsrealis.com",
            loginUrl = "https://careers.atkinsrealis.com",
            signupUrl = "https://careers.atkinsrealis.com",
            checkUrl = "https://careers.atkinsrealis.com",
            authCookieHints = listOf("atkinsrealis", "session", "auth", "token")
        ),
        JobPortalAccount(
            id = "alstom_ca",
            name = "Alstom Canada",
            badge = "🇨🇦 Canada Transport",
            description = "Systèmes ferroviaires, signalisation, pose de voies et métré",
            domainUrl = "https://jobsearch.alstom.com",
            loginUrl = "https://jobsearch.alstom.com",
            signupUrl = "https://jobsearch.alstom.com",
            checkUrl = "https://jobsearch.alstom.com",
            authCookieHints = listOf("alstom", "session", "auth", "token")
        ),
        JobPortalAccount(
            id = "guichet_emplois",
            name = "Guichet-Emplois Canada",
            badge = "🇨🇦 Canada Fédéral",
            description = "Portail officiel gouvernemental canadien — Métreur, Estimateur, Travaux Publics",
            domainUrl = "https://www.guichetemplois.gc.ca",
            loginUrl = "https://www.guichetemplois.gc.ca/connexion",
            signupUrl = "https://www.guichetemplois.gc.ca/inscrire",
            checkUrl = "https://www.guichetemplois.gc.ca/accueil",
            authCookieHints = listOf("guichet", "jobbank", "session", "auth", "token")
        ),

        // --- ALGÉRIE (Infrastructures Ferroviaires & Travaux Publics) ---
        JobPortalAccount(
            id = "setirail",
            name = "SETIRAIL",
            badge = "🇩🇿 Algérie Rail",
            description = "Société Technique de l'Ingénierie de Rail (Ingénierie, voies ferrées & études)",
            domainUrl = "https://www.setirail.dz",
            loginUrl = "https://www.setirail.dz",
            signupUrl = "https://www.setirail.dz",
            checkUrl = "https://www.setirail.dz",
            authCookieHints = listOf("setirail", "session", "auth")
        ),
        JobPortalAccount(
            id = "anesrif",
            name = "ANESRIF Algérie",
            badge = "🇩🇿 Algérie Rail",
            description = "Agence Nationale d'Études et de Suivi de la Réalisation des Investissements Ferroviaires",
            domainUrl = "https://www.anesrif.dz",
            loginUrl = "https://www.anesrif.dz",
            signupUrl = "https://www.anesrif.dz",
            checkUrl = "https://www.anesrif.dz",
            authCookieHints = listOf("anesrif", "session", "auth")
        ),
        JobPortalAccount(
            id = "cosider_tp",
            name = "Cosider Travaux Publics",
            badge = "🇩🇿 Algérie TP",
            description = "Leader national des grands travaux publics, terrassements et ouvrages d'art",
            domainUrl = "https://www.cosider-groupe.dz",
            loginUrl = "https://www.cosider-groupe.dz",
            signupUrl = "https://www.cosider-groupe.dz",
            checkUrl = "https://www.cosider-groupe.dz",
            authCookieHints = listOf("cosider", "session", "auth")
        ),
        JobPortalAccount(
            id = "emploitic",
            name = "Emploitic Algérie (BTP / TP)",
            badge = "🇩🇿 Algérie",
            description = "Portail n°1 Algérie — Catégorie Génie Civil, Travaux Publics & Chantiers",
            domainUrl = "https://emploitic.com",
            loginUrl = "https://emploitic.com/offres-d-emploi",
            signupUrl = "https://emploitic.com/auth/realms/candidate/account/",
            checkUrl = "https://emploitic.com/offres-d-emploi",
            authCookieHints = listOf("keycloak", "next-auth", "session", "token", "auth", "connect.sid")
        ),

        // --- RÉSEAU PRO & GOLFE ---
        JobPortalAccount(
            id = "linkedin",
            name = "LinkedIn Jobs",
            badge = "💼 Réseau Pro",
            description = "Candidatures ferroviaires & TP ciblées France, Canada, Algérie et Golfe",
            domainUrl = "https://www.linkedin.com",
            loginUrl = "https://www.linkedin.com/login",
            signupUrl = "https://www.linkedin.com/signup",
            checkUrl = "https://www.linkedin.com/feed/",
            authCookieHints = listOf("li_at", "liap", "jsessionid", "bcookie", "bscookie")
        ),
        JobPortalAccount(
            id = "tanqeeb",
            name = "Tanqeeb Golfe (GCC)",
            badge = "🏜️ Golfe",
            description = "Postes Quantity Surveyor & Civil Engineer aux Émirats, Qatar et Arabie Saoudite",
            domainUrl = "https://uae.tanqeeb.com",
            loginUrl = "https://uae.tanqeeb.com/users/login",
            signupUrl = "https://uae.tanqeeb.com/cv/Register",
            checkUrl = "https://uae.tanqeeb.com/users/login",
            authCookieHints = listOf("phpsessid", "laravel", "tanqeeb", "session", "auth", "remember")
        )
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /**
     * Tests real portal authentication using actual WebView CookieManager cookies
     * + live HTTP session probe (never mocks or fakes a connected status).
     */
    suspend fun testPortalConnection(portal: JobPortalAccount): PortalCheckResult = withContext(Dispatchers.IO) {
        val cookieManager = CookieManager.getInstance()
        val rawCookies = listOfNotNull(
            runCatching { cookieManager.getCookie(portal.domainUrl) }.getOrNull(),
            runCatching { cookieManager.getCookie(portal.loginUrl) }.getOrNull(),
            runCatching { cookieManager.getCookie(portal.signupUrl) }.getOrNull()
        )
            .flatMap { it.split(";") }
            .map { it.trim() }
            .filter { it.contains("=") }
            .distinctBy { it.substringBefore("=").lowercase() }

        if (rawCookies.isEmpty()) {
            return@withContext PortalCheckResult(
                portalId = portal.id,
                connected = false,
                message = "✗ Aucun cookie sur ${portal.name} — ouvrez « Créer / Connecter », créez votre compte ou connectez-vous."
            )
        }

        val cookieMap = rawCookies.associate { pair ->
            pair.substringBefore("=").trim().lowercase() to pair.substringAfter("=").trim()
        }
        val cookieHeader = rawCookies.joinToString("; ")
        val hasLikelyAuthCookie = rawCookies.any { pair ->
            val name = pair.substringBefore("=").lowercase()
            val value = pair.substringAfter("=").trim()
            value.length >= 6 && portal.authCookieHints.any { hint -> name.contains(hint) }
        }

        val liveCheck = runCatching {
            val req = Request.Builder()
                .url(portal.checkUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36")
                .header("Cookie", cookieHeader)
                .build()

            client.newCall(req).execute().use { resp ->
                val location = resp.header("Location").orEmpty().lowercase()
                val body = resp.body?.string().orEmpty().take(35000).lowercase()

                when (portal.id) {
                    "tanqeeb" -> {
                        // Tanqeeb /users/login redirects away to dashboard/home when already authenticated
                        val redirectedLoggedIn = resp.code in 300..399 && !location.contains("login") && location.isNotBlank()
                        val bodyLoggedIn = resp.isSuccessful && (body.contains("logout") || body.contains("sign out") || body.contains("my cv"))
                        redirectedLoggedIn || bodyLoggedIn
                    }
                    "emploitic" -> {
                        val hasLoggedInDom = body.contains("déconnexion") || body.contains("mon profil") || body.contains("mes candidatures")
                        hasLikelyAuthCookie && (hasLoggedInDom || rawCookies.any { it.lowercase().contains("keycloak_identity") || it.lowercase().contains("next-auth.session") })
                    }
                    "qatarliving" -> {
                        // QatarLiving Next.js SPA stores auth in qat (JWT access token) & qat_refresh Http cookies
                        val qatToken = cookieMap["qat"].orEmpty()
                        val qatRefresh = cookieMap["qat_refresh"].orEmpty()
                        val validJwtSession = qatToken.length >= 40 && isJwtActive(qatToken) && qatRefresh.length >= 20
                        val hasDomSignInSuccess = (resp.isSuccessful || resp.code in 300..399) &&
                            !location.contains("login") &&
                            (body.contains("logout") || body.contains("sign out") || body.contains("qlnext_user"))
                        (resp.isSuccessful && validJwtSession) || (hasLikelyAuthCookie && hasDomSignInSuccess)
                    }
                    "linkedin" -> {
                        val hasLiAt = cookieMap["li_at"].orEmpty().length >= 20 || cookieMap["liap"].orEmpty().length >= 10
                        val bodyLoggedIn = body.contains("feed") || body.contains("mynetwork") || body.contains("sign out")
                        hasLiAt || (resp.code in 300..399 && !location.contains("login")) || bodyLoggedIn
                    }
                    else -> {
                        val hasLoggedInDom = body.contains("logout") || body.contains("sign out") || body.contains("my account")
                        hasLikelyAuthCookie && hasLoggedInDom
                    }
                }
            }
        }

        val verified = liveCheck.getOrDefault(false)
        if (verified) {
            PortalCheckResult(
                portalId = portal.id,
                connected = true,
                message = "✓ Session ${portal.name} connectée & vérifiée (${rawCookies.size} cookies) !"
            )
        } else {
            val suffix = if (liveCheck.isFailure) " (${liveCheck.exceptionOrNull()?.message?.take(40)})" else ""
            PortalCheckResult(
                portalId = portal.id,
                connected = false,
                message = "✗ Non connecté sur ${portal.name}$suffix — touchez « Créer / Connecter », connectez-vous et validez le CAPTCHA."
            )
        }
    }

    private fun isJwtActive(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) return token.length >= 40
        return runCatching {
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val expSec = JSONObject(String(payloadBytes, Charsets.UTF_8)).optLong("exp", 0L)
            expSec == 0L || (expSec * 1000L) > System.currentTimeMillis()
        }.getOrDefault(true)
    }
}
