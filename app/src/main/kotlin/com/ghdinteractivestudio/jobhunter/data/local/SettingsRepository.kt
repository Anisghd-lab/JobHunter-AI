package com.ghdinteractivestudio.jobhunter.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FreeLlmProvider(val displayName: String, val baseUrl: String, val badge: String) {
    OPENROUTER_FREE("OpenRouter (100% Free)", "https://openrouter.ai/api/v1", "FREE"),
    XKIRO_FREE("xKIRO (Free Tier)", "https://api.xkiro.com/v1", "FREE"),
    BYTEZ_FREE("Bytez (Free Tier <=7B)", "https://api.bytez.com/models/v2/openai/v1", "FREE"),
    GEMINI_FREE("Google Gemini (AI Studio Free)", "https://generativelanguage.googleapis.com", "FREE"),
    OLLAMA_LOCAL("Ollama Local (Offline)", "http://127.0.0.1:11434/v1", "LOCAL")
}

data class FreeModelOption(
    val id: String,
    val label: String,
    val provider: FreeLlmProvider
)

object FreeLlmCatalog {
    val models: List<FreeModelOption> = listOf(
        // OpenRouter 100% Free Models
        FreeModelOption("openrouter/free", "OpenRouter Auto Free Router (200k)", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning Free (1M)", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("google/gemma-4-31b-it:free", "Gemma 4 31B IT Free (262k)", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B A4B Free (262k)", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("nvidia/nemotron-3-ultra-550b-a55b:free", "Nemotron 3 Ultra 550B Free (1M)", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", "Nemotron 3 Nano Reasoning Free", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("baidu/ernie-5.5-flash-preview:free", "ERNIE 5.5 Flash Free (1M)", FreeLlmProvider.OPENROUTER_FREE),
        FreeModelOption("cohere/north-mini-code:free", "Cohere North Mini Free (256k)", FreeLlmProvider.OPENROUTER_FREE),

        // xKIRO Free Tier Models (Strict Free List)
        FreeModelOption("mistralai/ministral-14b", "Ministral 3 14B Free (256k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("mistralai/mistral-large-2512", "Mistral Large 3 Free (256k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("cohere/command-a", "Command A Free (288k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("cohere/command-a-plus", "Command A+ Free (436k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("cohere/command-r-plus-08-2024", "Command R+ Free (128k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("mistralai/ministral-8b", "Ministral 3 8B Free (256k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("mistralai/codestral-2508", "Codestral 2508 Free (256k)", FreeLlmProvider.XKIRO_FREE),
        FreeModelOption("sensenova/sensenova-6.8-flash-lite", "SenseNova 6.8 Flash-Lite Free", FreeLlmProvider.XKIRO_FREE),

        // Bytez Free Tier
        FreeModelOption("Qwen/Qwen2.5-7B-Instruct", "Qwen 2.5 7B Instruct Free", FreeLlmProvider.BYTEZ_FREE),
        FreeModelOption("mistralai/Mistral-7B-Instruct-v0.3", "Mistral 7B Free", FreeLlmProvider.BYTEZ_FREE),
        FreeModelOption("microsoft/Phi-3-mini-4k-instruct", "Phi-3 Mini Free", FreeLlmProvider.BYTEZ_FREE),
        FreeModelOption("google/gemma-2-2b-it", "Gemma 2 2B Free", FreeLlmProvider.BYTEZ_FREE),

        // Gemini Free Tier
        FreeModelOption("gemini-2.0-flash", "Gemini 2.0 Flash Free", FreeLlmProvider.GEMINI_FREE),

        // Local Ollama
        FreeModelOption("qwen2.5-coder:3b", "Qwen 2.5 3B Local", FreeLlmProvider.OLLAMA_LOCAL)
    )

    fun modelsFor(provider: FreeLlmProvider): List<FreeModelOption> =
        models.filter { it.provider == provider }
}

data class AppSettings(
    val activeProvider: FreeLlmProvider = FreeLlmProvider.OPENROUTER_FREE,
    val activeModel: String = "openrouter/free",
    val openrouterApiKey: String = "",
    val xkiroApiKey: String = "",
    val bytezApiKey: String = "",
    val geminiApiKey: String = "",
    val ollamaUrl: String = "http://127.0.0.1:11434/v1",
    val senderEmail: String = "",
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 465,
    val smtpPassword: String = "",
    val gmailOAuthToken: String = "",
    val agentActive247: Boolean = true,
    val autoApplyEnabled: Boolean = false,
    val autoApplyMinScore: Int = 50,
    val adaptCvPerOffer: Boolean = false,
    val aiAutonomousEmailControl: Boolean = true,
    val autoFollowUpEnabled: Boolean = true,
    val followUpDelayDays: Int = 5,
    val preferInstalledYahooApp: Boolean = false,
    val connectedYahooPackage: String = "",
    val smtpAuthPaused: Boolean = false,
    val autoApplyEmailOnly: Boolean = false,
    val autoApplyMaxPerCycle: Int = 12,
    val autoApplyMaxPerDay: Int = 50,
    val maxFollowUpsPerOffer: Int = 1,
    val lastAutoApplyRunAt: Long = 0L,
    val lastInboxCheckAt: Long = 0L,
    val lastAutomationSummary: String = "",
    val humanizeWritingEnabled: Boolean = true,
    val connectedPortalsCsv: String = "",
    val isDarkTheme: Boolean = true,
    val lastScanTimestamp: Long = 0L
) {
    fun isPortalConnected(portalId: String): Boolean =
        connectedPortalsCsv.split(",").map { it.trim() }.any { it == portalId }

    fun withPortalStatus(portalId: String, connected: Boolean): AppSettings {
        val current = connectedPortalsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        if (connected) current.add(portalId) else current.remove(portalId)
        return copy(connectedPortalsCsv = current.sorted().joinToString(","))
    }
    fun apiKeyFor(provider: FreeLlmProvider): String = when (provider) {
        FreeLlmProvider.OPENROUTER_FREE -> openrouterApiKey
        FreeLlmProvider.XKIRO_FREE -> xkiroApiKey
        FreeLlmProvider.BYTEZ_FREE -> bytezApiKey
        FreeLlmProvider.GEMINI_FREE -> geminiApiKey
        FreeLlmProvider.OLLAMA_LOCAL -> "ollama"
    }

    fun hasAnyAiKey(): Boolean =
        openrouterApiKey.isNotBlank() ||
            xkiroApiKey.isNotBlank() ||
            bytezApiKey.isNotBlank() ||
            geminiApiKey.isNotBlank()

    fun hasEmailCredentials(): Boolean =
        (senderEmail.isNotBlank() && smtpPassword.isNotBlank()) ||
            (senderEmail.isNotBlank() && gmailOAuthToken.isNotBlank())

    fun canSendSilently(): Boolean = hasEmailCredentials() && !smtpAuthPaused
}

class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("jobhunter_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadFromPrefs())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadFromPrefs(): AppSettings {
        val provStr = prefs.getString("active_provider", FreeLlmProvider.OPENROUTER_FREE.name)
            ?: FreeLlmProvider.OPENROUTER_FREE.name
        val prov = runCatching { FreeLlmProvider.valueOf(provStr) }
            .getOrDefault(FreeLlmProvider.OPENROUTER_FREE)

        // One-time upgrade so users immediately benefit from 50% threshold, 50/day capacity & Web Portal Assistant
        if (!prefs.getBoolean("auto_50_web_v3", false)) {
            prefs.edit()
                .putInt("auto_apply_min_score", 50)
                .putInt("auto_apply_max_day", 50)
                .putInt("auto_apply_max_cycle", 12)
                .putBoolean("auto_apply_email_only", false)
                .putBoolean("auto_50_web_v3", true)
                .apply()
        }

        return AppSettings(
            activeProvider = prov,
            activeModel = prefs.getString("active_model", "openrouter/free") ?: "openrouter/free",
            openrouterApiKey = prefs.getString("openrouter_key", "") ?: "",
            xkiroApiKey = prefs.getString("xkiro_key", "") ?: "",
            bytezApiKey = prefs.getString("bytez_key", "") ?: "",
            geminiApiKey = prefs.getString("gemini_key", "") ?: "",
            ollamaUrl = prefs.getString("ollama_url", "http://127.0.0.1:11434/v1") ?: "http://127.0.0.1:11434/v1",
            senderEmail = prefs.getString("sender_email", "") ?: "",
            smtpHost = prefs.getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com",
            smtpPort = prefs.getInt("smtp_port", 465),
            smtpPassword = prefs.getString("smtp_password", "") ?: "",
            gmailOAuthToken = prefs.getString("gmail_oauth_token", "") ?: "",
            agentActive247 = prefs.getBoolean("agent_247", true),
            autoApplyEnabled = prefs.getBoolean("auto_apply", false),
            autoApplyMinScore = prefs.getInt("auto_apply_min_score", 50),
            adaptCvPerOffer = prefs.getBoolean("adapt_cv_per_offer", false),
            aiAutonomousEmailControl = prefs.getBoolean("ai_auto_email", true),
            autoFollowUpEnabled = prefs.getBoolean("auto_follow_up", true),
            followUpDelayDays = prefs.getInt("follow_up_days", 5),
            preferInstalledYahooApp = prefs.getBoolean("prefer_yahoo_app", false),
            connectedYahooPackage = prefs.getString("connected_yahoo_pkg", "") ?: "",
            smtpAuthPaused = prefs.getBoolean("smtp_auth_paused", false),
            autoApplyEmailOnly = prefs.getBoolean("auto_apply_email_only", false),
            autoApplyMaxPerCycle = prefs.getInt("auto_apply_max_cycle", 12),
            autoApplyMaxPerDay = prefs.getInt("auto_apply_max_day", 50),
            maxFollowUpsPerOffer = prefs.getInt("max_followups_offer", 1),
            lastAutoApplyRunAt = prefs.getLong("last_auto_apply_ts", 0L),
            lastInboxCheckAt = prefs.getLong("last_inbox_check_ts", 0L),
            lastAutomationSummary = prefs.getString("last_auto_summary", "") ?: "",
            humanizeWritingEnabled = prefs.getBoolean("humanize_writing", true),
            connectedPortalsCsv = prefs.getString("connected_portals_csv", "") ?: "",
            isDarkTheme = prefs.getBoolean("dark_theme", true),
            lastScanTimestamp = prefs.getLong("last_scan_ts", 0L)
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit().apply {
            putString("active_provider", next.activeProvider.name)
            putString("active_model", next.activeModel)
            putString("openrouter_key", next.openrouterApiKey.trim())
            putString("xkiro_key", next.xkiroApiKey.trim())
            putString("bytez_key", next.bytezApiKey.trim())
            putString("gemini_key", next.geminiApiKey.trim())
            putString("ollama_url", next.ollamaUrl.trim())
            putString("sender_email", next.senderEmail.trim())
            putString("smtp_host", next.smtpHost.trim())
            putInt("smtp_port", next.smtpPort)
            putString("smtp_password", next.smtpPassword)
            putString("gmail_oauth_token", next.gmailOAuthToken.trim())
            putBoolean("agent_247", next.agentActive247)
            putBoolean("auto_apply", next.autoApplyEnabled)
            putInt("auto_apply_min_score", next.autoApplyMinScore)
            putBoolean("adapt_cv_per_offer", next.adaptCvPerOffer)
            putBoolean("ai_auto_email", next.aiAutonomousEmailControl)
            putBoolean("auto_follow_up", next.autoFollowUpEnabled)
            putInt("follow_up_days", next.followUpDelayDays)
            putBoolean("prefer_yahoo_app", next.preferInstalledYahooApp)
            putString("connected_yahoo_pkg", next.connectedYahooPackage.trim())
            putBoolean("smtp_auth_paused", next.smtpAuthPaused)
            putBoolean("auto_apply_email_only", next.autoApplyEmailOnly)
            putInt("auto_apply_max_cycle", next.autoApplyMaxPerCycle)
            putInt("auto_apply_max_day", next.autoApplyMaxPerDay)
            putInt("max_followups_offer", next.maxFollowUpsPerOffer)
            putLong("last_auto_apply_ts", next.lastAutoApplyRunAt)
            putLong("last_inbox_check_ts", next.lastInboxCheckAt)
            putString("last_auto_summary", next.lastAutomationSummary)
            putBoolean("humanize_writing", next.humanizeWritingEnabled)
            putString("connected_portals_csv", next.connectedPortalsCsv)
            putBoolean("dark_theme", next.isDarkTheme)
            putLong("last_scan_ts", next.lastScanTimestamp)
        }.apply()
        _settings.value = next
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
    }
}
