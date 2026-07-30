package com.example.data.remote

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

data class AiNotificationResult(
    val title: String,
    val message: String
)

object GeminiNotificationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generatePersonalizedNotification(
        userContext: String
    ): Result<AiNotificationResult> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(generateLocalPersonalizedFallback(userContext))
        }

        val prompt = """
            You are an AI Shadow Fighter & Personal Assistant companion crafting custom personalized push notifications for an Android app.
            User Goal / Topic / Context: "$userContext"
            
            Generate an awesome, highly personalized, high-octane notification title and message.
            - Keep title under 6 words with relevant action emoji (e.g., ⚡, ⚔️, 🏆, 🔥, 🎯).
            - Keep message concise (1-2 sentences), highly engaging and tailored directly to the user's context.
            
            Return JSON strictly with format:
            {
              "title": "Your Generated Title",
              "message": "Your Generated Personalized Message"
            }
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.success(generateLocalPersonalizedFallback(userContext))
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text.isNullOrBlank()) {
                return@withContext Result.success(generateLocalPersonalizedFallback(userContext))
            }

            val parsedAiJson = JSONObject(text.trim())
            val title = parsedAiJson.optString("title", "⚡ Shadow Fighter Alert")
            val message = parsedAiJson.optString("message", userContext)

            Result.success(AiNotificationResult(title = title, message = message))
        } catch (e: Exception) {
            Result.success(generateLocalPersonalizedFallback(userContext))
        }
    }

    private fun generateLocalPersonalizedFallback(userContext: String): AiNotificationResult {
        val lower = userContext.lowercase()
        return when {
            lower.contains("workout") || lower.contains("gym") || lower.contains("exercise") -> AiNotificationResult(
                title = "🔥 SHADOW WORKOUT MISSION",
                message = "Relentless strength! Push through your workout like a boss — every rep builds legendary power!"
            )
            lower.contains("study") || lower.contains("exam") || lower.contains("read") || lower.contains("book") -> AiNotificationResult(
                title = "📚 SCHOLAR SHADOW QUEST",
                message = "Lock into supreme focus! Conquer your study goals with disciplined precision and level up your mind."
            )
            lower.contains("water") || lower.contains("drink") || lower.contains("hydrate") -> AiNotificationResult(
                title = "💧 CYBER HYDRATION BOOST",
                message = "Fuel your body for combat! Drink water immediately and restore your peak stamina."
            )
            lower.contains("sleep") || lower.contains("rest") || lower.contains("zen") || lower.contains("relax") -> AiNotificationResult(
                title = "🧘 SHADOW RECOVERY MODE",
                message = "True warriors know when to rest. Recharge your spirit so you can rise stronger tomorrow!"
            )
            lower.contains("victory") || lower.contains("win") || lower.contains("hype") || lower.contains("energy") -> AiNotificationResult(
                title = "⚡ HIGH-OCTANE VICTORY",
                message = "Unleash your full potential! No challenge can withstand your relentless determination!"
            )
            else -> AiNotificationResult(
                title = "⚔️ SHADOW MISSION: ACT NOW",
                message = "Your mission is ready: '$userContext'. Strike fast, stay focused, and claim your victory!"
            )
        }
    }
}
