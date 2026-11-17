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
        JobPortalAccount(
            id = "emploitic",
            name = "Emploitic Algérie",
            badge = "🇩🇿 Algérie",
            description = "Portail n°1 Algérie (session Keycloak persistante après connexion/CAPTCHA)",
            domainUrl = "https://emploitic.com",
            loginUrl = "https://emploitic.com/offres-d-emploi",
            signupUrl = "https://emploitic.com/auth/realms/candidate/account/",
            checkUrl = "https://emploitic.com/offres-d-emploi",
            authCookieHints = listOf("keycloak", "next-auth", "session", "token", "auth", "connect.sid")
        ),
        JobPortalAccount(
            id = "tanqeeb",
            name = "Tanqeeb Golfe (GCC)",
            badge = "🏜️ Golfe",
            description = "UAE, Arabie Saoudite, Qatar, Koweït, Oman & Bahreïn",
            domainUrl = "https://uae.tanqeeb.com",
            loginUrl = "https://uae.tanqeeb.com/users/login",
            signupUrl = "https://uae.tanqeeb.com/cv/Register",
            checkUrl = "https://uae.tanqeeb.com/users/login",
            authCookieHints = listOf("phpsessid", "laravel", "tanqeeb", "session", "auth", "remember")
        ),
        JobPortalAccount(
            id = "qatarliving",
            name = "QatarLiving Jobs",
            badge = "🇶🇦 Qatar",
            description = "Annonces directes Qatar (connexion utilisateur & candidatures)",
            domainUrl = "https://www.qatarliving.com",
            loginUrl = "https://www.qatarliving.com/en/jobs",
            signupUrl = "https://www.qatarliving.com/en/jobs",
            checkUrl = "https://www.qatarliving.com/en/jobs",
            authCookieHints = listOf("qat", "qat_refresh", "ssess", "sess", "ql_", "token", "auth", "session")
        ),
        JobPortalAccount(
            id = "linkedin",
            name = "LinkedIn Jobs",
            badge = "💼 Réseau Pro",
            description = "Postuler en 1 clic sur LinkedIn avec session persistante",
            domainUrl = "https://www.linkedin.com",
            loginUrl = "https://www.linkedin.com/login",
            signupUrl = "https://www.linkedin.com/signup",
            checkUrl = "https://www.linkedin.com/feed/",
            authCookieHints = listOf("li_at", "liap", "jsessionid", "bcookie", "bscookie")
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
