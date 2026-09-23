package com.example.domain.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiThinkingService {

    private const val MODEL_NAME = "gemini-3.1-pro-preview"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeWithHighThinking(
        bookTitle: String,
        chapterTitle: String,
        contextText: String,
        userQuery: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Kunci API Gemini belum dikonfigurasi di Secrets panel. Silakan tambahkan GEMINI_API_KEY di Secrets AI Studio.")
            )
        }

        try {
            val systemPrompt = """
                Anda adalah asisten pembaca Light Novel (Yomu AI Companion) yang cerdas, analitis, mendalam, dan memahami sastra serta budaya Jepang/LN secara mendalam.
                Analisis teks dengan penalaran mendalam (high thinking mode), berikan wawasan tentang relasi karakter, motif tersembunyi, makna kanji/furigana, worldbuilding, dan struktur narasi.
                Gunakan bahasa Indonesia yang santun, jernih, dan berbobot.
            """.trimIndent()

            val userPrompt = """
                Buku: $bookTitle
                Bab: $chapterTitle
                
                Konteks Bacaan:
                \"\"\"
                ${contextText.take(6000)}
                \"\"\"
                
                Permintaan Pembaca:
                $userQuery
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                // systemInstruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })

                // contents
                val contentsArray = JSONArray()
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                })
                put("contents", contentsArray)

                // generationConfig with HIGH thinking level as mandated
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                    // Note: As specified, do not set maxOutputTokens
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyString)
                        errObj.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $bodyString"
                    }
                    return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val textBuilder = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val text = part.optString("text", "")
                            textBuilder.append(text)
                        }
                        return@withContext Result.success(textBuilder.toString().trim())
                    }
                }
                Result.failure(Exception("Respons model kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
