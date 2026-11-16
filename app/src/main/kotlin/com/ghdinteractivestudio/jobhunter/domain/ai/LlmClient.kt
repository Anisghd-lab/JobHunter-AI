package com.ghdinteractivestudio.jobhunter.domain.ai

import android.util.Log
import com.ghdinteractivestudio.jobhunter.data.local.AppSettings
import com.ghdinteractivestudio.jobhunter.data.local.FreeLlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

enum class LlmProvider {
    GEMINI,
    OPENAI,
    OLLAMA_CUSTOM
}

data class LlmConfig(
    val provider: LlmProvider = LlmProvider.OPENAI,
    val apiKey: String = "",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val modelName: String = "openrouter/free"
)

data class LlmResponse(
    val text: String,
    val providerUsed: String,
    val modelUsed: String
)

class LlmClient(private val config: LlmConfig = LlmConfig()) {

    companion object {
        private const val TAG = "LlmClient"

        /**
         * Smart Free-Model Fallback execution across all configured free LLM providers.
         * Exclusively calls 100% Free models (:free / Free Tier).
         */
        suspend fun generateWithFreeFallback(
            settings: AppSettings,
            prompt: String,
            systemInstruction: String = ""
        ): Result<LlmResponse> = withContext(Dispatchers.IO) {
            val candidates = buildCandidateChain(settings)
            if (candidates.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Aucune clé API LLM gratuite configurée dans Agent 24/7.")
                )
            }

            var lastError: Throwable? = null
            for ((provider, model, key) in candidates) {
                val baseUrl = if (provider == FreeLlmProvider.OLLAMA_LOCAL) {
                    settings.ollamaUrl
                } else {
                    provider.baseUrl
                }
                val llmProvider = when (provider) {
                    FreeLlmProvider.GEMINI_FREE -> LlmProvider.GEMINI
                    FreeLlmProvider.OLLAMA_LOCAL -> LlmProvider.OLLAMA_CUSTOM
                    else -> LlmProvider.OPENAI
                }

                val client = LlmClient(
                    LlmConfig(
                        provider = llmProvider,
                        apiKey = key,
                        baseUrl = baseUrl,
                        modelName = model
                    )
                )

                val result = client.generateText(prompt, systemInstruction)
                if (result.isSuccess) {
                    val out = result.getOrNull().orEmpty().trim()
                    if (out.isNotBlank()) {
                        return@withContext Result.success(
                            LlmResponse(
                                text = out,
                                providerUsed = provider.displayName,
                                modelUsed = model
                            )
                        )
                    }
                } else {
                    lastError = result.exceptionOrNull()
                    Log.w(TAG, "Free model $model (${provider.name}) failed: ${lastError?.message}")
                }
            }

            Result.failure(lastError ?: IllegalStateException("Tous les modèles gratuits ont échoué."))
        }

        private fun buildCandidateChain(settings: AppSettings): List<Triple<FreeLlmProvider, String, String>> {
            val chain = mutableListOf<Triple<FreeLlmProvider, String, String>>()

            // 1. Primary selected provider & model if key exists
            val primaryKey = settings.apiKeyFor(settings.activeProvider)
            if (primaryKey.isNotBlank()) {
                chain.add(Triple(settings.activeProvider, settings.activeModel, primaryKey))
            }

            // 2. OpenRouter Free fallbacks
            if (settings.openrouterApiKey.isNotBlank()) {
                val fallbacks = listOf(
                    "openrouter/free",
                    "google/gemma-4-31b-it:free",
                    "nvidia/nemotron-3.5-lightning:free",
                    "google/gemma-4-26b-a4b-it:free"
                )
                for (m in fallbacks) {
                    val item = Triple(FreeLlmProvider.OPENROUTER_FREE, m, settings.openrouterApiKey)
                    if (item !in chain) chain.add(item)
                }
            }

            // 3. xKIRO Free Tier fallbacks
            if (settings.xkiroApiKey.isNotBlank()) {
                val fallbacks = listOf(
                    "mistralai/ministral-14b",
                    "cohere/command-a",
                    "mistralai/mistral-large-2512"
                )
                for (m in fallbacks) {
                    val item = Triple(FreeLlmProvider.XKIRO_FREE, m, settings.xkiroApiKey)
                    if (item !in chain) chain.add(item)
                }
            }

            // 4. Gemini Free fallback
            if (settings.geminiApiKey.isNotBlank()) {
                val item = Triple(FreeLlmProvider.GEMINI_FREE, "gemini-2.0-flash", settings.geminiApiKey)
                if (item !in chain) chain.add(item)
            }

            // 5. Bytez Free fallback
            if (settings.bytezApiKey.isNotBlank()) {
                val item = Triple(FreeLlmProvider.BYTEZ_FREE, "Qwen/Qwen2.5-7B-Instruct", settings.bytezApiKey)
                if (item !in chain) chain.add(item)
            }

            return chain
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun generateText(prompt: String, systemInstruction: String = ""): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (config.apiKey.isBlank() && config.provider != LlmProvider.OLLAMA_CUSTOM) {
                error("Clé API manquante pour ${config.modelName}")
            }
            when (config.provider) {
                LlmProvider.GEMINI -> callGemini(prompt, systemInstruction)
                LlmProvider.OPENAI, LlmProvider.OLLAMA_CUSTOM -> callOpenAiCompatible(prompt, systemInstruction)
            }
        }
    }

    private fun callGemini(prompt: String, systemInstruction: String): String {
        val cleanBase = config.baseUrl.trimEnd('/')
        val endpoint = "$cleanBase/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}"
        val systemPart = if (systemInstruction.isNotBlank()) {
            """ "system_instruction": { "parts": [{ "text": ${escapeJson(systemInstruction)} }] },"""
        } else ""

        val requestJson = """
        {
            $systemPart
            "contents": [
                {
                    "parts": [{ "text": ${escapeJson(prompt)} }]
                }
            ],
            "generationConfig": {
                "temperature": 0.2
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error("Gemini API Error ${response.code}: ${body.take(250)}")
            }
            val root = jsonParser.parseToJsonElement(body).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            return parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
        }
    }

    private fun callOpenAiCompatible(prompt: String, systemInstruction: String): String {
        val cleanBase = config.baseUrl.trimEnd('/')
        val url = when {
            cleanBase.endsWith("/chat/completions") -> cleanBase
            cleanBase.endsWith("/v1") || cleanBase.endsWith("/openai/v1") -> "$cleanBase/chat/completions"
            else -> "$cleanBase/v1/chat/completions"
        }

        val messages = mutableListOf<String>()
        if (systemInstruction.isNotBlank()) {
            messages.add("""{"role": "system", "content": ${escapeJson(systemInstruction)}}""")
        }
        messages.add("""{"role": "user", "content": ${escapeJson(prompt)}}""")

        val requestJson = """
        {
            "model": "${config.modelName}",
            "messages": [${messages.joinToString(",")}],
            "temperature": 0.2
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("HTTP-Referer", "https://github.com/Anisghd-lab/JobHunter-AI")
            .addHeader("X-Title", "JobHunter AI Android")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error("API Error ${response.code} ($url): ${body.take(250)}")
            }
            val root = jsonParser.parseToJsonElement(body).jsonObject
            val choices = root["choices"]?.jsonArray
            val message = choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            return message?.get("content")?.jsonPrimitive?.content ?: ""
        }
    }

    private fun escapeJson(str: String): String =
        "\"" + str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
}
