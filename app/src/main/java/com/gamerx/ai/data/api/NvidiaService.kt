package com.gamerx.ai.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class NvidiaService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class ChatMessage(val role: String, val content: String)

    fun streamChat(
        messages: List<ChatMessage>,
        apiKey: String = "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"
    ): Flow<String> = flow {
        val messagesArray = JSONArray()
        for (msg in messages) {
            val obj = JSONObject()
            obj.put("role", msg.role)
            obj.put("content", msg.content)
            messagesArray.put(obj)
        }

        val body = JSONObject()
        body.put("model", "qwen/qwen2.5-coder-32b-instruct")
        body.put("messages", messagesArray)
        body.put("max_tokens", 4096)
        body.put("temperature", 0.6)
        body.put("top_p", 0.95)
        body.put("stream", true)

        val request = Request.Builder()
            .url("https://integrate.api.nvidia.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toByteArray().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API error ${response.code}: ${response.body?.string()}")
            }

            val source = response.body?.source() ?: throw IOException("Empty response")

            var insideThink = false

            while (currentCoroutineContext().isActive && !source.exhausted()) {
                val line = source.readUtf8Line() ?: continue

                if (!line.startsWith("data: ")) continue
                val data = line.substring(6).trim()
                if (data == "[DONE]") break

                try {
                    val json = JSONObject(data)
                    val choices = json.optJSONArray("choices") ?: continue
                    if (choices.length() == 0) continue
                    val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                    if (!delta.has("content")) continue

                    var content = delta.getString("content")

                    // Strip <think> blocks from Qwen thinking mode
                    if (content.contains("<think>")) {
                        insideThink = true
                        content = content.substringBefore("<think>")
                    }
                    if (insideThink) {
                        if (content.contains("</think>")) {
                            insideThink = false
                            content = content.substringAfter("</think>")
                        } else {
                            continue
                        }
                    }

                    if (content.isNotEmpty()) {
                        emit(content)
                    }
                } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateTitle(prompt: String): String {
        val messages = listOf(
            ChatMessage("system", "Generate a very short 3-5 word title summarizing the user's message. Output ONLY the title, nothing else."),
            ChatMessage("user", prompt)
        )

        val result = StringBuilder()
        try {
            streamChat(messages).collect { chunk ->
                result.append(chunk)
            }
        } catch (_: Exception) {}

        val title = result.toString().trim().trim('"', '\'', '.', '\n')
        return if (title.length > 2) title else prompt.take(30) + "…"
    }
}
