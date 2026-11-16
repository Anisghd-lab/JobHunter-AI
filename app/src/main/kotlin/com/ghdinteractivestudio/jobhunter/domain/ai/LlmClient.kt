package com.ghdinteractivestudio.jobhunter.domain.ai

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
    val provider: LlmProvider = LlmProvider.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val modelName: String = "gemini-2.0-flash"
)

class LlmClient(private val config: LlmConfig) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun generateText(prompt: String, systemInstruction: String = ""): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (config.provider) {
                LlmProvider.GEMINI -> callGemini(prompt, systemInstruction)
                LlmProvider.OPENAI, LlmProvider.OLLAMA_CUSTOM -> callOpenAiCompatible(prompt, systemInstruction)
            }
        }
    }

    private fun callGemini(prompt: String, systemInstruction: String): String {
        val endpoint = "${config.baseUrl}/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}"
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
                error("Gemini API Error ${response.code}: $body")
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
        val url = if (config.baseUrl.endsWith("/")) "${config.baseUrl}v1/chat/completions" else "${config.baseUrl}/v1/chat/completions"
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
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error("OpenAI API Error ${response.code}: $body")
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
