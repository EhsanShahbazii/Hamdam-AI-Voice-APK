package com.example.data.api

import android.util.Log
import com.example.data.database.VoiceMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class GapGptService {

    private val client = OkClient.client

    object OkClient {
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    suspend fun transcribe(
        apiKey: String,
        baseUrl: String,
        whisperModel: String,
        audioFile: File
    ): String = withContext(Dispatchers.IO) {
        if (!audioFile.exists()) {
            throw IOException("فایل صوتی یافت نشد.")
        }

        val url = "${baseUrl.trimEnd('/')}/audio/transcriptions"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType())
            )
            .addFormDataPart("model", whisperModel)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            Log.d("GapGptService", "Whisper Response: Code=${response.code}, Body=$responseBody")
            
            if (!response.isSuccessful) {
                throw IOException("خطای ویسپر (${response.code}): $responseBody")
            }

            val json = JSONObject(responseBody)
            var text = json.optString("text", "")
            if (text.isEmpty()) {
                text = json.optString("transcription", "")
            }
            
            if (text.isEmpty()) {
                throw IOException("پاسخی از ویسپر دریافت نشد.")
            }
            text.trim()
        }
    }

    suspend fun askAi(
        apiKey: String,
        baseUrl: String,
        chatModel: String,
        systemPrompt: String,
        history: List<VoiceMessage>
    ): String = withContext(Dispatchers.IO) {
        val url = "${baseUrl.trimEnd('/')}/chat/completions"

        val jsonMessages = JSONArray()
        // Add system prompt
        jsonMessages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        // Add last 16 messages from history
        val recentHistory = if (history.size > 16) history.takeLast(16) else history
        recentHistory.forEach { msg ->
            jsonMessages.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.transcript)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", chatModel)
            put("messages", jsonMessages)
            put("temperature", 0.7)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            Log.d("GapGptService", "Chat Response: Code=${response.code}")

            if (!response.isSuccessful) {
                throw IOException("خطای چت جی‌پی‌تی (${response.code}): $responseBody")
            }

            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                content.trim()
            } else {
                throw IOException("پاسخی از هوش مصنوعی دریافت نشد.")
            }
        }
    }

    suspend fun generateSpeech(
        apiKey: String,
        baseUrl: String,
        ttsModel: String,
        voice: String,
        text: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "${baseUrl.trimEnd('/')}/audio/speech"

        val jsonBody = JSONObject().apply {
            put("model", ttsModel)
            put("voice", voice)
            put("input", text)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("GapGptService", "TTS Response Error: Code=${response.code}, Body=$errorBody")
                throw IOException("خطای تولید صدا (${response.code}): $errorBody")
            }

            val body = response.body ?: throw IOException("پاسخ تولید صدا خالی است.")
            
            FileOutputStream(outputFile).use { out ->
                body.byteStream().use { inp ->
                    inp.copyTo(out)
                }
            }
            Log.d("GapGptService", "Speech file generated: ${outputFile.absolutePath}")
            true
        }
    }
}
