package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LLMClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun streamCompletion(
        baseUrl: String,
        token: String,
        model: String,
        prompt: String,
        systemInstruction: String = ""
    ): Flow<String> = flow {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val endpoint = if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl
        } else if (cleanBaseUrl.endsWith("/v1")) {
            "$cleanBaseUrl/chat/completions"
        } else {
            "$cleanBaseUrl/v1/chat/completions"
        }

        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val json = JSONObject().apply {
            put("model", model.ifBlank { "gpt-4o" })
            put("stream", true)
            put("messages", messages)
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(json.toString().toRequestBody(jsonMediaType))

        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IOException("LLM API Error ${response.code}: $errorBody")
            }
            val source: BufferedSource = response.body?.source()
                ?: throw IOException("Empty response body from LLM provider")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isBlank()) continue
                    try {
                        val chunk = JSONObject(data)
                        val choices = chunk.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content")
                            if (!content.isNullOrEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (_: Exception) {
                        // Tolerate unparsed chunks or non-standard SSE lines
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
